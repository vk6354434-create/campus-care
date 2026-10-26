import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// Full details of one complaint: info, photo, rating and the
// complete status timeline. Used by both students and admins.
public class ComplaintDetailsDialog extends JDialog {

    private ComplaintDetailsDialog(Window owner, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
    }

    // studentId = -1 when an admin opens it (admins can see everything)
    public static void open(Component parent, int complaintId, int studentId) {

        Window owner = SwingUtilities.getWindowAncestor(parent);

        String sql = """
                SELECT c.*, s.name AS student_name, s.email AS student_email,
                       s.phone AS student_phone, s.department, s.year
                FROM complaints c
                JOIN students s ON c.student_id = s.student_id
                WHERE c.complaint_id = ?
                """;

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                JOptionPane.showMessageDialog(parent,
                        "Database connection failed!");
                return;
            }

            ComplaintDetailsDialog dialog = new ComplaintDetailsDialog(
                    owner, "Complaint #" + complaintId);

            JPanel content = new WidthTrackingPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBackground(UI.BACKGROUND);
            content.setBorder(new EmptyBorder(20, 22, 20, 22));

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, complaintId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next() || (studentId != -1
                            && rs.getInt("student_id") != studentId)) {
                        JOptionPane.showMessageDialog(parent,
                                "Complaint not found.");
                        return;
                    }

                    buildDetails(content, rs, studentId == -1);
                }
            }

            content.add(Box.createVerticalStrut(16));
            content.add(left(buildTimeline(con, complaintId)));

            JScrollPane scroll = new JScrollPane(content);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(16);

            JButton close = UI.primaryButton("Close");
            close.addActionListener(e -> dialog.dispose());

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            bottom.setBackground(Color.WHITE);
            bottom.setBorder(BorderFactory.createMatteBorder(
                    1, 0, 0, 0, UI.BORDER));
            bottom.add(close);

            dialog.setLayout(new BorderLayout());
            dialog.add(scroll, BorderLayout.CENTER);
            dialog.add(bottom, BorderLayout.SOUTH);
            dialog.setSize(760, 680);
            dialog.setLocationRelativeTo(owner);

            // Always open at the top of the details
            SwingUtilities.invokeLater(() ->
                    scroll.getVerticalScrollBar().setValue(0));

            dialog.setVisible(true);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(parent,
                    "Could not load complaint details!\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void buildDetails(
            JPanel content, ResultSet rs, boolean adminView)
            throws SQLException {

        String status = rs.getString("status");
        String priority = rs.getString("priority");
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        Timestamp resolvedAt = rs.getTimestamp("resolved_at");
        String sla = UI.slaStatus(status, priority, createdAt);

        // Title and badges
        JLabel title = UI.heading(rs.getString("title"), 22);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        badges.setOpaque(false);
        badges.add(UI.badge(status, UI.statusColor(status)));
        badges.add(UI.badge(priority + " priority", UI.priorityColor(priority)));
        if (!UI.CLOSED.equals(sla)) {
            badges.add(UI.badge(sla, UI.slaColor(sla)));
        }

        content.add(left(title));
        content.add(Box.createVerticalStrut(8));
        content.add(left(badges));
        content.add(Box.createVerticalStrut(16));

        // Information grid
        JPanel info = UI.whiteBox(new GridLayout(0, 2, 18, 10));

        addInfo(info, "Complaint ID", "#" + rs.getInt("complaint_id"));
        addInfo(info, "Category", rs.getString("category"));

        String assigned = rs.getString("assigned_to");
        addInfo(info, "Handled By", assigned != null ? assigned
                : "Not assigned yet");

        String aiCategory = rs.getString("ai_category");
        int aiConfidence = rs.getInt("ai_confidence");
        addInfo(info, "AI Prediction", aiCategory == null
                ? "Not available (older complaint)"
                : aiCategory + " (" + aiConfidence + "% sure)"
                  + (aiCategory.equals(rs.getString("category")) ? "  ✓" : "  - corrected"));
        addInfo(info, "Location", "Block " + rs.getString("block")
                + ", Room " + rs.getString("room_no"));
        addInfo(info, "Submitted", UI.formatDate(createdAt));
        addInfo(info, "Last Updated", UI.formatDate(updatedAt));

        if (resolvedAt != null && createdAt != null) {
            long hours = Math.max(0,
                    (resolvedAt.getTime() - createdAt.getTime()) / 3600000);
            addInfo(info, "Resolved In", hours < 24
                    ? hours + " hours" : (hours / 24) + " days " + (hours % 24) + " hours");
        } else {
            addInfo(info, "Target Time",
                    UI.slaDays(priority) + " day(s) for " + priority + " priority");
        }

        content.add(left(info));
        content.add(Box.createVerticalStrut(14));

        // Student details (admin only)
        if (adminView) {
            JPanel student = UI.whiteBox(new GridLayout(0, 2, 18, 10));
            addInfo(student, "Student", rs.getString("student_name"));
            addInfo(student, "Email", rs.getString("student_email"));
            addInfo(student, "Phone", rs.getString("student_phone"));
            addInfo(student, "Department / Year",
                    rs.getString("department") + " / Year " + rs.getInt("year"));

            content.add(left(sectionTitle("Student Details")));
            content.add(Box.createVerticalStrut(6));
            content.add(left(student));
            content.add(Box.createVerticalStrut(14));
        }

        // Description
        JTextArea description = new JTextArea(rs.getString("description"));
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setFont(new Font("SansSerif", Font.PLAIN, 14));
        description.setForeground(UI.DARK);
        description.setOpaque(false);

        JPanel descriptionBox = UI.whiteBox(new BorderLayout());
        descriptionBox.add(description);

        content.add(left(sectionTitle("Description")));
        content.add(Box.createVerticalStrut(6));
        content.add(left(descriptionBox));
        content.add(Box.createVerticalStrut(14));

        // Photo
        byte[] imageBytes = rs.getBytes("image");

        if (imageBytes != null && imageBytes.length > 0) {
            try {
                BufferedImage image =
                        ImageIO.read(new ByteArrayInputStream(imageBytes));

                if (image != null) {
                    content.add(left(sectionTitle("Attached Photo")));
                    content.add(Box.createVerticalStrut(6));
                    content.add(left(photoThumbnail(image,
                            rs.getString("image_name"))));
                    content.add(Box.createVerticalStrut(14));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Rating and feedback
        int rating = rs.getInt("rating");

        if (!rs.wasNull() && rating > 0) {
            JPanel ratingBox = UI.whiteBox(new BorderLayout(0, 6));

            JLabel stars = new JLabel(UI.stars(rating) + "   " + rating + "/5");
            stars.setFont(new Font("SansSerif", Font.BOLD, 18));
            stars.setForeground(new Color(245, 158, 11));
            ratingBox.add(stars, BorderLayout.NORTH);

            String feedback = rs.getString("feedback");
            if (feedback != null && !feedback.isBlank()) {
                ratingBox.add(UI.muted("\"" + feedback + "\""),
                        BorderLayout.CENTER);
            }

            content.add(left(sectionTitle("Student Rating")));
            content.add(Box.createVerticalStrut(6));
            content.add(left(ratingBox));
        }
    }

    private static JPanel buildTimeline(Connection con, int complaintId)
            throws SQLException {

        JPanel timeline = new JPanel();
        timeline.setLayout(new BoxLayout(timeline, BoxLayout.Y_AXIS));
        timeline.setOpaque(false);

        timeline.add(left(sectionTitle("Status Timeline")));
        timeline.add(Box.createVerticalStrut(8));

        String sql = """
                SELECT u.status, u.remarks, u.updated_at, a.name AS admin_name
                FROM complaint_updates u
                LEFT JOIN admins a ON u.admin_id = a.admin_id
                WHERE u.complaint_id = ?
                ORDER BY u.updated_at ASC, u.update_id ASC
                """;

        boolean any = false;

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, complaintId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    any = true;

                    String status = rs.getString("status");
                    String admin = rs.getString("admin_name");
                    String remarks = rs.getString("remarks");

                    timeline.add(left(timelineEntry(
                            status,
                            "by " + (admin == null ? "Admin" : admin)
                                    + "  •  " + UI.formatDate(rs.getTimestamp("updated_at")),
                            remarks == null || remarks.isBlank()
                                    ? "No remarks provided" : remarks)));
                    timeline.add(Box.createVerticalStrut(8));
                }
            }
        }

        if (!any) {
            timeline.add(left(UI.muted(
                    "No admin updates yet. Your complaint is waiting for review.")));
        }

        return timeline;
    }

    private static JPanel timelineEntry(String status, String meta, String remarks) {

        Color color = UI.statusColor(status);

        JPanel entry = new JPanel(new BorderLayout(0, 4));
        entry.setBackground(Color.WHITE);
        entry.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, color),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UI.BORDER),
                        new EmptyBorder(10, 14, 10, 14))));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        top.add(UI.badge(status, color));

        JLabel metaLabel = UI.muted(meta);
        metaLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        top.add(metaLabel);

        JTextArea text = new JTextArea(remarks);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setOpaque(false);
        text.setFont(new Font("SansSerif", Font.PLAIN, 13));
        text.setForeground(UI.DARK);

        entry.add(top, BorderLayout.NORTH);
        entry.add(text, BorderLayout.CENTER);

        return entry;
    }

    private static JComponent photoThumbnail(BufferedImage image, String name) {

        int maxWidth = 320;
        int maxHeight = 220;
        double scale = Math.min(1.0, Math.min(
                maxWidth / (double) image.getWidth(),
                maxHeight / (double) image.getHeight()));

        Image small = image.getScaledInstance(
                (int) (image.getWidth() * scale),
                (int) (image.getHeight() * scale),
                Image.SCALE_SMOOTH);

        JLabel label = new JLabel(new ImageIcon(small));
        label.setText("Click to enlarge" + (name == null ? "" : "  (" + name + ")"));
        label.setHorizontalTextPosition(SwingConstants.CENTER);
        label.setVerticalTextPosition(SwingConstants.BOTTOM);
        label.setForeground(UI.MUTED);
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        label.setBorder(BorderFactory.createLineBorder(UI.BORDER));

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showFullImage(label, image);
            }
        });

        return label;
    }

    private static void showFullImage(Component parent, BufferedImage image) {

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        double scale = Math.min(1.0, Math.min(
                (screen.width - 160) / (double) image.getWidth(),
                (screen.height - 200) / (double) image.getHeight()));

        Image scaled = image.getScaledInstance(
                (int) (image.getWidth() * scale),
                (int) (image.getHeight() * scale),
                Image.SCALE_SMOOTH);

        JDialog viewer = new JDialog(
                SwingUtilities.getWindowAncestor(parent),
                "Complaint Photo",
                ModalityType.APPLICATION_MODAL);

        viewer.add(new JScrollPane(new JLabel(new ImageIcon(scaled))));
        viewer.pack();
        viewer.setLocationRelativeTo(parent);
        viewer.setVisible(true);
    }

    private static void addInfo(JPanel panel, String label, String value) {

        JPanel cell = new JPanel(new BorderLayout(0, 2));
        cell.setOpaque(false);

        JLabel name = UI.muted(label);
        name.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JLabel text = new JLabel(value == null ? "-" : value);
        text.setFont(new Font("SansSerif", Font.BOLD, 14));
        text.setForeground(UI.DARK);

        cell.add(name, BorderLayout.NORTH);
        cell.add(text, BorderLayout.CENTER);
        panel.add(cell);
    }

    private static JLabel sectionTitle(String text) {
        return UI.heading(text, 15);
    }

    // BoxLayout needs every child left-aligned
    private static <T extends JComponent> T left(T component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }

    // Panel that always fits the dialog width, so long text wraps
    // instead of making the dialog scroll sideways
    private static class WidthTrackingPanel extends JPanel implements Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return 120;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
