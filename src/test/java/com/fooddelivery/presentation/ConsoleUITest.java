package com.fooddelivery.presentation;

import static org.junit.jupiter.api.Assertions.*;

import com.fooddelivery.config.DataSeeder;
import com.fooddelivery.repository.InMemoryOrderRepository;
import com.fooddelivery.repository.InMemoryRestaurantRepository;
import com.fooddelivery.repository.InMemoryUserRepository;
import com.fooddelivery.service.AuthService;
import com.fooddelivery.service.OrderService;
import com.fooddelivery.service.RestaurantService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsoleUITest {

    private InMemoryUserRepository users;
    private InMemoryRestaurantRepository restaurants;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        restaurants = new InMemoryRestaurantRepository();
        DataSeeder.seed(users, restaurants);
    }

    private String runWith(String input) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ConsoleUI ui = new ConsoleUI(new AuthService(users), new RestaurantService(restaurants),
                new OrderService(restaurants, new InMemoryOrderRepository()),
                new ByteArrayInputStream(input.getBytes()), new PrintStream(bytes));
        ui.run();
        return bytes.toString();
    }

    @Test
    void invalidLoginShowsErrorAndStops() {
        String out = runWith("alice\nwrong\n");
        assertTrue(out.contains("Login failed: Invalid username or password"));
        assertFalse(out.contains("View open restaurants"));
    }

    @Test
    void validLoginThenListRestaurantsHidesClosedOnes() {
        String out = runWith("alice\nalice123\n1\n0\n");
        assertTrue(out.contains("Welcome, alice (CUSTOMER)"));
        assertTrue(out.contains("Pizza Palace"));
        assertTrue(out.contains("Burger House"));
        assertFalse(out.contains("Sushi Corner"));
        assertTrue(out.contains("Goodbye!"));
    }

    @Test
    void menuHidesUnavailableItemsAndShowsPrices() {
        String out = runWith("alice\nalice123\n2\nr1\n0\n");
        assertTrue(out.contains("[m1] Margherita - 30.00"));
        assertTrue(out.contains("[m2] Pepperoni - 35.50"));
        assertFalse(out.contains("Four Cheese"));
    }

    @Test
    void unknownRestaurantAndInvalidChoiceAreHandled() {
        String out = runWith("bob\nmanager123\n2\nzzz\n9\n0\n");
        assertTrue(out.contains("Restaurant not found: zzz"));
        assertTrue(out.contains("Invalid choice."));
    }

    @Test
    void inputEndingEarlyDoesNotCrash() {
        assertDoesNotThrow(() -> runWith("alice\nalice123\n"));
        assertDoesNotThrow(() -> runWith(""));
    }

    private static final String LOGIN = "alice\nalice123\n";

    @Test
    void customerCanPlaceListAndCancelAnOrder() {
        // 2 x Margherita (30.00) = 60.00 >= minimum 50.00; (1,1) is inside the zone
        String out = runWith(LOGIN + "3\nr1\nm1\n2\n\nMain St\n1\n1\n4\n5\nORD-1\n4\n0\n");
        assertTrue(out.contains("Order ORD-1 placed. Status: PENDING, items total: 60.00"));
        assertTrue(out.contains("ORD-1 | Pizza Palace | PENDING | 60.00"));
        assertTrue(out.contains("Order ORD-1 is now CANCELLED"));
        assertTrue(out.contains("ORD-1 | Pizza Palace | CANCELLED | 60.00"));
    }

    @Test
    void rejectedOrdersShowTheReason() {
        // unavailable item m3
        String out = runWith(LOGIN + "3\nr1\nm3\n1\n\nMain St\n1\n1\n0\n");
        assertTrue(out.contains("Order rejected: Item is not available: Four Cheese"));
        // below minimum (1 x 30.00 < 50.00)
        out = runWith(LOGIN + "3\nr1\nm1\n1\n\nMain St\n1\n1\n0\n");
        assertTrue(out.contains("Order rejected:"));
        assertTrue(out.contains("minimum"));
        // empty order
        out = runWith(LOGIN + "3\nr1\n\nMain St\n1\n1\n0\n");
        assertTrue(out.contains("at least one item"));
        // outside the zone
        out = runWith(LOGIN + "3\nr1\nm1\n2\n\nMain St\n50\n50\n0\n");
        assertTrue(out.contains("outside the delivery zone"));
    }

    @Test
    void invalidNumbersAndUnknownOrdersAreHandled() {
        String out = runWith(LOGIN + "3\nr1\nm1\nabc\n0\n");
        assertTrue(out.contains("Please enter a valid number."));
        out = runWith(LOGIN + "5\nORD-99\n4\n0\n");
        assertTrue(out.contains("Cannot cancel: Order not found: ORD-99"));
        assertTrue(out.contains("You have no orders."));
    }
}
