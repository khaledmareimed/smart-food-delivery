package com.fooddelivery.presentation;

import com.fooddelivery.domain.Driver;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.OrderStatus;
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
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/**
 * Manager screen: all orders, status changes and driver assignment.
 */
public class ManagerPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private MainFrame frame;

    private DefaultTableModel ordersModel = new DefaultTableModel(
            new Object[] {"Order", "Customer", "Restaurant", "Status", "Driver", "Items total"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private JTable ordersTable = new JTable(ordersModel);
    private DefaultTableModel driversModel = new DefaultTableModel(
            new Object[] {"Id", "Name", "Location", "Speed km/h", "Status"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private JComboBox<OrderStatus> statusBox = new JComboBox<>(OrderStatus.values());
    private JComboBox<String> modeBox = new JComboBox<>(new String[] {"NEAREST", "FASTEST"});
    private JComboBox<Driver> driverBox = new JComboBox<>();

    public ManagerPanel(MainFrame frame, User user) {
        this.frame = frame;
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.add(new JLabel("  Manager: " + user.getUsername()), BorderLayout.WEST);
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> frame.logout());
        top.add(logout, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Orders", buildOrdersTab());
        tabs.addTab("Drivers", new JScrollPane(new JTable(driversModel)));
        tabs.addChangeListener(e -> refresh());
        add(tabs, BorderLayout.CENTER);
        refresh();
    }

    private JPanel buildOrdersTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(ordersTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(0, 1));
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusRow.setBorder(BorderFactory.createTitledBorder("Status"));
        JButton setStatus = new JButton("Set status");
        setStatus.addActionListener(e -> changeStatus());
        statusRow.add(statusBox);
        statusRow.add(setStatus);
        actions.add(statusRow);

        JPanel driverRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        driverRow.setBorder(BorderFactory.createTitledBorder("Driver"));
        JButton auto = new JButton("Auto assign");
        auto.addActionListener(e -> autoAssign());
        JButton manual = new JButton("Assign selected driver");
        manual.addActionListener(e -> manualAssign());
        driverRow.add(modeBox);
        driverRow.add(auto);
        driverRow.add(new JLabel("   or "));
        driverRow.add(driverBox);
        driverRow.add(manual);
        actions.add(driverRow);

        addExtraActions(actions);

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refresh());
        JPanel refreshRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        refreshRow.add(refresh);
        actions.add(refreshRow);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    void addExtraActions(JPanel actions) {
        // more actions are added in later sprints
    }

    String selectedOrderId() {
        int row = ordersTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an order first");
            return null;
        }
        return (String) ordersModel.getValueAt(row, 0);
    }

    void showError(RuntimeException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void changeStatus() {
        String id = selectedOrderId();
        if (id == null) {
            return;
        }
        try {
            frame.orderService.updateStatus(id, (OrderStatus) statusBox.getSelectedItem());
        } catch (RuntimeException ex) {
            showError(ex);
        }
        refresh();
    }

    private void autoAssign() {
        String id = selectedOrderId();
        if (id == null) {
            return;
        }
        try {
            Order o = frame.driverService.assignBestDriver(id, (String) modeBox.getSelectedItem());
            JOptionPane.showMessageDialog(this, "Assigned " + o.getDriver().getName());
        } catch (RuntimeException ex) {
            showError(ex);
        }
        refresh();
    }

    private void manualAssign() {
        String id = selectedOrderId();
        Driver d = (Driver) driverBox.getSelectedItem();
        if (id == null || d == null) {
            return;
        }
        try {
            frame.driverService.assignDriver(id, d.getId());
        } catch (RuntimeException ex) {
            showError(ex);
        }
        refresh();
    }

    void refresh() {
        int selected = ordersTable.getSelectedRow();
        ordersModel.setRowCount(0);
        for (Order o : frame.orderService.getAllOrders()) {
            ordersModel.addRow(new Object[] {o.getId(), o.getCustomer().getUsername(),
                    o.getRestaurant().getName(), o.getStatus(),
                    o.getDriver() == null ? "-" : o.getDriver().getName(), o.getItemsTotal()});
        }
        if (selected >= 0 && selected < ordersModel.getRowCount()) {
            ordersTable.setRowSelectionInterval(selected, selected);
        }
        driversModel.setRowCount(0);
        driverBox.removeAllItems();
        for (Driver d : frame.driverService.getAllDrivers()) {
            driversModel.addRow(new Object[] {d.getId(), d.getName(), d.getLocation(),
                    d.getSpeedKmh(), d.getStatus()});
            if (d.isAvailable()) {
                driverBox.addItem(d);
            }
        }
    }
}
