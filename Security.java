// Security.java
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.HashMap;

public class Security {
    private static final String USER_DATA_FILE = "users.txt";
    private static HashMap<String, User> users = new HashMap<>();
    
    public static void main(String[] args) {
        loadUsers();
        showAuthDialog();
    }
    
    
    
    private static void loadUsers() {
        try {
            File file = new File(USER_DATA_FILE);
            if (!file.exists()) {
                // Create default admin user
                users.put("admin", new User("admin", "admin123", "Manager"));
                saveUsers();
                return;
            }
            
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length == 3) {
                    users.put(parts[0], new User(parts[0], parts[1], parts[2]));
                }
            }
            reader.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error loading user data: " + e.getMessage());
        }
    }
    
    private static void saveUsers() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(USER_DATA_FILE));
            for (User user : users.values()) {
                writer.println(user.getUsername() + " " + user.getPassword() + " " + user.getRole());
            }
            writer.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error saving user data: " + e.getMessage());
        }
    }
    
    private static void showAuthDialog() {
        JFrame authFrame = new JFrame("Layers Desert Bar - Authentication");
        authFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        authFrame.setSize(400, 300);
        authFrame.setLocationRelativeTo(null);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("Welcome to Layers Desert Bar", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setForeground(new Color(70, 130, 180));
        
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
        
        JButton signInBtn = new JButton("Sign In");
        JButton signUpBtn = new JButton("Sign Up");
        
        signInBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        signUpBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        
        signInBtn.addActionListener(e -> showSignInDialog(authFrame));
        signUpBtn.addActionListener(e -> showSignUpDialog());
        
        buttonPanel.add(signInBtn);
        buttonPanel.add(signUpBtn);
        
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        
        authFrame.add(mainPanel);
        authFrame.setVisible(true);
    }
    
    private static void showSignInDialog(JFrame parentFrame) {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        
        int result = JOptionPane.showConfirmDialog(parentFrame, panel, "Sign In", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            
            if (authenticateUser(username, password)) {
                User user = users.get(username);
                JOptionPane.showMessageDialog(parentFrame, "Welcome, " + username + "!");
                parentFrame.dispose();
                launchMainApplication(user);
            } else {
                JOptionPane.showMessageDialog(parentFrame, "Invalid username or password!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private static void showSignUpDialog() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JComboBox<String> roleComboBox = new JComboBox<>(new String[]{"Employee", "Manager"});
        
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(new JLabel("Role:"));
        panel.add(roleComboBox);
        
        int result = JOptionPane.showConfirmDialog(null, panel, "Sign Up", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String role = (String) roleComboBox.getSelectedItem();
            
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please fill all fields!");
                return;
            }
            
            if (users.containsKey(username)) {
                JOptionPane.showMessageDialog(null, "Username already exists!");
                return;
            }
            
            users.put(username, new User(username, password, role));
            saveUsers();
            JOptionPane.showMessageDialog(null, "Registration successful! Please sign in.");
        }
    }
    
    private static boolean authenticateUser(String username, String password) {
        User user = users.get(username);
        return user != null && user.getPassword().equals(password);
    }
    
    private static void launchMainApplication(User user) {
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame(user);
            mainFrame.setVisible(true);
        });
    }
}