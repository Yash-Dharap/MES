package com.simulator.servlet;

import com.simulator.db.PriceHistoryRepository;

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
 * PriceHistoryServlet  GET /PriceHistoryServlet
 *
 * Returns a JSON object with historical price data from the database.
 * The data is pulled from the `price_history` table which is populated
 * every 5 seconds by the ScheduledExecutor in MarketInitializer.
 *
 * Unlike the in-memory Chart.js data in the old index.jsp (which resets
 * when the page refreshes), this data PERSISTS across browser refreshes
 * and app restarts.
 *
 * Response format:
 *   {
 *     "symbol": "AAPL",
 *     "dayHigh": 190.50,
 *     "dayLow":  180.20,
 *     "count":   72,
 *     "history": [
 *       { "price": 185.00, "time": "2025-01-15 10:00:05.0" },
 *       { "price": 185.30, "time": "2025-01-15 10:00:10.0" },
 *       ...
 *     ]
 *   }
 *
 * Query parameters:
 *   ?symbol=AAPL   stock symbol (default: AAPL)
 *   ?limit=200     max data points to return (default: 120, max: 500)
 */
@WebServlet("/PriceHistoryServlet")
public class PriceHistoryServlet extends HttpServlet {

    private static final int DEFAULT_LIMIT = 120;
    private static final int MAX_LIMIT     = 500;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        // Parameters
        String symbol = request.getParameter("symbol");
        if (symbol == null || symbol.isBlank()) symbol = "AAPL";

        int limit = DEFAULT_LIMIT;
        String limitParam = request.getParameter("limit");
        if (limitParam != null) {
            try {
                limit = Math.min(Integer.parseInt(limitParam), MAX_LIMIT);
            } catch (NumberFormatException ignored) { /* use default */ }
        }

        // Fetch from DB
        List<Map<String, Object>> history = PriceHistoryRepository.getPriceHistory(symbol, limit);
        double[] highLow = PriceHistoryRepository.getDayHighLow(symbol);

        // Build JSON
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"symbol\":\"").append(symbol).append("\",");
        json.append("\"dayHigh\":").append(String.format(Locale.US, "%.4f", highLow[0])).append(",");
        json.append("\"dayLow\":").append(String.format(Locale.US, "%.4f", highLow[1])).append(",");
        json.append("\"count\":").append(history.size()).append(",");
        json.append("\"history\":[");

        for (int i = 0; i < history.size(); i++) {
            Map<String, Object> point = history.get(i);
            json.append("{");
            json.append("\"price\":").append(String.format(Locale.US, "%.4f", point.get("price"))).append(",");
            json.append("\"time\":\"").append(point.get("time")).append("\"");
            json.append("}");
            if (i < history.size() - 1) json.append(",");
        }

        json.append("]}");

        PrintWriter writer = response.getWriter();
        writer.write(json.toString());
        writer.flush();
    }
}