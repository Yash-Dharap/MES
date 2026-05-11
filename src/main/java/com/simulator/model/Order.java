package com.simulator.model;

public class Order {
    private boolean isBuy; // true for Buy (Bid), false for Sell (Ask)
    private String symbol;
    private int quantity;
    private double price;
    private String traderName;

    public Order(boolean isBuy, String symbol, int quantity, double price, String traderName) {
        this.isBuy = isBuy;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.traderName = traderName;
    }

  
    public boolean isBuy() { return isBuy; }
    public String getSymbol() { return symbol; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public String getTraderName() { return traderName; }

    public void setQuantity(int quantity) { this.quantity = quantity; }

    @Override
    public String toString() {
        String type = isBuy ? "BUY" : "SELL";
        return String.format("[%s] %s %d %s @ $%.2f", traderName, type, quantity, symbol, price);
    }
}