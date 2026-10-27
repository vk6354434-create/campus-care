import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// First screen: choose Student or Admin, or track a complaint
public class Welcome extends JPanel {

    // Colours depend on the theme: dark navy (Default / Dark)
    // or soft white-lavender (Light). Read when the screen is built.
    private static boolean dark;
    private static Color BG_TOP;
    private static Color BG_BOTTOM;
    private static Color TEXT;
    private static Color MUTED;

    private static void loadColors() {
        dark = UI.chromeDark();
        BG_TOP = dark ? new Color(11, 17, 34) : new Color(238, 242, 255);
        BG_BOTTOM = dark ? new Color(23, 30, 66) : new Color(250, 245, 255);
        TEXT = dark ? new Color(240, 244, 255) : new Color(17, 24, 39);
        MUTED = dark ? new Color(165, 178, 215) : new Color(75, 85, 105);
    }

    public Welcome(MainFrame frame) {

        loadColors();

        setLayout(new BorderLayout());
        setOpaque(true);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(30, 40, 30, 40));

        // ================= HEADER =================

        JLabel badge = new JLabel("  AI-POWERED  •  COLLEGES • HOSTELS • PGs  ") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Theme.smooth(g);
                g2.setColor(new Color(99, 102, 241, 45));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        getHeight(), getHeight());
                g2.setColor(new Color(129, 140, 248, 120));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(new Font("SansSerif", Font.BOLD, 11));
        badge.setForeground(dark ? new Color(165, 180, 252) : new Color(79, 70, 229));
        badge.setBorder(new EmptyBorder(6, 12, 6, 12));
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new GradientLabel("Campus Complaint System");
        title.setFont(new Font("SansSerif", Font.BOLD, 40));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("A better campus starts with your voice.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 17));
        subtitle.setForeground(MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(badge);
        content.add(Box.createVerticalStrut(18));
        content.add(title);
        content.add(Box.createVerticalStrut(10));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(36));

        // ================= ROLE CARDS =================

        JLabel question = new JLabel("How would you like to continue?");
        question.setFont(new Font("SansSerif", Font.BOLD, 18));
        question.setForeground(TEXT);
        question.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(question);
        content.add(Box.createVerticalStrut(20));

        JPanel cards = new JPanel(new GridLayout(1, 2, 26, 0));
        cards.setOpaque(false);
        cards.setMaximumSize(new Dimension(720, 270));
        cards.setPreferredSize(new Dimension(720, 270));
        cards.setAlignmentX(Component.CENTER_ALIGNMENT);

        cards.add(new RoleCard(
                false,
                "Student",
                "Submit complaints with photos, let AI pick the category, and track every update.",
                new Color(59, 130, 246), new Color(139, 92, 246),
                () -> frame.navigateTo(MainFrame.STUDENT_LOGIN)));

        cards.add(new RoleCard(
                true,
                "Admin",
                "Manage complaints, assign departments, watch deadlines and see live analytics.",
                new Color(16, 185, 129), new Color(14, 165, 233),
                () -> frame.navigateTo(MainFrame.ADMIN_LOGIN)));

        content.add(cards);
        content.add(Box.createVerticalStrut(26));

        // ================= TRACK LINK =================

        JButton trackButton = new JButton(
                "Already submitted a complaint?  Track it by ID  →");
        trackButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        trackButton.setForeground(dark ? new Color(129, 170, 255) : UI.PRIMARY);
        trackButton.setContentAreaFilled(false);
        trackButton.setBorderPainted(false);
        trackButton.setFocusPainted(false);
        trackButton.setBorder(new EmptyBorder(8, 14, 8, 14));
        trackButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        trackButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        trackButton.addActionListener(e ->
                frame.navigateTo(MainFrame.TRACK_COMPLAINT));

        content.add(trackButton);
        content.add(Box.createVerticalStrut(30));

        // ================= FEATURE STRIP =================

        JPanel features = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        features.setOpaque(false);
        features.setAlignmentX(Component.CENTER_ALIGNMENT);
        features.add(featureChip("AI Smart Category", new Color(167, 139, 250)));
        features.add(featureChip("Auto Priority", new Color(251, 146, 60)));
        features.add(featureChip("SLA Tracking", new Color(248, 113, 113)));
        features.add(featureChip("Live Analytics", new Color(52, 211, 153)));
        features.add(featureChip("Monthly Reports", new Color(96, 165, 250)));
        features.setMaximumSize(new Dimension(900, 40));

        content.add(features);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(content);

        JScrollPane scroll = new JScrollPane(center);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        JLabel footer = new JLabel(
                "CAMPUS CARE  •  Simple. Organized. Connected.",
                SwingConstants.CENTER);
        footer.setFont(new Font("SansSerif", Font.PLAIN, 11));
        footer.setForeground(dark ? new Color(110, 125, 165) : new Color(100, 116, 139));
        footer.setBorder(new EmptyBorder(0, 0, 16, 0));

        add(scroll, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    // Dark gradient background with soft coloured glows
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        int w = getWidth();
        int h = getHeight();

        g2.setPaint(new GradientPaint(0, 0, BG_TOP, 0, h, BG_BOTTOM));
        g2.fillRect(0, 0, w, h);

        glow(g2, w / 5, h / 4, 360, new Color(59, 130, 246, dark ? 70 : 40));
        glow(g2, w * 4 / 5, h / 3, 380, new Color(168, 85, 247, dark ? 60 : 35));
        glow(g2, w / 2, h + 40, 420, new Color(236, 72, 153, dark ? 45 : 30));

        // Faint dotted grid
        g2.setColor(dark ? new Color(255, 255, 255, 10) : new Color(30, 41, 90, 14));
        for (int x = 20; x < w; x += 32) {
            for (int y = 20; y < h; y += 32) {
                g2.fillOval(x, y, 2, 2);
            }
        }
        g2.dispose();
    }

    private static void glow(Graphics2D g2, int x, int y, int radius, Color color) {
        g2.setPaint(new RadialGradientPaint(new Point(x, y), radius,
                new float[]{0f, 1f},
                new Color[]{color, new Color(color.getRed(), color.getGreen(),
                        color.getBlue(), 0)}));
        g2.fillRect(x - radius, y - radius, radius * 2, radius * 2);
    }

    private static JComponent featureChip(String text, Color dot) {
        JLabel chip = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Theme.smooth(g);
                g2.setColor(dark ? new Color(255, 255, 255, 14) : new Color(255, 255, 255, 220));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        getHeight(), getHeight());
                g2.setColor(dark ? new Color(255, 255, 255, 35) : new Color(203, 213, 225));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        getHeight(), getHeight());
                g2.setColor(dot);
                g2.fillOval(12, getHeight() / 2 - 4, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setFont(new Font("SansSerif", Font.BOLD, 12));
        chip.setForeground(dark ? new Color(220, 228, 250) : UI.LABEL);
        chip.setBorder(new EmptyBorder(8, 28, 8, 14));
        return chip;
    }

    // Heading painted with a white-to-lavender gradient
    private static class GradientLabel extends JLabel {

        GradientLabel(String text) {
            super(text);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.setPaint(dark
                    ? new GradientPaint(x, 0, Color.WHITE,
                            x + fm.stringWidth(getText()), 0, new Color(196, 181, 253))
                    : new GradientPaint(x, 0, new Color(30, 41, 110),
                            x + fm.stringWidth(getText()), 0, new Color(124, 58, 237)));
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }

    // Glassy card with a drawn icon; glows and lifts on hover
    private static class RoleCard extends JPanel {

        private final boolean admin;
        private final Color start;
        private final Color end;
        private boolean hover;

        RoleCard(boolean admin, String role, String description,
                 Color start, Color end, Runnable action) {

            this.admin = admin;
            this.start = start;
            this.end = end;

            setOpaque(false);
            setLayout(new BorderLayout(0, 12));
            setBorder(new EmptyBorder(96, 28, 26, 28));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            JLabel roleLabel = new JLabel(role);
            roleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
            roleLabel.setForeground(TEXT);

            JLabel text = new JLabel("<html>" + description + "</html>");
            text.setFont(new Font("SansSerif", Font.PLAIN, 13));
            text.setForeground(MUTED);
            text.setVerticalAlignment(SwingConstants.TOP);

            JButton button = new JButton("Continue as " + role + "  →");
            button.setBackground(start);
            button.setForeground(Color.WHITE);
            button.setFont(new Font("SansSerif", Font.BOLD, 14));
            button.setPreferredSize(new Dimension(0, 44));
            button.addActionListener(e -> action.run());

            JPanel textBox = new JPanel(new BorderLayout(0, 6));
            textBox.setOpaque(false);
            textBox.add(roleLabel, BorderLayout.NORTH);
            textBox.add(text, BorderLayout.CENTER);

            add(textBox, BorderLayout.CENTER);
            add(button, BorderLayout.SOUTH);

            MouseAdapter hoverListener = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    Point p = SwingUtilities.convertPoint(
                            e.getComponent(), e.getPoint(), RoleCard.this);
                    if (!RoleCard.this.contains(p)) {
                        hover = false;
                        repaint();
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    action.run();
                }
            };
            addMouseListener(hoverListener);
            button.addMouseListener(hoverListener);
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = Theme.smooth(g);
            int w = getWidth();
            int h = getHeight();
            int lift = hover ? 0 : 4;

            // Coloured glow under the card on hover
            if (hover) {
                for (int i = 14; i > 0; i -= 2) {
                    g2.setColor(new Color(start.getRed(), start.getGreen(),
                            start.getBlue(), 6));
                    g2.fillRoundRect(-i / 2 + 4, -i / 2 + 8, w + i - 8, h + i - 10, 30, 30);
                }
            }

            RoundRectangle2D shape = new RoundRectangle2D.Float(
                    2, 2 + lift, w - 4, h - 8, 24, 24);

            if (dark) {
                g2.setColor(new Color(255, 255, 255, hover ? 22 : 13));
            } else {
                Theme.paintShadow(g2, 2, 2 + lift, w - 4, h - 8, 24, 6);
                g2.setColor(Color.WHITE);
            }
            g2.fill(shape);

            g2.setPaint(new GradientPaint(0, 0,
                    new Color(start.getRed(), start.getGreen(), start.getBlue(), hover ? 210 : 110),
                    w, h,
                    new Color(end.getRed(), end.getGreen(), end.getBlue(), hover ? 210 : 70)));
            g2.setStroke(new BasicStroke(hover ? 2f : 1.3f));
            g2.draw(shape);

            // Icon tile
            int size = 56;
            int ix = 28;
            int iy = 26 + lift;
            g2.setPaint(new GradientPaint(ix, iy, start, ix + size, iy + size, end));
            g2.fillRoundRect(ix, iy, size, size, 18, 18);
            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillRoundRect(ix, iy, size, size / 2, 18, 18);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            if (admin) {
                drawShield(g2, ix, iy, size);
            } else {
                drawCap(g2, ix, iy, size);
            }

            g2.dispose();
        }

        private static void drawCap(Graphics2D g2, int x, int y, int s) {
            Path2D top = new Path2D.Float();
            top.moveTo(x + s * 0.18, y + s * 0.42);
            top.lineTo(x + s * 0.50, y + s * 0.27);
            top.lineTo(x + s * 0.82, y + s * 0.42);
            top.lineTo(x + s * 0.50, y + s * 0.57);
            top.closePath();
            g2.fill(top);

            Path2D body = new Path2D.Float();
            body.moveTo(x + s * 0.30, y + s * 0.50);
            body.lineTo(x + s * 0.30, y + s * 0.66);
            body.quadTo(x + s * 0.50, y + s * 0.78, x + s * 0.70, y + s * 0.66);
            body.lineTo(x + s * 0.70, y + s * 0.50);
            g2.draw(body);

            g2.drawLine((int) (x + s * 0.82), (int) (y + s * 0.42),
                    (int) (x + s * 0.82), (int) (y + s * 0.62));
        }

        private static void drawShield(Graphics2D g2, int x, int y, int s) {
            Path2D shield = new Path2D.Float();
            shield.moveTo(x + s * 0.50, y + s * 0.20);
            shield.lineTo(x + s * 0.76, y + s * 0.30);
            shield.curveTo(x + s * 0.76, y + s * 0.56, x + s * 0.66, y + s * 0.71,
                    x + s * 0.50, y + s * 0.80);
            shield.curveTo(x + s * 0.34, y + s * 0.71, x + s * 0.24, y + s * 0.56,
                    x + s * 0.24, y + s * 0.30);
            shield.closePath();
            g2.draw(shield);

            Path2D tick = new Path2D.Float();
            tick.moveTo(x + s * 0.39, y + s * 0.50);
            tick.lineTo(x + s * 0.47, y + s * 0.58);
            tick.lineTo(x + s * 0.62, y + s * 0.42);
            g2.draw(tick);
        }
    }
}
