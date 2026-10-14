package com.veer.assistant.core;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.veer.assistant.lang.Hindi;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandParser {
    private static Map<String, String> appIndex;
    private static long appIndexAt;

    public static Command parse(Context ctx, String raw) {
        String latin = Hindi.toLatin(raw);
        String t = latin.toLowerCase(Locale.ROOT).trim();
        String name = Prefs.getString("name", "Veer").toLowerCase(Locale.ROOT);
        t = t.replaceAll("\\b(" + Pattern.quote(name) + "|veer|vir|beer)\\b", " ")
             .replaceAll("\\s+", " ").trim();
        if (t.isEmpty()) return new Command("HELLO", raw);

        // NOTIFICATIONS
        if (has("kisne (message|msg|call)|kaun (message|call)|naya (message|msg)|notification|kya aaya", t))
            return new Command("NOTIFY_READ", raw);
        if (has("notification (clear|saaf|hatao)|clear notification", t))
            return new Command("NOTIFY_CLEAR", raw);

        // TRADE
        Matcher price = Pattern.compile("(binance|angel ?one)?\\s*(price|bhav|rate|kitna)\\s+(\\w+)").matcher(t);
        if (price.find()) {
            String exch = price.group(1) == null ? "auto" : price.group(1).replace(" ", "");
            return new Command("TRADE_PRICE", raw, exch, price.group(3).toUpperCase(Locale.ROOT));
        }
        Matcher sym = Pattern.compile("^(binance|angel ?one)\\s+([a-z]{2,10})$").matcher(t);
        if (sym.find()) return new Command("TRADE_PRICE", raw, sym.group(1).replace(" ", ""), sym.group(2).toUpperCase(Locale.ROOT));
        if (has("^(portfolio|mera portfolio|holdings|mera pnl)", t)) return new Command("TRADE_PORTFOLIO", raw);

        // SCREEN
        if (has("^(agla|agli|agle|next|aage)( reel| video| short)?$|agla (reel|video|short)", t)) return new Command("SCREEN_NEXT", raw);
        if (has("^(pichla|pichli|pichle|previous)( reel| video)?$|pichla (reel|video)", t)) return new Command("SCREEN_PREV", raw);
        if (has("^(like|pasand)( karo| kar)?$", t)) return new Command("SCREEN_LIKE", raw);
        if (has("^(back|wapas|peeche)( jao| karo)?$", t)) return new Command("SCREEN_BACK", raw);
        if (has("^(home|ghar)( jao| screen)?$", t)) return new Command("SCREEN_HOME", raw);
        if (has("^recent|multitask", t)) return new Command("SCREEN_RECENTS", raw);
        if (has("^(notification|notif|panel) (kholo|khol|dikhao)", t)) return new Command("SCREEN_NOTIF", raw);
        if (has("^(screen )?(lock|band)( karo)?$", t)) return new Command("SCREEN_LOCK", raw);
        if (has("^screenshot", t)) return new Command("SCREEN_SHOT", raw);
        if (has("^(scroll )?neeche|scroll down", t)) return new Command("SCREEN_SCROLL_DOWN", raw);
        if (has("^(scroll )?upar|scroll up", t)) return new Command("SCREEN_SCROLL_UP", raw);
        if (has("^long press", t)) return new Command("SCREEN_LONGPRESS", raw);
        Matcher tap = Pattern.compile("^tap\\s+(\\d+)\\s+(\\d+)").matcher(t);
        if (tap.find()) return new Command("SCREEN_TAP", raw, tap.group(1), tap.group(2));

        // INFO
        if (has("^madad|^help|kya kar sakte", t)) return new Command("HELP", raw);
        if (has("^status|screen (on|off)|kya chalu", t)) return new Command("STATUS", raw);
        Matcher rn = Pattern.compile("naam badlo\\s+(\\w+)").matcher(t);
        if (rn.find()) return new Command("RENAME", raw, rn.group(1));
        if (has("^battery|charge", t)) return new Command("BATTERY", raw);
        if (has("^time|samay|kitne baje", t)) return new Command("TIME", raw);
        if (has("^date|tarikh|aaj kaun", t)) return new Command("DATE", raw);
        if (has("^joke|chutkula|hasao", t)) return new Command("JOKE", raw);
        if (has("^(hi|hello|hey|namaste)", t)) return new Command("HELLO", raw);
        if (has("^(bye|alvida|tata)", t)) return new Command("BYE", raw);
        if (has("kaun ho|tumhara naam", t)) return new Command("WHOAMI", raw);

        // CALL
        Matcher call = Pattern.compile("^(.+?)\\s+ko\\s+call").matcher(t);
        if (!call.find()) call = Pattern.compile("^call\\s+(.+)").matcher(t);
        if (call.find()) return new Command("CALL", raw, call.group(1).trim());

        // SMS
        Matcher sms = Pattern.compile("^sms\\s+(\\+?\\d{10,13})\\s+(.+)").matcher(t);
        if (sms.find()) return new Command("SMS", raw, sms.group(1), sms.group(2));

        // APP
        if (has("kholo|open|chalao|start", t)) {
            String best = findApp(ctx, t);
            if (best != null) return new Command("OPEN_APP", raw, best);
        }
        return new Command("UNKNOWN", raw);
    }

    private static String findApp(Context ctx, String t) {
        Map<String, String> m = appIndex(ctx);
        String best = null;
        for (String label : m.keySet()) {
            if (label.length() >= 3 && t.contains(label)) {
                if (best == null || label.length() > best.length()) best = label;
            }
        }
        return best;
    }

    private static synchronized Map<String, String> appIndex(Context ctx) {
        long now = System.currentTimeMillis();
        if (appIndex != null && now - appIndexAt < 60000) return appIndex;
        Map<String, String> m = new HashMap<>();
        PackageManager pm = ctx.getPackageManager();
        Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(q, 0);
        for (ResolveInfo ri : list) {
            String l = String.valueOf(ri.loadLabel(pm)).toLowerCase(Locale.ROOT).trim();
            if (!l.isEmpty()) m.put(l, ri.activityInfo.packageName);
        }
        appIndex = m; appIndexAt = now;
        return m;
    }

    private static boolean has(String re, String s) { return Pattern.compile(re).matcher(s).find(); }
}
