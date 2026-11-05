package com.fooddelivery.domain;

/**
 * An immutable point on a flat 2D map. Coordinates are expressed in kilometres,
 * so the distance between two locations is also in kilometres.
 */
public final class Location {

    private final double x;
    private final double y;

    /**
     * Creates a location.
     *
     * @param x the x coordinate in km
     * @param y the y coordinate in km
     * @throws IllegalArgumentException if a coordinate is NaN or infinite
     */
    public Location(double x, double y) {
        if (Double.isNaN(x) || Double.isInfinite(x) || Double.isNaN(y) || Double.isInfinite(y)) {
            throw new IllegalArgumentException("Coordinates must be finite numbers");
        }
        this.x = x;
        this.y = y;
    }

    /** @return the x coordinate in km */
    public double getX() {
        return x;
    }

    /** @return the y coordinate in km */
    public double getY() {
        return y;
    }

    /**
     * Straight-line (Euclidean) distance to another location.
     *
     * @param other the other location, must not be null
     * @return the distance in km
     * @throws IllegalArgumentException if {@code other} is null
     */
    public double distanceTo(Location other) {
        if (other == null) {
            throw new IllegalArgumentException("Other location must not be null");
        }
        double dx = x - other.x;
        double dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Location)) {
            return false;
        }
        Location other = (Location) o;
        return Double.compare(x, other.x) == 0 && Double.compare(y, other.y) == 0;
    }

    @Override
    public int hashCode() {
        return 31 * Double.hashCode(x) + Double.hashCode(y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
