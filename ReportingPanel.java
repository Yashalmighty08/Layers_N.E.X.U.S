import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * ReportingPanel
 * ---------------------------------------------------------
 * GUI panel for Feature 4: Reporting.
 *
 * Shows a summary table with columns:
 * Employee | Assigned | In Progress | Completed
 *
 * Logic:
 * - Uses ReportSystem to read tasks.txt
 * - Aggregates counts per employee
 * - Displays results in a non-editable JTable
 */
public class ReportingPanel extends JPanel {

    private final ReportSystem reportSystem;
    private JTable table;
    private DefaultTableModel model;

    public ReportingPanel() {
        this.reportSystem = new ReportSystem();
        initializeUI();
        loadReport();
    }

    /**
     * Set up layout, title bar and table.
     */
    private void initializeUI() {
        setLayout(new BorderLayout());

        // ---- Top bar: title + refresh button ----
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Reporting - Task Distribution by Employee");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadReport());

        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(refreshButton, BorderLayout.EAST);

        // ---- Table + model ----
        model = new DefaultTableModel(
                new Object[] { "Employee", "Assigned", "In Progress", "Completed" },
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // reporting is view-only
            }
        };

        table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Loads summarized data from ReportSystem and fills the table.
     */
    private void loadReport() {
        // clear existing rows
        model.setRowCount(0);

        // get data from backend
        List<ReportSystem.ReportRow> rows = reportSystem.generateReport();

        // one row per employee
        for (ReportSystem.ReportRow r : rows) {
            model.addRow(new Object[] {
                    r.employee,
                    r.assigned,
                    r.inProgress,
                    r.completed
            });
        }
    }
}
