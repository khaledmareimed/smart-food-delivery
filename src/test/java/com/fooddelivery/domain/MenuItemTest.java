package com.fooddelivery.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MenuItemTest {

    @Test
    void storesPriceExactly() {
        MenuItem item = new MenuItem("m1", "Pizza", new BigDecimal("30.50"), true);
        assertEquals(new BigDecimal("30.50"), item.getPrice());
        assertEquals("Pizza", item.getName());
        assertTrue(item.isAvailable());
    }

    @Test
    void availabilityCanChange() {
        MenuItem item = new MenuItem("m1", "Pizza", BigDecimal.TEN, true);
        item.setAvailable(false);
        assertFalse(item.isAvailable());
    }

    @Test
    void rejectsNegativeOrNullPrice() {
        assertThrows(IllegalArgumentException.class,
                () -> new MenuItem("m1", "Pizza", new BigDecimal("-1"), true));
        assertThrows(IllegalArgumentException.class,
                () -> new MenuItem("m1", "Pizza", null, true));
    }

    @Test
    void rejectsBlankIdOrName() {
        assertThrows(IllegalArgumentException.class,
                () -> new MenuItem(" ", "Pizza", BigDecimal.ONE, true));
        assertThrows(IllegalArgumentException.class,
                () -> new MenuItem("m1", null, BigDecimal.ONE, true));
    }

    @Test
    void equalityIsBasedOnId() {
        MenuItem a = new MenuItem("m1", "A", BigDecimal.ONE, true);
        MenuItem b = new MenuItem("m1", "B", BigDecimal.TEN, false);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new MenuItem("m2", "A", BigDecimal.ONE, true));
        assertNotEquals(a, "x");
    }
}
