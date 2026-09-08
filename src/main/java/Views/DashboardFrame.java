package Views;

import Models.User;
import Services.ProductService;
import Utils.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DashboardFrame extends JFrame {

    private final User user;
    private final ProductService productService;

        private static final Color BACKGROUND = Theme.BACKGROUND;
        private static final Color SIDEBAR = Theme.SIDEBAR;
        private static final Color CARD = Theme.CARD;
        private static final Color PRIMARY = Theme.PRIMARY;
        private static final Color TEXT = Theme.TEXT;
        private static final Color MUTED = Theme.MUTED;

    public DashboardFrame(User user) {

        this.user = user;
        this.productService = new ProductService();

        setTitle("bAI - Dashboard");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        createUI();
    }

    private void createUI() {

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BACKGROUND);

        mainPanel.add(createSidebar(), BorderLayout.WEST);
        mainPanel.add(createContent(), BorderLayout.CENTER);

        add(mainPanel);
    }

    private JPanel createSidebar() {

        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(230, 700));
        sidebar.setBackground(SIDEBAR);
        sidebar.setLayout(new BoxLayout(
                sidebar,
                BoxLayout.Y_AXIS));

        sidebar.setBorder(
                new EmptyBorder(30, 20, 30, 20));

        JLabel logo = new JLabel("bAI");
        logo.setFont(
                new Font("SansSerif", Font.BOLD, 20));
        logo.setForeground(TEXT);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("COMPANION");
        subtitle.setFont(
                new Font("SansSerif", Font.BOLD, 10));
        subtitle.setForeground(PRIMARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(2));
        sidebar.add(subtitle);

        sidebar.add(Box.createVerticalStrut(40));

        JButton dashboardButton = createSidebarButton("Dashboard");

        JButton productsButton = createSidebarButton("Products");

        JButton inventoryButton = createSidebarButton("Inventory");

        JButton salesButton = createSidebarButton("Sales");

        JButton analyticsButton = createSidebarButton("Analytics");

        JButton aiButton = createSidebarButton("AI Assistant");

        sidebar.add(dashboardButton);
        sidebar.add(productsButton);
        sidebar.add(inventoryButton);
        sidebar.add(salesButton);
        sidebar.add(analyticsButton);
        sidebar.add(aiButton);

        sidebar.add(Box.createVerticalGlue());

        JButton logoutButton = createSidebarButton("Logout");

        logoutButton.addActionListener(e -> logout());

        sidebar.add(logoutButton);

        return sidebar;
    }

    private JButton createSidebarButton(String text) {

        JButton button = new JButton(text);

        button.setHorizontalAlignment(
                SwingConstants.LEFT);

        button.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 45));

        button.setBackground(SIDEBAR);
        button.setForeground(MUTED);

        button.setBorder(
                new EmptyBorder(10, 15, 10, 10));

        button.setFocusPainted(false);
        button.setBorderPainted(false);

        button.setFont(
                new Font("SansSerif", Font.PLAIN, 14));

        button.setCursor(
                new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    private JPanel createContent() {

        JPanel content = new JPanel(
                new BorderLayout());

        content.setBackground(BACKGROUND);
        content.setBorder(
                new EmptyBorder(35, 40, 35, 40));

        JPanel header = new JPanel(
                new BorderLayout());

        header.setBackground(BACKGROUND);

        JLabel title = new JLabel("Dashboard");

        title.setFont(
                new Font("SansSerif", Font.BOLD, 30));

        title.setForeground(TEXT);

        JLabel welcome = new JLabel(
                "Welcome back, " + user.getUsername());

        welcome.setFont(
                new Font("SansSerif", Font.PLAIN, 14));

        welcome.setForeground(MUTED);

        JPanel headerText = new JPanel();
        headerText.setBackground(BACKGROUND);
        headerText.setLayout(
                new BoxLayout(
                        headerText,
                        BoxLayout.Y_AXIS));

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(5));
        headerText.add(welcome);

        header.add(headerText, BorderLayout.WEST);

        content.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();

        center.setBackground(BACKGROUND);

        center.setLayout(
                new BoxLayout(
                        center,
                        BoxLayout.Y_AXIS));

        center.setBorder(
                new EmptyBorder(35, 0, 0, 0));

        JPanel statsPanel = new JPanel(
                new GridLayout(1, 3, 20, 0));

        statsPanel.setBackground(BACKGROUND);
        statsPanel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        130));

        statsPanel.add(
                createStatCard(
                        "Total Products",
                        String.valueOf(
                                productService.getProductCount())));

        statsPanel.add(
                createStatCard(
                        "Inventory Items",
                        String.valueOf(
                                productService.getInventoryCount())));

        statsPanel.add(
                createStatCard(
                        "Total Sales",
                        String.format(
                                "₱%,.2f",
                                productService.getTotalSales())));

        center.add(statsPanel);

        center.add(Box.createVerticalStrut(30));

        JPanel bottomPanel = new JPanel(
                new GridLayout(1, 2, 20, 0));

        bottomPanel.setBackground(BACKGROUND);

        bottomPanel.add(createQuickActions());
        bottomPanel.add(createAISection());

        center.add(bottomPanel);

        content.add(center, BorderLayout.CENTER);

        return content;
    }

    private JPanel createStatCard(
            String title,
            String value) {

        JPanel card = new JPanel();

        card.setBackground(CARD);

        card.setBorder(
                new EmptyBorder(20, 20, 20, 20));

        card.setLayout(
                new BoxLayout(
                        card,
                        BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);

        titleLabel.setForeground(MUTED);
        titleLabel.setFont(
                new Font("SansSerif", Font.PLAIN, 14));

        JLabel valueLabel = new JLabel(value);

        valueLabel.setForeground(TEXT);
        valueLabel.setFont(
                new Font("SansSerif", Font.BOLD, 28));

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(valueLabel);

        return card;
    }

    private JPanel createQuickActions() {

        JPanel panel = createCardPanel();

        JLabel title = createSectionTitle(
                "Quick Actions");

        panel.add(title);
        panel.add(Box.createVerticalStrut(20));

        JButton addProduct = new JButton("+ Add Product");

        JButton viewProducts = new JButton("View Products");

        JButton viewSales = new JButton("View Sales");

        styleActionButton(addProduct);
        styleActionButton(viewProducts);
        styleActionButton(viewSales);

        panel.add(addProduct);
        panel.add(Box.createVerticalStrut(10));

        panel.add(viewProducts);
        panel.add(Box.createVerticalStrut(10));

        panel.add(viewSales);

        return panel;
    }

    private JPanel createAISection() {

        JPanel panel = createCardPanel();

        JLabel title = createSectionTitle(
                "AI Business Assistant");

        JLabel description = new JLabel(
                "<html>Ask AI about your business,<br>" +
                        "sales, inventory, and products.</html>");

        description.setForeground(MUTED);

        description.setFont(
                new Font("SansSerif", Font.PLAIN, 14));

        JButton askAI = new JButton("Open AI Assistant");

        styleActionButton(askAI);

        askAI.addActionListener(e -> {

            JOptionPane.showMessageDialog(
                    this,
                    "AI Assistant will be connected here.",
                    "AI Assistant",
                    JOptionPane.INFORMATION_MESSAGE);

        });

        panel.add(title);
        panel.add(Box.createVerticalStrut(15));
        panel.add(description);
        panel.add(Box.createVerticalStrut(25));
        panel.add(askAI);

        return panel;
    }

    private JPanel createCardPanel() {

        JPanel panel = new JPanel();

        panel.setBackground(CARD);

        panel.setLayout(
                new BoxLayout(
                        panel,
                        BoxLayout.Y_AXIS));

        panel.setBorder(
                new EmptyBorder(25, 25, 25, 25));

        return panel;
    }

    private JLabel createSectionTitle(String text) {

        JLabel label = new JLabel(text);

        label.setForeground(TEXT);

        label.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        19));

        return label;
    }

    private void styleActionButton(JButton button) {

        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);

        button.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        13));

        button.setFocusPainted(false);
        button.setBorderPainted(false);

        button.setAlignmentX(Component.LEFT_ALIGNMENT);

        button.setCursor(
                new Cursor(Cursor.HAND_CURSOR));
    }

    private void logout() {

        int result = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to logout?",
                "Logout",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {

            new Loginframe().setVisible(true);

            dispose();
        }
    }
}
