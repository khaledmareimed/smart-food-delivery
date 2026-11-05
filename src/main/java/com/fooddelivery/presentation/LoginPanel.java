package com.fooddelivery.presentation;

import com.fooddelivery.domain.User;
import com.fooddelivery.exception.AuthenticationException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class LoginPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private JTextField usernameField = new JTextField(15);
    private JPasswordField passwordField = new JPasswordField(15);
    private JLabel errorLabel = new JLabel(" ");

    public LoginPanel(final MainFrame frame) {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Smart Food Delivery", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        add(title, c);

        c.gridwidth = 1;
        c.gridy = 1;
        add(new JLabel("Username:"), c);
        c.gridx = 1;
        add(usernameField, c);

        c.gridx = 0;
        c.gridy = 2;
        add(new JLabel("Password:"), c);
        c.gridx = 1;
        add(passwordField, c);

        JButton loginButton = new JButton("Login");
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        add(loginButton, c);

        errorLabel.setForeground(Color.RED);
        c.gridy = 4;
        add(errorLabel, c);

        JLabel hint = new JLabel("Demo: alice / alice123 (customer), bob / manager123 (manager)");
        hint.setForeground(Color.GRAY);
        hint.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        c.gridy = 5;
        add(hint, c);

        loginButton.addActionListener(e -> {
            try {
                User user = frame.authService.login(usernameField.getText(),
                        new String(passwordField.getPassword()));
                frame.loggedIn(user);
            } catch (AuthenticationException ex) {
                errorLabel.setText(ex.getMessage());
                passwordField.setText("");
            }
        });
        passwordField.addActionListener(e -> loginButton.doClick());
    }
}
