package edu.aiou.hostel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StudentPortal {
    private final Auth.StudentSession session;
    private JFrame frame;
    private JPanel contentPanel;
    private JPanel sidebarNav;
    private String currentPage = "Overview";

    private final String[] PAGES = {
            "Overview", "My Room & Booking", "My Complaints", "Fee & Payments", "My Profile"
    };

    public static void show(Auth.StudentSession session) {
        SwingUtilities.invokeLater(() -> new StudentPortal(session).createAndShowGUI());
    }

    private StudentPortal(Auth.StudentSession session) {
        this.session = session;
    }

    private void createAndShowGUI() {
        frame = new JFrame("HOSTEL | Student Portal - " + session.fullName + " (" + session.studentNo + ")");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1180, 720);
        frame.setMinimumSize(new Dimension(1000, 620));
        frame.setLocationRelativeTo(null);

        JPanel mainLayout = new JPanel(new BorderLayout());
        mainLayout.setBackground(UIComponents.BG_APP);

        // Sidebar
        JPanel sidebar = createSidebar();
        mainLayout.add(sidebar, BorderLayout.WEST);

        // Content Area
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(UIComponents.BG_APP);
        mainLayout.add(contentPanel, BorderLayout.CENTER);

        frame.setContentPane(mainLayout);
        navigateTo("Overview");
        frame.setVisible(true);
    }

    // ==========================================
    // SIDEBAR
    // ==========================================
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBackground(UIComponents.SIDEBAR_BG);
        sidebar.setBorder(new EmptyBorder(22, 16, 22, 16));

        // Top Brand & Student Card
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        // Logo
        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        logoRow.setOpaque(false);
        logoRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        UIComponents.RoundPanel diamond = new UIComponents.RoundPanel(8, UIComponents.CHART_LIME);
        diamond.setPreferredSize(new Dimension(20, 20));
        logoRow.add(diamond);

        JLabel brandLbl = new JLabel("HOSTEL");
        brandLbl.setFont(new Font("Segoe UI", Font.BOLD, 19));
        brandLbl.setForeground(Color.WHITE);
        logoRow.add(brandLbl);

        JLabel tag = new JLabel("STUDENT PORTAL");
        tag.setFont(new Font("Segoe UI", Font.BOLD, 10));
        tag.setForeground(UIComponents.CHART_PURPLE);
        tag.setBorder(new EmptyBorder(3, 4, 0, 0));
        logoRow.add(tag);

        topContainer.add(logoRow);
        topContainer.add(Box.createVerticalStrut(20));

        // Student Profile Badge
        UIComponents.RoundPanel studentCard = new UIComponents.RoundPanel(14, new Color(34, 35, 42));
        studentCard.setLayout(new BorderLayout(10, 0));
        studentCard.setBorder(new EmptyBorder(12, 12, 12, 12));
        studentCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        UIComponents.RoundPanel avatar = new UIComponents.RoundPanel(12, UIComponents.CHART_PURPLE);
        avatar.setPreferredSize(new Dimension(38, 38));
        avatar.setLayout(new GridBagLayout());
        JLabel avIcon = new JLabel("🎓");
        avIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        avatar.add(avIcon);
        studentCard.add(avatar, BorderLayout.WEST);

        JPanel details = new JPanel(new GridLayout(2, 1, 0, 2));
        details.setOpaque(false);
        JLabel nameLbl = new JLabel(session.fullName);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLbl.setForeground(Color.WHITE);
        JLabel rollLbl = new JLabel(session.studentNo + " • " + (session.department.isEmpty() ? "AIOU" : session.department));
        rollLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        rollLbl.setForeground(UIComponents.SIDEBAR_TEXT);
        details.add(nameLbl);
        details.add(rollLbl);
        studentCard.add(details, BorderLayout.CENTER);

        topContainer.add(studentCard);
        topContainer.add(Box.createVerticalStrut(24));

        // Navigation Items
        sidebarNav = new JPanel();
        sidebarNav.setLayout(new BoxLayout(sidebarNav, BoxLayout.Y_AXIS));
        sidebarNav.setOpaque(false);
        refreshSidebar();
        topContainer.add(sidebarNav);

        sidebar.add(topContainer, BorderLayout.NORTH);

        // Bottom Sign Out
        JButton signOutBtn = new JButton("🚪  Sign Out");
        signOutBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        signOutBtn.setForeground(new Color(254, 202, 202));
        signOutBtn.setBackground(new Color(42, 24, 28));
        signOutBtn.setFocusPainted(false);
        signOutBtn.setBorder(new EmptyBorder(10, 16, 10, 16));
        signOutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        signOutBtn.addActionListener(e -> {
            frame.dispose();
            Main.showLogin();
        });
        sidebar.add(signOutBtn, BorderLayout.SOUTH);

        return sidebar;
    }

    private void refreshSidebar() {
        if (sidebarNav == null) return;
        sidebarNav.removeAll();

        String[][] items = {
                {"Overview", "📊"},
                {"My Room & Booking", "🛏️"},
                {"My Complaints", "💬"},
                {"Fee & Payments", "💳"},
                {"My Profile", "👤"}
        };

        for (String[] it : items) {
            String name = it[0];
            String icon = it[1];
            boolean active = name.equalsIgnoreCase(currentPage);

            UIComponents.RoundPanel itemPanel = new UIComponents.RoundPanel(10, active ? Color.WHITE : UIComponents.SIDEBAR_BG);
            itemPanel.setLayout(new BorderLayout(10, 0));
            itemPanel.setBorder(new EmptyBorder(9, 12, 9, 12));
            itemPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            itemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

            JLabel iconLbl = new JLabel(icon);
            iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));

            JLabel textLbl = new JLabel(name);
            textLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            textLbl.setForeground(active ? UIComponents.TEXT_DARK : UIComponents.SIDEBAR_TEXT);

            itemPanel.add(iconLbl, BorderLayout.WEST);
            itemPanel.add(textLbl, BorderLayout.CENTER);

            itemPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    navigateTo(name);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!name.equalsIgnoreCase(currentPage)) {
                        itemPanel.setBgColor(UIComponents.SIDEBAR_HOVER);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!name.equalsIgnoreCase(currentPage)) {
                        itemPanel.setBgColor(UIComponents.SIDEBAR_BG);
                    }
                }
            });

            sidebarNav.add(itemPanel);
            sidebarNav.add(Box.createVerticalStrut(6));
        }

        sidebarNav.revalidate();
        sidebarNav.repaint();
    }

    private void navigateTo(String page) {
        this.currentPage = page;
        refreshSidebar();

        contentPanel.removeAll();
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UIComponents.BG_APP);

        switch (page) {
            case "Overview":
                p.add(renderOverviewPage(), BorderLayout.CENTER);
                break;
            case "My Room & Booking":
                p.add(renderRoomBookingPage(), BorderLayout.CENTER);
                break;
            case "My Complaints":
                p.add(renderComplaintsPage(), BorderLayout.CENTER);
                break;
            case "Fee & Payments":
                p.add(renderFeePaymentsPage(), BorderLayout.CENTER);
                break;
            case "My Profile":
                p.add(renderProfilePage(), BorderLayout.CENTER);
                break;
            default:
                p.add(renderOverviewPage(), BorderLayout.CENTER);
        }

        contentPanel.add(p, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // ==========================================
    // 1. OVERVIEW PAGE
    // ==========================================
    private JComponent renderOverviewPage() {
        JPanel scrollContent = new JPanel();
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setBackground(UIComponents.BG_APP);
        scrollContent.setBorder(new EmptyBorder(24, 28, 28, 28));

        // Welcome Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Welcome, " + session.fullName);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(UIComponents.TEXT_DARK);
        JLabel sub = new JLabel("Roll No: " + session.studentNo + " • Department: " + session.department);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(UIComponents.TEXT_MUTED);

        JPanel titleCol = new JPanel(new GridLayout(2, 1, 0, 4));
        titleCol.setOpaque(false);
        titleCol.add(title);
        titleCol.add(sub);
        header.add(titleCol, BorderLayout.WEST);

        scrollContent.add(header);
        scrollContent.add(Box.createVerticalStrut(20));

        // Fetch student specific counts
        String roomDisplay = "Not Allocated";
        int openComplaints = 0;
        int totalComplaints = 0;
        double totalPaid = 0.0;

        try (Connection c = Database.getConnection()) {
            // Room Allocation
            try (PreparedStatement p = c.prepareStatement(
                    "SELECT r.room_no, a.bed_no FROM allocations a JOIN rooms r ON a.room_id = r.id WHERE a.student_id = ? AND a.status = 'Active' LIMIT 1")) {
                p.setInt(1, session.id);
                try (ResultSet rs = p.executeQuery()) {
                    if (rs.next()) {
                        roomDisplay = "Room " + rs.getString(1) + (rs.getString(2) != null ? " (Bed " + rs.getString(2) + ")" : "");
                    }
                }
            }
            // Complaints
            try (PreparedStatement p = c.prepareStatement(
                    "SELECT COUNT(*), COALESCE(SUM(CASE WHEN status='Open' THEN 1 ELSE 0 END), 0) FROM complaints WHERE student_id = ?")) {
                p.setInt(1, session.id);
                try (ResultSet rs = p.executeQuery()) {
                    if (rs.next()) {
                        totalComplaints = rs.getInt(1);
                        openComplaints = rs.getInt(2);
                    }
                }
            }
            // Payments
            try (PreparedStatement p = c.prepareStatement(
                    "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE student_id = ?")) {
                p.setInt(1, session.id);
                try (ResultSet rs = p.executeQuery()) {
                    if (rs.next()) {
                        totalPaid = rs.getDouble(1);
                    }
                }
            }
        } catch (Exception ignored) {}

        // Stat Row
        JPanel statRow = new JPanel(new GridLayout(1, 3, 16, 0));
        statRow.setOpaque(false);
        statRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));

        JPanel cardRoom = UIComponents.createStatCard(
                "My Allocated Room",
                roomDisplay,
                UIComponents.STAT_BLUE,
                UIComponents.STAT_BLUE_TEXT,
                "🛏️",
                () -> navigateTo("My Room & Booking"),
                "Click to view or book room"
        );

        JPanel cardComp = UIComponents.createStatCard(
                "My Complaints",
                openComplaints + " Open / " + totalComplaints + " Total",
                UIComponents.STAT_LIME,
                UIComponents.STAT_LIME_TEXT,
                "💬",
                () -> navigateTo("My Complaints"),
                "Click to view or lodge complaints"
        );

        JPanel cardFee = UIComponents.createStatCard(
                "Total Fee Paid",
                "Rs. " + String.format("%,.0f", totalPaid),
                UIComponents.STAT_PURPLE,
                UIComponents.STAT_PURPLE_TEXT,
                "💳",
                () -> navigateTo("Fee & Payments"),
                "Click to view payment history"
        );

        statRow.add(cardRoom);
        statRow.add(cardComp);
        statRow.add(cardFee);
        scrollContent.add(statRow);
        scrollContent.add(Box.createVerticalStrut(24));

        // Quick Actions Row
        UIComponents.RoundPanel quickPanel = new UIComponents.RoundPanel(16, Color.WHITE, UIComponents.CARD_BORDER);
        quickPanel.setLayout(new BorderLayout(0, 12));
        quickPanel.setBorder(new EmptyBorder(16, 18, 16, 18));
        quickPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JLabel qTitle = new JLabel("Quick Actions");
        qTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        qTitle.setForeground(UIComponents.TEXT_DARK);
        quickPanel.add(qTitle, BorderLayout.NORTH);

        JPanel actionsBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        actionsBar.setOpaque(false);

        JButton btnBook = createButton("🛏️  Book / View Room", UIComponents.SIDEBAR_BG, Color.WHITE);
        btnBook.addActionListener(e -> navigateTo("My Room & Booking"));

        JButton btnComp = createButton("💬  Lodge a Complaint", new Color(63, 98, 18), Color.WHITE);
        btnComp.addActionListener(e -> showAddComplaintDialog());

        JButton btnPay = createButton("💳  Report Fee Payment", new Color(91, 33, 182), Color.WHITE);
        btnPay.addActionListener(e -> showAddPaymentDialog());

        actionsBar.add(btnBook);
        actionsBar.add(btnComp);
        actionsBar.add(btnPay);
        quickPanel.add(actionsBar, BorderLayout.CENTER);

        scrollContent.add(quickPanel);
        scrollContent.add(Box.createVerticalStrut(24));

        // Recent Complaints Card
        UIComponents.RoundPanel compPanel = new UIComponents.RoundPanel(16, Color.WHITE, UIComponents.CARD_BORDER);
        compPanel.setLayout(new BorderLayout(0, 10));
        compPanel.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel compHead = new JPanel(new BorderLayout());
        compHead.setOpaque(false);
        JLabel cTitle = new JLabel("Recent Complaints & Status");
        cTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        cTitle.setForeground(UIComponents.TEXT_DARK);
        compHead.add(cTitle, BorderLayout.WEST);

        JButton viewAll = new JButton("View All →");
        viewAll.setFont(new Font("Segoe UI", Font.BOLD, 12));
        viewAll.setForeground(new Color(91, 33, 182));
        viewAll.setBorderPainted(false);
        viewAll.setContentAreaFilled(false);
        viewAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        viewAll.addActionListener(e -> navigateTo("My Complaints"));
        compHead.add(viewAll, BorderLayout.EAST);
        compPanel.add(compHead, BorderLayout.NORTH);

        String[] cols = {"Category", "Subject", "Details", "Status", "Date"};
        DefaultTableModel cModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                     "SELECT category, subject, details, status, DATE_FORMAT(created_at, '%Y-%m-%d') FROM complaints WHERE student_id = ? ORDER BY created_at DESC LIMIT 5")) {
            p.setInt(1, session.id);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    cModel.addRow(new Object[]{
                            rs.getString(1),
                            rs.getString(2),
                            rs.getString(3),
                            rs.getString(4),
                            rs.getString(5)
                    });
                }
            }
        } catch (Exception ignored) {}

        if (cModel.getRowCount() == 0) {
            cModel.addRow(new Object[]{"-", "No complaints lodged yet", "All good!", "Resolved", "-"});
        }

        JTable table = new JTable(cModel);
        UIComponents.styleTable(table);
        table.getColumnModel().getColumn(3).setCellRenderer(new UIComponents.StatusBadgeRenderer());

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        sp.setPreferredSize(new Dimension(600, 160));
        compPanel.add(sp, BorderLayout.CENTER);

        scrollContent.add(compPanel);

        JScrollPane mainScroll = new JScrollPane(scrollContent);
        mainScroll.setBorder(BorderFactory.createEmptyBorder());
        mainScroll.getVerticalScrollBar().setUnitIncrement(14);
        return mainScroll;
    }

    // ==========================================
    // 2. MY ROOM & BOOKING PAGE
    // ==========================================
    private JComponent renderRoomBookingPage() {
        JPanel scrollContent = new JPanel();
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setBackground(UIComponents.BG_APP);
        scrollContent.setBorder(new EmptyBorder(24, 28, 28, 28));

        // Title
        JLabel title = new JLabel("My Room & Hostel Booking");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(UIComponents.TEXT_DARK);
        scrollContent.add(title);
        scrollContent.add(Box.createVerticalStrut(16));

        // Check if student has active room
        boolean hasRoom = false;
        String curRoomNo = "", curType = "", curFloor = "", curBed = "", curStatus = "", curStart = "";
        double curFee = 0.0;
        int allocId = 0;

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                     "SELECT a.id, r.room_no, r.room_type, r.floor_no, a.bed_no, r.monthly_fee, a.status, a.start_date " +
                     "FROM allocations a JOIN rooms r ON a.room_id = r.id " +
                     "WHERE a.student_id = ? AND a.status = 'Active' LIMIT 1")) {
            p.setInt(1, session.id);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    hasRoom = true;
                    allocId = rs.getInt(1);
                    curRoomNo = rs.getString(2);
                    curType = rs.getString(3);
                    curFloor = rs.getString(4);
                    curBed = rs.getString(5);
                    curFee = rs.getDouble(6);
                    curStatus = rs.getString(7);
                    curStart = rs.getString(8);
                }
            }
        } catch (Exception ignored) {}

        // Current Room Panel
        UIComponents.RoundPanel currentPanel = new UIComponents.RoundPanel(16, Color.WHITE, UIComponents.CARD_BORDER);
        currentPanel.setLayout(new BorderLayout(0, 16));
        currentPanel.setBorder(new EmptyBorder(18, 20, 18, 20));
        currentPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JLabel curTitle = new JLabel("Current Room Allocation");
        curTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        curTitle.setForeground(UIComponents.TEXT_DARK);
        currentPanel.add(curTitle, BorderLayout.NORTH);

        if (hasRoom) {
            JPanel infoGrid = new JPanel(new GridLayout(2, 4, 16, 10));
            infoGrid.setOpaque(false);

            infoGrid.add(createInfoField("Room Number", "Room " + curRoomNo));
            infoGrid.add(createInfoField("Room Type", curType != null ? curType : "Standard"));
            infoGrid.add(createInfoField("Floor", curFloor != null ? curFloor : "1st"));
            infoGrid.add(createInfoField("Bed Number", curBed != null ? curBed : "1"));

            infoGrid.add(createInfoField("Monthly Rent", "Rs. " + String.format("%,.0f", curFee)));
            infoGrid.add(createInfoField("Start Date", curStart));
            infoGrid.add(createInfoField("Status", curStatus));

            final int finalAllocId = allocId;
            final String finalRoomNo = curRoomNo;
            JButton vacateBtn = createButton("Request Checkout / Vacate", new Color(153, 27, 27), Color.WHITE);
            vacateBtn.addActionListener(e -> {
                int conf = JOptionPane.showConfirmDialog(frame, "Are you sure you want to vacate Room " + finalRoomNo + "?", "Confirm Vacate", JOptionPane.YES_NO_OPTION);
                if (conf == JOptionPane.YES_OPTION) {
                    try (Connection c = Database.getConnection();
                         PreparedStatement up = c.prepareStatement("UPDATE allocations SET status = 'Completed', end_date = CURRENT_DATE WHERE id = ?")) {
                        up.setInt(1, finalAllocId);
                        up.executeUpdate();
                        JOptionPane.showMessageDialog(frame, "Room vacated successfully.");
                        navigateTo("My Room & Booking");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
                    }
                }
            });
            infoGrid.add(vacateBtn);

            currentPanel.add(infoGrid, BorderLayout.CENTER);
        } else {
            JPanel emptyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            emptyPanel.setOpaque(false);
            JLabel msg = new JLabel("⚠️ You currently have no allocated room. Please choose an available room below to book.");
            msg.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            msg.setForeground(new Color(180, 83, 9));
            emptyPanel.add(msg);
            currentPanel.add(emptyPanel, BorderLayout.CENTER);
        }

        scrollContent.add(currentPanel);
        scrollContent.add(Box.createVerticalStrut(24));

        // Available Rooms Section
        UIComponents.RoundPanel availPanel = new UIComponents.RoundPanel(16, Color.WHITE, UIComponents.CARD_BORDER);
        availPanel.setLayout(new BorderLayout(0, 14));
        availPanel.setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel headRow = new JPanel(new BorderLayout());
        headRow.setOpaque(false);
        JLabel availTitle = new JLabel("Available Hostel Rooms (Select & Book)");
        availTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        availTitle.setForeground(UIComponents.TEXT_DARK);
        headRow.add(availTitle, BorderLayout.WEST);

        JButton bookBtn = createButton("+ Book Selected Room", UIComponents.SIDEBAR_BG, Color.WHITE);
        headRow.add(bookBtn, BorderLayout.EAST);
        availPanel.add(headRow, BorderLayout.NORTH);

        String[] cols = {"Room ID", "Room No", "Type", "Capacity", "Monthly Fee (Rs)", "Floor", "Status"};
        DefaultTableModel rModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, room_no, room_type, capacity, monthly_fee, floor_no, status FROM rooms WHERE status = 'Available' ORDER BY room_no ASC")) {
            while (rs.next()) {
                rModel.addRow(new Object[]{
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getInt(4),
                        String.format("%,.0f", rs.getDouble(5)),
                        rs.getString(6),
                        rs.getString(7)
                });
            }
        } catch (Exception ignored) {}

        JTable table = new JTable(rModel);
        UIComponents.styleTable(table);
        table.getColumnModel().getColumn(6).setCellRenderer(new UIComponents.StatusBadgeRenderer());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        sp.setPreferredSize(new Dimension(700, 220));
        availPanel.add(sp, BorderLayout.CENTER);

        final boolean studentHasRoom = hasRoom;
        bookBtn.addActionListener(e -> {
            if (studentHasRoom) {
                int conf = JOptionPane.showConfirmDialog(frame,
                        "You already have an active room allocation. Booking a new room will vacate your previous room. Continue?",
                        "Room Change Notice", JOptionPane.YES_NO_OPTION);
                if (conf != JOptionPane.YES_OPTION) return;
            }

            int selRow = table.getSelectedRow();
            if (selRow < 0) {
                JOptionPane.showMessageDialog(frame, "Please select an available room from the table first.", "Select Room", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int roomId = (int) table.getValueAt(selRow, 0);
            String roomNo = String.valueOf(table.getValueAt(selRow, 1));
            String roomType = String.valueOf(table.getValueAt(selRow, 2));
            String fee = String.valueOf(table.getValueAt(selRow, 4));

            showConfirmBookingDialog(roomId, roomNo, roomType, fee);
        });

        scrollContent.add(availPanel);

        JScrollPane mainScroll = new JScrollPane(scrollContent);
        mainScroll.setBorder(BorderFactory.createEmptyBorder());
        mainScroll.getVerticalScrollBar().setUnitIncrement(14);
        return mainScroll;
    }

    private void showConfirmBookingDialog(int roomId, String roomNo, String roomType, String fee) {
        JDialog d = new JDialog(frame, "Confirm Room Booking", true);
        d.setSize(440, 320);
        d.setLocationRelativeTo(frame);
        d.setResizable(false);

        JPanel p = new JPanel(new GridLayout(5, 2, 12, 14));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(20, 24, 20, 24));

        p.add(new JLabel("Room Selected:"));
        JLabel rLbl = new JLabel("Room " + roomNo + " (" + roomType + ")");
        rLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        p.add(rLbl);

        p.add(new JLabel("Monthly Fee:"));
        JLabel fLbl = new JLabel("Rs. " + fee);
        fLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        p.add(fLbl);

        p.add(new JLabel("Bed Preference:"));
        JComboBox<String> bedCombo = new JComboBox<>(new String[]{"Bed 1", "Bed 2", "Bed 3", "Bed 4"});
        p.add(bedCombo);

        p.add(new JLabel("Start Date:"));
        JTextField dateField = new JTextField(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        p.add(dateField);

        JButton cancel = createButton("Cancel", new Color(240, 242, 245), UIComponents.TEXT_DARK);
        cancel.addActionListener(e -> d.dispose());
        p.add(cancel);

        JButton confirm = createButton("Confirm Booking", UIComponents.SIDEBAR_BG, Color.WHITE);
        confirm.addActionListener(e -> {
            try (Connection c = Database.getConnection()) {
                // If student has existing active room, complete it
                try (PreparedStatement up = c.prepareStatement("UPDATE allocations SET status = 'Completed', end_date = CURRENT_DATE WHERE student_id = ? AND status = 'Active'")) {
                    up.setInt(1, session.id);
                    up.executeUpdate();
                }

                // Insert new allocation
                try (PreparedStatement ins = c.prepareStatement(
                        "INSERT INTO allocations (student_id, room_id, bed_no, start_date, status) VALUES (?, ?, ?, ?, 'Active')")) {
                    ins.setInt(1, session.id);
                    ins.setInt(2, roomId);
                    ins.setString(3, (String) bedCombo.getSelectedItem());
                    ins.setString(4, dateField.getText().trim());
                    ins.executeUpdate();
                }

                d.dispose();
                JOptionPane.showMessageDialog(frame, "🎉 Room " + roomNo + " booked successfully!", "Booking Confirmed", JOptionPane.INFORMATION_MESSAGE);
                navigateTo("My Room & Booking");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(d, "Booking failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        p.add(confirm);

        d.setContentPane(p);
        d.setVisible(true);
    }

    private JPanel createInfoField(String label, String value) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(UIComponents.TEXT_MUTED);
        JLabel v = new JLabel(value);
        v.setFont(new Font("Segoe UI", Font.BOLD, 13));
        v.setForeground(UIComponents.TEXT_DARK);
        p.add(l);
        p.add(v);
        return p;
    }

    // ==========================================
    // 3. MY COMPLAINTS PAGE
    // ==========================================
    private JComponent renderComplaintsPage() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(UIComponents.BG_APP);
        panel.setBorder(new EmptyBorder(24, 28, 28, 28));

        // Header
        JPanel headRow = new JPanel(new BorderLayout());
        headRow.setOpaque(false);

        JPanel titleCol = new JPanel(new GridLayout(2, 1, 0, 4));
        titleCol.setOpaque(false);
        JLabel title = new JLabel("My Complaints");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(UIComponents.TEXT_DARK);
        JLabel sub = new JLabel("Lodge and track your hostel maintenance, mess, or cleanliness complaints.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(UIComponents.TEXT_MUTED);
        titleCol.add(title);
        titleCol.add(sub);
        headRow.add(titleCol, BorderLayout.WEST);

        JButton lodgeBtn = createButton("+ Lodge New Complaint", new Color(63, 98, 18), Color.WHITE);
        lodgeBtn.addActionListener(e -> showAddComplaintDialog());
        headRow.add(lodgeBtn, BorderLayout.EAST);
        panel.add(headRow, BorderLayout.NORTH);

        // Complaints Table Card
        UIComponents.RoundPanel tableCard = new UIComponents.RoundPanel(16, Color.WHITE, UIComponents.CARD_BORDER);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] cols = {"ID", "Category", "Subject", "Details", "Status", "Date Submitted"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                     "SELECT id, category, subject, details, status, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i') FROM complaints WHERE student_id = ? ORDER BY created_at DESC")) {
            p.setInt(1, session.id);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt(1),
                            rs.getString(2),
                            rs.getString(3),
                            rs.getString(4),
                            rs.getString(5),
                            rs.getString(6)
                    });
                }
            }
        } catch (Exception ignored) {}

        JTable table = new JTable(model);
        UIComponents.styleTable(table);
        table.getColumnModel().getColumn(4).setCellRenderer(new UIComponents.StatusBadgeRenderer());

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        tableCard.add(sp, BorderLayout.CENTER);

        panel.add(tableCard, BorderLayout.CENTER);
        return panel;
    }

    private void showAddComplaintDialog() {
        JDialog d = new JDialog(frame, "Lodge a Complaint", true);
        d.setSize(480, 420);
        d.setLocationRelativeTo(frame);
        d.setResizable(false);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(22, 24, 22, 24));

        JLabel title = new JLabel("New Complaint");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(UIComponents.TEXT_DARK);
        p.add(title);
        p.add(Box.createVerticalStrut(16));

        // Category
        JLabel catLbl = new JLabel("Category");
        catLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        p.add(catLbl);
        p.add(Box.createVerticalStrut(6));

        String[] categories = {"Maintenance", "Cleanliness", "Food & Mess", "Electricity & Lights", "Water Supply", "Security", "Internet & Wi-Fi", "Other"};
        JComboBox<String> catCombo = new JComboBox<>(categories);
        catCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        catCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        p.add(catCombo);
        p.add(Box.createVerticalStrut(14));

        // Subject
        JLabel subLbl = new JLabel("Subject");
        subLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        p.add(subLbl);
        p.add(Box.createVerticalStrut(6));

        JTextField subField = new JTextField();
        subField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        p.add(subField);
        p.add(Box.createVerticalStrut(14));

        // Details
        JLabel detLbl = new JLabel("Details / Description");
        detLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        p.add(detLbl);
        p.add(Box.createVerticalStrut(6));

        JTextArea detArea = new JTextArea(4, 20);
        detArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        detArea.setLineWrap(true);
        detArea.setWrapStyleWord(true);
        JScrollPane detScroll = new JScrollPane(detArea);
        detScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        p.add(detScroll);
        p.add(Box.createVerticalStrut(20));

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);
        JButton cancel = createButton("Cancel", new Color(240, 242, 245), UIComponents.TEXT_DARK);
        cancel.addActionListener(e -> d.dispose());

        JButton submit = createButton("Submit Complaint", new Color(63, 98, 18), Color.WHITE);
        submit.addActionListener(e -> {
            String subject = subField.getText().trim();
            String details = detArea.getText().trim();
            String category = (String) catCombo.getSelectedItem();

            if (subject.isEmpty()) {
                JOptionPane.showMessageDialog(d, "Please enter a subject for your complaint.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection c = Database.getConnection();
                 PreparedStatement ins = c.prepareStatement(
                         "INSERT INTO complaints (student_id, category, subject, details, status, created_at) VALUES (?, ?, ?, ?, 'Open', NOW())")) {
                ins.setInt(1, session.id);
                ins.setString(2, category);
                ins.setString(3, subject);
                ins.setString(4, details);
                ins.executeUpdate();

                d.dispose();
                JOptionPane.showMessageDialog(frame, "Complaint submitted successfully! Administration will review it shortly.", "Submitted", JOptionPane.INFORMATION_MESSAGE);
                if ("My Complaints".equals(currentPage)) navigateTo("My Complaints");
                else navigateTo("Overview");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(d, "Error submitting complaint: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnRow.add(cancel);
        btnRow.add(submit);
        p.add(btnRow);

        d.setContentPane(p);
        d.setVisible(true);
    }

    // ==========================================
    // 4. FEE & PAYMENTS PAGE
    // ==========================================
    private JComponent renderFeePaymentsPage() {
        JPanel scrollContent = new JPanel();
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setBackground(UIComponents.BG_APP);
        scrollContent.setBorder(new EmptyBorder(24, 28, 28, 28));

        // Header
        JPanel headRow = new JPanel(new BorderLayout());
        headRow.setOpaque(false);

        JPanel titleCol = new JPanel(new GridLayout(2, 1, 0, 4));
        titleCol.setOpaque(false);
        JLabel title = new JLabel("Fee Status & Payment History");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(UIComponents.TEXT_DARK);
        JLabel sub = new JLabel("Track your monthly hostel fees and submitted payment receipts.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(UIComponents.TEXT_MUTED);
        titleCol.add(title);
        titleCol.add(sub);
        headRow.add(titleCol, BorderLayout.WEST);

        JButton reportBtn = createButton("+ Report Payment", new Color(91, 33, 182), Color.WHITE);
        reportBtn.addActionListener(e -> showAddPaymentDialog());
        headRow.add(reportBtn, BorderLayout.EAST);
        scrollContent.add(headRow);
        scrollContent.add(Box.createVerticalStrut(20));

        // Fetch stats
        double monthlyFee = 0.0;
        double totalPaid = 0.0;
        try (Connection c = Database.getConnection()) {
            try (PreparedStatement p = c.prepareStatement(
                    "SELECT r.monthly_fee FROM allocations a JOIN rooms r ON a.room_id = r.id WHERE a.student_id = ? AND a.status = 'Active' LIMIT 1")) {
                p.setInt(1, session.id);
                try (ResultSet rs = p.executeQuery()) {
                    if (rs.next()) monthlyFee = rs.getDouble(1);
                }
            }
            try (PreparedStatement p = c.prepareStatement("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE student_id = ?")) {
                p.setInt(1, session.id);
                try (ResultSet rs = p.executeQuery()) {
                    if (rs.next()) totalPaid = rs.getDouble(1);
                }
            }
        } catch (Exception ignored) {}

        // Stat Cards
        JPanel statRow = new JPanel(new GridLayout(1, 2, 16, 0));
        statRow.setOpaque(false);
        statRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));

        JPanel feeCard = UIComponents.createStatCard(
                "My Monthly Rent",
                monthlyFee > 0 ? "Rs. " + String.format("%,.0f", monthlyFee) : "No Room Allocated",
                UIComponents.STAT_BLUE,
                UIComponents.STAT_BLUE_TEXT,
                "🏠",
                null,
                null
        );

        JPanel paidCard = UIComponents.createStatCard(
                "Total Amount Paid",
                "Rs. " + String.format("%,.0f", totalPaid),
                UIComponents.STAT_PURPLE,
                UIComponents.STAT_PURPLE_TEXT,
                "✅",
                null,
                null
        );

        statRow.add(feeCard);
        statRow.add(paidCard);
        scrollContent.add(statRow);
        scrollContent.add(Box.createVerticalStrut(24));

        // Payments Table
        UIComponents.RoundPanel tableCard = new UIComponents.RoundPanel(16, Color.WHITE, UIComponents.CARD_BORDER);
        tableCard.setLayout(new BorderLayout(0, 10));
        tableCard.setBorder(new EmptyBorder(16, 18, 16, 18));

        JLabel tTitle = new JLabel("Payment Transactions");
        tTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        tTitle.setForeground(UIComponents.TEXT_DARK);
        tableCard.add(tTitle, BorderLayout.NORTH);

        String[] cols = {"ID", "Month For", "Amount (Rs)", "Payment Date", "Method", "Reference No", "Notes"};
        DefaultTableModel pModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                     "SELECT id, month_for, amount, payment_date, method, reference_no, notes FROM payments WHERE student_id = ? ORDER BY payment_date DESC")) {
            p.setInt(1, session.id);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    pModel.addRow(new Object[]{
                            rs.getInt(1),
                            rs.getString(2) != null ? rs.getString(2) : "-",
                            String.format("%,.2f", rs.getDouble(3)),
                            rs.getString(4),
                            rs.getString(5),
                            rs.getString(6) != null ? rs.getString(6) : "-",
                            rs.getString(7) != null ? rs.getString(7) : "-"
                    });
                }
            }
        } catch (Exception ignored) {}

        JTable table = new JTable(pModel);
        UIComponents.styleTable(table);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        sp.setPreferredSize(new Dimension(700, 240));
        tableCard.add(sp, BorderLayout.CENTER);

        scrollContent.add(tableCard);

        JScrollPane mainScroll = new JScrollPane(scrollContent);
        mainScroll.setBorder(BorderFactory.createEmptyBorder());
        mainScroll.getVerticalScrollBar().setUnitIncrement(14);
        return mainScroll;
    }

    private void showAddPaymentDialog() {
        JDialog d = new JDialog(frame, "Report Fee Payment", true);
        d.setSize(440, 430);
        d.setLocationRelativeTo(frame);
        d.setResizable(false);

        JPanel p = new JPanel(new GridLayout(6, 2, 12, 14));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(22, 24, 22, 24));

        p.add(new JLabel("Amount (Rs):"));
        JTextField amtField = new JTextField("12000");
        p.add(amtField);

        p.add(new JLabel("Month For:"));
        JTextField monthField = new JTextField(LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        p.add(monthField);

        p.add(new JLabel("Payment Date:"));
        JTextField dateField = new JTextField(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        p.add(dateField);

        p.add(new JLabel("Payment Method:"));
        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"Bank Transfer", "Online / EasyPaisa / JazzCash", "Cash"});
        p.add(methodCombo);

        p.add(new JLabel("Receipt / Ref No:"));
        JTextField refField = new JTextField();
        p.add(refField);

        JButton cancel = createButton("Cancel", new Color(240, 242, 245), UIComponents.TEXT_DARK);
        cancel.addActionListener(e -> d.dispose());
        p.add(cancel);

        JButton submit = createButton("Submit Payment", new Color(91, 33, 182), Color.WHITE);
        submit.addActionListener(e -> {
            try {
                double amt = Double.parseDouble(amtField.getText().trim());
                try (Connection c = Database.getConnection();
                     PreparedStatement ins = c.prepareStatement(
                             "INSERT INTO payments (student_id, amount, payment_date, month_for, method, reference_no, notes) VALUES (?, ?, ?, ?, ?, ?, 'Submitted via Student Portal')")) {
                    ins.setInt(1, session.id);
                    ins.setDouble(2, amt);
                    ins.setString(3, dateField.getText().trim());
                    ins.setString(4, monthField.getText().trim());
                    ins.setString(5, (String) methodCombo.getSelectedItem());
                    ins.setString(6, refField.getText().trim());
                    ins.executeUpdate();

                    d.dispose();
                    JOptionPane.showMessageDialog(frame, "Payment recorded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    if ("Fee & Payments".equals(currentPage)) navigateTo("Fee & Payments");
                    else navigateTo("Overview");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(d, "Invalid input: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        p.add(submit);

        d.setContentPane(p);
        d.setVisible(true);
    }

    // ==========================================
    // 5. MY PROFILE & SETTINGS PAGE
    // ==========================================
    private JComponent renderProfilePage() {
        JPanel scrollContent = new JPanel();
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setBackground(UIComponents.BG_APP);
        scrollContent.setBorder(new EmptyBorder(24, 28, 28, 28));

        JLabel title = new JLabel("My Profile & Security");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(UIComponents.TEXT_DARK);
        scrollContent.add(title);
        scrollContent.add(Box.createVerticalStrut(18));

        // Fetch latest student record
        String phone = "", email = "", dept = "", gName = "", gPhone = "", addr = "", gender = "";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                     "SELECT phone, email, department, guardian_name, guardian_phone, address, gender FROM students WHERE id = ?")) {
            p.setInt(1, session.id);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    phone = rs.getString(1);
                    email = rs.getString(2);
                    dept = rs.getString(3);
                    gName = rs.getString(4);
                    gPhone = rs.getString(5);
                    addr = rs.getString(6);
                    gender = rs.getString(7);
                }
            }
        } catch (Exception ignored) {}

        // Profile Card
        UIComponents.RoundPanel profCard = new UIComponents.RoundPanel(16, Color.WHITE, UIComponents.CARD_BORDER);
        profCard.setLayout(new BorderLayout(0, 16));
        profCard.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel pTitle = new JLabel("Personal Information");
        pTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        pTitle.setForeground(UIComponents.TEXT_DARK);
        profCard.add(pTitle, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(4, 2, 16, 12));
        grid.setOpaque(false);

        grid.add(createInfoField("Full Name", session.fullName));
        grid.add(createInfoField("Student Roll / Reg No", session.studentNo));
        grid.add(createInfoField("Department", dept != null ? dept : "N/A"));
        grid.add(createInfoField("Gender", gender != null ? gender : "N/A"));
        grid.add(createInfoField("Phone", phone != null ? phone : "N/A"));
        grid.add(createInfoField("Email", email != null ? email : "N/A"));
        grid.add(createInfoField("Guardian Name", gName != null ? gName : "N/A"));
        grid.add(createInfoField("Guardian Phone", gPhone != null ? gPhone : "N/A"));

        profCard.add(grid, BorderLayout.CENTER);
        scrollContent.add(profCard);
        scrollContent.add(Box.createVerticalStrut(20));

        // Change Password Card
        UIComponents.RoundPanel passCard = new UIComponents.RoundPanel(16, Color.WHITE, UIComponents.CARD_BORDER);
        passCard.setLayout(new BorderLayout(0, 14));
        passCard.setBorder(new EmptyBorder(18, 20, 18, 20));
        passCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));

        JLabel passTitle = new JLabel("Change Password");
        passTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        passTitle.setForeground(UIComponents.TEXT_DARK);
        passCard.add(passTitle, BorderLayout.NORTH);

        JPanel passForm = new JPanel(new GridLayout(3, 2, 16, 10));
        passForm.setOpaque(false);

        passForm.add(new JLabel("Current Password:"));
        JPasswordField curPass = new JPasswordField();
        passForm.add(curPass);

        passForm.add(new JLabel("New Password:"));
        JPasswordField newPass = new JPasswordField();
        passForm.add(newPass);

        passForm.add(new JLabel("Confirm New Password:"));
        JPasswordField confPass = new JPasswordField();
        passForm.add(confPass);

        passCard.add(passForm, BorderLayout.CENTER);

        JButton savePass = createButton("Update Password", UIComponents.SIDEBAR_BG, Color.WHITE);
        savePass.addActionListener(e -> {
            String cPass = new String(curPass.getPassword());
            String nPass = new String(newPass.getPassword());
            String conf = new String(confPass.getPassword());

            if (nPass.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "New password cannot be empty.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!nPass.equals(conf)) {
                JOptionPane.showMessageDialog(frame, "New passwords do not match.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (Auth.changeStudentPassword(session.id, cPass, nPass)) {
                    curPass.setText("");
                    newPass.setText("");
                    confPass.setText("");
                    JOptionPane.showMessageDialog(frame, "Password changed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(frame, "Current password is incorrect.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel passBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        passBottom.setOpaque(false);
        passBottom.add(savePass);
        passCard.add(passBottom, BorderLayout.SOUTH);

        scrollContent.add(passCard);

        JScrollPane mainScroll = new JScrollPane(scrollContent);
        mainScroll.setBorder(BorderFactory.createEmptyBorder());
        mainScroll.getVerticalScrollBar().setUnitIncrement(14);
        return mainScroll;
    }

    private JButton createButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(fg);
        b.setBackground(bg);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }
}
