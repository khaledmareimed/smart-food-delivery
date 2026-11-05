package com.fooddelivery.domain;

import java.util.Objects;

/**
 * A registered user of the system (customer or manager).
 * The password is never stored in clear text; only its hash is kept.
 */
public class User {

    private final String id;
    private final String username;
    private final String passwordHash;
    private final Role role;

    /**
     * Creates a user.
     *
     * @param id           unique identifier, must not be blank
     * @param username     login name, must not be blank
     * @param passwordHash hashed password, must not be blank
     * @param role         the user's role, must not be null
     * @throws IllegalArgumentException if any argument is null or blank
     */
    public User(String id, String username, String passwordHash, Role role) {
        if (isBlank(id) || isBlank(username) || isBlank(passwordHash) || role == null) {
            throw new IllegalArgumentException("User fields must not be null or blank");
        }
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** @return the unique identifier */
    public String getId() {
        return id;
    }

    /** @return the login name */
    public String getUsername() {
        return username;
    }

    /** @return the hashed password */
    public String getPasswordHash() {
        return passwordHash;
    }

    /** @return the user's role */
    public Role getRole() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User)) {
            return false;
        }
        return id.equals(((User) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
