package com.fooddelivery.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fooddelivery.domain.BicycleDriver;
import com.fooddelivery.domain.CarDriver;
import com.fooddelivery.domain.DeliveryAddress;
import com.fooddelivery.domain.Driver;
import com.fooddelivery.domain.ElectricDriver;
import com.fooddelivery.domain.Location;
import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.OrderItem;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.domain.Role;
import com.fooddelivery.domain.User;
import com.fooddelivery.exception.NoDriverAvailableException;
import com.fooddelivery.repository.DriverRepository;
import com.fooddelivery.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Only eligible driver types are considered (US3.2 + US5.4). */
class DriverTypeAssignmentTest {

    private Order order(double customerX, int qty) {
        MenuItem m = new MenuItem("m1", "A", new BigDecimal("5"), true);
        return new Order("ORD-1", new User("u1", "a", "h", Role.CUSTOMER),
                new Restaurant("r1", "R", true), Collections.singletonList(new OrderItem(m, qty)),
                new DeliveryAddress("Main", new Location(customerX, 0)));
    }

    private DriverService serviceWith(Order order, Driver... drivers) {
        DriverRepository driverRepo = mock(DriverRepository.class);
        OrderRepository orderRepo = mock(OrderRepository.class);
        when(orderRepo.findById("ORD-1")).thenReturn(Optional.of(order));
        when(driverRepo.findAll()).thenReturn(Arrays.asList(drivers));
        for (Driver d : drivers) {
            when(driverRepo.findById(d.getId())).thenReturn(Optional.of(d));
        }
        return new DriverService(driverRepo, orderRepo, mock(NotificationService.class));
    }

    @Test
    void nearestSkipsBicycleForLongTrip() {
        Driver bike = new BicycleDriver("d1", "Bike", new Location(0, 0), 15);
        Driver car = new CarDriver("d2", "Car", new Location(4, 0), 30);
        Order o = order(8, 1);
        assertSame(car, serviceWith(o, bike, car).assignBestDriver("ORD-1", "NEAREST").getDriver());
    }

    @Test
    void largeOrderNeedsCar() {
        Driver ev = new ElectricDriver("d1", "EV", new Location(0, 0), 50, 100);
        Driver car = new CarDriver("d2", "Car", new Location(9, 9), 20);
        Order o = order(2, 12);
        assertSame(car, serviceWith(o, ev, car).assignBestDriver("ORD-1", "FASTEST").getDriver());
    }

    @Test
    void noEligibleDriverRejected() {
        Driver bike = new BicycleDriver("d1", "Bike", new Location(0, 0), 15);
        DriverService s = serviceWith(order(8, 1), bike);
        assertThrows(NoDriverAvailableException.class, () -> s.assignBestDriver("ORD-1", "NEAREST"));
    }

    @Test
    void manualAssignmentChecksVehicleRules() {
        Driver bike = new BicycleDriver("d1", "Bike", new Location(0, 0), 15);
        DriverService s = serviceWith(order(8, 1), bike);
        NoDriverAvailableException e = assertThrows(NoDriverAvailableException.class,
                () -> s.assignDriver("ORD-1", "d1"));
        assertTrue(e.getMessage().contains("BICYCLE"));
        assertTrue(bike.isAvailable());
    }
}
