package com.simulator.servlet;

import com.simulator.engine.OrderBook;
import com.simulator.model.Order;
import com.simulator.model.Portfolio;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * TradeServlet  POST /TradeServlet
 *
 * Places a limit order for the human player into the correct symbol's OrderBook.
 *
 * ── MULTI-STOCK CHANGE ─────────────────────────────────────────────────────
 *   Now reads a `symbol` parameter (default: AAPL) and routes to the
 *   corresponding OrderBook from the "orderBooks" map in ServletContext.
 *
 * ── REQUEST PARAMETERS ─────────────────────────────────────────────────────
 *   symbol   (optional) e.g. "AAPL", "GOOGL" – defaults to "AAPL"
 *   action   "buy" or "sell"
 *   quantity integer > 0
 *   price    decimal > 0
 *
 * ── RESPONSE ───────────────────────────────────────────────────────────────
 *   200 text/html  – confirmation message
 *   400            – validation error with description
 *   500            – market offline
 */
@WebServlet("/TradeServlet")
public class TradeServlet extends HttpServlet {

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        try {
            // ── Read parameters ────────────────────────────────────────────
            String symbolParam = request.getParameter("symbol");
            String actionParam = request.getParameter("action");
            String qtyParam    = request.getParameter("quantity");
            String priceParam  = request.getParameter("price");

            System.out.println("--- INCOMING TRADE REQUEST ---");
            System.out.printf("Symbol: '%s' | Action: '%s' | Qty: '%s' | Price: '%s'%n",
                    symbolParam, actionParam, qtyParam, priceParam);

            // ── Validate required fields ───────────────────────────────────
            if (actionParam == null || qtyParam == null || priceParam == null
                    || actionParam.isBlank() || qtyParam.isBlank() || priceParam.isBlank()) {
                response.setStatus(400);
                response.getWriter().write("❌ Missing required parameters: action, quantity, price.");
                return;
            }

            // ── Parse ──────────────────────────────────────────────────────
            String symbol   = (symbolParam != null && !symbolParam.isBlank())
                                ? symbolParam.trim().toUpperCase()
                                : "AAPL";
            String action   = actionParam.trim();
            int    quantity = Integer.parseInt(qtyParam.trim());
            double price    = Double.parseDouble(priceParam.trim().replace(",", "."));
            boolean isBuy   = "buy".equalsIgnoreCase(action);

            if (quantity <= 0 || price <= 0) {
                response.setStatus(400);
                response.getWriter().write("❌ Quantity and price must be positive.");
                return;
            }

            // ── Sell validation: must own shares ───────────────────────────
            if (!isBuy) {
                Portfolio portfolio = (Portfolio) getServletContext().getAttribute("myPortfolio");
                if (portfolio == null || portfolio.getSharesOwned(symbol) == 0) {
                    response.setStatus(400);
                    response.getWriter().write(
                            "❌ No " + symbol + " shares to sell. Your current holding is 0.");
                    return;
                }
            }

            // ── Route to the correct OrderBook ─────────────────────────────
            Map<String, OrderBook> orderBooks =
                    (Map<String, OrderBook>) getServletContext().getAttribute("orderBooks");

            if (orderBooks == null || !orderBooks.containsKey(symbol)) {
                response.setStatus(400);
                response.getWriter().write("❌ Unknown symbol: " + symbol);
                return;
            }

            OrderBook market = orderBooks.get(symbol);

            // ── Place order ────────────────────────────────────────────────
            Order humanOrder = new Order(isBuy, symbol, quantity, price, "Human Player");
            market.addOrder(humanOrder);

            System.out.println(">>> SUCCESSFUL ORDER: " + humanOrder);
            response.getWriter().write(String.format(
                    "Order placed: %s %d %s @ $%.2f",
                    action.toUpperCase(), quantity, symbol, price));

        } catch (NumberFormatException e) {
            response.setStatus(400);
            response.getWriter().write("❌ Invalid number format: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ CRASH IN TRADESERVLET:");
            e.printStackTrace();
            response.setStatus(400);
            response.getWriter().write("Invalid trade parameters.");
        }
    }
}