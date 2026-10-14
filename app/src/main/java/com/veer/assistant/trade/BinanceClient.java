package com.veer.assistant.trade;

import android.content.Context;
import com.veer.assistant.core.Logger;
import com.veer.assistant.core.Prefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public final class BinanceClient {
    private static final String BASE = "https://api.binance.com";

    public static String price(String symbol) {
        try {
            String sym = normalize(symbol);
            String url = BASE + "/api/v3/ticker/price?symbol=" + sym + "USDT";
            String json = get(url);
            JSONObject o = new JSONObject(json);
            String p = o.getString("price");
            return "Binance " + symbol.toUpperCase(Locale.ROOT) + ": $" + trim(p);
        } catch (Exception e) {
            Logger.e("Binance", "price fail", e);
            return "Binance price nahi mila (" + symbol + "). Symbol check karo (BTC, ETH, SOL...).";
        }
    }

    public static String portfolio(Context ctx) {
        String key = Prefs.getString("binance_api_key", "");
        String secret = Prefs.getString("binance_secret", "");
        if (key.isEmpty() || secret.isEmpty())
            return "Binance portfolio: API key nahi hai (Settings mein daalo).";
        // Placeholder — signed account endpoint
        return "Binance portfolio: API key mili, par signed request abhi implement nahi hui.";
    }

    private static String normalize(String s) {
        return s.toUpperCase(Locale.ROOT).replace("USDT", "");
    }

    private static String trim(String s) {
        try {
            double d = Double.parseDouble(s);
            if (d >= 1000) return String.format(Locale.US, "%.2f", d);
            if (d >= 1) return String.format(Locale.US, "%.4f", d);
            return String.format(Locale.US, "%.8f", d);
        } catch (Exception e) { return s; }
    }

    private static String get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        c.setRequestProperty("User-Agent", "Veer/3.0");
        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }
}
