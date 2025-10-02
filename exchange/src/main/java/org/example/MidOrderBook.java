package org.example;

import java.util.*;

public final class MidOrderBook implements Book {

    private final Deque<MidOrder> bids;
    private final Deque<MidOrder> asks;
    private final LimitOrderBook limitOrderBook;

    public MidOrderBook(LimitOrderBook lob) {
        this.bids = new ArrayDeque<>();
        this.asks = new ArrayDeque<>();
        this.limitOrderBook = Objects.requireNonNull(lob, "limitOrderBook");
    }

    public void addOrder(MidOrder o) {
        Objects.requireNonNull(o);
        if (o.side() == Direction.Buy) bids.offer(o);
        else asks.offer(o);
    }

    @Override
    public ExecutionReport placeOrder(Order order) {
        if (!(order instanceof MidOrder)) {
            throw new IllegalArgumentException("MidOrderBook only accepts MidOrders");
        }

        MidOrder mo = (MidOrder) order;

        List<Trade> trades = new ArrayList<>();

        if (mo.side() == Direction.Buy) {
            while (!asks.isEmpty() && !mo.isDone()) {
                MidOrder maker = asks.poll();
                double fill = Math.min(mo.remaining(), maker.remaining());

                trades.add(new Trade(mo.id(), maker.id(), limitOrderBook.getMid(), fill));

                mo = mo.consume(fill);
                maker = maker.consume(fill);

                if (!maker.isDone()) asks.offerFirst(maker);
            }
        } else {
            while (!bids.isEmpty() && !mo.isDone()) {
                MidOrder maker = bids.poll();
                double fill = Math.min(mo.remaining(), maker.remaining());

                trades.add(new Trade(mo.id(), maker.id(), limitOrderBook.getMid(), fill));

                mo = mo.consume(fill);
                maker = maker.consume(fill);

                if (!maker.isDone()) bids.offerFirst(maker);
            }
        }

        if (!mo.isDone()) {
            addOrder(mo);
        }

        if (trades.isEmpty()) {
            return new ExecutionReport(trades, 0.0, 0.0);
        }

        double filledQty = trades.stream().mapToDouble(Trade::quantity).sum();
        double notional  = trades.stream().mapToDouble(t -> t.price() * t.quantity()).sum();
        double vwap      = filledQty > 0 ? notional / filledQty : 0.0;

        return new ExecutionReport(trades, filledQty, vwap);
    }
}
