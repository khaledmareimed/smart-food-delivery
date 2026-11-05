package com.fooddelivery.config;

import com.fooddelivery.domain.BicycleDriver;
import com.fooddelivery.domain.CarDriver;
import com.fooddelivery.domain.ElectricDriver;
import com.fooddelivery.domain.Location;
import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.MotorcycleDriver;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.domain.Role;
import com.fooddelivery.domain.User;
import com.fooddelivery.repository.DriverRepository;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.util.PasswordHasher;

import java.math.BigDecimal;

/**
 * Fills the in-memory repositories with demo data for the console application.
 */
public final class DataSeeder {

    private DataSeeder() {
    }

    /**
     * Seeds demo users and restaurants.
     *
     * @param users       the user repository to fill
     * @param restaurants the restaurant repository to fill
     */
    public static void seed(UserRepository users, RestaurantRepository restaurants) {
        users.save(new User("u1", "alice", PasswordHasher.hash("alice123"), Role.CUSTOMER));
        users.save(new User("u2", "bob", PasswordHasher.hash("manager123"), Role.MANAGER));

        Restaurant pizza = new Restaurant("r1", "Pizza Palace", true,
                new Location(0, 0), 10.0, new BigDecimal("50.00"));
        pizza.addMenuItem(new MenuItem("m1", "Margherita", new BigDecimal("30.00"), true));
        pizza.addMenuItem(new MenuItem("m2", "Pepperoni", new BigDecimal("35.50"), true));
        pizza.addMenuItem(new MenuItem("m3", "Four Cheese", new BigDecimal("40.00"), false));

        Restaurant burger = new Restaurant("r2", "Burger House", true,
                new Location(5, 5), 8.0, new BigDecimal("30.00"));
        burger.addMenuItem(new MenuItem("m4", "Classic Burger", new BigDecimal("25.00"), true));
        burger.addMenuItem(new MenuItem("m5", "Fries", new BigDecimal("10.00"), true));

        Restaurant sushi = new Restaurant("r3", "Sushi Corner", false,
                new Location(2, 8), 12.0, new BigDecimal("60.00"));
        sushi.addMenuItem(new MenuItem("m6", "Salmon Roll", new BigDecimal("45.00"), true));

        restaurants.save(pizza);
        restaurants.save(burger);
        restaurants.save(sushi);
    }

    public static void seedDrivers(DriverRepository drivers) {
        drivers.save(new BicycleDriver("d1", "Omar", new Location(1, 1), 15));
        drivers.save(new MotorcycleDriver("d2", "Sara", new Location(6, 6), 40));
        drivers.save(new CarDriver("d3", "Ali", new Location(3, 0), 30));
        drivers.save(new ElectricDriver("d4", "Lina", new Location(2, 2), 35, 12));
    }
}
