import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

public class OrderApplicationUI extends JFrame {

    private DefaultTableModel menuModel;
    private DefaultTableModel cartModel;
    private DefaultTableModel ordersHistoryModel;
    private JTable menuTable;
    private JTable cartTable;
    private JTable ordersTable;

    private JLabel lblCustomer;
    private JLabel lblTotal;
    private JTextArea lowStockText;
    private JPanel mainRightPanel;
    private JPanel ordersPanel;

    private DecimalFormat fmt = new DecimalFormat("#0.00");
    private FileManager fileManager;
    private OrderApplication controller;

    public OrderApplicationUI() {
        setTitle("Order Application - Beginner Version");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        fileManager = new FileManager();

        buildUI();
        loadApplicationData();

        // create controller after UI and files are loaded
        controller = new OrderApplication(this, fileManager);
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(420);
        add(split, BorderLayout.CENTER);

        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder("Menu Browser"));
        menuModel = new DefaultTableModel(new String[]{"Product", "Price"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        menuTable = new JTable(menuModel);
        left.add(new JScrollPane(menuTable), BorderLayout.CENTER);

        JButton addBtn = new JButton("Add to Cart");
        addBtn.addActionListener(e -> {
            if (controller != null) controller.addSelectedMenuToCart();
        });
        left.add(addBtn, BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(BorderFactory.createTitledBorder("Order Summary"));

        cartModel = new DefaultTableModel(new String[]{"Product", "Qty", "Price"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        cartTable = new JTable(cartModel);
        right.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        JPanel topControls = createTopControls();
        right.add(topControls, BorderLayout.NORTH);

        JPanel bottom = createBottomPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        right.add(bottom, BorderLayout.SOUTH);

        split.setLeftComponent(left);
        split.setRightComponent(right);
        mainRightPanel = right;
    }

    private JPanel createTopControls() {
        JPanel topControls = new JPanel();
        JButton removeBtn = new JButton("Remove Item");
        removeBtn.addActionListener(e -> { if (controller != null) controller.removeCartItem(); });
        JButton qtyBtn = new JButton("Update Quantity");
        qtyBtn.addActionListener(e -> { if (controller != null) controller.updateCartQuantity(); });
        JButton priceBtn = new JButton("Update Price");
        priceBtn.addActionListener(e -> { if (controller != null) controller.updateMenuPrice(); });
        topControls.add(removeBtn);
        topControls.add(qtyBtn);
        topControls.add(priceBtn);
        return topControls;
    }

    private JPanel createBottomPanel() {
        JPanel bottom = new JPanel();
        JPanel details = new JPanel(new GridLayout(2, 2));
        details.setBorder(BorderFactory.createTitledBorder("Order Details"));
        details.add(new JLabel("Customer ID:"));
        lblCustomer = new JLabel("---");
        details.add(lblCustomer);
        details.add(new JLabel("Total Price:"));
        lblTotal = new JLabel("$0.00");
        details.add(lblTotal);
        bottom.add(details);

        JPanel low = new JPanel(new BorderLayout());
        low.setBorder(BorderFactory.createTitledBorder("Low Stock Alert"));
        lowStockText = new JTextArea(5, 20);
        lowStockText.setEditable(false);
        lowStockText.setText("Low:\nNone");
        low.add(new JScrollPane(lowStockText), BorderLayout.CENTER);
        bottom.add(low);

        JPanel actions = new JPanel();
        JButton placeBtn = new JButton("Place Order");
        placeBtn.addActionListener(e -> { if (controller != null) controller.placeOrder(); });
        JButton viewOrdersBtn = new JButton("View Orders");
        viewOrdersBtn.addActionListener(e -> {
            if (controller != null) controller.showOrdersScreen();
        });
        actions.add(placeBtn);
        actions.add(viewOrdersBtn);
        bottom.add(actions);
        return bottom;
    }

    private void loadApplicationData() {
        fileManager.loadRecipes();
        fileManager.loadPrices();
        fileManager.loadInventory();
        loadMenuTable();
        updateTotalLabel();
    }

    // ---- methods used by controller ----
    public DefaultTableModel getMenuModel() { return menuModel; }
    public DefaultTableModel getCartModel() { return cartModel; }
    public DefaultTableModel getOrdersHistoryModel() { return ordersHistoryModel; }
    public JTable getMenuTable() { return menuTable; }
    public JTable getCartTable() { return cartTable; }
    public JTable getOrdersTable() { return ordersTable; }
    public JLabel getLblCustomer() { return lblCustomer; }
    public JLabel getLblTotal() { return lblTotal; }
    public JTextArea getLowStockText() { return lowStockText; }
    public DecimalFormat getFmt() { return fmt; }
    public FileManager getFileManager() { return fileManager; }
    public JPanel getMainRightPanel() { return mainRightPanel; }
    public JPanel getOrdersPanel() { return ordersPanel; }

    // Populate menu table from fileManager maps
    public void loadMenuTable() {
        menuModel.setRowCount(0);
        for (String prod : fileManager.getRecipeMap().keySet()) {
            double p = fileManager.getPriceMap().getOrDefault(prod, 0.00);
            menuModel.addRow(new Object[]{prod, "$" + fmt.format(p)});
        }
    }

    public void updateTotalLabel() {
        double total = 0;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            double price = safeParseDouble((String) cartModel.getValueAt(i, 2));
            int qty = (int) cartModel.getValueAt(i, 1);
            total += price * qty;
        }
        lblTotal.setText("$" + fmt.format(total));
    }

    public void cancelCart() {
        cartModel.setRowCount(0);
        lblCustomer.setText("---");
        lblTotal.setText("$0.00");
        lowStockText.setText("Low:\nNone");
    }

    // Orders screen build & load
    public void buildOrdersPanel() {
        ordersPanel = new JPanel(new BorderLayout());
        ordersPanel.setBorder(BorderFactory.createTitledBorder("Order Summary"));

        ordersHistoryModel = new DefaultTableModel(new String[]{"Customer ID", "Product", "Qty", "Total Price"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        ordersTable = new JTable(ordersHistoryModel);
        ordersPanel.add(new JScrollPane(ordersTable), BorderLayout.CENTER);

        JButton cancelOrderBtn = new JButton("Cancel Order");
        JButton backBtn = new JButton("Return");
        cancelOrderBtn.addActionListener(e -> { if (controller != null) controller.cancelSelectedOrder(); });
        backBtn.addActionListener(e -> returnToMain());

        JPanel bot = new JPanel();
        bot.add(cancelOrderBtn); bot.add(backBtn);
        ordersPanel.add(bot, BorderLayout.SOUTH);
    }

    public void showOrdersScreenInUI() {
        buildOrdersPanel();
        loadOrdersIntoTable();
        JSplitPane split = (JSplitPane) getContentPane().getComponent(0);
        split.setRightComponent(ordersPanel);
    }

    public void loadOrdersIntoTable() {
        ordersHistoryModel.setRowCount(0);
        List<String> lines = fileManager.readOrdersFileLines();
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            String[] p = line.split(",");
            if (p.length < 3) continue;
            String id = p[0];
            String items = p[1];
            String total = p[2];
            String[] parts = items.split("\\|");
            for (String it : parts) {
                String[] kv = it.split(":");
                if (kv.length == 2) {
                    ordersHistoryModel.addRow(new Object[]{id, kv[0], kv[1], "$" + total});
                }
            }
        }
    }

    public void returnToMain() {
        JSplitPane split = (JSplitPane) getContentPane().getComponent(0);
        split.setRightComponent(mainRightPanel);
    }

    // small helper
    private double safeParseDouble(String s) {
        try {
            String t = s.replace("$", "").trim();
            return Double.parseDouble(t);
        } catch (Exception e) {
            return 0.0;
        }
    }

    // main
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OrderApplicationUI().setVisible(true));
    }
}