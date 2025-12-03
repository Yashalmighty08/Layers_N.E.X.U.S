import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.*;
import java.awt.BorderLayout;
import java.awt.GridLayout;

public class OrderApplicationUI extends JFrame {

    // ---------------- UI components ----------------
    private DefaultTableModel menuModel;
    private DefaultTableModel cartModel;
    private DefaultTableModel ordersHistoryModel;
    private JTable menuTable;
    private JTable cartTable;
    private JTable ordersTable;

    private JLabel lblCustomer;
    private JLabel lblTotal;
    private JTextArea lowStockText;

    // Panels
    private JPanel mainRightPanel;
    private JPanel ordersPanel;

    // ---------------- data ----------------
    // Keep names simple
    private Map<String, Recipe> recipeMap = new LinkedHashMap<>();
    private Map<String, InventoryItem> inventoryMap = new LinkedHashMap<>();
    private Map<String, Double> priceMap = new LinkedHashMap<>();

    private DecimalFormat fmt = new DecimalFormat("#0.00");

    // files
    private static final Path RECIPES_FILE = Paths.get("recipes.txt");
    private static final Path INVENTORY_FILE = Paths.get("inventory.txt");
    private static final Path ORDERS_FILE = Paths.get("orders.txt");
    private static final Path PRICES_FILE = Paths.get("prices.txt");

    public OrderApplicationUI() {
        setTitle("Order Application - Beginner Version");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        buildUI();
        // load data in a simple order
        loadRecipes();
        loadPrices();
        loadInventory();
        loadMenuTable();
        updateTotalLabel();
    }

    // ---------------- UI BUILD ----------------
    private void buildUI() {
        setLayout(new BorderLayout());
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(420);
        add(split, BorderLayout.CENTER);

        // LEFT: menu list and add button
        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder("Menu Browser"));
        menuModel = new DefaultTableModel(new String[]{"Product", "Price"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        menuTable = new JTable(menuModel);
        left.add(new JScrollPane(menuTable), BorderLayout.CENTER);

        JButton addBtn = new JButton("Add to Cart");
        addBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addSelectedMenuToCart();
            }
        });
        left.add(addBtn, BorderLayout.SOUTH);

        // RIGHT: cart and order controls
        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(BorderFactory.createTitledBorder("Order Summary"));

        cartModel = new DefaultTableModel(new String[]{"Product", "Qty", "Price"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        cartTable = new JTable(cartModel);
        right.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        // simple top controls (remove, qty, price)
        JPanel topControls = new JPanel();
        JButton removeBtn = new JButton("Remove Item");
        removeBtn.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent e){ removeCartItem(); }});
        JButton qtyBtn = new JButton("Update Quantity");
        qtyBtn.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent e){ updateCartQuantity(); }});
        JButton priceBtn = new JButton("Update Price");
        priceBtn.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent e){ updateMenuPrice(); }});
        topControls.add(removeBtn); topControls.add(qtyBtn); topControls.add(priceBtn);
        right.add(topControls, BorderLayout.NORTH);

        // bottom area: order details, low stock, actions
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));

        JPanel details = new JPanel(new GridLayout(2,2));
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
        placeBtn.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent e){ placeOrder(); }});
        JButton viewOrdersBtn = new JButton("View Orders");
        viewOrdersBtn.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent e){ showOrdersScreen(); }});
        actions.add(placeBtn); actions.add(viewOrdersBtn);
        bottom.add(actions);

        right.add(bottom, BorderLayout.SOUTH);

        split.setLeftComponent(left);
        split.setRightComponent(right);
        mainRightPanel = right;
    }

    // ---------------- LOAD FILES ----------------
    // Very simple parsing, beginner-friendly style

    private void loadRecipes() {
        recipeMap.clear();
        if (!Files.exists(RECIPES_FILE)) {
            // no recipes file - nothing to do
            return;
        }
        try (BufferedReader br = Files.newBufferedReader(RECIPES_FILE)) {
            String header = br.readLine();
            if (header == null) return;

            // header like: Product,ing1,ing2,...
            String[] cols = header.split(",");
            List<String> ingredientNames = new ArrayList<>();
            for (int i = 1; i < cols.length; i++) {
                ingredientNames.add(cols[i].trim().toLowerCase());
            }

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");
                String product = parts[0].trim();
                Recipe r = new Recipe(product);
                for (int i = 1; i < parts.length && i - 1 < ingredientNames.size(); i++) {
                    int amt = safeParseInt(parts[i]);
                    r.ingredients.put(ingredientNames.get(i - 1), amt);
                }
                recipeMap.put(product, r);
            }
        } catch (Exception e) {
            // beginner style: print stack for debugging
            e.printStackTrace();
        }
    }

    private void loadInventory() {
        inventoryMap.clear();
        if (!Files.exists(INVENTORY_FILE)) return;
        try (BufferedReader br = Files.newBufferedReader(INVENTORY_FILE)) {
            String header = br.readLine(); // skip header if present
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",");
                if (p.length < 3) continue;
                String name = p[0].trim().toLowerCase();
                int stock = safeParseInt(p[1]);
                int low = safeParseInt(p[2]);
                inventoryMap.put(name, new InventoryItem(name, stock, low));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Prices: simpler approach - create file if missing, ensure every recipe has a price entry
    private void loadPrices() {
        priceMap.clear();

        if (!Files.exists(PRICES_FILE)) {
            // create file with defaults (0.00)
            try {
                List<String> lines = new ArrayList<>();
                lines.add("Product,Price");
                for (String prod : recipeMap.keySet()) {
                    lines.add(prod + ",0.00");
                    priceMap.put(prod, 0.00);
                }
                Files.write(PRICES_FILE, lines);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        try (BufferedReader br = Files.newBufferedReader(PRICES_FILE)) {
            String header = br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",");
                if (p.length < 2) continue;
                String prod = p[0].trim();
                double price = safeParseDouble(p[1]);
                priceMap.put(prod, price);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Ensure all recipes have a price entry (default 0.00)
        for (String prod : recipeMap.keySet()) {
            if (!priceMap.containsKey(prod)) {
                priceMap.put(prod, 0.00);
            }
        }

        // If some products were missing in file, rewrite to include them
        try {
            List<String> fileLines = Files.exists(PRICES_FILE) ? Files.readAllLines(PRICES_FILE) : new ArrayList<>();
            if (fileLines.size() - 1 < recipeMap.size()) {
                savePrices();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadMenuTable() {
        menuModel.setRowCount(0);
        for (String prod : recipeMap.keySet()) {
            double p = priceMap.getOrDefault(prod, 0.00);
            menuModel.addRow(new Object[]{prod, "$" + fmt.format(p)});
        }
    }

    // ---------------- SAVE prices ----------------
    private void savePrices() {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("Product,Price");
            for (String prod : priceMap.keySet()) {
                lines.add(prod + "," + fmt.format(priceMap.get(prod)));
            }
            Files.write(PRICES_FILE, lines);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------- CART ACTIONS ----------------
    private void addSelectedMenuToCart() {
        int row = menuTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a product first.");
            return;
        }
        String product = (String) menuModel.getValueAt(row, 0);
        String priceStr = (String) menuModel.getValueAt(row, 1);
        double price = safeParseDouble(priceStr);

        // If exists in cart, increment qty
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            if (cartModel.getValueAt(i, 0).equals(product)) {
                int q = (int) cartModel.getValueAt(i, 1);
                cartModel.setValueAt(q + 1, i, 1);
                updateTotalLabel();
                return;
            }
        }

        cartModel.addRow(new Object[]{product, 1, "$" + fmt.format(price)});
        updateTotalLabel();
    }

    private void removeCartItem() {
        int row = cartTable.getSelectedRow();
        if (row >= 0) {
            cartModel.removeRow(row);
            updateTotalLabel();
        }
    }

    private void updateCartQuantity() {
        int row = cartTable.getSelectedRow();
        if (row < 0) return;
        String prod = (String) cartModel.getValueAt(row, 0);
        int curr = (int) cartModel.getValueAt(row, 1);
        String s = JOptionPane.showInputDialog(this, "Quantity for " + prod + ":", curr);
        if (s == null) return;
        try {
            int q = Integer.parseInt(s.trim());
            if (q > 0) {
                cartModel.setValueAt(q, row, 1);
                updateTotalLabel();
            } else {
                // ignore zero or negative
            }
        } catch (Exception e) {
            // ignore parse errors
        }
    }

    // Update menu price: simple dialog and save
    private void updateMenuPrice() {
        int row = menuTable.getSelectedRow();
        if (row < 0) return;
        String prod = (String) menuModel.getValueAt(row, 0);
        double cur = priceMap.getOrDefault(prod, 0.00);
        String s = JOptionPane.showInputDialog(this, "New price for " + prod + ":", fmt.format(cur));
        if (s == null) return;
        try {
            double p = Double.parseDouble(s.trim());
            priceMap.put(prod, p);
            menuModel.setValueAt("$" + fmt.format(p), row, 1);
            savePrices();
            updateTotalLabel();
        } catch (Exception e) {
            // ignore
        }
    }

    private void cancelCart() {
        cartModel.setRowCount(0);
        lblCustomer.setText("---");
        lblTotal.setText("$0.00");
        lowStockText.setText("Low:\nNone");
    }

    // ---------------- PLACE ORDER ----------------
    private void placeOrder() {
        if (cartModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Order is empty.");
            return;
        }

        // build order items map product->qty and compute total price
        Map<String, Integer> orderItems = new LinkedHashMap<>();
        double total = 0;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            String prod = (String) cartModel.getValueAt(i, 0);
            int qty = (int) cartModel.getValueAt(i, 1);
            double price = safeParseDouble((String) cartModel.getValueAt(i, 2));
            orderItems.put(prod, qty);
            total += price * qty;
        }

        // compute needed ingredients
        Map<String, Integer> needed = computeNeededIngredients(orderItems);

        // check inventory - build low and insufficient lists
        List<String> lowList = new ArrayList<>();
        List<String> insufficient = new ArrayList<>();

        for (String ing : needed.keySet()) {
            int req = needed.get(ing);
            InventoryItem inv = inventoryMap.get(ing);
            if (inv == null || inv.stock <= 0 || inv.stock < req) {
                insufficient.add(ing);
            } else if (inv.stock - req < inv.low) {
                lowList.add(ing);
            }
        }
        
        // STOP ORDER if anything is low or insufficient
        if (!insufficient.isEmpty() || !lowList.isEmpty()) {
            StringBuilder msg = new StringBuilder("Order cannot be placed due to stock issues:\n\n");

        for (String s : insufficient) {
            msg.append("Insufficient stock: ").append(s).append("\n");
        }

        for (String s : lowList) {
            msg.append("⚠ Low stock limit reached: ").append(s).append("\n");
        }

        JOptionPane.showMessageDialog(this, msg.toString(), "Order Denied", JOptionPane.ERROR_MESSAGE);
        return;  
        }


        // display low/insufficient in text area
        StringBuilder sb = new StringBuilder("Low:\n");
        if (lowList.isEmpty() && insufficient.isEmpty()) {
            sb.append("None");
        } else {
            for (String s : insufficient) {
                sb.append("- Insufficient: ").append(s).append("\n");
            }
            for (String s : lowList) {
                sb.append("- Low: ").append(s).append("\n");
            }
        }
        lowStockText.setText(sb.toString());

        int confirm = JOptionPane.showConfirmDialog(this, "Place order?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String id = generateCustomerID();
        lblCustomer.setText(id);
        lblTotal.setText("$" + fmt.format(total));

        // save order, deduct inventory, save files
        saveOrder(id, orderItems, total);
        deductInventory(needed);
        saveInventory();

        JOptionPane.showMessageDialog(this, "Order placed! ID: " + id);
        cancelCart();
    }

    // ---------------- ORDERS SCREEN ----------------
    private void buildOrdersPanel() {
        ordersPanel = new JPanel(new BorderLayout());
        ordersPanel.setBorder(BorderFactory.createTitledBorder("Order Summary"));

        ordersHistoryModel = new DefaultTableModel(new String[]{"Customer ID", "Product", "Qty", "Total Price"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        ordersTable = new JTable(ordersHistoryModel);
        ordersPanel.add(new JScrollPane(ordersTable), BorderLayout.CENTER);

        JButton cancelOrderBtn = new JButton("Cancel Order");
        JButton backBtn = new JButton("Return");
        cancelOrderBtn.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent e){ cancelSelectedOrder(); }});
        backBtn.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent e){ returnToMain(); }});

        JPanel bot = new JPanel();
        bot.add(cancelOrderBtn); bot.add(backBtn);
        ordersPanel.add(bot, BorderLayout.SOUTH);
    }

    private void showOrdersScreen() {
        buildOrdersPanel();
        loadOrdersIntoTable();
        JSplitPane split = (JSplitPane) getContentPane().getComponent(0);
        split.setRightComponent(ordersPanel);
    }

    private void returnToMain() {
        JSplitPane split = (JSplitPane) getContentPane().getComponent(0);
        split.setRightComponent(mainRightPanel);
    }

    private void loadOrdersIntoTable() {
        ordersHistoryModel.setRowCount(0);
        if (!Files.exists(ORDERS_FILE)) return;
        try {
            List<String> lines = Files.readAllLines(ORDERS_FILE);
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",");
                if (p.length < 3) continue;
                String id = p[0];
                String items = p[1];
                String total = p[2];

                // items are product:qty|product2:qty2
                String[] parts = items.split("\\|");
                for (String it : parts) {
                    String[] kv = it.split(":");
                    if (kv.length == 2) {
                        ordersHistoryModel.addRow(new Object[]{id, kv[0], kv[1], "$" + total});
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Cancel selected order (remove from file), restore inventory only if status was Incomplete
    private void cancelSelectedOrder() {
        int row = ordersTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an order.");
            return;
        }
        String id = (String) ordersHistoryModel.getValueAt(row, 0);

        try {
            List<String> lines = Files.readAllLines(ORDERS_FILE);
            List<String> keep = new ArrayList<>();
            for (String line : lines) {
                if (line.startsWith(id + ",")) {
                    // restore inventory based on this line
                    restoreInventoryFromOrderLine(line);
                    // skip adding this line to keep -> effectively delete it
                    continue;
                } else {
                    keep.add(line);
                }
            }
            Files.write(ORDERS_FILE, keep);
            loadOrdersIntoTable();
            JOptionPane.showMessageDialog(this, "Order canceled.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void restoreInventoryFromOrderLine(String line) {
        try {
            // Expected format: id,product:qty|p2:qty2,total,status
            String[] p = line.split(",");
            if (p.length < 4) return;
            String items = p[1];
            String status = p[3].trim();
            // Only add back inventory if status is Incomplete
            if (!status.equalsIgnoreCase("Incomplete")) return;

            String[] parts = items.split("\\|");
            for (String it : parts) {
                String[] kv = it.split(":");
                if (kv.length != 2) continue;
                String productName = kv[0];
                int qty = safeParseInt(kv[1]);
                Recipe r = recipeMap.get(productName);
                if (r == null) continue;
                for (String ing : r.ingredients.keySet()) {
                    int used = r.ingredients.get(ing) * qty;
                    InventoryItem inv = inventoryMap.get(ing);
                    if (inv != null) {
                        inv.stock += used;
                    }
                }
            }
            saveInventory();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------- HELPERS (logic) ----------------

    // Compute all ingredient needs for the order
    private Map<String, Integer> computeNeededIngredients(Map<String, Integer> items) {
        Map<String, Integer> req = new HashMap<>();
        for (String prod : items.keySet()) {
            Recipe r = recipeMap.get(prod);
            if (r == null) continue;
            int q = items.get(prod);
            for (String ing : r.ingredients.keySet()) {
                int amt = r.ingredients.get(ing) * q;
                int cur = req.containsKey(ing) ? req.get(ing) : 0;
                req.put(ing, cur + amt);
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
                sb.append(p).append(":").append(items.get(p));
                first = false;
            }
            sb.append(",").append(fmt.format(total)).append(",Incomplete");
            sb.append("\n");
            Files.write(ORDERS_FILE, sb.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deductInventory(Map<String, Integer> needed) {
        for (String ing : needed.keySet()) {
            InventoryItem inv = inventoryMap.get(ing);
            if (inv != null) {
                inv.stock -= needed.get(ing);
            }
        }
    }

    private void saveInventory() {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("Ingredient,Stock,Low");
            for (String ing : inventoryMap.keySet()) {
                InventoryItem it = inventoryMap.get(ing);
                lines.add(it.name + "," + it.stock + "," + it.low);
            }
            Files.write(INVENTORY_FILE, lines);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------- small helpers ----------------
    private int safeParseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private double safeParseDouble(String s) {
        try {
            String t = s.replace("$", "").trim();
            return Double.parseDouble(t);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String generateCustomerID() {
        int count = 1;
        try {
            if (Files.exists(ORDERS_FILE)) {
                List<String> all = Files.readAllLines(ORDERS_FILE);
                count = all.size() + 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return String.format("C%04d", count);
    }

    private void updateTotalLabel() {
        double total = 0;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            double price = safeParseDouble((String) cartModel.getValueAt(i, 2));
            int qty = (int) cartModel.getValueAt(i, 1);
            total += price * qty;
        }
        lblTotal.setText("$" + fmt.format(total));
    }

    // ---------------- data classes ----------------
    private static class Recipe {
        String name;
        Map<String, Integer> ingredients = new LinkedHashMap<>();
        Recipe(String n) { name = n; }
    }

    private static class InventoryItem {
        String name;
        int stock;
        int low;
        InventoryItem(String n, int s, int l) { name = n; stock = s; low = l; }
    }

    // ---------------- main ----------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new OrderApplicationUI().setVisible(true);
            }
        });
    }
}
