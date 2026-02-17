import java.io.*;
import java.util.*;

/**
 * TaskManagement
 * ---------------------------------------------------------
 * Backend logic for Feature 2: Task Management.
 *
 * Responsibilities:
 * - Read employee names from users.txt
 * - Read and write task data from tasks.txt
 * - Ensure every employee in users.txt has at least one row in tasks.txt
 * (a default placeholder row if needed)
 * - Allow creating a new task for an employee
 * - Allow updating status of an employee's task
 *
 * File formats (plain CSV):
 *
 * users.txt
 * ----------
 * One employee name per line:
 * John_Smith
 * Emily_Johnson
 *
 * tasks.txt
 * ----------
 * employeeName,wage,description,deadline,priority,status
 *
 * Example:
 * John_Smith,20.00,Prepare Monthly Report,2025-12-01,High,Completed
 */
public class TaskManagement {

    // Paths to the text files (relative to working directory)
    final String usersFile = "users.txt";
    final String tasksFile = "tasks.txt";

    /**
     * Represents one task row (one line in tasks.txt).
     */
    public static class TaskEntry {
        public String employee;
        public String wage;
        public String description;
        public String deadline;
        public String priority;
        public String status;

        public TaskEntry(String employee,
                String wage,
                String description,
                String deadline,
                String priority,
                String status) {
            this.employee = employee;
            this.wage = wage;
            this.description = description;
            this.deadline = deadline;
            this.priority = priority;
            this.status = status;
        }
    }

    /**
     * Loads and synchronizes tasks:
     * 1. Reads all employees from users.txt
     * 2. Reads all existing tasks from tasks.txt
     * 3. If an employee has no row in tasks.txt, add a default placeholder:
     * description = ""
     * deadline = ""
     * priority = ""
     * status = "Pending!"
     * 4. If any placeholders were added, write the full list back to tasks.txt
     */
    public List<TaskEntry> loadTasks() {
        List<String> employees = loadUsers();
        List<TaskEntry> tasks = loadExistingTasks();

        boolean changed = false;

        for (String emp : employees) {
            if (!containsUser(tasks, emp)) {
                // Add default row for new employee
                tasks.add(new TaskEntry(
                        emp,
                        "", // wage can be filled by finance system
                        "", // description
                        "", // deadline
                        "", // priority
                        "Pending!" // default status
                ));
                changed = true;
            }
        }

        if (changed) {
            saveTasks(tasks);
        }

        return tasks;
    }

    /**
     * Checks if the list already contains at least one row for the employee.
     */
    private boolean containsUser(List<TaskEntry> tasks, String user) {
        for (TaskEntry t : tasks) {
            // Extract just the name from the employee field for comparison
            String taskEmployeeName = t.employee;
            if (t.employee.contains(" ")) {
                taskEmployeeName = t.employee.split("\\s+")[0];
            }
            if (taskEmployeeName.equalsIgnoreCase(user)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Loads employee names from users.txt (one name per line).
     */
    private List<String> loadUsers() {
    List<String> list = new ArrayList<>();

    try (BufferedReader br = new BufferedReader(new FileReader(usersFile))) {
        String line;
        while ((line = br.readLine()) != null) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                // Extract only the first part (name) before the first whitespace
                String[] parts = trimmedLine.split("\\s+");
                if (parts.length > 0) {
                    String name = parts[0]; // Get just the name, not the entire line
                    list.add(name);
                }
            }
        }
    } catch (Exception e) {
        // If users.txt is missing, just return empty list.
        e.printStackTrace();
    }

    return list;
}


private Map<String, String> loadWages() {
    Map<String, String> wageMap = new HashMap<>();
    final String financeFile = "finance.txt";
    
    try (BufferedReader br = new BufferedReader(new FileReader(financeFile))) {
        String line;
        while ((line = br.readLine()) != null) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                String[] parts = trimmedLine.split("\\s+");
                if (parts.length >= 2) {
                    String name = parts[0];
                    String wage = parts[1]; // Second value after first whitespace
                    wageMap.put(name, wage);
                }
            }
        }
    } catch (FileNotFoundException e) {
        System.err.println("finance.txt not found - wages will be empty");
    } catch (Exception e) {
        e.printStackTrace();
    }
    
    return wageMap;
}
    
public List<TaskEntry> loadExistingTasks() {
    List<TaskEntry> list = new ArrayList<>();
    Map<String, String> wageMap = loadWages();

    try (BufferedReader br = new BufferedReader(new FileReader(tasksFile))) {
        String line;
        while ((line = br.readLine()) != null) {
            String[] arr = line.split(",");
            
            if (arr.length < 6) {
                // skip malformed lines
                continue;
            }

            // Extract just the name from the first field (before first space)
            String fullEmployeeField = arr[0].trim();
            String employeeName = fullEmployeeField;
            if (fullEmployeeField.contains(" ")) {
                employeeName = fullEmployeeField.split("\\s+")[0];
            }

            String wage = arr[1].trim();
            if (wage.isEmpty() && wageMap.containsKey(employeeName)) {
                wage = wageMap.get(employeeName); // Use wage from finance.txt
                }
            
            TaskEntry entry = new TaskEntry(
                    employeeName,  // Use just the name, not the full field
                    arr[1],
                    arr[2],
                    arr[3],
                    arr[4],
                    arr[5]);
            list.add(entry);
        }
    } catch (FileNotFoundException e) {
        // tasks.txt might not exist yet; that's okay.
    } catch (Exception e) {
        e.printStackTrace();
    }

    return list;
}
    public void createTask(String employee,
            String description,
            String deadline,
            String priority) {

        String emp = employee == null ? "" : employee.trim();
        String desc = description == null ? "" : description.trim();
        String dl = deadline == null ? "" : deadline.trim();
        String pr = priority == null ? "" : priority.trim();

        if (emp.isEmpty() || desc.isEmpty()) {
            System.err.println("createTask: employee and description are required.");
            return;
        }

        
        Map<String, String> wageMap = loadWages();
        String wageToUse = wageMap.getOrDefault(emp, "");

        // 2) Append the new task row, reusing wage if we found one
        try (PrintWriter pw = new PrintWriter(new FileWriter(tasksFile, true))) {
            pw.println(String.join(",",
                    emp,
                    wageToUse, // <- reuse existing wage, or blank if none
                    desc,
                    dl,
                    pr,
                    "Pending!" // initial status
            ));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the status of the first matching task for an employee.
     */
    public void updateStatus(String employee, String newStatus) {
        List<TaskEntry> tasks = loadTasks();
        // Extract just the name from the employee parameter if it contains spaces
        String empToFind = employee;
        if (employee != null && employee.contains(" ")) {
            empToFind = employee.split("\\s+")[0];
        }
        for (TaskEntry t : tasks) {
            if (t.employee.equalsIgnoreCase(empToFind)) {
                t.status = newStatus;
                break;
            }
        }
        saveTasks(tasks);
    }

    /**
     * Writes the full list of TaskEntry objects back to tasks.txt.
     * This is used by loadTasks() when it needs to sync placeholder rows.
     */
    public void saveTasks(List<TaskEntry> tasks) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(tasksFile))) {
            for (TaskEntry t : tasks) {
                pw.println(String.join(",",
                        t.employee != null ? t.employee : "",
                        t.wage != null ? t.wage : "",
                        t.description != null ? t.description : "",
                        t.deadline != null ? t.deadline : "",
                        t.priority != null ? t.priority : "",
                        t.status != null ? t.status : ""));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
