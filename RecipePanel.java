import javax.swing.*;
import java.awt.*;

public class RecipePanel extends JPanel {
    private JTable recipeTable;
    private JButton addRecipeButton;
    private JButton addIngredientButton;
    private JButton editProductButton;

    public RecipePanel() {
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Create buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        addRecipeButton = new JButton("Add Recipe");
        addIngredientButton = new JButton("Add Ingredient");
        editProductButton = new JButton("Edit Product");

        buttonPanel.add(addRecipeButton);
        buttonPanel.add(addIngredientButton);
        buttonPanel.add(editProductButton);

        // Create table with scroll pane
        recipeTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(recipeTable);

        add(buttonPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    // Getters for the components
    public JTable getRecipeTable() {
        return recipeTable;
    }

    public JButton getAddRecipeButton() {
        return addRecipeButton;
    }

    public JButton getAddIngredientButton() {
        return addIngredientButton;
    }

    public JButton getEditProductButton() {
        return editProductButton;
    }
}