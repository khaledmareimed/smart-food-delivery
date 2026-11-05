package com.fooddelivery.repository;

import com.fooddelivery.domain.Restaurant;

import java.util.List;
import java.util.Optional;

/**
 * Persistence abstraction for {@link Restaurant} entities.
 */
public interface RestaurantRepository {

    /**
     * Saves (inserts or replaces) a restaurant.
     *
     * @param restaurant the restaurant to save
     */
    void save(Restaurant restaurant);

    /**
     * Finds a restaurant by id.
     *
     * @param id the restaurant id
     * @return the restaurant, or empty if none exists
     */
    Optional<Restaurant> findById(String id);

    /** @return all restaurants, open and closed, in insertion order */
    List<Restaurant> findAll();
}
