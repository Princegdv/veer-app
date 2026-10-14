package com.veer.assistant;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Hinglish / Hindi vakya -> Cmd. Pure Java (Android ke bina test hota hai). */
final class Parser {
    private static final Set<String> TIMER_STOP = new HashSet<>(Arrays.asList(
            "baad", "ke", "remind", "reminder", "yaad", "dilao", "dila", "timer", "set", "laga", "lagao",
            "karo", "kar", "do", "me", "mein", "mujhe", "ki", "ka", "hai", "ek", "bhi", "please", "aur"));
    private static final Set<String> SONG_STOP = new HashSet<>(Arrays.asList(
            "gana", "gaana", "song", "play", "bajao", "sunao", "chalao", "laga", "lagao", "do", "karo", "kar",
            "mujhe", "ek", "ka", "ke", "ki", "wala", "wali", "mere", "hai", "youtube", "pe", "par", "mein", "me"));
    private static final Set<String> APP_STOP = new HashSet<>(Arrays.asList(
            "do", "karo", "kar", "ko", "ke", "ka", "ki", "app", "ek", "zara", "please", "mujhe", "mera", "meri", "hai"));
    private static final Set<String> WEATHER_STOP = new HashSet<>(Arrays.asList(
            "mausam", "weather", "kaisa", "hai", "batao", "in", "ka", "ki", "ke", "aaj", "abhi", "kal", "mein",
            "me", "kya", "hal", "btao"));

    private static Matcher find(String re, String s) {
        Matcher m = Pattern.compile(re).matcher(s);
        return m.find() ? m : null;
    }

    private static boolean has(String re, String s) {
        return Pattern.compile(re).matcher(s).find();
    }

    private static String filter(String text, Set<String> stop) {
        StringBuilder sb = new StringBuilder();
        for (String w : text.split(" ")) {
            if (w.isEmpty() || stop.contains(w)) continue;
            sb.append(w).append(' ');
        }
        return sb.toString().trim();
    }

    /** apps: phone mein installed apps ke lowercase naam (null ho sakta hai). */
    static Cmd parse(String raw, String name, Set<String> apps) {
        String latin = Hindi.toLatin(raw);
        String t = latin.toLowerCase(Locale.ROOT).trim();
        Matcher m;

        m = find("naam (?:badlo|rakho)\\s+(?:ab\\s+)?(?!kya\\b|kaun\\b)(\\w+)", t);
        if (m != null) return new Cmd("RENAME", m.group(1));

        m = Pattern.compile("api key\\s+(\\S+)", Pattern.CASE_INSENSITIVE).matcher(latin);
        if (m.find()) return new Cmd("APIKEY", m.group(1));

        // naam kahin bhi ho (shuru, beech, aakhir) use hata do
        t = t.replaceAll("\\b(" + Pattern.quote(name.toLowerCase(Locale.ROOT)) + "|veer)\\b", " ");
        t = t.replaceAll("\\s+", " ").replaceAll("^[ ,.]+|[ ,.]+$", "");
        if (t.isEmpty()) return new Cmd("EMPTY");

        if (has("madad|help|kya kar sakte", t)) return new Cmd("HELP");
        if (has("(kyon|kyu|kyun).*(band|off)|control.*(band|kyon)|phone control", t)) return new Cmd("STATUS");

        // ---- screen control ----
        if (has("(agla|agli|agle|next|aage).{0,12}(reel|video|short)|^(next|agla|agli|aage)$", t)) return new Cmd("NEXT");
        if (has("(pichla|pichli|pichle|previous|peeche wali|pehle wali).{0,12}(reel|video|short)|^(previous|pichla|pichli)$", t))
            return new Cmd("PREV");
        if (has("scroll\\s*(down|neeche)|neeche (scroll|karo)", t)) return new Cmd("SCROLLDN");
        if (has("scroll\\s*(up|upar)|upar (scroll|karo)", t)) return new Cmd("SCROLLUP");
        if (has("^like( karo| kar do| kar)?$|like (karo|kar do)|pasand (karo|kar do)", t)) return new Cmd("LIKE");
        if (has("^(back|wapas|peeche)( jao| karo)?$", t)) return new Cmd("BACK");
        if (has("^home( screen)?( jao| par jao)?$|home screen", t)) return new Cmd("HOME");
        if (has("recent|multitask|saari apps", t)) return new Cmd("RECENTS");
        if (has("screen (band|lock|off)|phone lock|lock karo", t)) return new Cmd("LOCK");
        if (has("notification|panel (kholo|khol)", t)) return new Cmd("NOTIF");
        if (has("screenshot", t)) return new Cmd("SHOT");
        m = find("^tap\\s+(\\d+)\\s+(\\d+)", t);
        if (m != null) return new Cmd("TAP", m.group(1), m.group(2));

        // ---- bot / trade (abhi sirf Termux wale Veer mein) ----
        if (has("trade|pnl|profit|munafa|nuksan|\\bbot\\b", t)) return new Cmd("BOT");

        // ---- timer / reminder ----
        m = find("(\\d{1,4})\\s*(second|sec|minute|min|mint|ghanta|ghante|hour)", t);
        if (m != null && has("timer|yaad|remind|baad|ke baad", t)) {
            int n = Integer.parseInt(m.group(1));
            String u = m.group(2);
            int mult = (u.equals("ghanta") || u.equals("ghante") || u.equals("hour")) ? 3600 : (u.startsWith("min") ? 60 : 1);
            String what = t.contains("timer") ? "" : filter(t.replace(m.group(0), " "), TIMER_STOP);
            return new Cmd("TIMER", String.valueOf(n * mult), what, n + " " + u);
        }

        // ---- alarm ----
        m = find("alarm.*?(\\d{1,2})\\s*[:.]\\s*(\\d{2})", t);
        if (m == null) m = find("alarm.*?(\\d{1,2})\\s*baje", t);
        if (m != null) {
            int h = Integer.parseInt(m.group(1));
            int mi = m.groupCount() >= 2 ? Integer.parseInt(m.group(2)) : 0;
            if (has("shaam|sham|raat|dopahar", t) && h < 12) h += 12;
            if (h <= 23 && mi <= 59) return new Cmd("ALARM", String.valueOf(h), String.valueOf(mi));
        }

        // ---- notes ----
        m = find("^(?:note|likh)\\s*(?:karo|kar|likho)?\\s+(.+)", t);
        if (m != null) return new Cmd("NOTE", m.group(1));
        if (has("notes? (dikhao|padho|batao)|mere notes", t)) return new Cmd("NOTES");

        // ---- phone control ----
        if (has("battery|charge", t)) return new Cmd("BATTERY");
        if (has("torch|flash", t)) return new Cmd("TORCH", has("band|off|bujha", t) ? "off" : "on");
        m = find("volume\\s*(\\d+)", t);
        if (m != null) return new Cmd("VOL_SET", m.group(1));
        if (has("volume (badhao|up|zyada|increase)", t)) return new Cmd("VOL_UP");
        if (has("volume (kam|down|ghatao)", t)) return new Cmd("VOL_DOWN");
        m = find("brightness\\s*(\\d+)", t);
        if (m != null) return new Cmd("BRIGHT", m.group(1));
        m = find("sms\\s+(?:bhejo\\s+)?(\\+?\\d{10,13})\\s+(.+)", t);
        if (m != null) return new Cmd("SMS", m.group(1), m.group(2));

        Matcher m1 = find("^(.+?)\\s+ko\\s+(?:call|phone)", t);
        m = m1 != null ? m1 : find("^(?:call|phone)\\s+(?:karo\\s+|lagao\\s+)?(.+)", t);
        if (m != null) {
            String q = m.group(1).replaceAll("\\b(ko|karo|lagao|kar do)\\b", " ").replaceAll("\\s+", " ").trim();
            if (!q.isEmpty()) return new Cmd("CALL", q);
        }

        if (has("joke|chutkula|hasao", t)) return new Cmd("JOKE");

        // ---- gaana ----
        if (has("gana|gaana|song|play|bajao|sunao", t)) return new Cmd("SONG", filter(t, SONG_STOP));

        // ---- app kholna ----
        if (has("chalao|chalu|khol|open|laga|start|shuru|jao|dikhao", t) && apps != null) {
            String best = null;
            for (String a : apps) {
                if (a.length() >= 3 && has("\\b" + Pattern.quote(a) + "\\b", t) && (best == null || a.length() > best.length())) {
                    best = a;
                }
            }
            if (best != null) return new Cmd("OPEN", best);
        }
        if (has("\\b(open|kholo|khol)\\b", t)) {
            String w = filter(t.replaceAll("\\b(open|kholo|khol)\\b", " "), APP_STOP);
            return new Cmd("NOAPP", w);
        }

        // ---- info ----
        if (has("mausam|weather", t)) return new Cmd("WEATHER", filter(t, WEATHER_STOP));
        if (has("\\btime\\b|samay|kitne baje|baje kya", t)) return new Cmd("TIME");
        if (has("\\bdate\\b|tarikh|aaj kaun|aaj ka din", t)) return new Cmd("DATE");
        if (has("^(bye|alvida|tata)\\b", t)) return new Cmd("BYE");
        if (has("^(hi|hello|hey|namaste|salam)\\b", t)) return new Cmd("HELLO");
        if (has("kaun ho|tum kaun|apna naam|tumhara naam kya", t)) return new Cmd("WHOAMI");

        return new Cmd("AI", raw);
    }
}
