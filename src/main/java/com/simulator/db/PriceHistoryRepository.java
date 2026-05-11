package com.simulator.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PriceHistoryRepository
 *
 * Data-access layer for `price_history` and `portfolio_snapshots` tables.
 *
 * ── MULTI-STOCK CHANGE ─────────────────────────────────────────────────────
 *   savePortfolioSnapshot() and getPortfolioPnlHistory() now accept a
 *   `symbol` parameter so each stock's P&L history is stored and queried
 *   independently.
 */
public class PriceHistoryRepository {

    // ── Price History ──────────────────────────────────────────────────────────

    /** Saves one price snapshot for a symbol. Called every 5s by SnapshotScheduler. */
    public static void savePrice(String symbol, double price) {
        String sql = "INSERT INTO price_history (symbol, price) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, symbol);
            ps.setDouble(2, price);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[PriceHistoryRepo] Failed to save price: " + e.getMessage());
        }
    }

    /**
     * Returns up to `limit` price snapshots for a symbol, oldest-first.
     * Inner subquery grabs newest N rows (DESC), outer flips to ASC for charting.
     */
    public static List<Map<String, Object>> getPriceHistory(String symbol, int limit) {
        List<Map<String, Object>> history = new ArrayList<>();

        String sql = """
            SELECT price, recorded_at
            FROM (
                SELECT price, recorded_at
                FROM price_history
                WHERE symbol = ?
                ORDER BY recorded_at DESC
                LIMIT ?
            ) sub
            ORDER BY recorded_at ASC
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, symbol);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("price", rs.getDouble("price"));
                point.put("time",  rs.getTimestamp("recorded_at").toString());
                history.add(point);
            }

        } catch (SQLException e) {
            System.err.println("[PriceHistoryRepo] Failed to fetch price history: " + e.getMessage());
        }

        return history;
    }

    /** Returns {max, min} price across all history for the given symbol. */
    public static double[] getDayHighLow(String symbol) {
        String sql = "SELECT MAX(price), MIN(price) FROM price_history WHERE symbol = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, symbol);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new double[]{ rs.getDouble(1), rs.getDouble(2) };
            }

        } catch (SQLException e) {
            System.err.println("[PriceHistoryRepo] Failed to fetch high/low: " + e.getMessage());
        }

        return new double[]{ 0.0, 0.0 };
    }

    // ── Portfolio Snapshots ────────────────────────────────────────────────────

    /**
     * Saves a per-symbol snapshot of the human portfolio's current state.
     * unrealizedPnl = (lastTradedPrice − avgCost) × sharesOwned
     *
     * @param symbol          the stock symbol this snapshot is for
     * @param sharesOwned     current shares held in that symbol
     * @param avgPrice        average cost basis for that symbol
     * @param lastTradedPrice current market price
     */
    public static void savePortfolioSnapshot(String symbol, int sharesOwned,
                                              double avgPrice, double lastTradedPrice) {
        double unrealizedPnl = sharesOwned > 0
                ? (lastTradedPrice - avgPrice) * sharesOwned
                : 0.0;

        String sql = """
            INSERT INTO portfolio_snapshots
                (symbol, shares_owned, avg_price, last_traded_price, unrealized_pnl)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, symbol);
            ps.setInt(2, sharesOwned);
            ps.setDouble(3, avgPrice);
            ps.setDouble(4, lastTradedPrice);
            ps.setDouble(5, unrealizedPnl);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[PriceHistoryRepo] Failed to save portfolio snapshot: " + e.getMessage());
        }
    }

    /**
     * Returns the last N unrealized-P&L snapshots for a specific symbol,
     * oldest-first (for charting).
     *
     * @param symbol  filter snapshots to this symbol
     * @param limit   max rows to return
     */
    public static List<Map<String, Object>> getPortfolioPnlHistory(String symbol, int limit) {
        List<Map<String, Object>> history = new ArrayList<>();

        String sql = """
            SELECT unrealized_pnl, snapshot_time
            FROM (
                SELECT unrealized_pnl, snapshot_time
                FROM portfolio_snapshots
                WHERE symbol = ?
                ORDER BY snapshot_time DESC
                LIMIT ?
            ) sub
            ORDER BY snapshot_time ASC
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, symbol);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("pnl",  rs.getDouble("unrealized_pnl"));
                point.put("time", rs.getTimestamp("snapshot_time").toString());
                history.add(point);
            }

        } catch (SQLException e) {
            System.err.println("[PriceHistoryRepo] Failed to fetch P&L history: " + e.getMessage());
        }

        return history;
    }

    /**
     * Returns the latest unrealized P&L snapshot for each symbol.
     * Useful for a portfolio summary panel showing all positions at once.
     *
     * Returns Map<symbol, latestUnrealizedPnl>
     */
    public static Map<String, Double> getLatestPnlPerSymbol() {
        Map<String, Double> result = new LinkedHashMap<>();

        String sql = """
            SELECT symbol, unrealized_pnl
            FROM portfolio_snapshots ps1
            WHERE snapshot_time = (
                SELECT MAX(snapshot_time)
                FROM portfolio_snapshots ps2
                WHERE ps2.symbol = ps1.symbol
            )
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.put(rs.getString("symbol"), rs.getDouble("unrealized_pnl"));
            }

        } catch (SQLException e) {
            System.err.println("[PriceHistoryRepo] Failed to fetch latest P&L per symbol: " + e.getMessage());
        }

        return result;
    }
}