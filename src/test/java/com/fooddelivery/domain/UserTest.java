package com.fooddelivery.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void createsValidUser() {
        User u = new User("u1", "alice", "hash", Role.CUSTOMER);
        assertEquals("u1", u.getId());
        assertEquals("alice", u.getUsername());
        assertEquals("hash", u.getPasswordHash());
        assertEquals(Role.CUSTOMER, u.getRole());
    }

    @Test
    void rejectsBlankOrNullFields() {
        assertThrows(IllegalArgumentException.class, () -> new User("", "a", "h", Role.CUSTOMER));
        assertThrows(IllegalArgumentException.class, () -> new User("u", " ", "h", Role.CUSTOMER));
        assertThrows(IllegalArgumentException.class, () -> new User("u", "a", null, Role.CUSTOMER));
        assertThrows(IllegalArgumentException.class, () -> new User("u", "a", "h", null));
    }

    @Test
    void equalityIsBasedOnId() {
        User a = new User("u1", "alice", "h", Role.CUSTOMER);
        User b = new User("u1", "other", "x", Role.MANAGER);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new User("u2", "alice", "h", Role.CUSTOMER));
        assertNotEquals(a, "not a user");
    }
}
