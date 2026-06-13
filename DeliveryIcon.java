package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * DeliveryIcon — Pure Java2D vector icon renderer.
 * Draws food-delivery themed icons that look like the reference image:
 * cloche/dish, timer/clock, cutlery, rider/scooter, phone, star, cart, etc.
 *
 * Usage:
 *   JLabel lbl = DeliveryIcon.iconLabel(DeliveryIcon.DISH,    28, new Color(108,45,180));
 *   JLabel lbl = DeliveryIcon.iconLabel(DeliveryIcon.TIMER,   24, Color.ORANGE);
 *   JLabel lbl = DeliveryIcon.iconLabel(DeliveryIcon.CUTLERY, 22, Color.WHITE);
 */
public class DeliveryIcon {

    // ── Icon type constants ───────────────────────────────────────────────────
    public static final int DISH     = 0;   // cloche / serving dish
    public static final int TIMER    = 1;   // clock / stopwatch
    public static final int CUTLERY  = 2;   // fork + knife
    public static final int RIDER    = 3;   // delivery scooter
    public static final int CART     = 4;   // shopping cart
    public static final int PHONE    = 5;   // mobile phone
    public static final int STAR     = 6;   // rating star
    public static final int LOCATION = 7;   // map pin
    public static final int ORDERS   = 8;   // receipt / orders
    public static final int HOME     = 9;   // home
    public static final int TRASH    = 10;  // delete / trash
    public static final int TRACK    = 11;  // tracking / route
    public static final int GIFT     = 12;  // gift / offer
    public static final int PERSON   = 13;  // user / person
    public static final int LOCK     = 14;  // password lock
    public static final int MAIL     = 15;  // email

    /**
     * Creates a JLabel with a custom-painted delivery icon.
     * @param type    one of the constants above
     * @param size    icon size in pixels (e.g. 24, 28, 32)
     * @param color   fill/stroke color
     */
    public static JLabel iconLabel(int type, int size, Color color) {
        Icon icon = new Icon() {
            @Override public int getIconWidth()  { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.translate(x, y);
                g2.setColor(color);
                drawIcon(g2, type, size);
                g2.dispose();
            }
        };
        JLabel lbl = new JLabel(icon);
        lbl.setPreferredSize(new Dimension(size, size));
        return lbl;
    }

    /** Draw inside a [0,0,size,size] coordinate space */
    private static void drawIcon(Graphics2D g2, int type, int s) {
        float S = s;          // float size for calculations
        float sw = S * 0.08f; // standard stroke width

        switch (type) {

            // ── DISH (serving cloche) ─────────────────────────────────────
            case DISH -> {
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // dome top
                Arc2D dome = new Arc2D.Float(S*0.05f, S*0.15f, S*0.90f, S*0.60f, 0, 180, Arc2D.OPEN);
                g2.draw(dome);
                // base plate line
                g2.drawLine((int)(S*0.02f),(int)(S*0.55f),(int)(S*0.98f),(int)(S*0.55f));
                // plate bottom arc
                Arc2D plate = new Arc2D.Float(S*0.08f, S*0.52f, S*0.84f, S*0.18f, 180, 180, Arc2D.OPEN);
                g2.draw(plate);
                // handle on top of dome
                g2.fillOval((int)(S*0.42f),(int)(S*0.08f),(int)(S*0.16f),(int)(S*0.12f));
            }

            // ── TIMER (stopwatch) ─────────────────────────────────────────
            case TIMER -> {
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // circle body
                g2.drawOval((int)(S*0.12f),(int)(S*0.18f),(int)(S*0.76f),(int)(S*0.76f));
                // clock hands
                g2.drawLine((int)(S*0.50f),(int)(S*0.56f),(int)(S*0.50f),(int)(S*0.32f)); // 12
                g2.drawLine((int)(S*0.50f),(int)(S*0.56f),(int)(S*0.68f),(int)(S*0.56f)); // 3
                // stem + buttons
                g2.drawLine((int)(S*0.50f),(int)(S*0.18f),(int)(S*0.50f),(int)(S*0.10f));
                g2.fillOval((int)(S*0.34f),(int)(S*0.04f),(int)(S*0.14f),(int)(S*0.10f));
                g2.fillOval((int)(S*0.52f),(int)(S*0.04f),(int)(S*0.14f),(int)(S*0.10f));
            }

            // ── CUTLERY (fork left, knife right) ─────────────────────────
            case CUTLERY -> {
                g2.setStroke(new BasicStroke(sw*0.85f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // Fork
                float fx = S*0.28f;
                g2.drawLine((int)fx,(int)(S*0.85f),(int)fx,(int)(S*0.55f));
                // fork tines (3)
                for (int t = -1; t <= 1; t++) {
                    g2.drawLine((int)(fx+t*S*0.06f),(int)(S*0.55f),(int)(fx+t*S*0.06f),(int)(S*0.25f));
                }
                g2.drawArc((int)(fx-S*0.09f),(int)(S*0.50f),(int)(S*0.18f),(int)(S*0.12f),0,180);
                // Knife
                float kx = S*0.66f;
                g2.drawLine((int)kx,(int)(S*0.85f),(int)kx,(int)(S*0.25f));
                Path2D blade = new Path2D.Float();
                blade.moveTo(kx, S*0.25f);
                blade.curveTo(kx+S*0.10f, S*0.30f, kx+S*0.10f, S*0.42f, kx, S*0.48f);
                g2.draw(blade);
            }

            // ── RIDER (scooter + person silhouette) ───────────────────────
            case RIDER -> {
                g2.setStroke(new BasicStroke(sw*0.85f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // wheels
                g2.drawOval((int)(S*0.06f),(int)(S*0.58f),(int)(S*0.26f),(int)(S*0.26f));
                g2.drawOval((int)(S*0.68f),(int)(S*0.58f),(int)(S*0.26f),(int)(S*0.26f));
                // body/frame
                g2.drawLine((int)(S*0.19f),(int)(S*0.71f),(int)(S*0.42f),(int)(S*0.48f));
                g2.drawLine((int)(S*0.42f),(int)(S*0.48f),(int)(S*0.72f),(int)(S*0.48f));
                g2.drawLine((int)(S*0.72f),(int)(S*0.48f),(int)(S*0.81f),(int)(S*0.71f));
                // seat
                g2.drawLine((int)(S*0.38f),(int)(S*0.42f),(int)(S*0.62f),(int)(S*0.42f));
                // handlebar
                g2.drawLine((int)(S*0.72f),(int)(S*0.48f),(int)(S*0.78f),(int)(S*0.38f));
                g2.drawLine((int)(S*0.74f),(int)(S*0.36f),(int)(S*0.84f),(int)(S*0.36f));
                // rider head
                g2.drawOval((int)(S*0.45f),(int)(S*0.10f),(int)(S*0.18f),(int)(S*0.18f));
                // body
                g2.drawLine((int)(S*0.54f),(int)(S*0.28f),(int)(S*0.54f),(int)(S*0.42f));
                // arm to handlebar
                g2.drawLine((int)(S*0.54f),(int)(S*0.36f),(int)(S*0.76f),(int)(S*0.38f));
            }

            // ── CART ─────────────────────────────────────────────────────
            case CART -> {
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // handle bar top-left
                g2.drawLine((int)(S*0.06f),(int)(S*0.14f),(int)(S*0.20f),(int)(S*0.14f));
                // cart body
                Path2D body = new Path2D.Float();
                body.moveTo(S*0.20f, S*0.14f);
                body.lineTo(S*0.32f, S*0.62f);
                body.lineTo(S*0.82f, S*0.62f);
                body.lineTo(S*0.90f, S*0.28f);
                body.lineTo(S*0.28f, S*0.28f);
                g2.draw(body);
                // wheels
                g2.fillOval((int)(S*0.34f),(int)(S*0.68f),(int)(S*0.14f),(int)(S*0.14f));
                g2.fillOval((int)(S*0.68f),(int)(S*0.68f),(int)(S*0.14f),(int)(S*0.14f));
            }

            // ── PHONE ────────────────────────────────────────────────────
            case PHONE -> {
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // phone outline
                RoundRectangle2D phone = new RoundRectangle2D.Float(S*0.22f,S*0.06f,S*0.56f,S*0.88f,S*0.12f,S*0.12f);
                g2.draw(phone);
                // screen area
                g2.drawRect((int)(S*0.28f),(int)(S*0.18f),(int)(S*0.44f),(int)(S*0.54f));
                // home button
                g2.fillOval((int)(S*0.42f),(int)(S*0.78f),(int)(S*0.16f),(int)(S*0.10f));
                // fork+dish on screen (mini delivery icon)
                g2.setStroke(new BasicStroke(sw*0.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine((int)(S*0.38f),(int)(S*0.32f),(int)(S*0.38f),(int)(S*0.52f));
                g2.drawLine((int)(S*0.50f),(int)(S*0.32f),(int)(S*0.50f),(int)(S*0.52f));
                g2.drawLine((int)(S*0.62f),(int)(S*0.32f),(int)(S*0.62f),(int)(S*0.52f));
            }

            // ── STAR ─────────────────────────────────────────────────────
            case STAR -> {
                float cx=S*0.50f, cy=S*0.50f, ro=S*0.44f, ri=S*0.20f;
                Path2D star = new Path2D.Float();
                for (int i=0;i<5;i++) {
                    double ao = Math.toRadians(-90+i*72);
                    double ai = Math.toRadians(-90+i*72+36);
                    if(i==0) star.moveTo(cx+ro*Math.cos(ao), cy+ro*Math.sin(ao));
                    else     star.lineTo(cx+ro*Math.cos(ao), cy+ro*Math.sin(ao));
                    star.lineTo(cx+ri*Math.cos(ai), cy+ri*Math.sin(ai));
                }
                star.closePath();
                g2.fill(star);
            }

            // ── LOCATION PIN ─────────────────────────────────────────────
            case LOCATION -> {
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // pin head
                float px=S*0.50f, py=S*0.38f, pr=S*0.28f;
                g2.drawOval((int)(px-pr),(int)(py-pr),(int)(pr*2),(int)(pr*2));
                g2.fillOval((int)(px-pr*0.35f),(int)(py-pr*0.35f),(int)(pr*0.70f),(int)(pr*0.70f));
                // pin tail
                Path2D tail = new Path2D.Float();
                tail.moveTo(px-pr*0.60f, py+pr*0.70f);
                tail.lineTo(px, S*0.92f);
                tail.lineTo(px+pr*0.60f, py+pr*0.70f);
                g2.draw(tail);
            }

            // ── ORDERS (receipt) ─────────────────────────────────────────
            case ORDERS -> {
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // paper outline with torn bottom
                Path2D paper = new Path2D.Float();
                paper.moveTo(S*0.20f, S*0.08f);
                paper.lineTo(S*0.80f, S*0.08f);
                paper.lineTo(S*0.80f, S*0.84f);
                // torn bottom
                for (int i=0;i<4;i++) {
                    paper.lineTo(S*(0.80f-i*0.15f), S*(i%2==0?0.92f:0.84f));
                }
                paper.lineTo(S*0.20f, S*0.84f);
                paper.closePath();
                g2.draw(paper);
                // lines on receipt
                g2.setStroke(new BasicStroke(sw*0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine((int)(S*0.30f),(int)(S*0.26f),(int)(S*0.70f),(int)(S*0.26f));
                g2.drawLine((int)(S*0.30f),(int)(S*0.38f),(int)(S*0.70f),(int)(S*0.38f));
                g2.drawLine((int)(S*0.30f),(int)(S*0.50f),(int)(S*0.56f),(int)(S*0.50f));
                // price line
                g2.drawLine((int)(S*0.30f),(int)(S*0.66f),(int)(S*0.70f),(int)(S*0.66f));
            }

            // ── HOME ─────────────────────────────────────────────────────
            case HOME -> {
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // roof
                Path2D roof = new Path2D.Float();
                roof.moveTo(S*0.10f, S*0.52f);
                roof.lineTo(S*0.50f, S*0.10f);
                roof.lineTo(S*0.90f, S*0.52f);
                g2.draw(roof);
                // walls
                g2.drawRect((int)(S*0.22f),(int)(S*0.52f),(int)(S*0.56f),(int)(S*0.36f));
                // door
                g2.drawRect((int)(S*0.40f),(int)(S*0.66f),(int)(S*0.20f),(int)(S*0.22f));
            }

            // ── TRASH / DELETE ────────────────────────────────────────────
            case TRASH -> {
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // lid
                g2.drawLine((int)(S*0.14f),(int)(S*0.20f),(int)(S*0.86f),(int)(S*0.20f));
                g2.drawRect((int)(S*0.34f),(int)(S*0.10f),(int)(S*0.32f),(int)(S*0.10f));
                // body
                Path2D body = new Path2D.Float();
                body.moveTo(S*0.22f, S*0.26f);
                body.lineTo(S*0.28f, S*0.90f);
                body.lineTo(S*0.72f, S*0.90f);
                body.lineTo(S*0.78f, S*0.26f);
                g2.draw(body);
                // inner lines
                g2.drawLine((int)(S*0.38f),(int)(S*0.36f),(int)(S*0.38f),(int)(S*0.80f));
                g2.drawLine((int)(S*0.50f),(int)(S*0.36f),(int)(S*0.50f),(int)(S*0.80f));
                g2.drawLine((int)(S*0.62f),(int)(S*0.36f),(int)(S*0.62f),(int)(S*0.80f));
            }

            // ── TRACK / ROUTE ─────────────────────────────────────────────
            case TRACK -> {
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // dashed route line
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{S*0.10f, S*0.07f}, 0f));
                Path2D route = new Path2D.Float();
                route.moveTo(S*0.14f, S*0.70f);
                route.curveTo(S*0.14f, S*0.30f, S*0.86f, S*0.70f, S*0.86f, S*0.30f);
                g2.draw(route);
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // start dot
                g2.fillOval((int)(S*0.06f),(int)(S*0.62f),(int)(S*0.16f),(int)(S*0.16f));
                // end pin
                g2.drawOval((int)(S*0.78f),(int)(S*0.18f),(int)(S*0.16f),(int)(S*0.16f));
                g2.drawLine((int)(S*0.86f),(int)(S*0.34f),(int)(S*0.86f),(int)(S*0.44f));
            }

            // ── GIFT ──────────────────────────────────────────────────────
            case GIFT -> {
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // box
                g2.drawRect((int)(S*0.14f),(int)(S*0.38f),(int)(S*0.72f),(int)(S*0.50f));
                // lid
                g2.drawRect((int)(S*0.10f),(int)(S*0.26f),(int)(S*0.80f),(int)(S*0.14f));
                // ribbon vertical
                g2.drawLine((int)(S*0.50f),(int)(S*0.26f),(int)(S*0.50f),(int)(S*0.88f));
                // ribbon horizontal
                g2.drawLine((int)(S*0.14f),(int)(S*0.44f),(int)(S*0.86f),(int)(S*0.44f));
                // bow loops
                g2.drawArc((int)(S*0.28f),(int)(S*0.08f),(int)(S*0.22f),(int)(S*0.22f),180,270);
                g2.drawArc((int)(S*0.50f),(int)(S*0.08f),(int)(S*0.22f),(int)(S*0.22f),270,270);
            }

            // ── PERSON ────────────────────────────────────────────────────
            case PERSON -> {
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // head
                g2.drawOval((int)(S*0.34f),(int)(S*0.08f),(int)(S*0.32f),(int)(S*0.32f));
                // body
                g2.drawLine((int)(S*0.50f),(int)(S*0.40f),(int)(S*0.50f),(int)(S*0.64f));
                // arms
                g2.drawLine((int)(S*0.24f),(int)(S*0.54f),(int)(S*0.76f),(int)(S*0.54f));
                // legs
                g2.drawLine((int)(S*0.50f),(int)(S*0.64f),(int)(S*0.34f),(int)(S*0.90f));
                g2.drawLine((int)(S*0.50f),(int)(S*0.64f),(int)(S*0.66f),(int)(S*0.90f));
            }

            // ── LOCK ──────────────────────────────────────────────────────
            case LOCK -> {
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // shackle
                g2.drawArc((int)(S*0.26f),(int)(S*0.06f),(int)(S*0.48f),(int)(S*0.52f),0,180);
                // body
                RoundRectangle2D body2 = new RoundRectangle2D.Float(S*0.16f,S*0.44f,S*0.68f,S*0.46f,S*0.10f,S*0.10f);
                g2.draw(body2);
                // keyhole circle
                g2.fillOval((int)(S*0.42f),(int)(S*0.56f),(int)(S*0.16f),(int)(S*0.14f));
                g2.drawLine((int)(S*0.50f),(int)(S*0.70f),(int)(S*0.50f),(int)(S*0.80f));
            }

            // ── MAIL ──────────────────────────────────────────────────────
            case MAIL -> {
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // envelope
                g2.drawRect((int)(S*0.08f),(int)(S*0.22f),(int)(S*0.84f),(int)(S*0.56f));
                // flap V
                g2.drawLine((int)(S*0.08f),(int)(S*0.22f),(int)(S*0.50f),(int)(S*0.56f));
                g2.drawLine((int)(S*0.92f),(int)(S*0.22f),(int)(S*0.50f),(int)(S*0.56f));
            }
        }
    }

    // ── Convenience: icon + text label panel ─────────────────────────────────
    /**
     * Creates a JPanel with icon + text side by side.
     * Good for section headings.
     */
    public static JPanel headingPanel(int iconType, int iconSize, Color iconColor,
                                       String text, Font font, Color textColor) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        p.add(iconLabel(iconType, iconSize, iconColor));
        JLabel lbl = new JLabel(text);
        lbl.setFont(font);
        lbl.setForeground(textColor);
        p.add(lbl);
        return p;
    }
}
