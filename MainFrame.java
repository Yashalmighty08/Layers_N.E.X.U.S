// Modified MainFrame.java
import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private User currentUser;
    TaskPanel taskManagementPanel = new TaskPanel();
    
    public MainFrame(User user) {
        this.currentUser = user;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Layers Desert Bar - Management System (" + currentUser.getRole() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);

        // Create main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Create header with brand name and user info
        JPanel headerPanel = createHeaderPanel();
        
        JTabbedPane tabbedPane = createTabbedPane();

        // Add components to main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        add(mainPanel);
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(70, 130, 180));
        headerPanel.setPreferredSize(new Dimension(1200, 100));
        headerPanel.setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Layers Desert Bar", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(25, 0, 25, 0));
        
        // User info label
        JLabel userLabel = new JLabel("Logged in as: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
        userLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        userLabel.setForeground(Color.WHITE);
        userLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(userLabel, BorderLayout.WEST);
        
        return headerPanel;
    }
    
    private JTabbedPane createTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();

        // Always available tabs
        JPanel homePanel = createHomePanel();
        JPanel taskAlertsPanel = createAlertPanel();
        JPanel orderApplicationPanel = createOrderPanel();
        // Add always available tabs
        tabbedPane.addTab("Home", homePanel);
        tabbedPane.addTab("Task Alerts", taskAlertsPanel);
        tabbedPane.addTab("Order Application", orderApplicationPanel);

        // Role-based tabs (only for Managers)
        if (currentUser.getRole().equals("Manager")) {
            JPanel taskManagementPanel = new TaskPanel();
            JPanel financePanel = createFinancePanel();
            JPanel reportingPanel = createReportPanel();
            JPanel recipePanel = createRecipePanel(); // Changed this line
            JPanel inventoryPanel = createInventoryPanel();
            
            tabbedPane.addTab("Task Management", taskManagementPanel);
            tabbedPane.addTab("Finance & Payroll", financePanel);
            tabbedPane.addTab("Reporting", reportingPanel);
            tabbedPane.addTab("Recipe", recipePanel);
            tabbedPane.addTab("Inventory", inventoryPanel);
        } else {
            // For employees, show disabled tabs with tooltip
            addDisabledTab(tabbedPane, "Task Management", "Access restricted to Managers only");
            addDisabledTab(tabbedPane, "Finance & Payroll", "Access restricted to Managers only");
            addDisabledTab(tabbedPane, "Reporting", "Access restricted to Managers only");
            addDisabledTab(tabbedPane, "Recipe", "Access restricted to Managers only");
            addDisabledTab(tabbedPane, "Inventory", "Access restricted to Managers only");
        }
        
        return tabbedPane;
    }

    private JPanel createInventoryPanel() {
        InventoryManagementSystem inventorySystem = new InventoryManagementSystem();
        return (JPanel) inventorySystem.getContentPane();
    }

    private JPanel createReportPanel() {
        ReportingPanel reportPanel = new ReportingPanel();
        return reportPanel; 
    }
    private JPanel createAlertPanel() {
        return new AlertsPanel(currentUser);
    }
    
    private JPanel createRecipePanel() {
        // Create an instance of RecipeManagementSystem and get its content
        RecipeManagementSystem recipeSystem = new RecipeManagementSystem();
        JPanel recipePanel = (JPanel) recipeSystem.getContentPane();

        recipePanel.setName("Recipe Management");
        
        return recipePanel;
    }
    private JPanel createOrderPanel(){
        OrderApplicationUI orderUI = new OrderApplicationUI();
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(orderUI.getContentPane(), BorderLayout.CENTER);
        return panel;
        
        

    }

    private JPanel createFinancePanel(){
        FinancePanel financepanel = new FinancePanel();
        return financepanel;
    }
    private void addDisabledTab(JTabbedPane tabbedPane, String title, String tooltip) {
        JPanel disabledPanel = new JPanel();
        disabledPanel.setBackground(Color.LIGHT_GRAY);
        
        JLabel messageLabel = new JLabel(title + " - Access Restricted", JLabel.CENTER);
        messageLabel.setForeground(Color.RED);
        messageLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        
        disabledPanel.add(messageLabel);
        tabbedPane.addTab(title, disabledPanel);
        
        // Disable the tab
        int tabIndex = tabbedPane.indexOfTab(title);
        tabbedPane.setEnabledAt(tabIndex, false);
    }
    
    private JPanel createPanel(String title) {
        JPanel panel = new JPanel();
        panel.add(new JLabel(title));
        return panel;
    }
    
    private JPanel createHomePanel() {
        JPanel homePanel = new JPanel(new BorderLayout());
        homePanel.setBackground(Color.WHITE);
        
        // Welcome message
        JLabel welcomeLabel = new JLabel("Welcome to Layers Desert Bar Management System", JLabel.CENTER);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        welcomeLabel.setForeground(new Color(70, 130, 180));
        welcomeLabel.setBorder(BorderFactory.createEmptyBorder(50, 0, 0, 0));
        
        // Role-specific greeting
        String roleGreeting = currentUser.getRole().equals("Manager") ? 
            "Manager Dashboard - Full System Access" : 
            "Employee Dashboard - Limited Access";
        
        JLabel roleLabel = new JLabel(roleGreeting, JLabel.CENTER);
        roleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        roleLabel.setForeground(Color.DARK_GRAY);
        roleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        
        // Subtitle
        JLabel subtitleLabel = new JLabel("Your complete solution for desert bar management", JLabel.CENTER);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        subtitleLabel.setForeground(Color.DARK_GRAY);
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 50, 0));
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(roleLabel, BorderLayout.NORTH);
        centerPanel.add(subtitleLabel, BorderLayout.CENTER);
        
        homePanel.add(welcomeLabel, BorderLayout.NORTH);
        homePanel.add(centerPanel, BorderLayout.CENTER);
        
        return homePanel;
    }
    
    // Make the main method launch the security system instead
    public static void main(String[] args) {
        Security.main(args);
    }

}
