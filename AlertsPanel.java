import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AlertsPanel extends JPanel {
    private final TaskAlerts taskAlerts; 
    private JTable table;
    private DefaultTableModel tableModel;
    private User currentUser;

    public AlertsPanel(User user) {
        this.currentUser = user;
        taskAlerts = new TaskAlerts(user);
        initializeUI();
        loaddata();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel;
        if (currentUser.getRole().equals("Manager")) {
            titleLabel = new JLabel("All Employee Tasks - Manager View");
        } else {
            titleLabel = new JLabel("My Tasks");
        }
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loaddata());

        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(refreshButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Different columns for Manager vs Employee
        String[] columns;
        if (currentUser.getRole().equals("Manager")) {
            columns = new String[]{"Employee", "Description", "Deadline", "Priority", "Status"};
        } else {
            columns = new String[]{"Description", "Deadline", "Priority", "Status"};
        }
        
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        table = new JTable(tableModel);
        table.setRowHeight(22);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(0, 500));
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void loaddata() {
        tableModel.setRowCount(0); // Clear existing data
        
        if (currentUser.getRole().equals("Manager")) {
            // Manager sees ALL tasks
            List<TaskManagement.TaskEntry> allTasks = taskAlerts.getAllTasks();
            
            if (allTasks.isEmpty()) {
                tableModel.addRow(new Object[]{"No tasks assigned to any employees yet", "-", "-", "-", "-"});
            } else {
                for (TaskManagement.TaskEntry entry : allTasks) {
                    tableModel.addRow(new Object[]{
                        entry.employee,
                        entry.description,
                        entry.deadline,
                        entry.priority,
                        entry.status
                    });
                }
            }
        } else {
            // Employee sees ONLY their OWN tasks
            List<TaskManagement.TaskEntry> myTasks = taskAlerts.getTasksForCurrentUser();
            
            if (myTasks.isEmpty()) {
                tableModel.addRow(new Object[]{"No tasks assigned to you yet", "-", "-", "-"});
            } else {
                for (TaskManagement.TaskEntry entry : myTasks) {
                    tableModel.addRow(new Object[]{
                        entry.description,
                        entry.deadline,
                        entry.priority,
                        entry.status
                    });
                }
            }
        }
    }
}