package org.example;

import java.util.Objects;

public record Order(
        long id,
        Direction side,
        double price,
        double quantity,
        double remaining
)
{
    private static final double EPSILON = 1e-9;

    public Order(long id, Direction side, double price, double quantity) {
        this(id, side, price, quantity, quantity);
    }

    public static Order of(long id, Direction side, double price, double quantity) {
        requirePositive(quantity, "quantity");
        requirePositive(price, "price");
        return new Order(id, Objects.requireNonNull(side), price, quantity);
    }

    public Order consume(double amount) {
        requirePositive(amount, "amount");
        if (amount >  remaining) {
            throw new IllegalArgumentException("overfill not allowed");
        }
        return new Order(id, side, price, quantity, remaining - amount);
    }

    public boolean isDone() {
        return Math.abs(remaining) < EPSILON;
    }

    private static void requirePositive(double v, String name) {
        if (v <= 0) throw new IllegalArgumentException(name + " must be > 0");
    }
}
