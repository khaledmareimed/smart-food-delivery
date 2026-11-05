package com.fooddelivery.presentation;

import com.fooddelivery.domain.DeliveryAddress;
import com.fooddelivery.domain.Location;
import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.domain.User;
import com.fooddelivery.exception.AuthenticationException;
import com.fooddelivery.exception.DeliveryPolicyException;
import com.fooddelivery.exception.InvalidOrderException;
import com.fooddelivery.exception.InvalidOrderStateException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.service.AuthService;
import com.fooddelivery.service.OrderLineRequest;
import com.fooddelivery.service.OrderService;
import com.fooddelivery.service.RestaurantService;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Simple text-based user interface. Input and output streams are injected so
 * the class can be tested without a real console.
 */
public class ConsoleUI {

    private final AuthService authService;
    private final RestaurantService restaurantService;
    private final OrderService orderService;
    private final Scanner in;
    private final PrintStream out;

    /**
     * @param authService       authentication service
     * @param restaurantService restaurant browsing service
     * @param orderService      order placing and cancelling service
     * @param input             where user input is read from
     * @param output            where messages are printed
     */
    public ConsoleUI(AuthService authService, RestaurantService restaurantService,
                     OrderService orderService, InputStream input, PrintStream output) {
        this.authService = authService;
        this.restaurantService = restaurantService;
        this.orderService = orderService;
        this.in = new Scanner(input);
        this.out = output;
    }

    /**
     * Runs the application: asks for credentials once, then shows the main menu
     * until the user chooses to exit.
     */
    public void run() {
        out.println("=== Smart Food Delivery ===");
        String username = ask("Username: ");
        String password = ask("Password: ");

        User user;
        try {
            user = authService.login(username, password);
        } catch (AuthenticationException e) {
            out.println("Login failed: " + e.getMessage());
            return;
        }
        out.println("Welcome, " + user.getUsername() + " (" + user.getRole() + ")");
        mainMenu(user);
    }

    private String ask(String prompt) {
        out.print(prompt);
        return in.hasNextLine() ? in.nextLine().trim() : "";
    }

    private void mainMenu(User user) {
        while (true) {
            out.println();
            out.println("1) View open restaurants");
            out.println("2) View restaurant menu");
            out.println("3) Place an order");
            out.println("4) My orders");
            out.println("5) Cancel an order");
            out.println("0) Exit");
            out.print("Choice: ");
            if (!in.hasNextLine()) {
                return;
            }
            String choice = in.nextLine().trim();
            switch (choice) {
                case "1":
                    showRestaurants();
                    break;
                case "2":
                    showMenu();
                    break;
                case "3":
                    placeOrder(user);
                    break;
                case "4":
                    showOrders(user);
                    break;
                case "5":
                    cancelOrder();
                    break;
                case "0":
                    out.println("Goodbye!");
                    return;
                default:
                    out.println("Invalid choice.");
            }
        }
    }

    private void showRestaurants() {
        List<Restaurant> restaurants = restaurantService.getOpenRestaurants();
        if (restaurants.isEmpty()) {
            out.println("No restaurants are open right now.");
            return;
        }
        for (Restaurant r : restaurants) {
            out.println("[" + r.getId() + "] " + r.getName());
        }
    }

    private void showMenu() {
        String id = ask("Restaurant id: ");
        try {
            List<MenuItem> items = restaurantService.getAvailableMenu(id);
            if (items.isEmpty()) {
                out.println("No items available.");
                return;
            }
            for (MenuItem item : items) {
                out.println("[" + item.getId() + "] " + item.getName()
                        + " - " + item.getPrice().toPlainString());
            }
        } catch (ResourceNotFoundException e) {
            out.println(e.getMessage());
        }
    }

    private void placeOrder(User user) {
        String restaurantId = ask("Restaurant id: ");
        List<OrderLineRequest> lines = new ArrayList<>();
        try {
            while (true) {
                String itemId = ask("Item id (blank to finish): ");
                if (itemId.isEmpty()) {
                    break;
                }
                int quantity = Integer.parseInt(ask("Quantity: "));
                lines.add(new OrderLineRequest(itemId, quantity));
            }
            String street = ask("Delivery street: ");
            double x = Double.parseDouble(ask("Delivery x (km): "));
            double y = Double.parseDouble(ask("Delivery y (km): "));
            DeliveryAddress address = new DeliveryAddress(street, new Location(x, y));

            Order order = orderService.placeOrder(user, restaurantId, lines, address);
            out.println("Order " + order.getId() + " placed. Status: " + order.getStatus()
                    + ", items total: " + order.getItemsTotal().toPlainString());
        } catch (NumberFormatException e) {
            out.println("Please enter a valid number.");
        } catch (InvalidOrderException | DeliveryPolicyException
                | ResourceNotFoundException | IllegalArgumentException e) {
            out.println("Order rejected: " + e.getMessage());
        }
    }

    private void showOrders(User user) {
        List<Order> orders = orderService.getOrdersFor(user);
        if (orders.isEmpty()) {
            out.println("You have no orders.");
            return;
        }
        for (Order o : orders) {
            out.println(o.getId() + " | " + o.getRestaurant().getName() + " | "
                    + o.getStatus() + " | " + o.getItemsTotal().toPlainString());
        }
    }

    private void cancelOrder() {
        String orderId = ask("Order id: ");
        try {
            Order order = orderService.cancelOrder(orderId);
            out.println("Order " + order.getId() + " is now " + order.getStatus());
        } catch (ResourceNotFoundException | InvalidOrderStateException e) {
            out.println("Cannot cancel: " + e.getMessage());
        }
    }
}
