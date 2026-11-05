package com.fooddelivery.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    @Test
    void hashIsDeterministic() {
        assertEquals(PasswordHasher.hash("secret"), PasswordHasher.hash("secret"));
    }

    @Test
    void differentPasswordsGiveDifferentHashes() {
        assertNotEquals(PasswordHasher.hash("a"), PasswordHasher.hash("b"));
    }

    @Test
    void hashIsNotClearText() {
        String hash = PasswordHasher.hash("secret");
        assertNotEquals("secret", hash);
        assertEquals(64, hash.length());
    }

    @Test
    void nullPasswordRejected() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(null));
    }
}
