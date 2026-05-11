package com.simulator.engine;

import com.simulator.model.Order;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * OrderBook
 *
 * One instance per traded symbol. Maintains the live bid/ask priority queues
 * and tracks the last traded price for that symbol.
 *
 *   Buy  orders: HIGH → LOW  (best bid at top)
 *   Sell orders: LOW  → HIGH (best ask at top)
 *
 * lastTradedPrice is volatile so MatchingEngine writes are immediately
 * visible to all bot threads reading it without a full synchronized block.
 */
public class OrderBook {

    private final String symbol;

    private final PriorityBlockingQueue<Order> buyOrders = new PriorityBlockingQueue<>(
            100, (o1, o2) -> Double.compare(o2.getPrice(), o1.getPrice())
    );

    private final PriorityBlockingQueue<Order> sellOrders = new PriorityBlockingQueue<>(
            100, (o1, o2) -> Double.compare(o1.getPrice(), o2.getPrice())
    );

    private volatile double lastTradedPrice;

    // ── Constructors ───────────────────────────────────────────────────────────

    /**
     * Primary constructor – used for all 5 symbols at startup.
     *
     * @param symbol    ticker, e.g. "AAPL"
     * @param seedPrice live price fetched from Alpha Vantage (or fallback default)
     */
    public OrderBook(String symbol, double seedPrice) {
        this.symbol          = symbol;
        this.lastTradedPrice = seedPrice;
        System.out.printf("📈 OrderBook [%s] seeded at $%.2f%n", symbol, seedPrice);
    }

    /** Legacy no-arg constructor – defaults to AAPL at $100 (kept for compatibility). */
    public OrderBook() {
        this("AAPL", 100.00);
    }

    /** Legacy single-price constructor – defaults symbol to AAPL. */
    public OrderBook(double seedPrice) {
        this("AAPL", seedPrice);
    }

    // ── Order Submission ───────────────────────────────────────────────────────

    public void addOrder(Order order) {
        if (order.isBuy()) {
            buyOrders.put(order);
        } else {
            sellOrders.put(order);
        }
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    public String                          getSymbol()                      { return symbol; }
    public PriorityBlockingQueue<Order>    getBuyOrders()                   { return buyOrders; }
    public PriorityBlockingQueue<Order>    getSellOrders()                  { return sellOrders; }
    public double                          getLastTradedPrice()             { return lastTradedPrice; }
    public void                            setLastTradedPrice(double price) { this.lastTradedPrice = price; }
}