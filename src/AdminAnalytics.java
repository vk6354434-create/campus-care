import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// Charts, key numbers and the monthly report
public class AdminAnalytics extends JPanel {

    private static final Color[] CATEGORY_COLORS = {
        new Color(37, 99, 235), new Color(14, 165, 233), new Color(16, 185, 129),
        new Color(234, 179, 8), new Color(249, 115, 22), new Color(168, 85, 247),
        new Color(236, 72, 153), new Color(100, 116, 139)
    };

    private final MainFrame frame;

    private final JLabel totalValue = new JLabel("0");
    private final JLabel resolutionRateValue = new JLabel("0%");
    private final JLabel avgTimeValue = new JLabel("-");
    private final JLabel ratingValue = new JLabel("-");
    private final JLabel overdueValue = new JLabel("0");
    private final JLabel aiAccuracyValue = new JLabel("-");

    private final Charts.DonutChart statusChart =
            new Charts.DonutChart("Complaints by Status");
    private final Charts.BarChart categoryChart =
            new Charts.BarChart("Complaints by Category");
    private final Charts.BarChart monthlyChart =
            new Charts.BarChart("Monthly Trend - Last 6 Months");
    private final Charts.BarChart departmentChart =
            new Charts.BarChart("Avg. Resolution Time by Department (hours)");
    private final Charts.DonutChart priorityChart =
            new Charts.DonutChart("Open Complaints by Priority");
    private final Charts.BarChart trendChart =
            new Charts.BarChart("New Complaints - Last 7 Days");

    private final JLabel insightLabel = UI.muted(" ");

    // Raw rows, also used for the monthly report
    private final List<Row> rows = new ArrayList<>();

    private static class Row {
        String title;
        String status;
        String category;
        String priority;
        String department;
        String aiCategory;
        Timestamp created;
        Timestamp resolved;
        int rating;
    }

    public AdminAnalytics(MainFrame frame) {

        this.frame = frame;

        setLayout(new BorderLayout());
        setBackground(UI.BACKGROUND);

        JPanel main = new JPanel(new BorderLayout(0, 14));
        main.setBackground(UI.BACKGROUND);
        main.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header
        JPanel headingText = new JPanel();
        headingText.setLayout(new BoxLayout(headingText, BoxLayout.Y_AXIS));
        headingText.setOpaque(false);
        JLabel title = UI.heading("Analytics & Insights", 26);
        title.setForeground(Color.WHITE);
        insightLabel.setForeground(new Color(255, 255, 255, 220));
        headingText.add(title);
        headingText.add(Box.createVerticalStrut(6));
        headingText.add(insightLabel);

        JButton reportButton = UI.glassButton("Monthly Report");
        reportButton.addActionListener(e -> exportMonthlyReport());

        JButton refreshButton = UI.glassButton("Refresh");
        refreshButton.addActionListener(e -> loadData());

        JButton backButton = UI.glassButton("Back to Dashboard");
        backButton.addActionListener(e -> frame.goBack());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        buttons.setOpaque(false);
        buttons.add(reportButton);
        buttons.add(refreshButton);
        buttons.add(backButton);

        Theme.HeroPanel header = new Theme.HeroPanel(new BorderLayout(),
                new Color(124, 58, 237), new Color(219, 39, 119));
        header.setBorder(new EmptyBorder(16, 22, 22, 22));
        header.add(headingText, BorderLayout.WEST);
        header.add(buttons, BorderLayout.EAST);

        // Key numbers
        JPanel cards = new JPanel(new GridLayout(1, 6, 10, 0));
        cards.setOpaque(false);
        cards.add(UI.statCard("Total Complaints", totalValue, UI.PRIMARY));
        cards.add(UI.statCard("Resolution Rate", resolutionRateValue, UI.RESOLVED));
        cards.add(UI.statCard("Avg. Resolution", avgTimeValue, UI.IN_PROGRESS));
        cards.add(UI.statCard("Student Rating", ratingValue, new Color(245, 158, 11)));
        cards.add(UI.statCard("Overdue Now", overdueValue, UI.REJECTED));
        cards.add(UI.statCard("AI Accuracy", aiAccuracyValue, new Color(124, 58, 237)));

        // Charts (scrollable, 3 rows x 2)
        JPanel charts = new JPanel(new GridLayout(3, 2, 14, 14));
        charts.setOpaque(false);
        charts.add(statusChart);
        charts.add(categoryChart);
        charts.add(monthlyChart);
        charts.add(departmentChart);
        charts.add(priorityChart);
        charts.add(trendChart);

        JPanel scrollContent = new JPanel(new BorderLayout(0, 14)) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                Container parent = getParent();
                return parent == null ? d
                        : new Dimension(parent.getWidth(), d.height);
            }
        };
        scrollContent.setBackground(UI.BACKGROUND);
        scrollContent.add(cards, BorderLayout.NORTH);
        scrollContent.add(charts, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(scrollContent);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UI.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        main.add(header, BorderLayout.NORTH);
        main.add(scroll, BorderLayout.CENTER);

        add(main, BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadData();
            }
        });
    }

    // ================= LOAD =================

    private void loadData() {

        rows.clear();

        String sql = """
                SELECT title, status, category, priority, assigned_to,
                       ai_category, created_at, resolved_at, rating
                FROM complaints
                """;

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                JOptionPane.showMessageDialog(frame, "Database connection failed!");
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Row r = new Row();
                    r.title = rs.getString("title");
                    r.status = rs.getString("status");
                    r.category = rs.getString("category");
                    r.priority = rs.getString("priority");
                    r.department = rs.getString("assigned_to");
                    if (r.department == null || r.department.isBlank()) {
                        r.department = SmartClassifier.departmentFor(r.category);
                    }
                    r.aiCategory = rs.getString("ai_category");
                    r.created = rs.getTimestamp("created_at");
                    r.resolved = rs.getTimestamp("resolved_at");
                    r.rating = rs.getInt("rating");
                    rows.add(r);
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame,
                    "Could not load analytics!\n" + e.getMessage());
            e.printStackTrace();
            return;
        }

        updateView();
    }

    private void updateView() {

        int[] statusCounts = new int[UI.STATUSES.length];
        int[] categoryCounts = new int[UI.CATEGORIES.length];
        int[] openPriorityCounts = new int[UI.PRIORITIES.length];
        int[] lastSevenDays = new int[7];
        int[] lastSixMonths = new int[6];

        String[] departments = SmartClassifier.DEPARTMENTS;
        long[] deptHours = new long[departments.length];
        int[] deptResolved = new int[departments.length];

        int overdue = 0;
        long resolvedHoursSum = 0;
        int resolvedWithTime = 0;
        int ratingSum = 0;
        int ratingCount = 0;
        int aiChecked = 0;
        int aiCorrect = 0;

        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.now();

        for (Row r : rows) {

            int s = indexOf(UI.STATUSES, r.status);
            if (s >= 0) {
                statusCounts[s]++;
            }

            int c = indexOf(UI.CATEGORIES, r.category);
            categoryCounts[c < 0 ? UI.CATEGORIES.length - 1 : c]++;

            if (UI.isOpen(r.status)) {
                int p = indexOf(UI.PRIORITIES, r.priority);
                openPriorityCounts[p < 0 ? 1 : p]++;
            }

            if (UI.OVERDUE.equals(UI.slaStatus(r.status, r.priority, r.created))) {
                overdue++;
            }

            long hours = resolutionHours(r);
            if (hours >= 0) {
                resolvedHoursSum += hours;
                resolvedWithTime++;

                int d = indexOf(departments, r.department);
                if (d >= 0) {
                    deptHours[d] += hours;
                    deptResolved[d]++;
                }
            }

            if (r.rating > 0) {
                ratingSum += r.rating;
                ratingCount++;
            }

            if (r.aiCategory != null) {
                aiChecked++;
                if (r.aiCategory.equals(r.category)) {
                    aiCorrect++;
                }
            }

            if (r.created != null) {
                LocalDate day = toDate(r.created);
                long daysAgo = ChronoUnit.DAYS.between(day, today);
                if (daysAgo >= 0 && daysAgo < 7) {
                    lastSevenDays[6 - (int) daysAgo]++;
                }

                long monthsAgo = ChronoUnit.MONTHS.between(
                        YearMonth.from(day), thisMonth);
                if (monthsAgo >= 0 && monthsAgo < 6) {
                    lastSixMonths[5 - (int) monthsAgo]++;
                }
            }
        }

        int total = rows.size();

        // Key numbers
        totalValue.setText(String.valueOf(total));
        resolutionRateValue.setText(total == 0
                ? "0%" : Math.round(statusCounts[2] * 100f / total) + "%");
        overdueValue.setText(String.valueOf(overdue));
        avgTimeValue.setText(resolvedWithTime == 0
                ? "-" : formatHours(resolvedHoursSum / resolvedWithTime));
        ratingValue.setText(ratingCount == 0
                ? "-" : String.format("%.1f / 5", ratingSum / (double) ratingCount));
        aiAccuracyValue.setText(aiChecked == 0
                ? "-" : Math.round(aiCorrect * 100f / aiChecked) + "%");

        // Department averages
        int[] deptAverage = new int[departments.length];
        String slowest = null;
        int slowestHours = -1;

        for (int i = 0; i < departments.length; i++) {
            if (deptResolved[i] > 0) {
                deptAverage[i] = (int) (deptHours[i] / deptResolved[i]);
                if (deptAverage[i] > slowestHours) {
                    slowestHours = deptAverage[i];
                    slowest = departments[i];
                }
            }
        }

        // One-line insight under the title
        int top = 0;
        for (int i = 1; i < categoryCounts.length; i++) {
            if (categoryCounts[i] > categoryCounts[top]) {
                top = i;
            }
        }

        if (total == 0) {
            insightLabel.setText("No complaints yet");
        } else {
            insightLabel.setText("Most reported: " + UI.CATEGORIES[top]
                    + " (" + categoryCounts[top] + ")"
                    + (slowest == null ? ""
                       : "   |   Slowest department: " + slowest
                         + " (" + formatHours(slowestHours) + " avg)")
                    + "   |   AI trained on " + SmartClassifier.getTrainingSize()
                    + " complaints");
        }

        // Charts
        statusChart.setData(UI.STATUSES, statusCounts, new Color[]{
            UI.SUBMITTED, UI.IN_PROGRESS, UI.RESOLVED, UI.REJECTED
        });

        categoryChart.setData(UI.CATEGORIES, categoryCounts, CATEGORY_COLORS);

        String[] monthLabels = new String[6];
        DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("MMM yy");
        for (int i = 0; i < 6; i++) {
            monthLabels[i] = thisMonth.minusMonths(5 - i).format(monthFormat);
        }
        monthlyChart.setData(monthLabels, lastSixMonths,
                new Color[]{new Color(14, 165, 233)});

        String[] shortDept = {
            "Electrical", "Plumbing", "Housekeep.", "Warden",
            "IT Cell", "Carpentry", "Mess", "Office"
        };
        Color[] deptColors = new Color[departments.length];
        for (int i = 0; i < departments.length; i++) {
            deptColors[i] = departments[i].equals(slowest)
                    ? UI.REJECTED : new Color(124, 58, 237);
        }
        departmentChart.setData(shortDept, deptAverage, deptColors);

        priorityChart.setData(UI.PRIORITIES, openPriorityCounts, new Color[]{
            UI.LOW, UI.MEDIUM, UI.HIGH, UI.URGENT
        });

        DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("EEE d");
        String[] dayLabels = new String[7];
        for (int i = 0; i < 7; i++) {
            dayLabels[i] = today.minusDays(6 - i).format(dayFormat);
        }
        trendChart.setData(dayLabels, lastSevenDays, new Color[]{UI.PRIMARY});
    }

    // ================= MONTHLY REPORT (HTML, printable) =================

    private void exportMonthlyReport() {

        if (rows.isEmpty()) {
            loadData();
        }

        YearMonth month = YearMonth.now();
        String monthName = month.format(DateTimeFormatter.ofPattern("MMMM yyyy"));

        // FileDialog opens the normal Windows "Save As" window
        FileDialog dialog = new FileDialog(frame,
                "Save monthly report", FileDialog.SAVE);
        dialog.setFile("CampusCare_Report_"
                + month.format(DateTimeFormatter.ofPattern("yyyy_MM")) + ".html");
        dialog.setVisible(true);

        if (dialog.getFile() == null) {
            return; // user pressed Cancel
        }

        File file = new File(dialog.getDirectory(), dialog.getFile());
        if (!file.getName().toLowerCase().endsWith(".html")) {
            file = new File(file.getParentFile(), file.getName() + ".html");
        }

        // This month's complaints
        List<Row> monthRows = new ArrayList<>();
        for (Row r : rows) {
            if (r.created != null && YearMonth.from(toDate(r.created)).equals(month)) {
                monthRows.add(r);
            }
        }

        int total = monthRows.size();
        int resolved = 0;
        int open = 0;
        int overdue = 0;
        long hoursSum = 0;
        int hoursCount = 0;

        int[] categoryCounts = new int[UI.CATEGORIES.length];
        String[] departments = SmartClassifier.DEPARTMENTS;
        int[] deptTotal = new int[departments.length];
        int[] deptDone = new int[departments.length];
        long[] deptHours = new long[departments.length];

        for (Row r : monthRows) {
            if ("Resolved".equals(r.status)) {
                resolved++;
            }
            if (UI.isOpen(r.status)) {
                open++;
            }
            if (UI.OVERDUE.equals(UI.slaStatus(r.status, r.priority, r.created))) {
                overdue++;
            }

            int c = indexOf(UI.CATEGORIES, r.category);
            categoryCounts[c < 0 ? UI.CATEGORIES.length - 1 : c]++;

            int d = indexOf(departments, r.department);
            long hours = resolutionHours(r);
            if (d >= 0) {
                deptTotal[d]++;
                if (hours >= 0) {
                    deptDone[d]++;
                    deptHours[d] += hours;
                }
            }
            if (hours >= 0) {
                hoursSum += hours;
                hoursCount++;
            }
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='utf-8'>")
            .append("<title>Campus Care Report - ").append(monthName).append("</title>")
            .append("<style>")
            .append("body{font-family:Segoe UI,Arial,sans-serif;margin:40px;color:#1f2937}")
            .append("h1{margin:0}h2{margin-top:32px;border-bottom:2px solid #e5e7eb;padding-bottom:6px}")
            .append(".muted{color:#6b7280}")
            .append(".cards{display:flex;gap:14px;margin-top:20px;flex-wrap:wrap}")
            .append(".card{border:1px solid #e5e7eb;border-left:4px solid #2563eb;padding:12px 18px;min-width:140px}")
            .append(".card b{display:block;font-size:26px;color:#2563eb}")
            .append("table{border-collapse:collapse;width:100%;margin-top:10px}")
            .append("th,td{border:1px solid #e5e7eb;padding:8px 10px;text-align:left}")
            .append("th{background:#f3f4f6}")
            .append(".bar{background:#2563eb;height:12px;border-radius:6px}")
            .append(".red{color:#dc2626;font-weight:bold}")
            .append("</style></head><body>");

        html.append("<h1>Campus Care - Monthly Complaint Report</h1>")
            .append("<p class='muted'>").append(monthName)
            .append(" &nbsp;|&nbsp; Generated on ")
            .append(UI.formatDate(new Timestamp(System.currentTimeMillis())))
            .append("</p>");

        html.append("<div class='cards'>")
            .append(card("Complaints", String.valueOf(total)))
            .append(card("Resolved", String.valueOf(resolved)))
            .append(card("Still Open", String.valueOf(open)))
            .append(card("Overdue", String.valueOf(overdue)))
            .append(card("Resolution Rate", total == 0 ? "0%"
                    : Math.round(resolved * 100f / total) + "%"))
            .append(card("Avg. Resolution", hoursCount == 0 ? "-"
                    : formatHours(hoursSum / hoursCount)))
            .append("</div>");

        html.append("<h2>Complaints by Category</h2><table>")
            .append("<tr><th>Category</th><th>Count</th><th style='width:50%'></th></tr>");
        int max = 1;
        for (int v : categoryCounts) {
            max = Math.max(max, v);
        }
        for (int i = 0; i < UI.CATEGORIES.length; i++) {
            html.append("<tr><td>").append(UI.CATEGORIES[i]).append("</td><td>")
                .append(categoryCounts[i]).append("</td><td><div class='bar' style='width:")
                .append(categoryCounts[i] * 100 / max).append("%'></div></td></tr>");
        }
        html.append("</table>");

        html.append("<h2>Department Performance</h2><table>")
            .append("<tr><th>Department</th><th>Complaints</th><th>Resolved</th>")
            .append("<th>Avg. Resolution Time</th></tr>");
        for (int i = 0; i < departments.length; i++) {
            if (deptTotal[i] == 0) {
                continue;
            }
            html.append("<tr><td>").append(departments[i]).append("</td><td>")
                .append(deptTotal[i]).append("</td><td>").append(deptDone[i])
                .append("</td><td>").append(deptDone[i] == 0 ? "-"
                        : formatHours(deptHours[i] / deptDone[i]))
                .append("</td></tr>");
        }
        html.append("</table>");

        html.append("<h2>Open Complaints Needing Attention</h2><table>")
            .append("<tr><th>Title</th><th>Category</th><th>Priority</th>")
            .append("<th>Handled By</th><th>Submitted</th><th>SLA</th></tr>");
        boolean anyOpen = false;
        for (Row r : rows) {
            if (!UI.isOpen(r.status)) {
                continue;
            }
            anyOpen = true;
            String sla = UI.slaStatus(r.status, r.priority, r.created);
            html.append("<tr><td>").append(escape(r.title)).append("</td><td>")
                .append(r.category).append("</td><td>").append(r.priority)
                .append("</td><td>").append(r.department).append("</td><td>")
                .append(UI.formatDate(r.created)).append("</td><td")
                .append(UI.OVERDUE.equals(sla) ? " class='red'>" : ">")
                .append(sla).append("</td></tr>");
        }
        if (!anyOpen) {
            html.append("<tr><td colspan='6'>No open complaints.</td></tr>");
        }
        html.append("</table>");

        html.append("<p class='muted' style='margin-top:40px'>Generated by Campus Care ")
            .append("- AI-powered Campus Complaint Management System</p>")
            .append("</body></html>");

        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
            out.print(html);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Could not save report: " + e.getMessage());
            return;
        }

        JOptionPane.showMessageDialog(frame,
                "Monthly report saved:\n" + file.getAbsolutePath()
                + "\n\nIt opens in any browser and can be printed as PDF.");

        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
            }
        } catch (Exception ignored) {
            // Opening the browser is optional
        }
    }

    // ================= HELPERS =================

    private static long resolutionHours(Row r) {
        if (!"Resolved".equals(r.status) || r.created == null || r.resolved == null) {
            return -1;
        }
        return Math.max(0, (r.resolved.getTime() - r.created.getTime()) / 3600000);
    }

    private static String formatHours(long hours) {
        return hours < 24 ? hours + " hrs" : String.format("%.1f days", hours / 24.0);
    }

    private static LocalDate toDate(Timestamp t) {
        return t.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static String card(String label, String value) {
        return "<div class='card'><span class='muted'>" + label + "</span><b>"
                + value + "</b></div>";
    }

    private static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;");
    }

    private static int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }
}
