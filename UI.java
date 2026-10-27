import java.awt.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.function.Function;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

// Shared colours, constants and small UI helpers used by every screen
public final class UI {

    private UI() {
    }

    // ================= THEME MODES =================

    // DEFAULT = dark top bar + light pages (the original look)
    // LIGHT   = everything light
    // DARK    = everything dark
    public enum Mode {
        DEFAULT("Default"), LIGHT("Light"), DARK("Dark");

        public final String label;

        Mode(String label) {
            this.label = label;
        }
    }

    private static Mode mode = Mode.DEFAULT;

    public static Mode mode() {
        return mode;
    }

    // true when the pages (cards, tables, fields) are dark
    public static boolean isDark() {
        return mode == Mode.DARK;
    }

    // true when the top bar and the home screen are dark
    public static boolean chromeDark() {
        return mode != Mode.LIGHT;
    }

    // ================= COLOURS =================
    // These change when the theme changes (see setMode).
    // Every screen reads them when it is built, so after a theme
    // change MainFrame simply builds the screens again.

    public static Color BACKGROUND;
    public static Color CARD;
    public static Color CARD_ALT;       // zebra rows, read-only fields, hover
    public static Color BORDER;
    public static Color LINE;           // card outline, table border
    public static Color GRID;           // table grid lines
    public static Color DARK;           // main text
    public static Color MUTED;          // secondary text
    public static Color LABEL;          // form labels
    public static Color FIELD_BG;
    public static Color FIELD_BORDER;
    public static Color DISABLED_BG;
    public static Color TABLE_HEAD_BG;
    public static Color TABLE_HEAD_TEXT;
    public static Color SELECTED_ROW;
    public static Color SELECTION;      // selected text in fields
    public static Color SOFT_PURPLE;    // AI suggestion box
    public static Color SCROLL_THUMB;
    public static Color SCROLL_THUMB_HOVER;
    public static Color STAR = new Color(245, 158, 11);

    // Top bar and home screen colours
    public static Color NAV_LEFT;
    public static Color NAV_RIGHT;
    public static Color NAV_TEXT;
    public static Color NAV_MUTED;
    public static Color NAV_ACCENT;
    public static Color NAV_DISABLED;

    public static final Color PRIMARY = new Color(37, 99, 235);

    public static Color SUBMITTED;
    public static Color IN_PROGRESS;
    public static Color RESOLVED;
    public static Color REJECTED;

    public static Color LOW;
    public static Color MEDIUM;
    public static Color HIGH;
    public static Color URGENT;

    static {
        setMode(Mode.DEFAULT);
    }

    public static void setMode(Mode newMode) {

        mode = newMode == null ? Mode.DEFAULT : newMode;

        if (isDark()) {
            BACKGROUND = new Color(13, 17, 28);
            CARD = new Color(26, 33, 50);
            CARD_ALT = new Color(32, 41, 61);
            BORDER = new Color(51, 63, 85);
            LINE = new Color(45, 56, 78);
            GRID = new Color(38, 48, 68);
            DARK = new Color(236, 241, 250);
            MUTED = new Color(150, 163, 188);
            LABEL = new Color(203, 213, 230);
            FIELD_BG = new Color(18, 24, 38);
            FIELD_BORDER = new Color(64, 78, 104);
            DISABLED_BG = new Color(30, 38, 56);
            TABLE_HEAD_BG = new Color(32, 41, 61);
            TABLE_HEAD_TEXT = new Color(150, 163, 188);
            SELECTED_ROW = new Color(35, 58, 120);
            SELECTION = new Color(37, 78, 160);
            SOFT_PURPLE = new Color(44, 36, 82);
            SCROLL_THUMB = new Color(64, 78, 104);
            SCROLL_THUMB_HOVER = new Color(100, 116, 145);

            SUBMITTED = new Color(251, 176, 59);
            IN_PROGRESS = new Color(167, 139, 250);
            RESOLVED = new Color(52, 211, 153);
            REJECTED = new Color(248, 113, 113);
            LOW = new Color(148, 163, 184);
            MEDIUM = new Color(96, 165, 250);
            HIGH = new Color(251, 146, 60);
            URGENT = new Color(248, 113, 113);
        } else {
            BACKGROUND = new Color(245, 247, 251);
            CARD = Color.WHITE;
            CARD_ALT = new Color(248, 250, 252);
            BORDER = new Color(229, 231, 235);
            LINE = new Color(226, 232, 240);
            GRID = new Color(241, 245, 249);
            DARK = new Color(31, 41, 55);
            MUTED = new Color(107, 114, 128);
            LABEL = new Color(55, 65, 81);
            FIELD_BG = Color.WHITE;
            FIELD_BORDER = new Color(209, 213, 219);
            DISABLED_BG = new Color(243, 244, 246);
            TABLE_HEAD_BG = new Color(248, 250, 252);
            TABLE_HEAD_TEXT = new Color(100, 116, 139);
            SELECTED_ROW = new Color(224, 236, 255);
            SELECTION = new Color(191, 219, 254);
            SOFT_PURPLE = new Color(237, 233, 254);
            SCROLL_THUMB = new Color(203, 213, 225);
            SCROLL_THUMB_HOVER = new Color(148, 163, 184);

            SUBMITTED = new Color(217, 119, 6);
            IN_PROGRESS = new Color(124, 58, 237);
            RESOLVED = new Color(5, 150, 105);
            REJECTED = new Color(220, 38, 38);
            LOW = new Color(100, 116, 139);
            MEDIUM = new Color(37, 99, 235);
            HIGH = new Color(234, 88, 12);
            URGENT = new Color(220, 38, 38);
        }

        if (chromeDark()) {
            NAV_LEFT = new Color(11, 17, 34);
            NAV_RIGHT = new Color(26, 35, 78);
            NAV_TEXT = new Color(240, 244, 255);
            NAV_MUTED = new Color(148, 163, 200);
            NAV_ACCENT = new Color(129, 160, 255);
            NAV_DISABLED = new Color(75, 88, 120);
        } else {
            NAV_LEFT = Color.WHITE;
            NAV_RIGHT = new Color(238, 242, 255);
            NAV_TEXT = new Color(17, 24, 39);
            NAV_MUTED = new Color(100, 116, 139);
            NAV_ACCENT = new Color(79, 70, 229);
            NAV_DISABLED = new Color(203, 213, 225);
        }
    }

    // See-through overlay for the top bar / home screen:
    // white on dark backgrounds, navy on light backgrounds
    public static Color glass(int alpha) {
        return chromeDark()
                ? new Color(255, 255, 255, alpha)
                : new Color(30, 41, 90, Math.min(255, alpha * 2 / 3 + 2));
    }

    // ================= APP CONSTANTS =================

    public static final String[] STATUSES = {
        "Submitted", "In Progress", "Resolved", "Rejected"
    };

    public static final String[] PRIORITIES = {
        "Low", "Medium", "High", "Urgent"
    };

    public static final String[] CATEGORIES = {
        "Electrical", "Water", "Cleaning", "Hostel",
        "Internet", "Furniture", "Mess", "Other"
    };

    public static final String OVERDUE = "Overdue";
    public static final String ON_TRACK = "On Track";
    public static final String CLOSED = "Closed";

    private static final SimpleDateFormat DISPLAY_DATE =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a");

    private static final SimpleDateFormat SORTABLE_DATE =
            new SimpleDateFormat("yyyy-MM-dd HH:mm");

    // ================= STATUS / PRIORITY / SLA =================

    public static Color statusColor(String status) {

        if (status == null) {
            return MUTED;
        }

        return switch (status) {
            case "Submitted" -> SUBMITTED;
            case "In Progress" -> IN_PROGRESS;
            case "Resolved" -> RESOLVED;
            case "Rejected" -> REJECTED;
            default -> MUTED;
        };
    }

    public static Color priorityColor(String priority) {

        if (priority == null) {
            return MUTED;
        }

        return switch (priority) {
            case "Low" -> LOW;
            case "Medium" -> MEDIUM;
            case "High" -> HIGH;
            case "Urgent" -> URGENT;
            default -> MUTED;
        };
    }

    public static Color slaColor(String sla) {

        if (OVERDUE.equals(sla)) {
            return REJECTED;
        }

        if (ON_TRACK.equals(sla)) {
            return RESOLVED;
        }

        return MUTED;
    }

    // How many days a complaint may stay open, based on priority
    public static int slaDays(String priority) {

        if (priority == null) {
            return 4;
        }

        return switch (priority) {
            case "Urgent" -> 1;
            case "High" -> 2;
            case "Low" -> 7;
            default -> 4;
        };
    }

    public static boolean isOpen(String status) {
        return "Submitted".equals(status) || "In Progress".equals(status);
    }

    // Overdue / On Track / Closed for a complaint
    public static String slaStatus(
            String status, String priority, Timestamp createdAt) {

        if (!isOpen(status)) {
            return CLOSED;
        }

        if (createdAt == null) {
            return ON_TRACK;
        }

        long limit = slaDays(priority) * 24L * 60 * 60 * 1000;
        long age = System.currentTimeMillis() - createdAt.getTime();

        return age > limit ? OVERDUE : ON_TRACK;
    }

    // ================= DATES =================

    public static String formatDate(Timestamp time) {
        return time == null ? "-" : DISPLAY_DATE.format(time);
    }

    // Used in tables so that sorting by date works correctly
    public static String sortableDate(Timestamp time) {
        return time == null ? "" : SORTABLE_DATE.format(time);
    }

    public static String timeAgo(Timestamp time) {

        if (time == null) {
            return "";
        }

        long minutes =
                (System.currentTimeMillis() - time.getTime()) / 60000;

        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + " min ago";
        }

        long hours = minutes / 60;

        if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        }

        long days = hours / 24;

        if (days < 30) {
            return days + (days == 1 ? " day ago" : " days ago");
        }

        return formatDate(time);
    }

    public static String stars(int rating) {

        if (rating <= 0) {
            return "-";
        }

        return "★".repeat(rating) + "☆".repeat(5 - rating);
    }

    // ================= LABELS AND BUTTONS =================

    public static JLabel heading(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, size));
        label.setForeground(DARK);
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        label.setForeground(MUTED);
        return label;
    }

    public static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(LABEL);
        return label;
    }

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(PRIMARY);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 20, 12, 20));
        return button;
    }

    public static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(DARK);
        // White background = "outlined" style (Theme paints it in theme colours)
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(9, 16, 11, 16));
        return button;
    }

    public static JButton dangerButton(String text) {
        JButton button = primaryButton(text);
        button.setBackground(REJECTED);
        return button;
    }

    // Purple-to-pink "special" button (AI, analytics, reports)
    public static JButton accentButton(String text) {
        JButton button = primaryButton(text);
        button.setBackground(new Color(124, 58, 237));
        return button;
    }

    // Rounded field with a blue glow when focused
    public static void styleField(JComponent field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setBorder(Theme.fieldBorder());
        field.setBackground(FIELD_BG);
        field.setForeground(DARK);
        if (field instanceof javax.swing.text.JTextComponent text) {
            text.setCaretColor(Theme.FOCUS);
            text.setSelectionColor(SELECTION);
            text.setSelectedTextColor(DARK);
        }
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                field.repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                field.repaint();
            }
        });
    }

    // Rounded frame around a scroll pane (e.g. description text area)
    public static void styleScrollField(JScrollPane scroll) {
        scroll.setBorder(new Theme.RoundedLineBorder(FIELD_BORDER, 12));
        scroll.getViewport().setBackground(FIELD_BG);
        if (scroll.getViewport().getView() instanceof javax.swing.text.JTextComponent text) {
            text.setBackground(FIELD_BG);
            text.setForeground(DARK);
            text.setCaretColor(Theme.FOCUS);
            text.setSelectionColor(SELECTION);
            text.setSelectedTextColor(DARK);
        }
    }

    // ================= CARDS =================

    // Rounded white card: coloured icon dot, title, big number
    public static JPanel statCard(String title, JLabel value, Color accent) {

        Theme.Card card = new Theme.Card(new BorderLayout(0, 6), 10, 16);
        card.setAccent(accent);

        JLabel titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        titleLabel.setForeground(MUTED);

        // Small glowing dot in the card colour
        JComponent dot = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(),
                        accent.getBlue(), 40));
                g2.fillOval(0, 0, 22, 22);
                g2.setColor(accent);
                g2.fillOval(7, 7, 8, 8);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(22, 22));

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        top.add(titleLabel, BorderLayout.CENTER);
        top.add(dot, BorderLayout.EAST);

        value.setFont(new Font("SansSerif", Font.BOLD, 28));
        value.setForeground(DARK);

        card.add(top, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);

        return card;
    }

    // Rounded white card with a soft shadow, used to group content
    public static JPanel whiteBox(LayoutManager layout) {
        return new Theme.Card(layout, 12);
    }

    // ================= TABLES =================


    public static void styleTable(JTable table) {

        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setForeground(DARK);
        table.setRowHeight(42);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(GRID);
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(SELECTED_ROW);
        table.setSelectionForeground(DARK);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setBackground(CARD);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 42));
        header.setDefaultRenderer(new javax.swing.table.TableCellRenderer() {
            private final JLabel label = new JLabel();

            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean selected,
                    boolean focus, int row, int column) {
                label.setText(value == null ? "" : value.toString().toUpperCase());
                label.setFont(new Font("SansSerif", Font.BOLD, 11));
                label.setForeground(TABLE_HEAD_TEXT);
                label.setOpaque(true);
                label.setBackground(TABLE_HEAD_BG);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBorder(BorderFactory.createMatteBorder(
                        0, 0, 2, 0, LINE));
                return label;
            }
        });
    }

    public static JScrollPane tableScroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(CARD);
        scroll.setBorder(new Theme.RoundedLineBorder(LINE, 14));
        scroll.setOpaque(false);
        return scroll;
    }

    // Renderer with zebra stripes (alternate rows lightly shaded)
    private static class ZebraRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(
                    table, value, isSelected, false, row, column);

            if (!isSelected) {
                setBackground(row % 2 == 0 ? CARD : CARD_ALT);
                setForeground(table.getForeground());
            }
            return this;
        }
    }

    public static DefaultTableCellRenderer centered() {
        DefaultTableCellRenderer renderer = new ZebraRenderer();
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        return renderer;
    }

    public static DefaultTableCellRenderer leftPadded() {
        DefaultTableCellRenderer renderer = new ZebraRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                setFont(new Font("SansSerif", Font.BOLD, 13));
                return this;
            }
        };
        return renderer;
    }

    // Gold stars for the rating column
    public static DefaultTableCellRenderer ratingStars() {
        DefaultTableCellRenderer renderer = new ZebraRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                setForeground(STAR);
                setFont(new Font("Dialog", Font.PLAIN, 14));
                return this;
            }
        };
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        return renderer;
    }

    public static DefaultTableCellRenderer statusBadge() {
        return new BadgeRenderer(UI::statusColor);
    }

    public static DefaultTableCellRenderer priorityBadge() {
        return new BadgeRenderer(UI::priorityColor);
    }

    public static DefaultTableCellRenderer slaBadge() {
        return new BadgeRenderer(UI::slaColor);
    }

    // A small coloured "pill" (badge) used outside tables
    public static JLabel badge(String text, Color color) {

        JLabel label = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(),
                        color.getBlue(), isDark() ? 55 : 35));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(),
                        getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };

        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(color);
        label.setBorder(new EmptyBorder(4, 12, 4, 12));
        return label;
    }

    // Draws table text inside a rounded coloured pill
    private static class BadgeRenderer extends DefaultTableCellRenderer {

        private final Function<String, Color> colorFor;
        private Color badgeColor = MUTED;

        BadgeRenderer(Function<String, Color> colorFor) {
            this.colorFor = colorFor;
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(
                    table, value, isSelected, false, row, column);

            badgeColor = colorFor.apply(value == null ? "" : value.toString());
            setBackground(isSelected ? table.getSelectionBackground()
                    : (row % 2 == 0 ? CARD : CARD_ALT));
            setFont(new Font("SansSerif", Font.BOLD, 12));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());

            String text = getText();

            if (text != null && !text.isEmpty()) {

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();

                int w = Math.min(fm.stringWidth(text) + 22, getWidth() - 4);
                int h = 22;
                int x = (getWidth() - w) / 2;
                int y = (getHeight() - h) / 2;

                g2.setColor(new Color(badgeColor.getRed(),
                        badgeColor.getGreen(), badgeColor.getBlue(), isDark() ? 55 : 35));
                g2.fillRoundRect(x, y, w, h, h, h);

                g2.setColor(badgeColor);
                g2.drawString(text,
                        x + (w - fm.stringWidth(text)) / 2,
                        y + (h - fm.getHeight()) / 2 + fm.getAscent());
            }

            g2.dispose();
        }
    }

    // ================= PAGE LAYOUTS =================

    // Transparent white button for dark gradient banners
    public static JButton glassButton(String text) {
        JButton button = secondaryButton(text);
        button.putClientProperty("style", "glass");
        button.setForeground(Color.WHITE);
        return button;
    }

    // Puts any form inside a rounded white card in the middle of the screen
    public static void cardScreen(JPanel screen, JComponent content) {
        screen.setBackground(BACKGROUND);
        Theme.Card card = new Theme.Card(new BorderLayout(), 4, 22);
        card.add(content, BorderLayout.CENTER);
        MainFrame.placeCentered(screen, card);
    }

    // Login / register screens: colourful panel on the left,
    // the form in a white card on the right
    public static void authScreen(JPanel screen, JComponent form, boolean admin) {

        screen.setBackground(BACKGROUND);

        Color start = admin ? new Color(13, 148, 136) : new Color(37, 99, 235);
        Color end = admin ? new Color(37, 99, 235) : new Color(124, 58, 237);

        Theme.HeroPanel hero = new Theme.HeroPanel(new BorderLayout(), start, end);
        hero.setBorder(new EmptyBorder(36, 34, 40, 34));
        hero.setPreferredSize(new Dimension(370, 0));

        JPanel heroText = new JPanel();
        heroText.setLayout(new BoxLayout(heroText, BoxLayout.Y_AXIS));
        heroText.setOpaque(false);

        JLabel logo = new JLabel(new ImageIcon(MainFrame.createAppIcon(54)));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tag = new JLabel(admin ? "ADMIN PORTAL" : "STUDENT PORTAL");
        tag.setFont(new Font("SansSerif", Font.BOLD, 11));
        tag.setForeground(new Color(255, 255, 255, 190));
        tag.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel(admin
                ? "<html>Resolve faster.<br>See every issue.</html>"
                : "<html>Raise it. Track it.<br>Get it fixed.</html>");
        title.setFont(new Font("SansSerif", Font.BOLD, 30));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(admin
                ? "<html>Smart dashboard for wardens and campus staff.</html>"
                : "<html>Your voice for a better campus, powered by AI.</html>");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(new Color(255, 255, 255, 210));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        heroText.add(logo);
        heroText.add(Box.createVerticalStrut(26));
        heroText.add(tag);
        heroText.add(Box.createVerticalStrut(8));
        heroText.add(title);
        heroText.add(Box.createVerticalStrut(12));
        heroText.add(subtitle);
        heroText.add(Box.createVerticalStrut(30));

        String[] features = admin
                ? new String[]{"SLA tracking & overdue alerts",
                               "Department-wise analytics",
                               "Monthly reports in one click"}
                : new String[]{"AI detects category & priority",
                               "Live status & full timeline",
                               "Photos, ratings & feedback"};

        for (String feature : features) {
            heroText.add(featureRow(feature));
            heroText.add(Box.createVerticalStrut(12));
        }

        hero.add(heroText, BorderLayout.NORTH);

        Theme.Card card = new Theme.Card(new BorderLayout(), 4, 22);
        card.add(form, BorderLayout.CENTER);

        JPanel shell = new JPanel(new BorderLayout(18, 0));
        shell.setOpaque(false);
        shell.add(hero, BorderLayout.WEST);
        shell.add(card, BorderLayout.CENTER);

        MainFrame.placeCentered(screen, shell);
    }

    private static JComponent featureRow(String text) {

        JLabel label = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int cy = getHeight() / 2;
                g2.setColor(new Color(255, 255, 255, 50));
                g2.fillOval(0, cy - 10, 20, 20);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g2.drawLine(6, cy, 9, cy + 3);
                g2.drawLine(9, cy + 3, 14, cy - 3);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(Color.WHITE);
        label.setBorder(new EmptyBorder(2, 30, 2, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}
