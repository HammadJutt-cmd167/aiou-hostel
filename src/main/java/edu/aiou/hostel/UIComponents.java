package edu.aiou.hostel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;

public class UIComponents {

    // Color Palette inspired by the modern SaaS dashboard design
    public static final Color BG_APP = new Color(245, 246, 250);
    public static final Color SIDEBAR_BG = new Color(24, 24, 28);
    public static final Color SIDEBAR_HOVER = new Color(38, 38, 44);
    public static final Color SIDEBAR_TEXT = new Color(165, 168, 180);
    public static final Color CARD_BG = Color.WHITE;
    public static final Color CARD_BORDER = new Color(236, 239, 244);
    public static final Color TEXT_DARK = new Color(24, 25, 30);
    public static final Color TEXT_MUTED = new Color(125, 130, 145);

    // Pastel top stat card colors
    public static final Color STAT_BLUE = new Color(186, 230, 253);       // #BAE6FD (Vacant Rooms)
    public static final Color STAT_BLUE_TEXT = new Color(14, 116, 144);
    public static final Color STAT_LIME = new Color(226, 249, 146);       // #E2F992 (Complaints)
    public static final Color STAT_LIME_TEXT = new Color(63, 98, 18);
    public static final Color STAT_PURPLE = new Color(216, 196, 255);     // #D8C4FF (Total Students)
    public static final Color STAT_PURPLE_TEXT = new Color(91, 33, 182);

    // Vibrant chart colors
    public static final Color CHART_LIME = new Color(219, 247, 107);      // #DBF76B
    public static final Color CHART_PURPLE = new Color(196, 175, 250);    // #C4AFFA
    public static final Color DARK_CARD_BG = new Color(26, 26, 30);       // #1A1A1E

    // Rounded Panel with interactive hover support
    public static class RoundPanel extends JPanel {
        private int radius;
        private Color bgColor;
        private Color borderColor;
        private Color originalBgColor;

        public RoundPanel(int radius, Color bgColor) {
            this(radius, bgColor, null);
        }

        public RoundPanel(int radius, Color bgColor, Color borderColor) {
            this.radius = radius;
            this.bgColor = bgColor;
            this.originalBgColor = bgColor;
            this.borderColor = borderColor;
            setOpaque(false);
        }

        public void setBgColor(Color c) { this.bgColor = c; repaint(); }
        public void setBorderColor(Color c) { this.borderColor = c; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (bgColor != null) {
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            }
            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Top Stat Card with click & hover
    public static JPanel createStatCard(String title, String value, Color bgColor, Color textColor, String iconSymbol, Runnable onClick, String tooltip) {
        RoundPanel card = new RoundPanel(22, bgColor);
        card.setLayout(new BorderLayout(14, 0));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (tooltip != null) card.setToolTipText(tooltip);

        RoundPanel iconBox = new RoundPanel(14, Color.WHITE);
        iconBox.setPreferredSize(new Dimension(46, 46));
        iconBox.setLayout(new GridBagLayout());
        JLabel iconLbl = new JLabel(iconSymbol);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        iconBox.add(iconLbl);

        JPanel textCol = new JPanel(new GridLayout(2, 1, 0, 4));
        textCol.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLbl.setForeground(textColor);

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valLbl.setForeground(TEXT_DARK);

        textCol.add(titleLbl);
        textCol.add(valLbl);

        card.add(iconBox, BorderLayout.WEST);
        card.add(textCol, BorderLayout.CENTER);

        // Hover & Click listeners
        MouseAdapter clickHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (onClick != null) onClick.run();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorderColor(new Color(60, 65, 80, 100));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorderColor(null);
            }
        };

        card.addMouseListener(clickHandler);
        iconBox.addMouseListener(clickHandler);
        iconLbl.addMouseListener(clickHandler);
        titleLbl.addMouseListener(clickHandler);
        valLbl.addMouseListener(clickHandler);

        return card;
    }

    // Custom Dual Bar Chart ("Newly joined vs Left")
    public static class BarChartPanel extends JComponent {
        private final String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug"};
        private int[] joined = new int[8];
        private int[] left = new int[8];
        private int maxVal = 10;

        public BarChartPanel() {
            setPreferredSize(new Dimension(500, 200));
            setMinimumSize(new Dimension(350, 160));
        }

        public void setValues(int[] j, int[] l) {
            if (j != null && j.length == 8) this.joined = j;
            if (l != null && l.length == 8) this.left = l;
            int max = 5;
            for (int v : joined) if (v > max) max = v;
            for (int v : left) if (v > max) max = v;
            this.maxVal = max + (5 - (max % 5));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int leftMargin = 32;
            int rightMargin = 16;
            int bottomMargin = 28;
            int topMargin = 15;

            int chartWidth = w - leftMargin - rightMargin;
            int chartHeight = h - topMargin - bottomMargin;

            // Draw horizontal grid lines & Y-axis labels
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            int steps = 5;
            for (int i = 0; i <= steps; i++) {
                int yVal = i * (maxVal / steps);
                int y = topMargin + chartHeight - (int) ((double) yVal / maxVal * chartHeight);

                g2.setColor(new Color(180, 185, 195));
                g2.drawString(String.valueOf(yVal), 8, y + 4);

                g2.setColor(new Color(240, 243, 248));
                g2.drawLine(leftMargin, y, w - rightMargin, y);
            }

            // Draw Dual Bars
            int n = months.length;
            double slotWidth = (double) chartWidth / n;
            int barWidth = Math.max(5, (int) (slotWidth * 0.18));
            int gap = 3;

            for (int i = 0; i < n; i++) {
                double centerX = leftMargin + i * slotWidth + slotWidth / 2.0;

                // Newly Joined Bar (Lime)
                int hJoined = (int) ((double) joined[i] / maxVal * chartHeight);
                int yJoined = topMargin + chartHeight - hJoined;
                int xJoined = (int) (centerX - barWidth - gap / 2.0);

                if (hJoined > 0) {
                    g2.setColor(CHART_LIME);
                    g2.fill(new RoundRectangle2D.Float(xJoined, yJoined, barWidth, hJoined, 5, 5));
                }

                // Left Bar (Purple)
                int hLeft = (int) ((double) left[i] / maxVal * chartHeight);
                int yLeft = topMargin + chartHeight - hLeft;
                int xLeft = (int) (centerX + gap / 2.0);

                if (hLeft > 0) {
                    g2.setColor(CHART_PURPLE);
                    g2.fill(new RoundRectangle2D.Float(xLeft, yLeft, barWidth, hLeft, 5, 5));
                }

                // Month Label
                g2.setColor(new Color(140, 145, 155));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                FontMetrics fm = g2.getFontMetrics();
                int labelWidth = fm.stringWidth(months[i]);
                g2.drawString(months[i], (int) (centerX - labelWidth / 2.0), h - 8);
            }

            g2.dispose();
        }
    }

    // Custom Donut Chart ("Rent status")
    public static class DonutChartPanel extends JComponent {
        private int paidPercent = 0;
        private int unpaidPercent = 0;

        public DonutChartPanel() {
            setPreferredSize(new Dimension(180, 180));
            setMinimumSize(new Dimension(140, 140));
        }

        public void setValues(int paid, int unpaid) {
            this.paidPercent = paid;
            this.unpaidPercent = unpaid;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight()) - 24;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            int strokeWidth = 24;

            int inset = strokeWidth / 2;
            Rectangle2D.Float arcRect = new Rectangle2D.Float(x + inset, y + inset, size - strokeWidth, size - strokeWidth);
            g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));

            if (paidPercent == 0 && unpaidPercent == 0) {
                // Neutral empty ring when 0
                g2.setColor(new Color(235, 238, 245));
                g2.draw(new Arc2D.Float(arcRect, 0, 360, Arc2D.OPEN));

                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                g2.setColor(new Color(140, 145, 155));
                String emptyMsg = "0 Records";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(emptyMsg, x + (size - fm.stringWidth(emptyMsg)) / 2, y + size / 2 + 5);
            } else {
                float paidAngle = (float) (paidPercent / 100.0 * 360.0);
                float unpaidAngle = 360.0f - paidAngle;

                // Draw Paid segment (Lime)
                g2.setColor(CHART_LIME);
                g2.draw(new Arc2D.Float(arcRect, 90, -paidAngle, Arc2D.OPEN));

                // Draw Unpaid segment (Purple)
                g2.setColor(CHART_PURPLE);
                g2.draw(new Arc2D.Float(arcRect, 90 - paidAngle, -unpaidAngle, Arc2D.OPEN));

                // Draw percentages on segments
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.setColor(new Color(60, 65, 75));
                g2.drawString(paidPercent + "%", x + 10, y + size / 2 + 5);
                g2.drawString(unpaidPercent + "%", x + size - 32, y + 36);
            }

            g2.dispose();
        }
    }

    // Custom Gauge / Semi-Circle Arc Chart ("Revenue overview")
    public static class GaugeChartPanel extends JComponent {
        private int incomePercent = 0;
        private int expensePercent = 0;

        public GaugeChartPanel() {
            this(0, 0);
        }

        public GaugeChartPanel(int income, int expense) {
            this.incomePercent = income;
            this.expensePercent = expense;
            setPreferredSize(new Dimension(200, 130));
        }

        public void setValues(int inc, int exp) {
            this.incomePercent = inc;
            this.expensePercent = exp;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int diameter = Math.min(w - 30, (h - 20) * 2);
            int x = (w - diameter) / 2;
            int y = 15;
            int strokeWidth = 22;

            int inset = strokeWidth / 2;
            Rectangle2D.Float arcRect = new Rectangle2D.Float(x + inset, y + inset, diameter - strokeWidth, diameter - strokeWidth);

            float totalArc = 180.0f;
            g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Background arc
            g2.setColor(new Color(45, 45, 55));
            g2.draw(new Arc2D.Float(arcRect, 0, 180, Arc2D.OPEN));

            if (incomePercent > 0 || expensePercent > 0) {
                float incomeAngle = totalArc * (incomePercent / 100.0f);
                float expenseAngle = totalArc - incomeAngle;

                // Expense Arc (Purple) on right
                g2.setColor(CHART_PURPLE);
                g2.draw(new Arc2D.Float(arcRect, 0, expenseAngle, Arc2D.OPEN));

                // Income Arc (Lime) on left
                g2.setColor(CHART_LIME);
                g2.draw(new Arc2D.Float(arcRect, 180, -incomeAngle, Arc2D.OPEN));
            }

            // Center decorative circle
            int centerRadius = 22;
            int centerX = w / 2;
            int centerY = y + diameter / 2;
            g2.setColor(new Color(40, 40, 50));
            g2.fillOval(centerX - centerRadius, centerY - centerRadius, centerRadius * 2, centerRadius * 2);

            g2.setColor(CHART_PURPLE);
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(centerX - centerRadius, centerY - centerRadius, centerRadius * 2, centerRadius * 2);

            g2.setColor(CHART_LIME);
            g2.fillOval(centerX - 5, centerY - 5, 10, 10);

            // Percentage labels under the gauge ends
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2.setColor(new Color(200, 205, 215));
            g2.drawString(incomePercent + "%", x + 10, centerY + 18);
            g2.drawString(expensePercent + "%", x + diameter - 32, centerY + 18);

            g2.dispose();
        }
    }

    // Abstract geometric wireframe art for bottom of sidebar
    public static class SidebarArtPanel extends JComponent {
        public SidebarArtPanel() {
            setPreferredSize(new Dimension(180, 100));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setStroke(new BasicStroke(1.2f));

            // Purple overlapping angled polygon
            g2.setColor(new Color(196, 175, 250, 90));
            int[] px = {25, 80, 130, 75};
            int[] py = {30, 15, 65, 80};
            g2.drawPolygon(px, py, 4);

            // Lime overlapping angled polygon
            g2.setColor(new Color(219, 247, 107, 100));
            int[] lx = {15, 70, 115, 60};
            int[] ly = {50, 35, 85, 95};
            g2.drawPolygon(lx, ly, 4);

            g2.dispose();
        }
    }

    // Custom Flat Pill Table Cell Renderer for Status
    public static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String text = value == null ? "" : value.toString();
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            panel.setOpaque(true);
            panel.setBackground(isSelected ? table.getSelectionBackground() : (row % 2 == 0 ? Color.WHITE : new Color(250, 251, 253)));

            RoundPanel badge;
            JLabel label = new JLabel(text);
            label.setFont(new Font("Segoe UI", Font.BOLD, 11));

            if ("Active".equalsIgnoreCase(text) || "Available".equalsIgnoreCase(text) || "Resolved".equalsIgnoreCase(text) || "Paid".equalsIgnoreCase(text)) {
                badge = new RoundPanel(12, new Color(220, 252, 231)); // Soft green
                label.setForeground(new Color(22, 101, 52));
            } else if ("Inactive".equalsIgnoreCase(text) || "Occupied".equalsIgnoreCase(text) || "Open".equalsIgnoreCase(text) || "Unpaid".equalsIgnoreCase(text)) {
                badge = new RoundPanel(12, new Color(254, 226, 226)); // Soft red
                label.setForeground(new Color(153, 27, 27));
            } else {
                badge = new RoundPanel(12, new Color(243, 244, 246)); // Neutral gray
                label.setForeground(new Color(75, 85, 99));
            }

            badge.setBorder(new EmptyBorder(3, 10, 3, 10));
            badge.add(label);
            panel.add(badge);
            return panel;
        }
    }

    // Custom Styled Table Header and Rows
    public static void styleTable(JTable table) {
        table.setRowHeight(38);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setGridColor(new Color(242, 244, 248));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setSelectionBackground(new Color(235, 240, 252));
        table.setSelectionForeground(TEXT_DARK);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(Color.WHITE);
        header.setForeground(new Color(130, 135, 148));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 234, 242)));
        header.setPreferredSize(new Dimension(0, 38));
    }
}
