package com.simulator.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TradeRepository
 *
 * Data-access layer for the `trades` table.
 * Called by MatchingEngine every time a trade is executed.
 *
 * All methods are stateless static helpers – they borrow a connection
 * from the HikariCP pool, do their work, and return it automatically
 * via try-with-resources.
 */
public class TradeRepository {

    // ─── Write ─────────────────────────────────────────────────────────────────

    /**
     * Persists a single executed trade to the database.
     * Called by MatchingEngine after each successful match.
     */
    public static void saveTrade(String symbol, int quantity, double price,
                                  String buyer, String seller) {
        String sql = "INSERT INTO trades (symbol, quantity, price, buyer, seller) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, symbol);
            ps.setInt(2, quantity);
            ps.setDouble(3, price);
            ps.setString(4, buyer);
            ps.setString(5, seller);
            ps.executeUpdate();

        } catch (SQLException e) {
            // Log but don't crash the matching engine over a DB write
            System.err.println("[TradeRepository] Failed to save trade: " + e.getMessage());
        }
    }

    // ─── Read ──────────────────────────────────────────────────────────────────

    /**
     * Returns the N most recently executed trades, newest first.
     * Used by TradeHistoryServlet to populate the UI table.
     */
    public static List<Map<String, Object>> getRecentTrades(int limit) {
        List<Map<String, Object>> trades = new ArrayList<>();
        String sql = "SELECT id, symbol, quantity, price, buyer, seller, trade_time " +
                     "FROM trades ORDER BY trade_time DESC LIMIT ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id",       rs.getInt("id"));
                row.put("symbol",   rs.getString("symbol"));
                row.put("quantity", rs.getInt("quantity"));
                row.put("price",    rs.getDouble("price"));
                row.put("buyer",    rs.getString("buyer"));
                row.put("seller",   rs.getString("seller"));
                row.put("time",     rs.getTimestamp("trade_time").toString());
                trades.add(row);
            }

        } catch (SQLException e) {
            System.err.println("[TradeRepository] Failed to fetch trades: " + e.getMessage());
        }

        return trades;
    }

    /**
     * Total number of trades executed since the market started.
     * Displayed in the dashboard header.
     */
    public static int getTotalTradeCount() {
        String sql = "SELECT COUNT(*) FROM trades";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) return rs.getInt(1);

        } catch (SQLException e) {
            System.err.println("[TradeRepository] Failed to count trades: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Total volume (sum of quantities) traded since market open.
     */
    public static long getTotalVolume() {
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM trades";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) return rs.getLong(1);

        } catch (SQLException e) {
            System.err.println("[TradeRepository] Failed to sum volume: " + e.getMessage());
        }

        return 0;
    }

    /**
     * VWAP – Volume Weighted Average Price across all recorded trades.
     * VWAP = SUM(price × qty) / SUM(qty)
     */
    public static double getVwap() {
        String sql = "SELECT SUM(price * quantity) / SUM(quantity) FROM trades WHERE quantity > 0";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                double v = rs.getDouble(1);
                return rs.wasNull() ? 0.0 : v;
            }

        } catch (SQLException e) {
            System.err.println("[TradeRepository] Failed to compute VWAP: " + e.getMessage());
        }

        return 0.0;
    }
}
