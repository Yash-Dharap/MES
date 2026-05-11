package com.simulator.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * AlphaVantageClient
 *
 * Integrates with the Alpha Vantage REST API to fetch real stock data.
 *
 * ── HOW TO GET A FREE API KEY ──────────────────────────────────────────────
 *   1. Go to: https://www.alphavantage.co/support/#api-key
 *   2. Sign up for free (no credit card needed)
 *   3. Copy your API key and paste it into API_KEY below
 *
 * ── FREE TIER LIMITS ───────────────────────────────────────────────────────
 *   25 requests/day, 5 requests/minute
 *   This app only calls the API ONCE at startup, so the free tier is enough.
 *
 * ── ENDPOINTS USED ─────────────────────────────────────────────────────────
 *   GLOBAL_QUOTE      → current price (called at startup to seed the market)
 *   TIME_SERIES_DAILY → recent daily OHLCV data (used for market context)
 *
 * ── FALLBACK ───────────────────────────────────────────────────────────────
 *   All methods return sensible defaults (-1.0 / empty array) if the API
 *   is unreachable or the key is not set, so the simulator always starts.
 */
public class AlphaVantageClient {

    // ⚠️  Replace this with your free key from https://www.alphavantage.co/support/#api-key
    private static final String API_KEY  = "BZ5KGYR1043BBZIY";
    private static final String BASE_URL = "https://www.alphavantage.co/query";

    // HTTP timeouts (ms) – short so a bad network doesn't stall app startup
    private static final int CONNECT_TIMEOUT = 5_000;
    private static final int READ_TIMEOUT    = 7_000;

    // ─── Public API ────────────────────────────────────────────────────────────

    /**
     * Fetches the latest real-time price for a stock symbol.
     *
     * Endpoint: GLOBAL_QUOTE
     * Response field used: "05. price"
     *
     * @param symbol e.g. "AAPL"
     * @return live price as a double, or -1.0 on any failure
     */
    public static double fetchLatestPrice(String symbol) {
        if ("YOUR_API_KEY_HERE".equals(API_KEY)) {
            System.out.println("⚠️  Alpha Vantage API key not set – using default seed price.");
            return -1.0;
        }

        try {
            String urlStr = BASE_URL
                    + "?function=GLOBAL_QUOTE"
                    + "&symbol=" + symbol
                    + "&apikey=" + API_KEY;

            String json = httpGet(urlStr);
            if (json == null) return -1.0;

            // Lightweight manual parse – avoids a JSON library dependency
            // Response format: { "Global Quote": { "05. price": "182.6300", ... } }
            String priceKey = "\"05. price\": \"";
            int idx = json.indexOf(priceKey);

            if (idx == -1) {
                System.err.println("[AlphaVantage] 'price' field not found. " +
                        "Daily limit reached? Full response: " + json.substring(0, Math.min(200, json.length())));
                return -1.0;
            }

            int start = idx + priceKey.length();
            int end   = json.indexOf("\"", start);
            double price = Double.parseDouble(json.substring(start, end));

            System.out.printf("🌐 Alpha Vantage: live price for %s = $%.2f%n", symbol, price);
            return price;

        } catch (NumberFormatException e) {
            System.err.println("[AlphaVantage] Could not parse price value: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[AlphaVantage] API call failed: " + e.getMessage());
        }

        return -1.0;
    }

    /**
     * Fetches the last N daily closing prices for a symbol.
     * Used to provide a historical price context to the bots (optional).
     *
     * Endpoint: TIME_SERIES_DAILY (compact output = last 100 trading days)
     * Response field used: "4. close"
     *
     * @param symbol e.g. "AAPL"
     * @param count  how many days to return (max 5 recommended for free tier speed)
     * @return array of closing prices oldest-to-newest, or empty array on failure
     */
    public static double[] fetchRecentDailyCloses(String symbol, int count) {
        if ("YOUR_API_KEY_HERE".equals(API_KEY)) return new double[0];

        try {
            String urlStr = BASE_URL
                    + "?function=TIME_SERIES_DAILY"
                    + "&symbol=" + symbol
                    + "&outputsize=compact"
                    + "&apikey=" + API_KEY;

            String json = httpGet(urlStr);
            if (json == null) return new double[0];

            // Extract "4. close" values in order of appearance (newest first in the JSON)
            String closeKey = "\"4. close\": \"";
            java.util.List<Double> prices = new java.util.ArrayList<>();

            int searchFrom = 0;
            while (prices.size() < count) {
                int idx = json.indexOf(closeKey, searchFrom);
                if (idx == -1) break;
                int start = idx + closeKey.length();
                int end   = json.indexOf("\"", start);
                prices.add(Double.parseDouble(json.substring(start, end)));
                searchFrom = end;
            }

            // Reverse so oldest comes first
            java.util.Collections.reverse(prices);
            return prices.stream().mapToDouble(Double::doubleValue).toArray();

        } catch (Exception e) {
            System.err.println("[AlphaVantage] fetchRecentDailyCloses failed: " + e.getMessage());
        }

        return new double[0];
    }

    // ─── Internal HTTP Helper ──────────────────────────────────────────────────

    private static String httpGet(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestProperty("User-Agent", "StockMarketSimulator/1.0");

            int status = conn.getResponseCode();
            if (status != 200) {
                System.err.println("[AlphaVantage] HTTP " + status + " from API");
                return null;
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                return sb.toString();
            }

        } catch (Exception e) {
            System.err.println("[AlphaVantage] HTTP request failed: " + e.getMessage());
            return null;
        }
    }
}
