package org.example;

import java.util.*;

/**
 * Price-time priority Order Book using double prices/sizes and immutable Order records.
 * Bids sorted descending, asks ascending. Levels are FIFO queues of orders at the same price.
 */
public class OrderBook {

    private final NavigableMap<Double, Deque<Order>> bids;
    private final NavigableMap<Double, Deque<Order>> asks;

    public static final OrderBook DEMO_BOOK = new OrderBook(List.of(
            // ASKS
            Order.of(1L,  Direction.Sell, 101.00, 500),
            Order.of(2L,  Direction.Sell, 101.00, 200),
            Order.of(3L,  Direction.Sell, 101.50, 350),
            Order.of(4L,  Direction.Sell, 101.50, 150),
            Order.of(5L,  Direction.Sell, 102.00, 700),
            Order.of(6L,  Direction.Sell, 102.50, 400),
            Order.of(7L,  Direction.Sell, 102.50, 300),
            Order.of(8L,  Direction.Sell, 102.50, 100),
            Order.of(9L,  Direction.Sell, 103.00, 900),
            Order.of(10L, Direction.Sell, 103.50, 450),
            Order.of(11L, Direction.Sell, 103.50, 250),
            Order.of(12L, Direction.Sell, 104.00, 600),
            Order.of(13L, Direction.Sell, 104.50, 1200),
            Order.of(14L, Direction.Sell, 105.00, 500),
            Order.of(15L, Direction.Sell, 105.00, 300),

            // BIDS
            Order.of(16L, Direction.Buy, 100.00, 500),
            Order.of(17L, Direction.Buy, 100.00, 200),
            Order.of(18L, Direction.Buy, 99.50, 300),
            Order.of(19L, Direction.Buy, 99.50, 150),
            Order.of(20L, Direction.Buy, 99.00, 450),
            Order.of(21L, Direction.Buy, 98.50, 250),
            Order.of(22L, Direction.Buy, 98.50, 100),
            Order.of(23L, Direction.Buy, 98.00, 400),
            Order.of(24L, Direction.Buy, 97.50, 220),
            Order.of(25L, Direction.Buy, 97.00, 300),
            Order.of(26L, Direction.Buy, 96.50, 180),
            Order.of(27L, Direction.Buy, 96.00, 200)
    ));

    // bids: highest price first; asks: lowest price first
    public OrderBook() {
        this.bids = new TreeMap<>(Comparator.reverseOrder());
        this.asks = new TreeMap<>();
    }

    public OrderBook(Collection<Order> orders) {
        this();
        if (orders != null) {
            orders.forEach(this::addOrder);
        }
    }

    public void addOrder(Order o) {
        Objects.requireNonNull(o);
        var book = (o.side() == Direction.Buy) ? bids : asks;
        book.computeIfAbsent(o.price(), px -> new ArrayDeque<>()).offer(o);
    }

    public double getBestBid() {
        if (bids.isEmpty()) throw new IllegalStateException("Empty bids");
        return bids.firstKey();
    }

    public double getBestAsk() {
        if (asks.isEmpty()) throw new IllegalStateException("Empty asks");
        return asks.firstKey();
    }

    public Deque<Order> getOrdersAtPrice(Double price) {
        var q = bids.get(price);
        if (q == null) q = asks.get(price);
        return (q == null) ? new ArrayDeque<>() : new ArrayDeque<>(q);
    }

    public List<BookLevel> getBidLevels() {
        var out = new ArrayList<BookLevel>(bids.size());
        bids.forEach((px, q) -> out.add(new BookLevel(px, totalQuantityAtLevel(q))));
        return out;
    }

    public List<BookLevel> getAskLevels() {
        var out = new ArrayList<BookLevel>(asks.size());
        asks.forEach((px, q) -> out.add(new BookLevel(px, totalQuantityAtLevel(q))));
        return out;
    }

    private static double totalQuantityAtLevel(Deque<Order> ordersAtPrice) {
        return ordersAtPrice.stream().mapToDouble(Order::remaining).sum();
    }

    public double vwapBook() {
        double notional = 0.0;
        double volume   = 0.0;

        for (Deque<Order> q : bids.values()) {
            for (Order o : q) {
                notional += o.price() * o.remaining();
                volume   += o.remaining();
            }
        }
        for (Deque<Order> q : asks.values()) {
            for (Order o : q) {
                notional += o.price() * o.remaining();
                volume   += o.remaining();
            }
        }

        if (volume == 0.0) {
            throw new IllegalStateException("Cannot compute VWAP on empty book");
        }
        return notional / volume;
    }

    public double vwapSide(Direction side) {
        double notional = 0.0;
        double volume   = 0.0;

        var book = (side == Direction.Buy) ? bids : asks;

        for (Deque<Order> q : book.values()) {
            for (Order o : q) {
                notional += o.price() * o.remaining();
                volume   += o.remaining();
            }
        }

        if (volume == 0.0) {
            throw new IllegalStateException("Cannot compute VWAP on empty " + side + " side");
        }
        return notional / volume;
    }


    public record BookLevel(double price, double totalQuantity) {}
}
