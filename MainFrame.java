import javax.swing.*;
import java.awt.*;

public class MainFrame {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Layers Desert Bar - Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);

        // Create main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Create header with brand name
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(70, 130, 180)); // Steel blue background
        headerPanel.setPreferredSize(new Dimension(1200, 100));
        headerPanel.setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Layers Desert Bar", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(25, 0, 25, 0));
        
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        JTabbedPane tabbedPane = new JTabbedPane();

        // Create panels for each feature
        JPanel homePanel = createHomePanel();
        JPanel taskAlertsPanel = new JPanel();
        taskAlertsPanel.add(new JLabel("Task Alerts & Notifications"));
        
        JPanel taskManagementPanel = new JPanel();
        taskManagementPanel.add(new JLabel("Task Management"));
        
        JPanel reportingPanel = new JPanel();
        reportingPanel.add(new JLabel("Reporting"));
        
        JPanel orderApplicationPanel = new JPanel();
        orderApplicationPanel.add(new JLabel("Order Application"));
        
        JPanel recipePanel = new JPanel();
        recipePanel.add(new JLabel("Recipe Management"));
        
        JPanel inventoryPanel = new JPanel();
        inventoryPanel.add(new JLabel("Inventory Management"));

        // Add tabs to the tabbed pane
        tabbedPane.addTab("Home", homePanel);
        tabbedPane.addTab("Task Alerts", taskAlertsPanel);
        tabbedPane.addTab("Task Management", taskManagementPanel);
        tabbedPane.addTab("Reporting", reportingPanel);
        tabbedPane.addTab("Order Application", orderApplicationPanel);
        tabbedPane.addTab("Recipe", recipePanel);
        tabbedPane.addTab("Inventory", inventoryPanel);

        // Add components to main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private static JPanel createHomePanel() {
        JPanel homePanel = new JPanel(new BorderLayout());
        homePanel.setBackground(Color.WHITE);
        
        // Welcome message
        JLabel welcomeLabel = new JLabel("Welcome to Layers Desert Bar Management System", JLabel.CENTER);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        welcomeLabel.setForeground(new Color(70, 130, 180)); // Same blue color
        welcomeLabel.setBorder(BorderFactory.createEmptyBorder(50, 0, 0, 0));
        
        // Subtitle
        JLabel subtitleLabel = new JLabel("Your complete solution for desert bar management", JLabel.CENTER);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        subtitleLabel.setForeground(Color.DARK_GRAY);
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 50, 0));
        
        homePanel.add(welcomeLabel, BorderLayout.NORTH);
        homePanel.add(subtitleLabel, BorderLayout.CENTER);
        
        return homePanel;
    }
}