import java.io.*;
import java.util.*;

/**
 * ReportSystem
 * ---------------------------------------------------------
 * Reads tasks from tasks.txt and produces a summary per
 * employee:
 *
 * assigned = total number of tasks
 * inProgress = tasks whose status is pending or in progress
 * completed = tasks whose status is completed
 */
public class ReportSystem {

    private final String tasksFile = "tasks.txt";

    public static class ReportRow {
        public String employee;
        public int assigned;
        public int inProgress;
        public int completed;

        public ReportRow(String employee) {
            this.employee = employee;
        }
    }

    public List<ReportRow> generateReport() {

        Map<String, ReportRow> summary = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(tasksFile))) {

            String line;
            while ((line = br.readLine()) != null) {

                String[] arr = line.split(",");
                if (arr.length < 6)
                    continue;

                String employee = arr[0].trim();
                String statusRaw = arr[5].trim().toLowerCase();
                statusRaw = statusRaw.replaceAll("\\s+", ""); // remove all space characters

                summary.putIfAbsent(employee, new ReportRow(employee));
                ReportRow row = summary.get(employee);

                row.assigned++;

                if (statusRaw.contains("pending") || statusRaw.contains("progress")) {
                    row.inProgress++;
                } else if (statusRaw.contains("complete")) {
                    row.completed++;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        List<ReportRow> result = new ArrayList<>(summary.values());
        result.sort(Comparator.comparing(r -> r.employee.toLowerCase()));

        return result;
    }
}
