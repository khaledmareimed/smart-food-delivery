package com.fooddelivery.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fooddelivery.domain.DeliveryAddress;
import com.fooddelivery.domain.Location;
import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.OrderItem;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.domain.Role;
import com.fooddelivery.domain.User;
import com.fooddelivery.util.TimeProvider;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US4.2 and US4.3 with a mocked clock. */
class PricingServiceTest {

    TimeProvider clock;
    PricingService pricing;

    @BeforeEach
    void setUp() {
        clock = mock(TimeProvider.class);
        when(clock.now()).thenReturn(LocalDateTime.of(2026, 10, 1, 10, 0)); // not peak
        pricing = new PricingService(clock);
    }

    /** Restaurant at (0,0), customer at (3,4) => 5 km => fee 5 + 7.5 = 12.50 */
    private Order order(String price, int qty) {
        MenuItem m = new MenuItem("m1", "A", new BigDecimal(price), true);
        return new Order("ORD-1", new User("u1", "a", "h", Role.CUSTOMER),
                new Restaurant("r1", "R", true), Collections.singletonList(new OrderItem(m, qty)),
                new DeliveryAddress("Main", new Location(3, 4)));
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " got " + actual);
    }

    @Test
    void deliveryFeeDependsOnDistance() {
        assertMoney("5.00", pricing.calculateDeliveryFee(0));
        assertMoney("12.50", pricing.calculateDeliveryFee(5));
        assertMoney("20.00", pricing.calculateDeliveryFee(10));
        assertThrows(IllegalArgumentException.class, () -> pricing.calculateDeliveryFee(-1));
    }

    @Test
    void normalPricingIncludesDeliveryFeeInBill() {
        Order o = order("20.00", 2);
        BigDecimal total = pricing.applyPricing(o, "NORMAL");
        assertMoney("52.50", total);
        assertMoney("12.50", o.getDeliveryFee());
        assertMoney("0", o.getDiscount());
        assertEquals("NORMAL", o.getPricingType());
        assertMoney("52.50", o.getTotal());
    }

    @Test
    void nullTypeMeansNormal() {
        Order o = order("20.00", 1);
        assertMoney("32.50", pricing.applyPricing(o, null));
        assertEquals("NORMAL", o.getPricingType());
    }

    @Test
    void peakPricingMultipliesFee() {
        Order o = order("20.00", 1);
        assertMoney("38.75", pricing.applyPricing(o, "PEAK"));
        assertMoney("18.75", o.getDeliveryFee());
    }

    @Test
    void normalBecomesPeakDuringLunch() {
        when(clock.now()).thenReturn(LocalDateTime.of(2026, 10, 1, 13, 30));
        Order o = order("20.00", 1);
        pricing.applyPricing(o, "NORMAL");
        assertEquals("PEAK", o.getPricingType());
        assertMoney("18.75", o.getDeliveryFee());
    }

    @Test
    void peakHoursBoundaries() {
        int[][] cases = {{11, 0}, {12, 1}, {13, 1}, {14, 0}, {17, 0}, {18, 1}, {20, 1}, {21, 0}};
        for (int[] c : cases) {
            when(clock.now()).thenReturn(LocalDateTime.of(2026, 10, 1, c[0], 0));
            assertEquals(c[1] == 1, pricing.isPeakHour(), "hour " + c[0]);
        }
    }

    @Test
    void expressPricingDoublesFee() {
        Order o = order("20.00", 1);
        assertMoney("45.00", pricing.applyPricing(o, "EXPRESS"));
        assertMoney("25.00", o.getDeliveryFee());
    }

    @Test
    void promoGivesFifteenPercentOffItems() {
        Order o = order("20.00", 2);   // items 40 -> discount 6
        assertMoney("46.50", pricing.applyPricing(o, "PROMO"));
        assertMoney("6.00", o.getDiscount());
    }

    @Test
    void promoDiscountIsCappedAtTwenty() {
        Order o = order("100.00", 2);  // 15% of 200 = 30 -> 20
        assertMoney("192.50", pricing.applyPricing(o, "PROMO"));
        assertMoney("20.00", o.getDiscount());
    }

    @Test
    void unknownPricingTypeRejected() {
        assertThrows(IllegalArgumentException.class, () -> pricing.applyPricing(order("1", 1), "FREE"));
    }
}
