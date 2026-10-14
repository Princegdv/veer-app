package com.veer.assistant.trade;

import android.content.Context;
import com.veer.assistant.core.Logger;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

/**
 * Angel One SmartAPI. API key ho to live price, warna NSE se approximate.
 * Reference: https://smartapi.angelbroking.com
 */
public final class AngelOneClient {

    public static String price(Context ctx, String symbol) {
        String key = com.veer.assistant.core.Prefs.getString("angel_api_key", "");
        if (!key.isEmpty()) {
            return livePrice(symbol, key);
        }
        return nsePrice(symbol);
    }

    public static String portfolio(Context ctx) {
        String key = com.veer.assistant.core.Prefs.getString("angel_api_key", "");
        if (key.isEmpty()) return "Angel One portfolio: API key nahi hai.";
        return "Angel One portfolio: API key mili, par auth flow abhi implement nahi hui.";
    }

    /** NSE India public quote endpoint (unofficial, fallback). */
    private static String nsePrice(String symbol) {
        try {
            String sym = symbol.toUpperCase(Locale.ROOT).trim();
            String url = "https://www.nseindia.com/api/quote-equity?symbol=" + URLEncoder.encode(sym, "UTF-8");
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            c.setRequestProperty("User-Agent", "Mozilla/5.0");
            c.setRequestProperty("Accept", "application/json");
            // NSE को पहले homepage hit चाहिए cookie के लिए — skip, mostly 403 आएगा
            if (c.getResponseCode() >= 400) throw new Exception("NSE " + c.getResponseCode());
            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            JSONObject o = new JSONObject(sb.toString());
            JSONObject pi = o.getJSONObject("priceInfo");
            double last = pi.getJSONObject("lastPrice").getDouble("value");
            return "Angel One (NSE) " + sym + ": ₹" + String.format(Locale.US, "%.2f", last);
        } catch (Exception e) {
            Logger.e("Angel", "nse fail", e);
            return "Angel One price nahi mila. API key Settings mein daalo (smartapi.angelbroking.com).";
        }
    }

    private static String livePrice(String symbol, String apiKey) {
        // Real SmartAPI integration यहाँ होगा — JWT + TOTP login, फिर /quote
        return "Angel One: API key set hai par auth flow pending. Binance से crypto try karo.";
    }
}
