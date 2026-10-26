import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.Map;
import javax.swing.*;

public class MainFrame extends JFrame {

    // Screen names used across the whole app
    public static final String WELCOME = "welcome";
    public static final String STUDENT_LOGIN = "studentLogin";
    public static final String STUDENT_REGISTER = "register";
    public static final String ADMIN_LOGIN = "adminLogin";
    public static final String ADMIN_REGISTER = "adminRegister";
    public static final String STUDENT_DASHBOARD = "studentDashboard";
    public static final String SUBMIT_COMPLAINT = "submitComplaint";
    public static final String MY_COMPLAINTS = "myComplaints";
    public static final String ADMIN_DASHBOARD = "adminDashboard";
    public static final String ADMIN_ANALYTICS = "adminAnalytics";
    public static final String CHANGE_PASSWORD = "changePassword";
    public static final String TRACK_COMPLAINT = "trackComplaint";
    public static final String PROFILE = "profile";

    // Screens that belong to a logged-in user and are removed on logout
    private static final String[] SESSION_SCREENS = {
        STUDENT_DASHBOARD, SUBMIT_COMPLAINT, MY_COMPLAINTS,
        ADMIN_DASHBOARD, ADMIN_ANALYTICS, CHANGE_PASSWORD, PROFILE
    };

    // Readable names shown in the navigation bar
    private static final Map<String, String> SCREEN_TITLES = Map.ofEntries(
        Map.entry(WELCOME, "Home"),
        Map.entry(STUDENT_LOGIN, "Student Login"),
        Map.entry(STUDENT_REGISTER, "Create Account"),
        Map.entry(ADMIN_LOGIN, "Admin Login"),
        Map.entry(ADMIN_REGISTER, "Admin Registration"),
        Map.entry(STUDENT_DASHBOARD, "Dashboard"),
        Map.entry(SUBMIT_COMPLAINT, "New Complaint"),
        Map.entry(MY_COMPLAINTS, "My Complaints"),
        Map.entry(ADMIN_DASHBOARD, "Admin Dashboard"),
        Map.entry(ADMIN_ANALYTICS, "Analytics & Insights"),
        Map.entry(CHANGE_PASSWORD, "Change Password"),
        Map.entry(TRACK_COMPLAINT, "Track Complaint"),
        Map.entry(PROFILE, "My Profile")
    );

    // ================= COLOURS =================

    private static final Color NAV_LEFT = new Color(11, 17, 34);
    private static final Color NAV_RIGHT = new Color(26, 35, 78);
    private static final Color GLOW_BLUE = new Color(59, 130, 246);
    private static final Color GLOW_PURPLE = new Color(168, 85, 247);
    private static final Color GLOW_PINK = new Color(236, 72, 153);
    private static final Color TEXT = new Color(240, 244, 255);
    private static final Color MUTED = new Color(148, 163, 200);
    private static final Color DISABLED = new Color(75, 88, 120);

    private final CardLayout cardLayout;
    private final JPanel screenContainer;

    private final NavButton backButton;
    private final NavButton forwardButton;

    private final JLabel screenTitle = new JLabel();
    private final JLabel clockLabel = new JLabel();
    private final UserChip userChip = new UserChip();

    private final Deque<String> backHistory = new ArrayDeque<>();
    private final Deque<String> forwardHistory = new ArrayDeque<>();

    private String currentScreen;

    // Fade animation between screens
    private final FadePane fadePane = new FadePane();

    public MainFrame() {

        setTitle("Campus Care - AI-Powered Complaint Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setSize(1280, 800);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setIconImage(createAppIcon(64));

        cardLayout = new CardLayout();
        screenContainer = new JPanel(cardLayout);
        screenContainer.setBackground(UI.BACKGROUND);

        backButton = new NavButton(true, "Back (Alt + Left)");
        forwardButton = new NavButton(false, "Forward (Alt + Right)");

        backButton.addActionListener(e -> goBack());
        forwardButton.addActionListener(e -> goForward());

        // Main layout
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(UI.BACKGROUND);
        rootPanel.add(createNavigationBar(), BorderLayout.NORTH);
        rootPanel.add(screenContainer, BorderLayout.CENTER);

        setContentPane(rootPanel);
        setGlassPane(fadePane);

        registerKeyboardShortcuts(rootPanel);
        updateArrowButtons();
        startClock();
    }

    // ================= NAVIGATION BAR =================

    private JPanel createNavigationBar() {

        JPanel bar = new JPanel(new BorderLayout(20, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                int w = getWidth();
                int h = getHeight();

                // Dark navy gradient
                g2.setPaint(new GradientPaint(0, 0, NAV_LEFT, w, 0, NAV_RIGHT));
                g2.fillRect(0, 0, w, h);

                // Soft glow behind the logo
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new RadialGradientPaint(
                        new Point(150, h / 2), 160f,
                        new float[]{0f, 1f},
                        new Color[]{new Color(59, 130, 246, 55),
                                    new Color(59, 130, 246, 0)}));
                g2.fillRect(0, 0, 360, h);

                // Colourful line at the bottom
                g2.setPaint(new LinearGradientPaint(0, 0, w, 0,
                        new float[]{0f, 0.5f, 1f},
                        new Color[]{GLOW_BLUE, GLOW_PURPLE, GLOW_PINK}));
                g2.fillRect(0, h - 3, w, 3);

                g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(0, 70));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 16, 3, 20));

        // Left: arrows + logo + name
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 15));
        left.setOpaque(false);
        left.add(backButton);
        left.add(forwardButton);
        left.add(Box.createHorizontalStrut(10));

        JLabel logo = new JLabel(new ImageIcon(createAppIcon(40)));
        left.add(logo);

        JPanel brand = new JPanel();
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setOpaque(false);

        JLabel name = new JLabel("Campus Care");
        name.setFont(new Font("SansSerif", Font.BOLD, 19));
        name.setForeground(TEXT);

        JLabel tagline = new JLabel("AI-POWERED COMPLAINT SYSTEM");
        tagline.setFont(new Font("SansSerif", Font.BOLD, 9));
        tagline.setForeground(new Color(129, 160, 255));

        brand.add(name);
        brand.add(Box.createVerticalStrut(1));
        brand.add(tagline);
        left.add(brand);

        // Centre: current page as a glass pill
        screenTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        screenTitle.setForeground(TEXT);
        screenTitle.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel pill = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        getHeight(), getHeight());
                g2.setColor(new Color(255, 255, 255, 40));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        getHeight(), getHeight());

                // Small glowing dot
                g2.setColor(new Color(52, 211, 153));
                g2.fillOval(14, getHeight() / 2 - 4, 8, 8);
                g2.setColor(new Color(52, 211, 153, 60));
                g2.fillOval(11, getHeight() / 2 - 7, 14, 14);
                g2.dispose();
            }
        };
        pill.setOpaque(false);
        pill.setBorder(BorderFactory.createEmptyBorder(0, 32, 0, 18));
        pill.add(screenTitle, BorderLayout.CENTER);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        pill.setPreferredSize(new Dimension(230, 34));
        center.add(pill);

        // Right: clock + user
        clockLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        clockLabel.setForeground(MUTED);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 13));
        right.setOpaque(false);
        right.add(clockLabel);
        right.add(userChip);

        bar.add(left, BorderLayout.WEST);
        bar.add(center, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    private void startClock() {

        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM  •  hh:mm a");
        Runnable tick = () -> clockLabel.setText(format.format(new Date()));
        tick.run();

        Timer timer = new Timer(15000, e -> tick.run());
        timer.start();
    }

    // The app logo: a gradient tile with a white shield and tick
    public static BufferedImage createAppIcon(int size) {

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);

        float s = size;

        g2.setPaint(new GradientPaint(0, 0, GLOW_BLUE, s, s, GLOW_PURPLE));
        g2.fill(new RoundRectangle2D.Float(0, 0, s, s, s * 0.32f, s * 0.32f));

        // Shine on the top half
        g2.setColor(new Color(255, 255, 255, 35));
        g2.fill(new RoundRectangle2D.Float(0, 0, s, s * 0.5f, s * 0.32f, s * 0.32f));

        // Shield
        Path2D shield = new Path2D.Float();
        shield.moveTo(s * 0.50, s * 0.17);
        shield.lineTo(s * 0.78, s * 0.28);
        shield.curveTo(s * 0.78, s * 0.58, s * 0.66, s * 0.74, s * 0.50, s * 0.84);
        shield.curveTo(s * 0.34, s * 0.74, s * 0.22, s * 0.58, s * 0.22, s * 0.28);
        shield.closePath();

        g2.setColor(Color.WHITE);
        g2.fill(shield);

        // Tick inside the shield
        g2.setPaint(new GradientPaint(0, 0, GLOW_BLUE, s, s, GLOW_PURPLE));
        g2.setStroke(new BasicStroke(s * 0.075f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D tick = new Path2D.Float();
        tick.moveTo(s * 0.37, s * 0.50);
        tick.lineTo(s * 0.47, s * 0.60);
        tick.lineTo(s * 0.64, s * 0.40);
        g2.draw(tick);

        g2.dispose();
        return image;
    }

    // Round back / forward button with a drawn arrow
    private static class NavButton extends JButton {

        private final boolean back;
        private boolean hover;
        private boolean active;

        NavButton(boolean back, String tooltip) {
            this.back = back;
            setToolTipText(tooltip);
            setPreferredSize(new Dimension(38, 38));
            setBorderPainted(false);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        void setActive(boolean active) {
            this.active = active;
            setCursor(active ? new Cursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Circle background
            if (active && hover) {
                g2.setPaint(new GradientPaint(0, 0, GLOW_BLUE, w, h, GLOW_PURPLE));
            } else {
                g2.setColor(new Color(255, 255, 255, active ? 22 : 8));
            }
            g2.fill(new Ellipse2D.Float(1, 1, w - 2, h - 2));

            g2.setColor(new Color(255, 255, 255, active ? 45 : 18));
            g2.draw(new Ellipse2D.Float(1, 1, w - 3, h - 3));

            // Arrow
            g2.setColor(active ? Color.WHITE : DISABLED);
            g2.setStroke(new BasicStroke(2.2f,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int cx = w / 2;
            int cy = h / 2;
            int dir = back ? -1 : 1;

            g2.drawLine(cx - 6 * dir, cy, cx + 6 * dir, cy);
            g2.drawLine(cx + 6 * dir, cy, cx + 1 * dir, cy - 5);
            g2.drawLine(cx + 6 * dir, cy, cx + 1 * dir, cy + 5);

            g2.dispose();
        }
    }

    // Avatar + name + role on the right of the bar
    private class UserChip extends JComponent {

        private String name;
        private String role;
        private boolean admin;
        private boolean hover;

        UserChip() {
            setGuest();
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (name != null && hasScreen(PROFILE)) {
                        navigateTo(PROFILE);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        void setGuest() {
            name = null;
            role = null;
            setToolTipText(null);
            setCursor(Cursor.getDefaultCursor());
            updateSize();
        }

        void setUser(String name, boolean admin) {
            this.name = name;
            this.admin = admin;
            this.role = admin ? "Administrator" : "Student";
            setToolTipText("Open my profile");
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            updateSize();
        }

        private void updateSize() {
            FontMetrics fm = getFontMetrics(new Font("SansSerif", Font.BOLD, 13));
            String text = name == null ? "Guest" : name;
            int width = 36 + 10 + Math.max(fm.stringWidth(text), 80) + 18;
            setPreferredSize(new Dimension(Math.min(width, 260), 42));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(new Color(255, 255, 255, hover && name != null ? 30 : 14));
            g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
            g2.setColor(new Color(255, 255, 255, 35));
            g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);

            // Avatar circle with initials
            int size = h - 8;
            if (name == null) {
                g2.setColor(new Color(255, 255, 255, 40));
            } else if (admin) {
                g2.setPaint(new GradientPaint(4, 4, new Color(16, 185, 129),
                        4 + size, 4 + size, new Color(14, 165, 233)));
            } else {
                g2.setPaint(new GradientPaint(4, 4, GLOW_BLUE,
                        4 + size, 4 + size, GLOW_PINK));
            }
            g2.fillOval(4, 4, size, size);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            String initials = initials(name);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(initials, 4 + (size - fm.stringWidth(initials)) / 2,
                    4 + (size - fm.getHeight()) / 2 + fm.getAscent());

            // Online dot
            if (name != null) {
                g2.setColor(NAV_RIGHT);
                g2.fillOval(4 + size - 10, 4 + size - 10, 12, 12);
                g2.setColor(new Color(52, 211, 153));
                g2.fillOval(4 + size - 8, 4 + size - 8, 8, 8);
            }

            // Name and role
            int textX = 4 + size + 10;
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.setColor(TEXT);
            String title = name == null ? "Guest" : name;
            FontMetrics bold = g2.getFontMetrics();
            int maxWidth = w - textX - 14;
            while (bold.stringWidth(title) > maxWidth && title.length() > 3) {
                title = title.substring(0, title.length() - 2) + "…";
                title = title.replace("……", "…");
            }
            g2.drawString(title, textX, h / 2 - 2);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.setColor(MUTED);
            g2.drawString(role == null ? "Not signed in" : role, textX, h / 2 + 12);

            g2.dispose();
        }

        private String initials(String text) {
            if (text == null || text.isBlank()) {
                return "?";
            }
            String[] parts = text.trim().split("\\s+");
            String result = parts[0].substring(0, 1);
            if (parts.length > 1) {
                result += parts[parts.length - 1].substring(0, 1);
            }
            return result.toUpperCase();
        }
    }

    // ================= FADE BETWEEN SCREENS =================

    private class FadePane extends JComponent {

        private BufferedImage snapshot;
        private float alpha;
        private Timer timer;

        void start(BufferedImage image) {
            if (image == null) {
                return;
            }
            if (timer != null) {
                timer.stop();
            }
            snapshot = image;
            alpha = 1f;
            setVisible(true);

            timer = new Timer(15, e -> {
                alpha -= 0.12f;
                if (alpha <= 0f) {
                    ((Timer) e.getSource()).stop();
                    snapshot = null;
                    setVisible(false);
                }
                repaint();
            });
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (snapshot == null) {
                return;
            }
            Point p = SwingUtilities.convertPoint(
                    screenContainer, 0, 0, this);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, Math.max(0f, alpha)));
            g2.drawImage(snapshot, p.x, p.y, null);
            g2.dispose();
        }
    }

    private BufferedImage snapshotOfCurrentScreen() {

        int w = screenContainer.getWidth();
        int h = screenContainer.getHeight();

        if (!isShowing() || w <= 0 || h <= 0) {
            return null;
        }

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        screenContainer.paint(g2);
        g2.dispose();
        return image;
    }

    // ================= KEYBOARD =================

    // Alt + Left / Alt + Right keyboard navigation
    private void registerKeyboardShortcuts(JComponent root) {

        InputMap inputMap = root.getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        ActionMap actionMap = root.getActionMap();

        inputMap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK),
                "goBack"
        );

        inputMap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK),
                "goForward"
        );

        actionMap.put("goBack", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                goBack();
            }
        });

        actionMap.put("goForward", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                goForward();
            }
        });
    }

    // ================= SCREENS =================

    // Register a screen and its unique name
    public void addScreen(String screenName, JPanel screen) {

        if (screenName == null || screenName.trim().isEmpty()) {
            throw new IllegalArgumentException("Screen name cannot be empty.");
        }

        if (screen == null) {
            throw new IllegalArgumentException("Screen panel cannot be null.");
        }

        if (hasScreen(screenName)) {
            throw new IllegalArgumentException(
                    "Screen already registered: " + screenName);
        }

        screen.setName(screenName);
        screenContainer.add(screen, screenName);
    }

    // Add a screen, or replace it if one with the same name exists
    public void setScreen(String screenName, JPanel screen) {

        removeScreen(screenName);
        addScreen(screenName, screen);
        screenContainer.revalidate();
    }

    // Remove a screen if it is registered
    private void removeScreen(String screenName) {

        for (Component component : screenContainer.getComponents()) {
            if (screenName.equals(component.getName())) {
                screenContainer.remove(component);
                return;
            }
        }
    }

    // Check whether a screen has been registered
    private boolean hasScreen(String screenName) {

        for (Component component : screenContainer.getComponents()) {
            if (screenName.equals(component.getName())) {
                return true;
            }
        }

        return false;
    }

    // Navigate to a registered screen
    public void navigateTo(String screenName) {

        if (screenName == null || screenName.equals(currentScreen)) {
            return;
        }

        if (!hasScreen(screenName)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Screen not found: " + screenName,
                    "Navigation Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (currentScreen != null) {
            backHistory.push(currentScreen);
            forwardHistory.clear();
        }

        showScreen(screenName);
    }

    // Go to the previous screen
    public void goBack() {

        if (backHistory.isEmpty()) {
            return;
        }

        forwardHistory.push(currentScreen);
        showScreen(backHistory.pop());
    }

    // Go forward to the next screen in history
    public void goForward() {

        if (forwardHistory.isEmpty()) {
            return;
        }

        backHistory.push(currentScreen);
        showScreen(forwardHistory.pop());
    }

    // Display a registered screen instantly (no fade, so it feels fast)
    private void showScreen(String screenName) {

        currentScreen = screenName;
        cardLayout.show(screenContainer, screenName);
        screenTitle.setText(SCREEN_TITLES.getOrDefault(screenName, screenName));
        updateArrowButtons();
    }

    // Show a screen and forget all history (used after login/logout)
    public void resetTo(String screenName) {

        backHistory.clear();
        forwardHistory.clear();
        showScreen(screenName);
    }

    // ================= LOGIN / LOGOUT FLOW =================

    // Called by Login after a student signs in.
    // previousLogin is used to show "new updates since your last visit".
    public void openStudentArea(
            int studentId, String studentName, java.sql.Timestamp previousLogin) {

        StudentDashboard dashboard =
                new StudentDashboard(this, studentId, studentName, previousLogin);

        setScreen(STUDENT_DASHBOARD, dashboard);
        setScreen(SUBMIT_COMPLAINT,
                new SubmitComplaint(this, studentId));
        setScreen(MY_COMPLAINTS,
                new MyComplaints(this, studentId));
        setScreen(CHANGE_PASSWORD,
                new ChangePassword(this, false, studentId));
        setScreen(PROFILE,
                new Profile(this, false, studentId, newName -> {
                    dashboard.setDisplayName(newName);
                    userChip.setUser(newName, false);
                }));

        userChip.setUser(studentName, false);

        // History is cleared so Back cannot return to the login form
        resetTo(STUDENT_DASHBOARD);
    }

    // Called by AdminLogin after an admin signs in
    public void openAdminArea(int adminId, String adminName) {

        AdminDashboard dashboard = new AdminDashboard(this, adminId, adminName);

        setScreen(ADMIN_DASHBOARD, dashboard);
        setScreen(ADMIN_ANALYTICS,
                new AdminAnalytics(this));
        setScreen(CHANGE_PASSWORD,
                new ChangePassword(this, true, adminId));
        setScreen(PROFILE,
                new Profile(this, true, adminId, newName -> {
                    dashboard.setDisplayName(newName);
                    userChip.setUser(newName, true);
                }));

        userChip.setUser(adminName, true);

        resetTo(ADMIN_DASHBOARD);
    }

    // Logout from any dashboard
    public void logout() {

        resetTo(WELCOME);

        // Remove the previous user's screens and data
        for (String name : SESSION_SCREENS) {
            removeScreen(name);
        }

        userChip.setGuest();

        screenContainer.revalidate();
        screenContainer.repaint();
    }

    // ================= HELPERS =================

    private void updateArrowButtons() {
        backButton.setActive(!backHistory.isEmpty());
        forwardButton.setActive(!forwardHistory.isEmpty());
    }

    // Clear navigation history
    public void clearHistory() {

        backHistory.clear();
        forwardHistory.clear();
        updateArrowButtons();
    }

    // Start the application on its first screen
    public void start(String firstScreenName) {

        if (!hasScreen(firstScreenName)) {
            throw new IllegalArgumentException(
                    "Initial screen not registered: " + firstScreenName
            );
        }

        backHistory.clear();
        forwardHistory.clear();
        currentScreen = null;

        navigateTo(firstScreenName);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    public String getCurrentScreen() {
        return currentScreen;
    }

    // Puts a small form (login, register, etc.) in the middle of a
    // screen, with a scrollbar when the window is too small.
    public static void placeCentered(JPanel screen, JComponent content) {

        CenterPanel wrapper = new CenterPanel();
        wrapper.add(content);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        screen.setLayout(new BorderLayout());
        screen.add(scroll, BorderLayout.CENTER);
    }

    private static class CenterPanel extends JPanel implements Scrollable {

        CenterPanel() {
            super(new GridBagLayout());
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(
                Rectangle r, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(
                Rectangle r, int orientation, int direction) {
            return 100;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return getParent() instanceof JViewport viewport
                    && viewport.getWidth() > getPreferredSize().width;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport viewport
                    && viewport.getHeight() > getPreferredSize().height;
        }
    }
}
