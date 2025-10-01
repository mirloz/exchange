package org.example;

import java.util.Objects;

public record LimitOrder(
        long id,
        Direction side,
        double price,
        double quantity,
        double remaining
) implements Order
{
    private static final double EPS = 1e-9;

    public LimitOrder(long id, Direction side, double price, double quantity) {
        this(id, side, price, quantity, quantity);
    }

    public static LimitOrder of(long id, Direction side, double price, double quantity) {
        Order.requirePositive(quantity, "quantity");
        Order.requirePositive(price, "price");
        return new LimitOrder(id, Objects.requireNonNull(side), price, quantity);
    }

    @Override
    public LimitOrder consume(double amount) {
        Order.requirePositive(amount, "amount");
        if (amount >  remaining) {
            throw new IllegalArgumentException("overfill not allowed");
        }
        return new LimitOrder(id, side, price, quantity, remaining - amount);
    }

    @Override
    public boolean isDone() {
        return Math.abs(remaining) < EPS;
    }
}
