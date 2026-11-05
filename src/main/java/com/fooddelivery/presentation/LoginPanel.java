package com.fooddelivery.presentation;

import com.fooddelivery.domain.User;
import com.fooddelivery.exception.AuthenticationException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

/**
 * Login screen: brand panel on the left, login form on the right.
 */
public class LoginPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private JTextField usernameField = new JTextField(18);
    private JPasswordField passwordField = new JPasswordField(18);
    private JLabel errorLabel = new JLabel(" ");

    public LoginPanel(final MainFrame frame) {
        setLayout(new GridLayout(1, 2));
        add(brandPanel());

        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(Theme.BACKGROUND);
        JPanel form = Theme.card(null);
        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Welcome back");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        fields.add(left(title));
        fields.add(left(Theme.muted("Sign in to continue")));
        fields.add(Box.createVerticalStrut(18));
        fields.add(left(new JLabel("Username")));
        fields.add(Box.createVerticalStrut(4));
        usernameField.putClientProperty("JTextField.placeholderText", "e.g. alice");
        fields.add(left(sized(usernameField)));
        fields.add(Box.createVerticalStrut(12));
        fields.add(left(new JLabel("Password")));
        fields.add(Box.createVerticalStrut(4));
        passwordField.putClientProperty("JComponent.roundRect", false);
        fields.add(left(sized(passwordField)));
        fields.add(Box.createVerticalStrut(16));

        final JButton loginButton = Theme.primary("Log in");
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        loginButton.setPreferredSize(new Dimension(300, 40));
        fields.add(left(loginButton));
        errorLabel.setForeground(Theme.RED);
        fields.add(Box.createVerticalStrut(8));
        fields.add(left(errorLabel));
        fields.add(Box.createVerticalStrut(14));
        fields.add(left(Theme.muted("Demo accounts (click to fill):")));
        fields.add(Box.createVerticalStrut(6));
        JPanel demo = new JPanel(new GridLayout(1, 3, 6, 0));
        demo.setOpaque(false);
        demo.add(demoButton("Customer", "alice", "alice123"));
        demo.add(demoButton("Manager", "bob", "manager123"));
        demo.add(demoButton("Driver", "lina", "driver123"));
        demo.setMaximumSize(new Dimension(320, 34));
        fields.add(left(demo));
        form.add(fields);
        right.add(form);
        add(right);

        loginButton.addActionListener(e -> {
            try {
                User user = frame.authService.login(usernameField.getText().trim(),
                        new String(passwordField.getPassword()));
                frame.loggedIn(user);
            } catch (AuthenticationException ex) {
                errorLabel.setText(ex.getMessage());
                passwordField.setText("");
            }
        });
        passwordField.addActionListener(e -> loginButton.doClick());
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
    }

    private JButton demoButton(String label, final String user, final String pass) {
        JButton b = Theme.secondary(label);
        b.addActionListener(e -> {
            usernameField.setText(user);
            passwordField.setText(pass);
            errorLabel.setText(" ");
        });
        return b;
    }

    private static JComponentHolder sized(java.awt.Component c) {
        return new JComponentHolder(c);
    }

    private static Component left(Component c) {
        if (c instanceof javax.swing.JComponent) {
            ((javax.swing.JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        return c;
    }

    /** Keeps text fields at a nice fixed height inside the BoxLayout. */
    private static class JComponentHolder extends JPanel {
        private static final long serialVersionUID = 1L;

        JComponentHolder(Component c) {
            super(new java.awt.BorderLayout());
            setOpaque(false);
            add(c);
            setPreferredSize(new Dimension(320, 38));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        }
    }

    private JPanel brandPanel() {
        JPanel brand = new JPanel(new GridBagLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, Theme.PRIMARY, getWidth(), getHeight(),
                        Theme.PRIMARY_DARK));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel name = new JLabel("<html>Smart Food<br>Delivery</html>");
        name.setFont(new Font("SansSerif", Font.BOLD, 40));
        name.setForeground(Color.WHITE);
        text.add(name);
        text.add(Box.createVerticalStrut(14));
        JLabel tag = new JLabel("Hot food from local restaurants, delivered.");
        tag.setFont(new Font("SansSerif", Font.PLAIN, 16));
        tag.setForeground(new Color(255, 255, 255, 230));
        text.add(tag);
        text.add(Box.createVerticalStrut(30));
        String[] points = {"Browse open restaurants and menus", "Track your order step by step",
            "Smart driver matching by distance and vehicle", "Pay cash on delivery"};
        for (String p : points) {
            JLabel l = new JLabel("✓  " + p);
            l.setFont(new Font("SansSerif", Font.PLAIN, 15));
            l.setForeground(Color.WHITE);
            l.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            text.add(l);
        }
        brand.add(text);
        return brand;
    }
}
