package com.fooddelivery.presentation;

import com.fooddelivery.domain.DeliveryAddress;
import com.fooddelivery.domain.Location;
import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.OrderItem;
import com.fooddelivery.domain.OrderStatus;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.domain.User;
import com.fooddelivery.service.OrderLineRequest;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Customer screen: browse restaurants and menus, build a cart, place orders,
 * track and cancel them, read notifications.
 */
public class CustomerPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private MainFrame frame;
    private User user;

    private DefaultListModel<Restaurant> restaurantModel = new DefaultListModel<>();
    private JList<Restaurant> restaurantList = new JList<>(restaurantModel);
    private JLabel menuTitle = new JLabel("Pick a restaurant");
    private DefaultTableModel menuModel = readOnlyModel("Item", "Price");
    private JTable menuTable = new JTable(menuModel);
    private List<MenuItem> currentMenu = new ArrayList<>();

    // cart: menu item id -> quantity, plus the items themselves
    private Map<String, Integer> cart = new LinkedHashMap<>();
    private Map<String, MenuItem> cartItems = new LinkedHashMap<>();
    private String cartRestaurantId;
    private DefaultTableModel cartModel = readOnlyModel("Item", "Qty", "Subtotal");
    private JTable cartTable = new JTable(cartModel);
    private JLabel cartTotal = new JLabel();
    private JLabel cartRestaurant = Theme.muted(" ");

    private JTextField streetField = new JTextField("Main Street 5");
    private JTextField xField = new JTextField("3");
    private JTextField yField = new JTextField("4");
    private JComboBox<String> pricingBox = new JComboBox<>(new String[] {"NORMAL", "EXPRESS", "PROMO"});
    private JComboBox<String> typeBox = new JComboBox<>(new String[] {"STANDARD", "EXPRESS", "SCHEDULED"});
    private JTextField scheduleField = new JTextField();

    private DefaultTableModel ordersModel = readOnlyModel("Order", "Restaurant", "Type", "Status",
            "Driver", "Total", "Payment");
    private JTable ordersTable = new JTable(ordersModel);
    private StatusStepper stepper = new StatusStepper();
    private JLabel orderDetails = new JLabel();
    private JButton cancelButton = Theme.danger("Cancel order");
    private DefaultListModel<String> notificationModel = new DefaultListModel<>();
    private JTabbedPane tabs = new JTabbedPane();

    public CustomerPanel(MainFrame frame, User user) {
        this.frame = frame;
        this.user = user;
        setLayout(new BorderLayout());
        add(Theme.header("Customer", "Hi " + user.getUsername() + ", what would you like to eat today?",
                frame::logout), BorderLayout.NORTH);

        tabs.addTab("Order food", buildOrderTab());
        tabs.addTab("My orders", buildMyOrdersTab());
        tabs.addTab("Notifications", buildNotificationsTab());
        tabs.addChangeListener(e -> {
            refreshOrders();
            refreshNotifications();
        });
        add(tabs, BorderLayout.CENTER);

        loadRestaurants();
        updateCart();
        refreshNotifications();
    }

    static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    // ---------------- order food ----------------

    private JPanel buildOrderTab() {
        JPanel page = Theme.page();

        JPanel restaurants = Theme.card("Open restaurants");
        restaurantList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        restaurantList.setFixedCellHeight(58);
        restaurantList.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                Restaurant r = (Restaurant) value;
                String html = "<html><b style='font-size:11px'>" + r.getName() + "</b><br>"
                        + "<span style='color:" + Theme.hex(Theme.MUTED) + "'>Min order "
                        + Theme.money(r.getMinimumOrderValue()) + " &middot; delivers "
                        + (int) r.getDeliveryRadiusKm() + " km</span></html>";
                JLabel l = (JLabel) super.getListCellRendererComponent(list, html, index, selected, false);
                l.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                return l;
            }
        });
        restaurantList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadMenu();
            }
        });
        restaurants.add(new JScrollPane(restaurantList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        restaurants.setPreferredSize(new Dimension(260, 0));
        page.add(restaurants, BorderLayout.WEST);

        JPanel menu = Theme.card(null);
        menuTitle.setFont(Theme.SUBTITLE);
        menu.add(menuTitle, BorderLayout.NORTH);
        Theme.styleTable(menuTable);
        menuTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menuTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    addToCart(1);
                }
            }
        });
        menu.add(new JScrollPane(menuTable), BorderLayout.CENTER);
        final JSpinner qty = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));
        qty.setPreferredSize(new Dimension(70, 34));
        JButton add = Theme.primary("Add to cart");
        add.addActionListener(e -> addToCart((Integer) qty.getValue()));
        menu.add(Theme.row(new JLabel("Quantity"), qty, add,
                Theme.muted("  tip: double-click an item to add one")), BorderLayout.SOUTH);
        page.add(menu, BorderLayout.CENTER);

        page.add(buildCartAndCheckout(), BorderLayout.EAST);
        return page;
    }

    private JPanel buildCartAndCheckout() {
        JPanel column = new JPanel(new BorderLayout(0, 14));
        column.setOpaque(false);
        column.setPreferredSize(new Dimension(380, 0));

        JPanel cartCard = Theme.card("Your cart");
        Theme.styleTable(cartTable);
        cartCard.add(new JScrollPane(cartTable), BorderLayout.CENTER);
        JButton remove = Theme.secondary("Remove");
        remove.addActionListener(e -> removeSelected());
        JButton clear = Theme.secondary("Clear");
        clear.addActionListener(e -> clearCart());
        cartTotal.setFont(Theme.SUBTITLE);
        JPanel cartBottom = new JPanel(new BorderLayout());
        cartBottom.setOpaque(false);
        cartBottom.add(cartRestaurant, BorderLayout.NORTH);
        cartBottom.add(Theme.row(remove, clear), BorderLayout.WEST);
        cartBottom.add(cartTotal, BorderLayout.EAST);
        cartCard.add(cartBottom, BorderLayout.SOUTH);
        column.add(cartCard, BorderLayout.CENTER);

        JPanel checkout = Theme.card("Delivery & checkout");
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 0, 3, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        int y = 0;
        y = formRow(form, c, y, "Street", streetField);
        JPanel xy = new JPanel(new GridLayout(1, 4, 6, 0));
        xy.setOpaque(false);
        xy.add(Theme.muted("X (km)"));
        xy.add(xField);
        xy.add(Theme.muted("Y (km)"));
        xy.add(yField);
        y = formRow(form, c, y, "Location", xy);
        y = formRow(form, c, y, "Pricing", pricingBox);
        y = formRow(form, c, y, "Order type", typeBox);
        scheduleField.putClientProperty("JTextField.placeholderText", "yyyy-MM-dd HH:mm");
        scheduleField.setEnabled(false);
        typeBox.addActionListener(e -> {
            boolean scheduled = "SCHEDULED".equals(typeBox.getSelectedItem());
            scheduleField.setEnabled(scheduled);
            if (scheduled && scheduleField.getText().trim().isEmpty()) {
                scheduleField.setText(LocalDateTime.now().plusHours(2).withMinute(0).format(TIME));
            }
        });
        formRow(form, c, y, "Deliver at", scheduleField);
        checkout.add(form, BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.setOpaque(false);
        south.add(Theme.muted("<html><div style='width:300px'>Express pricing doubles the delivery fee. Promo gives 15% off "
                + "(max 20 NIS). Express orders add 10 NIS.</div></html>"), BorderLayout.NORTH);
        JButton place = Theme.primary("Place order");
        place.setPreferredSize(new Dimension(0, 42));
        place.addActionListener(e -> placeOrder());
        south.add(place, BorderLayout.SOUTH);
        checkout.add(south, BorderLayout.SOUTH);
        column.add(checkout, BorderLayout.SOUTH);
        return column;
    }

    private static int formRow(JPanel form, GridBagConstraints c, int y, String label, Component field) {
        c.gridx = 0;
        c.gridy = y;
        c.weightx = 0;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        form.add(field, c);
        return y + 1;
    }

    private void loadRestaurants() {
        restaurantModel.clear();
        for (Restaurant r : frame.restaurantService.getOpenRestaurants()) {
            restaurantModel.addElement(r);
        }
        if (!restaurantModel.isEmpty()) {
            restaurantList.setSelectedIndex(0);
        }
    }

    private void loadMenu() {
        menuModel.setRowCount(0);
        Restaurant r = restaurantList.getSelectedValue();
        if (r == null) {
            return;
        }
        menuTitle.setText("Menu  -  " + r.getName());
        currentMenu = frame.restaurantService.getAvailableMenu(r.getId());
        for (MenuItem m : currentMenu) {
            menuModel.addRow(new Object[] {m.getName(), m.getPrice()});
        }
    }

    private void addToCart(int qty) {
        Restaurant r = restaurantList.getSelectedValue();
        int row = menuTable.getSelectedRow();
        if (r == null || row < 0) {
            JOptionPane.showMessageDialog(this, "Select a menu item first");
            return;
        }
        if (cartRestaurantId != null && !cartRestaurantId.equals(r.getId()) && !cart.isEmpty()) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "Your cart has items from another restaurant. Start a new cart?", "New cart",
                    JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
            clearCart();
        }
        cartRestaurantId = r.getId();
        cartRestaurant.setText("From " + r.getName());
        MenuItem item = currentMenu.get(row);
        Integer old = cart.get(item.getId());
        cart.put(item.getId(), old == null ? qty : old + qty);
        cartItems.put(item.getId(), item);
        updateCart();
    }

    private void removeSelected() {
        int row = cartTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        String id = new ArrayList<>(cart.keySet()).get(row);
        cart.remove(id);
        cartItems.remove(id);
        updateCart();
    }

    private void clearCart() {
        cart.clear();
        cartItems.clear();
        cartRestaurantId = null;
        cartRestaurant.setText(" ");
        updateCart();
    }

    private void updateCart() {
        cartModel.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> e : cart.entrySet()) {
            MenuItem m = cartItems.get(e.getKey());
            BigDecimal sub = m.getPrice().multiply(BigDecimal.valueOf(e.getValue()));
            total = total.add(sub);
            cartModel.addRow(new Object[] {m.getName(), e.getValue(), sub});
        }
        cartTotal.setText("Items: " + Theme.money(total));
    }

    List<OrderLineRequest> cartLines() {
        List<OrderLineRequest> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> e : cart.entrySet()) {
            lines.add(new OrderLineRequest(e.getKey(), e.getValue()));
        }
        return lines;
    }

    DeliveryAddress readAddress() {
        return new DeliveryAddress(streetField.getText(), new Location(
                Double.parseDouble(xField.getText().trim()),
                Double.parseDouble(yField.getText().trim())));
    }

    LocalDateTime readScheduledTime() {
        if (!"SCHEDULED".equals(typeBox.getSelectedItem()) || scheduleField.getText().trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(scheduleField.getText().trim(), TIME);
    }

    void placeOrder() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Your cart is empty", "Order",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Order order = frame.orderService.placeOrder(user, cartRestaurantId, cartLines(),
                    readAddress(), (String) pricingBox.getSelectedItem(),
                    (String) typeBox.getSelectedItem(), readScheduledTime());
            JOptionPane.showMessageDialog(this, billHtml(order), "Order placed",
                    JOptionPane.INFORMATION_MESSAGE);
            clearCart();
            tabs.setSelectedIndex(1);
            selectOrder(order.getId());
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Delivery time must look like 2026-11-05 19:30",
                    "Check the form", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "X and Y must be numbers", "Check the form",
                    JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Order rejected",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    static String billHtml(Order o) {
        StringBuilder sb = new StringBuilder("<html><div style='width:280px'>");
        sb.append("<h2>Order ").append(o.getId()).append("</h2>");
        sb.append("<table width='100%'>");
        for (OrderItem i : o.getItems()) {
            sb.append("<tr><td>").append(i.getQuantity()).append(" x ").append(i.getName())
                    .append("</td><td align='right'>").append(Theme.money(i.getSubtotal()))
                    .append("</td></tr>");
        }
        sb.append("<tr><td colspan='2'><hr></td></tr>");
        sb.append(line("Items", Theme.money(o.getItemsTotal())));
        sb.append(line("Delivery fee (" + Theme.nice(o.getPricingType()) + ")",
                Theme.money(o.getDeliveryFee())));
        if (o.getDiscount().signum() > 0) {
            sb.append(line("Discount", "- " + Theme.money(o.getDiscount())));
        }
        sb.append("<tr><td><b>Total</b></td><td align='right'><b>").append(Theme.money(o.getTotal()))
                .append("</b></td></tr></table>");
        sb.append("<p>Type: ").append(Theme.nice(o.getOrderType()));
        if (o.getScheduledTime() != null) {
            sb.append(" at ").append(o.getScheduledTime().format(TIME));
        }
        sb.append("<br>Payment: cash on delivery (").append(Theme.nice(o.getPaymentStatus()))
                .append(")</p></div></html>");
        return sb.toString();
    }

    private static String line(String label, String value) {
        return "<tr><td>" + label + "</td><td align='right'>" + value + "</td></tr>";
    }

    // ---------------- my orders ----------------

    private JPanel buildMyOrdersTab() {
        JPanel page = Theme.page();
        JPanel list = Theme.card("Your orders");
        Theme.styleTable(ordersTable);
        ordersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ordersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedOrder();
            }
        });
        list.add(new JScrollPane(ordersTable), BorderLayout.CENTER);
        JButton refresh = Theme.secondary("Refresh");
        refresh.addActionListener(e -> refreshOrders());
        list.add(Theme.row(refresh), BorderLayout.SOUTH);
        page.add(list, BorderLayout.CENTER);

        JPanel details = Theme.card("Order tracking");
        details.add(Theme.below(stepper, Theme.htmlScroll(orderDetails)), BorderLayout.CENTER);
        cancelButton.addActionListener(e -> cancelSelected());
        details.add(Theme.row(cancelButton), BorderLayout.SOUTH);
        details.setPreferredSize(new Dimension(0, 330));
        page.add(details, BorderLayout.SOUTH);
        showSelectedOrder();
        return page;
    }

    private String selectedOrderId() {
        int row = ordersTable.getSelectedRow();
        return row < 0 ? null : (String) ordersModel.getValueAt(row, 0);
    }

    private void selectOrder(String id) {
        refreshOrders();
        for (int i = 0; i < ordersModel.getRowCount(); i++) {
            if (ordersModel.getValueAt(i, 0).equals(id)) {
                ordersTable.setRowSelectionInterval(i, i);
            }
        }
    }

    private void showSelectedOrder() {
        String id = selectedOrderId();
        if (id == null) {
            stepper.setStatus(null);
            orderDetails.setText("<html><span style='color:" + Theme.hex(Theme.MUTED)
                    + "'>Select an order to see where it is.</span></html>");
            cancelButton.setEnabled(false);
            return;
        }
        Order o = frame.orderService.getOrder(id);
        stepper.setStatus(o.getStatus());
        String driver = o.getDriver() == null ? "not assigned yet"
                : o.getDriver().getName() + " (" + Theme.nice(o.getDriver().getType()) + ")";
        orderDetails.setText(billHtml(o).replace("</div></html>",
                "Restaurant: " + o.getRestaurant().getName() + "<br>Deliver to: "
                        + o.getDeliveryAddress().getStreet() + "<br>Driver: " + driver
                        + "</div></html>").replace("width:280px", "width:600px"));
        cancelButton.setEnabled(o.getStatus().isCancellable());
    }

    void refreshOrders() {
        String selected = selectedOrderId();
        ordersModel.setRowCount(0);
        List<Order> orders = frame.orderService.getOrdersFor(user);
        for (int i = orders.size() - 1; i >= 0; i--) {
            Order o = orders.get(i);
            ordersModel.addRow(new Object[] {o.getId(), o.getRestaurant().getName(),
                Theme.nice(o.getOrderType()), o.getStatus(),
                o.getDriver() == null ? "-" : o.getDriver().getName(), o.getTotal(),
                o.getPaymentStatus()});
        }
        for (int i = 0; i < ordersModel.getRowCount(); i++) {
            if (ordersModel.getValueAt(i, 0).equals(selected)) {
                ordersTable.setRowSelectionInterval(i, i);
            }
        }
        showSelectedOrder();
    }

    private void cancelSelected() {
        String id = selectedOrderId();
        if (id == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Cancel order " + id + "?", "Cancel order",
                JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            frame.orderService.cancelOrder(id);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot cancel",
                    JOptionPane.ERROR_MESSAGE);
        }
        refreshOrders();
    }

    // ---------------- notifications ----------------

    private JPanel buildNotificationsTab() {
        JPanel page = Theme.page();
        JPanel card = Theme.card("Notifications");
        JList<String> list = new JList<>(notificationModel);
        list.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                                                          boolean selected, boolean focus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l, value, index, selected, false);
                c.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12)));
                return c;
            }
        });
        card.add(new JScrollPane(list));
        page.add(card);
        return page;
    }

    private void refreshNotifications() {
        notificationModel.clear();
        List<String> all = frame.notificationService.getNotifications(user.getId());
        for (int i = all.size() - 1; i >= 0; i--) {
            notificationModel.addElement(all.get(i));
        }
        if (tabs.getTabCount() > 2) {
            tabs.setTitleAt(2, all.isEmpty() ? "Notifications" : "Notifications (" + all.size() + ")");
        }
    }

    /** @return true when a status is still moving */
    static boolean isActive(OrderStatus s) {
        return s != OrderStatus.DELIVERED && s != OrderStatus.CANCELLED;
    }
}
