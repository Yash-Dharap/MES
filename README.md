# 📈 Market Engine Simulator (MES)

A multi-stock **Limit Order Book (LOB)** trading simulator built in Java. Trade real equity symbols against autonomous market-making bots, with live price seeding from the Alpha Vantage API and persistent trade history backed by an embedded H2 database.

> Academic project — MIT World Peace University, Computer Engineering
> Team: Yash Dharap · Smera Agrawal · Anvesha Barate · Arya Kadam

---

## ✨ Features

- **5 Live Equity Symbols** — AAPL, GOOGL, MSFT, AMZN, TSLA
- **Limit Order Book Engine** — price-time priority matching with partial fill support
- **10 Autonomous LobAgent Bots** — randomly place buy/sell limit orders across all symbols to simulate real market liquidity
- **Alpha Vantage API Integration** — seeds the market with real current prices at startup (falls back to defaults if API is unavailable)
- **H2 Embedded Database** — zero-setup, file-based persistence for trades, price history, and portfolio snapshots
- **HikariCP Connection Pooling** — efficient, production-grade JDBC connection management
- **Java Servlet Web Interface** — RESTful JSON endpoints for all trading operations
- **Portfolio Tracking** — per-symbol P&L, average cost basis, and shares owned
- **Price Snapshots** — market state recorded every 5 seconds for charting and history

---

## 🏗️ Architecture
┌─────────────────────────────────────────────────────────────┐
│                      Web Browser (JSP)                       │
└────────────────────────┬────────────────────────────────────┘
│ HTTP
┌────────────────────────▼────────────────────────────────────┐
│                   Java Servlet Layer                         │
│  TradeServlet · PortfolioServlet · PriceServlet             │
│  TradeHistoryServlet · OpenOrdersServlet · PriceHistoryServlet│
└────────────────────────┬────────────────────────────────────┘
│
┌────────────────────────▼────────────────────────────────────┐
│                    Market Engine                             │
│  MatchingEngine (×5 threads)  │  LobAgent bots (×10 threads)│
│  OrderBook (×5 symbols)       │  Alpha Vantage API Client   │
└────────────────────────┬────────────────────────────────────┘
│ HikariCP
┌────────────────────────▼────────────────────────────────────┐
│              H2 Embedded Database (stockmarket.mv.db)        │
│  trades · price_history · portfolio_snapshots               │
└─────────────────────────────────────────────────────────────┘

### Component Breakdown

| Component | Role |
|---|---|
| `MarketInitializer` | Boots everything on app startup via `ServletContextListener` |
| `OrderBook` | Per-symbol LOB with sorted buy/sell queues |
| `MatchingEngine` | Daemon thread — matches best bid vs best ask every 10ms |
| `LobAgent` | Bot thread — places random limit orders with Gaussian price jitter |
| `AlphaVantageClient` | Fetches live seed prices at startup |
| `DatabaseManager` | HikariCP pool setup + H2 schema initialization |
| `Portfolio` | Thread-safe per-symbol position and P&L tracking |

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.6+
- (Optional) Free [Alpha Vantage API key](https://www.alphavantage.co/support/#api-key)

### Run Locally

```bash
# Clone the repository
git clone https://github.com/<your-username>/market-engine-simulator.git
cd market-engine-simulator

# Build and run on embedded Tomcat (port 8081)
mvn tomcat7:run
```

Then open your browser at:
http://localhost:8081/stockmarketsimulation

> **Note:** On first startup, the app fetches live prices for all 5 symbols from Alpha Vantage sequentially. Due to the free-tier rate limit (5 requests/minute), this takes about **60 seconds**. The simulator starts cleanly with default prices if the API is unavailable.

### Alpha Vantage API Key (Optional)

The project includes a demo API key. To use your own free key:

1. Sign up at [alphavantage.co](https://www.alphavantage.co/support/#api-key) (no credit card needed)
2. Open `src/main/java/com/simulator/api/AlphaVantageClient.java`
3. Replace the `API_KEY` value with your key

---

## 🌐 API Endpoints

All endpoints are served under `/stockmarketsimulation/` and return JSON.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/trade` | Place a limit buy or sell order |
| `GET` | `/price` | Get current last-traded price for a symbol |
| `GET` | `/portfolio` | Get current portfolio positions and P&L |
| `GET` | `/tradeHistory` | Retrieve trade execution history |
| `GET` | `/openOrders` | List pending unmatched orders |
| `GET` | `/priceHistory` | Fetch price snapshot history for charting |

### Example: Place a Trade

```bash
curl -X POST http://localhost:8081/stockmarketsimulation/trade \
  -d "symbol=AAPL&side=buy&quantity=10&price=185.50"
```

---

## 🗄️ Database Schema

The H2 database file (`stockmarket.mv.db`) is created automatically on first run.

| Table | Columns | Purpose |
|---|---|---|
| `trades` | id, symbol, side, quantity, price, timestamp | Executed trade log |
| `price_history` | id, symbol, price, timestamp | Price snapshots (every 5s) |
| `portfolio_snapshots` | id, symbol, shares, avg_price, current_price, timestamp | Portfolio state history |

Access the H2 web console at `http://localhost:8081/h2-console` with JDBC URL `jdbc:h2:./stockmarket`.

---

## 🤖 How the Bots Work

Ten `LobAgent` threads run continuously in the background. On each iteration (random 500–2500ms sleep), each bot:

1. Picks a **random symbol** from the 5 available
2. Decides buy or sell with **50/50 probability**
3. Sets a price using **Gaussian jitter** (σ = $0.50) around the last traded price
4. Places a **passive order** (70% of the time, adding liquidity) or **aggressive order** (30% of the time, crossing the spread)

This keeps all 5 order books active and ensures there's always liquidity for your trades to match against.

---

## 📦 Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 17 | Core language |
| Maven | 3.6+ | Build and dependency management |
| Apache Tomcat | 7 (embedded) | Web server / Servlet container |
| H2 Database | 2.2.224 | Embedded SQL database |
| HikariCP | 5.1.0 | JDBC connection pooling |
| Alpha Vantage | REST API | Live market data |
| JSP / Servlets | Java EE 4.0 | Web interface |

---

## 📁 Project Structure
src/
└── main/
├── java/com/simulator/
│   ├── MarketInitializer.java
│   ├── agents/
│   │   └── LobAgent.java
│   ├── api/
│   │   └── AlphaVantageClient.java
│   ├── db/
│   │   ├── DatabaseManager.java
│   │   ├── PriceHistoryRepository.java
│   │   └── TradeRepository.java
│   ├── engine/
│   │   ├── MatchingEngine.java
│   │   └── OrderBook.java
│   ├── model/
│   │   ├── Order.java
│   │   └── Portfolio.java
│   └── servlet/
│       ├── TradeServlet.java
│       ├── PortfolioServlet.java
│       ├── PriceServlet.java
│       ├── PriceHistoryServlet.java
│       ├── TradeHistoryServlet.java
│       └── OpenOrdersServlet.java
└── webapp/
├── index.jsp
└── WEB-INF/
└── web.xml

---

## ⚠️ Notes

- The `target/` directory and `stockmarket.mv.db` database file are excluded from version control via `.gitignore`
- The simulator always starts cleanly — if Alpha Vantage is unreachable, sensible default prices are used
- Thread safety: `OrderBook` and `Portfolio` use synchronized access for concurrent bot and user operations

---

## 📄 License
This project was developed for academic purposes at MIT World Peace University. All rights reserved by the authors.



