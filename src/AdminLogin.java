import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class AdminLogin extends JPanel {

    private final MainFrame frame;

    private JTextField emailField;
    private JPasswordField passwordField;

    public AdminLogin(MainFrame frame) {

        this.frame = frame;

        Color backgroundColor = new Color(245, 247, 251);
        Color primaryColor = new Color(37, 99, 235);

        setBackground(backgroundColor);

        // Heading
        JLabel titleLabel = new JLabel("Admin Login");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 27));
        titleLabel.setForeground(new Color(31, 41, 55));

        JLabel subtitleLabel = new JLabel(
                "Sign in to manage campus complaints");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(107, 114, 128));

        JPanel headingPanel = new JPanel();
        headingPanel.setLayout(
                new BoxLayout(headingPanel, BoxLayout.Y_AXIS));
        headingPanel.setOpaque(false);

        headingPanel.add(titleLabel);
        headingPanel.add(Box.createVerticalStrut(8));
        headingPanel.add(subtitleLabel);

        // Email field
        JLabel emailLabel = new JLabel("Admin Email");
        emailLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        emailField = new JTextField();
        styleField(emailField);

        JPanel emailPanel = new JPanel(new BorderLayout(0, 8));
        emailPanel.setOpaque(false);
        emailPanel.add(emailLabel, BorderLayout.NORTH);
        emailPanel.add(emailField, BorderLayout.CENTER);

        // Password field
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        passwordField = new JPasswordField();
        styleField(passwordField);

        JButton showButton = new JButton("Show");
        showButton.setFocusable(false);

        showButton.addActionListener(e -> {
            if (passwordField.getEchoChar() != (char) 0) {
                passwordField.setEchoChar((char) 0);
                showButton.setText("Hide");
            } else {
                passwordField.setEchoChar('•');
                showButton.setText("Show");
            }
        });

        JPanel passwordInputPanel =
                new JPanel(new BorderLayout(6, 0));
        passwordInputPanel.setOpaque(false);
        passwordInputPanel.add(
                passwordField, BorderLayout.CENTER);
        passwordInputPanel.add(showButton, BorderLayout.EAST);

        JPanel passwordPanel =
                new JPanel(new BorderLayout(0, 8));
        passwordPanel.setOpaque(false);
        passwordPanel.add(passwordLabel, BorderLayout.NORTH);
        passwordPanel.add(
                passwordInputPanel, BorderLayout.CENTER);

        // Login button
        JButton loginButton = new JButton("Login as Admin");
        loginButton.setFont(
                new Font("SansSerif", Font.BOLD, 15));
        loginButton.setForeground(Color.WHITE);
        loginButton.setBackground(primaryColor);
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setCursor(
                new Cursor(Cursor.HAND_CURSOR));
        loginButton.setPreferredSize(new Dimension(0, 45));

        loginButton.addActionListener(e -> loginAdmin());

        // Pressing Enter in the password field also logs in
        passwordField.addActionListener(e -> loginAdmin());

        // Register button
        JButton registerButton =
                new JButton("New Admin? Register Here");

        registerButton.setFont(
                new Font("SansSerif", Font.BOLD, 13));
        registerButton.setForeground(primaryColor);
        registerButton.setBackground(Color.WHITE);
        registerButton.setFocusPainted(false);
        registerButton.setCursor(
                new Cursor(Cursor.HAND_CURSOR));

        registerButton.addActionListener(e ->
                frame.navigateTo(MainFrame.ADMIN_REGISTER));

        // Form layout
        JPanel formPanel = new JPanel(
                new GridLayout(2, 1, 0, 18));
        formPanel.setOpaque(false);
        formPanel.add(emailPanel);
        formPanel.add(passwordPanel);

        // Buttons layout
        JPanel buttonPanel = new JPanel(
                new GridLayout(2, 1, 0, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        JPanel contentPanel = new JPanel(
                new BorderLayout(0, 25));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(35, 35, 30, 35));
        contentPanel.add(headingPanel, BorderLayout.NORTH);
        contentPanel.add(formPanel, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        UI.authScreen(this, contentPanel, true);
    }

    private void styleField(JComponent field) {
        UI.styleField(field);
        field.setPreferredSize(new Dimension(340, 46));
    }

    private void loginAdmin() {

        String email = emailField.getText().trim();
        String password =
                new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Please enter email and password!");
            return;
        }

        // Only normal Admin accounts are used in this flow.
        String sql = """
                SELECT admin_id, name, password
                FROM admins
                WHERE email = ?
                  AND role = 'ADMIN'
                  AND approval_status = 'APPROVED'
                """;

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                JOptionPane.showMessageDialog(
                        frame,
                        "Database connection failed!");
                return;
            }

            try (PreparedStatement ps =
                    con.prepareStatement(sql)) {

                ps.setString(1, email);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next() || !PasswordUtil.verify(
                            password, rs.getString("password"))) {
                        JOptionPane.showMessageDialog(
                                frame,
                                "Invalid admin email or password!");
                        return;
                    }

                    int adminId = rs.getInt("admin_id");
                    String adminName = rs.getString("name");

                    // Upgrade an old plain-text password to a secure hash
                    if (PasswordUtil.isLegacy(rs.getString("password"))) {
                        try (PreparedStatement upgrade = con.prepareStatement(
                                "UPDATE admins SET password = ? WHERE admin_id = ?")) {
                            upgrade.setString(1, PasswordUtil.hash(password));
                            upgrade.setInt(2, adminId);
                            upgrade.executeUpdate();
                        }
                    }

                    JOptionPane.showMessageDialog(
                            frame,
                            "Login successful! Welcome, "
                            + adminName + "!");

                    // Clear the form for the next user
                    emailField.setText("");
                    passwordField.setText("");

                    openDashboard(adminId, adminName);
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Admin login failed!\n" + e.getMessage());

            e.printStackTrace();
        }
    }

    // Opens the admin dashboard. If anything goes wrong (for example a
    // file in the project is old or has a compile error), show the real
    // reason instead of silently staying on the login screen.
    private void openDashboard(int adminId, String adminName) {
        try {
            frame.openAdminArea(adminId, adminName);
        } catch (Throwable t) {
            t.printStackTrace();
            JOptionPane.showMessageDialog(
                    frame,
                    "Login worked, but the dashboard could not open.\n\n"
                    + "Reason: " + t + "\n\n"
                    + "Check that ALL updated .java files are in the src folder\n"
                    + "and that VS Code shows no red errors (Ctrl + Shift + M).",
                    "Dashboard Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
