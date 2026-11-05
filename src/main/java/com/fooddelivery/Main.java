package com.fooddelivery;

import com.fooddelivery.config.DataSeeder;
import com.fooddelivery.presentation.ConsoleUI;
import com.fooddelivery.repository.InMemoryRestaurantRepository;
import com.fooddelivery.repository.InMemoryUserRepository;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.AuthService;
import com.fooddelivery.service.RestaurantService;

/**
 * Application entry point: wires the layers together and starts the console UI.
 */
public final class Main {

    private Main() {
    }

    /**
     * @param args ignored
     */
    public static void main(String[] args) {
        UserRepository users = new InMemoryUserRepository();
        RestaurantRepository restaurants = new InMemoryRestaurantRepository();
        DataSeeder.seed(users, restaurants);

        ConsoleUI ui = new ConsoleUI(new AuthService(users),
                new RestaurantService(restaurants), System.in, System.out);
        ui.run();
    }
}
