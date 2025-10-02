package org.example;

import java.util.*;

/**
 * Price-time priority Order Book using double prices/sizes and immutable Order records.
 * Bids sorted descending, asks ascending. Levels are FIFO queues of orders at the same price.
 */
public class LimitOrderBook implements Book {

    private final NavigableMap<Double, Deque<LimitOrder>> bids;
    private final NavigableMap<Double, Deque<LimitOrder>> asks;

    public static final LimitOrderBook DEMO_BOOK = new LimitOrderBook(List.of(
            // ASKS
            LimitOrder.of(1L,  Direction.Sell, 101.00, 500),
            LimitOrder.of(2L,  Direction.Sell, 101.00, 200),
            LimitOrder.of(3L,  Direction.Sell, 101.50, 350),
            LimitOrder.of(4L,  Direction.Sell, 101.50, 150),
            LimitOrder.of(5L,  Direction.Sell, 102.00, 700),
            LimitOrder.of(6L,  Direction.Sell, 102.50, 400),
            LimitOrder.of(7L,  Direction.Sell, 102.50, 300),
            LimitOrder.of(8L,  Direction.Sell, 102.50, 100),
            LimitOrder.of(9L,  Direction.Sell, 103.00, 900),
            LimitOrder.of(10L, Direction.Sell, 103.50, 450),
            LimitOrder.of(11L, Direction.Sell, 103.50, 250),
            LimitOrder.of(12L, Direction.Sell, 104.00, 600),
            LimitOrder.of(13L, Direction.Sell, 104.50, 1200),
            LimitOrder.of(14L, Direction.Sell, 105.00, 500),
            LimitOrder.of(15L, Direction.Sell, 105.00, 300),

            // BIDS
            LimitOrder.of(16L, Direction.Buy, 100.00, 500),
            LimitOrder.of(17L, Direction.Buy, 100.00, 200),
            LimitOrder.of(18L, Direction.Buy, 99.50, 300),
            LimitOrder.of(19L, Direction.Buy, 99.50, 150),
            LimitOrder.of(20L, Direction.Buy, 99.00, 450),
            LimitOrder.of(21L, Direction.Buy, 98.50, 250),
            LimitOrder.of(22L, Direction.Buy, 98.50, 100),
            LimitOrder.of(23L, Direction.Buy, 98.00, 400),
            LimitOrder.of(24L, Direction.Buy, 97.50, 220),
            LimitOrder.of(25L, Direction.Buy, 97.00, 300),
            LimitOrder.of(26L, Direction.Buy, 96.50, 180),
            LimitOrder.of(27L, Direction.Buy, 96.00, 200)
    ));

    // Private Constructor: initialise Bids (highest price first) and Asks (lowest price first)
    private LimitOrderBook() {
        this.bids = new TreeMap<>(Comparator.reverseOrder());
        this.asks = new TreeMap<>();
    }

    // Canonical Constructor
    public LimitOrderBook(Collection<LimitOrder> orders) {
        this();
        if (orders != null) {
            for (LimitOrder o : orders) {
                ExecutionReport report = placeOrder(o);
                if (!report.trades().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Cannot initialise Order Book with crossing orders. Offending order: " + o
                    );
                }
            }
        }
    }

    // Copy Constructor
    public LimitOrderBook(LimitOrderBook book) {
        this();
        book.bids.forEach((px, q) -> this.bids.put(px, new ArrayDeque<>(q)));
        book.asks.forEach((px, q) -> this.asks.put(px, new ArrayDeque<>(q)));
    }

    public void addOrder(LimitOrder o) {
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

    public double getMid() {
        return (getBestBid() + getBestAsk()) / 2.0;
    }

    public Deque<LimitOrder> getOrdersAtPrice(Double price) {
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

    private static double totalQuantityAtLevel(Deque<LimitOrder> ordersAtPrice) {
        return ordersAtPrice.stream().mapToDouble(LimitOrder::remaining).sum();
    }

    public double vwapBook() {
        double notional = 0.0;
        double volume   = 0.0;

        for (Deque<LimitOrder> q : bids.values()) {
            for (LimitOrder o : q) {
                notional += o.price() * o.remaining();
                volume   += o.remaining();
            }
        }
        for (Deque<LimitOrder> q : asks.values()) {
            for (LimitOrder o : q) {
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

        for (Deque<LimitOrder> q : book.values()) {
            for (LimitOrder o : q) {
                notional += o.price() * o.remaining();
                volume   += o.remaining();
            }
        }

        if (volume == 0.0) {
            throw new IllegalStateException("Cannot compute VWAP on empty " + side + " side");
        }
        return notional / volume;
    }

    @Override
    public ExecutionReport placeOrder(Order order) {
        if (!(order instanceof LimitOrder)) {
            throw new IllegalArgumentException("LimitOrderBook only accepts LimitOrders");
        }
        LimitOrder lo = (LimitOrder) order;

        List<Trade> trades = new ArrayList<>();

        if (lo.side() == Direction.Buy) {
            // --- MATCH BUY ORDER AGAINST ASKS ---
            while (!asks.isEmpty() && !lo.isDone() && lo.price() >= getBestAsk()) {
                double bestAsk = getBestAsk();
                Deque<LimitOrder> ordersAtLevel = asks.get(bestAsk);

                while (!lo.isDone() && !ordersAtLevel.isEmpty()) {
                    // Get maker at head of queue
                    LimitOrder maker = ordersAtLevel.pollFirst();

                    double fill = Math.min(lo.remaining(), maker.remaining());
                    trades.add(new Trade(lo.id(), maker.id(), maker.price(), fill));

                    // Update taker and maker (records -> new instances)
                    lo = lo.consume(fill);
                    maker = maker.consume(fill);

                    // If maker still has remaining, put back at head of queue (FIFO)
                    if (!maker.isDone()) {
                        ordersAtLevel.addFirst(maker);
                    }
                }

                if (ordersAtLevel.isEmpty()) {
                    asks.remove(bestAsk);
                }
            }
        } else {
            // --- MATCH SELL ORDER AGAINST BIDS ---
            while (!bids.isEmpty() && !lo.isDone() && lo.price() <= getBestBid()) {
                double bestBid = getBestBid();
                Deque<LimitOrder> ordersAtLevel = bids.get(bestBid);

                while (!lo.isDone() && !ordersAtLevel.isEmpty()) {
                    // Get maker at head of queue
                    LimitOrder maker = ordersAtLevel.pollFirst();

                    double fill = Math.min(lo.remaining(), maker.remaining());
                    trades.add(new Trade(lo.id(), maker.id(), maker.price(), fill));

                    // Update taker and maker (records -> new instances)
                    lo = lo.consume(fill);
                    maker = maker.consume(fill);

                    // If maker still has remaining, put back at head of queue (FIFO)
                    if (!maker.isDone()) {
                        ordersAtLevel.addFirst(maker);
                    }
                }

                if (ordersAtLevel.isEmpty()) {
                    bids.remove(bestBid);
                }
            }
        }

        if (!lo.isDone()) {
            addOrder(lo);
        }

        if (trades.isEmpty()) {
            return new ExecutionReport(trades, 0.0, 0.0);
        }

        double filledQty = trades.stream().mapToDouble(Trade::quantity).sum();
        double notional  = trades.stream().mapToDouble(t -> t.price() * t.quantity()).sum();
        double vwap      = filledQty > 0 ? notional / filledQty : 0.0;

        return new ExecutionReport(trades, filledQty, vwap);
    }


    public record BookLevel(double price, double totalQuantity) {}
}
