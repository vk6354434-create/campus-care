import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// Change password screen, shared by students and admins
public class ChangePassword extends JPanel {

    private final MainFrame frame;
    private final boolean admin;
    private final int userId;

    private final JPasswordField currentField = new JPasswordField();
    private final JPasswordField newField = new JPasswordField();
    private final JPasswordField confirmField = new JPasswordField();

    public ChangePassword(MainFrame frame, boolean admin, int userId) {

        this.frame = frame;
        this.admin = admin;
        this.userId = userId;

        setBackground(UI.BACKGROUND);

        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setOpaque(false);
        heading.add(UI.heading("Change Password", 26));
        heading.add(Box.createVerticalStrut(8));
        heading.add(UI.muted("Keep your account safe with a strong password"));

        for (JPasswordField field : new JPasswordField[]{
                currentField, newField, confirmField}) {
            UI.styleField(field);
            field.setPreferredSize(new Dimension(360, 46));
        }

        JCheckBox show = new JCheckBox("Show passwords");
        show.setOpaque(false);
        show.addActionListener(e -> {
            char echo = show.isSelected() ? (char) 0 : '•';
            currentField.setEchoChar(echo);
            newField.setEchoChar(echo);
            confirmField.setEchoChar(echo);
        });

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 8));
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(22, 0, 8, 0));
        form.add(UI.fieldLabel("Current Password"));
        form.add(currentField);
        form.add(UI.fieldLabel("New Password"));
        form.add(newField);
        form.add(UI.fieldLabel("Confirm New Password"));
        form.add(confirmField);
        form.add(show);

        JButton save = UI.primaryButton("Update Password");
        save.setPreferredSize(new Dimension(0, 45));
        save.addActionListener(e -> changePassword());
        confirmField.addActionListener(e -> changePassword());

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(30, 40, 30, 40));
        content.add(heading, BorderLayout.NORTH);
        content.add(form, BorderLayout.CENTER);
        content.add(save, BorderLayout.SOUTH);

        UI.cardScreen(this, content);
    }

    private void changePassword() {

        String current = new String(currentField.getPassword());
        String newPassword = new String(newField.getPassword());
        String confirm = new String(confirmField.getPassword());

        if (current.isEmpty() || newPassword.isEmpty() || confirm.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please fill in all fields!");
            return;
        }

        if (!newPassword.equals(confirm)) {
            JOptionPane.showMessageDialog(frame, "New passwords do not match!");
            return;
        }

        if (newPassword.equals(current)) {
            JOptionPane.showMessageDialog(frame,
                    "New password must be different from the current one!");
            return;
        }

        if (!PasswordUtil.isStrong(newPassword)) {
            JOptionPane.showMessageDialog(frame, PasswordUtil.RULES);
            return;
        }

        String table = admin ? "admins" : "students";
        String idColumn = admin ? "admin_id" : "student_id";

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                JOptionPane.showMessageDialog(frame, "Database connection failed!");
                return;
            }

            String stored;

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT password FROM " + table + " WHERE " + idColumn + " = ?")) {

                ps.setInt(1, userId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        JOptionPane.showMessageDialog(frame, "Account not found!");
                        return;
                    }
                    stored = rs.getString("password");
                }
            }

            if (!PasswordUtil.verify(current, stored)) {
                JOptionPane.showMessageDialog(frame, "Current password is incorrect!");
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE " + table + " SET password = ? WHERE " + idColumn + " = ?")) {

                ps.setString(1, PasswordUtil.hash(newPassword));
                ps.setInt(2, userId);
                ps.executeUpdate();
            }

            currentField.setText("");
            newField.setText("");
            confirmField.setText("");

            JOptionPane.showMessageDialog(frame, "Password changed successfully!");
            frame.goBack();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame,
                    "Could not change password!\n" + e.getMessage());
            e.printStackTrace();
        }
    }
}
