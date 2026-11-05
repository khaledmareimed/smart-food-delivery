package com.fooddelivery.domain;

/**
 * Electric vehicles need enough battery range for the whole trip
 * (driver to restaurant, then restaurant to customer).
 */
public class ElectricDriver extends Driver {

    private double batteryRangeKm;

    public ElectricDriver(String id, String name, Location location, double speedKmh,
                          double batteryRangeKm) {
        super(id, name, location, speedKmh);
        if (batteryRangeKm < 0) {
            throw new IllegalArgumentException("Battery range must not be negative");
        }
        this.batteryRangeKm = batteryRangeKm;
    }

    public double getBatteryRangeKm() {
        return batteryRangeKm;
    }

    public void setBatteryRangeKm(double batteryRangeKm) {
        this.batteryRangeKm = batteryRangeKm;
    }

    @Override
    public String getType() {
        return "ELECTRIC";
    }

    @Override
    public double getMaxDistanceKm() {
        return batteryRangeKm;
    }

    @Override
    public boolean canDeliver(Order order) {
        if (order.getItemCount() >= 10) {
            return false;
        }
        double trip = getLocation().distanceTo(order.getRestaurant().getLocation())
                + deliveryDistance(order);
        return trip <= batteryRangeKm;
    }
}
