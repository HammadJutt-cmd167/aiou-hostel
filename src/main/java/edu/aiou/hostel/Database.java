package edu.aiou.hostel;

import java.sql.*;

public final class Database {
    private static final String URL = System.getenv().getOrDefault("HOSTEL_DB_URL",
            "jdbc:mysql://localhost:3306/hostel_management_system?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    private static final String USER = System.getenv().getOrDefault("HOSTEL_DB_USER", "root");
    private static final String PASS = System.getenv().getOrDefault("HOSTEL_DB_PASSWORD", "@Hammad167");

    private Database() {}
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
    public static void initialize() throws SQLException {
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            String[] ddl = {
                "CREATE TABLE IF NOT EXISTS admins (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) UNIQUE NOT NULL, password_hash VARCHAR(64) NOT NULL, full_name VARCHAR(100) NOT NULL)",
                "CREATE TABLE IF NOT EXISTS students (id INT AUTO_INCREMENT PRIMARY KEY, student_no VARCHAR(40) UNIQUE, full_name VARCHAR(120) NOT NULL, gender VARCHAR(20), phone VARCHAR(30), email VARCHAR(120), department VARCHAR(100), guardian_name VARCHAR(120), guardian_phone VARCHAR(30), address TEXT, admission_date DATE, status VARCHAR(20) DEFAULT 'Active', password_hash VARCHAR(64) DEFAULT NULL)",
                "CREATE TABLE IF NOT EXISTS rooms (id INT AUTO_INCREMENT PRIMARY KEY, room_no VARCHAR(20) UNIQUE NOT NULL, room_type VARCHAR(40), capacity INT NOT NULL, monthly_fee DECIMAL(10,2) NOT NULL DEFAULT 0, floor_no VARCHAR(20), status VARCHAR(20) DEFAULT 'Available')",
                "CREATE TABLE IF NOT EXISTS allocations (id INT AUTO_INCREMENT PRIMARY KEY, student_id INT NOT NULL, room_id INT NOT NULL, bed_no VARCHAR(20), start_date DATE NOT NULL, end_date DATE, status VARCHAR(20) DEFAULT 'Active', FOREIGN KEY(student_id) REFERENCES students(id), FOREIGN KEY(room_id) REFERENCES rooms(id))",
                "CREATE TABLE IF NOT EXISTS payments (id INT AUTO_INCREMENT PRIMARY KEY, student_id INT NOT NULL, amount DECIMAL(10,2) NOT NULL, payment_date DATE NOT NULL, month_for VARCHAR(30), method VARCHAR(30), reference_no VARCHAR(80), notes TEXT, FOREIGN KEY(student_id) REFERENCES students(id))",
                "CREATE TABLE IF NOT EXISTS complaints (id INT AUTO_INCREMENT PRIMARY KEY, student_id INT, category VARCHAR(60), subject VARCHAR(150) NOT NULL, details TEXT, status VARCHAR(30) DEFAULT 'Open', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE SET NULL)",
                "CREATE TABLE IF NOT EXISTS staff (id INT AUTO_INCREMENT PRIMARY KEY, full_name VARCHAR(120) NOT NULL, role VARCHAR(80), phone VARCHAR(30), salary DECIMAL(10,2) DEFAULT 0, joining_date DATE, status VARCHAR(20) DEFAULT 'Active')",
                "CREATE TABLE IF NOT EXISTS visitors (id INT AUTO_INCREMENT PRIMARY KEY, visitor_name VARCHAR(120) NOT NULL, student_id INT, relation VARCHAR(60), phone VARCHAR(30), visit_date DATE, check_in TIME, check_out TIME, purpose VARCHAR(200), FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE SET NULL)"
            };
            for (String sql : ddl) s.executeUpdate(sql);

            // Add password_hash column if it doesn't exist on pre-existing students table
            try {
                s.executeUpdate("ALTER TABLE students ADD COLUMN password_hash VARCHAR(64) DEFAULT NULL");
            } catch (SQLException ignored) {}

            // Set default password hash (123456) for any student that doesn't have one set yet
            try (PreparedStatement p = c.prepareStatement("UPDATE students SET password_hash = ? WHERE password_hash IS NULL OR password_hash = ''")) {
                p.setString(1, Auth.hash("123456"));
                p.executeUpdate();
            } catch (SQLException ignored) {}

            try (PreparedStatement p = c.prepareStatement("INSERT IGNORE INTO admins(username,password_hash,full_name) VALUES(?,?,?)")) {
                p.setString(1, "admin"); p.setString(2, Auth.hash("admin123")); p.setString(3, "Hostel Administrator"); p.executeUpdate();
            }
        }
    }
}
