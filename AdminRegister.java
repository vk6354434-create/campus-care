import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class AdminRegister extends JPanel {

    private final MainFrame frame;

    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;

    public AdminRegister(MainFrame frame) {

        this.frame = frame;

        Color background = UI.BACKGROUND;
        Color blue = new Color(37, 99, 235);

        setBackground(background);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(background);
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(25, 35, 25, 35)
        );

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        JLabel heading = new JLabel("Admin Registration");
        heading.setFont(new Font("SansSerif", Font.BOLD, 26));
        heading.setForeground(UI.DARK);
        heading.setHorizontalAlignment(SwingConstants.CENTER);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(heading, gbc);

        JLabel subtitle =
                new JLabel("Create your administrator account");
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        subtitle.setForeground(UI.MUTED);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));

        gbc.gridy = 1;
        mainPanel.add(subtitle, gbc);

        gbc.gridwidth = 1;

        // Full Name
        gbc.gridx = 0;
        gbc.gridy = 2;
        mainPanel.add(new JLabel("Full Name"), gbc);

        nameField = new JTextField(20);
        gbc.gridx = 1;
        mainPanel.add(nameField, gbc);

        // Email
        gbc.gridx = 0;
        gbc.gridy = 3;
        mainPanel.add(new JLabel("Email Address"), gbc);

        emailField = new JTextField(20);
        gbc.gridx = 1;
        mainPanel.add(emailField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 4;
        mainPanel.add(new JLabel("Password"), gbc);

        passwordField = new JPasswordField(20);
        gbc.gridx = 1;
        mainPanel.add(passwordField, gbc);

        // Confirm Password
        gbc.gridx = 0;
        gbc.gridy = 5;
        mainPanel.add(new JLabel("Confirm Password"), gbc);

        confirmPasswordField = new JPasswordField(20);
        gbc.gridx = 1;
        mainPanel.add(confirmPasswordField, gbc);

        // Show/Hide Password
        JCheckBox showPassword = new JCheckBox("Show passwords");
        showPassword.setBackground(background);

        showPassword.addActionListener(e -> {
            char echo = showPassword.isSelected()
                    ? (char) 0 : '\u2022';

            passwordField.setEchoChar(echo);
            confirmPasswordField.setEchoChar(echo);
        });

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        mainPanel.add(showPassword, gbc);

        // Register Button
        JButton registerButton =
                new JButton("Create Admin Account");

        registerButton.setBackground(blue);
        registerButton.setForeground(Color.WHITE);
        registerButton.setFocusPainted(false);
        registerButton.setPreferredSize(new Dimension(0, 46));

        gbc.gridy = 7;
        mainPanel.add(registerButton, gbc);

        // Back to Login
        JButton loginButton =
                new JButton("Already registered? Admin Login");

        gbc.gridy = 8;
        mainPanel.add(loginButton, gbc);

        registerButton.addActionListener(e -> registerAdmin());

        loginButton.addActionListener(e ->
                frame.navigateTo(MainFrame.ADMIN_LOGIN));

        // Modern rounded fields
        for (JComponent field : new JComponent[]{
                nameField, emailField, passwordField, confirmPasswordField}) {
            UI.styleField(field);
            field.setPreferredSize(new Dimension(280, 44));
        }

        for (Component c : mainPanel.getComponents()) {
            if (c instanceof JLabel label && label != heading && label != subtitle) {
                label.setFont(new Font("SansSerif", Font.BOLD, 13));
                label.setForeground(UI.LABEL);
            }
        }

        showPassword.setOpaque(false);
        loginButton.setBackground(Color.WHITE);

        // The form sits in a card next to the admin banner
        mainPanel.setOpaque(false);
        UI.authScreen(this, mainPanel, true);
    }

    private void registerAdmin() {

        String name = nameField.getText().trim();
        String email = emailField.getText().trim();

        String password =
                new String(passwordField.getPassword());

        String confirmPassword =
                new String(confirmPasswordField.getPassword());

        // Validate empty fields
        if (name.isEmpty() || email.isEmpty()
                || password.isEmpty()
                || confirmPassword.isEmpty()) {

            JOptionPane.showMessageDialog(
                    frame,
                    "Please fill in all fields."
            );
            return;
        }

        // Validate name
        if (name.length() > 100) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Name cannot exceed 100 characters."
            );
            return;
        }

        // Validate email
        if (!email.matches(
                "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
                || email.length() > 100) {

            JOptionPane.showMessageDialog(
                    frame,
                    "Please enter a valid email address."
            );
            return;
        }

        // Validate password
        if (password.length() < 8
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[a-z].*")
                || !password.matches(".*[0-9].*")
                || !password.matches(".*[^a-zA-Z0-9].*")) {

            JOptionPane.showMessageDialog(
                    frame,
                    "Password must have at least 8 characters, "
                    + "uppercase, lowercase, number and special character."
            );
            return;
        }

        // Confirm password
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Passwords do not match."
            );
            return;
        }

        // Create a normal Admin account directly
        String sql = """
                INSERT INTO admins
                    (name, email, password, role, approval_status)
                VALUES (?, ?, ?, 'ADMIN', 'APPROVED')
                """;

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                JOptionPane.showMessageDialog(
                        frame,
                        "Database connection failed."
                );
                return;
            }

            try (PreparedStatement ps =
                    con.prepareStatement(sql)) {

                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, PasswordUtil.hash(password));

                int rowsInserted = ps.executeUpdate();

                if (rowsInserted > 0) {

                    JOptionPane.showMessageDialog(
                            frame,
                            "Admin account created successfully!\n"
                            + "You can now log in."
                    );

                    clearForm();
                    frame.navigateTo(MainFrame.ADMIN_LOGIN);
                }
            }

        } catch (SQLIntegrityConstraintViolationException ex) {

            JOptionPane.showMessageDialog(
                    frame,
                    "This email is already registered. "
                    + "Please use another email."
            );

        } catch (SQLException ex) {

            // 1062 = duplicate entry (some drivers use a plain SQLException)
            if (ex.getErrorCode() == 1062 || "23000".equals(ex.getSQLState())
                    || String.valueOf(ex.getMessage()).toLowerCase().contains("duplicate")) {
                JOptionPane.showMessageDialog(
                        frame,
                        "This email is already registered. "
                        + "Please use another email."
                );
                return;
            }

            JOptionPane.showMessageDialog(
                    frame,
                    "Registration failed: " + ex.getMessage()
            );

            ex.printStackTrace();
        }
    }

    private void clearForm() {
        nameField.setText("");
        emailField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
    }
}
