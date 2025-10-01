package org.example;

public record MidOrder(
        long id,
        Direction side,
        double quantity,
        double remaining
) implements Order
{
    private static final double EPS = 1e-9;

    public MidOrder(long id, Direction side, double quantity) {
        this(id, side, quantity, quantity);
    }

    public MidOrder of(long id, Direction side, double quantity) {
        Order.requirePositive(quantity, "quantity");
        return new MidOrder(id, side, quantity);
    }

    @Override
    public MidOrder consume(double amt) {
        if (amt <= 0 || amt > remaining) throw new IllegalArgumentException();
        return new MidOrder(id, side, quantity, remaining - amt);
    }

    @Override
    public boolean isDone() { return Math.abs(remaining) < EPS; }
}
