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

    @Test
    void deliveryZoneIsInclusiveOfTheRadius() {
        Restaurant r = new Restaurant("r1", "Pizza", true,
                new Location(0, 0), 5.0, new BigDecimal("20"));
        assertTrue(r.isWithinDeliveryZone(new Location(3, 4)));
        assertTrue(r.isWithinDeliveryZone(new Location(0, 0)));
        assertFalse(r.isWithinDeliveryZone(new Location(3.1, 4)));
        assertEquals(5.0, r.getDeliveryRadiusKm(), 0);
        assertEquals(new Location(0, 0), r.getLocation());
        assertEquals(0, new BigDecimal("20").compareTo(r.getMinimumOrderValue()));
    }

    @Test
    void shortConstructorUsesDefaults() {
        Restaurant r = new Restaurant("r1", "Pizza", true);
        assertEquals(Restaurant.DEFAULT_DELIVERY_RADIUS_KM, r.getDeliveryRadiusKm(), 0);
        assertEquals(0, BigDecimal.ZERO.compareTo(r.getMinimumOrderValue()));
    }

    @Test
    void rejectsInvalidZoneSettings() {
        assertThrows(IllegalArgumentException.class, () -> new Restaurant("r1", "P", true,
                null, 5.0, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new Restaurant("r1", "P", true,
                new Location(0, 0), -1, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new Restaurant("r1", "P", true,
                new Location(0, 0), 5, new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> new Restaurant("r1", "P", true,
                new Location(0, 0), 5, null));
    }

    @Test
    void findMenuItemFindsAvailableAndUnavailableItems() {
        Restaurant r = new Restaurant("r1", "Pizza", true);
        r.addMenuItem(item("a", true));
        r.addMenuItem(item("b", false));
        assertTrue(r.findMenuItem("a").isPresent());
        assertTrue(r.findMenuItem("b").isPresent());
        assertFalse(r.findMenuItem("zzz").isPresent());
    }
}
