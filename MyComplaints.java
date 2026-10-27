import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public class MyComplaints extends JPanel {

    // Table column positions
    private static final int COL_ID = 0;
    private static final int COL_TITLE = 1;
    private static final int COL_STATUS = 5;
    private static final int COL_RATING = 8;

    private final MainFrame frame;
    private final int studentId;

    private JTable complaintsTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    private JTextField searchField;
    private JComboBox<String> statusFilter;

    private JButton rateButton;
    private JButton withdrawButton;

    public MyComplaints(MainFrame frame, int studentId) {

        this.frame = frame;
        this.studentId = studentId;

        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout(10, 14));
        mainPanel.setBorder(new EmptyBorder(24, 25, 20, 25));
        mainPanel.setBackground(UI.BACKGROUND);

        // ================= HEADER =================

        Theme.HeroPanel header = new Theme.HeroPanel(new GridLayout(2, 1, 0, 4),
                new Color(37, 99, 235), new Color(124, 58, 237));
        header.setBorder(new EmptyBorder(18, 24, 24, 24));

        JLabel title = UI.heading("My Complaints & History", 26);
        title.setForeground(Color.WHITE);

        JLabel hint = new JLabel("Track status, read admin remarks, "
                + "view full history and rate resolved complaints. "
                + "Double-click a row for details.");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 13));
        hint.setForeground(new Color(255, 255, 255, 215));

        header.add(title);
        header.add(hint);

        // ================= FILTERS =================

        searchField = new JTextField(22);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(260, 40));

        String[] statuses = new String[UI.STATUSES.length + 1];
        statuses[0] = "All Statuses";
        System.arraycopy(UI.STATUSES, 0, statuses, 1, UI.STATUSES.length);

        statusFilter = new JComboBox<>(statuses);
        statusFilter.setPreferredSize(new Dimension(170, 40));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(UI.fieldLabel("Search:"));
        filters.add(searchField);
        filters.add(UI.fieldLabel("Status:"));
        filters.add(statusFilter);

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        top.add(filters, BorderLayout.SOUTH);

        // ================= TABLE =================

        String[] columns = {
            "ID", "Title", "Category", "Priority", "Location",
            "Status", "Submitted", "Latest Remarks", "Rating", "Handled By"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column == COL_ID ? Integer.class : String.class;
            }
        };

        complaintsTable = new JTable(tableModel);
        UI.styleTable(complaintsTable);

        sorter = new TableRowSorter<>(tableModel);
        complaintsTable.setRowSorter(sorter);

        int[] widths = {45, 160, 90, 85, 90, 105, 125, 230, 85, 140};

        for (int i = 0; i < widths.length; i++) {
            complaintsTable.getColumnModel().getColumn(i)
                    .setPreferredWidth(widths[i]);
            complaintsTable.getColumnModel().getColumn(i)
                    .setCellRenderer(UI.centered());
        }

        complaintsTable.getColumnModel().getColumn(COL_TITLE)
                .setCellRenderer(UI.leftPadded());
        complaintsTable.getColumnModel().getColumn(7)
                .setCellRenderer(UI.leftPadded());
        complaintsTable.getColumnModel().getColumn(3)
                .setCellRenderer(UI.priorityBadge());
        complaintsTable.getColumnModel().getColumn(COL_STATUS)
                .setCellRenderer(UI.statusBadge());
        complaintsTable.getColumnModel().getColumn(COL_RATING)
                .setCellRenderer(UI.ratingStars());

        // ================= BUTTONS =================

        JButton detailsButton = UI.primaryButton("View Details");
        rateButton = UI.secondaryButton("Rate & Feedback");
        withdrawButton = UI.secondaryButton("Withdraw");
        JButton refreshButton = UI.secondaryButton("Refresh");

        rateButton.setToolTipText("Available after your complaint is resolved");
        withdrawButton.setToolTipText(
                "Possible only while the complaint is still 'Submitted'");

        detailsButton.addActionListener(e -> viewDetails());
        rateButton.addActionListener(e -> rateComplaint());
        withdrawButton.addActionListener(e -> withdrawComplaint());
        refreshButton.addActionListener(e -> loadComplaints());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        bottomPanel.setOpaque(false);
        bottomPanel.add(withdrawButton);
        bottomPanel.add(rateButton);
        bottomPanel.add(detailsButton);
        bottomPanel.add(refreshButton);

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

        complaintsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtons();
            }
        });

        complaintsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2
                        && complaintsTable.getSelectedRow() >= 0) {
                    viewDetails();
                }
            }
        });

        // ================= ADD COMPONENTS =================

        mainPanel.add(top, BorderLayout.NORTH);
        mainPanel.add(UI.tableScroll(complaintsTable), BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        updateButtons();

        // Reload the list every time this screen is opened,
        // so new complaints and admin remarks always show up
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadComplaints();
            }
        });
    }

    // ================= LOAD STUDENT COMPLAINTS =================

    private void loadComplaints() {

        tableModel.setRowCount(0);

        String sql = """
            SELECT c.complaint_id,
                   c.title,
                   c.category,
                   c.priority,
                   c.block,
                   c.room_no,
                   c.status,
                   c.created_at,
                   c.rating,
                   c.assigned_to,
                   (
                       SELECT u.remarks
                       FROM complaint_updates u
                       WHERE u.complaint_id = c.complaint_id
                       ORDER BY u.updated_at DESC, u.update_id DESC
                       LIMIT 1
                   ) AS latest_remarks
            FROM complaints c
            WHERE c.student_id = ?
            ORDER BY c.complaint_id DESC
            """;

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                JOptionPane.showMessageDialog(
                        frame,
                        "Database connection failed!");
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, studentId);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {

                        Timestamp createdAt = rs.getTimestamp("created_at");

                        String latestRemarks = rs.getString("latest_remarks");

                        if (latestRemarks == null || latestRemarks.isBlank()) {
                            latestRemarks = "No remarks yet";
                        }

                        int rating = rs.getInt("rating");
                        boolean noRating = rs.wasNull();

                        tableModel.addRow(new Object[]{
                            rs.getInt("complaint_id"),
                            rs.getString("title"),
                            rs.getString("category"),
                            rs.getString("priority"),
                            rs.getString("block") + " - " + rs.getString("room_no"),
                            rs.getString("status"),
                            UI.sortableDate(createdAt),
                            latestRemarks,
                            noRating ? "-" : UI.stars(rating),
                            rs.getString("assigned_to") == null
                                    ? "Not assigned yet"
                                    : rs.getString("assigned_to")
                        });
                    }
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Could not load complaints!\n"
                    + e.getMessage());

            e.printStackTrace();
        }

        updateButtons();
    }

    // ================= SEARCH AND FILTER =================

    private void applyFilters() {

        String search = searchField.getText().trim().toLowerCase();
        String status = (String) statusFilter.getSelectedItem();

        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(
                    Entry<? extends DefaultTableModel, ? extends Integer> entry) {

                boolean matchesStatus = status == null
                        || status.equals("All Statuses")
                        || status.equals(String.valueOf(entry.getValue(COL_STATUS)));

                if (!matchesStatus) {
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
    }

    // ================= SELECTED ROW HELPERS =================

    private int selectedModelRow() {
        int viewRow = complaintsTable.getSelectedRow();
        return viewRow < 0 ? -1 : complaintsTable.convertRowIndexToModel(viewRow);
    }

    // Buttons always stay clickable. If an action is not allowed for the
    // selected complaint, a message explains why (a greyed-out button
    // looked "broken" because clicking it did nothing).
    private void updateButtons() {
        rateButton.setEnabled(true);
        withdrawButton.setEnabled(true);
    }

    // ================= VIEW DETAILS =================

    private void viewDetails() {

        int row = selectedModelRow();

        if (row < 0) {
            JOptionPane.showMessageDialog(frame, "Please select a complaint first.");
            return;
        }

        int complaintId = (Integer) tableModel.getValueAt(row, COL_ID);
        ComplaintDetailsDialog.open(frame, complaintId, studentId);
    }

    // ================= RATE A RESOLVED COMPLAINT =================

    private void rateComplaint() {

        int row = selectedModelRow();

        if (row < 0) {
            JOptionPane.showMessageDialog(frame,
                    "Please select a complaint from the table first.");
            return;
        }

        String currentStatus = String.valueOf(tableModel.getValueAt(row, COL_STATUS));

        if (!"Resolved".equals(currentStatus)) {
            JOptionPane.showMessageDialog(frame,
                    "You can rate a complaint only after it is Resolved.\n"
                    + "This complaint is currently: " + currentStatus);
            return;
        }

        if (!"-".equals(String.valueOf(tableModel.getValueAt(row, COL_RATING)))) {
            JOptionPane.showMessageDialog(frame,
                    "You have already rated this complaint. Thank you!");
            return;
        }

        int complaintId = (Integer) tableModel.getValueAt(row, COL_ID);
        String title = String.valueOf(tableModel.getValueAt(row, COL_TITLE));

        // Star picker
        int[] rating = {0};
        JToggleButton[] stars = new JToggleButton[5];
        JLabel ratingText = UI.muted("Click a star to rate");

        JPanel starPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));

        for (int i = 0; i < 5; i++) {
            int value = i + 1;
            JToggleButton star = new JToggleButton("☆");
            star.setFont(new Font("Dialog", Font.PLAIN, 28));
            star.setForeground(UI.STAR);
            star.setBorderPainted(false);
            star.setContentAreaFilled(false);
            star.setFocusPainted(false);
            star.setCursor(new Cursor(Cursor.HAND_CURSOR));

            star.addActionListener(e -> {
                rating[0] = value;
                for (int j = 0; j < 5; j++) {
                    stars[j].setText(j < value ? "★" : "☆");
                }
                String[] words = {"Very poor", "Poor", "Okay", "Good", "Excellent"};
                ratingText.setText(value + "/5  -  " + words[value - 1]);
            });

            stars[i] = star;
            starPanel.add(star);
        }

        JTextArea feedback = new JTextArea(4, 30);
        feedback.setLineWrap(true);
        feedback.setWrapStyleWord(true);
        feedback.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        JPanel north = new JPanel(new GridLayout(0, 1, 0, 4));
        north.add(new JLabel("How satisfied are you with the resolution of:"));
        north.add(UI.heading(title, 14));
        north.add(starPanel);
        north.add(ratingText);
        north.add(new JLabel("Feedback (optional):"));
        panel.add(north, BorderLayout.NORTH);
        JScrollPane feedbackScroll = new JScrollPane(feedback);
        UI.styleScrollField(feedbackScroll);
        panel.add(feedbackScroll, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(frame, panel,
                "Rate Complaint #" + complaintId,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        if (rating[0] == 0) {
            JOptionPane.showMessageDialog(frame, "Please select a rating (1 to 5 stars).");
            return;
        }

        String sql = """
                UPDATE complaints SET rating = ?, feedback = ?
                WHERE complaint_id = ? AND student_id = ?
                  AND status = 'Resolved'
                """;

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                JOptionPane.showMessageDialog(frame, "Database connection failed!");
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, rating[0]);
                ps.setString(2, feedback.getText().trim());
                ps.setInt(3, complaintId);
                ps.setInt(4, studentId);
                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(frame, "Thank you for your feedback!");
            loadComplaints();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame,
                    "Could not save rating!\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= WITHDRAW A COMPLAINT =================

    private void withdrawComplaint() {

        int row = selectedModelRow();

        if (row < 0) {
            JOptionPane.showMessageDialog(frame,
                    "Please select the complaint you want to withdraw.");
            return;
        }

        String currentStatus = String.valueOf(tableModel.getValueAt(row, COL_STATUS));

        if (!"Submitted".equals(currentStatus)) {
            JOptionPane.showMessageDialog(frame,
                    "Only complaints that are still \"Submitted\" can be withdrawn.\n"
                    + "This complaint is already: " + currentStatus
                    + "\n(an admin has started working on it).");
            return;
        }

        int complaintId = (Integer) tableModel.getValueAt(row, COL_ID);

        int choice = JOptionPane.showConfirmDialog(frame,
                "Withdraw complaint #" + complaintId + "?\n"
                + "It will be permanently removed.",
                "Withdraw Complaint",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                JOptionPane.showMessageDialog(frame, "Database connection failed!");
                return;
            }

            con.setAutoCommit(false);

            try (PreparedStatement history = con.prepareStatement(
                    "DELETE FROM complaint_updates WHERE complaint_id = ?");
                 PreparedStatement complaint = con.prepareStatement(
                    "DELETE FROM complaints WHERE complaint_id = ? "
                    + "AND student_id = ? AND status = 'Submitted'")) {

                complaint.setInt(1, complaintId);
                complaint.setInt(2, studentId);

                history.setInt(1, complaintId);
                history.executeUpdate();

                if (complaint.executeUpdate() == 0) {
                    con.rollback();
                    JOptionPane.showMessageDialog(frame,
                            "This complaint can no longer be withdrawn.");
                } else {
                    con.commit();
                    JOptionPane.showMessageDialog(frame, "Complaint withdrawn.");
                }

            } catch (SQLException e) {
                con.rollback();
                throw e;
            }

            loadComplaints();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame,
                    "Could not withdraw complaint!\n" + e.getMessage());
            e.printStackTrace();
        }
    }
}
