import javax.swing.*;
import java.util.*;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class OrderApplication {

    private OrderApplicationUI ui;
    private FileManager fileManager;

    public OrderApplication(OrderApplicationUI ui, FileManager fileManager) {
        this.ui = ui;
        this.fileManager = fileManager;
    }

    public void addSelectedMenuToCart() {
        int row = ui.getMenuTable().getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(ui, "Select a product first.");
            return;
        }

        String product = (String) ui.getMenuModel().getValueAt(row, 0);
        String priceStr = (String) ui.getMenuModel().getValueAt(row, 1);
        double price = safeParseDouble(priceStr);

        for (int i = 0; i < ui.getCartModel().getRowCount(); i++) {
            if (ui.getCartModel().getValueAt(i, 0).equals(product)) {
                int q = (int) ui.getCartModel().getValueAt(i, 1);
                ui.getCartModel().setValueAt(q + 1, i, 1);
                ui.updateTotalLabel();
                return;
            }
        }

        ui.getCartModel().addRow(new Object[]{product, 1, "$" + ui.getFmt().format(price)});
        ui.updateTotalLabel();
    }

    public void removeCartItem() {
        int row = ui.getCartTable().getSelectedRow();
        if (row >= 0) {
            ui.getCartModel().removeRow(row);
            ui.updateTotalLabel();
        }
    }

    public void updateCartQuantity() {
        int row = ui.getCartTable().getSelectedRow();
        if (row < 0) return;
        String prod = (String) ui.getCartModel().getValueAt(row, 0);
        int curr = (int) ui.getCartModel().getValueAt(row, 1);
        String s = JOptionPane.showInputDialog(ui, "Quantity for " + prod + ":", curr);
        if (s == null) return;
        try {
            int q = Integer.parseInt(s.trim());
            if (q > 0) {
                ui.getCartModel().setValueAt(q, row, 1);
                ui.updateTotalLabel();
            }
        } catch (Exception e) {
            // ignore parse errors
        }
    }

    public void updateMenuPrice() {
        int row = ui.getMenuTable().getSelectedRow();
        if (row < 0) return;
        String prod = (String) ui.getMenuModel().getValueAt(row, 0);
        double cur = fileManager.getPriceMap().getOrDefault(prod, 0.00);
        String s = JOptionPane.showInputDialog(ui, "New price for " + prod + ":", ui.getFmt().format(cur));
        if (s == null) return;
        try {
            double p = Double.parseDouble(s.trim());
            fileManager.getPriceMap().put(prod, p);
            ui.getMenuModel().setValueAt("$" + ui.getFmt().format(p), row, 1);
            fileManager.savePrices();
            ui.updateTotalLabel();
        } catch (Exception e) {
            // ignore
        }
    }

    public void placeOrder() {
        if (ui.getCartModel().getRowCount() == 0) {
            JOptionPane.showMessageDialog(ui, "Order is empty.");
            return;
        }

        Map<String, Integer> orderItems = new LinkedHashMap<>();
        double total = 0;
        for (int i = 0; i < ui.getCartModel().getRowCount(); i++) {
            String prod = (String) ui.getCartModel().getValueAt(i, 0);
            int qty = (int) ui.getCartModel().getValueAt(i, 1);
            double price = safeParseDouble((String) ui.getCartModel().getValueAt(i, 2));
            orderItems.put(prod, qty);
            total += price * qty;
        }

        Map<String, Integer> needed = computeNeededIngredients(orderItems);
        List<String> lowList = new ArrayList<>();
        List<String> insufficient = new ArrayList<>();

        checkInventory(needed, lowList, insufficient);

        if (!insufficient.isEmpty() || !lowList.isEmpty()) {
            showOrderDeniedMessage(insufficient, lowList);
            return;
        }

        StringBuilder sb = new StringBuilder("Low:\n");
        if (lowList.isEmpty() && insufficient.isEmpty()) {
            sb.append("None");
        } else {
            for (String s : insufficient) sb.append("- Insufficient: ").append(s).append("\n");
            for (String s : lowList) sb.append("- Low: ").append(s).append("\n");
        }
        ui.getLowStockText().setText(sb.toString());

        int confirm = JOptionPane.showConfirmDialog(ui, "Place order?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String id = generateCustomerID();
        ui.getLblCustomer().setText(id);
        ui.getLblTotal().setText("$" + ui.getFmt().format(total));

        fileManager.saveOrder(id, orderItems, total);
        deductInventory(needed);
        fileManager.saveInventory();

        JOptionPane.showMessageDialog(ui, "Order placed! ID: " + id);
        cancelCart();
    }

    private Map<String, Integer> computeNeededIngredients(Map<String, Integer> items) {
        Map<String, Integer> req = new HashMap<>();
        for (String prod : items.keySet()) {
            FileManager.Recipe r = fileManager.getRecipeMap().get(prod);
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

    private void checkInventory(Map<String, Integer> needed, List<String> lowList, List<String> insufficient) {
        Map<String, FileManager.InventoryItem> inv = fileManager.getInventoryMap();
        for (String ing : needed.keySet()) {
            int req = needed.get(ing);
            FileManager.InventoryItem item = inv.get(ing);
            if (item == null || item.stock <= 0 || item.stock < req) {
                insufficient.add(ing);
            } else if (item.stock - req < item.low) {
                lowList.add(ing);
            }
        }
    }

    private void showOrderDeniedMessage(List<String> insufficient, List<String> lowList) {
        StringBuilder msg = new StringBuilder("Order cannot be placed due to stock issues:\n\n");
        for (String s : insufficient) msg.append("Insufficient stock: ").append(s).append("\n");
        for (String s : lowList) msg.append("⚠ Low stock limit reached: ").append(s).append("\n");
        JOptionPane.showMessageDialog(ui, msg.toString(), "Order Denied", JOptionPane.ERROR_MESSAGE);
    }

    private String generateCustomerID() {
        int count = 1;
        try {
            List<String> all = fileManager.readOrdersFileLines();
            if (all != null) count = all.size() + 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return String.format("C%04d", count);
    }

    private void deductInventory(Map<String, Integer> needed) {
        Map<String, FileManager.InventoryItem> inv = fileManager.getInventoryMap();
        for (String ing : needed.keySet()) {
            FileManager.InventoryItem item = inv.get(ing);
            if (item != null) item.stock -= needed.get(ing);
        }
    }

    public void cancelCart() {
        ui.cancelCart();
    }

    public void showOrdersScreen() {
        ui.showOrdersScreenInUI();
    }

    public void cancelSelectedOrder() {
        int row = ui.getOrdersTable() != null ? ui.getOrdersTable().getSelectedRow() : -1;
        if (row < 0) {
            JOptionPane.showMessageDialog(ui, "Select an order.");
            return;
        }
        String id = (String) ui.getOrdersHistoryModel().getValueAt(row, 0);
        try {
            List<String> lines = fileManager.readOrdersFileLines();
            List<String> keep = new ArrayList<>();
            for (String line : lines) {
                if (line.startsWith(id + ",")) {
                    // restore inventory based on this line
                    fileManager.restoreInventoryFromOrderLine(line);
                    continue; // skip - delete
                } else {
                    keep.add(line);
                }
            }
            Files.write(fileManager.getOrdersFile(), keep, StandardCharsets.UTF_8);
            ui.loadOrdersIntoTable();
            JOptionPane.showMessageDialog(ui, "Order canceled.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // small helpers
    private double safeParseDouble(String s) {
        try {
            String t = s.replace("$", "").trim();
            return Double.parseDouble(t);
        } catch (Exception e) {
            return 0.0;
        }
    }
}