import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.List;

public class InventoryManagement {
    private static InventoryPanel inventoryPanel;
    private DefaultTableModel tableModel;
    private static String dataFile = "inventory.txt";
    private String recipeFile = "recipes.txt";

    public InventoryManagement(InventoryPanel panel) {
        this.inventoryPanel = panel;
        initializeListeners();
        loadInventory();
    }

    private void initializeListeners() {
        inventoryPanel.getEditInventoryButton().addActionListener(new EditInventoryListener());
        inventoryPanel.getRefreshButton().addActionListener(new RefreshListener());
    }

    public void loadInventory() {
        try {
            // First, synchronize with recipe ingredients
            syncWithRecipes();

            File file = new File(dataFile);
            if (!file.exists()) {
                createDefaultInventory();
                return;
            }

            BufferedReader reader = new BufferedReader(new FileReader(dataFile));
            String headerLine = reader.readLine();
            
            if (headerLine != null) {
                // Create table model
                Vector<String> columnNames = new Vector<>();
                columnNames.add("Ingredient Name");
                columnNames.add("Current Stock");
                columnNames.add("Low Stock Threshold");

                tableModel = new DefaultTableModel(columnNames, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false; // Make table non-editable directly
                    }
                    
                    @Override
                    public Class<?> getColumnClass(int column) {
                        if (column == 1 || column == 2) return Integer.class;
                        return String.class;
                    }
                };

                // Read inventory data
                String line;
                int rowCount = 0;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    
                    String[] data = line.split(",");
                    if (data.length >= 3) {
                        // Skip header rows
                        if (data[0].equalsIgnoreCase("Ingredient Name") || 
                            data[1].equalsIgnoreCase("Current Stock") || 
                            data[2].equalsIgnoreCase("Low Stock Threshold")) {
                            System.out.println("Skipping header row: " + line);
                            continue;
                        }
                        
                        try {
                            String ingredientName = data[0].trim();
                            int currentStock = Integer.parseInt(data[1].trim());
                            int lowStockThreshold = Integer.parseInt(data[2].trim());
                            
                            Vector<Object> rowData = new Vector<>();
                            rowData.add(ingredientName);
                            rowData.add(currentStock);
                            rowData.add(lowStockThreshold);
                            tableModel.addRow(rowData);
                            rowCount++;
                            
                        } catch (NumberFormatException e) {
                            System.err.println("Skipping invalid row: " + line);
                        }
                    }
                }

                inventoryPanel.getInventoryTable().setModel(tableModel);
                highlightLowStockItems();
                System.out.println("Successfully loaded " + rowCount + " inventory items");
            }
            reader.close();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(inventoryPanel, "Error loading inventory: " + e.getMessage(), 
                                         "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void syncWithRecipes() {
        try {
            // Read all ingredients from recipes
            Set<String> recipeIngredients = new HashSet<>();
            File recipeFileObj = new File(recipeFile);
            
            if (recipeFileObj.exists()) {
                BufferedReader recipeReader = new BufferedReader(new FileReader(recipeFile));

                String headerLine = recipeReader.readLine();
                if (headerLine != null) {
                    String[] headers = headerLine.split(",");
                    // Skip "Product Name" (index 0), rest are ingredients
                    for (int i = 1; i < headers.length; i++) {
                        recipeIngredients.add(headers[i].trim().toLowerCase());
                    }
                }
                recipeReader.close();
            }

            // Read existing inventory
            Map<String, String[]> existingInventory = new HashMap<>();
            File inventoryFileObj = new File(dataFile);
            if (inventoryFileObj.exists()) {
                BufferedReader inventoryReader = new BufferedReader(new FileReader(dataFile));
                // Skip header
                String header = inventoryReader.readLine();
               
                String line;
                while ((line = inventoryReader.readLine()) != null) {
                    line = line.trim();
                    if(line.isEmpty()) continue;

                    String[] data = line.split(",");
                    if (data.length >= 3) {
                        String ingredientName = data[0].trim().toLowerCase(); // Use lowercase for consistency
                        // Skip if this is a header row
                        if (!ingredientName.equalsIgnoreCase("ingredient name")) {
                            existingInventory.put(ingredientName, new String[]{data[1].trim(), data[2].trim()});
                        }
                    }
                }
                inventoryReader.close();
            }

            // Add missing ingredients to inventory
            boolean needsUpdate = false;
            for (String ingredient : recipeIngredients) {
                if (!existingInventory.containsKey(ingredient)) {
                    existingInventory.put(ingredient, new String[]{"0", "10"}); // Default: stock=0, threshold=10
                    needsUpdate = true;
                    System.out.println("Added new ingredient: " + ingredient);
                }
            }

            // Save updated inventory if needed
            if (needsUpdate) {
                saveInventoryData(existingInventory);
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(inventoryPanel, "Error syncing with recipes: " + e.getMessage(), 
                                         "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void saveInventoryData(Map<String, String[]> inventoryData) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(dataFile));
            writer.println("Ingredient Name,Current Stock,Low Stock Threshold");

            // Sort ingredients alphabetically for better organization
            List<String> sortedIngredients = new ArrayList<>(inventoryData.keySet());
            Collections.sort(sortedIngredients);

            for (String ingredient : sortedIngredients) {
                String[] values = inventoryData.get(ingredient);
                writer.println(ingredient + "," + values[0] + "," + values[1]);
            }
            writer.close();
            
            System.out.println("Inventory saved with " + inventoryData.size() + " items");

        } catch (IOException e) {
            JOptionPane.showMessageDialog(inventoryPanel, "Error saving inventory: " + e.getMessage(), 
                                         "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createDefaultInventory() {
        try {
            // Create default inventory based on recipe ingredients
            syncWithRecipes();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(inventoryPanel, "Error creating default inventory: " + e.getMessage(), 
                                         "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void highlightLowStockItems() {
        inventoryPanel.getInventoryTable().setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    try {
                        int stock = Integer.parseInt(table.getValueAt(row, 1).toString());
                        int threshold = Integer.parseInt(table.getValueAt(row, 2).toString());
                        
                        if (stock < threshold) {
                            c.setBackground(Color.PINK);
                            c.setForeground(Color.RED);
                        } else {
                            c.setBackground(Color.WHITE);
                            c.setForeground(Color.BLACK);
                        }
                    } catch (Exception e) {
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                    }
                }
                return c;
            }
        });
    }

    public static void addIngredientToInventory(String newIngredient) {
    try {
        File inventoryFile = new File("inventory.txt");
        Map<String, String[]> inventoryData = new HashMap<>();
        
        if (inventoryFile.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(inventoryFile));
            String header = reader.readLine();
            
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 3) {
                    String ingredient = data[0].trim().toLowerCase();
                    if (!ingredient.equalsIgnoreCase("ingredient name")) {
                        inventoryData.put(ingredient, new String[]{data[1].trim(), data[2].trim()});
                    }
                }
            }
            reader.close();
        }
        
        String ingredientLower = newIngredient.toLowerCase();
        if (!inventoryData.containsKey(ingredientLower)) {
            inventoryData.put(ingredientLower, new String[]{"0", "10"});
            
            // Save using existing saveInventoryData method
            // (you'd need to make saveInventoryData public or package-private)
            saveInventoryData(inventoryData);
        }
        
    } catch (IOException e) {
        System.err.println("Error adding ingredient to inventory: " + e.getMessage());
    }
}

    // Method to update inventory when orders are placed (to be called from Order System)
    public boolean processOrder(Map<String, Integer> requiredIngredients) {
        try {
            // Read current inventory
            Map<String, String[]> inventoryData = readInventoryData();
            
            // Check if sufficient stock exists
            for (Map.Entry<String, Integer> entry : requiredIngredients.entrySet()) {
                String ingredient = entry.getKey();
                int required = entry.getValue();
                
                if (inventoryData.containsKey(ingredient)) {
                    int currentStock = Integer.parseInt(inventoryData.get(ingredient)[0]);
                    if (currentStock < required) {
                        JOptionPane.showMessageDialog(inventoryPanel, 
                            "Insufficient stock for " + ingredient + 
                            ". Required: " + required + ", Available: " + currentStock,
                            "Stock Error", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                } else {
                    JOptionPane.showMessageDialog(inventoryPanel, 
                        "Ingredient not found in inventory: " + ingredient,
                        "Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
            
            // Deduct from inventory
            for (Map.Entry<String, Integer> entry : requiredIngredients.entrySet()) {
                String ingredient = entry.getKey();
                int required = entry.getValue();
                int currentStock = Integer.parseInt(inventoryData.get(ingredient)[0]);
                int newStock = currentStock - required;
                inventoryData.get(ingredient)[0] = String.valueOf(newStock);
            }
            
            // Save updated inventory
            saveInventoryData(inventoryData);
            
            // Reload table to show updated values and highlights
            loadInventory();
            
            // Check for low stock alerts
            checkLowStockAlerts(inventoryData);
            
            return true;
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(inventoryPanel, "Error processing order: " + e.getMessage(), 
                                         "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private Map<String, String[]> readInventoryData() throws IOException {
        Map<String, String[]> inventoryData = new HashMap<>();
        File file = new File(dataFile);
        
        if (file.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(dataFile));
            String header = reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 3) {
                    inventoryData.put(data[0], new String[]{data[1], data[2]});
                }
            }
            reader.close();
        }
        return inventoryData;
    }

    private void checkLowStockAlerts(Map<String, String[]> inventoryData) {
        StringBuilder alerts = new StringBuilder();
        for (Map.Entry<String, String[]> entry : inventoryData.entrySet()) {
            String ingredient = entry.getKey();
            int stock = Integer.parseInt(entry.getValue()[0]);
            int threshold = Integer.parseInt(entry.getValue()[1]);
            
            if (stock < threshold) {
                alerts.append("• ").append(ingredient)
                      .append(": ").append(stock).append(" (Threshold: ").append(threshold).append(")\n");
            }
        }
        
        if (alerts.length() > 0) {
            JOptionPane.showMessageDialog(inventoryPanel, 
                "LOW STOCK ALERTS:\n\n" + alerts.toString(),
                "Inventory Alert", JOptionPane.WARNING_MESSAGE);
        }
    }

    private class EditInventoryListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int selectedRow = inventoryPanel.getInventoryTable().getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(inventoryPanel, 
                                             "Please select an inventory item to edit", 
                                             "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String ingredientName = tableModel.getValueAt(selectedRow, 0).toString();
            int currentStock = Integer.parseInt(tableModel.getValueAt(selectedRow, 1).toString());
            int currentThreshold = Integer.parseInt(tableModel.getValueAt(selectedRow, 2).toString());

            // Create editing dialog
            JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(inventoryPanel), 
                                       "Edit Inventory", true);
            dialog.setLayout(new GridLayout(3, 2, 10, 10));
            dialog.setSize(300, 150);
            dialog.setLocationRelativeTo(inventoryPanel);

            dialog.add(new JLabel("Ingredient:"));
            JTextField nameField = new JTextField(ingredientName);
            nameField.setEditable(false);
            dialog.add(nameField);

            dialog.add(new JLabel("Current Stock:"));
            JTextField stockField = new JTextField(String.valueOf(currentStock));
            dialog.add(stockField);

            dialog.add(new JLabel("Low Stock Threshold:"));
            JTextField thresholdField = new JTextField(String.valueOf(currentThreshold));
            dialog.add(thresholdField);

            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton saveButton = new JButton("Save");
            JButton cancelButton = new JButton("Cancel");

            saveButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        int newStock = Integer.parseInt(stockField.getText().trim());
                        int newThreshold = Integer.parseInt(thresholdField.getText().trim());

                        // Update table
                        tableModel.setValueAt(newStock, selectedRow, 1);
                        tableModel.setValueAt(newThreshold, selectedRow, 2);

                        // Save to file
                        saveCurrentTableToFile();
                        
                        // Refresh highlights
                        highlightLowStockItems();
                        
                        dialog.dispose();
                        
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(dialog, "Please enter valid numbers for stock and threshold", 
                                                     "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            });

            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);

            dialog.add(buttonPanel);
            dialog.setVisible(true);
        }
    }

    private class RefreshListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            loadInventory();
        }
    }

    private void saveCurrentTableToFile() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(dataFile));
            writer.println("Ingredient Name,Current Stock,Low Stock Threshold");
            
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                writer.println(tableModel.getValueAt(i, 0) + "," + 
                              tableModel.getValueAt(i, 1) + "," + 
                              tableModel.getValueAt(i, 2));
            }
            writer.close();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(inventoryPanel, "Error saving inventory: " + e.getMessage(), 
                                         "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}