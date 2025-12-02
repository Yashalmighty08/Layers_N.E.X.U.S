import javax.swing.*;

public class RecipeManagementSystem extends JFrame {
    private RecipePanel recipePanel;
    private RecipeManagement recipeManagement;

    public RecipeManagementSystem() {
        initializeUI();
        recipeManagement = new RecipeManagement(recipePanel);
    }

    private void initializeUI() {
        setTitle("Recipe Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // Create and add the recipe panel
        recipePanel = new RecipePanel();
        add(recipePanel);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new RecipeManagementSystem().setVisible(true);
            }
        });
    }
}