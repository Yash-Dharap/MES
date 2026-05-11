package com.simulator.servlet;

import com.simulator.engine.OrderBook;
import com.simulator.model.Portfolio;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

@WebServlet("/PortfolioServlet")
public class PortfolioServlet extends HttpServlet {
    
    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        Portfolio portfolio = (Portfolio) getServletContext().getAttribute("myPortfolio");
        Map<String, OrderBook> orderBooks = (Map<String, OrderBook>) getServletContext().getAttribute("orderBooks");

        // If nothing is loaded yet, return an empty layout
        if (portfolio == null || orderBooks == null) {
            response.getWriter().write("{\"totalPnl\":0, \"positions\":[]}");
            return;
        }

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"positions\": [");

        Set<String> symbols = portfolio.getTrackedSymbols();
        double totalUnrealizedPnl = 0.0;
        boolean first = true;

        // Loop through every stock you have ever traded
        for (String sym : symbols) {
            int shares = portfolio.getSharesOwned(sym);
            if (shares > 0) {
                OrderBook book = orderBooks.get(sym);
                double currentPrice = (book != null) ? book.getLastTradedPrice() : 0.0;
                double avgPrice = portfolio.getAveragePrice(sym);
                double unrealizedPnl = portfolio.getUnrealizedPnl(sym, currentPrice);
                double marketValue = portfolio.getMarketValue(sym, currentPrice);
                
                totalUnrealizedPnl += unrealizedPnl;

                if (!first) json.append(",");
                
                // Build the JSON for this specific stock
                json.append(String.format(
                    "{\"symbol\":\"%s\", \"shares\":%d, \"avgPrice\":%.4f, \"marketPrice\":%.4f, \"marketValue\":%.4f, \"unrealizedPnl\":%.4f}",
                    sym, shares, avgPrice, currentPrice, marketValue, unrealizedPnl
                ));
                first = false;
            }
        }
        
        json.append("],");
        json.append(String.format("\"totalPnl\":%.4f", totalUnrealizedPnl));
        json.append("}");

        response.getWriter().write(json.toString());
    }
}