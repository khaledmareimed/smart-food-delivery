package com.fooddelivery.presentation;

import com.fooddelivery.domain.Role;
import com.fooddelivery.domain.User;
import com.fooddelivery.service.AuthService;
import com.fooddelivery.service.DriverService;
import com.fooddelivery.service.NotificationService;
import com.fooddelivery.service.OrderService;
import com.fooddelivery.service.PaymentService;
import com.fooddelivery.service.RestaurantService;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.CardLayout;

/**
 * Main window. Switches between the login screen and the customer / manager screens.
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    AuthService authService;
    RestaurantService restaurantService;
    OrderService orderService;
    DriverService driverService;
    NotificationService notificationService;
    PaymentService paymentService;

    private CardLayout cards = new CardLayout();
    private JPanel root = new JPanel(cards);

    public MainFrame(AuthService authService, RestaurantService restaurantService,
                     OrderService orderService, DriverService driverService,
                     NotificationService notificationService, PaymentService paymentService) {
        super("Smart Food Delivery");
        this.authService = authService;
        this.restaurantService = restaurantService;
        this.orderService = orderService;
        this.driverService = driverService;
        this.notificationService = notificationService;
        this.paymentService = paymentService;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);
        root.add(new LoginPanel(this), "login");
        setContentPane(root);
        cards.show(root, "login");
    }

    void loggedIn(User user) {
        JPanel panel;
        if (user.getRole() == Role.MANAGER) {
            panel = new ManagerPanel(this, user);
        } else {
            panel = new CustomerPanel(this, user);
        }
        root.add(panel, "home");
        cards.show(root, "home");
        setTitle("Smart Food Delivery - " + user.getUsername() + " (" + user.getRole() + ")");
    }

    void logout() {
        root.removeAll();
        root.add(new LoginPanel(this), "login");
        cards.show(root, "login");
        setTitle("Smart Food Delivery");
        root.revalidate();
        root.repaint();
    }
}
