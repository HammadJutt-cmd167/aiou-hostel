package edu.aiou.hostel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

public class Main {
    static JFrame frame;
    static JPanel contentPanel;
    static String currentPage = "Dashboard";
    static JPanel sidebarNav;

    static final String[] MENU_PAGES = {
            "Dashboard", "Students", "Rooms & Beds", "Allocations", "Rent", "Complaints", "Staff", "Visitors"
    };

    public static void main(String[] args) {
        boolean headless = GraphicsEnvironment.isHeadless() || (args.length > 0 && "--server".equalsIgnoreCase(args[0]));
        if (headless) {
            System.out.println("Starting AIOU Hostel in Cloud Server Mode (Headless)...");
            try {
                Database.initialize();
                WebServer.start();
                System.out.println("AIOU Hostel Management Cloud Web Server running successfully on port " + WebServer.getPort());
                Thread.currentThread().join();
            } catch (Exception e) {
                System.err.println("Fatal server error: " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                Database.initialize();
                WebServer.start();
                setupGlobalKeyNavigation();
                showLogin();
            } catch (Exception e) {
                error("Could not connect to MySQL. Check setup instructions.\n" + e.getMessage());
            }
        });
    }

    // ==========================================
    // KEYBOARD NAVIGATION (Arrow Keys Up / Down)
    // ==========================================
    static void setupGlobalKeyNavigation() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && frame != null && frame.isVisible()) {
                Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                boolean isTypingInText = focus instanceof javax.swing.text.JTextComponent;

                // When Alt+Up/Down is pressed, or when user is not actively typing in a text field
                if (e.isAltDown() || !isTypingInText) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        navigateNext();
                        return true;
                    } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                        navigatePrevious();
                        return true;
                    }
                }
            }
            return false;
        });
    }

    static void navigateNext() {
        int idx = getPageIndex(currentPage);
        int nextIdx = (idx + 1) % MENU_PAGES.length;
        navigateTo(MENU_PAGES[nextIdx]);
    }

    static void navigatePrevious() {
        int idx = getPageIndex(currentPage);
        int prevIdx = (idx - 1 + MENU_PAGES.length) % MENU_PAGES.length;
        navigateTo(MENU_PAGES[prevIdx]);
    }

    static int getPageIndex(String page) {
        for (int i = 0; i < MENU_PAGES.length; i++) {
            if (MENU_PAGES[i].equalsIgnoreCase(page)) return i;
        }
        return 0;
    }

    // ==========================================
    // LOGIN WINDOW
    // ==========================================
    static void showLogin() {
        JFrame login = new JFrame("HOSTEL | Sign In");
        login.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        login.setSize(920, 580);
        login.setLocationRelativeTo(null);
        login.setResizable(false);

        JPanel root = new JPanel(new GridLayout(1, 2));
        root.setBackground(Color.WHITE);

        // Left Brand Panel (Dark SaaS Theme)
        JPanel brand = new JPanel();
        brand.setBackground(UIComponents.SIDEBAR_BG);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(new EmptyBorder(55, 45, 40, 45));

        // Brand Logo Row
        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logoRow.setOpaque(false);
        logoRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        UIComponents.RoundPanel diamondIcon = new UIComponents.RoundPanel(8, UIComponents.CHART_LIME);
        diamondIcon.setPreferredSize(new Dimension(24, 24));
        logoRow.add(diamondIcon);

        JLabel brandTitle = new JLabel("HOSTEL");
        brandTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        brandTitle.setForeground(Color.WHITE);
        logoRow.add(brandTitle);

        brand.add(logoRow);
        brand.add(Box.createVerticalStrut(40));

        JLabel uni = new JLabel("ALLAMA IQBAL OPEN UNIVERSITY");
        uni.setFont(new Font("Segoe UI", Font.BOLD, 13));
        uni.setForeground(UIComponents.CHART_PURPLE);
        uni.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.add(uni);

        brand.add(Box.createVerticalStrut(10));

        JLabel title = new JLabel("<html>Smart Hostel<br>Management<br>System</html>");
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.add(title);

        brand.add(Box.createVerticalStrut(15));

        JLabel sub = new JLabel("<html>A modern, professional platform for managing students, rooms, allocations and financial tracking.</html>");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(UIComponents.SIDEBAR_TEXT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.add(sub);

        brand.add(Box.createVerticalGlue());

        // Abstract Wireframe Art at bottom
        UIComponents.SidebarArtPanel art = new UIComponents.SidebarArtPanel();
        art.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.add(art);

        JLabel footer = new JLabel("BSCS • Semester 3 Semester Project");
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footer.setForeground(new Color(120, 125, 140));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.add(footer);

        // Right Form Panel
        JPanel form = new JPanel();
        form.setBackground(Color.WHITE);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(45, 50, 45, 50));

        // Role Selector Toggle (Admin vs Student)
        final boolean[] isStudent = {false};

        JPanel toggleRow = new JPanel(new GridLayout(1, 2, 8, 0));
        toggleRow.setOpaque(false);
        toggleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        toggleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton adminTab = new JButton("🛡️  Admin Login");
        JButton studentTab = new JButton("🎓  Student Portal");

        adminTab.setFont(new Font("Segoe UI", Font.BOLD, 12));
        studentTab.setFont(new Font("Segoe UI", Font.BOLD, 12));
        adminTab.setFocusPainted(false);
        studentTab.setFocusPainted(false);
        adminTab.setCursor(new Cursor(Cursor.HAND_CURSOR));
        studentTab.setCursor(new Cursor(Cursor.HAND_CURSOR));

        form.add(toggleRow);
        form.add(Box.createVerticalStrut(20));

        JLabel welcome = new JLabel("Welcome back");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 26));
        welcome.setForeground(UIComponents.TEXT_DARK);
        form.add(welcome);

        form.add(Box.createVerticalStrut(4));

        JLabel signSubtitle = new JLabel("Please enter your administrator credentials");
        signSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        signSubtitle.setForeground(UIComponents.TEXT_MUTED);
        form.add(signSubtitle);

        form.add(Box.createVerticalStrut(22));

        // Username / Roll No Field
        JLabel uLbl = new JLabel("Username");
        uLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        uLbl.setForeground(UIComponents.TEXT_DARK);
        form.add(uLbl);
        form.add(Box.createVerticalStrut(6));

        JTextField user = new JTextField();
        styleTextInput(user);
        form.add(user);

        form.add(Box.createVerticalStrut(14));

        // Password Field
        JLabel pLbl = new JLabel("Password");
        pLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pLbl.setForeground(UIComponents.TEXT_DARK);
        form.add(pLbl);
        form.add(Box.createVerticalStrut(6));

        JPasswordField pass = new JPasswordField();
        styleTextInput(pass);
        form.add(pass);

        form.add(Box.createVerticalStrut(20));

        // Sign In Button
        JButton btn = createPrimaryButton("Sign In as Admin", UIComponents.SIDEBAR_BG, Color.WHITE);
        btn.setPreferredSize(new Dimension(300, 42));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Register Button for Student
        JButton regBtn = new JButton("New student? Create an Account →");
        regBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        regBtn.setForeground(new Color(91, 33, 182));
        regBtn.setContentAreaFilled(false);
        regBtn.setBorderPainted(false);
        regBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        regBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        regBtn.setVisible(false);

        regBtn.addActionListener(e -> showStudentRegisterDialog(login, user));

        Runnable updateToggleState = () -> {
            if (!isStudent[0]) {
                adminTab.setBackground(UIComponents.SIDEBAR_BG);
                adminTab.setForeground(Color.WHITE);
                studentTab.setBackground(new Color(243, 244, 246));
                studentTab.setForeground(UIComponents.TEXT_DARK);

                welcome.setText("Admin Sign In");
                signSubtitle.setText("Please enter your administrator credentials");
                uLbl.setText("Username");
                btn.setText("Sign In as Admin");
                regBtn.setVisible(false);
            } else {
                studentTab.setBackground(new Color(91, 33, 182));
                studentTab.setForeground(Color.WHITE);
                adminTab.setBackground(new Color(243, 244, 246));
                adminTab.setForeground(UIComponents.TEXT_DARK);

                welcome.setText("Student Sign In");
                signSubtitle.setText("Sign in to book room, lodge complaints & view fee status");
                uLbl.setText("Student Roll No or Email");
                btn.setText("Sign In to Student Portal");
                regBtn.setVisible(true);
            }
            form.revalidate();
            form.repaint();
        };

        adminTab.addActionListener(e -> {
            isStudent[0] = false;
            updateToggleState.run();
        });

        studentTab.addActionListener(e -> {
            isStudent[0] = true;
            updateToggleState.run();
        });

        toggleRow.add(adminTab);
        toggleRow.add(studentTab);
        updateToggleState.run();

        btn.addActionListener(e -> {
            String u = user.getText().trim();
            String p = new String(pass.getPassword());

            if (u.isEmpty() || p.isEmpty()) {
                error("Please enter both credentials.");
                return;
            }

            try {
                if (!isStudent[0]) {
                    if (Auth.login(u, p)) {
                        login.dispose();
                        showDashboardWindow();
                    } else {
                        error("Invalid administrator username or password.");
                    }
                } else {
                    Auth.StudentSession session = Auth.loginStudent(u, p);
                    if (session != null) {
                        login.dispose();
                        StudentPortal.show(session);
                    } else {
                        error("Invalid Student Roll No or Password.\n\nNote: Default password for registered students is '123456'.");
                    }
                }
            } catch (Exception ex) {
                error("Login error: " + ex.getMessage());
            }
        });

        form.add(btn);
        form.add(Box.createVerticalStrut(10));
        form.add(regBtn);
        form.add(Box.createVerticalStrut(14));

        UIComponents.RoundPanel demoHint = new UIComponents.RoundPanel(10, new Color(245, 246, 250), new Color(230, 233, 240));
        demoHint.setLayout(new FlowLayout(FlowLayout.CENTER, 6, 8));
        demoHint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JLabel hintText = new JLabel("Admin: admin/admin123  |  Student Default Pass: 123456");
        hintText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hintText.setForeground(UIComponents.TEXT_MUTED);
        demoHint.add(hintText);
        form.add(demoHint);

        form.add(Box.createVerticalStrut(8));

        UIComponents.RoundPanel mobileLinkBox = new UIComponents.RoundPanel(10, new Color(243, 232, 255), new Color(216, 180, 254));
        mobileLinkBox.setLayout(new FlowLayout(FlowLayout.CENTER, 6, 8));
        mobileLinkBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        mobileLinkBox.setCursor(new Cursor(Cursor.HAND_CURSOR));
        mobileLinkBox.setToolTipText("Open AIOU Hostel Management Portal: " + WebServer.getServerUrl());
        JLabel mobileLink = new JLabel("📱 AIOU Hostel Management (Open Web Portal)");
        mobileLink.setFont(new Font("Segoe UI", Font.BOLD, 11));
        mobileLink.setForeground(new Color(107, 33, 168));
        mobileLinkBox.add(mobileLink);
        mobileLinkBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showMobileLinkDialog(login);
            }
        });
        form.add(mobileLinkBox);

        root.add(brand);
        root.add(form);
        login.setContentPane(root);
        login.setVisible(true);
    }

    // ==========================================
    // MAIN APPLICATION WINDOW
    // ==========================================
    static void showDashboardWindow() {
        frame = new JFrame("AIOU Hostel Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1320, 840);
        frame.setMinimumSize(new Dimension(1100, 720));
        frame.setLocationRelativeTo(null);

        JPanel mainLayout = new JPanel(new BorderLayout());
        mainLayout.setBackground(UIComponents.BG_APP);

        // 1. LEFT SIDEBAR
        JPanel sidebar = createSidebar();
        mainLayout.add(sidebar, BorderLayout.WEST);

        // 2. RIGHT CONTAINER (TopBar + Content)
        JPanel rightArea = new JPanel(new BorderLayout());
        rightArea.setBackground(UIComponents.BG_APP);

        // Top Navigation Bar
        JPanel topBar = createTopBar();
        rightArea.add(topBar, BorderLayout.NORTH);

        // Dynamic Page Content
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(UIComponents.BG_APP);
        contentPanel.setBorder(new EmptyBorder(10, 24, 24, 24));

        rightArea.add(contentPanel, BorderLayout.CENTER);
        mainLayout.add(rightArea, BorderLayout.CENTER);

        frame.setContentPane(mainLayout);
        frame.setVisible(true);

        navigateTo("Dashboard");
    }

    // ==========================================
    // SIDEBAR DESIGN (Matching the exact screenshot)
    // ==========================================
    static JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(UIComponents.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setBorder(new EmptyBorder(22, 14, 20, 14));

        // Top Brand Header
        JPanel topBrand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topBrand.setOpaque(false);

        // Diamond Logo
        UIComponents.RoundPanel diamond = new UIComponents.RoundPanel(7, UIComponents.CHART_LIME);
        diamond.setPreferredSize(new Dimension(22, 22));
        topBrand.add(diamond);

        JLabel logoText = new JLabel("HOSTEL");
        logoText.setFont(new Font("Segoe UI", Font.BOLD, 19));
        logoText.setForeground(Color.WHITE);
        topBrand.add(logoText);

        sidebar.add(topBrand, BorderLayout.NORTH);

        // Center Menu Items
        sidebarNav = new JPanel();
        sidebarNav.setLayout(new BoxLayout(sidebarNav, BoxLayout.Y_AXIS));
        sidebarNav.setOpaque(false);
        sidebarNav.setBorder(new EmptyBorder(25, 0, 0, 0));

        String[][] menuItems = {
                {"Dashboard", "📊"},
                {"Students", "👥"},
                {"Rooms & Beds", "🛏️"},
                {"Allocations", "📑"},
                {"Rent", "💳"},
                {"Complaints", "💬"},
                {"Staff", "👔"},
                {"Visitors", "🚪"}
        };

        for (String[] item : menuItems) {
            String name = item[0];
            String icon = item[1];
            JPanel navBtn = createNavItem(name, icon);
            sidebarNav.add(navBtn);
            sidebarNav.add(Box.createVerticalStrut(6));
        }

        sidebar.add(sidebarNav, BorderLayout.CENTER);

        // Bottom Footer (Abstract geometric wireframe + Logout)
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);

        UIComponents.SidebarArtPanel art = new UIComponents.SidebarArtPanel();
        bottomPanel.add(art);
        bottomPanel.add(Box.createVerticalStrut(10));

        // Logout Button
        JPanel logoutBtn = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        logoutBtn.setOpaque(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel logoutIcon = new JLabel("🚪");
        logoutIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        logoutIcon.setForeground(UIComponents.SIDEBAR_TEXT);

        JLabel logoutLbl = new JLabel("Logout");
        logoutLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        logoutLbl.setForeground(UIComponents.SIDEBAR_TEXT);

        logoutBtn.add(logoutIcon);
        logoutBtn.add(logoutLbl);

        logoutBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                frame.dispose();
                showLogin();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                logoutLbl.setForeground(Color.WHITE);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                logoutLbl.setForeground(UIComponents.SIDEBAR_TEXT);
            }
        });

        bottomPanel.add(logoutBtn);
        sidebar.add(bottomPanel, BorderLayout.SOUTH);

        return sidebar;
    }

    // Creates an individual sidebar nav pill
    static JPanel createNavItem(String name, String icon) {
        boolean isActive = name.equals(currentPage);

        UIComponents.RoundPanel panel = new UIComponents.RoundPanel(
                16,
                isActive ? Color.WHITE : UIComponents.SIDEBAR_BG
        );
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 14, 10));
        panel.setMaximumSize(new Dimension(190, 42));
        panel.setPreferredSize(new Dimension(190, 42));
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        iconLbl.setForeground(isActive ? UIComponents.TEXT_DARK : UIComponents.SIDEBAR_TEXT);

        JLabel textLbl = new JLabel(name);
        textLbl.setFont(new Font("Segoe UI", isActive ? Font.BOLD : Font.PLAIN, 13));
        textLbl.setForeground(isActive ? UIComponents.TEXT_DARK : UIComponents.SIDEBAR_TEXT);

        panel.add(iconLbl);
        panel.add(textLbl);

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                navigateTo(name);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!name.equals(currentPage)) {
                    panel.setBgColor(UIComponents.SIDEBAR_HOVER);
                    textLbl.setForeground(Color.WHITE);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (!name.equals(currentPage)) {
                    panel.setBgColor(UIComponents.SIDEBAR_BG);
                    textLbl.setForeground(UIComponents.SIDEBAR_TEXT);
                }
            }
        });

        return panel;
    }

    // Refresh Sidebar to highlight active menu item
    static void refreshSidebar() {
        if (sidebarNav == null) return;
        sidebarNav.removeAll();

        String[][] menuItems = {
                {"Dashboard", "📊"},
                {"Students", "👥"},
                {"Rooms & Beds", "🛏️"},
                {"Allocations", "📑"},
                {"Rent", "💳"},
                {"Complaints", "💬"},
                {"Staff", "👔"},
                {"Visitors", "🚪"}
        };

        for (String[] item : menuItems) {
            sidebarNav.add(createNavItem(item[0], item[1]));
            sidebarNav.add(Box.createVerticalStrut(6));
        }
        sidebarNav.revalidate();
        sidebarNav.repaint();
    }

    // ==========================================
    // TOP BAR DESIGN (Search, Notification, Profile)
    // ==========================================
    static JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(16, 24, 12, 24));

        // Search Input (Rounded pill shape)
        UIComponents.RoundPanel searchPill = new UIComponents.RoundPanel(20, Color.WHITE, UIComponents.CARD_BORDER);
        searchPill.setLayout(new BorderLayout(8, 0));
        searchPill.setPreferredSize(new Dimension(280, 40));
        searchPill.setBorder(new EmptyBorder(0, 14, 0, 14));

        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        searchIcon.setForeground(UIComponents.TEXT_MUTED);

        JTextField searchField = new JTextField();
        searchField.setBorder(null);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setForeground(UIComponents.TEXT_DARK);
        searchField.setText("Search (or press Up/Down arrows to navigate)...");
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (searchField.getText().startsWith("Search")) searchField.setText("");
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().trim().isEmpty()) searchField.setText("Search (or press Up/Down arrows to navigate)...");
            }
        });

        searchPill.add(searchIcon, BorderLayout.WEST);
        searchPill.add(searchField, BorderLayout.CENTER);
        bar.add(searchPill, BorderLayout.WEST);

        // Right side: Keyboard Hint, Notification Bell & Profile Avatar
        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightTools.setOpaque(false);

        // Keyboard navigation pill
        UIComponents.RoundPanel keyHint = new UIComponents.RoundPanel(12, new Color(240, 242, 248));
        keyHint.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 4));
        JLabel keyLbl = new JLabel("⌨️ Up/Down keys to switch tabs");
        keyLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        keyLbl.setForeground(UIComponents.TEXT_MUTED);
        keyHint.add(keyLbl);
        rightTools.add(keyHint);

        // Notification Bell with red dot
        JPanel bellPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
                g2.drawString("🔔", 4, 24);
                g2.setColor(new Color(239, 68, 68));
                g2.fillOval(20, 4, 8, 8);
                g2.dispose();
            }
        };
        bellPanel.setOpaque(false);
        bellPanel.setPreferredSize(new Dimension(32, 32));
        bellPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        bellPanel.setToolTipText("No unread alerts");
        rightTools.add(bellPanel);

        // Profile Avatar
        UIComponents.RoundPanel avatar = new UIComponents.RoundPanel(16, UIComponents.CHART_PURPLE);
        avatar.setPreferredSize(new Dimension(36, 36));
        avatar.setLayout(new GridBagLayout());
        JLabel avatarText = new JLabel("👤");
        avatarText.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        avatar.add(avatarText);
        avatar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        avatar.setToolTipText("Admin Profile");
        rightTools.add(avatar);

        bar.add(rightTools, BorderLayout.EAST);
        return bar;
    }

    // ==========================================
    // PAGE NAVIGATION
    // ==========================================
    static void navigateTo(String page) {
        currentPage = page;
        refreshSidebar();
        contentPanel.removeAll();

        if ("Dashboard".equalsIgnoreCase(page)) {
            renderDashboard(contentPanel);
        } else {
            renderDataModule(contentPanel, page);
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // ==========================================
    // DASHBOARD RENDERING (Interactive & Clickable)
    // ==========================================
    static void renderDashboard(JPanel panel) {
        JPanel scrollContent = new JPanel();
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setOpaque(false);

        // ------------------------------------------
        // ROW 1: 3 STAT CARDS (ALL CLICKABLE!)
        // ------------------------------------------
        JPanel statRow = new JPanel(new GridLayout(1, 3, 16, 0));
        statRow.setOpaque(false);
        statRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));
        statRow.setPreferredSize(new Dimension(0, 86));

        // Fetch dynamic counts strictly from DB (Defaults 0)
        int totalRooms = 0, vacantRooms = 0, totalCapacity = 0;
        int totalComplaints = 0, receivedComplaints = 0;
        int totalStudents = 0;

        try (Connection c = Database.getConnection(); Statement s = c.createStatement()) {
            ResultSet rRooms = s.executeQuery("SELECT COUNT(*), COALESCE(SUM(CASE WHEN status='Available' THEN 1 ELSE 0 END), 0), COALESCE(SUM(capacity), 0) FROM rooms");
            if (rRooms.next()) {
                totalRooms = rRooms.getInt(1);
                vacantRooms = rRooms.getInt(2);
                totalCapacity = rRooms.getInt(3);
            }
            ResultSet rComp = s.executeQuery("SELECT COUNT(*), COALESCE(SUM(CASE WHEN status='Open' THEN 1 ELSE 0 END), 0) FROM complaints");
            if (rComp.next()) {
                totalComplaints = rComp.getInt(1);
                receivedComplaints = rComp.getInt(2);
            }
            ResultSet rStud = s.executeQuery("SELECT COUNT(*) FROM students WHERE status='Active'");
            if (rStud.next()) {
                totalStudents = rStud.getInt(1);
            }
        } catch (Exception ignored) {}

        // 1. Vacant Rooms Card -> Clicks to "Rooms & Beds"
        JPanel cardVacant = UIComponents.createStatCard(
                "Vacant Rooms",
                vacantRooms + "/" + totalRooms,
                UIComponents.STAT_BLUE,
                UIComponents.STAT_BLUE_TEXT,
                "🚪",
                () -> navigateTo("Rooms & Beds"),
                "Click to view & manage Rooms"
        );

        // 2. Received Complaints Card -> Clicks to "Complaints"
        JPanel cardComp = UIComponents.createStatCard(
                "Received Complaints",
                receivedComplaints + "/" + totalComplaints,
                UIComponents.STAT_LIME,
                UIComponents.STAT_LIME_TEXT,
                "💬",
                () -> navigateTo("Complaints"),
                "Click to view & resolve Complaints"
        );

        // 3. Total Students Card -> Clicks to "Students"
        JPanel cardStudents = UIComponents.createStatCard(
                "Total Students",
                totalStudents + "/" + totalCapacity,
                UIComponents.STAT_PURPLE,
                UIComponents.STAT_PURPLE_TEXT,
                "👥",
                () -> navigateTo("Students"),
                "Click to view all registered Students"
        );

        statRow.add(cardVacant);
        statRow.add(cardComp);
        statRow.add(cardStudents);
        scrollContent.add(statRow);

        scrollContent.add(Box.createVerticalStrut(16));

        // ------------------------------------------
        // ROW 2: Bar Chart & Donut Chart (Clickable)
        // ------------------------------------------
        JPanel middleRow = new JPanel(new GridBagLayout());
        middleRow.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Left: Bar Chart Card
        UIComponents.RoundPanel barCard = new UIComponents.RoundPanel(20, Color.WHITE, UIComponents.CARD_BORDER);
        barCard.setLayout(new BorderLayout(0, 10));
        barCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        barCard.setCursor(new Cursor(Cursor.HAND_CURSOR));
        barCard.setToolTipText("Click to view Students & Allocations records");
        barCard.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { navigateTo("Students"); }
        });

        JPanel barHead = new JPanel(new BorderLayout());
        barHead.setOpaque(false);
        JLabel barTitle = new JLabel("Newly joined vs Left");
        barTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        barTitle.setForeground(UIComponents.TEXT_DARK);
        barHead.add(barTitle, BorderLayout.WEST);

        JPanel barLegend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        barLegend.setOpaque(false);
        barLegend.add(createLegendDot("Newly joined", UIComponents.CHART_LIME));
        barLegend.add(createLegendDot("Left", UIComponents.CHART_PURPLE));
        barHead.add(barLegend, BorderLayout.EAST);

        barCard.add(barHead, BorderLayout.NORTH);
        barCard.add(new UIComponents.BarChartPanel(), BorderLayout.CENTER);

        // Right: Donut Chart Card -> Clicks to "Rent"
        UIComponents.RoundPanel donutCard = new UIComponents.RoundPanel(20, Color.WHITE, UIComponents.CARD_BORDER);
        donutCard.setLayout(new BorderLayout(0, 8));
        donutCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        donutCard.setCursor(new Cursor(Cursor.HAND_CURSOR));
        donutCard.setToolTipText("Click to view Rent / Fee Payments");
        donutCard.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { navigateTo("Rent"); }
        });

        JLabel donutTitle = new JLabel("Rent status");
        donutTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        donutTitle.setForeground(UIComponents.TEXT_DARK);
        donutCard.add(donutTitle, BorderLayout.NORTH);

        donutCard.add(new UIComponents.DonutChartPanel(), BorderLayout.CENTER);

        JPanel donutLegend = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        donutLegend.setOpaque(false);
        donutLegend.add(createLegendDot("Paid", UIComponents.CHART_LIME));
        donutLegend.add(createLegendDot("Unpaid", UIComponents.CHART_PURPLE));
        donutCard.add(donutLegend, BorderLayout.SOUTH);

        gbc.gridx = 0;
        gbc.weightx = 0.68;
        middleRow.add(barCard, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.32;
        gbc.insets = new Insets(0, 16, 0, 0);
        middleRow.add(donutCard, gbc);

        scrollContent.add(middleRow);

        scrollContent.add(Box.createVerticalStrut(16));

        // ------------------------------------------
        // ROW 3: Attendance Table & Revenue Overview
        // ------------------------------------------
        JPanel bottomRow = new JPanel(new GridBagLayout());
        bottomRow.setOpaque(false);
        gbc.insets = new Insets(0, 0, 0, 0);

        // Left: Attendance Table Card -> Clicks to "Students"
        UIComponents.RoundPanel tableCard = new UIComponents.RoundPanel(20, Color.WHITE, UIComponents.CARD_BORDER);
        tableCard.setLayout(new BorderLayout(0, 12));
        tableCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel tableHead = new JPanel(new BorderLayout());
        tableHead.setOpaque(false);
        tableHead.setCursor(new Cursor(Cursor.HAND_CURSOR));
        tableHead.setToolTipText("Click to open Students module");
        tableHead.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { navigateTo("Students"); }
        });

        JLabel tableTitle = new JLabel("Attendance / Recent Students (Click to view all)");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        tableTitle.setForeground(UIComponents.TEXT_DARK);
        tableHead.add(tableTitle, BorderLayout.WEST);

        String dateStr = "< " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")) + " >";
        JLabel dateLbl = new JLabel(dateStr);
        dateLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        dateLbl.setForeground(UIComponents.TEXT_MUTED);
        tableHead.add(dateLbl, BorderLayout.EAST);

        tableCard.add(tableHead, BorderLayout.NORTH);

        String[] cols = {"Name", "Room no", "Check in", "Checkout", "Status"};
        DefaultTableModel tModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        boolean loadedFromDB = false;
        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT s.full_name, COALESCE(r.room_no, 'N/A'), COALESCE(a.start_date, s.admission_date), COALESCE(a.end_date, '--'), s.status " +
                             "FROM students s " +
                             "LEFT JOIN allocations a ON s.id = a.student_id " +
                             "LEFT JOIN rooms r ON a.room_id = r.id " +
                             "LIMIT 5")) {
            while (rs.next()) {
                loadedFromDB = true;
                tModel.addRow(new Object[]{
                        rs.getString(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5)
                });
            }
        } catch (Exception ignored) {}

        if (!loadedFromDB || tModel.getRowCount() == 0) {
            tModel.addRow(new Object[]{"No students registered yet", "-", "-", "-", "None"});
        }

        JTable miniTable = new JTable(tModel);
        UIComponents.styleTable(miniTable);
        miniTable.getColumnModel().getColumn(4).setCellRenderer(new UIComponents.StatusBadgeRenderer());
        miniTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) navigateTo("Students");
            }
        });

        JScrollPane scrollPane = new JScrollPane(miniTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setPreferredSize(new Dimension(500, 160));

        tableCard.add(scrollPane, BorderLayout.CENTER);

        // Right: Revenue Overview Card -> Clicks to "Rent"
        UIComponents.RoundPanel revCard = new UIComponents.RoundPanel(20, UIComponents.DARK_CARD_BG);
        revCard.setLayout(new BorderLayout(0, 10));
        revCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        revCard.setCursor(new Cursor(Cursor.HAND_CURSOR));
        revCard.setToolTipText("Click to view Rent / Payment ledger");
        revCard.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { navigateTo("Rent"); }
        });

        JLabel revTitle = new JLabel("Revenue overview");
        revTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        revTitle.setForeground(Color.WHITE);
        revCard.add(revTitle, BorderLayout.NORTH);

        revCard.add(new UIComponents.GaugeChartPanel(), BorderLayout.CENTER);

        JPanel revLegend = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        revLegend.setOpaque(false);
        revLegend.add(createLegendDot("Income", UIComponents.CHART_LIME, Color.WHITE));
        revLegend.add(createLegendDot("Expenses", UIComponents.CHART_PURPLE, Color.WHITE));
        revCard.add(revLegend, BorderLayout.SOUTH);

        gbc.gridx = 0;
        gbc.weightx = 0.68;
        bottomRow.add(tableCard, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.32;
        gbc.insets = new Insets(0, 16, 0, 0);
        bottomRow.add(revCard, gbc);

        scrollContent.add(bottomRow);

        panel.add(scrollContent, BorderLayout.CENTER);
    }

    // Helper to create small colored legend indicators
    static JPanel createLegendDot(String text, Color dotColor) {
        return createLegendDot(text, dotColor, UIComponents.TEXT_MUTED);
    }

    static JPanel createLegendDot(String text, Color dotColor, Color textColor) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);

        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(dotColor);
                g2.fillOval(0, 1, 9, 9);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(10, 10));

        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(textColor);

        p.add(dot);
        p.add(lbl);
        return p;
    }

    // ==========================================
    // DATA MODULE PAGES (Students, Rooms, Rent, etc.)
    // ==========================================
    static class Module {
        String table, title;
        String[] cols, labels, types;
        Module(String t, String title, String[] c, String[] l, String[] ty) {
            this.table = t;
            this.title = title;
            this.cols = c;
            this.labels = l;
            this.types = ty;
        }
    }

    static Module module(String p) {
        switch (p) {
            case "Students":
            case "Tenants":
                return new Module("students", "Students",
                        new String[]{"student_no", "full_name", "gender", "phone", "email", "department", "guardian_name", "guardian_phone", "address", "admission_date", "status"},
                        new String[]{"Student No", "Full Name", "Gender", "Phone", "Email", "Department", "Guardian Name", "Guardian Phone", "Address", "Admission Date (YYYY-MM-DD)", "Status"},
                        new String[]{"text", "text", "choice:Male,Female,Other", "text", "text", "text", "text", "text", "text", "date", "choice:Active,Inactive"});
            case "Rooms & Beds":
                return new Module("rooms", p,
                        new String[]{"room_no", "room_type", "capacity", "monthly_fee", "floor_no", "status"},
                        new String[]{"Room No", "Room Type", "Capacity", "Monthly Fee", "Floor", "Status"},
                        new String[]{"text", "choice:Single,Double,Triple,Shared", "number", "decimal", "text", "choice:Available,Occupied,Maintenance"});
            case "Allocations":
                return new Module("allocations", p,
                        new String[]{"student_id", "room_id", "bed_no", "start_date", "end_date", "status"},
                        new String[]{"Student (Roll / Name)", "Room", "Bed No", "Start Date (YYYY-MM-DD)", "End Date (YYYY-MM-DD)", "Status"},
                        new String[]{"student_ref", "room_ref", "text", "date", "date", "choice:Active,Completed"});
            case "Rent":
            case "Fee Payments":
                return new Module("payments", "Rent / Payments",
                        new String[]{"student_id", "amount", "payment_date", "month_for", "method", "reference_no", "notes"},
                        new String[]{"Student (Roll / Name)", "Amount (Rs)", "Payment Date (YYYY-MM-DD)", "Month For", "Payment Method", "Reference No", "Notes"},
                        new String[]{"student_ref", "decimal", "date", "text", "choice:Cash,Bank Transfer,Online", "text", "text"});
            case "Complaints":
                return new Module("complaints", p,
                        new String[]{"student_id", "category", "subject", "details", "status"},
                        new String[]{"Student (Roll / Name - Optional)", "Category", "Subject", "Details", "Status"},
                        new String[]{"student_ref_opt", "choice:Maintenance,Cleanliness,Food,Security,Other", "text", "text", "choice:Open,In Progress,Resolved"});
            case "Staff":
                return new Module("staff", p,
                        new String[]{"full_name", "role", "phone", "salary", "joining_date", "status"},
                        new String[]{"Full Name", "Role", "Phone", "Salary", "Joining Date (YYYY-MM-DD)", "Status"},
                        new String[]{"text", "text", "text", "decimal", "date", "choice:Active,Inactive"});
            default:
                return new Module("visitors", "Visitors",
                        new String[]{"visitor_name", "student_id", "relation", "phone", "visit_date", "check_in", "check_out", "purpose"},
                        new String[]{"Visitor Name", "Student (Roll / Name - Optional)", "Relation", "Phone", "Visit Date (YYYY-MM-DD)", "Check In (HH:MM:SS)", "Check Out (HH:MM:SS)", "Purpose"},
                        new String[]{"text", "student_ref_opt", "text", "text", "date", "text", "text", "text"});
        }
    }

    static void renderDataModule(JPanel panel, String page) {
        Module m = module(page);

        UIComponents.RoundPanel card = new UIComponents.RoundPanel(20, Color.WHITE, UIComponents.CARD_BORDER);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header Action Bar
        JPanel head = new JPanel(new BorderLayout(15, 0));
        head.setOpaque(false);

        JLabel titleLbl = new JLabel(m.title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setForeground(UIComponents.TEXT_DARK);
        head.add(titleLbl, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JTextField searchField = new JTextField(15);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIComponents.CARD_BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        JButton addBtn = createPrimaryButton("+ Add Record", UIComponents.SIDEBAR_BG, Color.WHITE);
        JButton editBtn = createSecondaryButton("Edit");
        JButton delBtn = createSecondaryButton("Delete");
        JButton refreshBtn = createSecondaryButton("Refresh");

        actions.add(new JLabel("Search:"));
        actions.add(searchField);
        actions.add(addBtn);
        actions.add(editBtn);
        actions.add(delBtn);
        actions.add(refreshBtn);

        head.add(actions, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        // Table Model & JTable
        String[] headers = new String[m.cols.length + 1];
        headers[0] = "ID";
        System.arraycopy(m.labels, 0, headers, 1, m.cols.length);

        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        UIComponents.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // For Students module: Add a Reset Password button so Admin can reset any student's password
        if ("students".equals(m.table)) {
            JButton resetPassBtn = createSecondaryButton("Reset Password");
            resetPassBtn.setToolTipText("Reset selected student's password to default (123456)");
            resetPassBtn.addActionListener(e -> {
                int r = table.getSelectedRow();
                if (r < 0) {
                    error("Please select a student from the table first.");
                    return;
                }
                int studentId = (int) table.getValueAt(r, 0);
                String rollNo = String.valueOf(table.getValueAt(r, 1));
                String name = String.valueOf(table.getValueAt(r, 2));

                int conf = JOptionPane.showConfirmDialog(frame,
                        "Reset password for " + name + " (" + rollNo + ") to default ('123456')?",
                        "Reset Student Password", JOptionPane.YES_NO_OPTION);
                if (conf == JOptionPane.YES_OPTION) {
                    try (Connection c = Database.getConnection();
                         PreparedStatement p = c.prepareStatement("UPDATE students SET password_hash = ? WHERE id = ?")) {
                        p.setString(1, Auth.hash("123456"));
                        p.setInt(2, studentId);
                        p.executeUpdate();
                        JOptionPane.showMessageDialog(frame,
                                "Password for " + name + " (" + rollNo + ") has been reset to: 123456\n\nStudent can now login with this password.",
                                "Password Reset Successful", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        error("Error resetting password: " + ex.getMessage());
                    }
                }
            });
            actions.add(resetPassBtn);
        }

        int statusColIdx = -1;
        for (int i = 0; i < m.cols.length; i++) {
            if ("status".equalsIgnoreCase(m.cols[i])) {
                statusColIdx = i + 1;
                break;
            }
        }
        if (statusColIdx != -1) {
            table.getColumnModel().getColumn(statusColIdx).setCellRenderer(new UIComponents.StatusBadgeRenderer());
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIComponents.CARD_BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        card.add(scroll, BorderLayout.CENTER);

        // Load DB Data
        Runnable load = () -> {
            model.setRowCount(0);
            String sql;
            if ("allocations".equalsIgnoreCase(m.table)) {
                sql = "SELECT a.id, " +
                      "CONCAT(COALESCE(s.student_no, a.student_id), ' — ', COALESCE(s.full_name, 'Unknown')), " +
                      "CONCAT(COALESCE(r.room_no, a.room_id), ' — ', COALESCE(r.room_type, 'Room')), " +
                      "a.bed_no, a.start_date, a.end_date, a.status " +
                      "FROM allocations a " +
                      "LEFT JOIN students s ON a.student_id = s.id " +
                      "LEFT JOIN rooms r ON a.room_id = r.id " +
                      "ORDER BY a.id DESC";
            } else if ("payments".equalsIgnoreCase(m.table)) {
                sql = "SELECT p.id, " +
                      "CONCAT(COALESCE(s.student_no, p.student_id), ' — ', COALESCE(s.full_name, 'Unknown')), " +
                      "p.amount, p.payment_date, p.month_for, p.method, p.reference_no, p.notes " +
                      "FROM payments p " +
                      "LEFT JOIN students s ON p.student_id = s.id " +
                      "ORDER BY p.id DESC";
            } else if ("complaints".equalsIgnoreCase(m.table)) {
                sql = "SELECT c.id, " +
                      "IF(c.student_id IS NULL, 'General', CONCAT(COALESCE(s.student_no, c.student_id), ' — ', COALESCE(s.full_name, 'Unknown'))), " +
                      "c.category, c.subject, c.details, c.status " +
                      "FROM complaints c " +
                      "LEFT JOIN students s ON c.student_id = s.id " +
                      "ORDER BY c.id DESC";
            } else if ("visitors".equalsIgnoreCase(m.table)) {
                sql = "SELECT v.id, v.visitor_name, " +
                      "IF(v.student_id IS NULL, 'N/A', CONCAT(COALESCE(s.student_no, v.student_id), ' — ', COALESCE(s.full_name, 'Unknown'))), " +
                      "v.relation, v.phone, v.visit_date, v.check_in, v.check_out, v.purpose " +
                      "FROM visitors v " +
                      "LEFT JOIN students s ON v.student_id = s.id " +
                      "ORDER BY v.id DESC";
            } else {
                sql = "SELECT id," + String.join(",", m.cols) + " FROM " + m.table + " ORDER BY id DESC";
            }
            try (Connection c = Database.getConnection(); Statement st = c.createStatement(); ResultSet r = st.executeQuery(sql)) {
                while (r.next()) {
                    Object[] row = new Object[m.cols.length + 1];
                    for (int i = 0; i < row.length; i++) row[i] = r.getObject(i + 1);
                    model.addRow(row);
                }
            } catch (Exception ex) {
                error(ex.getMessage());
            }
        };
        load.run();

        // Button Actions
        addBtn.addActionListener(e -> editDialog(m, null, load));

        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { error("Select a record first."); return; }
            Object[] vals = new Object[m.cols.length + 1];
            for (int i = 0; i < vals.length; i++) vals[i] = model.getValueAt(row, i);
            editDialog(m, vals, load);
        });

        delBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { error("Select a record first."); return; }
            int id = (int) model.getValueAt(row, 0);
            if (JOptionPane.showConfirmDialog(frame, "Delete selected record?", "Confirm Deletion", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement("DELETE FROM " + m.table + " WHERE id=?")) {
                    p.setInt(1, id);
                    p.executeUpdate();
                    load.run();
                } catch (Exception ex) {
                    error("Cannot delete record. It may be linked to other records.\n" + ex.getMessage());
                }
            }
        });

        refreshBtn.addActionListener(e -> load.run());

        searchField.addActionListener(e -> {
            String q = searchField.getText().trim();
            if (q.isEmpty()) { load.run(); return; }
            model.setRowCount(0);
            if ("allocations".equalsIgnoreCase(m.table)) {
                String sql = "SELECT a.id, " +
                             "CONCAT(COALESCE(s.student_no, a.student_id), ' — ', COALESCE(s.full_name, 'Unknown')), " +
                             "CONCAT(COALESCE(r.room_no, a.room_id), ' — ', COALESCE(r.room_type, 'Room')), " +
                             "a.bed_no, a.start_date, a.end_date, a.status " +
                             "FROM allocations a " +
                             "LEFT JOIN students s ON a.student_id = s.id " +
                             "LEFT JOIN rooms r ON a.room_id = r.id " +
                             "WHERE CAST(a.id AS CHAR) LIKE ? OR s.student_no LIKE ? OR s.full_name LIKE ? OR r.room_no LIKE ? OR a.bed_no LIKE ? OR a.status LIKE ? " +
                             "ORDER BY a.id DESC";
                try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
                    for (int k = 1; k <= 6; k++) p.setString(k, "%" + q + "%");
                    try (ResultSet r = p.executeQuery()) {
                        while (r.next()) {
                            Object[] row = new Object[m.cols.length + 1];
                            for (int i = 0; i < row.length; i++) row[i] = r.getObject(i + 1);
                            model.addRow(row);
                        }
                    }
                } catch (Exception ex) {
                    error(ex.getMessage());
                }
                return;
            } else if ("payments".equalsIgnoreCase(m.table)) {
                String sql = "SELECT p.id, " +
                             "CONCAT(COALESCE(s.student_no, p.student_id), ' — ', COALESCE(s.full_name, 'Unknown')), " +
                             "p.amount, p.payment_date, p.month_for, p.method, p.reference_no, p.notes " +
                             "FROM payments p " +
                             "LEFT JOIN students s ON p.student_id = s.id " +
                             "WHERE CAST(p.id AS CHAR) LIKE ? OR s.student_no LIKE ? OR s.full_name LIKE ? OR p.month_for LIKE ? OR p.reference_no LIKE ? " +
                             "ORDER BY p.id DESC";
                try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
                    for (int k = 1; k <= 5; k++) p.setString(k, "%" + q + "%");
                    try (ResultSet r = p.executeQuery()) {
                        while (r.next()) {
                            Object[] row = new Object[m.cols.length + 1];
                            for (int i = 0; i < row.length; i++) row[i] = r.getObject(i + 1);
                            model.addRow(row);
                        }
                    }
                } catch (Exception ex) {
                    error(ex.getMessage());
                }
                return;
            }
            String sql = "SELECT id," + String.join(",", m.cols) + " FROM " + m.table +
                    " WHERE CAST(id AS CHAR) LIKE ? OR CONCAT_WS(' '," + String.join(",", m.cols) + ") LIKE ? ORDER BY id DESC";
            try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
                p.setString(1, "%" + q + "%");
                p.setString(2, "%" + q + "%");
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) {
                        Object[] row = new Object[m.cols.length + 1];
                        for (int i = 0; i < row.length; i++) row[i] = r.getObject(i + 1);
                        model.addRow(row);
                    }
                }
            } catch (Exception ex) {
                error(ex.getMessage());
            }
        });

        panel.add(card, BorderLayout.CENTER);
    }

    // Modal Edit/Add Dialog
    static void editDialog(Module m, Object[] old, Runnable reload) {
        boolean editing = old != null;
        JDialog d = new JDialog(frame, (editing ? "Edit " : "Add ") + m.title, true);
        d.getContentPane().setBackground(Color.WHITE);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(22, 24, 20, 24));

        JLabel titleLbl = new JLabel((editing ? "Edit Record" : "Add New " + m.title));
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLbl.setForeground(UIComponents.TEXT_DARK);
        root.add(titleLbl, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 14, 12));
        form.setOpaque(false);
        JComponent[] inputs = new JComponent[m.cols.length];

        for (int i = 0; i < m.cols.length; i++) {
            JLabel lbl = new JLabel(m.labels[i]);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lbl.setForeground(UIComponents.TEXT_DARK);
            form.add(lbl);

            String type = m.types[i];
            if (type.startsWith("choice:")) {
                JComboBox<String> cb = new JComboBox<>(type.substring(7).split(","));
                cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                inputs[i] = cb;
            } else if ("student_ref".equals(type) || "student_ref_opt".equals(type)) {
                boolean opt = "student_ref_opt".equals(type);
                JComboBox<String> cb = new JComboBox<>(getStudentComboModel(opt));
                cb.setEditable(true);
                cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                inputs[i] = cb;
            } else if ("room_ref".equals(type)) {
                JComboBox<String> cb = new JComboBox<>(getRoomComboModel());
                cb.setEditable(true);
                cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                inputs[i] = cb;
            } else {
                JTextField tf = new JTextField();
                tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                if ("date".equals(type)) tf.setText(LocalDate.now().toString());
                inputs[i] = tf;
            }

            if (editing) {
                Object v = old[i + 1];
                if (inputs[i] instanceof JTextField) {
                    ((JTextField) inputs[i]).setText(v == null ? "" : String.valueOf(v));
                } else if (inputs[i] instanceof JComboBox) {
                    @SuppressWarnings("unchecked")
                    JComboBox<String> cb = (JComboBox<String>) inputs[i];
                    String textVal = v == null ? "" : String.valueOf(v).trim();
                    boolean found = false;
                    for (int j = 0; j < cb.getItemCount(); j++) {
                        String item = cb.getItemAt(j);
                        if (item.equalsIgnoreCase(textVal) || item.startsWith(textVal) || (textVal.length() > 3 && item.contains(textVal))) {
                            cb.setSelectedIndex(j);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        cb.setSelectedItem(textVal);
                    }
                }
            }
            form.add(inputs[i]);
        }
        root.add(form, BorderLayout.CENTER);

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);

        JButton cancel = createSecondaryButton("Cancel");
        JButton save = createPrimaryButton("Save Record", UIComponents.SIDEBAR_BG, Color.WHITE);

        buttons.add(cancel);
        buttons.add(save);
        root.add(buttons, BorderLayout.SOUTH);

        cancel.addActionListener(e -> d.dispose());
        save.addActionListener(e -> {
            String names = String.join(",", m.cols);
            String marks = String.join(",", Collections.nCopies(m.cols.length, "?"));
            String sql = editing ?
                    "UPDATE " + m.table + " SET " + String.join("=?,", m.cols) + "=? WHERE id=?" :
                    "INSERT INTO " + m.table + "(" + names + ") VALUES(" + marks + ")";

            try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
                int k = 1;
                for (int i = 0; i < inputs.length; i++) {
                    String value = inputs[i] instanceof JTextField ?
                            ((JTextField) inputs[i]).getText().trim() :
                            (inputs[i] instanceof JComboBox ?
                                    String.valueOf(((JComboBox<?>) inputs[i]).getSelectedItem()).trim() : "");

                    if ("student_ref".equals(m.types[i]) || "student_ref_opt".equals(m.types[i])) {
                        if (value.isEmpty() || value.startsWith("-- None")) {
                            if ("student_ref_opt".equals(m.types[i])) {
                                p.setNull(k++, Types.INTEGER);
                            } else {
                                throw new IllegalArgumentException("Student is required. Please select or enter a student.");
                            }
                        } else {
                            int sid = resolveStudentId(c, value);
                            p.setInt(k++, sid);
                        }
                    } else if ("room_ref".equals(m.types[i])) {
                        if (value.isEmpty() || value.startsWith("-- None")) {
                            throw new IllegalArgumentException("Room is required. Please select or enter a room.");
                        } else {
                            int rid = resolveRoomId(c, value);
                            p.setInt(k++, rid);
                        }
                    } else if (value.isEmpty()) {
                        p.setNull(k++, Types.NULL);
                    } else if (m.types[i].equals("number")) {
                        p.setInt(k++, Integer.parseInt(value));
                    } else if (m.types[i].equals("decimal")) {
                        p.setBigDecimal(k++, new java.math.BigDecimal(value));
                    } else {
                        p.setString(k++, value);
                    }
                }
                if (editing) p.setInt(k, (int) old[0]);
                p.executeUpdate();

                // If adding a new student, ensure their default password is set to '123456'
                if (!editing && "students".equals(m.table)) {
                    try (PreparedStatement up = c.prepareStatement("UPDATE students SET password_hash = ? WHERE password_hash IS NULL OR password_hash = ''")) {
                        up.setString(1, Auth.hash("123456"));
                        up.executeUpdate();
                    }
                }

                d.dispose();
                reload.run();
            } catch (IllegalArgumentException ex) {
                error(ex.getMessage());
            } catch (SQLException ex) {
                String msg = ex.getMessage();
                if (msg != null && msg.contains("foreign key constraint fails")) {
                    if (msg.contains("student_id") || msg.contains("ibfk_1")) {
                        error("Student was not found in the database.\nPlease check the Student No in Students or select from the dropdown.");
                    } else if (msg.contains("room_id") || msg.contains("ibfk_2")) {
                        error("Room was not found in the database.\nPlease check Room No in Rooms & Beds or select from the dropdown.");
                    } else {
                        error("Referenced record does not exist in the database.\n" + msg);
                    }
                } else {
                    error("Database error occurred:\n" + msg);
                }
            } catch (Exception ex) {
                error("Please check values entered:\n" + ex.getMessage());
            }
        });

        d.setContentPane(root);
        d.pack();
        d.setMinimumSize(new Dimension(580, Math.min(740, 140 + m.cols.length * 42)));
        d.setLocationRelativeTo(frame);
        d.setVisible(true);
    }

    // ==========================================
    // ENTITY RESOLVERS & COMBO HELPERS
    // ==========================================
    static int resolveStudentId(Connection c, String raw) throws SQLException {
        if (raw == null || raw.trim().isEmpty() || raw.startsWith("--")) return 0;
        String val = raw.trim();
        String lookup = val;
        if (lookup.contains("—")) {
            lookup = lookup.split("—")[0].trim();
        } else if (lookup.contains(" - ")) {
            lookup = lookup.split(" - ")[0].trim();
        }

        // 1. Try student_no exact match with lookup (e.g. "0001072084")
        try (PreparedStatement p = c.prepareStatement("SELECT id FROM students WHERE student_no = ?")) {
            p.setString(1, lookup);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        // 2. Try student_no exact match with raw
        try (PreparedStatement p = c.prepareStatement("SELECT id FROM students WHERE student_no = ?")) {
            p.setString(1, val);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        // 3. Try integer ID with lookup or raw
        try {
            int num = Integer.parseInt(lookup);
            try (PreparedStatement p = c.prepareStatement("SELECT id FROM students WHERE id = ?")) {
                p.setInt(1, num);
                try (ResultSet rs = p.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (NumberFormatException ignored) {}
        try {
            int num = Integer.parseInt(val);
            try (PreparedStatement p = c.prepareStatement("SELECT id FROM students WHERE id = ?")) {
                p.setInt(1, num);
                try (ResultSet rs = p.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (NumberFormatException ignored) {}
        // 4. Try matching full_name
        try (PreparedStatement p = c.prepareStatement("SELECT id FROM students WHERE full_name LIKE ? LIMIT 1")) {
            p.setString(1, "%" + lookup + "%");
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        throw new IllegalArgumentException("Student '" + raw + "' was not found in Students records.\nPlease verify the Student No or add the student first.");
    }

    static int resolveRoomId(Connection c, String raw) throws SQLException {
        if (raw == null || raw.trim().isEmpty() || raw.startsWith("--")) return 0;
        String val = raw.trim();
        String lookup = val;
        if (lookup.toLowerCase().startsWith("room ")) {
            lookup = lookup.substring(5).trim();
        }
        if (lookup.contains("(")) {
            lookup = lookup.substring(0, lookup.indexOf('(')).trim();
        }
        if (lookup.contains("—")) {
            lookup = lookup.split("—")[0].trim();
        } else if (lookup.contains(" - ")) {
            lookup = lookup.split(" - ")[0].trim();
        }

        // 1. Try room_no exact match
        try (PreparedStatement p = c.prepareStatement("SELECT id FROM rooms WHERE room_no = ?")) {
            p.setString(1, lookup);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        // 2. Try raw input as room_no
        try (PreparedStatement p = c.prepareStatement("SELECT id FROM rooms WHERE room_no = ?")) {
            p.setString(1, val);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        // 3. Try integer ID
        try {
            int num = Integer.parseInt(lookup);
            try (PreparedStatement p = c.prepareStatement("SELECT id FROM rooms WHERE id = ?")) {
                p.setInt(1, num);
                try (ResultSet rs = p.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (NumberFormatException ignored) {}
        try {
            int num = Integer.parseInt(val);
            try (PreparedStatement p = c.prepareStatement("SELECT id FROM rooms WHERE id = ?")) {
                p.setInt(1, num);
                try (ResultSet rs = p.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (NumberFormatException ignored) {}

        throw new IllegalArgumentException("Room '" + raw + "' was not found in Rooms & Beds records.\nPlease verify the Room No or add the room first.");
    }

    static DefaultComboBoxModel<String> getStudentComboModel(boolean optional) {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        if (optional) {
            model.addElement("-- None (Optional) --");
        }
        try (Connection c = Database.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, student_no, full_name FROM students ORDER BY full_name ASC")) {
            while (rs.next()) {
                String no = rs.getString("student_no");
                String name = rs.getString("full_name");
                String item = (no != null && !no.isEmpty() ? no : "ID:" + rs.getInt("id")) + " — " + name;
                model.addElement(item);
            }
        } catch (Exception ex) {
            System.err.println("Could not load students: " + ex.getMessage());
        }
        return model;
    }

    static DefaultComboBoxModel<String> getRoomComboModel() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        try (Connection c = Database.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, room_no, room_type, status FROM rooms ORDER BY room_no ASC")) {
            while (rs.next()) {
                String no = rs.getString("room_no");
                String type = rs.getString("room_type");
                String status = rs.getString("status");
                String item = "Room " + no + " (" + (type != null ? type : "Standard") + " - " + (status != null ? status : "Available") + ")";
                model.addElement(item);
            }
        } catch (Exception ex) {
            System.err.println("Could not load rooms: " + ex.getMessage());
        }
        return model;
    }

    // ==========================================
    // UI STYLING HELPERS
    // ==========================================
    static void styleTextInput(JTextField f) {
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setPreferredSize(new Dimension(280, 40));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIComponents.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
    }

    static JButton createPrimaryButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(fg);
        b.setBackground(bg);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(9, 18, 9, 18));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JButton createSecondaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(UIComponents.TEXT_DARK);
        b.setBackground(new Color(244, 246, 250));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIComponents.CARD_BORDER),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    static void showStudentRegisterDialog(JFrame parent, JTextField userField) {
        JDialog d = new JDialog(parent, "Register Student Account", true);
        d.setSize(460, 520);
        d.setLocationRelativeTo(parent);
        d.setResizable(false);

        JPanel p = new JPanel(new GridLayout(8, 2, 10, 12));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(22, 24, 22, 24));

        p.add(new JLabel("Roll / Student No *:"));
        JTextField noField = new JTextField();
        p.add(noField);

        p.add(new JLabel("Full Name *:"));
        JTextField nameField = new JTextField();
        p.add(nameField);

        p.add(new JLabel("Department:"));
        JTextField deptField = new JTextField("BSCS");
        p.add(deptField);

        p.add(new JLabel("Gender:"));
        JComboBox<String> genCombo = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        p.add(genCombo);

        p.add(new JLabel("Phone:"));
        JTextField phoneField = new JTextField();
        p.add(phoneField);

        p.add(new JLabel("Email:"));
        JTextField emailField = new JTextField();
        p.add(emailField);

        p.add(new JLabel("Password *:"));
        JPasswordField passField = new JPasswordField();
        p.add(passField);

        JButton cancel = createSecondaryButton("Cancel");
        cancel.addActionListener(e -> d.dispose());
        p.add(cancel);

        JButton submit = createPrimaryButton("Create Account", new Color(91, 33, 182), Color.WHITE);
        submit.addActionListener(e -> {
            String no = noField.getText().trim();
            String name = nameField.getText().trim();
            String dept = deptField.getText().trim();
            String gen = (String) genCombo.getSelectedItem();
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();
            String pass = new String(passField.getPassword());

            if (no.isEmpty() || name.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(d, "Roll No, Full Name and Password are required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                Auth.registerStudent(no, name, email, phone, dept, gen, pass);
                d.dispose();
                JOptionPane.showMessageDialog(parent, "🎉 Account created successfully!\nYou can now sign in with Roll No: " + no, "Account Created", JOptionPane.INFORMATION_MESSAGE);
                userField.setText(no);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(d, ex.getMessage(), "Registration Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        p.add(submit);

        d.setContentPane(p);
        d.setVisible(true);
    }

    static void showMobileLinkDialog(JFrame parent) {
        JDialog d = new JDialog(parent, "AIOU Hostel Management — Mobile & Laptop Portal", true);
        d.setSize(540, 480);
        d.setLocationRelativeTo(parent);
        d.setResizable(false);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(24, 26, 24, 26));

        JLabel title = new JLabel("📱 Connect from Mobile Phone or Laptop");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(UIComponents.TEXT_DARK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(title);
        p.add(Box.createVerticalStrut(4));

        JLabel sub = new JLabel("Mobile phone par open kar ke 'Install App' par click karein.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(UIComponents.TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(sub);
        p.add(Box.createVerticalStrut(18));

        String pubUrl = WebServer.getPublicUrl();
        String localUrl = WebServer.getServerUrl();

        // Box 1: Online Mobile Link (HTTPS)
        UIComponents.RoundPanel box1 = new UIComponents.RoundPanel(14, new Color(245, 243, 255), new Color(221, 214, 254));
        box1.setLayout(new BorderLayout(10, 8));
        box1.setBorder(new EmptyBorder(12, 14, 12, 14));
        box1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));

        JLabel l1 = new JLabel("<html><b>🌐 Online Mobile Link (Recommended - Any Network / 4G):</b><br><span style='color:#7C3AED; font-size:12px;'>" + pubUrl + "</span></html>");
        box1.add(l1, BorderLayout.CENTER);

        JPanel btnRow1 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnRow1.setOpaque(false);
        JButton copy1 = createSecondaryButton("Copy Link");
        copy1.addActionListener(e -> {
            java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(pubUrl);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
            JOptionPane.showMessageDialog(d, "Link copied to clipboard!\nSend this to students via WhatsApp or SMS:\n\n" + pubUrl, "Copied", JOptionPane.INFORMATION_MESSAGE);
        });
        JButton open1 = createPrimaryButton("Open in Browser", new Color(124, 58, 237), Color.WHITE);
        open1.addActionListener(e -> {
            try { Desktop.getDesktop().browse(new URI(pubUrl)); } catch (Exception ignored) {}
        });
        btnRow1.add(copy1);
        btnRow1.add(open1);
        box1.add(btnRow1, BorderLayout.SOUTH);
        p.add(box1);

        p.add(Box.createVerticalStrut(12));

        // Box 2: Local Wi-Fi Link
        UIComponents.RoundPanel box2 = new UIComponents.RoundPanel(14, new Color(240, 249, 255), new Color(186, 230, 253));
        box2.setLayout(new BorderLayout(10, 8));
        box2.setBorder(new EmptyBorder(12, 14, 12, 14));
        box2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));

        JLabel l2 = new JLabel("<html><b>🏠 Same Wi-Fi Link (Direct LAN):</b><br><span style='color:#0284C7; font-size:12px;'>" + localUrl + "</span></html>");
        box2.add(l2, BorderLayout.CENTER);

        JPanel btnRow2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnRow2.setOpaque(false);
        JButton copy2 = createSecondaryButton("Copy Link");
        copy2.addActionListener(e -> {
            java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(localUrl);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
            JOptionPane.showMessageDialog(d, "Local link copied to clipboard!\n\n" + localUrl, "Copied", JOptionPane.INFORMATION_MESSAGE);
        });
        JButton open2 = createPrimaryButton("Open in Browser", new Color(2, 132, 199), Color.WHITE);
        open2.addActionListener(e -> {
            try { Desktop.getDesktop().browse(new URI(localUrl)); } catch (Exception ignored) {}
        });
        btnRow2.add(copy2);
        btnRow2.add(open2);
        box2.add(btnRow2, BorderLayout.SOUTH);
        p.add(box2);

        p.add(Box.createVerticalStrut(14));

        JLabel hint = new JLabel("<html><center>Students apne mobile ke Chrome browser mein yeh link open karein ge to unhein <b>'Install App'</b> ka option mil jaye ga.<br>Laptop users <b>" + pubUrl + "/download</b> se Windows app download kar saktay hain.</center></html>");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(UIComponents.TEXT_MUTED);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(hint);

        d.setContentPane(p);
        d.setVisible(true);
    }

    static void error(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Hostel Management System", JOptionPane.ERROR_MESSAGE);
    }
}
