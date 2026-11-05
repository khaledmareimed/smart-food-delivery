package com.fooddelivery.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.fooddelivery.config.DataSeeder;
import com.fooddelivery.domain.DeliveryAddress;
import com.fooddelivery.domain.Driver;
import com.fooddelivery.domain.DriverStatus;
import com.fooddelivery.domain.Location;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.OrderItem;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.domain.User;
import com.fooddelivery.persistence.Database;
import com.fooddelivery.service.DatabaseNotificationService;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class SqliteDriverRepositoryTest {

    @Test
    void driversAreSavedAndLoaded() {
        Database db = Database.inMemory();
        SqliteDriverRepository repo = new SqliteDriverRepository(db);
        DataSeeder.seedDrivers(repo);
        assertEquals(4, repo.findAll().size());

        Driver d = repo.findById("d1").get();
        assertEquals("Omar", d.getName());
        assertEquals(new Location(1, 1), d.getLocation());
        d.setStatus(DriverStatus.BUSY);
        repo.save(d);
        assertEquals(DriverStatus.BUSY, repo.findById("d1").get().getStatus());
        assertFalse(repo.findById("zz").isPresent());
        db.close();
    }

    @Test
    void orderRemembersItsDriver() {
        Database db = Database.inMemory();
        SqliteUserRepository users = new SqliteUserRepository(db);
        SqliteRestaurantRepository restaurants = new SqliteRestaurantRepository(db);
        SqliteDriverRepository drivers = new SqliteDriverRepository(db);
        SqliteOrderRepository orders = new SqliteOrderRepository(db);
        DataSeeder.seed(users, restaurants);
        DataSeeder.seedDrivers(drivers);
        User alice = users.findByUsername("alice").get();
        Restaurant r1 = restaurants.findById("r1").get();
        Order order = new Order("ORD-1", alice, r1,
                Collections.singletonList(new OrderItem(r1.getMenu().get(0), 2)),
                new DeliveryAddress("Main", new Location(1, 1)));
        orders.save(order);
        assertNull(orders.findById("ORD-1").get().getDriver());

        order.setDriver(drivers.findById("d2").get());
        orders.save(order);
        assertEquals("Sara", orders.findById("ORD-1").get().getDriver().getName());
        assertEquals(1, orders.findByDriverId("d2").size());
        assertTrue(orders.findByDriverId("d1").isEmpty());

        DataSeeder.seedDriverUsers(users);
        assertEquals(com.fooddelivery.domain.Role.DRIVER, users.findByUsername("omar").get().getRole());
        assertEquals("d1", users.findByUsername("omar").get().getId());
        db.close();
    }

    @Test
    void notificationsAreStoredPerUser() {
        Database db = Database.inMemory();
        DatabaseNotificationService service = new DatabaseNotificationService(db);
        service.send("u1", "hello");
        service.send("u1", "second");
        service.send("u2", "other");
        assertEquals(java.util.Arrays.asList("hello", "second"), service.getNotifications("u1"));
        assertTrue(service.getNotifications("nobody").isEmpty());
        db.close();
    }

    @Test
    void driverTypesAreRestored() {
        Database db = Database.inMemory();
        SqliteDriverRepository repo = new SqliteDriverRepository(db);
        DataSeeder.seedDrivers(repo);
        repo.save(new Driver("d9", "Plain", new Location(0, 0), 10));
        assertEquals("BICYCLE", repo.findById("d1").get().getType());
        assertEquals("MOTORCYCLE", repo.findById("d2").get().getType());
        assertEquals("CAR", repo.findById("d3").get().getType());
        Driver ev = repo.findById("d4").get();
        assertEquals("ELECTRIC", ev.getType());
        assertEquals(12, ((com.fooddelivery.domain.ElectricDriver) ev).getBatteryRangeKm());
        assertEquals("STANDARD", repo.findById("d9").get().getType());
        db.close();
    }

    @Test
    void orderTypeAndScheduleAreStored() {
        Database db = Database.inMemory();
        SqliteUserRepository users = new SqliteUserRepository(db);
        SqliteRestaurantRepository restaurants = new SqliteRestaurantRepository(db);
        SqliteOrderRepository orders = new SqliteOrderRepository(db);
        DataSeeder.seed(users, restaurants);
        Restaurant r1 = restaurants.findById("r1").get();
        Order order = new Order("ORD-1", users.findByUsername("alice").get(), r1,
                Collections.singletonList(new OrderItem(r1.getMenu().get(0), 2)),
                new DeliveryAddress("Main", new Location(1, 1)));
        java.time.LocalDateTime when = java.time.LocalDateTime.of(2026, 11, 10, 19, 30);
        order.setOrderType("SCHEDULED");
        order.setScheduledTime(when);
        orders.save(order);
        Order loaded = orders.findById("ORD-1").get();
        assertEquals("SCHEDULED", loaded.getOrderType());
        assertEquals(when, loaded.getScheduledTime());
        db.close();
    }
}
