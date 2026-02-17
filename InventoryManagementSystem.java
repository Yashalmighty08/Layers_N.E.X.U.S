import javax.swing.*;

public class InventoryManagementSystem extends JFrame {
    private InventoryPanel inventoryPanel;
    private InventoryManagement inventoryManagement;

    public InventoryManagementSystem() {
        initializeUI();
        inventoryManagement = new InventoryManagement(inventoryPanel);
    }

    private void initializeUI() {
        setTitle("Inventory Management System");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        // Create and add the inventory panel
        inventoryPanel = new InventoryPanel();
        add(inventoryPanel);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new InventoryManagementSystem().setVisible(true);
            }
        });
    }
}