package Views;

import Models.User;
import Utils.ValidationUtils;

import javax.swing.*;
import java.awt.*;

public class Loginframe extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;

    public Loginframe() {

        setTitle("AI Business Companion - Login");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 2, 10, 10));

        panel.add(new JLabel("Username:"));

        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel("Password:"));

        passwordField = new JPasswordField();
        panel.add(passwordField);

        panel.add(new JLabel(""));

        JButton loginButton = new JButton("Login");
        panel.add(loginButton);

        add(panel);

        loginButton.addActionListener(e -> login());
    }

    private void login() {

        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (ValidationUtils.isEmpty(username)
                || ValidationUtils.isEmpty(password)) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter username and password.");

            return;
        }

        User user = new User("admin", "1234");

        if (username.equals(user.getUsername())
                && password.equals(user.getPassword())) {

            JOptionPane.showMessageDialog(
                    this,
                    "Login successful!");

            new DashboardFrame(username).setVisible(true);

            dispose();

        } else {

            JOptionPane.showMessageDialog(
                    this,
                    "Invalid username or password.");
        }
    }
}