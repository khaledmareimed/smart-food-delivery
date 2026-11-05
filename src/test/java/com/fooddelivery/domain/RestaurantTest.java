package com.fooddelivery.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RestaurantTest {

    private MenuItem item(String id, boolean available) {
        return new MenuItem(id, "Item " + id, BigDecimal.TEN, available);
    }

    @Test
    void availableMenuItemsExcludeUnavailableOnes() {
        Restaurant r = new Restaurant("r1", "Pizza", true);
        r.addMenuItem(item("a", true));
        r.addMenuItem(item("b", false));
        r.addMenuItem(item("c", true));

        List<MenuItem> available = r.getAvailableMenuItems();
        assertEquals(2, available.size());
        assertEquals("a", available.get(0).getId());
        assertEquals("c", available.get(1).getId());
        assertEquals(3, r.getMenu().size());
    }

    @Test
    void menuViewIsUnmodifiable() {
        Restaurant r = new Restaurant("r1", "Pizza", true);
        assertThrows(UnsupportedOperationException.class, () -> r.getMenu().add(item("a", true)));
    }

    @Test
    void nullMenuItemRejected() {
        Restaurant r = new Restaurant("r1", "Pizza", true);
        assertThrows(IllegalArgumentException.class, () -> r.addMenuItem(null));
    }

    @Test
    void openStatusCanChange() {
        Restaurant r = new Restaurant("r1", "Pizza", true);
        r.setOpen(false);
        assertFalse(r.isOpen());
        assertEquals("Pizza", r.getName());
    }

    @Test
    void rejectsBlankIdOrName() {
        assertThrows(IllegalArgumentException.class, () -> new Restaurant("", "Pizza", true));
        assertThrows(IllegalArgumentException.class, () -> new Restaurant("r1", null, true));
    }

    @Test
    void equalityIsBasedOnId() {
        Restaurant a = new Restaurant("r1", "A", true);
        Restaurant b = new Restaurant("r1", "B", false);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new Restaurant("r2", "A", true));
        assertNotEquals(a, "x");
    }
}
