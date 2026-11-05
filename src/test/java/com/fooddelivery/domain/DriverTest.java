package com.fooddelivery.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DriverTest {

    @Test
    void newDriverIsAvailable() {
        Driver d = new Driver("d1", "Omar", new Location(1, 2), 20);
        assertEquals("d1", d.getId());
        assertEquals("Omar", d.getName());
        assertEquals(new Location(1, 2), d.getLocation());
        assertEquals(20, d.getSpeedKmh());
        assertTrue(d.isAvailable());
        assertEquals(DriverStatus.AVAILABLE, d.getStatus());
        assertEquals("Omar (d1)", d.toString());
    }

    @Test
    void statusAndLocationCanChange() {
        Driver d = new Driver("d1", "Omar", new Location(1, 2), 20);
        d.setStatus(DriverStatus.BUSY);
        d.setLocation(new Location(5, 5));
        assertFalse(d.isAvailable());
        assertEquals(new Location(5, 5), d.getLocation());
    }

    @Test
    void invalidArgumentsRejected() {
        Location l = new Location(0, 0);
        assertThrows(IllegalArgumentException.class, () -> new Driver(null, "a", l, 10));
        assertThrows(IllegalArgumentException.class, () -> new Driver(" ", "a", l, 10));
        assertThrows(IllegalArgumentException.class, () -> new Driver("d", "", l, 10));
        assertThrows(IllegalArgumentException.class, () -> new Driver("d", null, l, 10));
        assertThrows(IllegalArgumentException.class, () -> new Driver("d", "a", null, 10));
        assertThrows(IllegalArgumentException.class, () -> new Driver("d", "a", l, 0));
    }

    @Test
    void equalityById() {
        Driver a = new Driver("d1", "Omar", new Location(1, 2), 20);
        Driver b = new Driver("d1", "Other", new Location(0, 0), 10);
        assertEquals(a, b);
        assertEquals(a, a);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new Driver("d2", "Omar", new Location(1, 2), 20));
        assertNotEquals(a, "d1");
    }
}
