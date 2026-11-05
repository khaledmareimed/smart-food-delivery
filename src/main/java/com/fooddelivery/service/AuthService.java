package com.fooddelivery.service;

import com.fooddelivery.domain.User;
import com.fooddelivery.exception.AuthenticationException;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.util.PasswordHasher;

import java.util.Optional;

/**
 * Handles user authentication (US1.1).
 */
public class AuthService {

    /** Single message for every failure so callers cannot tell which part was wrong. */
    static final String INVALID_CREDENTIALS = "Invalid username or password";

    private final UserRepository userRepository;

    /**
     * @param userRepository the repository used to look up users
     */
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Logs a user in.
     *
     * @param username the login name
     * @param password the clear-text password
     * @return the authenticated user
     * @throws AuthenticationException if the credentials are missing or do not match
     */
    public User login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new AuthenticationException(INVALID_CREDENTIALS);
        }
        Optional<User> found = userRepository.findByUsername(username);
        if (!found.isPresent()
                || !found.get().getPasswordHash().equals(PasswordHasher.hash(password))) {
            throw new AuthenticationException(INVALID_CREDENTIALS);
        }
        return found.get();
    }
}
