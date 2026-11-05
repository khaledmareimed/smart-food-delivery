package com.fooddelivery.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.fooddelivery.domain.DeliveryAddress;
import com.fooddelivery.domain.Location;
import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.OrderItem;
import com.fooddelivery.domain.OrderStatus;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.domain.Role;
import com.fooddelivery.domain.User;
import com.fooddelivery.persistence.Database;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteRepositoriesTest {

    Database db;
    SqliteUserRepository users;
    SqliteRestaurantRepository restaurants;
    SqliteOrderRepository orders;
    User alice;
    Restaurant pizza;

    @BeforeEach
    void setUp() {
        db = Database.inMemory();
        users = new SqliteUserRepository(db);
        restaurants = new SqliteRestaurantRepository(db);
        orders = new SqliteOrderRepository(db);
        alice = new User("u1", "alice", "hash", Role.CUSTOMER);
        users.save(alice);
        pizza = new Restaurant("r1", "Pizza", true, new Location(1, 2), 5.0, new BigDecimal("20.00"));
        pizza.addMenuItem(new MenuItem("m1", "Margherita", new BigDecimal("30.00"), true));
        pizza.addMenuItem(new MenuItem("m2", "Pepperoni", new BigDecimal("35.50"), false));
        restaurants.save(pizza);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void userCanBeFoundByUsernameAndId() {
        Optional<User> byName = users.findByUsername("alice");
        assertTrue(byName.isPresent());
        assertEquals("u1", byName.get().getId());
        assertEquals(Role.CUSTOMER, byName.get().getRole());
        assertEquals("hash", users.findById("u1").get().getPasswordHash());
        assertFalse(users.findByUsername("nobody").isPresent());
        assertFalse(users.findById("x").isPresent());
    }

    @Test
    void restaurantIsSavedWithItsMenu() {
        Restaurant loaded = restaurants.findById("r1").get();
        assertEquals("Pizza", loaded.getName());
        assertTrue(loaded.isOpen());
        assertEquals(new Location(1, 2), loaded.getLocation());
        assertEquals(5.0, loaded.getDeliveryRadiusKm());
        assertEquals(0, new BigDecimal("20").compareTo(loaded.getMinimumOrderValue()));
        assertEquals(2, loaded.getMenu().size());
        assertEquals(1, loaded.getAvailableMenuItems().size());
        assertFalse(restaurants.findById("nope").isPresent());
    }

    @Test
    void savingAgainUpdatesRestaurant() {
        pizza.setOpen(false);
        pizza.getMenu().get(1).setAvailable(true);
        restaurants.save(pizza);
        restaurants.save(new Restaurant("r2", "Burger", true));

        assertEquals(2, restaurants.findAll().size());
        Restaurant loaded = restaurants.findById("r1").get();
        assertFalse(loaded.isOpen());
        assertEquals(2, loaded.getAvailableMenuItems().size());
    }

    @Test
    void orderRoundTrip() {
        Order order = new Order("ORD-1", alice, pizza,
                Arrays.asList(new OrderItem(pizza.getMenu().get(0), 2)),
                new DeliveryAddress("Main St", new Location(3, 4)));
        orders.save(order);

        Order loaded = orders.findById("ORD-1").get();
        assertEquals(alice, loaded.getCustomer());
        assertEquals(pizza, loaded.getRestaurant());
        assertEquals("Main St", loaded.getDeliveryAddress().getStreet());
        assertEquals(new Location(3, 4), loaded.getDeliveryAddress().getLocation());
        assertEquals(OrderStatus.PENDING, loaded.getStatus());
        assertEquals(1, loaded.getItems().size());
        assertEquals(0, new BigDecimal("60.00").compareTo(loaded.getItemsTotal()));
        assertFalse(orders.findById("ORD-2").isPresent());
    }

    @Test
    void orderStatusUpdateAndQueries() {
        User bob = new User("u2", "bob", "h", Role.CUSTOMER);
        users.save(bob);
        Order o1 = new Order("ORD-1", alice, pizza,
                Arrays.asList(new OrderItem(pizza.getMenu().get(0), 1)),
                new DeliveryAddress("A", new Location(0, 0)));
        Order o2 = new Order("ORD-2", bob, pizza,
                Arrays.asList(new OrderItem(pizza.getMenu().get(0), 1)),
                new DeliveryAddress("B", new Location(0, 0)));
        orders.save(o1);
        orders.save(o2);
        o1.cancel();
        orders.save(o1);

        assertEquals(OrderStatus.CANCELLED, orders.findById("ORD-1").get().getStatus());
        assertEquals(1, orders.findByCustomerId("u1").size());
        assertEquals(2, orders.findAll().size());
        assertEquals(1, orders.findById("ORD-1").get().getItems().size());
    }
}
