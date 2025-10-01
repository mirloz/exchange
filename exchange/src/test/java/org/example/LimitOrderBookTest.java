package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LimitOrderBookTest {

    private LimitOrderBook book;

    @BeforeEach
    void setUp() {
        // fresh defensive copy of DEMO_BOOK for each test
        book = new LimitOrderBook(LimitOrderBook.DEMO_BOOK);
    }

    @Test
    void testBestBidAndAsk() {
        assertEquals(100.00, book.getBestBid(), 1e-6);
        assertEquals(101.00, book.getBestAsk(), 1e-6);
    }

    @Test
    void testOrdersAtPrice() {
        Deque<LimitOrder> ordersAt101 = book.getOrdersAtPrice(101.00);
        assertEquals(2, ordersAt101.size());
        assertEquals(500, ordersAt101.peekFirst().remaining());
    }

    @Test
    void testBidLevelsAggregation() {
        List<LimitOrderBook.BookLevel> bids = book.getBidLevels();

        // top bid is 100.00 with 500+200 = 700
        LimitOrderBook.BookLevel top = bids.get(0);
        assertEquals(100.00, top.price(), 1e-6);
        assertEquals(700.0, top.totalQuantity(), 1e-6);
    }

    @Test
    void testAskLevelsAggregation() {
        List<LimitOrderBook.BookLevel> asks = book.getAskLevels();

        // top ask is 101.00 with 500+200 = 700
        LimitOrderBook.BookLevel top = asks.get(0);
        assertEquals(101.00, top.price(), 1e-6);
        assertEquals(700.0, top.totalQuantity(), 1e-6);
    }

    @Test
    void testVwapSide() {
        double bidVWAP = book.vwapSide(Direction.Buy);
        double askVWAP = book.vwapSide(Direction.Sell);

        assertTrue(bidVWAP > 96.0 && bidVWAP < 101.0, "Bid VWAP reasonable");
        assertTrue(askVWAP > 101.0 && askVWAP < 106.0, "Ask VWAP reasonable");
    }

    @Test
    void testVwapBook() {
        double vwap = book.vwapBook();
        assertTrue(vwap > 98.0 && vwap < 104.0, "Book VWAP reasonable");
    }
}
