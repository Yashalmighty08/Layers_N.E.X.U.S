//Order Appllication UI

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.*;
import java.awt.GridLayout;
import javax.swing.BorderFactory; 
public class OrderApplicationUI extends JFrame {

    // --- UI MODELS & COMPONENTS ---
    private DefaultTableModel menuModel;
    private DefaultTableModel orderModel;
     private DefaultTableModel ordersHistoryModel;
    private JTable menuTable, orderTable;
    private JLabel lblCustomerID, lblTotalPrice;
    private JTextArea lowStockArea;
    private JPanel rightPanelOrders;
    private JTable ordersHistoryTable;
    private JPanel rightPanelMain;

    // --- DATA STRUCTURES ---
    private Map<String, Recipe> recipes = new LinkedHashMap<>();
    private Map<String, InventoryItem> inventory = new LinkedHashMap<>();
    private DecimalFormat fmt = new DecimalFormat("#0.00");

    // --- FILES ---
    private static final Path RECIPES_FILE = Paths.get("recipes.txt");
    private static final Path INVENTORY_FILE = Paths.get("inventory.txt");
    private static final Path ORDERS_FILE = Paths.get("orders.txt");

   
    public OrderApplicationUI() {
        setTitle("Order Application");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setupUI();   // Build all UI components
        loadRecipes();
        loadInventory();
        loadMenu();
        updateTotal();
    }

    // ---------------------------------------------------------
    // BUILD UI
    // ---------------------------------------------------------
    private void setupUI() {
        setLayout(new BorderLayout());
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(420);
        add(split);

        // LEFT PANEL (MENU)
        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder("Menu Browser"));

        menuModel = new DefaultTableModel(new String[]{"Product", "Price"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        menuTable = new JTable(menuModel);
        left.add(new JScrollPane(menuTable), BorderLayout.CENTER);

        JButton addToCart = new JButton("Add to Cart");
        addToCart.addActionListener(e -> addMenuItemToOrder());
        left.add(addToCart, BorderLayout.SOUTH);
        split.setLeftComponent(left);

        // RIGHT PANEL (ORDER SUMMARY)
        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(BorderFactory.createTitledBorder("Order Summary"));

        orderModel = new DefaultTableModel(new String[]{"Product", "Qty", "Price"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        orderTable = new JTable(orderModel);
        right.add(new JScrollPane(orderTable), BorderLayout.CENTER);

        JPanel controls = new JPanel();
        JButton rmBtn = new JButton("Remove Item");     rmBtn.addActionListener(e -> removeOrderItem());
        JButton qtyBtn = new JButton("Update Quantity"); qtyBtn.addActionListener(e -> updateQuantity());
        JButton priceBtn = new JButton("Update Price");  priceBtn.addActionListener(e -> updateMenuPrice());

        controls.add(rmBtn); controls.add(qtyBtn); controls.add(priceBtn);
        right.add(controls, BorderLayout.NORTH);

        // LOWER RIGHT PANEL
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));

        JPanel details = new JPanel(new GridLayout(2, 2));
        details.setBorder(BorderFactory.createTitledBorder("Order Details"));
        lblCustomerID = new JLabel("---");
        lblTotalPrice = new JLabel("$0.00");
        details.add(new JLabel("Customer ID:")); details.add(lblCustomerID);
        details.add(new JLabel("Total Price:")); details.add(lblTotalPrice);
        bottom.add(details);

        JPanel low = new JPanel(new BorderLayout());
        low.setBorder(BorderFactory.createTitledBorder("Low Stock Alert"));
        lowStockArea = new JTextArea(5, 20);
        lowStockArea.setEditable(false);
        lowStockArea.setText("Low:\nNone");
        low.add(new JScrollPane(lowStockArea), BorderLayout.CENTER);
        bottom.add(low);

        JPanel actions = new JPanel();
        JButton placeOrder = new JButton("Place Order"); placeOrder.addActionListener(e -> placeOrder());
        JButton viewOrders = new JButton("View Orders");     viewOrders.addActionListener(e -> showOrdersPanel());
        actions.add(placeOrder); actions.add(viewOrders);
        bottom.add(actions);

        right.add(bottom, BorderLayout.SOUTH);
        split.setRightComponent(right);
        rightPanelMain = right;
    }

    // ---------------------------------------------------------
    // LOAD FILES
    // ---------------------------------------------------------
    private void loadRecipes() {
        recipes.clear();
        if (!Files.exists(RECIPES_FILE)) return;

        try (BufferedReader br = Files.newBufferedReader(RECIPES_FILE)) {
            String header = br.readLine();
            if (header == null) return;
            String[] cols = header.split(",");
            List<String> ingNames = new ArrayList<>();
            for (int i = 1; i < cols.length; i++) ingNames.add(cols[i].trim().toLowerCase());

            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length == 0) continue;
                Recipe r = new Recipe(p[0]);
                for (int i = 1; i < p.length && i - 1 < ingNames.size(); i++) {
                    r.ingredients.put(ingNames.get(i - 1), parseInt(p[i]));
                }
                recipes.put(r.name, r);
            }
        } catch (Exception ignored) {}
    }

    private void loadInventory() {
        inventory.clear();
        if (!Files.exists(INVENTORY_FILE)) return;

        try (BufferedReader br = Files.newBufferedReader(INVENTORY_FILE)) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length < 3) continue;
                String name = p[0].trim().toLowerCase();
                inventory.put(name, new InventoryItem(name, parseInt(p[1]), parseInt(p[2])));
            }
        } catch (Exception ignored) {}
    }

    private void loadMenu() {
        menuModel.setRowCount(0);
        for (String prod : recipes.keySet()) menuModel.addRow(new Object[]{prod, "$0.00"});
    }

    // ---------------------------------------------------------
    // ORDER ACTIONS
    // ---------------------------------------------------------
    private void addMenuItemToOrder() {
        int row = menuTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a product first.");
            return;
        }
        String product = (String) menuModel.getValueAt(row, 0);
        double price  = parseDouble((String) menuModel.getValueAt(row, 1));

        // If product already exists, increase quantity
        for (int i = 0; i < orderModel.getRowCount(); i++) {
            if (orderModel.getValueAt(i, 0).equals(product)) {
                int q = (int) orderModel.getValueAt(i, 1);
                orderModel.setValueAt(q + 1, i, 1);
                updateTotal();
                return;
            }
        }
        orderModel.addRow(new Object[]{product, 1, "$" + fmt.format(price)});
        updateTotal();
    }

    private void removeOrderItem() {
        int row = orderTable.getSelectedRow();
        if (row >= 0) {
            orderModel.removeRow(row);
            updateTotal();
        }
    }

    private void updateQuantity() {
        int row = orderTable.getSelectedRow();
        if (row < 0) return;

        String name = (String) orderModel.getValueAt(row, 0);
        int curr = (int) orderModel.getValueAt(row, 1);
        String s = JOptionPane.showInputDialog(this, "Quantity for " + name + ":", curr);
        if (s == null) return;

        try {
            int q = Integer.parseInt(s.trim());
            if (q > 0) {
                orderModel.setValueAt(q, row, 1);
                updateTotal();
            }
        } catch (Exception ignored) {}
    }

    private void updateMenuPrice() {
        int row = menuTable.getSelectedRow();
        if (row < 0) return;

        String product = (String) menuModel.getValueAt(row, 0);
        String s = JOptionPane.showInputDialog(this, "New price for " + product + ":", "0.00");
        if (s == null) return;

        try {
            double p = Double.parseDouble(s.trim());
            menuModel.setValueAt("$" + fmt.format(p), row, 1);
            updateTotal();
        } catch (Exception ignored) {}
    }

    private void cancelOrder() {
        orderModel.setRowCount(0);
        lblCustomerID.setText("---");
        lblTotalPrice.setText("$0.00");
        lowStockArea.setText("Low:\nNone");
    }

    private void placeOrder() {
        if (orderModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Order is empty.");
            return;
        }

        Map<String, Integer> orderItems = new LinkedHashMap<>();
        double total = 0;
        for (int i = 0; i < orderModel.getRowCount(); i++) {
            String prod = (String) orderModel.getValueAt(i, 0);
            int qty = (int) orderModel.getValueAt(i, 1);
            double price = parseDouble((String) orderModel.getValueAt(i, 2));
            orderItems.put(prod, qty);
            total += qty * price;
        }

        // Compute ingredients required
        Map<String, Integer> needed = computeNeeds(orderItems);
        List<String> lowList = new ArrayList<>();
        List<String> insufficient = new ArrayList<>();

        for (String ing : needed.keySet()) {
            int req = needed.get(ing);
            InventoryItem item = inventory.get(ing);
            if (item == null || item.stock < req) {
                insufficient.add(ing);
            } else if (item.stock - req < item.low) {
                lowList.add(ing);
            }
        }

        // Display low/insufficient
        StringBuilder sb = new StringBuilder("Low:\n");
        if (lowList.isEmpty() && insufficient.isEmpty()) sb.append("None");
        else {
            insufficient.forEach(x -> sb.append("- Insufficient: ").append(x).append("\n"));
            lowList.forEach(x -> sb.append("- Low: ").append(x).append("\n"));
        }
        lowStockArea.setText(sb.toString());

        if (JOptionPane.showConfirmDialog(this, "Place order?", "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            return;

        String id = generateID();
        lblCustomerID.setText(id);
        lblTotalPrice.setText("$" + fmt.format(total));

        saveOrder(id, orderItems, total);
        deductInventory(needed);
        saveInventory();

        JOptionPane.showMessageDialog(this, "Order placed! ID: " + id);
        cancelOrder();
    }
    
    private void buildOrdersPanel() {
        rightPanelOrders= new JPanel(new BorderLayout());
        rightPanelOrders.setBorder(BorderFactory.createTitledBorder("Order Summary"));

        ordersHistoryModel = new DefaultTableModel(new String[]{"Customer ID", "Product", "Qty", "Total Price"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        ordersHistoryTable= new JTable(ordersHistoryModel);
        rightPanelOrders.add(new JScrollPane(ordersHistoryTable), BorderLayout.CENTER);

        JButton cancel = new JButton("Cancel Order");
        JButton back = new JButton("Return");

        cancel.addActionListener(e -> cancelSelectedOrder());
        back.addActionListener(e -> returnToMainPanel());

        JPanel bottom = new JPanel();
        bottom.add(cancel);
        bottom.add(back);
        rightPanelOrders.add(bottom, BorderLayout.SOUTH);
    }
    
     private void showOrdersPanel() {
        buildOrdersPanel();
        loadOrdersIntoTable();
        JSplitPane split = (JSplitPane)getContentPane().getComponent(0);
        split.setRightComponent(rightPanelOrders);
    }
    
    private void returnToMainPanel() {
        JSplitPane split = (JSplitPane)getContentPane().getComponent(0);
        split.setRightComponent(rightPanelMain);
    }
    
        private void loadOrdersIntoTable() {
        ordersHistoryModel.setRowCount(0);
        try {
            List<String> lines = Files.readAllLines(ORDERS_FILE);
            for (String line : lines) {
                String[] p = line.split(",");
                if (p.length < 3) continue;

                String id = p[0];
                String items = p[1];
                String total = p[2];

                String[] parts = items.split("\\|");
                for (String item : parts) {
                    String[] kv = item.split(":");
                    if (kv.length == 2) {
                        ordersHistoryModel.addRow(new Object[]{id, kv[0], kv[1], "$" + total});
                    }
                }
            }
        } catch (Exception ignored) {}
    }
    
        private void cancelSelectedOrder() {
        int row = ordersHistoryTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an order.");
            return;
        }

        String id = (String) ordersHistoryModel.getValueAt(row, 0);

        try {
            List<String> lines = Files.readAllLines(ORDERS_FILE);
            List<String> updated = new ArrayList<>();

            for (String line : lines) {
                if (line.startsWith(id + ",")) {
                    restoreInventoryFromOrder(line);
                    continue;
                }
                updated.add(line);
            }

            Files.write(ORDERS_FILE, updated);
            loadOrdersIntoTable();
            JOptionPane.showMessageDialog(this, "Order canceled.");

        } catch (Exception ignored) {}
    }
    
private void restoreInventoryFromOrder(String line) {
    try {
        String[] p = line.split(",");
        if (p.length < 4) return;

        String items = p[1];
        String status = p[3].trim();

        // Only restore inventory if order was incomplete
        if (!status.equalsIgnoreCase("Incomplete")) {
            return;
        }

        for (String item : items.split("\\|")) {
            String[] kv = item.split(":");
            if (kv.length != 2) continue;

            String productName = kv[0];
            int qty = Integer.parseInt(kv[1]);

            Recipe r = recipes.get(productName);
            if (r == null) continue;

            // Restore ingredients used by this product
            for (String ing : r.ingredients.keySet()) {
                InventoryItem inv = inventory.get(ing);
                if (inv != null) {
                    inv.stock += r.ingredients.get(ing) * qty;
                }
            }
        }

        saveInventory();

    } catch (Exception ignored) {}
}


    // ---------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------
    private Map<String, Integer> computeNeeds(Map<String, Integer> items) {
        Map<String, Integer> req = new HashMap<>();
        for (String prod : items.keySet()) {
            Recipe r = recipes.get(prod);
            if (r == null) continue;
            int qty = items.get(prod);
            for (String ing : r.ingredients.keySet()) {
                int amount = r.ingredients.get(ing) * qty;
                req.put(ing, req.getOrDefault(ing, 0) + amount);
            }
        }
        return req;
    }

    private void saveOrder(String id, Map<String, Integer> items, double total) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(id).append(",");

            boolean first = true;
            for (String p : items.keySet()) {
                if (!first) sb.append("|");
                sb.append(p).append(":" + items.get(p));
                first = false;
            }

            sb.append(",").append(fmt.format(total)).append(",Incomplete\n");
            Files.write(ORDERS_FILE, sb.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
    }

    private void deductInventory(Map<String, Integer> needed) {
        for (String ing : needed.keySet()) {
            InventoryItem it = inventory.get(ing);
            if (it != null) it.stock -= needed.get(ing);
        }
    }

    private void saveInventory() {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("Ingredient,Stock,Low");
            for (String ing : inventory.keySet()) {
                InventoryItem it = inventory.get(ing);
                lines.add(it.name + "," + it.stock + "," + it.low);
            }
            Files.write(INVENTORY_FILE, lines);
        } catch (Exception ignored) {}
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim()); 
        } 
        catch (Exception e) { return 0; }
    }
    
    private double parseDouble(String s) {
        try { return Double.parseDouble(s.replace("$", "").trim()); } catch (Exception e) { return 0; }
    }

    private String generateID() {
        int count = 1;
        try {
            List<String> all = Files.readAllLines(ORDERS_FILE);
            count = all.size() + 1;
        } catch (Exception ignored) {}
        return String.format("C%04d", count);
    }

    private void updateTotal() {
        double total = 0;
        for (int i = 0; i < orderModel.getRowCount(); i++) {
            double price = parseDouble((String) orderModel.getValueAt(i, 2));
            int qty = (int) orderModel.getValueAt(i, 1);
            total += price * qty;
        }
        lblTotalPrice.setText("$" + fmt.format(total));
    }

    // ---------------------------------------------------------
    // DATA CLASSES
    // ---------------------------------------------------------
    private static class Recipe {
        String name;
        Map<String, Integer> ingredients = new LinkedHashMap<>();
        Recipe(String n) { name = n; }
    }

    private static class InventoryItem {
        String name;
        int stock, low;
        InventoryItem(String n, int s, int l) { name = n; stock = s; low = l; }
    }

    // ---------------------------------------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OrderApplicationUI().setVisible(true));
    }
}
