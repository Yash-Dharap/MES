package com.simulator.servlet;

import com.simulator.db.TradeRepository;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * TradeHistoryServlet  GET /TradeHistoryServlet
 *
 * Returns a JSON object containing:
 *   {
 *     "totalCount": 1234,          // all-time executed trade count
 *     "totalVolume": 56789,        // all-time shares traded
 *     "vwap": 183.42,              // volume-weighted average price
 *     "trades": [                  // N most recent trades (newest first)
 *       {
 *         "id": 42,
 *         "symbol": "AAPL",
 *         "quantity": 7,
 *         "price": 185.30,
 *         "buyer": "Bot-3",
 *         "seller": "Human Player",
 *         "time": "2025-01-15 10:23:45.0"
 *       }, ...
 *     ]
 *   }
 *
 * Query parameter:
 *   ?limit=N   how many recent trades to return (default: 25, max: 100)
 */
@WebServlet("/TradeHistoryServlet")
public class TradeHistoryServlet extends HttpServlet {

    private static final int DEFAULT_LIMIT = 25;
    private static final int MAX_LIMIT     = 100;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        // Parse ?limit parameter
        int limit = DEFAULT_LIMIT;
        String limitParam = request.getParameter("limit");
        if (limitParam != null) {
            try {
                limit = Math.min(Integer.parseInt(limitParam), MAX_LIMIT);
            } catch (NumberFormatException ignored) { /* use default */ }
        }

        // Fetch from DB
        List<Map<String, Object>> trades = TradeRepository.getRecentTrades(limit);
        int    totalCount  = TradeRepository.getTotalTradeCount();
        long   totalVolume = TradeRepository.getTotalVolume();
        double vwap        = TradeRepository.getVwap();

        // Build JSON manually (no library dependency)
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"totalCount\":").append(totalCount).append(",");
        json.append("\"totalVolume\":").append(totalVolume).append(",");
        json.append("\"vwap\":").append(String.format(Locale.US, "%.4f", vwap)).append(",");
        json.append("\"trades\":[");

        for (int i = 0; i < trades.size(); i++) {
            Map<String, Object> t = trades.get(i);
            json.append("{");
            json.append("\"id\":").append(t.get("id")).append(",");
            json.append("\"symbol\":\"").append(escape(t.get("symbol"))).append("\",");
            json.append("\"quantity\":").append(t.get("quantity")).append(",");
            json.append("\"price\":").append(String.format(Locale.US, "%.4f", t.get("price"))).append(",");
            json.append("\"buyer\":\"").append(escape(t.get("buyer"))).append("\",");
            json.append("\"seller\":\"").append(escape(t.get("seller"))).append("\",");
            json.append("\"time\":\"").append(escape(t.get("time"))).append("\"");
            json.append("}");
            if (i < trades.size() - 1) json.append(",");
        }

        json.append("]}");

        PrintWriter writer = response.getWriter();
        writer.write(json.toString());
        writer.flush();
    }

    /** Minimal JSON string escaping – handles the trader names we use. */
    private String escape(Object value) {
        if (value == null) return "";
        return value.toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}