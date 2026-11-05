package com.fooddelivery.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LocationTest {

    @Test
    void distanceUsesPythagoras() {
        assertEquals(5.0, new Location(0, 0).distanceTo(new Location(3, 4)), 1e-9);
    }

    @Test
    void distanceToSelfIsZero() {
        Location l = new Location(2, 2);
        assertEquals(0.0, l.distanceTo(l), 1e-9);
    }

    @Test
    void rejectsNonFiniteCoordinatesAndNullOther() {
        assertThrows(IllegalArgumentException.class, () -> new Location(Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> new Location(0, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new Location(0, 0).distanceTo(null));
    }

    @Test
    void equalityAndAccessors() {
        Location a = new Location(1.5, 2.5);
        assertEquals(a, new Location(1.5, 2.5));
        assertEquals(a.hashCode(), new Location(1.5, 2.5).hashCode());
        assertNotEquals(a, new Location(1.5, 9));
        assertNotEquals(a, "x");
        assertEquals(1.5, a.getX(), 0);
        assertEquals(2.5, a.getY(), 0);
        assertEquals("(1.5, 2.5)", a.toString());
    }
}
