package com.fooddelivery.service;

import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.RestaurantRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides restaurant and menu browsing (US1.2 and US1.3).
 */
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    /**
     * @param restaurantRepository the repository used to look up restaurants
     */
    public RestaurantService(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    /**
     * Lists restaurants that are currently open; closed ones are hidden.
     *
     * @return the open restaurants
     */
    public List<Restaurant> getOpenRestaurants() {
        List<Restaurant> result = new ArrayList<>();
        for (Restaurant restaurant : restaurantRepository.findAll()) {
            if (restaurant.isOpen()) {
                result.add(restaurant);
            }
        }
        return result;
    }

    /**
     * Lists the available menu items of a restaurant; unavailable items are hidden.
     *
     * @param restaurantId the restaurant id
     * @return the available menu items
     * @throws ResourceNotFoundException if the restaurant does not exist
     */
    public List<MenuItem> getAvailableMenu(String restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Restaurant not found: " + restaurantId));
        return restaurant.getAvailableMenuItems();
    }

    /** @return all restaurants, open or closed (manager screen) */
    public List<Restaurant> getAllRestaurants() {
        return restaurantRepository.findAll();
    }

    /** Opens or closes a restaurant. */
    public void setRestaurantOpen(String restaurantId, boolean open) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Restaurant not found: " + restaurantId));
        restaurant.setOpen(open);
        restaurantRepository.save(restaurant);
    }

    /** Marks a menu item as available or sold out. */
    public void setMenuItemAvailable(String restaurantId, String menuItemId, boolean available) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Restaurant not found: " + restaurantId));
        MenuItem item = null;
        for (MenuItem m : restaurant.getMenu()) {
            if (m.getId().equals(menuItemId)) {
                item = m;
            }
        }
        if (item == null) {
            throw new ResourceNotFoundException("Menu item not found: " + menuItemId);
        }
        item.setAvailable(available);
        restaurantRepository.save(restaurant);
    }
}
