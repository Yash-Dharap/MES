package com.simulator.engine;

import com.simulator.db.TradeRepository;
import com.simulator.model.Order;
import com.simulator.model.Portfolio;

/**
 * MatchingEngine
 *
 * One instance per symbol. Continuously matches the best buy order against
 * the best sell order for its assigned OrderBook.
 * Runs in its own daemon thread (started by MarketInitializer).
 *
 * ── MATCHING RULE ──────────────────────────────────────────────────────────
 *   Trade executes when: bestBid.price >= bestAsk.price
 *   Trade price        = bestAsk.price  (price-time priority)
 *   Trade quantity     = min(buyQty, sellQty)  (partial fills supported)
 *
 * ── MULTI-STOCK CHANGE ─────────────────────────────────────────────────────
 *   Portfolio.buy/sell now require a symbol argument.
 *   The symbol is read from orderBook.getSymbol() – no hardcoding.
 */
public class MatchingEngine implements Runnable {

    private final OrderBook orderBook;
    private final Portfolio humanPortfolio;

    public MatchingEngine(OrderBook orderBook, Portfolio humanPortfolio) {
        this.orderBook      = orderBook;
        this.humanPortfolio = humanPortfolio;
    }

    @Override
    public void run() {
        System.out.printf("⚙️  MatchingEngine [%s] started%n", orderBook.getSymbol());

        while (!Thread.currentThread().isInterrupted()) {
            try {
                matchOrders();
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.printf("⚙️  MatchingEngine [%s] shutting down%n", orderBook.getSymbol());
                break;
            }
        }
    }

    // ── Core Matching Logic ────────────────────────────────────────────────────

    private void matchOrders() {
        Order bestBuy  = orderBook.getBuyOrders().peek();
        Order bestSell = orderBook.getSellOrders().peek();

        if (bestBuy == null || bestSell == null || bestBuy.getPrice() < bestSell.getPrice()) {
            return;
        }

        int    tradeQuantity;
        double tradePrice;
        String buyerName;
        String sellerName;
        String symbol = orderBook.getSymbol();

        synchronized (orderBook) {
            bestBuy  = orderBook.getBuyOrders().peek();
            bestSell = orderBook.getSellOrders().peek();

            if (bestBuy == null || bestSell == null || bestBuy.getPrice() < bestSell.getPrice()) {
                return;
            }

            bestBuy  = orderBook.getBuyOrders().poll();
            bestSell = orderBook.getSellOrders().poll();

            tradeQuantity = Math.min(bestBuy.getQuantity(), bestSell.getQuantity());
            tradePrice    = bestSell.getPrice();
            buyerName     = bestBuy.getTraderName();
            sellerName    = bestSell.getTraderName();

            // ── Update human portfolio (symbol-aware) ───────────────────────
            if ("Human Player".equals(buyerName) && humanPortfolio != null) {
                humanPortfolio.buy(symbol, tradeQuantity, tradePrice);
            }
            if ("Human Player".equals(sellerName) && humanPortfolio != null) {
                humanPortfolio.sell(symbol, tradeQuantity, tradePrice);
            }

            orderBook.setLastTradedPrice(tradePrice);

            // ── Partial fills ───────────────────────────────────────────────
            bestBuy.setQuantity(bestBuy.getQuantity()   - tradeQuantity);
            bestSell.setQuantity(bestSell.getQuantity() - tradeQuantity);

            if (bestBuy.getQuantity()  > 0) orderBook.getBuyOrders().put(bestBuy);
            if (bestSell.getQuantity() > 0) orderBook.getSellOrders().put(bestSell);
        }

        System.out.printf(">>> TRADE [%s]: %d shares @ $%.2f  (%s ← %s)%n",
                symbol, tradeQuantity, tradePrice, buyerName, sellerName);

        TradeRepository.saveTrade(symbol, tradeQuantity, tradePrice, buyerName, sellerName);
    }
}