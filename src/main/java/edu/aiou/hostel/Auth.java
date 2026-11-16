package edu.aiou.hostel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;

public final class Auth {
    private Auth() {}

    public static class StudentSession {
        public final int id;
        public final String studentNo;
        public final String fullName;
        public final String email;
        public final String phone;
        public final String department;
        public final String gender;

        public StudentSession(int id, String studentNo, String fullName, String email, String phone, String department, String gender) {
            this.id = id;
            this.studentNo = studentNo != null ? studentNo : "";
            this.fullName = fullName != null ? fullName : "";
            this.email = email != null ? email : "";
            this.phone = phone != null ? phone : "";
            this.department = department != null ? department : "";
            this.gender = gender != null ? gender : "";
        }
    }

    public static String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean login(String username, String password) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT password_hash FROM admins WHERE username=?")) {
            p.setString(1, username);
            try (ResultSet r = p.executeQuery()) {
                return r.next() && r.getString(1).equals(hash(password));
            }
        }
    }

    public static StudentSession loginStudent(String rollOrEmail, String password) throws SQLException {
        if (rollOrEmail == null || rollOrEmail.trim().isEmpty() || password == null) return null;
        String iden = rollOrEmail.trim();
        String hashed = hash(password);

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                     "SELECT id, student_no, full_name, email, phone, department, gender, password_hash " +
                     "FROM students WHERE (LOWER(student_no) = LOWER(?) OR LOWER(email) = LOWER(?)) AND status = 'Active'")) {
            p.setString(1, iden);
            p.setString(2, iden);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    String dbHash = rs.getString("password_hash");
                    boolean match = false;
                    if (dbHash != null && dbHash.equalsIgnoreCase(hashed)) {
                        match = true;
                    } else if ((dbHash == null || dbHash.isEmpty()) && ("123456".equals(password) || iden.equalsIgnoreCase(password))) {
                        match = true;
                        try (PreparedStatement up = c.prepareStatement("UPDATE students SET password_hash = ? WHERE id = ?")) {
                            up.setString(1, hashed);
                            up.setInt(2, rs.getInt("id"));
                            up.executeUpdate();
                        }
                    }
                    if (match) {
                        return new StudentSession(
                                rs.getInt("id"),
                                rs.getString("student_no"),
                                rs.getString("full_name"),
                                rs.getString("email"),
                                rs.getString("phone"),
                                rs.getString("department"),
                                rs.getString("gender")
                        );
                    }
                }
            }
        }
        return null;
    }

    public static void registerStudent(String studentNo, String fullName, String email, String phone, String department, String gender, String password) throws SQLException {
        if (studentNo == null || studentNo.trim().isEmpty()) throw new IllegalArgumentException("Student Roll / Registration No is required.");
        if (fullName == null || fullName.trim().isEmpty()) throw new IllegalArgumentException("Full Name is required.");
        if (password == null || password.trim().isEmpty()) throw new IllegalArgumentException("Password is required.");

        try (Connection c = Database.getConnection()) {
            try (PreparedStatement check = c.prepareStatement("SELECT id FROM students WHERE LOWER(student_no) = LOWER(?)")) {
                check.setString(1, studentNo.trim());
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        throw new IllegalArgumentException("Student with Roll No '" + studentNo.trim() + "' is already registered.\nPlease sign in instead.");
                    }
                }
            }

            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO students (student_no, full_name, gender, phone, email, department, admission_date, status, password_hash) " +
                    "VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE, 'Active', ?)")) {
                p.setString(1, studentNo.trim());
                p.setString(2, fullName.trim());
                p.setString(3, (gender != null && !gender.trim().isEmpty()) ? gender : "Male");
                p.setString(4, phone != null ? phone.trim() : "");
                p.setString(5, email != null ? email.trim() : "");
                p.setString(6, (department != null && !department.trim().isEmpty()) ? department.trim() : "BSCS");
                p.setString(7, hash(password));
                p.executeUpdate();
            }
        }
    }

    public static boolean changeStudentPassword(int studentId, String oldPass, String newPass) throws SQLException {
        if (newPass == null || newPass.trim().isEmpty()) throw new IllegalArgumentException("New password cannot be empty.");
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT password_hash, student_no FROM students WHERE id = ?")) {
            p.setInt(1, studentId);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    String dbHash = rs.getString("password_hash");
                    String roll = rs.getString("student_no");
                    boolean matches = (dbHash != null && dbHash.equalsIgnoreCase(hash(oldPass)))
                            || ((dbHash == null || dbHash.isEmpty()) && ("123456".equals(oldPass) || oldPass.equals(roll)));
                    if (!matches) {
                        return false;
                    }
                    try (PreparedStatement up = c.prepareStatement("UPDATE students SET password_hash = ? WHERE id = ?")) {
                        up.setString(1, hash(newPass));
                        up.setInt(2, studentId);
                        up.executeUpdate();
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
