package org.example;

public interface Order {
    long id();
    Direction side();
    double quantity();
    double remaining();

    boolean isDone();
    Order consume(double amount);

    static void requirePositive(double v, String name) {
        if (v <= 0) throw new IllegalArgumentException(name + " must be > 0");
    }
}
