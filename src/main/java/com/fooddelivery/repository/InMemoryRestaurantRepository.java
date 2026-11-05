package com.fooddelivery.repository;

import com.fooddelivery.domain.Restaurant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory {@link RestaurantRepository} that preserves insertion order.
 */
public class InMemoryRestaurantRepository implements RestaurantRepository {

    private final Map<String, Restaurant> restaurants = new LinkedHashMap<>();

    @Override
    public void save(Restaurant restaurant) {
        restaurants.put(restaurant.getId(), restaurant);
    }

    @Override
    public Optional<Restaurant> findById(String id) {
        return Optional.ofNullable(restaurants.get(id));
    }

    @Override
    public List<Restaurant> findAll() {
        return new ArrayList<>(restaurants.values());
    }
}
