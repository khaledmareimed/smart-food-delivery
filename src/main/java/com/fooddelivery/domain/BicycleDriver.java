package com.fooddelivery.domain;

/**
 * Bicycles only do short trips (max 3 km) and small orders.
 */
public class BicycleDriver extends Driver {

    public BicycleDriver(String id, String name, Location location, double speedKmh) {
        super(id, name, location, speedKmh);
    }

    @Override
    public String getType() {
        return "BICYCLE";
    }

    @Override
    public double getMaxDistanceKm() {
        return 3;
    }

    @Override
    public boolean canDeliver(Order order) {
        if (order.getItemCount() >= 10) {
            return false;
        }
        return deliveryDistance(order) <= 3;
    }
}
