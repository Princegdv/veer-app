package com.veer.assistant.plugins;

import android.content.Context;
import com.veer.assistant.core.Command;
import com.veer.assistant.trade.BinanceClient;
import com.veer.assistant.trade.AngelOneClient;

public class TradePlugin implements Plugin {
    @Override public String name() { return "trade"; }
    @Override public boolean canHandle(Command c) {
        return "TRADE_PRICE".equals(c.action) || "TRADE_PORTFOLIO".equals(c.action);
    }
    @Override public String handle(Context ctx, Command c) {
        if ("TRADE_PRICE".equals(c.action)) {
            String exch = c.arg(0);
            String sym = c.arg(1);
            if (exch == null || "auto".equals(exch)) {
                // Crypto symbols के लिए Binance, stocks के लिए Angel One
                if (isCryptoSymbol(sym)) return BinanceClient.price(sym);
                return AngelOneClient.price(ctx, sym);
            }
            if ("binance".equals(exch)) return BinanceClient.price(sym);
            if (exch.startsWith("angel")) return AngelOneClient.price(ctx, sym);
            return "Exchange samajh nahi aaya.";
        }
        if ("TRADE_PORTFOLIO".equals(c.action)) {
            StringBuilder sb = new StringBuilder();
            sb.append(BinanceClient.portfolio(ctx)).append("\n");
            sb.append(AngelOneClient.portfolio(ctx));
            return sb.toString();
        }
        return "?";
    }
    private boolean isCryptoSymbol(String sym) {
        String[] crypto = {"BTC","ETH","BNB","SOL","XRP","ADA","DOGE","MATIC","DOT","LTC","AVAX","LINK","USDT","SHIB","TRX"};
        for (String c : crypto) if (c.equalsIgnoreCase(sym)) return true;
        return false;
    }
}
