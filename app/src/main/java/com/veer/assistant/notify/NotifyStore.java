package com.veer.assistant.notify;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public final class NotifyStore {
    public static final class Item {
        public final String app, title, text;
        public final long time;
        Item(String a, String t, String x) { app = a; title = t; text = x; time = System.currentTimeMillis(); }
    }

    private static final int MAX = 30;
    private static final LinkedList<Item> LIST = new LinkedList<>();

    public static synchronized void add(String app, String title, String text) {
        LIST.addFirst(new Item(app, title, text));
        while (LIST.size() > MAX) LIST.removeLast();
    }

    public static synchronized void clear() { LIST.clear(); }

    public static synchronized String summaryRecent(int n) {
        if (LIST.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int count = Math.min(n, LIST.size());
        for (int i = 0; i < count; i++) {
            Item it = LIST.get(i);
            sb.append("• ").append(it.app);
            if (it.title != null) sb.append(" — ").append(it.title);
            if (it.text != null) sb.append(": ").append(trim(it.text));
            sb.append('\n');
        }
        return sb.toString().trim();
    }

    public static synchronized String lastFrom(String app) {
        for (Item it : LIST) {
            if (it.app != null && it.app.toLowerCase(Locale.ROOT).contains(app.toLowerCase(Locale.ROOT))) {
                return it.title + ": " + it.text;
            }
        }
        return null;
    }

    private static String trim(String s) {
        s = s.replace('\n', ' ');
        return s.length() > 80 ? s.substring(0, 80) + "…" : s;
    }
}
