package com.simulator.agents;

import com.simulator.engine.OrderBook;
import com.simulator.model.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * LobAgent
 *
 * A market-making bot that places random limit orders.
 * Each bot has access to ALL symbol OrderBooks and randomly picks
 * which stock to trade on each iteration, so the same 10 bots
 * provide liquidity across all 5 symbols simultaneously.
 *
 * ── MULTI-STOCK CHANGE ─────────────────────────────────────────────────────
 *   Constructor now accepts Map<String, OrderBook> instead of a single book.
 *   Each sleep cycle: pick a random symbol, then place an order on its book.
 *
 * ── ORDER LOGIC (unchanged) ────────────────────────────────────────────────
 *   70% passive  → price offset away from mid (adds liquidity)
 *   30% aggressive→ price offset into the spread (takes liquidity)
 *   Price jitter  ~ Gaussian(0, 0.50) around lastTradedPrice
 */
public class LobAgent implements Runnable {

    private final String                   botName;
    private final Map<String, OrderBook>   markets;     // all symbol books
    private final List<String>             symbols;     // stable list for random pick
    private final Random                   random = new Random();

    public LobAgent(String botName, Map<String, OrderBook> markets) {
        this.botName  = botName;
        this.markets  = markets;
        this.symbols  = new ArrayList<>(markets.keySet());
    }

    @Override
    public void run() {
        System.out.println(botName + " has entered the market (" + symbols.size() + " symbols).");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(random.nextInt(2000) + 500);

                // ── Pick a random symbol this iteration ─────────────────────
                String    symbol = symbols.get(random.nextInt(symbols.size()));
                OrderBook market = markets.get(symbol);
                if (market == null) continue;

                // ── Order parameters ────────────────────────────────────────
                boolean isBuy       = random.nextBoolean();
                int     quantity    = random.nextInt(15) + 1;
                double  currentPrice = market.getLastTradedPrice();
                double  priceOffset = random.nextGaussian() * 0.50;
                boolean isAggressive = random.nextInt(100) < 30;

                double orderPrice;
                if (isBuy) {
                    orderPrice = isAggressive
                            ? currentPrice + Math.abs(priceOffset)   // cross spread
                            : currentPrice - Math.abs(priceOffset);  // passive bid
                } else {
                    orderPrice = isAggressive
                            ? currentPrice - Math.abs(priceOffset)   // cross spread
                            : currentPrice + Math.abs(priceOffset);  // passive ask
                }

                orderPrice = Math.max(0.01, orderPrice);

                Order botOrder = new Order(isBuy, symbol, quantity, orderPrice, botName);
                market.addOrder(botOrder);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println(botName + " is shutting down.");
                break;
            }
        }
    }
}