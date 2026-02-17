import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * TaskPanel
 * ---------------------------------------------------------
 * GUI panel for Feature 2: Task Management (manager view).
 *
 * - Shows one row per employee (their MOST RECENT non-empty task)
 * - Hides manager accounts and placeholder rows
 * - Buttons:
 * * Refresh (top-right)
 * * View Tasks for Employee (bottom-left)
 * * Create Task (bottom-right)
 *
 * Uses TaskManagement as its backend.
 */
public class TaskPanel extends JPanel {

    private final TaskManagement taskManagement;
    private JTable table;
    private DefaultTableModel model;

    public TaskPanel() {
        this.taskManagement = new TaskManagement();
        initializeUI();
        loadData();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // ---------- Top (title + refresh) ----------
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Task Management");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadData());

        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(refreshButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // ---------- Center (table) ----------
        String[] columns = { "Employee", "Wage", "Task", "Deadline", "Priority", "Status" };
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // read-only table
            }
        };

        table = new JTable(model);
        table.setRowHeight(22);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(0, 500));
        add(scrollPane, BorderLayout.CENTER);

        // ---------- Bottom (buttons) ----------
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton viewTasksButton = new JButton("View Tasks for Employee");
        viewTasksButton.addActionListener(e -> openViewTasksDialog());

        JButton createTaskButton = new JButton("Create Task");
        createTaskButton.addActionListener(e -> openCreateTaskDialog());

        leftButtons.add(viewTasksButton);
        rightButtons.add(createTaskButton);

        bottomPanel.add(leftButtons, BorderLayout.WEST);
        bottomPanel.add(rightButtons, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    // ---------- Helper ----------

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ---------- Data loading for summary table ----------

    /**
     * Loads tasks from backend and populates the summary table:
     * - Skips manager accounts
     * - Skips placeholder rows (no description/deadline/priority/wage)
     * - Keeps LAST non-empty task per employee
     * - If the LAST task has no wage, reuse a previous non-blank wage
     */
    private void loadData() {
        model.setRowCount(0);

        List<TaskManagement.TaskEntry> tasks = taskManagement.loadTasks();

        // employee -> latest TaskEntry (with wage fallback)
        Map<String, TaskManagement.TaskEntry> latestByEmployee = new LinkedHashMap<>();

        for (TaskManagement.TaskEntry t : tasks) {
            if (t == null || t.employee == null)
                continue;

            String emp = t.employee.trim();
            if (emp.isEmpty())
                continue;

            // skip managers
            if (emp.toLowerCase().contains("manager"))
                continue;

            // skip pure placeholder rows
            if (isBlank(t.description) && isBlank(t.deadline)
                    && isBlank(t.priority) && isBlank(t.wage)) {
                continue;
            }

            TaskManagement.TaskEntry existing = latestByEmployee.get(emp);

            // Decide which wage to keep:
            String wageToUse = t.wage;
            if (isBlank(wageToUse) && existing != null && !isBlank(existing.wage)) {
                // keep old wage if new row's wage is blank
                wageToUse = existing.wage;
            }

            // create a copy so we don't mutate the original object
            TaskManagement.TaskEntry copy = new TaskManagement.TaskEntry(
                    t.employee,
                    wageToUse,
                    t.description,
                    t.deadline,
                    t.priority,
                    t.status);

            // overwrite: this row is now the "latest" for this employee
            latestByEmployee.put(emp, copy);
        }

        // fill table
        for (TaskManagement.TaskEntry t : latestByEmployee.values()) {
            model.addRow(new Object[] {
                    t.employee,
                    t.wage,
                    t.description,
                    t.deadline,
                    t.priority,
                    t.status
            });
        }
    }

    // ---------- Create Task dialog (with date validation) ----------

    private void openCreateTaskDialog() {
        // Build employee list (same filters as loadData)
        List<TaskManagement.TaskEntry> tasks = taskManagement.loadTasks();
        Set<String> employees = new LinkedHashSet<>();
        for (TaskManagement.TaskEntry t : tasks) {
            if (t == null || t.employee == null)
                continue;
            String emp = t.employee.trim();
            if (emp.isEmpty())
                continue;
            if (emp.toLowerCase().contains("manager"))
                continue;
            employees.add(emp);
        }

        if (employees.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "No employees available.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JComboBox<String> employeeCombo = new JComboBox<>(employees.toArray(new String[0]));
        JTextField descField = new JTextField();
        JTextField deadlineField = new JTextField();
        JTextField priorityField = new JTextField();

        Object[] fields = {
                "Employee:", employeeCombo,
                "Task Description:", descField,
                "Deadline (YYYY-MM-DD):", deadlineField,
                "Priority (Low / Medium / High):", priorityField
        };

        int option = JOptionPane.showConfirmDialog(
                this,
                fields,
                "Create Task",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            String employee = (String) employeeCombo.getSelectedItem();
            String desc = descField.getText().trim();
            String deadline = deadlineField.getText().trim();
            String priority = priorityField.getText().trim();

            // Required fields
            if (employee == null || employee.isEmpty() || desc.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Employee and Task Description are required.",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // ---- DATE VALIDATION ----
            if (deadline.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please enter a deadline date in the format YYYY-MM-DD.",
                        "Invalid Deadline",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            LocalDate deadlineDate;
            try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                deadlineDate = LocalDate.parse(deadline, fmt);
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Deadline must be in the format YYYY-MM-DD (e.g., 2025-12-20).",
                        "Invalid Deadline",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            LocalDate today = LocalDate.now();
            if (deadlineDate.isBefore(today)) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please enter a deadline that is today or a future date.\n"
                                + "Past dates (like last year) are not allowed.",
                        "Deadline in the Past",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            // ---- END DATE VALIDATION ----

            // Append new row to tasks.txt (status = "Pending!")
            taskManagement.createTask(employee, desc, deadline, priority);

            // Refresh summary so the new/latest task shows (with wage fallback)
            loadData();
        }
    }

    // ---------- Helper to populate detail table for one employee ----------

    /**
     * Fills the detail model with all tasks for empKey and
     * records, for each row, which index in the full task list it came from.
     */
    private void populateEmployeeTasks(String empKey,
            DefaultTableModel detailModel,
            java.util.List<Integer> rowToIndex) {
        List<TaskManagement.TaskEntry> allTasks = taskManagement.loadTasks();

        // Find a non-blank wage to reuse as fallback
        String wageFallback = "";
        for (TaskManagement.TaskEntry t : allTasks) {
            if (t == null || t.employee == null)
                continue;
            String emp = t.employee.trim();
            if (!emp.equalsIgnoreCase(empKey))
                continue;
            if (!isBlank(t.wage)) {
                wageFallback = t.wage;
                break;
            }
        }

        detailModel.setRowCount(0);
        rowToIndex.clear();

        for (int i = 0; i < allTasks.size(); i++) {
            TaskManagement.TaskEntry t = allTasks.get(i);
            if (t == null || t.employee == null)
                continue;
            String emp = t.employee.trim();
            if (emp.isEmpty())
                continue;
            if (!emp.equalsIgnoreCase(empKey))
                continue;
            if (emp.toLowerCase().contains("manager"))
                continue;
            if (isBlank(t.description) && isBlank(t.deadline)
                    && isBlank(t.priority) && isBlank(t.wage)) {
                continue;
            }

            String wageToShow = t.wage;
            if (isBlank(wageToShow) && !isBlank(wageFallback)) {
                wageToShow = wageFallback;
            }

            detailModel.addRow(new Object[] {
                    t.employee,
                    wageToShow,
                    t.description,
                    t.deadline,
                    t.priority,
                    t.status
            });

            // Remember which index in allTasks this row came from
            rowToIndex.add(i);
        }
    }

    // ---------- View & Delete Tasks dialog ----------

    private void openViewTasksDialog() {
    int row = table.getSelectedRow();
    if (row < 0) {
        JOptionPane.showMessageDialog(
                this,
                "Please select an employee row first.",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
        return;
    }

    String employee = (String) model.getValueAt(row, 0);
    if (employee == null)
        return;
    String empKey = employee.trim();

    DefaultTableModel detailModel = new DefaultTableModel(
            new Object[] { "Employee", "Wage", "Task", "Deadline", "Priority", "Status" },
            0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 5;  // Only Status column editable
        }
    };

    JTable detailTable = new JTable(detailModel);
    detailTable.setRowHeight(22);

    // Add dropdown for Status column
    JComboBox<String> statusCombo = new JComboBox<>(new String[]{
        "Pending!", "Pending", "In Progress", "Completed"
    });
    detailTable.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(statusCombo));

    JScrollPane scrollPane = new JScrollPane(detailTable);

    // Mapping: detail row index -> index in full task list
    java.util.List<Integer> rowToIndex = new ArrayList<>();
    populateEmployeeTasks(empKey, detailModel, rowToIndex);

    // Add listener to save status changes
    detailModel.addTableModelListener(e -> {
        if (e.getType() == TableModelEvent.UPDATE) {
            int rowIdx = e.getFirstRow();
            int column = e.getColumn();
            
            if (column == 5) {
                int taskIndex = rowToIndex.get(rowIdx);
                List<TaskManagement.TaskEntry> allTasks = taskManagement.loadTasks();
                
                if (taskIndex >= 0 && taskIndex < allTasks.size()) {
                    String newStatus = (String) detailModel.getValueAt(rowIdx, 5);
                    allTasks.get(taskIndex).status = newStatus;
                    taskManagement.saveTasks(allTasks);
                    loadData();  // Refresh main table
                }
            }
        }
    });

    JDialog dialog = new JDialog(
            SwingUtilities.getWindowAncestor(this),
            "Tasks for " + employee,
            Dialog.ModalityType.APPLICATION_MODAL);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.setSize(800, 400);
    dialog.setLocationRelativeTo(this);
    dialog.setLayout(new BorderLayout());
    dialog.add(scrollPane, BorderLayout.CENTER);

    // --- Buttons at bottom: Delete + Close ---
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton deleteButton = new JButton("Delete Selected Task");
    JButton closeButton = new JButton("Close");

    // Delete logic
    deleteButton.addActionListener(e -> {
        int selectedRow = detailTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(
                    dialog,
                    "Please select a task to delete.",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                dialog,
                "Are you sure you want to delete this task?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Load all tasks, remove the one at the mapped index, save back.
        List<TaskManagement.TaskEntry> allTasks = taskManagement.loadTasks();
        int idx = rowToIndex.get(selectedRow);

        if (idx < 0 || idx >= allTasks.size()) {
            // Just a sanity check
            JOptionPane.showMessageDialog(
                    dialog,
                    "Unable to delete the selected task (index out of range).",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        allTasks.remove(idx);
        taskManagement.saveTasks(allTasks);

        // Refresh main summary table
        loadData();

        // Rebuild detail table & mappings for further deletions
        populateEmployeeTasks(empKey, detailModel, rowToIndex);
    });

    closeButton.addActionListener(e -> dialog.dispose());

    buttonPanel.add(deleteButton);
    buttonPanel.add(closeButton);
    dialog.add(buttonPanel, BorderLayout.SOUTH);

    dialog.setVisible(true);
}
}

