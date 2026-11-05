package com.fooddelivery.presentation;

import com.fooddelivery.domain.OrderStatus;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Draws the order lifecycle as a row of dots so the customer can track the order.
 */
class StatusStepper extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final OrderStatus[] STEPS = {OrderStatus.PENDING, OrderStatus.CONFIRMED,
        OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED};

    private OrderStatus status;

    StatusStepper() {
        setOpaque(false);
        setPreferredSize(new Dimension(520, 64));
    }

    void setStatus(OrderStatus status) {
        this.status = status;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (status == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (status == OrderStatus.CANCELLED) {
            g2.setColor(Theme.RED);
            g2.setFont(Theme.SUBTITLE);
            g2.drawString("This order was cancelled", 10, 36);
            g2.dispose();
            return;
        }
        int current = 0;
        for (int i = 0; i < STEPS.length; i++) {
            if (STEPS[i] == status) {
                current = i;
            }
        }
        int margin = 40;
        int step = (getWidth() - 2 * margin) / (STEPS.length - 1);
        int y = 20;
        g2.setStroke(new BasicStroke(4));
        for (int i = 0; i < STEPS.length - 1; i++) {
            g2.setColor(i < current ? Theme.GREEN : Theme.BORDER);
            g2.drawLine(margin + i * step, y, margin + (i + 1) * step, y);
        }
        g2.setFont(Theme.SMALL);
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < STEPS.length; i++) {
            int x = margin + i * step;
            if (i < current) {
                g2.setColor(Theme.GREEN);
            } else if (i == current) {
                g2.setColor(Theme.PRIMARY);
            } else {
                g2.setColor(Theme.BORDER);
            }
            g2.fillOval(x - 9, y - 9, 18, 18);
            String label = Theme.nice(STEPS[i]);
            g2.setColor(i == current ? Theme.TEXT : Theme.MUTED);
            g2.setFont(i == current ? Theme.SMALL.deriveFont(Font.BOLD) : Theme.SMALL);
            g2.drawString(label, x - fm.stringWidth(label) / 2, y + 30);
        }
        g2.dispose();
    }
}
