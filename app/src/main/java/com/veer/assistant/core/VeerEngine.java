package com.veer.assistant.core;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.veer.assistant.plugins.AppPlugin;
import com.veer.assistant.plugins.InfoPlugin;
import com.veer.assistant.plugins.NotifyPlugin;
import com.veer.assistant.plugins.PhonePlugin;
import com.veer.assistant.plugins.Plugin;
import com.veer.assistant.plugins.ScreenPlugin;
import com.veer.assistant.plugins.TradePlugin;

import java.util.ArrayList;
import java.util.List;

public final class VeerEngine {
    public interface Callback { void onReply(String reply); }
    private static final List<Plugin> PLUGINS = new ArrayList<>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static Context appCtx;

    public static void init(Context c) {
        appCtx = c.getApplicationContext();
        PLUGINS.clear();
        PLUGINS.add(new ScreenPlugin());
        PLUGINS.add(new PhonePlugin());
        PLUGINS.add(new AppPlugin());
        PLUGINS.add(new NotifyPlugin());
        PLUGINS.add(new TradePlugin());
        PLUGINS.add(new InfoPlugin());
        Logger.i("Engine", "Plugins: " + PLUGINS.size());
    }

    public static void ask(Context ctx, String text, Callback cb) {
        final Context app = ctx.getApplicationContext();
        new Thread(() -> {
            String reply;
            try { reply = process(app, text); }
            catch (Throwable t) { Logger.e("Engine", "fail", t); reply = "Gadbad: " + t.getMessage(); }
            final String out = reply;
            MAIN.post(() -> cb.onReply(out));
        }, "veer-engine").start();
    }

    private static String process(Context ctx, String raw) {
        if (raw == null) return "Kuch bolo.";
        Command c = CommandParser.parse(ctx, raw);
        Logger.d("Engine", "Parsed: " + c);
        for (Plugin p : PLUGINS) {
            if (p.canHandle(c)) {
                try { return p.handle(ctx, c); }
                catch (Throwable t) { Logger.e("Engine", p.name() + " fail", t); return "Fail: " + t.getMessage(); }
            }
        }
        return "Samajh nahi aaya. 'madad' bolo.";
    }
}
