package com.fooddelivery.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderItemTest {

    @Test
    void subtotalIsPriceTimesQuantity() {
        MenuItem pizza = new MenuItem("m1", "Pizza", new BigDecimal("30.50"), true);
        OrderItem line = new OrderItem(pizza, 3);
        assertEquals(0, new BigDecimal("91.50").compareTo(line.getSubtotal()));
        assertEquals("m1", line.getMenuItemId());
        assertEquals("Pizza", line.getName());
        assertEquals(3, line.getQuantity());
    }

    @Test
    void pricesAreCopiedAtOrderTime() {
        MenuItem pizza = new MenuItem("m1", "Pizza", new BigDecimal("30.00"), true);
        OrderItem line = new OrderItem(pizza, 1);
        assertEquals(0, new BigDecimal("30.00").compareTo(line.getUnitPrice()));
    }

    @Test
    void rejectsInvalidQuantityOrNullItem() {
        MenuItem pizza = new MenuItem("m1", "Pizza", BigDecimal.TEN, true);
        assertThrows(IllegalArgumentException.class, () -> new OrderItem(pizza, 0));
        assertThrows(IllegalArgumentException.class, () -> new OrderItem(pizza, -2));
        assertThrows(IllegalArgumentException.class, () -> new OrderItem(null, 1));
    }
}
