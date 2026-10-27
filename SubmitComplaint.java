import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class SubmitComplaint extends JPanel {

    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024; // 5 MB

    private final MainFrame frame;
    private final int studentId;

    private JTextField titleField;
    private JComboBox<String> categoryBox;
    private JComboBox<String> priorityBox;
    private JTextArea descriptionArea;
    private JTextField blockField;
    private JTextField roomField;

    private JLabel photoLabel;
    private JButton removePhotoButton;
    private byte[] photoBytes;

    // Smart Assist (AI category + priority)
    private JLabel aiLabel;
    private javax.swing.Timer aiTimer;
    private SmartClassifier.Prediction lastPrediction;
    private boolean categoryTouched;
    private boolean priorityTouched;
    private boolean applyingSuggestion;
    private String photoName;

    private final Color backgroundColor = UI.BACKGROUND;
    private final Color primaryColor = new Color(37, 99, 235);

    public SubmitComplaint(MainFrame frame, int studentId) {

        this.frame = frame;
        this.studentId = studentId;

        setBackground(backgroundColor);

        // Heading
        JLabel heading = new JLabel("Submit a Complaint");
        heading.setFont(new Font("SansSerif", Font.BOLD, 27));
        heading.setForeground(UI.DARK);

        JLabel subtitle = new JLabel(
                "Tell us about the issue in your campus");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(UI.MUTED);

        JPanel headingPanel = new JPanel();
        headingPanel.setLayout(
                new BoxLayout(headingPanel, BoxLayout.Y_AXIS));
        headingPanel.setOpaque(false);
        headingPanel.add(heading);
        headingPanel.add(Box.createVerticalStrut(8));
        headingPanel.add(subtitle);

        // Form fields
        titleField = new JTextField();

        categoryBox = new JComboBox<>(UI.CATEGORIES);

        priorityBox = new JComboBox<>(UI.PRIORITIES);
        priorityBox.setSelectedItem("Medium");
        priorityBox.setToolTipText(
                "Urgent: 1 day, High: 2 days, Medium: 4 days, Low: 7 days");

        descriptionArea = new JTextArea(5, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        descriptionArea.setBorder(new EmptyBorder(8, 10, 8, 10));

        blockField = new JTextField();
        roomField = new JTextField();

        styleField(titleField);
        styleField(categoryBox);
        styleField(priorityBox);

        // ================= SMART ASSIST =================
        // While the student types, the AI suggests category and priority.
        // If the student changes a box by hand, the AI stops changing it.

        aiLabel = new JLabel();
        aiLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        aiLabel.setOpaque(true);
        aiLabel.setBackground(UI.SOFT_PURPLE);
        aiLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, new Color(124, 58, 237)),
                new EmptyBorder(8, 12, 8, 12)));
        resetAiLabel();

        categoryBox.addActionListener(e -> {
            if (!applyingSuggestion) {
                categoryTouched = true;
            }
        });

        priorityBox.addActionListener(e -> {
            if (!applyingSuggestion) {
                priorityTouched = true;
            }
        });

        aiTimer = new javax.swing.Timer(400, e -> runSmartAssist());
        aiTimer.setRepeats(false);

        javax.swing.event.DocumentListener typing = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                aiTimer.restart();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                aiTimer.restart();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                aiTimer.restart();
            }
        };

        titleField.getDocument().addDocumentListener(typing);
        descriptionArea.getDocument().addDocumentListener(typing);
        styleField(blockField);
        styleField(roomField);

        // Photo attachment
        JButton attachButton = UI.secondaryButton("Attach Photo");
        attachButton.addActionListener(e -> choosePhoto());

        removePhotoButton = UI.secondaryButton("Remove");
        removePhotoButton.setVisible(false);
        removePhotoButton.addActionListener(e -> clearPhoto());

        photoLabel = new JLabel("No photo selected (optional, max 5 MB)");
        photoLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        photoLabel.setForeground(UI.MUTED);

        JPanel photoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        photoPanel.setOpaque(false);
        photoPanel.add(attachButton);
        photoPanel.add(removePhotoButton);
        photoPanel.add(photoLabel);

        // Layout for the form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 6, 8, 6);
        gbc.weightx = 1.0;

        addField(formPanel, gbc, "Complaint Title",
                titleField, 0, 0, 2);

        addField(formPanel, gbc, "Category",
                categoryBox, 0, 1, 1);

        addField(formPanel, gbc, "Priority",
                priorityBox, 1, 1, 1);

        addField(formPanel, gbc, "Description (at least 10 characters)",
                descriptionScroll(), 0, 2, 2);

        addField(formPanel, gbc, "Smart Assist (AI)",
                aiLabel, 0, 3, 2);

        addField(formPanel, gbc, "Block",
                blockField, 0, 4, 1);

        addField(formPanel, gbc, "Room Number",
                roomField, 1, 4, 1);

        addField(formPanel, gbc, "Photo of the Problem",
                photoPanel, 0, 5, 2);

        // Submit button
        JButton submitButton = new JButton("Submit Complaint");
        submitButton.setFont(new Font("SansSerif", Font.BOLD, 15));
        submitButton.setForeground(Color.WHITE);
        submitButton.setBackground(primaryColor);
        submitButton.setFocusPainted(false);
        submitButton.setBorderPainted(false);
        submitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitButton.setPreferredSize(new Dimension(0, 45));

        submitButton.addActionListener(e -> submitComplaint());

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(12, 6, 0, 6));
        buttonPanel.add(submitButton, BorderLayout.CENTER);

        JPanel contentPanel = new JPanel(new BorderLayout(0, 16));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(26, 35, 26, 35));
        contentPanel.add(headingPanel, BorderLayout.NORTH);
        contentPanel.add(formPanel, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        UI.cardScreen(this, contentPanel);
    }

    private JScrollPane descriptionScroll() {
        JScrollPane scroll = new JScrollPane(descriptionArea);
        UI.styleScrollField(scroll);
        return scroll;
    }

    private void styleField(JComponent field) {
        UI.styleField(field);
        field.setPreferredSize(new Dimension(250, 44));
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

    // ================= PHOTO =================

    private void choosePhoto() {

        // FileDialog opens the normal Windows "Open" window
        // (with Desktop, Downloads, Pictures etc. on the left)
        FileDialog dialog = new FileDialog(frame,
                "Choose a photo of the problem", FileDialog.LOAD);

        // On Windows this shows only image files
        dialog.setFile("*.jpg;*.jpeg;*.png;*.gif");

        // Same filter for macOS / Linux
        dialog.setFilenameFilter((folder, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                    || lower.endsWith(".png") || lower.endsWith(".gif");
        });

        dialog.setVisible(true);

        if (dialog.getFile() == null) {
            return; // user pressed Cancel
        }

        File file = new File(dialog.getDirectory(), dialog.getFile());

        if (file.length() > MAX_PHOTO_BYTES) {
            JOptionPane.showMessageDialog(frame,
                    "Photo is too large. Please choose an image under 5 MB.");
            return;
        }

        try {
            BufferedImage image = ImageIO.read(file);

            if (image == null) {
                JOptionPane.showMessageDialog(frame,
                        "This file is not a valid image.");
                return;
            }

            photoBytes = Files.readAllBytes(file.toPath());
            photoName = file.getName();

            // Small preview next to the button
            int size = 40;
            double scale = Math.min(size / (double) image.getWidth(),
                    size / (double) image.getHeight());

            Image thumb = image.getScaledInstance(
                    Math.max(1, (int) (image.getWidth() * scale)),
                    Math.max(1, (int) (image.getHeight() * scale)),
                    Image.SCALE_SMOOTH);

            photoLabel.setIcon(new ImageIcon(thumb));
            photoLabel.setText(photoName + "  (" + (file.length() / 1024) + " KB)");
            photoLabel.setForeground(UI.DARK);
            removePhotoButton.setVisible(true);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame,
                    "Could not read the photo!\n" + e.getMessage());
        }
    }

    private void clearPhoto() {
        photoBytes = null;
        photoName = null;
        photoLabel.setIcon(null);
        photoLabel.setText("No photo selected (optional, max 5 MB)");
        photoLabel.setForeground(UI.MUTED);
        removePhotoButton.setVisible(false);
    }

    // ================= SUBMIT =================

    private void submitComplaint() {

        String title = titleField.getText().trim();
        String category =
                (String) categoryBox.getSelectedItem();
        String priority =
                (String) priorityBox.getSelectedItem();
        String description = descriptionArea.getText().trim();
        String block = blockField.getText().trim();
        String room = roomField.getText().trim();

        if (title.isEmpty() || description.isEmpty()
                || block.isEmpty() || room.isEmpty()) {

            JOptionPane.showMessageDialog(
                    frame,
                    "Please fill in all fields!");
            return;
        }

        if (title.length() > 150) {
            JOptionPane.showMessageDialog(frame,
                    "Title cannot be longer than 150 characters!");
            return;
        }

        if (description.length() < 10) {
            JOptionPane.showMessageDialog(frame,
                    "Please describe the problem in at least 10 characters.");
            return;
        }

        if (block.length() > 50 || room.length() > 20) {
            JOptionPane.showMessageDialog(frame,
                    "Block or room number is too long!");
            return;
        }

        String sql = """
                INSERT INTO complaints
                (student_id, category, title, description,
                 room_no, block, status, priority, image, image_name,
                 assigned_to, ai_category, ai_confidence)
                VALUES (?, ?, ?, ?, ?, ?, 'Submitted', ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                JOptionPane.showMessageDialog(
                        frame, "Database connection failed!");
                return;
            }

            // What the AI thinks (saved to measure AI accuracy later)
            SmartClassifier.Prediction ai =
                    SmartClassifier.predict(title + " " + description);

            try (PreparedStatement ps = con.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setInt(1, studentId);
                ps.setString(2, category);
                ps.setString(3, title);
                ps.setString(4, description);
                ps.setString(5, room);
                ps.setString(6, block);
                ps.setString(7, priority);

                if (photoBytes != null) {
                    ps.setBytes(8, photoBytes);
                    ps.setString(9, photoName);
                } else {
                    ps.setNull(8, Types.LONGVARBINARY);
                    ps.setNull(9, Types.VARCHAR);
                }

                // Not assigned yet - the admin chooses the department later
                ps.setNull(10, Types.VARCHAR);
                ps.setString(11, ai.category);
                ps.setInt(12, ai.confidence);

                ps.executeUpdate();

                String complaintId = "";
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        complaintId = " #" + keys.getInt(1);
                    }
                }

                JOptionPane.showMessageDialog(
                        frame,
                        "Complaint" + complaintId + " submitted successfully!\n\n"
                        + "Priority: " + priority + "\n"
                        + "The admin will review it and assign it to the right department.\n"
                        + "Expected resolution time: "
                        + UI.slaDays(priority) + " day(s).\n\n"
                        + "Keep your complaint ID to track it anytime.");

                clearForm();

                // Show the new complaint in the student's list
                frame.navigateTo(MainFrame.MY_COMPLAINTS);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Complaint submission failed!\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearForm() {
        applyingSuggestion = true;
        titleField.setText("");
        categoryBox.setSelectedIndex(0);
        priorityBox.setSelectedItem("Medium");
        descriptionArea.setText("");
        blockField.setText("");
        roomField.setText("");
        applyingSuggestion = false;
        aiTimer.stop();
        categoryTouched = false;
        priorityTouched = false;
        lastPrediction = null;
        resetAiLabel();
        clearPhoto();
    }

    // ================= SMART ASSIST =================

    private void resetAiLabel() {
        aiLabel.setText("<html><body style='width:460px'><b>AI suggestion:</b> start typing the title and "
                + "description - category and priority will be filled automatically."
                + "</html>");
    }

    private void runSmartAssist() {

        String text = (titleField.getText() + " " + descriptionArea.getText()).trim();

        if (text.length() < 4) {
            lastPrediction = null;
            resetAiLabel();
            return;
        }

        SmartClassifier.Prediction p = SmartClassifier.predict(text);
        lastPrediction = p;

        applyingSuggestion = true;
        if (!categoryTouched) {
            categoryBox.setSelectedItem(p.category);
        }
        if (!priorityTouched) {
            priorityBox.setSelectedItem(p.priority);
        }
        applyingSuggestion = false;

        String detected = p.reasons.isEmpty()
                ? "learned from past complaints"
                : "detected: " + String.join(", ", p.reasons);

        aiLabel.setText("<html><body style='width:460px'><b>AI suggestion:</b> " + p.category
                + " (" + p.confidence + "% sure)  &nbsp;|&nbsp;  Priority: <b>"
                + p.priority + "</b>"
                + "<br><font color='#6b7280'>" + detected + "  -  " + p.method
                + (categoryTouched || priorityTouched
                        ? "  -  you changed it manually" : "")
                + "</font></html>");
    }
}
