package com.fooddelivery.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.fooddelivery.config.DataSeeder;
import com.fooddelivery.domain.DeliveryAddress;
import com.fooddelivery.domain.Location;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.OrderItem;
import com.fooddelivery.domain.Payment;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.persistence.Database;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class SqlitePaymentRepositoryTest {

    @Test
    void paymentsAreStored() {
        Database db = Database.inMemory();
        SqlitePaymentRepository repo = new SqlitePaymentRepository(db);
        assertEquals(0, repo.count());
        repo.save(new Payment("PAY-1", "ORD-1", "CASH", new BigDecimal("10"), null, "REJECTED"));
        repo.save(new Payment("PAY-2", "ORD-1", "CASH", new BigDecimal("60"), new BigDecimal("7.5"), "PAID"));
        repo.save(new Payment("PAY-3", "ORD-2", "CASH", null, null, "REJECTED"));

        List<Payment> list = repo.findByOrderId("ORD-1");
        assertEquals(2, list.size());
        assertEquals("REJECTED", list.get(0).getStatus());
        assertNull(list.get(0).getChange());
        assertEquals(0, new BigDecimal("7.5").compareTo(list.get(1).getChange()));
        assertNull(repo.findByOrderId("ORD-2").get(0).getAmount());
        assertEquals(3, repo.count());
        db.close();
    }

    @Test
    void orderBillFieldsAreStored() {
        Database db = Database.inMemory();
        SqliteUserRepository users = new SqliteUserRepository(db);
        SqliteRestaurantRepository restaurants = new SqliteRestaurantRepository(db);
        SqliteOrderRepository orders = new SqliteOrderRepository(db);
        DataSeeder.seed(users, restaurants);
        Restaurant r1 = restaurants.findById("r1").get();
        Order order = new Order("ORD-1", users.findByUsername("alice").get(), r1,
                Collections.singletonList(new OrderItem(r1.getMenu().get(0), 2)),
                new DeliveryAddress("Main", new Location(3, 4)));
        order.setPricingType("PROMO");
        order.setDeliveryFee(new BigDecimal("12.50"));
        order.setDiscount(new BigDecimal("9.00"));
        order.setTotal(new BigDecimal("63.50"));
        order.setPaymentStatus("PAID");
        orders.save(order);

        Order loaded = orders.findById("ORD-1").get();
        assertEquals("PROMO", loaded.getPricingType());
        assertEquals(new BigDecimal("12.50"), loaded.getDeliveryFee());
        assertEquals(new BigDecimal("9.00"), loaded.getDiscount());
        assertEquals(new BigDecimal("63.50"), loaded.getTotal());
        assertEquals("PAID", loaded.getPaymentStatus());
        db.close();
    }
}
