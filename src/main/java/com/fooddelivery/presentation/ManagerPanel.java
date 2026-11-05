package com.fooddelivery.presentation;

import com.fooddelivery.domain.Driver;
import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.OrderStatus;
import com.fooddelivery.domain.Payment;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.domain.User;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.List;

/**
 * Manager screen: dashboard, orders (status, drivers, payment), drivers, restaurants.
 */
public class ManagerPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private MainFrame frame;

    private JLabel totalOrders = new JLabel("0");
    private JLabel pendingOrders = new JLabel("0");
    private JLabel activeOrders = new JLabel("0");
    private JLabel deliveredOrders = new JLabel("0");
    private JLabel collected = new JLabel("0");

    private DefaultTableModel ordersModel = CustomerPanel.readOnlyModel("Order", "Customer",
            "Restaurant", "Type", "Items", "Status", "Driver", "Total", "Payment");
    private JTable ordersTable = new JTable(ordersModel);
    private JLabel orderInfo = new JLabel();
    private JButton advanceButton = Theme.primary("Next step");
    private JButton cancelButton = Theme.danger("Cancel order");
    private JComboBox<String> modeBox = new JComboBox<>(new String[] {"NEAREST", "FASTEST"});
    private JButton autoButton = Theme.secondary("Auto assign");
    private JComboBox<Driver> driverBox = new JComboBox<>();
    private JButton assignButton = Theme.secondary("Assign");
    private JButton payButton = Theme.secondary("Take cash payment");

    private DefaultTableModel driversModel = CustomerPanel.readOnlyModel("Id", "Name", "Vehicle",
            "Max distance", "Location", "Speed km/h", "Status", "Current order");
    private DefaultTableModel restaurantsModel = CustomerPanel.readOnlyModel("Id", "Restaurant",
            "Open", "Min order", "Radius km");
    private JTable restaurantsTable = new JTable(restaurantsModel);
    private DefaultTableModel menuModel = CustomerPanel.readOnlyModel("Id", "Item", "Price", "Available");
    private JTable menuTable = new JTable(menuModel);

    public ManagerPanel(MainFrame frame, User user) {
        this.frame = frame;
        setLayout(new BorderLayout());
        add(Theme.header("Manager", "Signed in as " + user.getUsername()
                + "  -  run the kitchen, the drivers and the money", frame::logout), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Orders", buildOrdersTab());
        tabs.addTab("Drivers", buildDriversTab());
        tabs.addTab("Restaurants", buildRestaurantsTab());
        tabs.addChangeListener(e -> refresh());
        add(tabs, BorderLayout.CENTER);
        refresh();
    }

    // ---------------- orders ----------------

    private JPanel buildOrdersTab() {
        JPanel page = Theme.page();
        JPanel stats = new JPanel(new GridLayout(1, 5, 12, 0));
        stats.setOpaque(false);
        stats.add(Theme.statCard("Orders", totalOrders, Theme.BLUE));
        stats.add(Theme.statCard("Pending", pendingOrders, Theme.AMBER));
        stats.add(Theme.statCard("In progress", activeOrders, Theme.PURPLE));
        stats.add(Theme.statCard("Delivered", deliveredOrders, Theme.GREEN));
        stats.add(Theme.statCard("Collected", collected, Theme.PRIMARY));
        page.add(stats, BorderLayout.NORTH);

        JPanel list = Theme.card("All orders");
        Theme.styleTable(ordersTable);
        ordersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ordersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelected();
            }
        });
        list.add(new JScrollPane(ordersTable), BorderLayout.CENTER);
        JButton refresh = Theme.secondary("Refresh");
        refresh.addActionListener(e -> refresh());
        list.add(Theme.row(refresh), BorderLayout.SOUTH);
        page.add(list, BorderLayout.CENTER);

        JPanel side = Theme.card("Selected order");
        side.setPreferredSize(new Dimension(420, 0));
        side.add(Theme.htmlScroll(orderInfo), BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(0, 1, 0, 8));
        actions.setOpaque(false);
        actions.add(section("Status"));
        actions.add(Theme.row(advanceButton, cancelButton));
        actions.add(section("Driver"));
        actions.add(Theme.row(modeBox, autoButton));
        driverBox.setPreferredSize(new Dimension(200, 32));
        actions.add(Theme.row(driverBox, assignButton));
        actions.add(section("Payment"));
        actions.add(Theme.row(payButton));
        side.add(actions, BorderLayout.SOUTH);
        page.add(side, BorderLayout.EAST);

        advanceButton.addActionListener(e -> advance());
        cancelButton.addActionListener(e -> cancel());
        autoButton.addActionListener(e -> autoAssign());
        assignButton.addActionListener(e -> manualAssign());
        payButton.addActionListener(e -> takePayment());
        return page;
    }

    private static JLabel section(String text) {
        JLabel l = Theme.muted(text.toUpperCase());
        l.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        return l;
    }

    String selectedOrderId() {
        int row = ordersTable.getSelectedRow();
        return row < 0 ? null : (String) ordersModel.getValueAt(row, 0);
    }

    private void showSelected() {
        String id = selectedOrderId();
        boolean has = id != null;
        advanceButton.setEnabled(false);
        cancelButton.setEnabled(false);
        autoButton.setEnabled(false);
        assignButton.setEnabled(false);
        payButton.setEnabled(false);
        driverBox.removeAllItems();
        if (!has) {
            orderInfo.setText("<html><span style='color:" + Theme.hex(Theme.MUTED)
                    + "'>Select an order on the left.</span></html>");
            advanceButton.setText("Next step");
            return;
        }
        Order o = frame.orderService.getOrder(id);
        String driver = o.getDriver() == null ? "<i>none yet</i>"
                : o.getDriver().getName() + " (" + Theme.nice(o.getDriver().getType()) + ")";
        String color = Theme.hex(Theme.statusColor(o.getStatus()));
        orderInfo.setText(CustomerPanel.billHtml(o).replace("<h2>Order " + o.getId() + "</h2>",
                "<h2>Order " + o.getId() + "</h2><b style='color:" + color + "'>"
                        + Theme.nice(o.getStatus()) + "</b><br>Customer: " + o.getCustomer().getUsername()
                        + "<br>Restaurant: " + o.getRestaurant().getName() + "<br>Deliver to: "
                        + o.getDeliveryAddress() + "<br>Driver: " + driver + "<br><br>"));

        OrderStatus next = frame.orderService.nextStatus(o.getStatus());
        advanceButton.setText(next == null ? "Next step" : "Move to " + Theme.nice(next));
        advanceButton.setEnabled(next != null);
        cancelButton.setEnabled(o.getStatus().isCancellable());
        boolean canAssign = o.getDriver() == null && CustomerPanel.isActive(o.getStatus());
        autoButton.setEnabled(canAssign);
        if (canAssign) {
            for (Driver d : frame.driverService.getAvailableDrivers()) {
                if (d.canDeliver(o)) {
                    driverBox.addItem(d);
                }
            }
        }
        assignButton.setEnabled(canAssign && driverBox.getItemCount() > 0);
        payButton.setEnabled("UNPAID".equals(o.getPaymentStatus())
                && o.getStatus() != OrderStatus.CANCELLED);
    }

    private void advance() {
        String id = selectedOrderId();
        try {
            frame.orderService.advanceStatus(id);
        } catch (RuntimeException ex) {
            showError(ex);
        }
        refresh();
    }

    private void cancel() {
        String id = selectedOrderId();
        if (JOptionPane.showConfirmDialog(this, "Cancel order " + id + "?", "Cancel",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            frame.orderService.cancelOrder(id);
        } catch (RuntimeException ex) {
            showError(ex);
        }
        refresh();
    }

    private void autoAssign() {
        String id = selectedOrderId();
        try {
            Order o = frame.driverService.assignBestDriver(id, (String) modeBox.getSelectedItem());
            JOptionPane.showMessageDialog(this, o.getDriver().getName() + " ("
                    + Theme.nice(o.getDriver().getType()) + ") will deliver " + id);
        } catch (RuntimeException ex) {
            showError(ex);
        }
        refresh();
    }

    private void manualAssign() {
        String id = selectedOrderId();
        Driver d = (Driver) driverBox.getSelectedItem();
        if (d == null) {
            return;
        }
        try {
            frame.driverService.assignDriver(id, d.getId());
        } catch (RuntimeException ex) {
            showError(ex);
        }
        refresh();
    }

    private void takePayment() {
        String id = selectedOrderId();
        Order order = frame.orderService.getOrder(id);
        String input = JOptionPane.showInputDialog(this, "Total is " + Theme.money(order.getTotal())
                + "\nCash received:", order.getTotal().toPlainString());
        if (input == null) {
            return;
        }
        try {
            Payment p = frame.paymentService.payCash(id, new BigDecimal(input.trim()));
            JOptionPane.showMessageDialog(this, "Paid. Change: " + Theme.money(p.getChange()));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Not a valid amount", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException ex) {
            showError(ex);
        }
        refresh();
    }

    void showError(RuntimeException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Not allowed", JOptionPane.ERROR_MESSAGE);
    }

    // ---------------- drivers ----------------

    private JPanel buildDriversTab() {
        JPanel page = Theme.page();
        JPanel card = Theme.card("Drivers");
        JTable table = new JTable(driversModel);
        Theme.styleTable(table);
        card.add(new JScrollPane(table));
        card.add(Theme.muted("Bicycle: up to 3 km  |  Motorcycle: up to 10 km  |  Car: any distance, "
                + "needed for 10+ items  |  Electric: battery must cover the whole trip"), BorderLayout.SOUTH);
        page.add(card);
        return page;
    }

    // ---------------- restaurants ----------------

    private JPanel buildRestaurantsTab() {
        JPanel page = Theme.page();
        JPanel left = Theme.card("Restaurants");
        Theme.styleTable(restaurantsTable);
        restaurantsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        restaurantsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadMenu();
            }
        });
        left.add(new JScrollPane(restaurantsTable));
        JButton toggleOpen = Theme.secondary("Open / close");
        toggleOpen.addActionListener(e -> {
            int row = restaurantsTable.getSelectedRow();
            if (row < 0) {
                return;
            }
            frame.restaurantService.setRestaurantOpen((String) restaurantsModel.getValueAt(row, 0),
                    !"Yes".equals(restaurantsModel.getValueAt(row, 2)));
            refreshRestaurants();
        });
        left.add(Theme.row(toggleOpen), BorderLayout.SOUTH);

        JPanel right = Theme.card("Menu");
        Theme.styleTable(menuTable);
        right.add(new JScrollPane(menuTable));
        JButton toggleItem = Theme.secondary("Available / sold out");
        toggleItem.addActionListener(e -> {
            int r = restaurantsTable.getSelectedRow();
            int m = menuTable.getSelectedRow();
            if (r < 0 || m < 0) {
                return;
            }
            frame.restaurantService.setMenuItemAvailable((String) restaurantsModel.getValueAt(r, 0),
                    (String) menuModel.getValueAt(m, 0), !"Yes".equals(menuModel.getValueAt(m, 3)));
            loadMenu();
            menuTable.setRowSelectionInterval(m, m);
        });
        right.add(Theme.row(toggleItem), BorderLayout.SOUTH);

        JPanel split = new JPanel(new GridLayout(1, 2, 14, 0));
        split.setOpaque(false);
        split.add(left);
        split.add(right);
        page.add(split);
        return page;
    }

    private void refreshRestaurants() {
        int selected = restaurantsTable.getSelectedRow();
        restaurantsModel.setRowCount(0);
        for (Restaurant r : frame.restaurantService.getAllRestaurants()) {
            restaurantsModel.addRow(new Object[] {r.getId(), r.getName(), r.isOpen() ? "Yes" : "No",
                r.getMinimumOrderValue(), r.getDeliveryRadiusKm()});
        }
        if (selected >= 0 && selected < restaurantsModel.getRowCount()) {
            restaurantsTable.setRowSelectionInterval(selected, selected);
        }
    }

    private void loadMenu() {
        menuModel.setRowCount(0);
        int row = restaurantsTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        for (Restaurant r : frame.restaurantService.getAllRestaurants()) {
            if (r.getId().equals(restaurantsModel.getValueAt(row, 0))) {
                for (MenuItem m : r.getMenu()) {
                    menuModel.addRow(new Object[] {m.getId(), m.getName(), m.getPrice(),
                        m.isAvailable() ? "Yes" : "No"});
                }
            }
        }
    }

    // ---------------- refresh ----------------

    void refresh() {
        String selected = selectedOrderId();
        List<Order> orders = frame.orderService.getAllOrders();
        ordersModel.setRowCount(0);
        int pending = 0;
        int active = 0;
        int delivered = 0;
        BigDecimal money = BigDecimal.ZERO;
        for (int i = orders.size() - 1; i >= 0; i--) {
            Order o = orders.get(i);
            ordersModel.addRow(new Object[] {o.getId(), o.getCustomer().getUsername(),
                o.getRestaurant().getName(), Theme.nice(o.getOrderType()), o.getItemCount(),
                o.getStatus(), o.getDriver() == null ? "-" : o.getDriver().getName(), o.getTotal(),
                o.getPaymentStatus()});
            if (o.getStatus() == OrderStatus.PENDING) {
                pending++;
            } else if (o.getStatus() == OrderStatus.DELIVERED) {
                delivered++;
            } else if (o.getStatus() != OrderStatus.CANCELLED) {
                active++;
            }
            if ("PAID".equals(o.getPaymentStatus())) {
                money = money.add(o.getTotal());
            }
        }
        totalOrders.setText(String.valueOf(orders.size()));
        pendingOrders.setText(String.valueOf(pending));
        activeOrders.setText(String.valueOf(active));
        deliveredOrders.setText(String.valueOf(delivered));
        collected.setText(Theme.money(money));
        for (int i = 0; i < ordersModel.getRowCount(); i++) {
            if (ordersModel.getValueAt(i, 0).equals(selected)) {
                ordersTable.setRowSelectionInterval(i, i);
            }
        }
        showSelected();

        driversModel.setRowCount(0);
        for (Driver d : frame.driverService.getAllDrivers()) {
            Order current = frame.driverService.getCurrentOrder(d.getId());
            driversModel.addRow(new Object[] {d.getId(), d.getName(), Theme.nice(d.getType()),
                d.getMaxDistanceKm() == 0 ? "No limit" : d.getMaxDistanceKm() + " km", d.getLocation(),
                d.getSpeedKmh(), d.getStatus(), current == null ? "-" : current.getId()});
        }
        refreshRestaurants();
    }
}
