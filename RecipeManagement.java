import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.List;

public class RecipeManagement {
    private RecipePanel recipePanel;
    private DefaultTableModel tableModel;
    private List<String> ingredients;
    private String dataFile = "recipes.txt";

    public RecipeManagement(RecipePanel panel) {
        this.recipePanel = panel;
        this.ingredients = new ArrayList<>();
        initializeListeners();
        loadRecipes();
    }

    private void initializeListeners() {
        recipePanel.getAddRecipeButton().addActionListener(new AddRecipeListener());
        recipePanel.getAddIngredientButton().addActionListener(new AddIngredientListener());
        recipePanel.getEditProductButton().addActionListener(new EditProductListener());
    }

    public void loadRecipes() {
        try {
            File file = new File(dataFile);
            if (!file.exists()) {
                createDefaultFile();
            }

            BufferedReader reader = new BufferedReader(new FileReader(dataFile));
            String headerLine = reader.readLine();
            
            if (headerLine != null) {
                String[] headers = headerLine.split(",");
                ingredients.clear();
                
                // First column is "Product Name", rest are ingredients
                for (int i = 1; i < headers.length; i++) {
                    ingredients.add(headers[i].trim());
                }
                
                // Create table model with index column
                Vector<String> columnNames = new Vector<>();
                columnNames.add("Index");
                columnNames.add("Product Name");
                columnNames.addAll(ingredients);

                tableModel = new DefaultTableModel(columnNames, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };

                // Read recipe data
                String line;
                int index = 1;
                while ((line = reader.readLine()) != null) {
                    String[] data = line.split(",");
                    Vector<Object> rowData = new Vector<>();
                    rowData.add(index++); // Index number
                    for (String value : data) {
                        rowData.add(value);
                    }
                    tableModel.addRow(rowData);
                }

                recipePanel.getRecipeTable().setModel(tableModel);
            }
            reader.close();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(recipePanel, "Error loading recipes: " + e.getMessage(), 
                                         "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createDefaultFile() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(dataFile));
            writer.println("Product Name,Flour,Sugar,Brown Sugar,Butter,Eggs,Chocolate Chips,Oats,Raisins,Peanut Butter,Vanilla,Baking Soda,Salt");
            writer.println("Chocolate Chip Cookie,200,150,100,150,2,200,0,0,0,5,5,2");
            writer.println("Oatmeal Raisin Cookie,180,100,120,130,2,0,150,120,0,5,5,2");
            writer.println("Peanut Butter Cookie,180,80,60,100,1,0,0,0,200,5,4,2");
            writer.println("Sugar Cookie,220,180,0,150,2,0,0,0,0,10,4,2");
            writer.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(recipePanel, "Error creating default file: " + e.getMessage(), 
                                         "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveRecipes() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(dataFile));
            
            // Write header
            writer.print("Product Name");
            for (String ingredient : ingredients) {
                writer.print("," + ingredient);
            }
            writer.println();

            // Write data
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                writer.print(tableModel.getValueAt(i, 1)); // Product Name
                for (int j = 2; j < tableModel.getColumnCount(); j++) {
                    writer.print("," + tableModel.getValueAt(i, j));
                }
                writer.println();
            }
            writer.close();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(recipePanel, "Error saving recipes: " + e.getMessage(), 
                                         "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class AddRecipeListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Create dialog for adding new recipe
            JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(recipePanel), 
                                       "Add New Recipe", true);
            dialog.setLayout(new BorderLayout());
            dialog.setSize(600, 600);
            dialog.setLocationRelativeTo(recipePanel);

            JPanel inputPanel = new JPanel(new GridLayout(ingredients.size() + 1, 2, 0, 0));

            JTextField productNameField = new JTextField();
            inputPanel.add(new JLabel("Product Name:"));
            inputPanel.add(productNameField);

            // Create input fields for each ingredient
            Map<String, JTextField> ingredientFields = new HashMap<>();
            for (String ingredient : ingredients) {
                inputPanel.add(new JLabel(ingredient + ":"));
                JTextField field = new JTextField("0");
                ingredientFields.put(ingredient, field);
                inputPanel.add(field);
            }

            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton saveButton = new JButton("Save");
            JButton cancelButton = new JButton("Cancel");

            saveButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String productName = productNameField.getText().trim();
                    if (productName.isEmpty()) {
                        JOptionPane.showMessageDialog(dialog, "Please enter a product name", 
                                                     "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // Create new row data
                    Vector<Object> rowData = new Vector<>();
                    rowData.add(tableModel.getRowCount() + 1); // Index
                    rowData.add(productName);

                    for (String ingredient : ingredients) {
                        String value = ingredientFields.get(ingredient).getText().trim();
                        try {
                            if (value.isEmpty()) value = "0";
                            rowData.add(value);
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(dialog, "Invalid quantity for " + ingredient, 
                                                         "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }

                    tableModel.addRow(rowData);
                    saveRecipes();
                    dialog.dispose();
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

            dialog.add(inputPanel, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
            dialog.setVisible(true);
        }
    }

    private class AddIngredientListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String ingredientName = JOptionPane.showInputDialog(recipePanel, 
                                                               "Enter new ingredient name:");
            if (ingredientName != null && !ingredientName.trim().isEmpty()) {
                ingredientName = ingredientName.trim();

                // Check case-insensitive duplicates
                boolean duplicateFound = false;
                for (String existingIngredient : ingredients) {
                    if (existingIngredient.equalsIgnoreCase(ingredientName)) {
                        duplicateFound = true;
                        break;
                    }
                }
                
                if (duplicateFound) {
                    JOptionPane.showMessageDialog(recipePanel,
                        "Ingredient '" + ingredientName + "' already exists!",
                        "Duplicate Ingredient", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // Also check existing column names in the table
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    String columnName = tableModel.getColumnName(i);
                    if (columnName.equalsIgnoreCase(ingredientName)) {
                        JOptionPane.showMessageDialog(recipePanel,
                            "Ingredient '" + ingredientName + "' already exists!",
                            "Duplicate Ingredient", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
                
                // Add to ingredients list
                ingredients.add(ingredientName);
                
                // Add column to table model
                tableModel.addColumn(ingredientName);
                
                // Set default value of 0 for all existing recipes
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    tableModel.setValueAt("0", i, tableModel.getColumnCount() - 1);
                }
                updateInventoryWithNewIngredient(ingredientName);
                
                saveRecipes();
            }
        }
    }

    private class EditProductListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Get selected row
            int selectedRow = recipePanel.getRecipeTable().getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(recipePanel, 
                                             "Please select a recipe to edit", 
                                             "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String productName = tableModel.getValueAt(selectedRow, 1).toString();

            // Create dialog for editing
            JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(recipePanel), 
                                       "Edit Recipe: " + productName, true);
            dialog.setLayout(new BorderLayout());
            dialog.setSize(400, 150);
            dialog.setLocationRelativeTo(recipePanel);

            JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));

            // ComboBox for selecting ingredient
            JComboBox<String> ingredientCombo = new JComboBox<>();
            for (int i = 2; i < tableModel.getColumnCount(); i++) {
                ingredientCombo.addItem(tableModel.getColumnName(i));
            }

            JTextField quantityField = new JTextField();
            inputPanel.add(new JLabel("Select Ingredient:"));
            inputPanel.add(ingredientCombo);
            inputPanel.add(new JLabel("New Quantity:"));
            inputPanel.add(quantityField);

            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton saveButton = new JButton("Save");
            JButton cancelButton = new JButton("Cancel");

            saveButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String selectedIngredient = (String) ingredientCombo.getSelectedItem();
                    String newQuantity = quantityField.getText().trim();

                    if (newQuantity.isEmpty()) {
                        JOptionPane.showMessageDialog(dialog, "Please enter a quantity", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    try {
                        // Find the column index for the selected ingredient
                        int columnIndex = -1;
                        for (int i = 2; i < tableModel.getColumnCount(); i++) {
                            if (tableModel.getColumnName(i).equals(selectedIngredient)) {
                                columnIndex = i;
                                break;
                            }
                        }

                        if (columnIndex != -1) {
                            tableModel.setValueAt(newQuantity, selectedRow, columnIndex);
                            saveRecipes();
                            dialog.dispose();
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(dialog, "Please enter a valid quantity", "Error", JOptionPane.ERROR_MESSAGE);
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

            dialog.add(inputPanel, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
            dialog.setVisible(true);
        }
    }

    private void updateInventoryWithNewIngredient(String newIngredient) {
    try {
        InventoryManagement.addIngredientToInventory(newIngredient);
    } catch (Exception e) {
        System.err.println("Could not update inventory with new ingredient: " + e.getMessage());
    }
}
}