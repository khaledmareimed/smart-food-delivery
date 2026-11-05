package com.fooddelivery.presentation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fooddelivery.config.DataSeeder;
import com.fooddelivery.domain.DeliveryAddress;
import com.fooddelivery.domain.Location;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.OrderStatus;
import com.fooddelivery.domain.User;
import com.fooddelivery.repository.InMemoryOrderRepository;
import com.fooddelivery.repository.InMemoryRestaurantRepository;
import com.fooddelivery.repository.InMemoryUserRepository;
import com.fooddelivery.service.AuthService;
import com.fooddelivery.service.OrderLineRequest;
import com.fooddelivery.service.OrderService;
import com.fooddelivery.service.RestaurantService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ConsoleUIExtraTest {

    private static final String LOGIN = "alice\nalice123\n";

    private String run(InMemoryUserRepository users, InMemoryRestaurantRepository restaurants,
                       OrderService orders, String input) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new ConsoleUI(new AuthService(users), new RestaurantService(restaurants), orders,
                new ByteArrayInputStream(input.getBytes()), new PrintStream(bytes)).run();
        return bytes.toString();
    }

    @Test
    void cancellingAnOrderInPreparationShowsTheReason() {
        InMemoryUserRepository users = new InMemoryUserRepository();
        InMemoryRestaurantRepository restaurants = new InMemoryRestaurantRepository();
        DataSeeder.seed(users, restaurants);
        OrderService orders = new OrderService(restaurants, new InMemoryOrderRepository());
        User alice = users.findByUsername("alice").get();
        Order order = orders.placeOrder(alice, "r1",
                Collections.singletonList(new OrderLineRequest("m1", 2)),
                new DeliveryAddress("Main St", new Location(1, 1)));
        order.setStatus(OrderStatus.PREPARING);

        String out = run(users, restaurants, orders, LOGIN + "5\n" + order.getId() + "\n0\n");

        assertTrue(out.contains("Cannot cancel:"));
    }

    @Test
    void noOpenRestaurantsShowsAMessage() {
        InMemoryUserRepository users = new InMemoryUserRepository();
        InMemoryRestaurantRepository seeded = new InMemoryRestaurantRepository();
        DataSeeder.seed(users, seeded);
        InMemoryRestaurantRepository empty = new InMemoryRestaurantRepository();

        String out = run(users, empty, new OrderService(empty, new InMemoryOrderRepository()),
                LOGIN + "1\n0\n");

        assertTrue(out.contains("No restaurants are open right now."));
    }
}
