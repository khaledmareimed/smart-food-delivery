package com.fooddelivery;

import com.fooddelivery.config.DataSeeder;
import com.fooddelivery.persistence.Database;
import com.fooddelivery.presentation.MainFrame;
import com.fooddelivery.repository.SqliteDriverRepository;
import com.fooddelivery.repository.SqliteOrderRepository;
import com.fooddelivery.repository.SqliteRestaurantRepository;
import com.fooddelivery.repository.SqliteUserRepository;
import com.fooddelivery.repository.SqlitePaymentRepository;
import com.fooddelivery.service.AuthService;
import com.fooddelivery.service.CashRegister;
import com.fooddelivery.service.DatabaseNotificationService;
import com.fooddelivery.service.DriverService;
import com.fooddelivery.service.OrderService;
import com.fooddelivery.service.PaymentService;
import com.fooddelivery.service.PricingService;
import com.fooddelivery.service.RestaurantService;
import com.fooddelivery.util.SystemTimeProvider;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point: wires the layers together and opens the Swing window.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Database db = new Database(Database.DEFAULT_URL);
        SqliteUserRepository users = new SqliteUserRepository(db);
        SqliteRestaurantRepository restaurants = new SqliteRestaurantRepository(db);
        SqliteOrderRepository orders = new SqliteOrderRepository(db);
        SqliteDriverRepository drivers = new SqliteDriverRepository(db);
        if (!users.findByUsername("alice").isPresent()) {
            DataSeeder.seed(users, restaurants);
        }
        if (drivers.findAll().isEmpty()) {
            DataSeeder.seedDrivers(drivers);
        }
        DatabaseNotificationService notifications = new DatabaseNotificationService(db);
        final AuthService auth = new AuthService(users);
        final RestaurantService restaurantService = new RestaurantService(restaurants);
        final OrderService orderService = new OrderService(restaurants, orders, notifications,
                drivers, new PricingService(new SystemTimeProvider()));
        final PaymentService paymentService = new PaymentService(orders,
                new SqlitePaymentRepository(db), new CashRegister());
        final DriverService driverService = new DriverService(drivers, orders, notifications);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // default look and feel is fine
        }
        SwingUtilities.invokeLater(() -> new MainFrame(auth, restaurantService, orderService,
                driverService, notifications, paymentService).setVisible(true));
    }
}
