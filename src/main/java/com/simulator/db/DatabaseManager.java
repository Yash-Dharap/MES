package com.simulator.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseManager
 *
 * Manages the HikariCP connection pool and bootstraps the H2 embedded database.
 *
 * ── MULTI-STOCK CHANGE ─────────────────────────────────────────────────────
 *   portfolio_snapshots now has a `symbol` column so we can store and
 *   retrieve P&L history per-symbol.
 *
 *   If upgrading an existing database, the ALTER TABLE statement below will
 *   add the column if it doesn't already exist (H2 supports IF NOT EXISTS).
 *   If you prefer a clean slate, just delete stockmarket.mv.db before restart.
 *
 * ── DATABASE ───────────────────────────────────────────────────────────────
 *   H2 Embedded, file-based, persists across restarts.
 *   File: ./stockmarket.mv.db  (relative to Tomcat working directory)
 *   H2 Console: http://localhost:8081/h2-console
 *   JDBC URL:   jdbc:h2:./stockmarket
 */
public class DatabaseManager {

    private static HikariDataSource dataSource;

    // ── Initialization ─────────────────────────────────────────────────────────

    public static synchronized void initialize() {
        if (dataSource != null && !dataSource.isClosed()) return;

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:./stockmarket;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1");
        config.setDriverClassName("org.h2.Driver");
        config.setUsername("sa");
        config.setPassword("");

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setPoolName("StockMarketPool");

        dataSource = new HikariDataSource(config);
        System.out.println("✅ HikariCP connection pool initialized.");
        createSchema();
    }

    // ── Schema Bootstrap ───────────────────────────────────────────────────────

    private static void createSchema() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // ── trades ──────────────────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS trades (
                    id          INT AUTO_INCREMENT PRIMARY KEY,
                    symbol      VARCHAR(10)     NOT NULL,
                    quantity    INT             NOT NULL,
                    price       DECIMAL(12, 4)  NOT NULL,
                    buyer       VARCHAR(100)    NOT NULL,
                    seller      VARCHAR(100)    NOT NULL,
                    trade_time  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // ── price_history ────────────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS price_history (
                    id          INT AUTO_INCREMENT PRIMARY KEY,
                    symbol      VARCHAR(10)     NOT NULL,
                    price       DECIMAL(12, 4)  NOT NULL,
                    recorded_at TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // ── portfolio_snapshots (multi-symbol) ───────────────────────────
            //    symbol column added for multi-stock support.
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS portfolio_snapshots (
                    id                  INT AUTO_INCREMENT PRIMARY KEY,
                    symbol              VARCHAR(10)     NOT NULL DEFAULT 'AAPL',
                    shares_owned        INT             NOT NULL,
                    avg_price           DECIMAL(12, 4)  NOT NULL,
                    last_traded_price   DECIMAL(12, 4)  NOT NULL,
                    unrealized_pnl      DECIMAL(12, 4)  NOT NULL,
                    snapshot_time       TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Migration guard: add symbol column to existing databases that lack it
            // H2 supports ADD COLUMN IF NOT EXISTS so this is a no-op on fresh DBs.
            stmt.execute("""
                ALTER TABLE portfolio_snapshots
                    ADD COLUMN IF NOT EXISTS symbol VARCHAR(10) NOT NULL DEFAULT 'AAPL'
            """);

            System.out.println("✅ Database schema ready (trades, price_history, portfolio_snapshots).");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema: " + e.getMessage(), e);
        }
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new IllegalStateException("DatabaseManager not initialized. Call initialize() first.");
        }
        return dataSource.getConnection();
    }

    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("✅ Database connection pool shut down cleanly.");
        }
    }
}