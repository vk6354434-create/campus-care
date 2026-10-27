import java.awt.*;
import java.awt.geom.Arc2D;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// Simple charts drawn with Java2D (no external library needed)
public final class Charts {

    private Charts() {
    }

    private static void smooth(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static JPanel chartBox() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(22, 24, 24, 24));
        panel.setPreferredSize(new Dimension(420, 290));
        return panel;
    }

    // Rounded white card with soft shadow behind every chart
    private static void paintCard(JComponent c, Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        int w = c.getWidth() - 12;
        int h = c.getHeight() - 14;
        Theme.paintShadow(g2, 6, 6, w, h, 18, 6);
        g2.setColor(UI.CARD);
        g2.fillRoundRect(6, 6, w, h, 18, 18);
        g2.setColor(UI.LINE);
        g2.drawRoundRect(6, 6, w - 1, h - 1, 18, 18);
        g2.dispose();
    }

    // ================= BAR CHART =================

    public static class BarChart extends JPanel {

        private final String title;
        private String[] labels = {};
        private int[] values = {};
        private Color[] colors = {UI.PRIMARY};

        public BarChart(String title) {
            this.title = title;
            JPanel box = chartBox();
            setOpaque(false);
            setBorder(box.getBorder());
            setPreferredSize(box.getPreferredSize());
        }

        public void setData(String[] labels, int[] values, Color[] colors) {
            this.labels = labels;
            this.values = values;
            this.colors = colors;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {

            paintCard(this, g);

            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);

            Insets in = getInsets();
            int x0 = in.left;
            int y0 = in.top;
            int width = getWidth() - in.left - in.right;
            int height = getHeight() - in.top - in.bottom;

            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.setColor(UI.DARK);
            g2.drawString(title, x0, y0 + 14);

            int top = y0 + 38;
            int bottom = y0 + height - 24;
            int left = x0 + 30;
            int chartWidth = width - 30;

            if (bottom - top < 20 || chartWidth < 20) {
                g2.dispose();
                return;
            }

            int max = 0;
            for (int v : values) {
                max = Math.max(max, v);
            }

            int lines = 4;
            int step = Math.max(1, (int) Math.ceil(max / (double) lines));
            int niceMax = step * lines;

            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            FontMetrics fm = g2.getFontMetrics();

            for (int i = 0; i <= lines; i++) {
                int y = bottom - (bottom - top) * i / lines;
                g2.setColor(UI.BORDER);
                g2.drawLine(left, y, left + chartWidth, y);

                String text = String.valueOf(step * i);
                g2.setColor(UI.MUTED);
                g2.drawString(text, left - 8 - fm.stringWidth(text), y + 4);
            }

            if (labels.length == 0 || max == 0) {
                g2.setColor(UI.MUTED);
                String empty = "No data yet";
                g2.drawString(empty,
                        left + (chartWidth - fm.stringWidth(empty)) / 2,
                        (top + bottom) / 2);
                g2.dispose();
                return;
            }

            int slot = chartWidth / labels.length;
            int barWidth = Math.max(8, Math.min(46, (int) (slot * 0.6)));

            for (int i = 0; i < labels.length; i++) {

                int barHeight = (int) ((bottom - top)
                        * (values[i] / (double) niceMax));
                int bx = left + slot * i + (slot - barWidth) / 2;

                Color barColor = colors[i % colors.length];

                if (barHeight > 0) {
                    g2.setPaint(new GradientPaint(0, bottom - barHeight,
                            Theme.mix(barColor, Color.WHITE, 0.25), 0, bottom, barColor));
                    g2.fillRoundRect(bx, bottom - barHeight,
                            barWidth, barHeight, 10, 10);
                    g2.fillRect(bx, bottom - Math.min(barHeight, 5),
                            barWidth, Math.min(barHeight, 5));
                }

                // Value above the bar
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                FontMetrics bold = g2.getFontMetrics();
                String value = String.valueOf(values[i]);
                g2.setColor(UI.DARK);
                g2.drawString(value,
                        bx + (barWidth - bold.stringWidth(value)) / 2,
                        bottom - barHeight - 5);

                // Label under the bar, shortened if it does not fit
                g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                String label = fit(labels[i], fm, slot - 4);
                g2.setColor(UI.MUTED);
                g2.drawString(label,
                        left + slot * i + (slot - fm.stringWidth(label)) / 2,
                        bottom + 16);
            }

            g2.dispose();
        }
    }

    // ================= DONUT CHART =================

    public static class DonutChart extends JPanel {

        private final String title;
        private String[] labels = {};
        private int[] values = {};
        private Color[] colors = {UI.PRIMARY};

        public DonutChart(String title) {
            this.title = title;
            JPanel box = chartBox();
            setOpaque(false);
            setBorder(box.getBorder());
            setPreferredSize(box.getPreferredSize());
        }

        public void setData(String[] labels, int[] values, Color[] colors) {
            this.labels = labels;
            this.values = values;
            this.colors = colors;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {

            paintCard(this, g);

            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);

            Insets in = getInsets();
            int x0 = in.left;
            int y0 = in.top;
            int width = getWidth() - in.left - in.right;
            int height = getHeight() - in.top - in.bottom;

            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.setColor(UI.DARK);
            g2.drawString(title, x0, y0 + 14);

            int total = 0;
            for (int v : values) {
                total += v;
            }

            int areaTop = y0 + 30;
            int size = Math.min(height - 40, width / 2 - 10);

            if (size < 40) {
                g2.dispose();
                return;
            }

            int cx = x0 + 6;
            int cy = areaTop + (height - 30 - size) / 2;
            int thickness = Math.max(14, size / 5);

            if (total == 0) {
                g2.setColor(UI.BORDER);
                g2.setStroke(new BasicStroke(thickness));
                g2.drawOval(cx + thickness / 2, cy + thickness / 2,
                        size - thickness, size - thickness);
            } else {
                double start = 90;
                g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER));

                for (int i = 0; i < values.length; i++) {
                    if (values[i] == 0) {
                        continue;
                    }
                    double extent = -360.0 * values[i] / total;
                    g2.setColor(colors[i % colors.length]);
                    g2.draw(new Arc2D.Double(
                            cx + thickness / 2.0, cy + thickness / 2.0,
                            size - thickness, size - thickness,
                            start, extent, Arc2D.OPEN));
                    start += extent;
                }
            }

            // Total in the middle
            g2.setFont(new Font("SansSerif", Font.BOLD, 20));
            FontMetrics big = g2.getFontMetrics();
            String totalText = String.valueOf(total);
            g2.setColor(UI.DARK);
            g2.drawString(totalText,
                    cx + (size - big.stringWidth(totalText)) / 2,
                    cy + size / 2 + 4);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            FontMetrics small = g2.getFontMetrics();
            g2.setColor(UI.MUTED);
            g2.drawString("total",
                    cx + (size - small.stringWidth("total")) / 2,
                    cy + size / 2 + 20);

            // Legend
            int lx = cx + size + 24;
            int ly = cy + Math.max(0, (size - labels.length * 26) / 2) + 12;

            for (int i = 0; i < labels.length; i++) {

                int percent = total == 0
                        ? 0 : Math.round(values[i] * 100f / total);

                g2.setColor(colors[i % colors.length]);
                g2.fillRoundRect(lx, ly - 10, 12, 12, 4, 4);

                g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g2.setColor(UI.DARK);
                g2.drawString(labels[i], lx + 20, ly);

                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.setColor(UI.MUTED);
                g2.drawString(values[i] + "  (" + percent + "%)",
                        lx + 112, ly);

                ly += 26;
            }

            g2.dispose();
        }
    }

    private static String fit(String text, FontMetrics fm, int width) {

        if (fm.stringWidth(text) <= width) {
            return text;
        }

        String shortText = text;

        while (shortText.length() > 1
                && fm.stringWidth(shortText + "…") > width) {
            shortText = shortText.substring(0, shortText.length() - 1);
        }

        return shortText + "…";
    }
}
