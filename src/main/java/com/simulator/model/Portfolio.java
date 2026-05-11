package com.simulator.model;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Portfolio
 *
 * Tracks the human player's positions across ALL traded symbols.
 *
 * ── ACCOUNTING: Average Cost ────────────────────────────────────────────────
 *   buy(sym, qty, price)  → totalCost[sym] += qty × price
 *   sell(sym, qty, price) → totalCost[sym] -= qty × avgCost[sym]   (not sale price)
 *
 * ── THREAD SAFETY ───────────────────────────────────────────────────────────
 *   Each symbol's position is stored in a PositionData object.
 *   All public methods synchronize on the relevant PositionData so that
 *   MatchingEngine writes and Servlet reads don't race.
 */
public class Portfolio {

    // ── Inner data class ──────────────────────────────────────────────────────

    private static class PositionData {
        int    sharesOwned = 0;
        double totalCost   = 0.0;
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** One entry per symbol; created lazily on first buy. */
    private final Map<String, PositionData> positions = new ConcurrentHashMap<>();

    // ── Private helper ────────────────────────────────────────────────────────

    private PositionData positionFor(String symbol) {
        return positions.computeIfAbsent(symbol, k -> new PositionData());
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    /**
     * Records a buy execution for the given symbol.
     */
    public void buy(String symbol, int quantity, double price) {
        PositionData p = positionFor(symbol);
        synchronized (p) {
            p.sharesOwned += quantity;
            p.totalCost   += (double) quantity * price;
        }
    }

    /**
     * Records a sell execution for the given symbol.
     * Reduces cost basis by qty × avgCost so the remaining position stays accurate.
     * Clamped to sharesOwned – can never go negative.
     */
    public void sell(String symbol, int quantity, double price) {
        PositionData p = positionFor(symbol);
        synchronized (p) {
            if (p.sharesOwned <= 0) return;

            int    filled  = Math.min(quantity, p.sharesOwned);
            double avgCost = (p.sharesOwned == 0) ? 0.0 : p.totalCost / p.sharesOwned;

            p.sharesOwned -= filled;
            p.totalCost   -= filled * avgCost;

            if (p.sharesOwned == 0) p.totalCost = 0.0; // guard FP drift
        }
    }

    // ── Read-only queries ─────────────────────────────────────────────────────

    public int getSharesOwned(String symbol) {
        PositionData p = positions.get(symbol);
        if (p == null) return 0;
        synchronized (p) { return p.sharesOwned; }
    }

    /** Average cost per share for symbol. Returns 0 if no position held. */
    public double getAveragePrice(String symbol) {
        PositionData p = positions.get(symbol);
        if (p == null) return 0.0;
        synchronized (p) {
            return (p.sharesOwned == 0) ? 0.0 : p.totalCost / p.sharesOwned;
        }
    }

    public double getTotalCost(String symbol) {
        PositionData p = positions.get(symbol);
        if (p == null) return 0.0;
        synchronized (p) { return p.totalCost; }
    }

    /** Unrealized P&L = (marketPrice − avgCost) × sharesOwned */
    public double getUnrealizedPnl(String symbol, double currentMarketPrice) {
        int shares = getSharesOwned(symbol);
        if (shares == 0) return 0.0;
        return (currentMarketPrice - getAveragePrice(symbol)) * shares;
    }

    /** Current market value = marketPrice × sharesOwned */
    public double getMarketValue(String symbol, double currentMarketPrice) {
        return currentMarketPrice * getSharesOwned(symbol);
    }

    /**
     * All symbols that have ever had a position opened.
     * Used by PortfolioServlet to iterate positions.
     */
    public Set<String> getTrackedSymbols() {
        return Collections.unmodifiableSet(positions.keySet());
    }

    /** Total unrealized P&L across all open positions. */
    public double getTotalUnrealizedPnl(Map<String, Double> currentPrices) {
        double total = 0.0;
        for (Map.Entry<String, PositionData> entry : positions.entrySet()) {
            Double price = currentPrices.get(entry.getKey());
            if (price != null) {
                PositionData p = entry.getValue();
                synchronized (p) {
                    if (p.sharesOwned > 0) {
                        double avgCost = p.totalCost / p.sharesOwned;
                        total += (price - avgCost) * p.sharesOwned;
                    }
                }
            }
        }
        return total;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Portfolio{");
        for (Map.Entry<String, PositionData> e : positions.entrySet()) {
            PositionData p = e.getValue();
            synchronized (p) {
                if (p.sharesOwned > 0) {
                    double avg = p.totalCost / p.sharesOwned;
                    sb.append(String.format(" %s=%d@%.2f", e.getKey(), p.sharesOwned, avg));
                }
            }
        }
        sb.append("}");
        return sb.toString();
    }
}