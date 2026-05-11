package com.simulator;

import com.simulator.api.AlphaVantageClient;
import com.simulator.db.DatabaseManager;
import com.simulator.db.PriceHistoryRepository;
import com.simulator.engine.MatchingEngine;
import com.simulator.engine.OrderBook;
import com.simulator.agents.LobAgent;
import com.simulator.model.Portfolio;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.*;
import java.util.concurrent.*;

/**
 * MarketInitializer  (ServletContextListener)
 *
 * Boots the multi-stock trading system on web application start.
 *
 * ── STARTUP SEQUENCE ───────────────────────────────────────────────────────
 *   1.  Initialize HikariCP + H2 database (creates schema if needed)
 *   2.  Fetch live prices for all 5 symbols from Alpha Vantage API
 *       (falls back to hardcoded defaults if API unavailable)
 *   3.  Create one OrderBook per symbol
 *   4.  Create one Portfolio for the human player (multi-symbol)
 *   5.  Start one MatchingEngine thread per symbol (5 threads)
 *   6.  Start 10 LobAgent bot threads (each trades all 5 symbols)
 *   7.  Start SnapshotScheduler – records price + portfolio state every 5s
 *   8.  Publish shared objects to ServletContext
 *
 * ── SHUTDOWN SEQUENCE ──────────────────────────────────────────────────────
 *   Interrupt all engine threads → shutdown snapshot scheduler → close DB pool
 *
 * ── ALPHA VANTAGE RATE LIMIT ───────────────────────────────────────────────
 *   Free tier: 5 requests/minute.  We call it once per symbol at startup.
 *   Fetches are sequential with a short delay; failed fetches fall back to
 *   the DEFAULT_PRICES map so the simulator always starts cleanly.
 */
@WebListener
public class MarketInitializer implements ServletContextListener {

    // ── Symbols traded ─────────────────────────────────────────────────────────
    public static final List<String> SYMBOLS = List.of("AAPL", "GOOGL", "MSFT", "AMZN", "TSLA");

    /** Sensible fallback prices if Alpha Vantage is unavailable. */
    private static final Map<String, Double> DEFAULT_PRICES = Map.of(
            "AAPL",  185.00,
            "GOOGL", 175.00,
            "MSFT",  420.00,
            "AMZN",  185.00,
            "TSLA",  175.00
    );

    // ── Lifecycle handles for clean shutdown ───────────────────────────────────
    private final List<Thread>       engineThreads     = new ArrayList<>();
    private       ScheduledExecutorService snapshotScheduler;

    // ── Startup ────────────────────────────────────────────────────────────────

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("========================================");
        System.out.println("  Waking up the Multi-Stock Simulator  ");
        System.out.println("  Symbols: " + SYMBOLS);
        System.out.println("========================================");

        // 1. Database first
        DatabaseManager.initialize();

        // 2. Fetch live prices for all symbols (sequential, best-effort)
        Map<String, OrderBook> orderBooks = new LinkedHashMap<>();

        for (String symbol : SYMBOLS) {
            double price = AlphaVantageClient.fetchLatestPrice(symbol);
            if (price <= 0) {
                price = DEFAULT_PRICES.getOrDefault(symbol, 100.00);
                System.out.printf("⚠️  [%s] Using default seed price: $%.2f%n", symbol, price);
            }
            orderBooks.put(symbol, new OrderBook(symbol, price));

            // Respect Alpha Vantage free-tier rate limit (5 req/min)
            // Sleep between fetches so we don't blow the quota.
            // Comment this out if you have a paid key.
            try { Thread.sleep(12_000); } catch (InterruptedException ignored) {}
        }

        // 3. Single human portfolio (tracks positions in all symbols)
        Portfolio myPortfolio = new Portfolio();

        // 4. One MatchingEngine per symbol
        for (String symbol : SYMBOLS) {
            MatchingEngine engine = new MatchingEngine(orderBooks.get(symbol), myPortfolio);
            Thread t = new Thread(engine, "MatchingEngine-" + symbol);
            t.setDaemon(true);
            t.start();
            engineThreads.add(t);
        }

        // 5. 10 LobAgent bots – each bot has access to all books and picks
        //    a random symbol on every trade iteration
        for (int i = 1; i <= 10; i++) {
            LobAgent bot = new LobAgent("Bot-" + i, orderBooks);
            Thread t = new Thread(bot, "LobAgent-" + i);
            t.setDaemon(true);
            t.start();
        }

        // 6. Snapshot scheduler – price + portfolio state for all symbols every 5s
        snapshotScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SnapshotScheduler");
            t.setDaemon(true);
            return t;
        });

        snapshotScheduler.scheduleAtFixedRate(() -> {
            try {
                for (String symbol : SYMBOLS) {
                    OrderBook book = orderBooks.get(symbol);
                    if (book == null) continue;
                    double currentPrice = book.getLastTradedPrice();

                    // Price snapshot → price_history table
                    PriceHistoryRepository.savePrice(symbol, currentPrice);

                    // Portfolio snapshot → portfolio_snapshots table (per symbol)
                    PriceHistoryRepository.savePortfolioSnapshot(
                            symbol,
                            myPortfolio.getSharesOwned(symbol),
                            myPortfolio.getAveragePrice(symbol),
                            currentPrice
                    );
                }
            } catch (Exception e) {
                System.err.println("[Snapshot] Error: " + e.getMessage());
            }
        }, 5, 5, TimeUnit.SECONDS);

        // 7. Publish shared objects to ServletContext
        sce.getServletContext().setAttribute("orderBooks",   orderBooks);   // Map<String,OrderBook>
        sce.getServletContext().setAttribute("myPortfolio",  myPortfolio);  // Portfolio (multi-symbol)
        sce.getServletContext().setAttribute("symbols",      SYMBOLS);      // List<String>

        System.out.println("========================================");
        System.out.println("  Market OPEN  |  " + SYMBOLS.size() + " symbols active");
        System.out.println("  H2 Console → jdbc:h2:./stockmarket   ");
        System.out.println("========================================");
    }

    // ── Shutdown ───────────────────────────────────────────────────────────────

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("=== Shutting down Multi-Stock Market ===");

        if (snapshotScheduler != null) {
            snapshotScheduler.shutdownNow();
        }

        for (Thread t : engineThreads) {
            if (t != null) t.interrupt();
        }

        DatabaseManager.shutdown();

        System.out.println("=== Market CLOSED ===");
    }
}