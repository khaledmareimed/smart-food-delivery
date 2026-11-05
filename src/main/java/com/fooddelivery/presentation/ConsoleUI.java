package com.fooddelivery.presentation;

import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.domain.User;
import com.fooddelivery.exception.AuthenticationException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.service.AuthService;
import com.fooddelivery.service.RestaurantService;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 * Simple text-based user interface. Input and output streams are injected so
 * the class can be tested without a real console.
 */
public class ConsoleUI {

    private final AuthService authService;
    private final RestaurantService restaurantService;
    private final Scanner in;
    private final PrintStream out;

    /**
     * @param authService       authentication service
     * @param restaurantService restaurant browsing service
     * @param input             where user input is read from
     * @param output            where messages are printed
     */
    public ConsoleUI(AuthService authService, RestaurantService restaurantService,
                     InputStream input, PrintStream output) {
        this.authService = authService;
        this.restaurantService = restaurantService;
        this.in = new Scanner(input);
        this.out = output;
    }

    /**
     * Runs the application: asks for credentials once, then shows the main menu
     * until the user chooses to exit.
     */
    public void run() {
        out.println("=== Smart Food Delivery ===");
        out.print("Username: ");
        String username = in.hasNextLine() ? in.nextLine() : "";
        out.print("Password: ");
        String password = in.hasNextLine() ? in.nextLine() : "";

        User user;
        try {
            user = authService.login(username, password);
        } catch (AuthenticationException e) {
            out.println("Login failed: " + e.getMessage());
            return;
        }
        out.println("Welcome, " + user.getUsername() + " (" + user.getRole() + ")");
        mainMenu();
    }

    private void mainMenu() {
        while (true) {
            out.println();
            out.println("1) View open restaurants");
            out.println("2) View restaurant menu");
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
        out.print("Restaurant id: ");
        String id = in.hasNextLine() ? in.nextLine().trim() : "";
        try {
            List<MenuItem> items = restaurantService.getAvailableMenu(id);
            if (items.isEmpty()) {
                out.println("No items available.");
                return;
            }
            for (MenuItem item : items) {
                out.println(item.getName() + " - " + item.getPrice().toPlainString());
            }
        } catch (ResourceNotFoundException e) {
            out.println(e.getMessage());
        }
    }
}
