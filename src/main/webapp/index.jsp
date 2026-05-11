<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Stock Market Simulator</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        :root {
            --bg:         #0d1117;
            --surface:    #161b22;
            --surface2:   #1c2128;
            --border:     #30363d;
            --text:       #c9d1d9;
            --muted:      #8b949e;
            --green:      #3fb950;
            --red:        #f85149;
            --blue:       #58a6ff;
            --yellow:     #d29922;
            --glow-green: rgba(63, 185, 80, 0.35);
            --glow-red:   rgba(248, 81, 73, 0.35);
            --glow-blue:  rgba(88, 166, 255, 0.35);
        }

        * { box-sizing: border-box; margin: 0; padding: 0; }

        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
            background: var(--bg);
            color: var(--text);
            min-height: 100vh;
        }

        /* =========================================
           --- LANDING SCREEN (TERMINAL BOOT) ---
           ========================================= */
        #landing-screen {
            position: fixed;
            top: 0; left: 0; width: 100%; height: 100%;
            background: radial-gradient(circle at center, #161b22 0%, #0d1117 100%);
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            z-index: 1000;
            transition: opacity 0.6s cubic-bezier(0.16, 1, 0.3, 1), transform 0.6s ease;
        }

        .landing-grid {
            position: absolute; top: 0; left: 0; right: 0; bottom: 0;
            background-image: 
                linear-gradient(rgba(48, 54, 61, 0.2) 1px, transparent 1px),
                linear-gradient(90deg, rgba(48, 54, 61, 0.2) 1px, transparent 1px);
            background-size: 30px 30px;
            z-index: 0;
            opacity: 0.5;
        }

        .boot-container {
            position: relative;
            z-index: 1;
            width: 100%;
            max-width: 600px;
            background: rgba(13, 17, 23, 0.85);
            border: 1px solid var(--border);
            border-radius: 8px;
            padding: 40px;
            box-shadow: 0 0 40px rgba(0,0,0,0.8);
            backdrop-filter: blur(4px);
        }

        .landing-title {
            font-size: 2rem; font-weight: 800; letter-spacing: 1px;
            color: var(--text); margin-bottom: 24px;
            display: flex; align-items: center; gap: 12px;
        }

        .boot-log {
            font-family: "Courier New", Courier, monospace;
            font-size: 0.95rem;
            color: var(--muted);
            margin-bottom: 8px;
            opacity: 0;
            transform: translateY(10px);
        }
        
        .log-1 { animation: bootFade 0.1s ease forwards 0.3s; }
        .log-2 { animation: bootFade 0.1s ease forwards 0.8s; }
        .log-3 { animation: bootFade 0.1s ease forwards 1.2s; }
        .log-4 { animation: bootFade 0.1s ease forwards 1.6s; }
        
        .log-highlight { color: var(--green); font-weight: bold; }

        @keyframes bootFade {
            to { opacity: 1; transform: translateY(0); }
        }

        .btn-proceed {
            margin-top: 32px;
            background: transparent;
            color: var(--blue);
            border: 1px solid var(--blue);
            padding: 12px 28px;
            border-radius: 4px;
            font-family: "Courier New", Courier, monospace;
            font-size: 1.1rem; font-weight: 700;
            cursor: pointer;
            opacity: 0;
            animation: bootFade 0.5s ease forwards 2.2s;
            transition: all 0.2s ease;
            width: 100%;
            text-transform: uppercase;
            letter-spacing: 2px;
        }
        .btn-proceed:hover {
            background: rgba(88, 166, 255, 0.1);
            box-shadow: 0 0 15px var(--glow-blue);
            text-shadow: 0 0 8px var(--blue);
        }

        /* =========================================
           --- DASHBOARD SCREEN ---
           ========================================= */
        #dashboard-screen {
            display: none; 
            padding: 20px;
        }

        header {
            max-width: 1200px; margin: 0 auto 24px;
            display: flex; justify-content: space-between; align-items: center;
            border-bottom: 1px solid var(--border); padding-bottom: 16px;
        }
        .header-left h1 { font-size: 1.3rem; font-weight: 700; letter-spacing: -0.3px; }
        .header-left span { font-size: 0.85rem; color: var(--muted); }
        
        .status-badge {
            display: flex; align-items: center; gap: 8px;
            background: rgba(63, 185, 80, 0.1); border: 1px solid rgba(63, 185, 80, 0.3);
            padding: 6px 14px; border-radius: 20px;
            font-size: 0.82rem; color: var(--green); font-weight: 600;
        }
        .dot { 
            width: 8px; height: 8px; border-radius: 50%; background: var(--green);
            box-shadow: 0 0 8px var(--green); animation: pulse 2s infinite;
        }
        @keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }

        .layout { 
            max-width: 1200px; margin: 0 auto; display: grid;
            grid-template-columns: 1fr 1fr 1fr; gap: 16px;
        }

        .card {
            background: var(--surface); border: 1px solid var(--border);
            border-radius: 10px; padding: 20px;
        }
        .card-title {
            font-size: 0.78rem; font-weight: 600; letter-spacing: 0.8px;
            color: var(--muted); text-transform: uppercase; margin-bottom: 16px;
        }

        .stock-selector {
            width: 100%; padding: 10px 12px;
            background: var(--surface2); color: var(--text);
            border: 1px solid var(--border); border-radius: 6px;
            font-size: 1rem; font-weight: 600; margin-bottom: 16px;
            outline: none; cursor: pointer;
        }
        .stock-selector:focus { border-color: var(--blue); }

        .ticker-card { grid-column: 1 / 2; }
        .ticker-symbol { font-size: 0.9rem; color: var(--muted); margin-bottom: 4px; }
        #price-display {
            font-size: 3.8rem; font-weight: 700; font-variant-numeric: tabular-nums;
            line-height: 1; margin-bottom: 8px; transition: color 0.3s, text-shadow 0.3s;
        }
        .price-up   { color: var(--green); text-shadow: 0 0 20px var(--glow-green); }
        .price-down { color: var(--red);   text-shadow: 0 0 20px var(--glow-red); }
        .price-flat { color: var(--text); }
        #price-change { font-size: 1rem; font-weight: 600; }
        
        .stats-row { display: flex; gap: 20px; margin-top: 16px; border-top: 1px solid var(--border); padding-top: 14px; }
        .stat { flex: 1; }
        .stat-label { font-size: 0.72rem; color: var(--muted); margin-bottom: 2px; }
        .stat-value { font-size: 0.95rem; font-weight: 600; }

        .trade-card { grid-column: 2 / 3; }
        .input-row { display: flex; gap: 10px; margin-bottom: 12px; }
        input[type=number] {
            flex: 1; background: var(--bg); color: var(--text);
            border: 1px solid var(--border); padding: 10px 12px;
            border-radius: 6px; font-size: 0.9rem; outline: none; transition: border-color 0.2s;
        }
        input[type=number]:focus { border-color: var(--blue); }
        .btn-row { display: flex; gap: 10px; }
        button {
            flex: 1; padding: 11px; border: none; border-radius: 6px;
            font-weight: 700; font-size: 0.9rem; cursor: pointer; transition: transform 0.1s, opacity 0.2s;
        }
        button:active { transform: scale(0.97); }
        .btn-buy  { background: var(--green); color: #0d1117; }
        .btn-sell { background: var(--red);   color: #fff; }
        
        #trade-status {
            margin-top: 12px; padding: 8px 12px; border-radius: 6px;
            font-size: 0.85rem; font-weight: 600; text-align: center; display: none;
        }
        .status-ok   { background: rgba(63,185,80,0.15);  color: var(--green); display:block!important; }
        .status-fail { background: rgba(248,81,73,0.15);  color: var(--red);   display:block!important; }

        .portfolio-card { grid-column: 3 / 4; }
        .pnl-big {
            font-size: 2rem; font-weight: 700; font-variant-numeric: tabular-nums;
            margin: 8px 0 4px; transition: color 0.3s;
        }
        .pnl-label { font-size: 0.8rem; color: var(--muted); }

        .chart-card { grid-column: 1 / 3; }
        .chart-wrap { position: relative; height: 260px; }

        .orders-card { grid-column: 3 / 4; }
        #orders-list { max-height: 260px; overflow-y: auto; }
        .order-row {
            display: flex; justify-content: space-between; align-items: center;
            padding: 7px 0; border-bottom: 1px solid var(--border); font-size: 0.85rem;
        }
        .order-row:last-child { border-bottom: none; }
        .order-type { font-weight: 700; width: 40px; }
        .order-qty  { color: var(--muted); flex: 1; text-align: center; }
        .order-price { font-variant-numeric: tabular-nums; }
        .empty-msg  { color: var(--muted); font-size: 0.85rem; text-align: center; padding: 20px 0; }

        .history-card { grid-column: 1 / -1; }
        .history-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
        .badge { background: var(--surface2); border: 1px solid var(--border); padding: 3px 10px; border-radius: 12px; font-size: 0.75rem; color: var(--muted); }
        .history-stats { display: flex; gap: 24px; margin-bottom: 14px; }
        .h-stat-label { font-size: 0.72rem; color: var(--muted); margin-bottom: 1px; }
        .h-stat-value { font-size: 0.95rem; font-weight: 600; }
        table { width: 100%; border-collapse: collapse; font-size: 0.85rem; }
        thead th { text-align: left; font-size: 0.72rem; font-weight: 600; color: var(--muted); text-transform: uppercase; padding: 0 10px 8px; border-bottom: 1px solid var(--border); }
        tbody tr { transition: background 0.15s; }
        tbody tr:hover { background: var(--surface2); }
        tbody td { padding: 8px 10px; border-bottom: 1px solid var(--border); }
        tbody tr:last-child td { border-bottom: none; }
        .human-row td { background: rgba(88,166,255,0.05); }
        .human-row:hover td { background: rgba(88,166,255,0.1); }
        .tbl-scroll { max-height: 300px; overflow-y: auto; }

        @media (max-width: 900px) {
            .layout { grid-template-columns: 1fr 1fr; }
            .ticker-card   { grid-column: 1 / 2; }
            .trade-card    { grid-column: 2 / 3; }
            .portfolio-card{ grid-column: 1 / 3; }
            .chart-card    { grid-column: 1 / 2; }
            .orders-card   { grid-column: 2 / 3; }
            .history-card  { grid-column: 1 / 3; }
        }
        @media (max-width: 600px) {
            .layout { grid-template-columns: 1fr; }
            .ticker-card,.trade-card,.portfolio-card,.chart-card,.orders-card,.history-card { grid-column: 1 / 2; }
        }
    </style>
</head>
<body>

<div id="landing-screen">
    <div class="landing-grid"></div>
    <div class="boot-container">
        <h1 class="landing-title">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color:var(--blue)"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline></svg>
            Market Engine Simulator
        </h1>
        
        <div class="boot-log log-1">> Booting simulation environment...</div>
        <div class="boot-log log-2">> Establishing database pool... <span class="log-highlight">[OK] H2 Connected</span></div>
        <div class="boot-log log-3">> Seeding OrderBooks... <span class="log-highlight">[OK] Markets Loaded</span></div>
        <div class="boot-log log-4">> Spawning AI liquidity providers... <span class="log-highlight">[OK] Agents Active</span></div>
        
        <button class="btn-proceed" onclick="enterMarket()">[ Initialize Terminal ]</button>
    </div>
</div>

<div id="dashboard-screen">
    <header>
        <div class="header-left">
            <h1>&#x1F4C8; Market Engine Terminal</h1>
            <span>Simulated LOB &bull; Multi-Stock &bull; Autonomous Liquidity</span>
        </div>
        <div class="status-badge"><div class="dot"></div> ENGINE ONLINE</div>
    </header>

    <div class="layout">

        <div class="card ticker-card">
            <div class="card-title">Select Market</div>
            <select id="stockSelector" onchange="changeStock()" class="stock-selector">
                <option value="AAPL">AAPL - Apple</option>
                <option value="MSFT">MSFT - Microsoft</option>
                <option value="GOOGL">GOOGL - Google</option>
                <option value="AMZN">AMZN - Amazon</option>
                <option value="TSLA">TSLA - Tesla</option>
            </select>

            <div class="ticker-symbol" id="ticker-display-name">AAPL (SIMULATED)</div>
            <div id="price-display" class="price-flat">$&mdash;</div>
            <div id="price-change" style="color:var(--muted)">Fetching...</div>
            
            <div class="stats-row">
                <div class="stat">
                    <div class="stat-label">Day High</div>
                    <div class="stat-value" id="stat-high" style="color:var(--green)">&mdash;</div>
                </div>
                <div class="stat">
                    <div class="stat-label">Day Low</div>
                    <div class="stat-value" id="stat-low" style="color:var(--red)">&mdash;</div>
                </div>
                <div class="stat">
                    <div class="stat-label">VWAP</div>
                    <div class="stat-value" id="stat-vwap" style="color:var(--blue)">&mdash;</div>
                </div>
            </div>
        </div>

        <div class="card trade-card">
            <div class="card-title">Execute Trade</div>
            <div class="input-row">
                <input type="number" id="trade-qty"   placeholder="Qty (e.g. 10)" min="1" step="1">
                <input type="number" id="trade-price" placeholder="Price (e.g. 185.50)" step="0.01">
            </div>
            <div class="btn-row">
                <button class="btn-buy"  onclick="submitTrade('buy')">&#9650; BUY</button>
                <button class="btn-sell" onclick="submitTrade('sell')">&#9660; SELL</button>
            </div>
            <div id="trade-status"></div>
        </div>

        <div class="card portfolio-card">
            <div class="card-title">My Portfolio (All Holdings)</div>
            <div class="pnl-label">Total Unrealized P&amp;L</div>
            <div class="pnl-big" id="pnl-display">$0.00</div>
            
            <div class="tbl-scroll" style="margin-top: 15px; border-top: 1px solid var(--border); padding-top: 10px; max-height: 200px;">
                <table style="width: 100%; text-align: left; font-size: 0.85rem;">
                    <thead>
                        <tr>
                            <th style="padding-bottom: 8px; color: var(--muted);">Asset</th>
                            <th style="padding-bottom: 8px; color: var(--muted);">Qty</th>
                            <th style="padding-bottom: 8px; color: var(--muted);">Avg Price</th>
                            <th style="padding-bottom: 8px; color: var(--muted); text-align: right;">P&amp;L</th>
                        </tr>
                    </thead>
                    <tbody id="portfolio-positions">
                        <tr><td colspan="4" style="text-align:center;color:var(--muted);padding:20px;">No open positions</td></tr>
                    </tbody>
                </table>
            </div>
        </div>

        <div class="card chart-card">
            <div class="card-title" style="margin-bottom:10px;">Price History</div>
            <div class="chart-wrap"><canvas id="priceChart"></canvas></div>
        </div>

        <div class="card orders-card">
            <div class="card-title">My Open Orders</div>
            <div id="orders-list"><div class="empty-msg">No pending orders</div></div>
        </div>

        <div class="card history-card">
            <div class="history-header">
                <div class="card-title" style="margin-bottom:0;">Market Trade History</div>
                <span class="badge" id="trade-count-badge">0 trades</span>
            </div>
            <div class="history-stats">
                <div class="h-stat">
                    <div class="h-stat-label">Total Trades</div>
                    <div class="h-stat-value" id="hist-count">0</div>
                </div>
                <div class="h-stat">
                    <div class="h-stat-label">Total Volume</div>
                    <div class="h-stat-value" id="hist-volume">0 shares</div>
                </div>
                <div class="h-stat">
                    <div class="h-stat-label">VWAP</div>
                    <div class="h-stat-value" id="hist-vwap-bottom" style="color:var(--blue)">$0.00</div>
                </div>
            </div>
            <div class="tbl-scroll">
                <table>
                    <thead>
                        <tr>
                            <th>#</th>
                            <th>Symbol</th>
                            <th>Qty</th>
                            <th>Price</th>
                            <th>Buyer</th>
                            <th>Seller</th>
                            <th>Time</th>
                        </tr>
                    </thead>
                    <tbody id="trade-table-body">
                        <tr><td colspan="7" style="text-align:center;color:var(--muted);padding:20px;">Waiting for trades...</td></tr>
                    </tbody>
                </table>
            </div>
        </div>

    </div></div><script>
// ---
//  Global State
// ---
let currentSymbol = 'AAPL'; // Default active stock
let lastPrice = null;
let chartColor = '#3fb950';
let pollIntervals = [];

// ---
//  Landing Page Logic
// ---
function enterMarket() {
    const landing = document.getElementById('landing-screen');
    landing.style.transform = 'scale(1.1)';
    landing.style.opacity = '0';
    
    setTimeout(() => {
        landing.style.display = 'none';
        document.getElementById('dashboard-screen').style.display = 'block';
        startPolling();
    }, 600);
}

// ---
//  Stock Selector Logic
// ---
function changeStock() {
    currentSymbol = document.getElementById('stockSelector').value;
    document.getElementById('ticker-display-name').textContent = currentSymbol + ' (SIMULATED)';
    
    // Reset local data
    lastPrice = null; 
    document.getElementById('price-change').textContent = 'Fetching...';
    document.getElementById('price-change').style.color = 'var(--muted)';
    
    // Reset Chart
    priceChart.data.labels = [];
    priceChart.data.datasets[0].data = [];
    priceChart.data.datasets[0].label = currentSymbol;
    priceChart.update();

    // Clear Trade History UI temporarily
    document.getElementById('trade-table-body').innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--muted);padding:20px;">Fetching ' + currentSymbol + ' trades...</td></tr>';

    pollAll(); 
}

// ---
//  Chart Setup
// ---
const ctx = document.getElementById('priceChart').getContext('2d');
const priceChart = new Chart(ctx, {
    type: 'line',
    data: {
        labels: [],
        datasets: [{
            label: currentSymbol,
            data: [],
            borderColor: '#3fb950',
            backgroundColor: 'rgba(63,185,80,0.08)',
            borderWidth: 1.5,
            pointRadius: 0,
            fill: true,
            tension: 0.3
        }]
    },
    options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 300 },
        interaction: { mode: 'index', intersect: false },
        scales: {
            x: { display: false },
            y: { grid: { color: '#21262d' }, ticks: { color: '#8b949e', callback: v => '$' + v.toFixed(2) } }
        },
        plugins: {
            legend: { display: false },
            tooltip: {
                backgroundColor: '#161b22', borderColor: '#30363d', borderWidth: 1,
                titleColor: '#8b949e', bodyColor: '#c9d1d9',
                callbacks: { label: ctx => ' $' + ctx.parsed.y.toFixed(2) }
            }
        }
    }
});

function setChartColor(color) {
    if (chartColor === color) return;
    chartColor = color;
    priceChart.data.datasets[0].borderColor = color;
    priceChart.data.datasets[0].backgroundColor = color === '#3fb950' ? 'rgba(63,185,80,0.08)' : 'rgba(248,81,73,0.08)';
}

// ---
//  Polling Functions
// ---
function pollPrice() {
    fetch('PriceServlet?symbol=' + currentSymbol)
        .then(r => r.text())
        .then(txt => {
            const p = parseFloat(txt);
            if(isNaN(p)) return;

            const el = document.getElementById('price-display');
            const chEl = document.getElementById('price-change');

            if (lastPrice !== null) {
                const diff = p - lastPrice;
                const pct  = lastPrice !== 0 ? (diff / lastPrice * 100) : 0;
                if (diff > 0) {
                    el.className = 'price-up';
                    chEl.style.color = 'var(--green)';
                    chEl.textContent = '+$' + diff.toFixed(2) + ' (+' + pct.toFixed(2) + '%)';
                    setChartColor('#3fb950');
                } else if (diff < 0) {
                    el.className = 'price-down';
                    chEl.style.color = 'var(--red)';
                    chEl.textContent = '-$' + Math.abs(diff).toFixed(2) + ' (' + pct.toFixed(2) + '%)';
                    setChartColor('#f85149');
                } else {
                    el.className = 'price-flat';
                }
            } else {
                chEl.textContent = 'Market open';
                chEl.style.color = 'var(--muted)';
            }

            el.textContent = '$' + p.toFixed(2);
            lastPrice = p;
        })
        .catch(() => {});
}

function pollPriceHistory() {
    fetch('PriceHistoryServlet?symbol=' + currentSymbol + '&limit=120')
        .then(r => r.json())
        .then(data => {
            if(!data.history) return;
            const labels = data.history.map(p => new Date(p.time.replace(' ', 'T')).toLocaleTimeString());
            const prices = data.history.map(p => p.price);

            priceChart.data.labels  = labels;
            priceChart.data.datasets[0].data = prices;
            priceChart.update('none');

            if (data.dayHigh > 0) {
                document.getElementById('stat-high').textContent = '$' + data.dayHigh.toFixed(2);
                document.getElementById('stat-low').textContent  = '$' + data.dayLow.toFixed(2);
            }
        })
        .catch(() => {});
}

function pollPortfolio() {
    fetch('PortfolioServlet')
        .then(r => r.json())
        .then(d => {
            const pnlEl = document.getElementById('pnl-display');
            const totalPnl = d.totalPnl || 0;
            pnlEl.textContent = (totalPnl >= 0 ? '+$' : '-$') + Math.abs(totalPnl).toFixed(2);
            pnlEl.style.color = totalPnl > 0 ? 'var(--green)' : totalPnl < 0 ? 'var(--red)' : 'var(--text)';

            const tbody = document.getElementById('portfolio-positions');
            const positions = d.positions || [];
            
            if (positions.length === 0) {
                tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;color:var(--muted);padding:20px;">No open positions</td></tr>';
                return;
            }

            tbody.innerHTML = positions.map(pos => {
                const pnlColor = pos.unrealizedPnl > 0 ? 'var(--green)' : pos.unrealizedPnl < 0 ? 'var(--red)' : 'var(--text)';
                const pnlSign = pos.unrealizedPnl > 0 ? '+' : '';
                return `
                    <tr>
                        <td style="font-weight: 700; color: var(--blue); padding-bottom: 6px;">${pos.symbol}</td>
                        <td style="padding-bottom: 6px;">${pos.shares}</td>
                        <td style="font-variant-numeric:tabular-nums; padding-bottom: 6px;">$${pos.avgPrice.toFixed(2)}</td>
                        <td style="text-align: right; color: ${pnlColor}; font-weight: 600; font-variant-numeric:tabular-nums; padding-bottom: 6px;">
                            ${pnlSign}$${pos.unrealizedPnl.toFixed(2)}
                        </td>
                    </tr>
                `;
            }).join('');
        })
        .catch(() => {});
}

function pollOpenOrders() {
    // Correctly fetching open orders specifically for the selected symbol
    fetch('OpenOrdersServlet?symbol=' + currentSymbol)
        .then(r => r.text())
        .then(text => {
            const el = document.getElementById('orders-list');
            let orders;
            try { orders = JSON.parse(text); } catch(e) { return; }
            
            if (!Array.isArray(orders) || orders.length === 0) {
                el.innerHTML = '<div class="empty-msg">No pending orders</div>';
                return;
            }
            
            const valid = orders.filter(o => o && o.type && o.symbol === currentSymbol);
            if (valid.length === 0) {
                el.innerHTML = '<div class="empty-msg">No pending orders for ' + currentSymbol + '</div>';
                return;
            }
            
            el.innerHTML = valid.map(o => {
                const color = o.type === 'BUY' ? 'var(--green)' : 'var(--red)';
                const price = parseFloat(o.price).toFixed(2);
                return `<div class="order-row">
                    <span class="order-type" style="color:${color}">${o.type}</span>
                    <span class="order-qty">${o.qty} shares</span>
                    <span class="order-price">$${price}</span>
                </div>`;
            }).join('');
        })
        .catch(() => {});
}

function pollTradeHistory() {
    fetch('TradeHistoryServlet?symbol=' + currentSymbol + '&limit=25')
        .then(r => r.text())
        .then(text => {
            let data;
            try { data = JSON.parse(text); } catch(e) { return; }

            const count  = data.totalCount  || 0;
            const volume = data.totalVolume || 0;
            const vwap   = parseFloat(data.vwap) || 0;

            document.getElementById('hist-count').textContent        = count.toLocaleString();
            document.getElementById('hist-volume').textContent       = volume.toLocaleString() + ' shares';
            document.getElementById('hist-vwap-bottom').textContent  = '$' + vwap.toFixed(2);
            document.getElementById('stat-vwap').textContent         = '$' + vwap.toFixed(2);
            document.getElementById('trade-count-badge').textContent = count + ' trades';

            const trades = data.trades || [];
            if (trades.length === 0) {
                document.getElementById('trade-table-body').innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--muted);padding:20px;">No trades yet for ' + currentSymbol + '...</td></tr>';
                return;
            }

            const rows = trades.map(t => {
                if (!t || t.id === undefined) return '';
                const isHuman = t.buyer === 'Human Player' || t.seller === 'Human Player';
                const rowClass = isHuman ? 'human-row' : '';
                const timeStr = t.time ? t.time.substring(11, 19) : '--';
                const price = parseFloat(t.price) || 0;
                const buyerStyle = t.buyer === 'Human Player' ? 'color:var(--blue);font-weight:700' : '';
                const sellerStyle = t.seller === 'Human Player' ? 'color:var(--blue);font-weight:700' : '';

                return `<tr class="${rowClass}">
                    <td style="color:var(--muted)">#${t.id}</td>
                    <td>${t.symbol || ''}</td>
                    <td>${t.quantity || ''}</td>
                    <td style="font-variant-numeric:tabular-nums">$${price.toFixed(2)}</td>
                    <td style="${buyerStyle}">${t.buyer || ''}</td>
                    <td style="${sellerStyle}">${t.seller || ''}</td>
                    <td style="color:var(--muted)">${timeStr}</td>
                </tr>`;
            }).join('');

            document.getElementById('trade-table-body').innerHTML = rows;
        })
        .catch(() => {});
}

// ---
//  Submit Trade
// ---
function submitTrade(action) {
    const qty   = parseFloat(document.getElementById('trade-qty').value);
    const price = parseFloat(document.getElementById('trade-price').value);
    const st    = document.getElementById('trade-status');

    if (isNaN(qty) || isNaN(price) || qty <= 0 || price <= 0) {
        st.textContent = '⚠ Enter a valid quantity and price.';
        st.className = 'status-fail';
        return;
    }

    const params = new URLSearchParams();
    params.append('symbol', currentSymbol);
    params.append('action', action);
    params.append('quantity', Math.floor(qty).toString());
    params.append('price', price.toFixed(2));

    fetch('TradeServlet', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params.toString()
    })
    .then(r => r.text().then(msg => ({ ok: r.ok, msg })))
    .then(({ ok, msg }) => {
        st.innerHTML = ok ? '&#10003; ' + msg : '&#10007; ' + msg;
        st.className = ok ? 'status-ok' : 'status-fail';
        if (ok) {
            document.getElementById('trade-qty').value   = '';
            document.getElementById('trade-price').value = '';
            
            // Force an immediate update of open orders to feel more responsive
            pollOpenOrders();
        }
    })
    .catch(() => {
        st.textContent = '❌ Network error.';
        st.className = 'status-fail';
    });
}

// ---
//  Startup Polling Controller
// ---
function pollAll() {
    pollPrice();
    pollPriceHistory();
    pollPortfolio();
    pollOpenOrders();
    pollTradeHistory();
}

function startPolling() {
    pollAll(); 
    pollIntervals.push(setInterval(pollPrice, 1000));
    pollIntervals.push(setInterval(pollPriceHistory, 5000));
    pollIntervals.push(setInterval(pollPortfolio, 1500));
    pollIntervals.push(setInterval(pollOpenOrders, 1500));
    pollIntervals.push(setInterval(pollTradeHistory, 3000));
}
</script>
</body>
</html>