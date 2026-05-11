package com.simulator.servlet;

import com.simulator.engine.OrderBook;
import com.simulator.model.Order;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@WebServlet("/OpenOrdersServlet")
public class OpenOrdersServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json; charset=UTF-8");

        String symbol = request.getParameter("symbol");
        if (symbol == null || symbol.isBlank()) symbol = "AAPL";

        @SuppressWarnings("unchecked")
        Map<String, OrderBook> orderBooks =
                (Map<String, OrderBook>) getServletContext().getAttribute("orderBooks");

        List<String> humanOrders = new ArrayList<>();

        if (orderBooks != null && orderBooks.containsKey(symbol)) {
            OrderBook market = orderBooks.get(symbol);
            Order[] buySnapshot  = market.getBuyOrders().toArray(new Order[0]);
            Order[] sellSnapshot = market.getSellOrders().toArray(new Order[0]);

            for (Order o : buySnapshot) {
                if ("Human Player".equals(o.getTraderName())) {
                    // Added the symbol to the JSON output!
                    humanOrders.add(String.format(Locale.US,
                            "{\"symbol\":\"%s\",\"type\":\"BUY\",\"qty\":%d,\"price\":%.2f}",
                            symbol, o.getQuantity(), o.getPrice()));
                }
            }
            for (Order o : sellSnapshot) {
                if ("Human Player".equals(o.getTraderName())) {
                    // Added the symbol to the JSON output!
                    humanOrders.add(String.format(Locale.US,
                            "{\"symbol\":\"%s\",\"type\":\"SELL\",\"qty\":%d,\"price\":%.2f}",
                            symbol, o.getQuantity(), o.getPrice()));
                }
            }
        }

        PrintWriter writer = response.getWriter();
        writer.print("[" + String.join(",", humanOrders) + "]");
        writer.flush();
    }
}