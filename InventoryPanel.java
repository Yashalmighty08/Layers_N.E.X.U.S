import javax.swing.*;
import java.awt.*;

public class InventoryPanel extends JPanel {
    private JTable inventoryTable;
    private JButton editInventoryButton;
    private JButton refreshButton;

    public InventoryPanel() {
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Create buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        editInventoryButton = new JButton("Edit Inventory");
        refreshButton = new JButton("Refresh");

        buttonPanel.add(editInventoryButton);
        buttonPanel.add(refreshButton);

        // Create table with scroll pane
        inventoryTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(inventoryTable);

        add(buttonPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    // Getters for the components
    public JTable getInventoryTable() {
        return inventoryTable;
    }

    public JButton getEditInventoryButton() {
        return editInventoryButton;
    }

    public JButton getRefreshButton() {
        return refreshButton;
    }
}