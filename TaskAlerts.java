import java.util.*;

public class TaskAlerts {
    private TaskManagement task;
    private User currentUser;
    
    public TaskAlerts(User user) {
        task = new TaskManagement();
        this.currentUser = user;
    }

    public List<TaskManagement.TaskEntry> getTasksForCurrentUser() {
        String currentUsername = currentUser.getUsername();
        return getTasksForEmployee(currentUsername);
    }
    
    public List<TaskManagement.TaskEntry> getTasksForEmployee(String employeeName) {
        List<TaskManagement.TaskEntry> allTasks = task.loadExistingTasks();
        List<TaskManagement.TaskEntry> employeeTasks = new ArrayList<>();
        
        for (TaskManagement.TaskEntry entry : allTasks) {
            if (entry.employee != null && entry.employee.trim().equalsIgnoreCase(employeeName.trim())) {
                // Skip placeholder/empty tasks
                if (isValidTask(entry)) {
                    employeeTasks.add(entry);
                }
            }
        }
        return employeeTasks;
    }
    
    private boolean isValidTask(TaskManagement.TaskEntry entry) {
        // Check if this is a real task (not a placeholder)
        return entry.description != null && 
               !entry.description.trim().isEmpty() &&
               !entry.description.equalsIgnoreCase("Pending!");
    }
    
    // For managers: get all tasks
    public List<TaskManagement.TaskEntry> getAllTasks() {
        List<TaskManagement.TaskEntry> allTasks = task.loadExistingTasks();
        List<TaskManagement.TaskEntry> validTasks = new ArrayList<>();
        
        for (TaskManagement.TaskEntry entry : allTasks) {
            if (isValidTask(entry)) {
                validTasks.add(entry);
            }
        }
        return validTasks;
    }
}