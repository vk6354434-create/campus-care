import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.SimpleDateFormat;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public class AdminDashboard extends JPanel {

    // Table column positions
    private static final int COL_ID = 0;
    private static final int COL_CATEGORY = 2;
    private static final int COL_TITLE = 3;
    private static final int COL_PRIORITY = 4;
    private static final int COL_STATUS = 7;
    private static final int COL_SLA = 8;
    private static final int COL_RATING = 9;
    private static final int COL_ASSIGNED = 10;

    // Shown until the admin picks a department
    private static final String NOT_ASSIGNED = "Not assigned";
    private static final String SELECT_DEPARTMENT = "-- Select department --";

    private static final String[] REMARK_TEMPLATES = {
        "Quick remarks...",
        "Complaint received. Our team will inspect it soon.",
        "Technician has been assigned to this issue.",
        "Work is in progress.",
        "Required parts have been ordered.",
        "Issue has been fixed. Please confirm.",
        "Duplicate complaint - already being handled.",
        "Not enough information. Please submit again with details."
    };

    private final MainFrame frame;
    private final int adminId;
    private final String adminName;

    private JTable table;
    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;

    private JComboBox<String> statusBox;
    private JComboBox<String> categoryEdit;
    private JComboBox<String> assignBox;
    private boolean loadingSelection;
    private JComboBox<String> statusFilter;
    private JComboBox<String> categoryFilter;
    private JComboBox<String> priorityFilter;
    private JCheckBox overdueOnly;
    private JTextField searchField;
    private JTextField remarksArea;
    private JLabel selectedLabel;
    private JLabel welcome;
    private JLabel countLabel;

    private JLabel totalValue;
    private JLabel submittedValue;
    private JLabel progressValue;
    private JLabel resolvedValue;
    private JLabel overdueValue;

    private final Color background = new Color(245, 247, 251);
    private final Color dark = new Color(31, 41, 55);
    private final Color muted = new Color(107, 114, 128);

    public AdminDashboard(MainFrame frame, int adminId, String adminName) {

        this.frame = frame;
        this.adminId = adminId;
        this.adminName = adminName;

        setLayout(new BorderLayout());

        JPanel main = new JPanel(new BorderLayout(0, 14));
        main.setBackground(background);
        main.setBorder(new EmptyBorder(20, 24, 18, 24));

        // ================= HEADER =================

        JLabel heading = new JLabel("Admin Dashboard");
        heading.setFont(new Font("SansSerif", Font.BOLD, 26));
        heading.setForeground(Color.WHITE);

        welcome = new JLabel(
                "Welcome, " + adminName
                + "  |  Manage campus complaints"
        );
        welcome.setFont(new Font("SansSerif", Font.PLAIN, 14));
        welcome.setForeground(new Color(255, 255, 255, 215));

        JButton analyticsButton = UI.glassButton("Analytics");
        JButton exportButton = UI.glassButton("Export CSV");
        JButton profileButton = UI.glassButton("My Profile");
        profileButton.addActionListener(e ->
                frame.navigateTo(MainFrame.PROFILE));

        JButton refreshButton = UI.glassButton("Refresh");
        JButton logoutButton = UI.glassButton("Logout");

        JPanel headingText = new JPanel();
        headingText.setLayout(new BoxLayout(headingText, BoxLayout.Y_AXIS));
        headingText.setOpaque(false);
        headingText.add(heading);
        headingText.add(Box.createVerticalStrut(6));
        headingText.add(welcome);

        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        headerButtons.setOpaque(false);
        headerButtons.add(analyticsButton);
        headerButtons.add(exportButton);
        headerButtons.add(profileButton);
        headerButtons.add(refreshButton);
        headerButtons.add(logoutButton);

        Theme.HeroPanel header = new Theme.HeroPanel(new BorderLayout(),
                new Color(13, 148, 136), new Color(37, 99, 235));
        header.setBorder(new EmptyBorder(16, 22, 20, 22));
        header.add(headingText, BorderLayout.WEST);
        header.add(headerButtons, BorderLayout.EAST);

        main.add(header, BorderLayout.NORTH);

        // ================= MAIN CONTENT =================

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);

        // Summary cards
        JPanel cards = new JPanel(new GridLayout(1, 5, 12, 0));
        cards.setOpaque(false);

        totalValue = new JLabel("0");
        submittedValue = new JLabel("0");
        progressValue = new JLabel("0");
        resolvedValue = new JLabel("0");
        overdueValue = new JLabel("0");

        cards.add(UI.statCard("Total Complaints", totalValue, UI.PRIMARY));
        cards.add(UI.statCard("Submitted", submittedValue, UI.SUBMITTED));
        cards.add(UI.statCard("In Progress", progressValue, UI.IN_PROGRESS));
        cards.add(UI.statCard("Resolved", resolvedValue, UI.RESOLVED));
        cards.add(UI.statCard("Overdue (SLA)", overdueValue, UI.REJECTED));

        // Search and filters
        searchField = new JTextField(16);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        searchField.setPreferredSize(new Dimension(200, 40));

        statusFilter = new JComboBox<>(withAll("All Statuses", UI.STATUSES));
        categoryFilter = new JComboBox<>(withAll("All Categories", UI.CATEGORIES));
        priorityFilter = new JComboBox<>(withAll("All Priorities", UI.PRIORITIES));

        statusFilter.setPreferredSize(new Dimension(150, 40));
        categoryFilter.setPreferredSize(new Dimension(160, 40));
        priorityFilter.setPreferredSize(new Dimension(150, 40));

        overdueOnly = new JCheckBox("Overdue only");
        overdueOnly.setOpaque(false);
        overdueOnly.setFont(new Font("SansSerif", Font.BOLD, 13));
        overdueOnly.setForeground(UI.REJECTED);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filters.setOpaque(false);
        filters.add(UI.fieldLabel("Search:"));
        filters.add(searchField);
        filters.add(UI.fieldLabel("Status:"));
        filters.add(statusFilter);
        filters.add(UI.fieldLabel("Category:"));
        filters.add(categoryFilter);
        filters.add(UI.fieldLabel("Priority:"));
        filters.add(priorityFilter);
        filters.add(overdueOnly);

        JPanel topContent = new JPanel(new BorderLayout(0, 10));
        topContent.setOpaque(false);
        topContent.add(cards, BorderLayout.NORTH);
        topContent.add(filters, BorderLayout.SOUTH);

        content.add(topContent, BorderLayout.NORTH);

        // ================= COMPLAINT TABLE =================

        String[] columns = {
            "ID", "Student", "Category", "Title", "Priority",
            "Location", "Submitted", "Status", "SLA", "Rating", "Assigned To"
        };

        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column == COL_ID ? Integer.class : String.class;
            }
        };

        table = new JTable(model);
        UI.styleTable(table);

        int[] widths = {45, 120, 90, 180, 85, 90, 125, 105, 90, 85, 150};

        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            table.getColumnModel().getColumn(i).setCellRenderer(UI.centered());
        }

        table.getColumnModel().getColumn(COL_TITLE).setCellRenderer(UI.leftPadded());
        table.getColumnModel().getColumn(COL_PRIORITY).setCellRenderer(UI.priorityBadge());
        table.getColumnModel().getColumn(COL_STATUS).setCellRenderer(UI.statusBadge());
        table.getColumnModel().getColumn(COL_SLA).setCellRenderer(UI.slaBadge());
        table.getColumnModel().getColumn(COL_RATING).setCellRenderer(UI.ratingStars());

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JPanel tablePanel = new JPanel(new BorderLayout(0, 6));
        tablePanel.setOpaque(false);

        JLabel tableHeading = new JLabel("All Complaints");
        tableHeading.setFont(new Font("SansSerif", Font.BOLD, 17));
        tableHeading.setForeground(dark);

        countLabel = UI.muted("");

        JPanel tableTop = new JPanel(new BorderLayout());
        tableTop.setOpaque(false);
        tableTop.add(tableHeading, BorderLayout.WEST);
        tableTop.add(countLabel, BorderLayout.EAST);

        tablePanel.add(tableTop, BorderLayout.NORTH);
        tablePanel.add(UI.tableScroll(table), BorderLayout.CENTER);

        content.add(tablePanel, BorderLayout.CENTER);

        // ================= UPDATE CONTROLS =================

        selectedLabel = new JLabel("Select a complaint to update  (double-click to view details)");
        selectedLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        selectedLabel.setForeground(dark);

        statusBox = new JComboBox<>(UI.STATUSES);
        statusBox.setPreferredSize(new Dimension(130, 32));

        // Admin can correct the AI's category; the AI learns from it
        categoryEdit = new JComboBox<>(UI.CATEGORIES);
        categoryEdit.setPreferredSize(new Dimension(130, 32));

        // The admin decides which department handles the complaint
        String[] assignOptions = new String[SmartClassifier.DEPARTMENTS.length + 1];
        assignOptions[0] = SELECT_DEPARTMENT;
        System.arraycopy(SmartClassifier.DEPARTMENTS, 0, assignOptions, 1,
                SmartClassifier.DEPARTMENTS.length);

        assignBox = new JComboBox<>(assignOptions);
        assignBox.setPreferredSize(new Dimension(210, 32));

        JComboBox<String> templates = new JComboBox<>(REMARK_TEMPLATES);
        templates.setPreferredSize(new Dimension(230, 36));
        templates.addActionListener(e -> {
            if (templates.getSelectedIndex() > 0) {
                remarksArea.setText((String) templates.getSelectedItem());
                templates.setSelectedIndex(0);
            }
        });

        // Compact one-line remarks box (press Enter to update)
        remarksArea = new JTextField();
        UI.styleField(remarksArea);
        remarksArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        remarksArea.setPreferredSize(new Dimension(260, 36));
        remarksArea.setToolTipText("Write a remark for the student, or pick a quick remark");

        JButton updateButton = UI.primaryButton("Update");
        JButton detailsButton = UI.secondaryButton("Details");
        updateButton.setPreferredSize(new Dimension(100, 38));
        detailsButton.setPreferredSize(new Dimension(90, 38));
        remarksArea.addActionListener(e -> updateComplaint());

        statusBox.setPreferredSize(new Dimension(130, 36));
        categoryEdit.setPreferredSize(new Dimension(125, 36));
        assignBox.setPreferredSize(new Dimension(200, 36));

        // Row 1: what to change
        JPanel optionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        optionsRow.setOpaque(false);
        optionsRow.add(UI.fieldLabel("Status"));
        optionsRow.add(statusBox);
        optionsRow.add(Box.createHorizontalStrut(6));
        optionsRow.add(UI.fieldLabel("Category"));
        optionsRow.add(categoryEdit);
        optionsRow.add(Box.createHorizontalStrut(6));
        optionsRow.add(UI.fieldLabel("Assign To"));
        optionsRow.add(assignBox);

        // Row 2: remark + buttons, all on one line
        JPanel buttonsBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttonsBox.setOpaque(false);
        buttonsBox.add(templates);
        buttonsBox.add(updateButton);
        buttonsBox.add(detailsButton);

        JPanel remarksRow = new JPanel(new BorderLayout(8, 0));
        remarksRow.setOpaque(false);
        remarksRow.add(UI.fieldLabel("Remarks"), BorderLayout.WEST);
        remarksRow.add(remarksArea, BorderLayout.CENTER);
        remarksRow.add(buttonsBox, BorderLayout.EAST);

        JPanel rows = new JPanel(new GridLayout(2, 1, 0, 6));
        rows.setOpaque(false);
        rows.add(optionsRow);
        rows.add(remarksRow);

        selectedLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        selectedLabel.setForeground(UI.MUTED);

        Theme.Card bottom = new Theme.Card(new BorderLayout(0, 6), 2, 14);
        bottom.add(selectedLabel, BorderLayout.NORTH);
        bottom.add(rows, BorderLayout.CENTER);

        content.add(bottom, BorderLayout.SOUTH);
        main.add(content, BorderLayout.CENTER);

        // ================= EVENTS =================

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                applyFilters();
            }

            public void removeUpdate(DocumentEvent e) {
                applyFilters();
            }

            public void changedUpdate(DocumentEvent e) {
                applyFilters();
            }
        });

        statusFilter.addActionListener(e -> applyFilters());
        categoryFilter.addActionListener(e -> applyFilters());
        priorityFilter.addActionListener(e -> applyFilters());
        overdueOnly.addActionListener(e -> applyFilters());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = selectedModelRow();

                if (row >= 0) {
                    loadingSelection = true;
                    statusBox.setSelectedItem(
                            String.valueOf(model.getValueAt(row, COL_STATUS)));
                    categoryEdit.setSelectedItem(
                            String.valueOf(model.getValueAt(row, COL_CATEGORY)));
                    String assigned = String.valueOf(model.getValueAt(row, COL_ASSIGNED));
                    if (NOT_ASSIGNED.equals(assigned)) {
                        assignBox.setSelectedIndex(0);
                    } else {
                        assignBox.setSelectedItem(assigned);
                    }
                    loadingSelection = false;
                    selectedLabel.setText("Selected: #"
                            + model.getValueAt(row, COL_ID) + "  "
                            + model.getValueAt(row, COL_TITLE));
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    openDetails();
                }
            }
        });

        updateButton.addActionListener(e -> updateComplaint());
        detailsButton.addActionListener(e -> openDetails());
        exportButton.addActionListener(e -> exportCsv());

        analyticsButton.addActionListener(e ->
                frame.navigateTo(MainFrame.ADMIN_ANALYTICS));

        refreshButton.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            categoryFilter.setSelectedIndex(0);
            priorityFilter.setSelectedIndex(0);
            overdueOnly.setSelected(false);
            loadComplaints();
        });

        logoutButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    frame,
                    "Are you sure you want to logout?",
                    "Confirm Logout",
                    JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                frame.logout();
            }
        });

        add(main, BorderLayout.CENTER);

        // Reload complaints every time the dashboard is opened
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadComplaints();
            }
        });
    }

    // Called by the Profile screen after the admin changes their name
    public void setDisplayName(String name) {
        welcome.setText("Welcome, " + name + "  |  Manage campus complaints");
    }

    private static String[] withAll(String first, String[] items) {
        String[] result = new String[items.length + 1];
        result[0] = first;
        System.arraycopy(items, 0, result, 1, items.length);
        return result;
    }

    private int selectedModelRow() {
        int viewRow = table.getSelectedRow();
        return viewRow < 0 ? -1 : table.convertRowIndexToModel(viewRow);
    }

    // ================= LOAD COMPLAINTS =================

    private void loadComplaints() {

        model.setRowCount(0);

        String sql = """
                SELECT c.complaint_id, s.name, c.category, c.title,
                       c.priority, c.block, c.room_no, c.status,
                       c.created_at, c.rating, c.assigned_to
                FROM complaints c
                JOIN students s ON c.student_id = s.student_id
                ORDER BY c.complaint_id DESC
                """;

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                JOptionPane.showMessageDialog(
                        frame, "Database connection failed.");
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    String status = rs.getString("status");
                    String priority = rs.getString("priority");
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    int rating = rs.getInt("rating");
                    boolean noRating = rs.wasNull();

                    model.addRow(new Object[]{
                        rs.getInt("complaint_id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getString("title"),
                        priority,
                        rs.getString("block") + " - " + rs.getString("room_no"),
                        UI.sortableDate(createdAt),
                        status,
                        UI.slaStatus(status, priority, createdAt),
                        noRating ? "-" : UI.stars(rating),
                        assignedOrDefault(rs.getString("assigned_to"),
                                rs.getString("category"))
                    });
                }
            }

            updateSummaryCards();
            applyFilters();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Error loading complaints: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ================= UPDATE SUMMARY CARDS =================

    private void updateSummaryCards() {

        int total = model.getRowCount();
        int submitted = 0;
        int inProgress = 0;
        int resolved = 0;
        int overdue = 0;

        for (int i = 0; i < total; i++) {
            String status = String.valueOf(model.getValueAt(i, COL_STATUS));

            if ("Submitted".equals(status)) {
                submitted++;
            } else if ("In Progress".equals(status)) {
                inProgress++;
            } else if ("Resolved".equals(status)) {
                resolved++;
            }

            if (UI.OVERDUE.equals(model.getValueAt(i, COL_SLA))) {
                overdue++;
            }
        }

        totalValue.setText(String.valueOf(total));
        submittedValue.setText(String.valueOf(submitted));
        progressValue.setText(String.valueOf(inProgress));
        resolvedValue.setText(String.valueOf(resolved));
        overdueValue.setText(String.valueOf(overdue));
    }

    // ================= SEARCH AND FILTER =================

    private void applyFilters() {

        if (sorter == null) {
            return;
        }

        String search = searchField.getText().trim().toLowerCase();
        String status = (String) statusFilter.getSelectedItem();
        String category = (String) categoryFilter.getSelectedItem();
        String priority = (String) priorityFilter.getSelectedItem();
        boolean onlyOverdue = overdueOnly.isSelected();

        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(
                    Entry<? extends DefaultTableModel, ? extends Integer> entry) {

                if (!matches(status, "All Statuses", entry.getValue(COL_STATUS))
                        || !matches(category, "All Categories", entry.getValue(COL_CATEGORY))
                        || !matches(priority, "All Priorities", entry.getValue(COL_PRIORITY))) {
                    return false;
                }

                if (onlyOverdue && !UI.OVERDUE.equals(entry.getValue(COL_SLA))) {
                    return false;
                }

                if (search.isEmpty()) {
                    return true;
                }

                for (int i = 0; i < entry.getValueCount(); i++) {
                    Object value = entry.getValue(i);
                    if (value != null
                            && value.toString().toLowerCase().contains(search)) {
                        return true;
                    }
                }

                return false;
            }
        });

        countLabel.setText("Showing " + table.getRowCount()
                + " of " + model.getRowCount());
    }

    private static boolean matches(String selected, String all, Object value) {
        return selected == null || selected.equals(all)
                || selected.equalsIgnoreCase(String.valueOf(value));
    }

    // ================= DETAILS =================

    private void openDetails() {

        int row = selectedModelRow();

        if (row < 0) {
            JOptionPane.showMessageDialog(frame,
                    "Please select a complaint from the table.");
            return;
        }

        ComplaintDetailsDialog.open(frame,
                (Integer) model.getValueAt(row, COL_ID), -1);
    }

    // ================= UPDATE STATUS AND REMARKS =================

    private void updateComplaint() {

        int row = selectedModelRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Please select a complaint from the table.");
            return;
        }

        int complaintId = (Integer) model.getValueAt(row, COL_ID);

        String newStatus = (String) statusBox.getSelectedItem();
        String newCategory = (String) categoryEdit.getSelectedItem();
        String newAssigned = assignBox.getSelectedIndex() <= 0
                ? null : (String) assignBox.getSelectedItem();
        String oldAssigned = String.valueOf(model.getValueAt(row, COL_ASSIGNED));
        boolean categoryCorrected = !String.valueOf(
                model.getValueAt(row, COL_CATEGORY)).equals(newCategory);
        String remarks = remarksArea.getText().trim();

        if (remarks.isEmpty()) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Please enter remarks before updating.");
            return;
        }

        if (newStatus == null) {
            JOptionPane.showMessageDialog(frame, "Please select a status.");
            return;
        }

        // Tell the student in the timeline when the department changes
        if (newAssigned != null && !newAssigned.equals(oldAssigned)) {
            remarks = "Assigned to " + newAssigned + ". " + remarks;
        }

        // resolved_at is set when a complaint is resolved,
        // and cleared again if it is re-opened
        String updateSQL = """
                UPDATE complaints
                SET status = ?,
                    category = ?,
                    assigned_to = ?,
                    updated_at = CURRENT_TIMESTAMP,
                    resolved_at = CASE
                        WHEN ? = 'Resolved'
                            THEN COALESCE(resolved_at, CURRENT_TIMESTAMP)
                        ELSE NULL
                    END
                WHERE complaint_id = ?
                """;

        String historySQL =
                "INSERT INTO complaint_updates "
                + "(complaint_id, admin_id, status, remarks) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                JOptionPane.showMessageDialog(frame, "Database connection failed.");
                return;
            }

            boolean originalAutoCommit = con.getAutoCommit();

            try {
                con.setAutoCommit(false);

                try (PreparedStatement updatePS = con.prepareStatement(updateSQL);
                     PreparedStatement historyPS = con.prepareStatement(historySQL)) {

                    updatePS.setString(1, newStatus);
                    updatePS.setString(2, newCategory);
                    updatePS.setString(3, newAssigned);
                    updatePS.setString(4, newStatus);
                    updatePS.setInt(5, complaintId);

                    if (updatePS.executeUpdate() == 0) {
                        throw new SQLException("Complaint was not found.");
                    }

                    historyPS.setInt(1, complaintId);
                    historyPS.setInt(2, adminId);
                    historyPS.setString(3, newStatus);
                    historyPS.setString(4, remarks);
                    historyPS.executeUpdate();

                    con.commit();
                }

                remarksArea.setText("");
                loadComplaints();
                reselect(complaintId);

                // The AI learns from the admin's correction
                if (categoryCorrected) {
                    SmartClassifier.trainInBackground();
                }

                JOptionPane.showMessageDialog(
                        frame,
                        "Complaint #" + complaintId + " updated to \""
                        + newStatus + "\" successfully!");

            } catch (SQLException ex) {
                con.rollback();

                JOptionPane.showMessageDialog(frame, "Update failed: " + ex.getMessage());
                ex.printStackTrace();

            } finally {
                con.setAutoCommit(originalAutoCommit);
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Database error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Keep the same complaint selected after the table reloads
    private void reselect(int complaintId) {
        for (int i = 0; i < model.getRowCount(); i++) {
            if (Integer.valueOf(complaintId).equals(model.getValueAt(i, COL_ID))) {
                int viewRow = table.convertRowIndexToView(i);
                if (viewRow >= 0) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                    table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                }
                return;
            }
        }
    }

    private static String assignedOrDefault(String assigned, String category) {
        return assigned == null || assigned.isBlank() ? NOT_ASSIGNED : assigned;
    }

    // ================= EXPORT TO CSV (opens in Excel) =================

    private void exportCsv() {

        if (table.getRowCount() == 0) {
            JOptionPane.showMessageDialog(frame, "There is nothing to export.");
            return;
        }

        // FileDialog opens the normal Windows "Save As" window
        FileDialog dialog = new FileDialog(frame,
                "Save complaints report", FileDialog.SAVE);
        dialog.setFile("complaints_report_"
                + new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date())
                + ".csv");
        dialog.setVisible(true);

        if (dialog.getFile() == null) {
            return; // user pressed Cancel
        }

        File file = new File(dialog.getDirectory(), dialog.getFile());

        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getParentFile(), file.getName() + ".csv");
        }

        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {

            // Byte order mark so Excel reads special characters correctly
            out.print('﻿');

            StringBuilder line = new StringBuilder();

            for (int c = 0; c < model.getColumnCount(); c++) {
                if (c > 0) {
                    line.append(',');
                }
                line.append(csv(model.getColumnName(c)));
            }
            out.println(line);

            // Only the rows currently visible (filters are respected)
            for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
                int row = table.convertRowIndexToModel(viewRow);
                line.setLength(0);

                for (int c = 0; c < model.getColumnCount(); c++) {
                    if (c > 0) {
                        line.append(',');
                    }
                    Object value = model.getValueAt(row, c);
                    String text = String.valueOf(value);
                    if (c == COL_RATING && !"-".equals(text)) {
                        text = text.replace("☆", "").length() + "/5";
                    }
                    line.append(csv(text));
                }
                out.println(line);
            }

            JOptionPane.showMessageDialog(frame,
                    "Exported " + table.getRowCount()
                    + " complaints to:\n" + file.getAbsolutePath());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Export failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
