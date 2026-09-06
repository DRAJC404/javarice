package Views;

import Models.User;
import Utils.ValidationUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class Loginframe extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel errorLabel;

    private static final Color BACKGROUND = new Color(15, 23, 42);
    private static final Color CARD = new Color(30, 41, 59);
    private static final Color PRIMARY = new Color(59, 130, 246);
    private static final Color TEXT = new Color(248, 250, 252);
    private static final Color MUTED = new Color(148, 163, 184);
    private static final Color ERROR = new Color(248, 113, 113);

    public Loginframe() {

        setTitle("bAI");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        createUI();
    }

    private void createUI() {

        JPanel mainPanel = new JPanel(new GridLayout(1, 2));
        mainPanel.setBackground(BACKGROUND);

        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(PRIMARY);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(new EmptyBorder(80, 50, 80, 50));

        JLabel logo = new JLabel("bAI");
        logo.setFont(new Font("SansSerif", Font.BOLD, 64));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel(
                "<html>AI Assistant Business</html>");
        title.setFont(new Font("SansSerif", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel description = new JLabel(
                "<html>Your intelligent assistant for<br>" +
                        "managing and understanding your business.</html>");
        description.setFont(new Font("SansSerif", Font.PLAIN, 16));
        description.setForeground(new Color(219, 234, 254));
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        leftPanel.add(logo);
        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.add(title);
        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.add(description);

        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(BACKGROUND);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(new EmptyBorder(100, 60, 80, 60));

        JLabel loginTitle = new JLabel("Welcome back");
        loginTitle.setFont(new Font("SansSerif", Font.BOLD, 30));
        loginTitle.setForeground(TEXT);
        loginTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel loginSubtitle = new JLabel("Sign in to continue to your dashboard");
        loginSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        loginSubtitle.setForeground(MUTED);
        loginSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        rightPanel.add(loginTitle);
        rightPanel.add(Box.createVerticalStrut(8));
        rightPanel.add(loginSubtitle);

        rightPanel.add(Box.createVerticalStrut(35));

        JLabel usernameLabel = createLabel("Username");
        usernameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        usernameField = new JTextField();
        styleTextField(usernameField);
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        rightPanel.add(usernameLabel);
        rightPanel.add(Box.createVerticalStrut(8));
        rightPanel.add(usernameField);

        rightPanel.add(Box.createVerticalStrut(20));

        JLabel passwordLabel = createLabel("Password");
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel passwordPanel = new JPanel(new BorderLayout());
        passwordPanel.setBackground(CARD);
        passwordPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        passwordPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        passwordField = new JPasswordField();
        passwordField.setBackground(CARD);
        passwordField.setForeground(TEXT);
        passwordField.setCaretColor(TEXT);
        passwordField.setBorder(new EmptyBorder(0, 12, 0, 0));
        passwordField.setFont(new Font("SansSerif", Font.PLAIN, 15));

        JButton showPasswordButton = new JButton("Show");
        showPasswordButton.setFocusPainted(false);
        showPasswordButton.setBorderPainted(false);
        showPasswordButton.setBackground(CARD);
        showPasswordButton.setForeground(MUTED);
        showPasswordButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        showPasswordButton.addActionListener(e -> {

            if (passwordField.getEchoChar() == '\u0000') {
                passwordField.setEchoChar('•');
                showPasswordButton.setText("Show");
            } else {
                passwordField.setEchoChar('\u0000');
                showPasswordButton.setText("Hide");
            }
        });

        passwordPanel.add(passwordField, BorderLayout.CENTER);
        passwordPanel.add(showPasswordButton, BorderLayout.EAST);

        rightPanel.add(passwordLabel);
        rightPanel.add(Box.createVerticalStrut(8));
        rightPanel.add(passwordPanel);

        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        errorLabel.setForeground(ERROR);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(errorLabel);

        JButton loginButton = new JButton("Sign In");
        styleButton(loginButton);
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        loginButton.addActionListener(e -> login());

        passwordField.addActionListener(e -> login());

        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(loginButton);

        mainPanel.add(leftPanel);
        mainPanel.add(rightPanel);

        add(mainPanel);
    }

    private void login() {

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (ValidationUtils.isEmpty(username)) {
            showError("Username is required.");
            usernameField.requestFocus();
            return;
        }

        if (ValidationUtils.isEmpty(password)) {
            showError("Password is required.");
            passwordField.requestFocus();
            return;
        }

        if (!ValidationUtils.isValidUsername(username)) {
            showError("Username must be at least 3 characters.");
            usernameField.requestFocus();
            return;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            showError("Password must be at least 4 characters.");
            passwordField.requestFocus();
            return;
        }

        User user = new User(
                "admin",
                "1234",
                "Administrator");

        if (username.equals(user.getUsername())
                && password.equals(user.getPassword())) {

            errorLabel.setText(" ");

            new DashboardFrame(user).setVisible(true);

            dispose();

        } else {

            showError("Invalid username or password.");
            passwordField.setText("");
            passwordField.requestFocus();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private JLabel createLabel(String text) {

        JLabel label = new JLabel(text);

        label.setFont(
                new Font("SansSerif", Font.BOLD, 13));

        label.setForeground(TEXT);

        return label;
    }

    private void styleTextField(JTextField field) {

        field.setBackground(CARD);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setBorder(
                new EmptyBorder(0, 12, 0, 12));

        field.setFont(
                new Font("SansSerif", Font.PLAIN, 15));

        field.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 45));
    }

    private void styleButton(JButton button) {

        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFont(
                new Font("SansSerif", Font.BOLD, 15));

        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(
                new Cursor(Cursor.HAND_CURSOR));

        button.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 45));
    }
}