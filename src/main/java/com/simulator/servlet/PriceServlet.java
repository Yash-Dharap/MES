package com.simulator.servlet;

import com.simulator.engine.OrderBook;

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
 * PriceServlet  GET /PriceServlet
 *
 * Returns current last-traded prices for all symbols (or a single symbol).
 *
 * ── MULTI-STOCK CHANGE ─────────────────────────────────────────────────────
 *   No ?symbol param → returns JSON object with all symbols.
 *   With ?symbol=AAPL → returns the single price as plain text (backward compat).
 *
 * ── RESPONSE (no symbol param) ─────────────────────────────────────────────
 *   { "AAPL": 185.32, "GOOGL": 174.21, "MSFT": 419.55, "AMZN": 184.90, "TSLA": 173.44 }
 *
 * ── RESPONSE (with ?symbol=AAPL) ───────────────────────────────────────────
 *   185.32      (plain text, 2 decimal places)
 */
@WebServlet("/PriceServlet")
public class PriceServlet extends HttpServlet {

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Map<String, OrderBook> orderBooks =
                (Map<String, OrderBook>) getServletContext().getAttribute("orderBooks");

        String symbolParam = request.getParameter("symbol");

        // ── Single-symbol mode (backward compatible) ───────────────────────
        if (symbolParam != null && !symbolParam.isBlank()) {
            response.setContentType("text/plain; charset=UTF-8");
            double price = 100.00;
            if (orderBooks != null) {
                OrderBook book = orderBooks.get(symbolParam.trim().toUpperCase());
                if (book != null) price = book.getLastTradedPrice();
            }
            response.getWriter().write(String.format(Locale.US, "%.2f", price));
            return;
        }

        // ── All-symbols mode (default) ─────────────────────────────────────
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        out.print("{");

        if (orderBooks != null) {
            // Respect the insertion order of the map (LinkedHashMap in MarketInitializer)
            List<String> symbols = (List<String>) getServletContext().getAttribute("symbols");
            if (symbols == null) symbols = orderBooks.keySet().stream().toList();

            for (int i = 0; i < symbols.size(); i++) {
                String sym   = symbols.get(i);
                OrderBook bk = orderBooks.get(sym);
                double price = (bk != null) ? bk.getLastTradedPrice() : 0.0;
                out.printf(Locale.US, "\"%s\":%.4f", sym, price);
                if (i < symbols.size() - 1) out.print(",");
            }
        }

        out.print("}");
        out.flush();
    }
}