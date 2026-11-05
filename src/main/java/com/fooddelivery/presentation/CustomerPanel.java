package com.fooddelivery.presentation;

import com.fooddelivery.domain.DeliveryAddress;
import com.fooddelivery.domain.Location;
import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.domain.User;
import com.fooddelivery.service.OrderLineRequest;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer screen: browse restaurants and menus, build a cart, place and cancel orders.
 */
public class CustomerPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private MainFrame frame;
    private User user;

    private DefaultListModel<Restaurant> restaurantModel = new DefaultListModel<>();
    private JList<Restaurant> restaurantList = new JList<>(restaurantModel);
    private DefaultTableModel menuModel = new DefaultTableModel(
            new Object[] {"Id", "Item", "Price"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private JTable menuTable = new JTable(menuModel);
    private DefaultTableModel cartModel = new DefaultTableModel(
            new Object[] {"Id", "Item", "Qty", "Subtotal"}, 0);
    private JTable cartTable = new JTable(cartModel);
    private JLabel cartTotal = new JLabel("Items total: 0.00");
    private String cartRestaurantId;
    private List<MenuItem> currentMenu = new ArrayList<>();

    private JTextField streetField = new JTextField("Main Street 5", 14);
    private JTextField xField = new JTextField("3", 4);
    private JTextField yField = new JTextField("4", 4);
    private javax.swing.JComboBox<String> pricingBox = new javax.swing.JComboBox<>(
            new String[] {"NORMAL", "EXPRESS", "PROMO"});
    private javax.swing.JComboBox<String> typeBox = new javax.swing.JComboBox<>(
            new String[] {"STANDARD", "EXPRESS", "SCHEDULED"});
    private JTextField scheduleField = new JTextField(12);

    private DefaultTableModel ordersModel = new DefaultTableModel(
            new Object[] {"Order", "Restaurant", "Type", "Status", "Driver", "Items", "Fee", "Discount",
                "Total", "Payment"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private JTable ordersTable = new JTable(ordersModel);
    private DefaultListModel<String> notificationModel = new DefaultListModel<>();

    public CustomerPanel(MainFrame frame, User user) {
        this.frame = frame;
        this.user = user;
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.add(new JLabel("  Welcome, " + user.getUsername()), BorderLayout.WEST);
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> frame.logout());
        top.add(logout, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Order Food", buildOrderTab());
        tabs.addTab("My Orders", buildMyOrdersTab());
        tabs.addTab("Notifications", new JScrollPane(new JList<>(notificationModel)));
        tabs.addChangeListener(e -> {
            refreshOrders();
            refreshNotifications();
        });
        add(tabs, BorderLayout.CENTER);

        loadRestaurants();
    }

    private JPanel buildOrderTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        restaurantList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        restaurantList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadMenu();
            }
        });
        JScrollPane restaurantScroll = new JScrollPane(restaurantList);
        restaurantScroll.setBorder(BorderFactory.createTitledBorder("Open restaurants"));
        restaurantScroll.setPreferredSize(new java.awt.Dimension(220, 0));
        panel.add(restaurantScroll, BorderLayout.WEST);

        JPanel center = new JPanel(new GridLayout(2, 1, 8, 8));
        JPanel menuPanel = new JPanel(new BorderLayout());
        menuPanel.setBorder(BorderFactory.createTitledBorder("Menu"));
        menuPanel.add(new JScrollPane(menuTable), BorderLayout.CENTER);
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JSpinner qty = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));
        JButton addButton = new JButton("Add to cart");
        addButton.addActionListener(e -> addToCart((Integer) qty.getValue()));
        addRow.add(new JLabel("Qty:"));
        addRow.add(qty);
        addRow.add(addButton);
        menuPanel.add(addRow, BorderLayout.SOUTH);
        center.add(menuPanel);

        JPanel cartPanel = new JPanel(new BorderLayout());
        cartPanel.setBorder(BorderFactory.createTitledBorder("Cart"));
        cartPanel.add(new JScrollPane(cartTable), BorderLayout.CENTER);
        JPanel cartButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton remove = new JButton("Remove selected");
        remove.addActionListener(e -> {
            int row = cartTable.getSelectedRow();
            if (row >= 0) {
                cartModel.removeRow(row);
                updateCartTotal();
            }
        });
        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> clearCart());
        cartButtons.add(remove);
        cartButtons.add(clear);
        cartButtons.add(cartTotal);
        cartPanel.add(cartButtons, BorderLayout.SOUTH);
        center.add(cartPanel);
        panel.add(center, BorderLayout.CENTER);

        panel.add(buildCheckoutPanel(), BorderLayout.SOUTH);
        return panel;
    }

    JPanel buildCheckoutPanel() {
        JPanel checkout = new JPanel(new FlowLayout(FlowLayout.LEFT));
        checkout.setBorder(BorderFactory.createTitledBorder("Delivery"));
        checkout.add(new JLabel("Street:"));
        checkout.add(streetField);
        checkout.add(new JLabel("X:"));
        checkout.add(xField);
        checkout.add(new JLabel("Y:"));
        checkout.add(yField);
        checkout.add(new JLabel("Pricing:"));
        checkout.add(pricingBox);
        checkout.add(new JLabel("Type:"));
        checkout.add(typeBox);
        checkout.add(new JLabel("Deliver at (yyyy-MM-dd HH:mm):"));
        scheduleField.setEnabled(false);
        typeBox.addActionListener(e -> scheduleField.setEnabled("SCHEDULED".equals(typeBox.getSelectedItem())));
        checkout.add(scheduleField);
        JButton place = new JButton("Place order");
        place.addActionListener(e -> placeOrder());
        checkout.add(place);
        return checkout;
    }

    private JPanel buildMyOrdersTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(ordersTable), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshOrders());
        JButton cancel = new JButton("Cancel selected order");
        cancel.addActionListener(e -> cancelSelected());
        buttons.add(refresh);
        buttons.add(cancel);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void loadRestaurants() {
        restaurantModel.clear();
        for (Restaurant r : frame.restaurantService.getOpenRestaurants()) {
            restaurantModel.addElement(r);
        }
        restaurantList.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                Restaurant r = (Restaurant) value;
                return super.getListCellRendererComponent(list, r.getName() + "  (min "
                        + r.getMinimumOrderValue() + ")", index, selected, focus);
            }
        });
    }

    private void loadMenu() {
        menuModel.setRowCount(0);
        Restaurant r = restaurantList.getSelectedValue();
        if (r == null) {
            return;
        }
        currentMenu = frame.restaurantService.getAvailableMenu(r.getId());
        for (MenuItem m : currentMenu) {
            menuModel.addRow(new Object[] {m.getId(), m.getName(), m.getPrice()});
        }
    }

    private void addToCart(int qty) {
        Restaurant r = restaurantList.getSelectedValue();
        int row = menuTable.getSelectedRow();
        if (r == null || row < 0) {
            JOptionPane.showMessageDialog(this, "Select a restaurant and a menu item first");
            return;
        }
        if (cartRestaurantId != null && !cartRestaurantId.equals(r.getId())
                && cartModel.getRowCount() > 0) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "Your cart has items from another restaurant. Clear it?");
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
            clearCart();
        }
        cartRestaurantId = r.getId();
        MenuItem item = currentMenu.get(row);
        BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(qty));
        cartModel.addRow(new Object[] {item.getId(), item.getName(), qty, subtotal});
        updateCartTotal();
    }

    private void clearCart() {
        cartModel.setRowCount(0);
        cartRestaurantId = null;
        updateCartTotal();
    }

    private void updateCartTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            total = total.add((BigDecimal) cartModel.getValueAt(i, 3));
        }
        cartTotal.setText("Items total: " + total.setScale(2, BigDecimal.ROUND_HALF_UP));
    }

    List<OrderLineRequest> cartLines() {
        List<OrderLineRequest> lines = new ArrayList<>();
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            lines.add(new OrderLineRequest((String) cartModel.getValueAt(i, 0),
                    (Integer) cartModel.getValueAt(i, 2)));
        }
        return lines;
    }

    DeliveryAddress readAddress() {
        return new DeliveryAddress(streetField.getText(), new Location(
                Double.parseDouble(xField.getText().trim()),
                Double.parseDouble(yField.getText().trim())));
    }

    java.time.LocalDateTime readScheduledTime() {
        if (!"SCHEDULED".equals(typeBox.getSelectedItem()) || scheduleField.getText().trim().isEmpty()) {
            return null;
        }
        return java.time.LocalDateTime.parse(scheduleField.getText().trim(),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    void placeOrder() {
        try {
            Order order = frame.orderService.placeOrder(user, cartRestaurantId, cartLines(),
                    readAddress(), (String) pricingBox.getSelectedItem(),
                    (String) typeBox.getSelectedItem(), readScheduledTime());
            JOptionPane.showMessageDialog(this, "Order " + order.getId() + " placed. Status: "
                    + order.getStatus()
                    + "\n\nItems total:   " + order.getItemsTotal()
                    + "\nDelivery fee:  " + order.getDeliveryFee()
                    + "\nDiscount:      -" + order.getDiscount()
                    + "\nPricing:       " + order.getPricingType()
                    + "\nOrder type:    " + order.getOrderType()
                    + (order.getScheduledTime() == null ? "" : " at " + order.getScheduledTime())
                    + "\n-----------------------"
                    + "\nTOTAL:         " + order.getTotal()
                    + "\n\nPay cash to the driver on delivery.");
            clearCart();
            refreshOrders();
        } catch (java.time.format.DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Delivery time must look like 2026-11-05 19:30",
                    "Error", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "X and Y must be numbers", "Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Order rejected",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelSelected() {
        int row = ordersTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an order first");
            return;
        }
        String id = (String) ordersModel.getValueAt(row, 0);
        try {
            frame.orderService.cancelOrder(id);
            refreshOrders();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot cancel",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    void refreshOrders() {
        ordersModel.setRowCount(0);
        for (Order o : frame.orderService.getOrdersFor(user)) {
            ordersModel.addRow(new Object[] {o.getId(), o.getRestaurant().getName(), o.getOrderType(),
                    o.getStatus(),
                    o.getDriver() == null ? "-" : o.getDriver().getName(), o.getItemsTotal(),
                    o.getDeliveryFee(), o.getDiscount(), o.getTotal(), o.getPaymentStatus()});
        }
    }

    private void refreshNotifications() {
        notificationModel.clear();
        for (String n : frame.notificationService.getNotifications(user.getId())) {
            notificationModel.addElement(n);
        }
    }
}
