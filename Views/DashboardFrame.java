package Views;

import Services.ProductService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DashboardFrame extends JFrame {

    private ProductService productService;

    public DashboardFrame(String username) {

        productService = new ProductService();

        setTitle("bAI - Dashboard");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel welcomeLabel = new JLabel(
                "Welcome, " + username + "!");

        welcomeLabel.setFont(
                new Font("Arial", Font.BOLD, 24));

        JButton productsButton = new JButton("View Products");

        productsButton.addActionListener(e -> showProducts());

        JButton aiButton = new JButton("AI Assistant");

        aiButton.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "AI Assistant coming soon!"));

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());

        panel.add(welcomeLabel);
        panel.add(productsButton);
        panel.add(aiButton);

        add(panel);
    }

    private void showProducts() {

        List<String> products = productService.getProducts();

        StringBuilder productList = new StringBuilder();

        for (String product : products) {
            productList.append(product).append("\n");
        }

        JOptionPane.showMessageDialog(
                this,
                productList.toString(),
                "Products",
                JOptionPane.INFORMATION_MESSAGE);
    }
}