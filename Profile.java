import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// "My Profile" screen for students and admins.
// Shows the user's details and lets them edit them.
public class Profile extends JPanel {

    private final MainFrame frame;
    private final boolean admin;
    private final int userId;
    private final Consumer<String> onNameChanged;

    private final JTextField nameField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JTextField departmentField = new JTextField();
    private final JTextField yearField = new JTextField();

    // Round gradient avatar with initials
    private final JLabel avatarLabel = new JLabel("", SwingConstants.CENTER) {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            int size = Math.min(getWidth(), getHeight()) - 6;
            int x = (getWidth() - size) / 2;
            Color a = admin ? new Color(16, 185, 129) : new Color(59, 130, 246);
            Color b = admin ? new Color(14, 165, 233) : new Color(236, 72, 153);
            g2.setColor(new Color(a.getRed(), a.getGreen(), a.getBlue(), 50));
            g2.fillOval(x - 3, 0, size + 6, size + 6);
            g2.setPaint(new GradientPaint(x, 3, a, x + size, 3 + size, b));
            g2.fillOval(x, 3, size, size);
            g2.dispose();
            super.paintComponent(g);
        }
    };
    private final JLabel nameTitle = UI.heading("", 22);
    private final JLabel roleLabel = UI.muted("");
    private final JLabel memberSinceLabel = UI.muted("");
    private final JLabel lastLoginLabel = UI.muted("");

    private final JLabel statOneValue = new JLabel("0");
    private final JLabel statTwoValue = new JLabel("0");
    private final JLabel statThreeValue = new JLabel("0");

    private final JButton editButton = UI.primaryButton("Edit Profile");
    private final JButton saveButton = UI.primaryButton("Save Changes");
    private final JButton cancelButton = UI.secondaryButton("Cancel");

    // onNameChanged lets the dashboard update "Welcome, <name>" after a save
    public Profile(MainFrame frame, boolean admin, int userId,
                   Consumer<String> onNameChanged) {

        this.frame = frame;
        this.admin = admin;
        this.userId = userId;
        this.onNameChanged = onNameChanged;

        setBackground(UI.BACKGROUND);

        // ================= LEFT: PROFILE CARD =================

        avatarLabel.setFont(new Font("SansSerif", Font.BOLD, 34));
        avatarLabel.setForeground(Color.WHITE);
        avatarLabel.setOpaque(false);
        avatarLabel.setPreferredSize(new Dimension(100, 100));
        avatarLabel.setMaximumSize(new Dimension(100, 100));
        avatarLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        for (JLabel label : new JLabel[]{nameTitle, roleLabel,
                memberSinceLabel, lastLoginLabel}) {
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        Theme.Card card = new Theme.Card(null, 18);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAccent(admin ? new Color(16, 185, 129) : new Color(99, 102, 241));
        card.add(avatarLabel);
        card.add(Box.createVerticalStrut(14));
        card.add(nameTitle);
        card.add(Box.createVerticalStrut(4));
        card.add(roleLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(memberSinceLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(lastLoginLabel);

        // Small stats under the card
        JPanel stats = UI.whiteBox(new GridLayout(3, 1, 0, 8));
        if (admin) {
            stats.add(statRow("Updates Made", statOneValue, UI.PRIMARY));
            stats.add(statRow("Complaints Resolved", statTwoValue, UI.RESOLVED));
            stats.add(statRow("Complaints Handled", statThreeValue, UI.IN_PROGRESS));
        } else {
            stats.add(statRow("Complaints Raised", statOneValue, UI.PRIMARY));
            stats.add(statRow("Resolved", statTwoValue, UI.RESOLVED));
            stats.add(statRow("Still Open", statThreeValue, UI.SUBMITTED));
        }

        JPanel left = new JPanel(new BorderLayout(0, 14));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(280, 0));
        left.add(card, BorderLayout.NORTH);
        left.add(stats, BorderLayout.CENTER);
        left.add(Box.createVerticalStrut(0), BorderLayout.SOUTH);

        // ================= RIGHT: DETAILS FORM =================

        for (JTextField field : new JTextField[]{nameField, emailField,
                phoneField, departmentField, yearField}) {
            UI.styleField(field);
            field.setPreferredSize(new Dimension(260, 44));
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.weightx = 1.0;

        addField(form, gbc, "Full Name", nameField, 0, 0, 2);
        addField(form, gbc, "Email Address", emailField, 0, 1, 2);

        if (!admin) {
            addField(form, gbc, "Phone Number", phoneField, 0, 2, 2);
            addField(form, gbc, "Department", departmentField, 0, 3, 1);
            addField(form, gbc, "Year (1-6)", yearField, 1, 3, 1);
        }

        JButton passwordButton = UI.secondaryButton("Change Password");
        passwordButton.addActionListener(e ->
                frame.navigateTo(MainFrame.CHANGE_PASSWORD));

        editButton.addActionListener(e -> setEditing(true));
        cancelButton.addActionListener(e -> {
            loadProfile();
            setEditing(false);
        });
        saveButton.addActionListener(e -> saveProfile());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.setBorder(new EmptyBorder(14, 6, 0, 0));
        buttons.add(editButton);
        buttons.add(saveButton);
        buttons.add(cancelButton);
        buttons.add(passwordButton);

        JPanel formHeading = new JPanel(new GridLayout(2, 1, 0, 4));
        formHeading.setOpaque(false);
        formHeading.setBorder(new EmptyBorder(0, 6, 8, 0));
        formHeading.add(UI.heading("Personal Details", 18));
        formHeading.add(UI.muted("Click \"Edit Profile\" to change your details"));

        JPanel right = new Theme.Card(new BorderLayout(), 16);
        right.add(formHeading, BorderLayout.NORTH);
        right.add(form, BorderLayout.CENTER);
        right.add(buttons, BorderLayout.SOUTH);

        JPanel rightWrap = new JPanel(new BorderLayout());
        rightWrap.setOpaque(false);
        rightWrap.add(right, BorderLayout.NORTH);

        // ================= PAGE =================

        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 4));
        heading.setOpaque(false);
        heading.add(UI.heading("My Profile", 27));
        heading.add(UI.muted("View and update your account information"));

        JPanel body = new JPanel(new BorderLayout(18, 0));
        body.setOpaque(false);
        body.add(left, BorderLayout.WEST);
        body.add(rightWrap, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(26, 30, 26, 30));
        content.setPreferredSize(new Dimension(860, admin ? 560 : 600));
        content.add(heading, BorderLayout.NORTH);
        content.add(body, BorderLayout.CENTER);

        MainFrame.placeCentered(this, content);

        setEditing(false);

        // Fresh data every time the screen is opened
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadProfile();
                setEditing(false);
            }
        });
    }

    // One line: "Complaints Raised .......... 3"
    private static JPanel statRow(String text, JLabel value, Color color) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel label = UI.muted(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        value.setFont(new Font("SansSerif", Font.BOLD, 20));
        value.setForeground(color);
        row.add(label, BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        return row;
    }

    private void addField(JPanel panel, GridBagConstraints gbc, String text,
                          JComponent input, int column, int row, int span) {

        JPanel cell = new JPanel(new BorderLayout(0, 6));
        cell.setOpaque(false);
        cell.add(UI.fieldLabel(text), BorderLayout.NORTH);
        cell.add(input, BorderLayout.CENTER);

        gbc.gridx = column;
        gbc.gridy = row;
        gbc.gridwidth = span;
        panel.add(cell, gbc);
        gbc.gridwidth = 1;
    }

    private void setEditing(boolean editing) {

        for (JTextField field : new JTextField[]{nameField, emailField,
                phoneField, departmentField, yearField}) {
            field.setEditable(editing);
            field.setBackground(editing ? UI.FIELD_BG : UI.CARD_ALT);
        }

        editButton.setVisible(!editing);
        saveButton.setVisible(editing);
        cancelButton.setVisible(editing);

        if (editing) {
            nameField.requestFocusInWindow();
        }
    }

    // ================= LOAD =================

    private void loadProfile() {

        String sql = admin
                ? "SELECT * FROM admins WHERE admin_id = ?"
                : "SELECT * FROM students WHERE student_id = ?";

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                JOptionPane.showMessageDialog(frame, "Database connection failed!");
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, userId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        JOptionPane.showMessageDialog(frame, "Account not found!");
                        return;
                    }

                    String name = rs.getString("name");

                    nameField.setText(name);
                    emailField.setText(rs.getString("email"));
                    nameTitle.setText(name);
                    avatarLabel.setText(initials(name));
                    java.sql.Timestamp since = rs.getTimestamp("created_at");
                    memberSinceLabel.setText("Member since: " + (since == null
                            ? "-" : new java.text.SimpleDateFormat("dd MMM yyyy")
                                    .format(since)));

                    if (admin) {
                        roleLabel.setText("Administrator");
                        lastLoginLabel.setText(" ");
                    } else {
                        phoneField.setText(rs.getString("phone"));
                        departmentField.setText(rs.getString("department"));
                        yearField.setText(String.valueOf(rs.getInt("year")));
                        roleLabel.setText("Student  •  " + rs.getString("department")
                                + ", Year " + rs.getInt("year"));
                        lastLoginLabel.setText("Previous login: "
                                + UI.formatDate(rs.getTimestamp("last_login")));
                    }
                }
            }

            loadStats(con);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame,
                    "Could not load profile!\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadStats(Connection con) throws SQLException {

        String[] queries = admin
                ? new String[]{
                    "SELECT COUNT(*) FROM complaint_updates WHERE admin_id = ?",
                    "SELECT COUNT(DISTINCT complaint_id) FROM complaint_updates "
                        + "WHERE admin_id = ? AND status = 'Resolved'",
                    "SELECT COUNT(DISTINCT complaint_id) FROM complaint_updates "
                        + "WHERE admin_id = ?"}
                : new String[]{
                    "SELECT COUNT(*) FROM complaints WHERE student_id = ?",
                    "SELECT COUNT(*) FROM complaints WHERE student_id = ? "
                        + "AND status = 'Resolved'",
                    "SELECT COUNT(*) FROM complaints WHERE student_id = ? "
                        + "AND status IN ('Submitted', 'In Progress')"};

        JLabel[] labels = {statOneValue, statTwoValue, statThreeValue};

        for (int i = 0; i < queries.length; i++) {
            try (PreparedStatement ps = con.prepareStatement(queries[i])) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    labels[i].setText(rs.next() ? String.valueOf(rs.getInt(1)) : "0");
                }
            }
        }
    }

    // ================= SAVE =================

    private void saveProfile() {

        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String department = departmentField.getText().trim();
        String yearText = yearField.getText().trim();

        if (name.isEmpty() || email.isEmpty()
                || (!admin && (phone.isEmpty() || department.isEmpty()
                               || yearText.isEmpty()))) {
            JOptionPane.showMessageDialog(frame, "Please fill in all fields!");
            return;
        }

        if (name.length() > 100) {
            JOptionPane.showMessageDialog(frame, "Name cannot exceed 100 characters.");
            return;
        }

        // Same email rules as registration
        boolean validEmail = admin
                ? email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
                : email.matches("^[A-Za-z0-9._%+-]+@gmail\\.com$"
                        + "|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.org$");

        if (!validEmail || email.length() > 100) {
            JOptionPane.showMessageDialog(frame, admin
                    ? "Please enter a valid email address."
                    : "Enter a valid Gmail address or .org email!");
            return;
        }

        int year = 0;

        if (!admin) {
            if (!phone.matches("^[6-9][0-9]{9}$")) {
                JOptionPane.showMessageDialog(frame,
                        "Enter a valid 10-digit Indian mobile number!");
                return;
            }

            try {
                year = Integer.parseInt(yearText);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(frame, "Year must be a number!");
                return;
            }

            if (year < 1 || year > 6) {
                JOptionPane.showMessageDialog(frame, "Year must be between 1 and 6!");
                return;
            }
        }

        String table = admin ? "admins" : "students";
        String idColumn = admin ? "admin_id" : "student_id";

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                JOptionPane.showMessageDialog(frame, "Database connection failed!");
                return;
            }

            // Email must not belong to another account
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM " + table
                    + " WHERE LOWER(email) = LOWER(?) AND " + idColumn + " <> ?")) {
                ps.setString(1, email);
                ps.setInt(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        JOptionPane.showMessageDialog(frame,
                                "This email is already used by another account.");
                        return;
                    }
                }
            }

            String sql = admin
                    ? "UPDATE admins SET name = ?, email = ? WHERE admin_id = ?"
                    : "UPDATE students SET name = ?, email = ?, phone = ?, "
                      + "department = ?, year = ? WHERE student_id = ?";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, email);

                if (admin) {
                    ps.setInt(3, userId);
                } else {
                    ps.setString(3, phone);
                    ps.setString(4, department);
                    ps.setInt(5, year);
                    ps.setInt(6, userId);
                }

                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(frame, "Profile updated successfully!");

            if (onNameChanged != null) {
                onNameChanged.accept(name);
            }

            loadProfile();
            setEditing(false);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame,
                    "Could not update profile!\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    // "Rakshit Chaudhary" -> "RC"
    private static String initials(String name) {

        if (name == null || name.isBlank()) {
            return "?";
        }

        String[] parts = name.trim().split("\\s+");
        String result = parts[0].substring(0, 1);

        if (parts.length > 1) {
            result += parts[parts.length - 1].substring(0, 1);
        }

        return result.toUpperCase();
    }
}
