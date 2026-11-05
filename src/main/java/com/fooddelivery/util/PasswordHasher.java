package com.fooddelivery.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for hashing passwords with SHA-256.
 * <p>
 * Note: plain SHA-256 is used to keep the project simple. A production system
 * should use a salted, slow hash such as bcrypt or PBKDF2.
 */
public final class PasswordHasher {

    private PasswordHasher() {
    }

    /**
     * Hashes a password.
     *
     * @param password the clear-text password, must not be null
     * @return the lowercase hexadecimal SHA-256 digest
     * @throws IllegalArgumentException if the password is null
     */
    public static String hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password must not be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
