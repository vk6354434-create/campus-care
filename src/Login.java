import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class Login extends JPanel {

    private final MainFrame frame;

    private JTextField emailField;
    private JPasswordField passwordField;

    private final Color backgroundColor = new Color(245, 247, 251);
    private final Color primaryColor = new Color(37, 99, 235);

    public Login(MainFrame frame) {

        this.frame = frame;

        setBackground(backgroundColor);

        // Heading
        JPanel headingPanel = new JPanel();
        headingPanel.setLayout(
                new BoxLayout(headingPanel, BoxLayout.Y_AXIS)
        );
        headingPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Welcome Back!");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 27));
        titleLabel.setForeground(new Color(31, 41, 55));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel(
                "Login to your student account"
        );
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(107, 114, 128));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        headingPanel.add(titleLabel);
        headingPanel.add(Box.createVerticalStrut(8));
        headingPanel.add(subtitleLabel);
        headingPanel.add(Box.createVerticalStrut(25));

        // Email field
        JLabel emailLabel = new JLabel("Email Address");
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

        JButton eyeButton = new JButton("Show");
        eyeButton.setFocusable(false);
        eyeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        eyeButton.addActionListener(e -> {
            if (passwordField.getEchoChar() != (char) 0) {
                passwordField.setEchoChar((char) 0);
                eyeButton.setText("Hide");
            } else {
                passwordField.setEchoChar('•');
                eyeButton.setText("Show");
            }
        });

        JPanel passwordInputPanel = new JPanel(
                new BorderLayout(6, 0)
        );
        passwordInputPanel.setOpaque(false);
        passwordInputPanel.add(passwordField, BorderLayout.CENTER);
        passwordInputPanel.add(eyeButton, BorderLayout.EAST);

        JPanel passwordPanel = new JPanel(
                new BorderLayout(0, 8)
        );
        passwordPanel.setOpaque(false);
        passwordPanel.add(passwordLabel, BorderLayout.NORTH);
        passwordPanel.add(passwordInputPanel, BorderLayout.CENTER);

        // Login button
        JButton loginButton = new JButton("Login");
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 15));
        loginButton.setForeground(Color.WHITE);
        loginButton.setBackground(primaryColor);
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setPreferredSize(new Dimension(0, 45));

        loginButton.addActionListener(e -> loginStudent());

        // Pressing Enter in the password field also logs in
        passwordField.addActionListener(e -> loginStudent());

        // Registration navigation
        JButton registerButton = new JButton("Create New Account");
        registerButton.setFont(new Font("SansSerif", Font.PLAIN, 13));
        registerButton.setFocusPainted(false);
        registerButton.setBorderPainted(false);
        registerButton.setContentAreaFilled(false);
        registerButton.setForeground(primaryColor);
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        registerButton.addActionListener(e ->
                frame.navigateTo(MainFrame.STUDENT_REGISTER)
        );

        JPanel buttonPanel = new JPanel(
                new GridLayout(2, 1, 0, 8)
        );
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(22, 0, 0, 0));
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        JPanel formPanel = new JPanel(
                new GridLayout(2, 1, 0, 18)
        );
        formPanel.setOpaque(false);
        formPanel.add(emailPanel);
        formPanel.add(passwordPanel);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(35, 40, 35, 40));
        contentPanel.add(headingPanel, BorderLayout.NORTH);
        contentPanel.add(formPanel, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        UI.authScreen(this, contentPanel, false);
    }

    private void styleField(JComponent field) {

        UI.styleField(field);
        field.setPreferredSize(new Dimension(340, 46));
    }

    // Student login
    private void loginStudent() {

        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Please enter email and password!"
            );
            return;
        }

        String sql = """
                SELECT student_id, name, password, last_login
                FROM students
                WHERE email = ?
                """;

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                JOptionPane.showMessageDialog(
                        frame,
                        "Database connection failed!"
                );
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, email);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next() && PasswordUtil.verify(
                            password, rs.getString("password"))) {

                        int studentId = rs.getInt("student_id");
                        String studentName = rs.getString("name");
                        String stored = rs.getString("password");
                        Timestamp previousLogin = rs.getTimestamp("last_login");

                        recordLogin(con, studentId, stored, password);

                        JOptionPane.showMessageDialog(
                                frame,
                                "Login Successful!\nWelcome, "
                                        + studentName
                        );

                        // Clear the form for the next user
                        emailField.setText("");
                        passwordField.setText("");

                        try {
                            frame.openStudentArea(
                                    studentId, studentName, previousLogin);
                        } catch (Throwable t) {
                            // Show the real reason instead of staying
                            // silently on the login screen
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

                    } else {
                        JOptionPane.showMessageDialog(
                                frame,
                                "Invalid email or password!"
                        );
                    }
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Login failed!\n" + e.getMessage()
            );
            e.printStackTrace();
        }
    }

    // Save the login time, and upgrade an old plain-text password
    // to a secure hash the first time the student logs in
    private void recordLogin(
            Connection con, int studentId, String stored, String password) {

        try {
            if (PasswordUtil.isLegacy(stored)) {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE students SET password = ? WHERE student_id = ?")) {
                    ps.setString(1, PasswordUtil.hash(password));
                    ps.setInt(2, studentId);
                    ps.executeUpdate();
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE students SET last_login = CURRENT_TIMESTAMP "
                    + "WHERE student_id = ?")) {
                ps.setInt(1, studentId);
                ps.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
