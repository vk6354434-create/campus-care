import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// Check a complaint's status without logging in.
// The registered email is asked too, so nobody can read other
// students' complaints just by guessing IDs.
public class TrackComplaint extends JPanel {

    private final MainFrame frame;

    private final JTextField idField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JPanel resultPanel = new JPanel(new BorderLayout());

    public TrackComplaint(MainFrame frame) {

        this.frame = frame;

        setBackground(UI.BACKGROUND);

        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setOpaque(false);
        heading.add(UI.heading("Track Your Complaint", 27));
        heading.add(Box.createVerticalStrut(8));
        heading.add(UI.muted("Enter your complaint ID and registered email - no login needed"));

        UI.styleField(idField);
        UI.styleField(emailField);
        idField.setPreferredSize(new Dimension(160, 46));
        emailField.setPreferredSize(new Dimension(300, 46));

        JButton trackButton = UI.primaryButton("Track Status");
        trackButton.setPreferredSize(new Dimension(160, 46));
        trackButton.addActionListener(e -> track());
        emailField.addActionListener(e -> track());

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 0, 4, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(UI.fieldLabel("Complaint ID"), gbc);
        gbc.gridx = 1;
        form.add(UI.fieldLabel("Registered Email"), gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        form.add(idField, gbc);
        gbc.gridx = 1;
        form.add(emailField, gbc);
        gbc.gridx = 2;
        form.add(trackButton, gbc);

        resultPanel.setOpaque(false);
        resultPanel.setBorder(new EmptyBorder(22, 0, 0, 0));

        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(30, 40, 30, 40));
        content.add(heading, BorderLayout.NORTH);
        content.add(form, BorderLayout.CENTER);
        content.add(resultPanel, BorderLayout.SOUTH);

        UI.cardScreen(this, content);
    }

    private void track() {

        String idText = idField.getText().trim().replace("#", "");
        String email = emailField.getText().trim();

        if (idText.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Please enter the complaint ID and your email.");
            return;
        }

        int complaintId;
        try {
            complaintId = Integer.parseInt(idText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Complaint ID must be a number.");
            return;
        }

        String sql = """
                SELECT c.complaint_id, c.student_id, c.title, c.category,
                       c.priority, c.status, c.created_at, c.assigned_to,
                       (SELECT u.remarks FROM complaint_updates u
                        WHERE u.complaint_id = c.complaint_id
                        ORDER BY u.updated_at DESC, u.update_id DESC
                        LIMIT 1) AS latest_remarks
                FROM complaints c
                JOIN students s ON c.student_id = s.student_id
                WHERE c.complaint_id = ? AND LOWER(s.email) = LOWER(?)
                """;

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                JOptionPane.showMessageDialog(frame, "Database connection failed!");
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, complaintId);
                ps.setString(2, email);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        showResult(null);
                        JOptionPane.showMessageDialog(frame,
                                "No complaint found with this ID and email.");
                        return;
                    }

                    showResult(resultCard(rs));
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame,
                    "Could not track complaint!\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private JPanel resultCard(ResultSet rs) throws Exception {

        int complaintId = rs.getInt("complaint_id");
        int studentId = rs.getInt("student_id");
        String status = rs.getString("status");
        String priority = rs.getString("priority");
        String category = rs.getString("category");
        Timestamp created = rs.getTimestamp("created_at");
        String assigned = rs.getString("assigned_to");
        String remarks = rs.getString("latest_remarks");
        String sla = UI.slaStatus(status, priority, created);

        JPanel card = UI.whiteBox(new BorderLayout(0, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        top.add(UI.heading("#" + complaintId + "  " + rs.getString("title"), 17));
        top.add(UI.badge(status, UI.statusColor(status)));
        if (!UI.CLOSED.equals(sla)) {
            top.add(UI.badge(sla, UI.slaColor(sla)));
        }

        JPanel info = new JPanel(new GridLayout(0, 2, 16, 6));
        info.setOpaque(false);
        info.add(UI.muted("Category: " + category));
        info.add(UI.muted("Priority: " + priority));
        info.add(UI.muted("Handled by: " + (assigned == null
                ? "Not assigned yet" : assigned)));
        info.add(UI.muted("Submitted: " + UI.formatDate(created)));

        JLabel latest = new JLabel("<html><body style='width:520px'><b>Latest update:</b> "
                + (remarks == null ? "Waiting for admin review." : remarks)
                + "</body></html>");
        latest.setForeground(UI.DARK);

        JButton details = UI.secondaryButton("View Full Timeline");
        details.addActionListener(e ->
                ComplaintDetailsDialog.open(frame, complaintId, studentId));

        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setOpaque(false);
        bottom.add(latest, BorderLayout.CENTER);
        bottom.add(details, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);
        card.add(info, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);

        return card;
    }

    private void showResult(JComponent card) {
        resultPanel.removeAll();
        if (card != null) {
            resultPanel.add(card, BorderLayout.CENTER);
        }
        resultPanel.revalidate();
        resultPanel.repaint();
    }
}
