package com.fooddelivery.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DeliveryAddressTest {

    @Test
    void validAddress() {
        DeliveryAddress a = new DeliveryAddress("Main St", new Location(1, 1));
        assertTrue(a.isValid());
        assertEquals("Main St", a.getStreet());
        assertEquals(new Location(1, 1), a.getLocation());
        assertTrue(a.toString().contains("Main St"));
    }

    @Test
    void blankStreetIsInvalid() {
        assertFalse(new DeliveryAddress("  ", new Location(1, 1)).isValid());
        assertFalse(new DeliveryAddress(null, new Location(1, 1)).isValid());
    }

    @Test
    void missingLocationIsInvalid() {
        assertFalse(new DeliveryAddress("Main St", null).isValid());
    }
}
