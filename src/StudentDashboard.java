import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class StudentDashboard extends JPanel {

    private final MainFrame frame;
    private final int studentId;
    private final String studentName;

    private final JLabel totalValue = new JLabel("0");
    private final JLabel submittedValue = new JLabel("0");
    private final JLabel progressValue = new JLabel("0");
    private final JLabel resolvedValue = new JLabel("0");

    private final JPanel activityList = new JPanel();

    private JLabel welcomeLabel;

    // previousLogin = time of the last login before this one (null = first login)
    public StudentDashboard(
            MainFrame frame, int studentId, String studentName,
            Timestamp previousLogin) {

        this.frame = frame;
        this.studentId = studentId;
        this.studentName = studentName;

        setLayout(new BorderLayout());
        setBackground(UI.BACKGROUND);

        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(24, 28, 22, 28));

        // ================= HEADER =================

        JPanel headingText = new JPanel();
        headingText.setLayout(new BoxLayout(headingText, BoxLayout.Y_AXIS));
        headingText.setOpaque(false);
        JLabel hello = new JLabel("STUDENT DASHBOARD");
        hello.setFont(new Font("SansSerif", Font.BOLD, 11));
        hello.setForeground(new Color(255, 255, 255, 190));

        welcomeLabel = UI.heading("Welcome, " + studentName + "!", 28);
        welcomeLabel.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Manage your campus complaints here.");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 14));
        sub.setForeground(new Color(255, 255, 255, 215));

        headingText.add(hello);
        headingText.add(Box.createVerticalStrut(6));
        headingText.add(welcomeLabel);
        headingText.add(Box.createVerticalStrut(4));
        headingText.add(sub);

        JButton profileButton = UI.glassButton("My Profile");
        profileButton.addActionListener(e ->
                frame.navigateTo(MainFrame.PROFILE));

        JButton logoutButton = UI.glassButton("Logout");

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

        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        headerButtons.setOpaque(false);
        headerButtons.add(profileButton);
        headerButtons.add(logoutButton);

        // ================= NOTIFICATION (inside the banner) =================

        JLabel banner = new JLabel(bannerText(previousLogin)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Theme.smooth(g);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(new Color(253, 224, 71));
                g2.fillOval(12, getHeight() / 2 - 5, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        banner.setFont(new Font("SansSerif", Font.BOLD, 13));
        banner.setForeground(Color.WHITE);
        banner.setBorder(new EmptyBorder(9, 32, 9, 14));

        JPanel bannerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bannerRow.setOpaque(false);
        bannerRow.add(banner);

        Theme.HeroPanel header = new Theme.HeroPanel(new BorderLayout(0, 16),
                new Color(37, 99, 235), new Color(124, 58, 237));
        header.setBorder(new EmptyBorder(22, 26, 26, 26));

        JPanel headerTop = new JPanel(new BorderLayout());
        headerTop.setOpaque(false);
        headerTop.add(headingText, BorderLayout.WEST);
        headerTop.add(headerButtons, BorderLayout.EAST);

        header.add(headerTop, BorderLayout.NORTH);
        header.add(bannerRow, BorderLayout.CENTER);

        // ================= STAT CARDS =================

        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.setOpaque(false);
        cards.add(UI.statCard("My Complaints", totalValue, UI.PRIMARY));
        cards.add(UI.statCard("Submitted", submittedValue, UI.SUBMITTED));
        cards.add(UI.statCard("In Progress", progressValue, UI.IN_PROGRESS));
        cards.add(UI.statCard("Resolved", resolvedValue, UI.RESOLVED));

        // ================= ACTIONS =================

        JButton submitButton = bigButton("+  Submit New Complaint", UI.PRIMARY);
        JButton myComplaintsButton = bigButton("My Complaints & History", UI.DARK);

        submitButton.addActionListener(e ->
                frame.navigateTo(MainFrame.SUBMIT_COMPLAINT));

        myComplaintsButton.addActionListener(e ->
                frame.navigateTo(MainFrame.MY_COMPLAINTS));

        JPanel actions = new JPanel(new GridLayout(1, 2, 14, 0));
        actions.setOpaque(false);
        actions.add(submitButton);
        actions.add(myComplaintsButton);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);

        for (JComponent c : new JComponent[]{header, cards, actions}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                    c.getPreferredSize().height));
            top.add(c);
            top.add(Box.createVerticalStrut(14));
        }

        // ================= RECENT ACTIVITY =================

        activityList.setLayout(new BoxLayout(activityList, BoxLayout.Y_AXIS));
        activityList.setBackground(Color.WHITE);

        JScrollPane activityScroll = new JScrollPane(activityList);
        activityScroll.setBorder(BorderFactory.createEmptyBorder());
        activityScroll.getViewport().setBackground(Color.WHITE);
        activityScroll.getVerticalScrollBar().setUnitIncrement(16);

        Theme.Card activity = new Theme.Card(new BorderLayout(0, 10), 10);
        activity.add(UI.heading("Recent Updates on Your Complaints", 16),
                BorderLayout.NORTH);
        activity.add(activityScroll, BorderLayout.CENTER);

        main.add(top, BorderLayout.NORTH);
        main.add(activity, BorderLayout.CENTER);

        add(main, BorderLayout.CENTER);

        // Refresh numbers every time the dashboard is opened
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadData();
            }
        });
    }

    private JButton bigButton(String text, Color color) {
        JButton button = UI.primaryButton(text);
        button.setBackground(color);
        button.setFont(new Font("SansSerif", Font.BOLD, 15));
        button.setPreferredSize(new Dimension(0, 56));
        return button;
    }

    // Message telling the student how many admin updates arrived
    // since their last login
    private String bannerText(Timestamp previousLogin) {

        if (previousLogin == null) {
            return "Welcome to Campus Care! All your complaints and admin "
                    + "updates will appear here.";
        }

        int count = 0;

        String sql = """
                SELECT COUNT(*)
                FROM complaint_updates u
                JOIN complaints c ON u.complaint_id = c.complaint_id
                WHERE c.student_id = ? AND u.updated_at > ?
                """;

        try (Connection con = DBConnection.getConnection()) {

            if (con != null) {
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, studentId);
                    ps.setTimestamp(2, previousLogin);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            count = rs.getInt(1);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        String last = "Last login: " + UI.formatDate(previousLogin);

        if (count == 0) {
            return "No new updates since your last visit.   " + last;
        }

        return "You have " + count + " new update"
                + (count == 1 ? "" : "s")
                + " on your complaints since your last visit.   " + last;
    }

    private void loadData() {

        int total = 0;
        int submitted = 0;
        int inProgress = 0;
        int resolved = 0;

        activityList.removeAll();

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT status, COUNT(*) AS total FROM complaints "
                    + "WHERE student_id = ? GROUP BY status")) {

                ps.setInt(1, studentId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String status = rs.getString("status");
                        int count = rs.getInt("total");
                        total += count;

                        if ("Submitted".equals(status)) {
                            submitted = count;
                        } else if ("In Progress".equals(status)) {
                            inProgress = count;
                        } else if ("Resolved".equals(status)) {
                            resolved = count;
                        }
                    }
                }
            }

            String activitySql = """
                    SELECT u.status, u.remarks, u.updated_at,
                           c.title, c.complaint_id
                    FROM complaint_updates u
                    JOIN complaints c ON u.complaint_id = c.complaint_id
                    WHERE c.student_id = ?
                    ORDER BY u.updated_at DESC, u.update_id DESC
                    LIMIT 8
                    """;

            try (PreparedStatement ps = con.prepareStatement(activitySql)) {

                ps.setInt(1, studentId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        activityList.add(activityRow(
                                rs.getInt("complaint_id"),
                                rs.getString("title"),
                                rs.getString("status"),
                                rs.getString("remarks"),
                                rs.getTimestamp("updated_at")));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (activityList.getComponentCount() == 0) {
            JLabel empty = UI.muted("   No updates yet. When an admin works on "
                    + "your complaint, it will show up here.");
            empty.setBorder(new EmptyBorder(16, 6, 16, 6));
            activityList.add(empty);
        }

        totalValue.setText(String.valueOf(total));
        submittedValue.setText(String.valueOf(submitted));
        progressValue.setText(String.valueOf(inProgress));
        resolvedValue.setText(String.valueOf(resolved));

        activityList.revalidate();
        activityList.repaint();
    }

    private JPanel activityRow(
            int complaintId, String title, String status,
            String remarks, Timestamp time) {

        JPanel row = new JPanel(new BorderLayout(12, 2));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UI.BORDER),
                new EmptyBorder(10, 14, 10, 14)));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        JLabel titleLabel = new JLabel("#" + complaintId + "  " + title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        titleLabel.setForeground(UI.DARK);

        JLabel remarksLabel = UI.muted(remarks == null || remarks.isBlank()
                ? "Status changed" : remarks);

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        text.add(titleLabel);
        text.add(remarksLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.setOpaque(false);
        JLabel when = UI.muted(UI.timeAgo(time));
        when.setFont(new Font("SansSerif", Font.PLAIN, 12));
        right.add(when);
        right.add(UI.badge(status, UI.statusColor(status)));

        row.add(text, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);

        row.setCursor(new Cursor(Cursor.HAND_CURSOR));
        row.setToolTipText("Click to see full details");
        row.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                ComplaintDetailsDialog.open(frame, complaintId, studentId);
            }
        });

        return row;
    }

    // Called by the Profile screen after the student changes their name
    public void setDisplayName(String name) {
        welcomeLabel.setText("Welcome, " + name + "!");
    }

    public int getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }
}
