package com.fooddelivery.repository;

import com.fooddelivery.domain.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory {@link UserRepository} backed by a map keyed by username.
 */
public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> users = new HashMap<>();

    @Override
    public void save(User user) {
        users.put(user.getUsername(), user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    @Override
    public Optional<User> findById(String id) {
        for (User u : users.values()) {
            if (u.getId().equals(id)) {
                return Optional.of(u);
            }
        }
        return Optional.empty();
    }
}
