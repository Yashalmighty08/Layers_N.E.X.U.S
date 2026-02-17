import java.io.BufferedReader;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FileManager {

    public static class Recipe {
        public String name;
        public Map<String, Integer> ingredients = new LinkedHashMap<>();
        public Recipe(String n) { name = n; }
    }

    public static class InventoryItem {
        public String name;
        public int stock;
        public int low;
        public InventoryItem(String n, int s, int l) { name = n; stock = s; low = l; }
    }

    private Map<String, Recipe> recipeMap = new LinkedHashMap<>();
    private Map<String, InventoryItem> inventoryMap = new LinkedHashMap<>();
    private Map<String, Double> priceMap = new LinkedHashMap<>();
    private DecimalFormat fmt = new DecimalFormat("#0.00");

    private static final Path RECIPES_FILE = Paths.get("recipes.txt");
    private static final Path INVENTORY_FILE = Paths.get("inventory.txt");
    private static final Path ORDERS_FILE = Paths.get("orders.txt");
    private static final Path PRICES_FILE = Paths.get("prices.txt");

    // ---- getters so UI / controller can access data ----
    public Map<String, Recipe> getRecipeMap() { return recipeMap; }
    public Map<String, InventoryItem> getInventoryMap() { return inventoryMap; }
    public Map<String, Double> getPriceMap() { return priceMap; }
    public DecimalFormat getFmt() { return fmt; }
    public Path getOrdersFile() { return ORDERS_FILE; }

    // ---- loading logic ----
    public void loadRecipes() {
        recipeMap.clear();
        if (!Files.exists(RECIPES_FILE)) return;
        try (BufferedReader br = Files.newBufferedReader(RECIPES_FILE, StandardCharsets.UTF_8)) {
            String header = br.readLine();
            if (header == null) return;
            String[] cols = header.split(",");
            List<String> ingredientNames = new ArrayList<>();
            for (int i = 1; i < cols.length; i++) ingredientNames.add(cols[i].trim().toLowerCase());

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
            e.printStackTrace();
        }
    }

    public void loadInventory() {
        inventoryMap.clear();
        if (!Files.exists(INVENTORY_FILE)) return;
        try (BufferedReader br = Files.newBufferedReader(INVENTORY_FILE, StandardCharsets.UTF_8)) {
            String header = br.readLine(); // may be header
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

    public void loadPrices() {
        priceMap.clear();
        if (!Files.exists(PRICES_FILE)) {
            createDefaultPricesFile();
            return;
        }

        try (BufferedReader br = Files.newBufferedReader(PRICES_FILE, StandardCharsets.UTF_8)) {
            String header = br.readLine(); // skip possible header
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

        // ensure every recipe has price
        for (String prod : recipeMap.keySet()) {
            if (!priceMap.containsKey(prod)) priceMap.put(prod, 0.00);
        }

        // if file missing some entries, rewrite
        try {
            List<String> fileLines = Files.exists(PRICES_FILE) ? Files.readAllLines(PRICES_FILE, StandardCharsets.UTF_8) : new ArrayList<>();
            if (fileLines.size() - 1 < recipeMap.size()) savePrices();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createDefaultPricesFile() {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("Product,Price");
            for (String prod : recipeMap.keySet()) {
                lines.add(prod + ",0.00");
                priceMap.put(prod, 0.00);
            }
            Files.write(PRICES_FILE, lines, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void savePrices() {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("Product,Price");
            for (String prod : priceMap.keySet()) {
                lines.add(prod + "," + fmt.format(priceMap.get(prod)));
            }
            Files.write(PRICES_FILE, lines, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveInventory() {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("Ingredient,Stock,Low");
            for (String ing : inventoryMap.keySet()) {
                InventoryItem it = inventoryMap.get(ing);
                lines.add(it.name + "," + it.stock + "," + it.low);
            }
            Files.write(INVENTORY_FILE, lines, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Save order (append), format: id,product:qty|p2:qty2,total,status\n
    public void saveOrder(String id, Map<String, Integer> items, double total) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(id).append(",");
            boolean first = true;
            for (String p : items.keySet()) {
                if (!first) sb.append("|");
                sb.append(p).append(":").append(items.get(p));
                first = false;
            }
            sb.append(",").append(fmt.format(total)).append(",Incomplete").append("\n");
            Files.write(ORDERS_FILE, sb.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> readOrdersFileLines() {
        try {
            if (!Files.exists(ORDERS_FILE)) return new ArrayList<>();
            return Files.readAllLines(ORDERS_FILE, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    // Restore inventory from a single order line (same format as saveOrder)
    // Only restore if status == Incomplete
    public void restoreInventoryFromOrderLine(String line) {
        try {
            if (line == null || line.trim().isEmpty()) return;
            String[] p = line.split(",");
            if (p.length < 4) return;
            String items = p[1];
            String status = p[3].trim();
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
                    if (inv != null) inv.stock += used;
                }
            }
            saveInventory();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Utilities
    private int safeParseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private double safeParseDouble(String s) {
        try {
            String t = s.replace("$", "").trim();
            return Double.parseDouble(t);
        } catch (Exception e) {
            return 0.0;
        }
    }
}