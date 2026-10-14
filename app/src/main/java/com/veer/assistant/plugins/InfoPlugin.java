package com.veer.assistant.plugins;

import android.content.Context;
import com.veer.assistant.core.Command;
import com.veer.assistant.core.Prefs;
import com.veer.assistant.voice.VoiceService;
import com.veer.assistant.voice.WakeWordService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class InfoPlugin implements Plugin {
    private static final String[] JOKES = {
            "Teacher: homework kahan? Student: sir cloud pe tha, net nahi aaya.",
            "WiFi aur pyaar mein ek baat common: paas aate hi signal full.",
            "Mobile ne battery se kaha: tu 1% pe bhi drama karti hai."
    };
    @Override public String name() { return "info"; }
    @Override public boolean canHandle(Command c) {
        switch (c.action) {
            case "TIME": case "DATE": case "JOKE": case "HELLO": case "BYE":
            case "HELP": case "RENAME": case "STATUS": case "WHOAMI":
                return true;
            default: return false;
        }
    }
    @Override public String handle(Context ctx, Command c) {
        switch (c.action) {
            case "TIME": return "Abhi " + new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(new Date()) + " hain.";
            case "DATE": return "Aaj " + new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(new Date()) + " hai.";
            case "JOKE": return JOKES[new Random().nextInt(JOKES.length)];
            case "HELLO": return "Namaste! Main " + Prefs.getString("name", "Veer") + " hu. 'madad' bolo.";
            case "BYE": return "Theek hai, bye!";
            case "WHOAMI": return "Main " + Prefs.getString("name", "Veer") + " hu.";
            case "RENAME": {
                String n = c.arg(0);
                n = n.substring(0,1).toUpperCase(Locale.ROOT) + n.substring(1);
                Prefs.putString("name", n);
                return "Ab mera naam " + n + " hai.";
            }
            case "STATUS": {
                boolean scr = com.veer.assistant.screen.VeerAccess.isConnected();
                boolean v = VoiceService.isRunning();
                boolean w = WakeWordService.isRunning();
                return "Screen: " + (scr?"ON":"OFF") + " | Voice: " + (v?"ON":"OFF") + " | Offline wake: " + (w?"ON":"OFF");
            }
            case "HELP": return "Main kar sakta hu:\n" +
                    "• Screen: agla, pichla, like, back, home, recents, screenshot, lock\n" +
                    "• Scroll: neeche, upar\n" +
                    "• Call: <naam> ko call | SMS: <number> <msg>\n" +
                    "• Notifications: 'kisne message kiya?' | 'notification clear karo'\n" +
                    "• Trade: 'binance price BTC' | 'angel one RELIANCE' | 'mera portfolio'\n" +
                    "• App kholna: youtube kholo | Naam badlo: naam badlo <naam>\n" +
                    "• Time, date, battery, joke, madad";
        }
        return "?";
    }
}
