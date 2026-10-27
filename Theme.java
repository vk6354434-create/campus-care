import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicPasswordFieldUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicTextFieldUI;

// The modern look of the whole app.
// Theme.install() is called once in Main, before any window is created.
// After that every JButton, text field, drop-down, scrollbar and
// checkbox in the app automatically gets the new design.
public final class Theme {

    private Theme() {
    }

    public static final Color FOCUS = new Color(59, 130, 246);
    public static final Color PURPLE = new Color(139, 92, 246);
    public static final Color PINK = new Color(236, 72, 153);
    public static final Color NAVY = new Color(11, 17, 34);
    public static final Color NAVY_LIGHT = new Color(30, 41, 90);

    public static void install() {

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // Default look and feel is fine
        }

        // Smooth text everywhere
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        register("ButtonUI", ModernButtonUI.class);
        register("TextFieldUI", ModernTextFieldUI.class);
        register("PasswordFieldUI", ModernPasswordFieldUI.class);
        register("ComboBoxUI", ModernComboBoxUI.class);
        register("ScrollBarUI", ModernScrollBarUI.class);

        Font base = new Font("SansSerif", Font.PLAIN, 13);

        UIManager.put("Button.font", new Font("SansSerif", Font.BOLD, 13));
        UIManager.put("Label.font", base);
        UIManager.put("TextField.font", new Font("SansSerif", Font.PLAIN, 14));
        UIManager.put("PasswordField.font", new Font("SansSerif", Font.PLAIN, 14));
        UIManager.put("TextArea.font", new Font("SansSerif", Font.PLAIN, 14));
        UIManager.put("ComboBox.font", new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("CheckBox.font", new Font("SansSerif", Font.BOLD, 13));
        UIManager.put("ToolTip.font", new Font("SansSerif", Font.PLAIN, 12));
        UIManager.put("OptionPane.messageFont", new Font("SansSerif", Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont", new Font("SansSerif", Font.BOLD, 13));

        UIManager.put("CheckBox.icon", new CheckIcon());
        UIManager.put("ToolTip.border", new EmptyBorder(6, 10, 6, 10));
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());

        // Use the theme the user picked last time
        UI.setMode(savedMode());
        applyColors();
    }

    // Saved theme (Default / Light / Dark), remembered between runs
    private static final String PREF_KEY = "theme";

    public static UI.Mode savedMode() {
        try {
            String name = java.util.prefs.Preferences.userRoot()
                    .node("campuscare").get(PREF_KEY, UI.Mode.DEFAULT.name());
            return UI.Mode.valueOf(name);
        } catch (Exception e) {
            return UI.Mode.DEFAULT;
        }
    }

    // Switch the colours of the whole app. MainFrame.changeTheme()
    // calls this and then builds the screens again.
    public static void setMode(UI.Mode mode) {
        UI.setMode(mode);
        applyColors();
        try {
            java.util.prefs.Preferences.userRoot()
                    .node("campuscare").put(PREF_KEY, UI.mode().name());
        } catch (Exception ignored) {
            // Could not save the choice; it still works for this run
        }
    }

    // Colours of standard Swing parts (dialogs, lists, menus, text areas)
    private static void applyColors() {

        Color selectionList = UI.isDark() ? new Color(37, 78, 160) : new Color(219, 234, 254);

        UIManager.put("Panel.background", UI.isDark() ? UI.CARD : new Color(238, 238, 238));
        UIManager.put("Label.foreground", UI.DARK);

        UIManager.put("TextField.selectionBackground", UI.SELECTION);
        UIManager.put("PasswordField.selectionBackground", UI.SELECTION);
        UIManager.put("TextArea.selectionBackground", UI.SELECTION);
        UIManager.put("TextField.selectionForeground", UI.DARK);
        UIManager.put("PasswordField.selectionForeground", UI.DARK);
        UIManager.put("TextArea.selectionForeground", UI.DARK);
        UIManager.put("TextArea.background", UI.FIELD_BG);
        UIManager.put("TextArea.foreground", UI.DARK);
        UIManager.put("TextArea.caretForeground", UI.DARK);
        UIManager.put("TextField.background", UI.FIELD_BG);
        UIManager.put("TextField.foreground", UI.DARK);
        UIManager.put("TextField.caretForeground", UI.DARK);
        UIManager.put("PasswordField.background", UI.FIELD_BG);
        UIManager.put("PasswordField.foreground", UI.DARK);
        UIManager.put("PasswordField.caretForeground", UI.DARK);

        UIManager.put("ComboBox.selectionBackground", selectionList);
        UIManager.put("ComboBox.selectionForeground", UI.DARK);
        UIManager.put("ComboBox.background", UI.FIELD_BG);
        UIManager.put("ComboBox.foreground", UI.DARK);
        UIManager.put("List.background", UI.CARD);
        UIManager.put("List.foreground", UI.DARK);
        UIManager.put("List.selectionBackground", selectionList);
        UIManager.put("List.selectionForeground", UI.DARK);

        UIManager.put("CheckBox.foreground", UI.DARK);
        UIManager.put("CheckBox.background", UI.CARD);
        UIManager.put("RadioButton.foreground", UI.DARK);

        UIManager.put("ToolTip.background", UI.isDark() ? new Color(236, 241, 250) : new Color(17, 24, 39));
        UIManager.put("ToolTip.foreground", UI.isDark() ? new Color(17, 24, 39) : Color.WHITE);

        UIManager.put("OptionPane.background", UI.CARD);
        UIManager.put("OptionPane.messageForeground", UI.DARK);

        UIManager.put("Viewport.background", UI.CARD);
        UIManager.put("ScrollPane.background", UI.CARD);
        UIManager.put("Table.background", UI.CARD);
        UIManager.put("Table.foreground", UI.DARK);

        UIManager.put("PopupMenu.background", UI.CARD);
        UIManager.put("PopupMenu.border", new RoundedLineBorder(UI.BORDER, 8));
        UIManager.put("MenuItem.background", UI.CARD);
        UIManager.put("MenuItem.foreground", UI.DARK);
        UIManager.put("MenuItem.selectionBackground", UI.CARD_ALT);
        UIManager.put("MenuItem.selectionForeground", UI.DARK);
    }

    private static void register(String key, Class<?> uiClass) {
        UIManager.put(key, uiClass.getName());
        UIManager.put(uiClass.getName(), uiClass);
    }

    // ================= COLOUR HELPERS =================

    static Color mix(Color a, Color b, double t) {
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    static Color alpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    static double luminance(Color c) {
        return (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255;
    }

    static boolean isLight(Color c) {
        double lum = (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255;
        return lum > 0.75;
    }

    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        return g2;
    }

    // Soft drop-shadow drawn as several faint rounded rectangles
    static void paintShadow(Graphics2D g2, int x, int y, int w, int h, int arc, int size) {
        for (int i = size; i > 0; i--) {
            int a = (int) (16.0 * (size - i + 1) / size);
            g2.setColor(new Color(15, 23, 42, Math.max(1, a / 2)));
            g2.fillRoundRect(x - i, y - i + 2, w + i * 2, h + i * 2, arc + i * 2, arc + i * 2);
        }
    }

    // ================= BUTTONS =================

    // Every JButton gets this: rounded corners, gradient, glow,
    // hover and press effects. White buttons get an outlined style,
    // buttons without a filled area get a "link" style.
    public static class ModernButtonUI extends BasicButtonUI {

        public static ComponentUI createUI(JComponent c) {
            return new ModernButtonUI();
        }

        // Fill colour of an outlined button (used to keep its text readable)
        private Color paintedFill;

        @Override
        protected void installDefaults(AbstractButton b) {
            super.installDefaults(b);
            b.setRolloverEnabled(true);
            b.setFocusPainted(false);

            // Transparent, so the rounded corners show the card behind
            // the button instead of a dark square
            b.setOpaque(false);
            if (b.getBorder() == null || b.getBorder() instanceof UIResource) {
                b.setBorder(new EmptyBorder(9, 18, 11, 18));
            }
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void update(Graphics g, JComponent c) {

            // Some code calls setOpaque(true) on buttons. An opaque button
            // must paint its whole rectangle, otherwise old pixels show in
            // the corners. So fill the corners with the colour behind it.
            if (c.isOpaque()) {
                g.setColor(backgroundBehind(c));
                g.fillRect(0, 0, c.getWidth(), c.getHeight());
            }
            paint(g, c);
        }

        // Colour of the first solid panel behind the button
        private static Color backgroundBehind(Component c) {
            Container parent = c.getParent();
            while (parent != null) {
                if (parent instanceof Card) {
                    return UI.CARD;
                }
                if (parent.isOpaque() && parent.getBackground() != null) {
                    return parent.getBackground();
                }
                parent = parent.getParent();
            }
            return UI.BACKGROUND;
        }

        @Override
        public void paint(Graphics g, JComponent c) {

            AbstractButton b = (AbstractButton) c;
            ButtonModel m = b.getModel();
            Graphics2D g2 = smooth(g);

            int w = c.getWidth();
            int h = c.getHeight();
            int arc = Math.min(h - 2, 14);
            boolean hover = m.isRollover() && b.isEnabled();
            boolean pressed = m.isPressed() && m.isArmed();

            paintedFill = null;

            if ("glass".equals(b.getClientProperty("style"))) {
                // See-through button for dark gradient banners
                g2.setColor(new Color(255, 255, 255, pressed ? 70 : hover ? 55 : 30));
                g2.fillRoundRect(0, 0, w - 1, h - 3, arc, arc);
                g2.setColor(new Color(255, 255, 255, hover ? 140 : 80));
                g2.setStroke(new BasicStroke(1.1f));
                g2.drawRoundRect(0, 0, w - 2, h - 4, arc, arc);
                b.setForeground(Color.WHITE);

            } else if (b.isContentAreaFilled()) {

                Color bg = b.getBackground();
                if (bg == null || bg instanceof UIResource) {
                    bg = Color.WHITE;
                }

                if (isLight(bg)) {
                    // Outlined / secondary button
                    Color fill;
                    if (UI.isDark()) {
                        fill = pressed ? UI.BORDER : hover ? UI.CARD_ALT : UI.CARD;
                    } else {
                        fill = pressed ? new Color(229, 231, 235)
                                : hover ? new Color(243, 246, 252) : Color.WHITE;
                    }
                    paintedFill = fill;

                    g2.setColor(new Color(0, 0, 0, UI.isDark() ? 40 : 12));
                    g2.fillRoundRect(0, 2, w - 1, h - 3, arc, arc);

                    g2.setColor(fill);
                    g2.fillRoundRect(0, 0, w - 1, h - 3, arc, arc);

                    g2.setColor(hover ? alpha(FOCUS, 160) : UI.FIELD_BORDER);
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(0, 0, w - 2, h - 4, arc, arc);

                    if (b.getForeground() instanceof UIResource) {
                        b.setForeground(UI.DARK);
                    }

                } else {
                    // Filled / primary button with gradient and glow
                    Color base = b.isEnabled() ? bg : mix(bg, Color.WHITE, 0.55);
                    Color top = mix(base, Color.WHITE, hover ? 0.22 : 0.10);
                    Color bottom = mix(base, Color.BLACK, pressed ? 0.25 : 0.10);

                    if (b.isEnabled()) {
                        g2.setColor(alpha(base, hover ? 90 : 55));
                        g2.fillRoundRect(2, 4, w - 4, h - 4, arc, arc);
                    }

                    int offset = pressed ? 1 : 0;
                    g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
                    g2.fillRoundRect(0, offset, w - 1, h - 3, arc, arc);

                    // Glassy highlight on the upper edge
                    g2.setColor(new Color(255, 255, 255, hover ? 70 : 45));
                    g2.drawRoundRect(1, 1 + offset, w - 3, h - 6, arc - 2, arc - 2);

                    if (b.getForeground() instanceof UIResource
                            || b.getForeground().equals(Color.BLACK)) {
                        b.setForeground(Color.WHITE);
                    }
                }
            } else if (hover && !(b instanceof JToggleButton)) {
                // Link button: soft highlight on hover
                g2.setColor(alpha(b.getForeground(), 22));
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
            }

            g2.dispose();
            super.paint(g, c);
        }

        @Override
        protected void paintText(Graphics g, AbstractButton b,
                                 Rectangle textRect, String text) {

            Graphics2D g2 = smooth(g);
            FontMetrics fm = g2.getFontMetrics(b.getFont());
            Color fg = b.getForeground();

            // Make sure text on an outlined button is always readable
            // (e.g. dark text on a dark-theme button)
            if (paintedFill != null
                    && Math.abs(luminance(fg) - luminance(paintedFill)) < 0.35) {
                fg = UI.DARK;
            }

            g2.setFont(b.getFont());
            g2.setColor(b.isEnabled() ? fg : alpha(fg, 110));

            int y = textRect.y + fm.getAscent();
            if (b.isContentAreaFilled() && b.getModel().isPressed()) {
                y += 1;
            }
            g2.drawString(text, textRect.x, y - (b.isContentAreaFilled() ? 1 : 0));
            g2.dispose();
        }

        @Override
        protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect,
                                  Rectangle textRect, Rectangle iconRect) {
            // No dotted focus rectangle
        }
    }

    // ================= TEXT FIELDS =================

    // Rounded white field; border turns blue with a glow when focused
    public static class FieldBorder extends AbstractBorder {

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = smooth(g);
            boolean focus = c.hasFocus()
                    || (c instanceof JComboBox<?> combo && combo.isPopupVisible());

            if (focus) {
                g2.setColor(alpha(FOCUS, 45));
                g2.setStroke(new BasicStroke(4f));
                g2.drawRoundRect(x + 2, y + 2, w - 5, h - 5, 12, 12);
                g2.setColor(FOCUS);
                g2.setStroke(new BasicStroke(1.6f));
            } else {
                g2.setColor(UI.FIELD_BORDER);
                g2.setStroke(new BasicStroke(1.2f));
            }
            g2.drawRoundRect(x + 2, y + 2, w - 5, h - 5, 12, 12);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(8, 13, 8, 13);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(8, 13, 8, 13);
            return insets;
        }
    }

    // Shared painting for text and password fields
    private static void paintFieldBackground(Graphics g, JComponent c) {
        Graphics2D g2 = smooth(g);
        g2.setColor(c.isEnabled()
                ? (c instanceof JTextField tf && !tf.isEditable()
                        ? UI.CARD_ALT : c.getBackground())
                : UI.DISABLED_BG);
        g2.fillRoundRect(2, 2, c.getWidth() - 4, c.getHeight() - 4, 12, 12);
        g2.dispose();
    }

    private static void prepareField(JComponent c) {
        c.setOpaque(false);
        if (c.getBorder() == null || c.getBorder() instanceof UIResource) {
            c.setBorder(new FieldBorder());
        }
        c.setBackground(UI.FIELD_BG);
        c.setForeground(UI.DARK);
        if (c instanceof javax.swing.text.JTextComponent text) {
            text.setSelectionColor(UI.SELECTION);
            text.setSelectedTextColor(UI.DARK);
        }
        c.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                c.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                c.repaint();
            }
        });
        if (c instanceof JTextField tf) {
            tf.setCaretColor(FOCUS);
        }
    }

    public static class ModernTextFieldUI extends BasicTextFieldUI {

        public static ComponentUI createUI(JComponent c) {
            return new ModernTextFieldUI();
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            prepareField(c);
        }

        @Override
        protected void paintSafely(Graphics g) {
            JComponent c = getComponent();
            if (!c.isOpaque()) {
                paintFieldBackground(g, c);
            }
            super.paintSafely(g);
        }
    }

    public static class ModernPasswordFieldUI extends BasicPasswordFieldUI {

        public static ComponentUI createUI(JComponent c) {
            return new ModernPasswordFieldUI();
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            prepareField(c);
        }

        @Override
        protected void paintSafely(Graphics g) {
            JComponent c = getComponent();
            if (!c.isOpaque()) {
                paintFieldBackground(g, c);
            }
            super.paintSafely(g);
        }
    }

    // ================= DROP-DOWNS =================

    public static class ModernComboBoxUI extends BasicComboBoxUI {

        public static ComponentUI createUI(JComponent c) {
            return new ModernComboBoxUI();
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            comboBox.setOpaque(false);
            comboBox.setBackground(UI.FIELD_BG);
            comboBox.setForeground(UI.DARK);
            comboBox.setBorder(new FieldBorder());
            comboBox.setCursor(new Cursor(Cursor.HAND_CURSOR));

            comboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(
                        JList<?> list, Object value, int index,
                        boolean selected, boolean focus) {
                    super.getListCellRendererComponent(list, value, index, selected, false);
                    setBorder(new EmptyBorder(7, 10, 7, 10));
                    return this;
                }
            });

            comboBox.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    comboBox.repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    comboBox.repaint();
                }
            });
        }

        @Override
        protected JButton createArrowButton() {
            JButton arrow = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = smooth(g);
                    int cx = getWidth() / 2 - 2;
                    int cy = getHeight() / 2;
                    g2.setColor(comboBox.isEnabled() ? UI.MUTED : UI.BORDER);
                    g2.setStroke(new BasicStroke(1.8f,
                            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(cx - 4, cy - 2, cx, cy + 2);
                    g2.drawLine(cx, cy + 2, cx + 4, cy - 2);
                    g2.dispose();
                }
            };
            arrow.setBorder(BorderFactory.createEmptyBorder());
            arrow.setContentAreaFilled(false);
            arrow.setFocusable(false);
            arrow.setPreferredSize(new Dimension(26, 26));
            return arrow;
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            Graphics2D g2 = smooth(g);
            g2.setColor(comboBox.isEnabled() ? UI.FIELD_BG : UI.DISABLED_BG);
            g2.fillRoundRect(2, 2, c.getWidth() - 4, c.getHeight() - 4, 12, 12);
            g2.dispose();
            super.paint(g, c);
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            // Background already painted (rounded)
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
            ListCellRenderer renderer = comboBox.getRenderer();
            Component c = renderer.getListCellRendererComponent(
                    listBox, comboBox.getSelectedItem(), -1, false, false);
            c.setFont(comboBox.getFont());
            c.setForeground(comboBox.isEnabled() ? UI.DARK : UI.MUTED);

            boolean wasOpaque = c.isOpaque();
            if (c instanceof JComponent jc) {
                jc.setOpaque(false);
                jc.setBorder(new EmptyBorder(0, 0, 0, 0));
            }
            currentValuePane.paintComponent(g, c, comboBox,
                    bounds.x, bounds.y, bounds.width, bounds.height, false);
            if (c instanceof JComponent jc) {
                jc.setOpaque(wasOpaque);
            }
        }
    }

    // ================= SCROLLBARS =================

    public static class ModernScrollBarUI extends BasicScrollBarUI {

        public static ComponentUI createUI(JComponent c) {
            return new ModernScrollBarUI();
        }

        private boolean hover;

        @Override
        protected void installListeners() {
            super.installListeners();
            scrollbar.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    scrollbar.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    scrollbar.repaint();
                }
            });
        }

        @Override
        protected void configureScrollBarColors() {
            thumbColor = UI.SCROLL_THUMB;
            trackColor = new Color(0, 0, 0, 0);
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            return scrollbar.getOrientation() == JScrollBar.VERTICAL
                    ? new Dimension(10, 48) : new Dimension(48, 10);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zeroButton();
        }

        private JButton zeroButton() {
            JButton b = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                }
            };
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            b.setBorder(BorderFactory.createEmptyBorder());
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            // Invisible track
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty()) {
                return;
            }
            Graphics2D g2 = smooth(g);
            g2.setColor(hover || isDragging ? UI.SCROLL_THUMB_HOVER : thumbColor);
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            } else {
                g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            }
            g2.dispose();
        }
    }

    // ================= CHECKBOX =================

    public static class CheckIcon implements Icon, UIResource {

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            AbstractButton b = (AbstractButton) c;
            boolean on = b.isSelected();
            Graphics2D g2 = smooth(g);

            if (on) {
                g2.setPaint(new GradientPaint(x, y, FOCUS, x + 18, y + 18, PURPLE));
                g2.fillRoundRect(x, y, 18, 18, 6, 6);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.2f,
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 5, y + 9, x + 8, y + 12);
                g2.drawLine(x + 8, y + 12, x + 13, y + 6);
            } else {
                g2.setColor(UI.FIELD_BG);
                g2.fillRoundRect(x, y, 18, 18, 6, 6);
                g2.setColor(b.getModel().isRollover() ? FOCUS : UI.FIELD_BORDER);
                g2.setStroke(new BasicStroke(1.4f));
                g2.drawRoundRect(x, y, 17, 17, 6, 6);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 20;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }
    }

    // ================= CARDS AND BORDERS =================

    // White rounded card with a soft shadow
    public static class Card extends JPanel {

        private final int arc;
        private Color accent;

        public Card(LayoutManager layout, int padding) {
            this(layout, padding, 18);
        }

        public Card(LayoutManager layout, int padding, int arc) {
            super(layout);
            this.arc = arc;
            setOpaque(false);
            setBorder(new EmptyBorder(padding + 6, padding + 6, padding + 8, padding + 6));
        }

        // Thin coloured strip on the top edge
        public void setAccent(Color accent) {
            this.accent = accent;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int x = 6;
            int y = 6;
            int w = getWidth() - 12;
            int h = getHeight() - 14;

            paintShadow(g2, x, y, w, h, arc, 6);

            g2.setColor(UI.CARD);
            g2.fillRoundRect(x, y, w, h, arc, arc);

            if (accent != null) {
                Shape old = g2.getClip();
                g2.clip(new RoundRectangle2D.Float(x, y, w, h, arc, arc));
                g2.setPaint(new GradientPaint(x, 0, accent, x + w, 0,
                        mix(accent, Color.WHITE, 0.45)));
                g2.fillRect(x, y, w, 4);
                g2.setClip(old);
            }

            g2.setColor(UI.LINE);
            g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
            g2.dispose();
        }
    }

    // Dark gradient banner used at the top of dashboards
    public static class HeroPanel extends JPanel {

        private final Color start;
        private final Color end;

        public HeroPanel(LayoutManager layout, Color start, Color end) {
            super(layout);
            this.start = start;
            this.end = end;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth();
            int h = getHeight();

            paintShadow(g2, 0, 0, w, h - 6, 22, 5);

            Shape shape = new RoundRectangle2D.Float(0, 0, w, h - 6, 22, 22);
            g2.setPaint(new GradientPaint(0, 0, start, w, h, end));
            g2.fill(shape);

            // Decorative glowing circles
            g2.clip(shape);
            g2.setPaint(new RadialGradientPaint(new Point(w - 120, -20), 220f,
                    new float[]{0f, 1f},
                    new Color[]{new Color(255, 255, 255, 50), new Color(255, 255, 255, 0)}));
            g2.fillRect(0, 0, w, h);
            g2.setPaint(new RadialGradientPaint(new Point(w / 3, h + 60), 200f,
                    new float[]{0f, 1f},
                    new Color[]{new Color(236, 72, 153, 70), new Color(236, 72, 153, 0)}));
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(255, 255, 255, 18));
            g2.fillOval(w - 200, h - 90, 160, 160);
            g2.fillOval(w - 90, 20, 60, 60);
            g2.dispose();
        }
    }

    // Rounded 1px border (used for scroll panes around tables)
    public static class RoundedLineBorder extends AbstractBorder {

        private final Color color;
        private final int arc;

        public RoundedLineBorder(Color color, int arc) {
            this.color = color;
            this.arc = arc;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = smooth(g);
            g2.setColor(color);
            g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(2, 2, 2, 2);
        }
    }

    public static Border fieldBorder() {
        return new FieldBorder();
    }
}
