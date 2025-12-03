import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

public class FinancePanel extends JPanel {
    private JTable financeTable;
    private DefaultTableModel tableModel;
    private JButton calculatePayrollButton, addPayButton, addHoursButton;
    private JTextField revenueField, totalExpensesField, balanceField;
    private String financeFile = "finance.txt";
    private String usersFile = "users.txt";
    private String ordersFile = "orders.txt";
    private DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");

    public FinancePanel() {
        initializeUI();
        loadFinanceData();
        updateFinancialSummary();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ---- Top panel with title ----
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Finance & Payroll Tracking", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // ---- Table setup ----
        String[] columnNames = {"Employee Name", "Pay per Hour", "Hours Worked This Week", "Calculated Weekly Pay", "Other Costs"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // View only
            }
            
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 1 || column == 2 || column == 3 || column == 4) {
                    return Double.class;
                }
                return String.class;
            }
        };

        financeTable = new JTable(tableModel);
        financeTable.setRowHeight(25);
        JScrollPane scrollPane = new JScrollPane(financeTable);

        // ---- Control buttons ----
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        calculatePayrollButton = new JButton("Calculate Payroll");
        addPayButton = new JButton("Add Pay");
        addHoursButton = new JButton("Add Hours");

        calculatePayrollButton.addActionListener(e -> calculatePayroll());
        addPayButton.addActionListener(e -> showAddPayDialog());
        addHoursButton.addActionListener(e -> showAddHoursDialog());

        buttonPanel.add(calculatePayrollButton);
        buttonPanel.add(addPayButton);
        buttonPanel.add(addHoursButton);

        // ---- Financial Summary Panel ----
        JPanel summaryPanel = createSummaryPanel();

        // ---- Add all panels to main layout ----
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        add(summaryPanel, BorderLayout.EAST);
    }

    private JPanel createSummaryPanel() {
        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Financial Summary"));
        summaryPanel.setPreferredSize(new Dimension(250, 200));

        // Revenue field
        JPanel revenuePanel = new JPanel(new BorderLayout());
        revenuePanel.add(new JLabel("Revenue:"), BorderLayout.WEST);
        revenueField = new JTextField(10);
        revenueField.setEditable(false);
        revenuePanel.add(revenueField, BorderLayout.EAST);

        // Total Expenses field
        JPanel expensesPanel = new JPanel(new BorderLayout());
        expensesPanel.add(new JLabel("Total Expenses:"), BorderLayout.WEST);
        totalExpensesField = new JTextField(10);
        totalExpensesField.setEditable(false);
        expensesPanel.add(totalExpensesField, BorderLayout.EAST);

        // Balance field
        JPanel balancePanel = new JPanel(new BorderLayout());
        balancePanel.add(new JLabel("Balance:"), BorderLayout.WEST);
        balanceField = new JTextField(10);
        balanceField.setEditable(false);
        balancePanel.add(balanceField, BorderLayout.EAST);

        // Add to summary panel
        summaryPanel.add(Box.createVerticalStrut(10));
        summaryPanel.add(revenuePanel);
        summaryPanel.add(Box.createVerticalStrut(10));
        summaryPanel.add(expensesPanel);
        summaryPanel.add(Box.createVerticalStrut(10));
        summaryPanel.add(balancePanel);
        summaryPanel.add(Box.createVerticalGlue());

        return summaryPanel;
    }

    private void loadFinanceData() {
        try {
            // First, sync employees with users.txt
            syncEmployeesWithUsers();
            
            // Read finance data
            File file = new File(financeFile);
            if (!file.exists()) {
                createDefaultFinanceFile();
                return;
            }

            tableModel.setRowCount(0);
            BufferedReader reader = new BufferedReader(new FileReader(financeFile));
            String line;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                String[] parts = line.split(" ");
                if (parts.length >= 5) {
                    String employeeName = parts[0];
                    double payPerHour = Double.parseDouble(parts[1]);
                    double hoursWorked = Double.parseDouble(parts[2]);
                    double weeklyPay = Double.parseDouble(parts[3]);
                    double otherCosts = Double.parseDouble(parts[4]);
                    
                    Object[] rowData = {
                        employeeName.replace("_", " "),
                        payPerHour,
                        hoursWorked,
                        weeklyPay,
                        otherCosts
                    };
                    tableModel.addRow(rowData);
                }
            }
            reader.close();
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading finance data: " + e.getMessage(),
                                         "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void syncEmployeesWithUsers() {
        try {
            // Read existing finance data
            Map<String, String[]> existingFinance = new HashMap<>();
            File financeFileObj = new File(financeFile);
            
            if (financeFileObj.exists()) {
                BufferedReader financeReader = new BufferedReader(new FileReader(financeFile));
                String line;
                while ((line = financeReader.readLine()) != null) {
                    String[] parts = line.split(" ");
                    if (parts.length >= 5) {
                        existingFinance.put(parts[0], new String[]{parts[1], parts[2], parts[3], parts[4]});
                    }
                }
                financeReader.close();
            }

            // Read users.txt to get all employees
            Set<String> allEmployees = new HashSet<>();
            File usersFileObj = new File(usersFile);
            
            if (usersFileObj.exists()) {
                BufferedReader usersReader = new BufferedReader(new FileReader(usersFile));
                String line;
                while ((line = usersReader.readLine()) != null) {
                    String[] parts = line.split(" ");
                    if (parts.length >= 1) {
                        String employeeName = parts[0];
                        allEmployees.add(employeeName);
                        
                        // Add missing employees with default values
                        if (!existingFinance.containsKey(employeeName)) {
                            existingFinance.put(employeeName, new String[]{"0.00", "0.00", "0.00", "0.00"});
                            System.out.println("Added new employee to finance: " + employeeName);
                        }
                    }
                }
                usersReader.close();
            }

            // Save updated finance data
            saveFinanceData(existingFinance);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error syncing employees: " + e.getMessage(),
                                         "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveFinanceData(Map<String, String[]> financeData) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(financeFile));
            
            // Sort employee names alphabetically
            List<String> sortedEmployees = new ArrayList<>(financeData.keySet());
            Collections.sort(sortedEmployees);
            
            for (String employee : sortedEmployees) {
                String[] values = financeData.get(employee);
                writer.println(employee + " " + values[0] + " " + values[1] + " " + values[2] + " " + values[3]);
            }
            writer.close();
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving finance data: " + e.getMessage(),
                                         "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createDefaultFinanceFile() {
        try {
            // This will trigger the sync and create the file
            syncEmployeesWithUsers();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error creating default finance file: " + e.getMessage(),
                                         "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void calculatePayroll() {
        try {
            // Read current finance data
            Map<String, String[]> financeData = readFinanceData();
            
            // Calculate weekly pay for each employee
            for (Map.Entry<String, String[]> entry : financeData.entrySet()) {
                String[] values = entry.getValue();
                double payPerHour = Double.parseDouble(values[0]);
                double hoursWorked = Double.parseDouble(values[1]);
                double weeklyPay = payPerHour * hoursWorked;
                
                // Update the weekly pay
                values[2] = String.format("%.2f", weeklyPay);
            }
            
            // Save updated data
            saveFinanceData(financeData);
            
            // Reload table
            loadFinanceData();
            
            // Update financial summary
            updateFinancialSummary();
            
            JOptionPane.showMessageDialog(this, "Payroll calculated successfully!",
                                         "Success", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error calculating payroll: " + e.getMessage(),
                                         "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Map<String, String[]> readFinanceData() throws IOException {
        Map<String, String[]> financeData = new HashMap<>();
        File file = new File(financeFile);
        
        if (file.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(financeFile));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length >= 5) {
                    financeData.put(parts[0], new String[]{parts[1], parts[2], parts[3], parts[4]});
                }
            }
            reader.close();
        }
        return financeData;
    }

    private void showAddPayDialog() {
        JDialog dialog = new JDialog((Frame)SwingUtilities.getWindowAncestor(this), "Add Pay", true);
        dialog.setLayout(new GridLayout(3, 2, 10, 10));
        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(this);

        JTextField nameField = new JTextField();
        JTextField payField = new JTextField();
        
        dialog.add(new JLabel("Employee Name:"));
        dialog.add(nameField);
        dialog.add(new JLabel("Pay per Hour:"));
        dialog.add(payField);

        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            String employeeName = nameField.getText().trim().replace(" ", "_");
            try {
                double payPerHour = Double.parseDouble(payField.getText().trim());
                
                // Update employee's pay rate
                Map<String, String[]> financeData = readFinanceData();
                if (financeData.containsKey(employeeName)) {
                    String[] values = financeData.get(employeeName);
                    values[0] = String.format("%.2f", payPerHour);
                    saveFinanceData(financeData);
                    
                    loadFinanceData();
                    updateFinancialSummary();
                    
                    dialog.dispose();
                    JOptionPane.showMessageDialog(this, "Pay rate updated successfully!",
                                                 "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(dialog, "Employee not found: " + employeeName,
                                                 "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter valid numbers",
                                             "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        dialog.add(new JLabel());
        dialog.add(buttonPanel);

        dialog.setVisible(true);
    }

    private void showAddHoursDialog() {
        JDialog dialog = new JDialog((Frame)SwingUtilities.getWindowAncestor(this), "Add Hours", true);
        dialog.setLayout(new GridLayout(3, 2, 10, 10));
        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(this);

        JTextField nameField = new JTextField();
        JTextField hoursField = new JTextField();
        
        dialog.add(new JLabel("Employee Name:"));
        dialog.add(nameField);
        dialog.add(new JLabel("Hours Worked:"));
        dialog.add(hoursField);

        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            String employeeName = nameField.getText().trim().replace(" ", "_");
            try {
                double hours = Double.parseDouble(hoursField.getText().trim());
                
                // Update employee's hours
                Map<String, String[]> financeData = readFinanceData();
                if (financeData.containsKey(employeeName)) {
                    String[] values = financeData.get(employeeName);
                    values[1] = String.format("%.2f", hours);
                    saveFinanceData(financeData);
                    
                    loadFinanceData();
                    updateFinancialSummary();
                    
                    dialog.dispose();
                    JOptionPane.showMessageDialog(this, "Hours updated successfully!",
                                                 "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(dialog, "Employee not found: " + employeeName,
                                                 "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter valid numbers",
                                             "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        dialog.add(new JLabel());
        dialog.add(buttonPanel);

        dialog.setVisible(true);
    }

    private void updateFinancialSummary() {
        try {
            // Calculate total revenue from orders.txt
            double totalRevenue = calculateTotalRevenue();
            revenueField.setText(currencyFormat.format(totalRevenue));
            
            // Calculate total expenses (payroll + other costs)
            double totalPayroll = 0.0;
            double totalOtherCosts = 0.0;
            
            Map<String, String[]> financeData = readFinanceData();
            for (String[] values : financeData.values()) {
                totalPayroll += Double.parseDouble(values[2]); // Weekly pay
                totalOtherCosts += Double.parseDouble(values[3]); // Other costs
            }
            
            double totalExpenses = totalPayroll + totalOtherCosts;
            totalExpensesField.setText(currencyFormat.format(totalExpenses));
            
            // Calculate balance
            double balance = totalRevenue - totalExpenses;
            balanceField.setText(currencyFormat.format(balance));
            
            // Color code the balance
            if (balance < 0) {
                balanceField.setForeground(Color.RED);
            } else {
                balanceField.setForeground(new Color(0, 100, 0)); // Dark green
            }
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error updating financial summary: " + e.getMessage(),
                                         "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private double calculateTotalRevenue() throws IOException {
        double totalRevenue = 0.0;
        File ordersFileObj = new File(ordersFile);
        
        if (!ordersFileObj.exists()) {
            System.out.println("Orders file not found: " + ordersFile);
            return 0.0;
        }
        
        System.out.println("Reading orders from: " + ordersFile);
        BufferedReader reader = new BufferedReader(new FileReader(ordersFile));
        String line;
        int orderCount = 0;
        
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // Parse the CSV format: OrderID,Items,TotalPrice,Status
            String[] parts = line.split(",");
            if (parts.length >= 3) {
                orderCount++;
                String orderId = parts[0].trim();
                String priceStr = parts[2].trim();
                String status = parts.length >= 4 ? parts[3].trim() : "Unknown";
                
                try {
                    // Count ALL orders as revenue (regardless of status)
                    if (!priceStr.isEmpty()) {
                        double orderTotal = Double.parseDouble(priceStr);
                        if (orderTotal > 0) {
                            totalRevenue += orderTotal;
                            System.out.println("Order #" + orderCount + ": ID=" + orderId + 
                                             ", Price=$" + orderTotal + 
                                             ", Status=" + status + 
                                             " -> Added to revenue");
                        } else {
                            System.out.println("Order #" + orderCount + ": ID=" + orderId + 
                                             ", Price=$" + orderTotal + 
                                             " -> Skipped (zero price)");
                        }
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Skipping invalid price in order " + orderId + 
                                     ": '" + priceStr + "'");
                    continue;
                }
            }
        }
        reader.close();
        
        System.out.println("Total orders processed: " + orderCount);
        System.out.println("Total revenue calculated: $" + totalRevenue);
        return totalRevenue;
    }

    public void refreshData() {
        loadFinanceData();
        updateFinancialSummary();
    }
}

