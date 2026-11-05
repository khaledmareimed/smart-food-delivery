package com.fooddelivery.presentation;

import com.fooddelivery.domain.DriverStatus;
import com.fooddelivery.domain.OrderStatus;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Colors, fonts and small helpers so all screens look the same.
 */
final class Theme {

    static final Color PRIMARY = new Color(0xE8, 0x5D, 0x24);
    static final Color PRIMARY_DARK = new Color(0xB9, 0x40, 0x10);
    static final Color BACKGROUND = new Color(0xF4, 0xF5, 0xF7);
    static final Color CARD = Color.WHITE;
    static final Color TEXT = new Color(0x1F, 0x23, 0x2B);
    static final Color MUTED = new Color(0x6B, 0x72, 0x80);
    static final Color BORDER = new Color(0xE2, 0xE5, 0xEA);
    static final Color GREEN = new Color(0x16, 0xA3, 0x4A);
    static final Color BLUE = new Color(0x25, 0x63, 0xEB);
    static final Color AMBER = new Color(0xD9, 0x77, 0x06);
    static final Color RED = new Color(0xDC, 0x26, 0x26);
    static final Color PURPLE = new Color(0x7C, 0x3A, 0xED);
    static final Color TEAL = new Color(0x0D, 0x94, 0x88);

    static final Font TITLE = new Font("SansSerif", Font.BOLD, 20);
    static final Font SUBTITLE = new Font("SansSerif", Font.BOLD, 15);
    static final Font BODY = new Font("SansSerif", Font.PLAIN, 13);
    static final Font SMALL = new Font("SansSerif", Font.PLAIN, 12);

    private Theme() {
    }

    static void install() {
        com.formdev.flatlaf.FlatLaf.setGlobalExtraDefaults(
                java.util.Collections.singletonMap("@accentColor", "#E85D24"));
        FlatLightLaf.setup();
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.accentColor", PRIMARY);
        UIManager.put("Component.focusColor", PRIMARY);
        UIManager.put("TabbedPane.underlineColor", PRIMARY);
        UIManager.put("TabbedPane.selectedForeground", PRIMARY_DARK);
        UIManager.put("TabbedPane.tabHeight", 38);
        UIManager.put("TabbedPane.font", SUBTITLE);
        UIManager.put("Table.rowHeight", 32);
        UIManager.put("Table.selectionBackground", new Color(0xFD, 0xE7, 0xDC));
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(BORDER));
        UIManager.put("defaultFont", BODY);
    }

    // ---------- layout pieces ----------

    /** Orange bar at the top of every screen with the user name and a logout button. */
    static JPanel header(String title, String subtitle, Runnable onLogout) {
        JPanel bar = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, PRIMARY, getWidth(), 0, PRIMARY_DARK));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        bar.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel t = new JLabel("Smart Food Delivery  |  " + title);
        t.setFont(TITLE);
        t.setForeground(Color.WHITE);
        JLabel s = new JLabel(subtitle);
        s.setFont(BODY);
        s.setForeground(new Color(255, 255, 255, 220));
        text.add(t);
        text.add(Box.createVerticalStrut(2));
        text.add(s);
        bar.add(text, BorderLayout.WEST);

        JButton logout = new JButton("Log out");
        logout.setFocusPainted(false);
        logout.setForeground(PRIMARY_DARK);
        logout.setBackground(Color.WHITE);
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.addActionListener(e -> onLogout.run());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        right.setOpaque(false);
        right.add(logout);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    /** White rounded box with a title. Put content in the CENTER. */
    static JPanel card(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(CARD);
        Border line = BorderFactory.createLineBorder(BORDER, 1, true);
        card.setBorder(BorderFactory.createCompoundBorder(line,
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        if (title != null) {
            JLabel l = new JLabel(title);
            l.setFont(SUBTITLE);
            l.setForeground(TEXT);
            card.add(l, BorderLayout.NORTH);
        }
        return card;
    }

    static JPanel page() {
        JPanel p = new JPanel(new BorderLayout(14, 14));
        p.setBackground(BACKGROUND);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        return p;
    }

    static JPanel row(Component... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        for (Component c : items) {
            p.add(c);
        }
        return p;
    }

    /** Scroll pane for an HTML label: white, no border, wraps instead of scrolling sideways. */
    static javax.swing.JScrollPane htmlScroll(JLabel label) {
        label.setOpaque(true);
        label.setBackground(CARD);
        label.setVerticalAlignment(SwingConstants.TOP);
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(label,
                javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(CARD);
        return scroll;
    }

    /** Puts a component under the card title (the title uses NORTH). */
    static JPanel below(Component top, Component center) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.add(top, BorderLayout.NORTH);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        l.setFont(SMALL);
        return l;
    }

    /** Small box with a big number, used on the manager dashboard. */
    static JPanel statCard(String label, JLabel value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                        BorderFactory.createEmptyBorder(10, 14, 10, 14))));
        value.setFont(new Font("SansSerif", Font.BOLD, 22));
        value.setForeground(TEXT);
        card.add(muted(label.toUpperCase()), BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    // ---------- buttons ----------

    static JButton primary(String text) {
        JButton b = new JButton(text);
        b.setBackground(PRIMARY);
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JButton secondary(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JButton danger(String text) {
        JButton b = secondary(text);
        b.setForeground(RED);
        return b;
    }

    static void bigger(JComponent c) {
        c.setPreferredSize(new Dimension(c.getPreferredSize().width + 20, 40));
    }

    // ---------- tables ----------

    static void styleTable(JTable table) {
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setRowHeight(32);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setDefaultRenderer(Object.class, new CellRenderer());
        JTableHeader header = table.getTableHeader();
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        header.setReorderingAllowed(false);
    }

    /** Renders statuses as colored pills and money aligned to the right. */
    static class CellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        private Color pill;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focus, int row, int column) {
            pill = statusColor(value);
            Object shown = pill != null ? nice(value) : value;
            if (value instanceof BigDecimal) {
                shown = money((BigDecimal) value);
            }
            super.getTableCellRendererComponent(table, shown, selected, false, row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            setHorizontalAlignment(value instanceof BigDecimal || value instanceof Number
                    ? SwingConstants.RIGHT : SwingConstants.LEFT);
            if (pill != null) {
                setForeground(pill.darker());
                setFont(getFont().deriveFont(Font.BOLD));
            } else if (!selected) {
                setForeground(TEXT);
            }
            if (!selected) {
                setBackground(row % 2 == 0 ? CARD : new Color(0xFA, 0xFB, 0xFC));
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (pill != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(getBackground());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getFontMetrics(getFont()).stringWidth(getText()) + 20;
                g2.setColor(new Color(pill.getRed(), pill.getGreen(), pill.getBlue(), 40));
                g2.fillRoundRect(4, 5, w, getHeight() - 10, 14, 14);
                g2.dispose();
                setOpaque(false);
                super.paintComponent(g);
                setOpaque(true);
            } else {
                super.paintComponent(g);
            }
        }
    }

    // ---------- text helpers ----------

    static Color statusColor(Object value) {
        if (value == OrderStatus.PENDING) {
            return AMBER;
        } else if (value == OrderStatus.CONFIRMED) {
            return BLUE;
        } else if (value == OrderStatus.PREPARING) {
            return PURPLE;
        } else if (value == OrderStatus.READY) {
            return TEAL;
        } else if (value == OrderStatus.OUT_FOR_DELIVERY) {
            return PRIMARY;
        } else if (value == OrderStatus.DELIVERED || value == DriverStatus.AVAILABLE) {
            return GREEN;
        } else if (value == OrderStatus.CANCELLED) {
            return RED;
        } else if (value == DriverStatus.BUSY) {
            return AMBER;
        } else if ("PAID".equals(value)) {
            return GREEN;
        } else if ("UNPAID".equals(value)) {
            return MUTED;
        }
        return null;
    }

    /** OUT_FOR_DELIVERY becomes "Out for delivery". */
    static String nice(Object value) {
        String s = String.valueOf(value).replace('_', ' ').toLowerCase();
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static String money(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + " NIS";
    }

    static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
