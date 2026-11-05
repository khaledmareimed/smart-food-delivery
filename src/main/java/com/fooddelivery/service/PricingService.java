package com.fooddelivery.service;

import com.fooddelivery.domain.Order;
import com.fooddelivery.util.TimeProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Delivery fee (US4.2) and dynamic pricing (US4.3).
 * Pricing types: NORMAL, PEAK, EXPRESS, PROMO.
 */
public class PricingService {

    private TimeProvider timeProvider;

    public PricingService(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    /**
     * Fee = 5 + 1.5 per km.
     */
    public BigDecimal calculateDeliveryFee(double distanceKm) {
        if (distanceKm < 0) {
            throw new IllegalArgumentException("Distance must not be negative");
        }
        BigDecimal fee = new BigDecimal("5.00").add(new BigDecimal("1.50")
                .multiply(BigDecimal.valueOf(distanceKm)));
        return fee.setScale(2, RoundingMode.HALF_UP);
    }

    /** Lunch 12-14 and dinner 18-21 are peak hours. */
    public boolean isPeakHour() {
        int hour = timeProvider.now().getHour();
        return (hour >= 12 && hour < 14) || (hour >= 18 && hour < 21);
    }

    /**
     * Calculates the bill of the order with the given pricing type and stores
     * the delivery fee, discount and total on the order. NORMAL becomes PEAK
     * automatically during peak hours.
     */
    public BigDecimal applyPricing(Order order, String pricingType) {
        if (pricingType == null) {
            pricingType = "NORMAL";
        }
        double distance = order.getRestaurant().getLocation()
                .distanceTo(order.getDeliveryAddress().getLocation());
        BigDecimal items = order.getItemsTotal();
        BigDecimal fee = calculateDeliveryFee(distance);
        BigDecimal discount = BigDecimal.ZERO;

        if (pricingType.equals("NORMAL")) {
            if (isPeakHour()) {
                pricingType = "PEAK";
                fee = fee.multiply(new BigDecimal("1.5"));
            }
        } else if (pricingType.equals("PEAK")) {
            fee = fee.multiply(new BigDecimal("1.5"));
        } else if (pricingType.equals("EXPRESS")) {
            fee = fee.multiply(new BigDecimal("2"));
        } else if (pricingType.equals("PROMO")) {
            // 15% off the items, at most 20
            discount = items.multiply(new BigDecimal("0.15"));
            if (discount.compareTo(new BigDecimal("20")) > 0) {
                discount = new BigDecimal("20");
            }
        } else {
            throw new IllegalArgumentException("Unknown pricing type: " + pricingType);
        }

        fee = fee.setScale(2, RoundingMode.HALF_UP);
        discount = discount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = items.add(fee).subtract(discount).setScale(2, RoundingMode.HALF_UP);
        order.setPricingType(pricingType);
        order.setDeliveryFee(fee);
        order.setDiscount(discount);
        order.setTotal(total);
        return total;
    }
}
