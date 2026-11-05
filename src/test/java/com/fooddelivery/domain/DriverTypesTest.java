package com.fooddelivery.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/** US5.3 and US5.4 driver types and their rules. */
class DriverTypesTest {

    /** Restaurant at (0,0). */
    private Order order(double customerX, int quantity) {
        MenuItem m = new MenuItem("m1", "A", new BigDecimal("5"), true);
        return new Order("ORD-1", new User("u1", "a", "h", Role.CUSTOMER),
                new Restaurant("r1", "R", true), Collections.singletonList(new OrderItem(m, quantity)),
                new DeliveryAddress("Main", new Location(customerX, 0)));
    }

    @Test
    void typesAndLimits() {
        Location l = new Location(0, 0);
        assertEquals("STANDARD", new Driver("d", "n", l, 10).getType());
        assertEquals(0, new Driver("d", "n", l, 10).getMaxDistanceKm());
        assertEquals("BICYCLE", new BicycleDriver("d", "n", l, 10).getType());
        assertEquals(3, new BicycleDriver("d", "n", l, 10).getMaxDistanceKm());
        assertEquals("MOTORCYCLE", new MotorcycleDriver("d", "n", l, 10).getType());
        assertEquals(10, new MotorcycleDriver("d", "n", l, 10).getMaxDistanceKm());
        assertEquals("CAR", new CarDriver("d", "n", l, 10).getType());
        assertEquals("ELECTRIC", new ElectricDriver("d", "n", l, 10, 15).getType());
        assertEquals(15, new ElectricDriver("d", "n", l, 10, 15).getMaxDistanceKm());
    }

    @Test
    void bicycleHasLimitedDistance() {
        Driver bike = new BicycleDriver("d1", "Bike", new Location(0, 0), 15);
        assertTrue(bike.canDeliver(order(3, 1)));
        assertFalse(bike.canDeliver(order(3.5, 1)));
        assertFalse(bike.canDeliver(order(1, 10)));
    }

    @Test
    void motorcycleSupportsMediumDistance() {
        Driver moto = new MotorcycleDriver("d2", "Moto", new Location(0, 0), 40);
        assertTrue(moto.canDeliver(order(8, 2)));
        assertTrue(moto.canDeliver(order(10, 9)));
        assertFalse(moto.canDeliver(order(11, 1)));
        assertFalse(moto.canDeliver(order(2, 12)));
    }

    @Test
    void carSupportsLargeOrders() {
        Driver car = new CarDriver("d3", "Car", new Location(0, 0), 30);
        assertTrue(car.canDeliver(order(25, 30)));
        assertTrue(car.canDeliver(order(1, 1)));
        assertTrue(new Driver("d", "plain", new Location(0, 0), 10).canDeliver(order(50, 50)));
    }

    @Test
    void electricNeedsEnoughRange() {
        // driver 2 km from the restaurant, customer 5 km away => trip 7 km
        ElectricDriver ev = new ElectricDriver("d4", "EV", new Location(-2, 0), 35, 7);
        assertTrue(ev.canDeliver(order(5, 1)));
        ev.setBatteryRangeKm(6.9);
        assertEquals(6.9, ev.getBatteryRangeKm());
        assertFalse(ev.canDeliver(order(5, 1)));
        ev.setBatteryRangeKm(100);
        assertFalse(ev.canDeliver(order(1, 10)));
        assertThrows(IllegalArgumentException.class,
                () -> new ElectricDriver("d", "n", new Location(0, 0), 10, -1));
    }

    @Test
    void itemCountSumsQuantities() {
        assertEquals(7, order(1, 7).getItemCount());
    }
}
