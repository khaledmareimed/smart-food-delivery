package com.fooddelivery.repository;

import com.fooddelivery.domain.User;

import java.util.Optional;

/**
 * Persistence abstraction for {@link User} entities.
 */
public interface UserRepository {

    /**
     * Saves (inserts or replaces) a user.
     *
     * @param user the user to save
     */
    void save(User user);

    /**
     * Finds a user by login name.
     *
     * @param username the login name
     * @return the user, or empty if none exists
     */
    Optional<User> findByUsername(String username);
}
