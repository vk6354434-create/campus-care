import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class Register extends JPanel {

    private final MainFrame frame;

    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JTextField phoneField;
    private JTextField departmentField;
    private JTextField yearField;

    private final Color backgroundColor = UI.BACKGROUND;
    private final Color primaryColor = new Color(37, 99, 235);

    public Register(MainFrame frame) {

        this.frame = frame;

        setBackground(backgroundColor);

        // Heading
        JPanel headingPanel = new JPanel();
        headingPanel.setLayout(
                new BoxLayout(headingPanel, BoxLayout.Y_AXIS));
        headingPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Create Student Account");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        titleLabel.setForeground(UI.DARK);

        JLabel subtitleLabel = new JLabel("Enter your details to get started");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitleLabel.setForeground(UI.MUTED);

        headingPanel.add(titleLabel);
        headingPanel.add(Box.createVerticalStrut(8));
        headingPanel.add(subtitleLabel);
        headingPanel.add(Box.createVerticalStrut(20));

        // Fields
        nameField = new JTextField();
        emailField = new JTextField();
        passwordField = new JPasswordField();
        phoneField = new JTextField();
        departmentField = new JTextField();
        yearField = new JTextField();

        styleField(nameField);
        styleField(emailField);
        styleField(passwordField);
        styleField(phoneField);
        styleField(departmentField);
        styleField(yearField);

        // Show / hide password
        JButton showButton = new JButton("Show");
        showButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        showButton.setFocusable(false);
        showButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        showButton.setBackground(Color.WHITE);
        showButton.setToolTipText("Show or hide password");

        showButton.addActionListener(e -> {
            if (passwordField.getEchoChar() != (char) 0) {
                passwordField.setEchoChar((char) 0);
                showButton.setText("Hide");
            } else {
                passwordField.setEchoChar('•');
                showButton.setText("Show");
            }
        });

        JPanel passwordPanel = new JPanel(new BorderLayout(5, 0));
        passwordPanel.setOpaque(false);
        passwordPanel.add(passwordField, BorderLayout.CENTER);
        passwordPanel.add(showButton, BorderLayout.EAST);

        // Form layout
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 6, 8, 6);
        gbc.weightx = 1.0;

        addField(formPanel, gbc, "Full Name", nameField, 0, 0, 1);
        addField(formPanel, gbc, "Phone Number", phoneField, 1, 0, 1);
        addField(formPanel, gbc, "Email (.gmail or .org)", emailField, 0, 1, 2);
        addField(formPanel, gbc, "Password", passwordPanel, 0, 2, 2);
        addField(formPanel, gbc, "Department", departmentField, 0, 3, 1);
        addField(formPanel, gbc, "Year (1-6)", yearField, 1, 3, 1);

        // Register button
        JButton registerButton = new JButton("Create Student Account");
        registerButton.setFont(new Font("SansSerif", Font.BOLD, 15));
        registerButton.setForeground(Color.WHITE);
        registerButton.setBackground(primaryColor);
        registerButton.setFocusPainted(false);
        registerButton.setBorderPainted(false);
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerButton.setPreferredSize(new Dimension(0, 45));

        registerButton.addActionListener(e -> registerStudent());

        // Back to login
        JButton loginButton = new JButton("Already have an account? Login");
        loginButton.setFont(new Font("SansSerif", Font.PLAIN, 13));
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setContentAreaFilled(false);
        loginButton.setForeground(primaryColor);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        loginButton.addActionListener(e ->
                frame.navigateTo(MainFrame.STUDENT_LOGIN));

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 6));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(18, 6, 0, 6));
        buttonPanel.add(registerButton);
        buttonPanel.add(loginButton);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(30, 35, 30, 35));
        contentPanel.add(headingPanel, BorderLayout.NORTH);
        contentPanel.add(formPanel, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        UI.authScreen(this, contentPanel, false);
    }

    private void styleField(JComponent field) {
        UI.styleField(field);
        field.setPreferredSize(new Dimension(220, 44));
    }

    private void addField(
            JPanel panel,
            GridBagConstraints gbc,
            String labelText,
            JComponent input,
            int column,
            int row,
            int columnSpan) {

        JPanel fieldPanel = new JPanel(new BorderLayout(0, 7));
        fieldPanel.setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(UI.LABEL);

        fieldPanel.add(label, BorderLayout.NORTH);
        fieldPanel.add(input, BorderLayout.CENTER);

        gbc.gridx = column;
        gbc.gridy = row;
        gbc.gridwidth = columnSpan;
        gbc.weightx = columnSpan;

        panel.add(fieldPanel, gbc);

        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
    }

    private void registerStudent() {

        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String phone = phoneField.getText().trim();
        String department = departmentField.getText().trim();
        String yearText = yearField.getText().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()
                || phone.isEmpty() || department.isEmpty()
                || yearText.isEmpty()) {

            JOptionPane.showMessageDialog(frame, "Please fill in all fields!");
            return;
        }

        boolean validEmail = email.matches(
                "^[A-Za-z0-9._%+-]+@gmail\\.com$"
                + "|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.org$");

        if (!validEmail) {
            JOptionPane.showMessageDialog(
                    frame, "Enter a valid Gmail address or .org email!");
            return;
        }

        if (!phone.matches("^[6-9][0-9]{9}$")) {
            JOptionPane.showMessageDialog(
                    frame, "Enter a valid 10-digit Indian mobile number!");
            return;
        }

        if (!password.matches(
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9\\s])\\S{8,}$")) {

            JOptionPane.showMessageDialog(
                    frame,
                    "Password must contain:\n"
                    + "- At least 8 characters\n"
                    + "- One uppercase letter (A-Z)\n"
                    + "- One lowercase letter (a-z)\n"
                    + "- One number (0-9)\n"
                    + "- One special character (@, #, !, etc.)");
            return;
        }

        int year;

        try {
            year = Integer.parseInt(yearText);

            if (year < 1 || year > 6) {
                JOptionPane.showMessageDialog(
                        frame, "Year must be between 1 and 6!");
                return;
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Year must be a number!");
            return;
        }

        String sql = """
                INSERT INTO students
                (name, email, password, phone, department, year)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                JOptionPane.showMessageDialog(
                        frame, "Database connection failed!");
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, PasswordUtil.hash(password));
                ps.setString(4, phone);
                ps.setString(5, department);
                ps.setInt(6, year);

                ps.executeUpdate();

                JOptionPane.showMessageDialog(
                        frame, "Registration successful!\nYou can now log in.");

                clearForm();
                frame.navigateTo(MainFrame.STUDENT_LOGIN);
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            JOptionPane.showMessageDialog(
                    frame, "This email is already registered!");

        } catch (java.sql.SQLException e) {
            // 1062 = duplicate entry (some drivers use a plain SQLException)
            if (e.getErrorCode() == 1062 || "23000".equals(e.getSQLState())
                    || String.valueOf(e.getMessage()).toLowerCase().contains("duplicate")) {
                JOptionPane.showMessageDialog(
                        frame, "This email is already registered!");
            } else {
                JOptionPane.showMessageDialog(
                        frame, "Registration failed!\n" + e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    frame, "Registration failed!\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearForm() {
        nameField.setText("");
        emailField.setText("");
        passwordField.setText("");
        phoneField.setText("");
        departmentField.setText("");
        yearField.setText("");
    }
}
