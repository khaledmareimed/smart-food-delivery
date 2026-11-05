package com.fooddelivery.presentation;

import com.fooddelivery.domain.Driver;
import com.fooddelivery.domain.DriverStatus;
import com.fooddelivery.domain.ElectricDriver;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.OrderItem;
import com.fooddelivery.domain.OrderStatus;
import com.fooddelivery.domain.Payment;
import com.fooddelivery.domain.User;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.List;

/**
 * Driver screen: go online/offline, see the current delivery, pick it up,
 * deliver it, collect the cash, and see past deliveries.
 */
public class DriverPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private MainFrame frame;
    private String driverId;

    private JLabel statusLabel = new JLabel();
    private JButton onlineButton = Theme.secondary("Go offline");
    private JLabel currentInfo = new JLabel();
    private StatusStepper stepper = new StatusStepper();
    private JButton pickUpButton = Theme.primary("Picked up");
    private JButton deliveredButton = Theme.primary("Delivered");
    private JButton cashButton = Theme.secondary("Collect cash");
    private DefaultTableModel historyModel = CustomerPanel.readOnlyModel("Order", "Restaurant",
            "Customer", "Deliver to", "Status", "Total", "Payment");
    private DefaultListModel<String> notificationModel = new DefaultListModel<>();
    private JLabel deliveredCount = new JLabel("0");
    private JLabel cashCollected = new JLabel("0");
    private Timer timer;
    private String currentOrderId;

    public DriverPanel(MainFrame frame, User user) {
        this.frame = frame;
        this.driverId = user.getId();
        Driver me = frame.driverService.getDriver(driverId);
        setLayout(new BorderLayout());
        add(Theme.header("Driver", me.getName() + "  -  " + Theme.nice(me.getType()) + " driver",
                frame::logout), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Current delivery", buildCurrentTab());
        tabs.addTab("History", buildHistoryTab());
        tabs.addTab("Notifications", buildNotificationsTab());
        tabs.addChangeListener(e -> refresh());
        add(tabs, BorderLayout.CENTER);

        onlineButton.addActionListener(e -> toggleOnline());
        pickUpButton.addActionListener(e -> changeStatus(OrderStatus.OUT_FOR_DELIVERY));
        deliveredButton.addActionListener(e -> changeStatus(OrderStatus.DELIVERED));
        cashButton.addActionListener(e -> collectCash());

        refresh();
        // new orders can be assigned by the manager at any time
        timer = new Timer(4000, e -> refresh());
        timer.start();
    }

    void stopTimer() {
        timer.stop();
    }

    private JPanel buildCurrentTab() {
        JPanel page = Theme.page();

        JPanel top = new JPanel(new GridLayout(1, 3, 12, 0));
        top.setOpaque(false);
        JPanel availability = Theme.card("Availability");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        availability.add(statusLabel, BorderLayout.CENTER);
        availability.add(onlineButton, BorderLayout.EAST);
        top.add(availability);
        top.add(Theme.statCard("Delivered", deliveredCount, Theme.GREEN));
        top.add(Theme.statCard("Cash collected", cashCollected, Theme.PRIMARY));
        page.add(top, BorderLayout.NORTH);

        JPanel card = Theme.card("Your delivery");
        card.add(Theme.below(stepper, Theme.htmlScroll(currentInfo)), BorderLayout.CENTER);
        Theme.bigger(pickUpButton);
        Theme.bigger(deliveredButton);
        Theme.bigger(cashButton);
        card.add(Theme.row(pickUpButton, deliveredButton, cashButton), BorderLayout.SOUTH);
        page.add(card, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildHistoryTab() {
        JPanel page = Theme.page();
        JPanel card = Theme.card("All my deliveries");
        JTable table = new JTable(historyModel);
        Theme.styleTable(table);
        card.add(new JScrollPane(table));
        page.add(card);
        return page;
    }

    private JPanel buildNotificationsTab() {
        JPanel page = Theme.page();
        JPanel card = Theme.card("Notifications");
        JList<String> list = new JList<>(notificationModel);
        list.setFixedCellHeight(36);
        card.add(new JScrollPane(list));
        page.add(card);
        return page;
    }

    void refresh() {
        Driver me = frame.driverService.getDriver(driverId);
        Order current = frame.driverService.getCurrentOrder(driverId);
        currentOrderId = current == null ? null : current.getId();

        if (current != null) {
            statusLabel.setText("On a delivery");
            statusLabel.setForeground(Theme.PRIMARY);
            onlineButton.setEnabled(false);
        } else if (me.getStatus() == DriverStatus.AVAILABLE) {
            statusLabel.setText("Online - waiting for orders");
            statusLabel.setForeground(Theme.GREEN);
            onlineButton.setText("Go offline");
            onlineButton.setEnabled(true);
        } else {
            statusLabel.setText("Offline");
            statusLabel.setForeground(Theme.MUTED);
            onlineButton.setText("Go online");
            onlineButton.setEnabled(true);
        }

        if (current == null) {
            stepper.setStatus(null);
            currentInfo.setText("<html><div style='padding:20px'><h2 style='color:"
                    + Theme.hex(Theme.MUTED) + "'>No active delivery</h2>"
                    + "Stay online and the manager will send you the next order.<br>"
                    + "Your vehicle: " + Theme.nice(me.getType()) + range(me) + "</div></html>");
            pickUpButton.setEnabled(false);
            deliveredButton.setEnabled(false);
            cashButton.setEnabled(false);
        } else {
            stepper.setStatus(current.getStatus());
            currentInfo.setText(deliveryHtml(me, current));
            pickUpButton.setEnabled(current.getStatus() == OrderStatus.READY);
            pickUpButton.setToolTipText(current.getStatus() == OrderStatus.READY ? null
                    : "Wait until the restaurant marks the order as ready");
            deliveredButton.setEnabled(current.getStatus() == OrderStatus.OUT_FOR_DELIVERY);
            cashButton.setEnabled(current.getStatus() == OrderStatus.OUT_FOR_DELIVERY
                    && "UNPAID".equals(current.getPaymentStatus()));
        }

        List<Order> all = frame.driverService.getOrdersForDriver(driverId);
        historyModel.setRowCount(0);
        int delivered = 0;
        BigDecimal cash = BigDecimal.ZERO;
        for (int i = all.size() - 1; i >= 0; i--) {
            Order o = all.get(i);
            historyModel.addRow(new Object[] {o.getId(), o.getRestaurant().getName(),
                o.getCustomer().getUsername(), o.getDeliveryAddress().getStreet(), o.getStatus(),
                o.getTotal(), o.getPaymentStatus()});
            if (o.getStatus() == OrderStatus.DELIVERED) {
                delivered++;
            }
            if ("PAID".equals(o.getPaymentStatus())) {
                cash = cash.add(o.getTotal());
            }
        }
        deliveredCount.setText(String.valueOf(delivered));
        cashCollected.setText(Theme.money(cash));

        notificationModel.clear();
        List<String> notes = frame.notificationService.getNotifications(driverId);
        for (int i = notes.size() - 1; i >= 0; i--) {
            notificationModel.addElement(notes.get(i));
        }
    }

    private static String range(Driver d) {
        if (d instanceof ElectricDriver) {
            return " (battery range " + ((ElectricDriver) d).getBatteryRangeKm() + " km)";
        }
        return d.getMaxDistanceKm() == 0 ? " (any distance)" : " (up to " + d.getMaxDistanceKm() + " km)";
    }

    private static String deliveryHtml(Driver me, Order o) {
        double toRestaurant = me.getLocation().distanceTo(o.getRestaurant().getLocation());
        double toCustomer = o.getRestaurant().getLocation().distanceTo(o.getDeliveryAddress().getLocation());
        StringBuilder items = new StringBuilder();
        for (OrderItem i : o.getItems()) {
            items.append(i.getQuantity()).append(" x ").append(i.getName()).append("<br>");
        }
        String step;
        if (o.getStatus() == OrderStatus.READY) {
            step = "The food is ready. Go to the restaurant and press <b>Picked up</b>.";
        } else if (o.getStatus() == OrderStatus.OUT_FOR_DELIVERY) {
            step = "Take the food to the customer, collect the cash and press <b>Delivered</b>.";
        } else {
            step = "The restaurant is still working on it (" + Theme.nice(o.getStatus()) + ").";
        }
        return "<html><div style='width:700px'>"
                + "<h2>Order " + o.getId() + "</h2>"
                + "<p style='color:" + Theme.hex(Theme.PRIMARY_DARK) + "'>" + step + "</p><br>"
                + "<table cellpadding='4'>"
                + "<tr><td><b>Pick up</b></td><td>" + o.getRestaurant().getName() + " at "
                + o.getRestaurant().getLocation() + String.format(" - %.1f km from you", toRestaurant)
                + "</td></tr>"
                + "<tr><td><b>Deliver to</b></td><td>" + o.getCustomer().getUsername() + ", "
                + o.getDeliveryAddress() + String.format(" - %.1f km trip", toCustomer) + "</td></tr>"
                + "<tr><td valign='top'><b>Items</b></td><td>" + items + "</td></tr>"
                + "<tr><td><b>Collect</b></td><td><b>" + Theme.money(o.getTotal()) + "</b> cash ("
                + Theme.nice(o.getPaymentStatus()) + ")</td></tr>"
                + (o.getScheduledTime() == null ? "" : "<tr><td><b>Deliver at</b></td><td>"
                + o.getScheduledTime().format(CustomerPanel.TIME) + "</td></tr>")
                + "</table></div></html>";
    }

    private void toggleOnline() {
        Driver me = frame.driverService.getDriver(driverId);
        try {
            frame.driverService.setAvailable(driverId, me.getStatus() != DriverStatus.AVAILABLE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Not allowed", JOptionPane.ERROR_MESSAGE);
        }
        refresh();
    }

    private void changeStatus(OrderStatus status) {
        if (currentOrderId == null) {
            return;
        }
        try {
            Order o = frame.orderService.getOrder(currentOrderId);
            if (status == OrderStatus.DELIVERED && "UNPAID".equals(o.getPaymentStatus())) {
                int answer = JOptionPane.showConfirmDialog(this,
                        "The order is not paid yet. Mark it delivered anyway?", "Not paid",
                        JOptionPane.YES_NO_OPTION);
                if (answer != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            frame.orderService.updateStatusByDriver(driverId, currentOrderId, status);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Not allowed", JOptionPane.ERROR_MESSAGE);
        }
        refresh();
    }

    private void collectCash() {
        if (currentOrderId == null) {
            return;
        }
        Order o = frame.orderService.getOrder(currentOrderId);
        String input = JOptionPane.showInputDialog(this, "Total is " + Theme.money(o.getTotal())
                + "\nCash received from the customer:", o.getTotal().toPlainString());
        if (input == null) {
            return;
        }
        try {
            Payment p = frame.paymentService.payCash(currentOrderId, new BigDecimal(input.trim()));
            JOptionPane.showMessageDialog(this, "Payment recorded. Give back "
                    + Theme.money(p.getChange()) + " change.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Not a valid amount", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Payment rejected",
                    JOptionPane.ERROR_MESSAGE);
        }
        refresh();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(1200, 760);
    }

}
