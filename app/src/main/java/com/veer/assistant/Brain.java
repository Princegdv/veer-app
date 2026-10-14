package com.veer.assistant;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.SmsManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Command ko asli kaam mein badalta hai (call, torch, screen control...). */
final class Brain {
    interface Cb {
        void done(String reply);
    }

    static final String HELP = "Main ye kar sakta hu: time, date, battery, torch, volume, brightness, "
            + "call <naam>, sms <number> <message>, gaana <naam>, app kholna (youtube kholo), "
            + "timer, alarm, yaad dilao, note, mausam, joke. "
            + "Screen: agla reel, pichli reel, like karo, back, home, recents, scroll neeche/upar, screenshot, screen band. "
            + "Naam badalne ke liye: naam badlo <naam>. Kuch bhi poochne ke liye: api key <key>.";

    static final String NEED_ACCESS = "Screen control abhi band hai. App ke upar 'Screen control ON karo' dabao, "
            + "list mein Veer ko chalu karo. Agar 'Restricted setting' likha aaye to 'App info' dabao, "
            + "teen dots (menu) mein 'Allow restricted settings' karo, phir dobara try karo.";

    private static final String[] JOKES = {
            "Teacher: homework kahan hai? Student: Sir, cloud pe tha, network nahi aaya.",
            "WiFi aur pyaar mein ek cheez common hai: paas aate hi signal full.",
            "Mobile ne battery se kaha: tu 1 percent pe bhi drama karti hai, main chalta hu 5 minute.",
            "Doctor ne kaha: mobile kam chalao. Maine kaha: Doctor sahab, aapki prescription bhi mobile mein hai.",
    };

    private static Map<String, String> appCache;
    private static long appCacheAt;

    static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("veer", Context.MODE_PRIVATE);
    }

    static String name(Context c) {
        return prefs(c).getString("name", "Veer");
    }

    /** Background thread mein chalata hai, jawab main thread par deta hai. */
    static void ask(Context ctx, final String text, final Cb cb) {
        final Context app = ctx.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                String r;
                try {
                    r = handle(app, text);
                } catch (Throwable e) {
                    r = "Kuch gadbad hui: " + e;
                }
                final String out = r;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        cb.done(out);
                    }
                });
            }
        }).start();
    }

    static String handle(Context ctx, String raw) throws Exception {
        Cmd c = Parser.parse(raw, name(ctx), appLabels(ctx));
        switch (c.type) {
            case "RENAME": {
                String n = c.a.substring(0, 1).toUpperCase(Locale.ROOT) + c.a.substring(1);
                prefs(ctx).edit().putString("name", n).apply();
                return "Theek hai, ab mera naam " + n + " hai. App ke upar ka title agli baar khologe to badal jayega.";
            }
            case "APIKEY":
                prefs(ctx).edit().putString("api_key", c.a).apply();
                return "API key save ho gayi. Ab main kuch bhi pooch sakte ho.";
            case "EMPTY":
                return "Haan boliye, main " + name(ctx) + " hu.";
            case "HELP":
                return HELP;
            case "STATUS":
                return "Screen control: " + (VeerAccess.inst != null ? "ON" : "OFF (Accessibility chalu karo)")
                        + ". Hands-free: " + (VoiceService.running ? "ON" : "OFF") + ".";

            case "NEXT": case "PREV": case "SCROLLDN": case "SCROLLUP": case "LIKE": case "BACK":
            case "HOME": case "RECENTS": case "LOCK": case "NOTIF": case "SHOT": case "TAP":
                return screen(c);

            case "BOT":
                return "Bot aur trade status abhi sirf Termux wale Veer (veer.py) mein hain. Is app mein baad mein jodenge.";

            case "TIMER": {
                Intent i = new Intent(AlarmClock.ACTION_SET_TIMER)
                        .putExtra(AlarmClock.EXTRA_LENGTH, Integer.parseInt(c.a))
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
                if (c.b != null && !c.b.isEmpty()) i.putExtra(AlarmClock.EXTRA_MESSAGE, c.b);
                return launch(ctx, i) ? "Theek hai, " + c.c + " baad bataunga." : "Clock app nahi mili, timer nahi lag paya.";
            }
            case "ALARM": {
                Intent i = new Intent(AlarmClock.ACTION_SET_ALARM)
                        .putExtra(AlarmClock.EXTRA_HOUR, Integer.parseInt(c.a))
                        .putExtra(AlarmClock.EXTRA_MINUTES, Integer.parseInt(c.b))
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
                return launch(ctx, i)
                        ? String.format(Locale.ROOT, "Alarm %02d:%02d ke liye laga diya.", Integer.parseInt(c.a), Integer.parseInt(c.b))
                        : "Clock app nahi mili, alarm nahi lag paya.";
            }
            case "NOTE": {
                SharedPreferences p = prefs(ctx);
                String stamp = new SimpleDateFormat("dd-MM HH:mm", Locale.ENGLISH).format(new Date());
                p.edit().putString("notes", p.getString("notes", "") + stamp + "  " + c.a + "\n").apply();
                return "Note kar liya.";
            }
            case "NOTES": {
                String all = prefs(ctx).getString("notes", "").trim();
                if (all.isEmpty()) return "Koi note nahi hai.";
                String[] lines = all.split("\n");
                StringBuilder sb = new StringBuilder("Aakhri notes: ");
                for (int k = Math.max(0, lines.length - 5); k < lines.length; k++) sb.append(lines[k]).append(" | ");
                return sb.toString();
            }

            case "BATTERY": {
                Intent b = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (b == null) return "Battery status nahi mila.";
                int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                int st = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean ch = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL;
                return "Battery " + (level * 100 / scale) + " percent hai, " + (ch ? "charging ho rahi hai." : "charging nahi ho rahi.");
            }
            case "TORCH": {
                boolean on = "on".equals(c.a);
                CameraManager cm = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
                for (String id : cm.getCameraIdList()) {
                    Boolean f = cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    if (Boolean.TRUE.equals(f)) {
                        cm.setTorchMode(id, on);
                        return "Torch " + (on ? "chalu." : "band.");
                    }
                }
                return "Is phone mein torch nahi mili.";
            }
            case "VOL_SET": {
                AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
                int pct = Math.max(0, Math.min(100, Integer.parseInt(c.a)));
                int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                am.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(max * pct / 100f), AudioManager.FLAG_SHOW_UI);
                return "Volume " + pct + " percent kar diya.";
            }
            case "VOL_UP": case "VOL_DOWN": {
                AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
                boolean up = "VOL_UP".equals(c.type);
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, up ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI);
                return up ? "Volume badhaya." : "Volume kam kiya.";
            }
            case "BRIGHT": {
                if (!Settings.System.canWrite(ctx)) {
                    Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + ctx.getPackageName()));
                    launch(ctx, i);
                    return "Brightness ke liye settings mein Veer ko permission do, phir dobara bolo.";
                }
                int pct = Math.max(1, Math.min(100, Integer.parseInt(c.a)));
                Settings.System.putInt(ctx.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                Settings.System.putInt(ctx.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, Math.round(pct * 2.55f));
                return "Brightness " + pct + " percent kar di.";
            }
            case "SMS": {
                if (!granted(ctx, Manifest.permission.SEND_SMS)) return "SMS ke liye app kholke SMS permission Allow karo.";
                SmsManager sm = SmsManager.getDefault();
                ArrayList<String> parts = sm.divideMessage(c.b);
                sm.sendMultipartTextMessage(c.a, null, parts, null, null);
                return "SMS bhej diya.";
            }
            case "CALL": {
                String q = c.a;
                String number;
                if (q.matches("\\+?[\\d ]{10,14}")) {
                    number = q.replace(" ", "");
                } else {
                    if (!granted(ctx, Manifest.permission.READ_CONTACTS)) return "Contacts ki permission do (app kholke Allow karo).";
                    number = contact(ctx, q);
                    if (number == null) return "'" + q + "' naam ka contact nahi mila.";
                }
                if (!granted(ctx, Manifest.permission.CALL_PHONE)) return "Call ke liye app kholke Phone permission Allow karo.";
                return launch(ctx, new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number)))
                        ? q + " ko call laga raha hu."
                        : "Call nahi lag paya. 'Display over other apps' permission do (app ke upar button).";
            }
            case "SONG": {
                if (c.a == null || c.a.isEmpty()) return "Kaunsa gaana? Jaise bolo: gaana bajao Arijit Singh.";
                Intent i = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com/results?search_query=" + URLEncoder.encode(c.a + " song", "UTF-8")));
                if (ctx.getPackageManager().getLaunchIntentForPackage("com.google.android.youtube") != null) {
                    i.setPackage("com.google.android.youtube");
                }
                return launch(ctx, i) ? "YouTube pe '" + c.a + "' khol raha hu." : "YouTube nahi khul paya.";
            }
            case "OPEN": {
                String pkg = appMap(ctx).get(c.a);
                Intent li = pkg == null ? null : ctx.getPackageManager().getLaunchIntentForPackage(pkg);
                if (li == null) return c.a + " nahi khul paya.";
                return launch(ctx, li) ? c.a + " khol diya." : c.a + " nahi khul paya. 'Display over other apps' permission do.";
            }
            case "NOAPP":
                return "'" + c.a + "' naam ka app nahi mila.";

            case "WEATHER":
                return weather(c.a);
            case "TIME":
                return "Abhi " + new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(new Date()) + " baje hain.";
            case "DATE":
                return "Aaj " + new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(new Date()) + " hai.";
            case "JOKE":
                return JOKES[new Random().nextInt(JOKES.length)];
            case "BYE":
                return "Theek hai, bye! Jab bulana ho bol dena.";
            case "HELLO":
                return "Namaste! Main " + name(ctx) + " hu. 'madad' bolo to batata hu kya kya kar sakta hu.";
            case "WHOAMI":
                return "Main " + name(ctx) + " hu, aapka phone assistant.";
            default: {
                String r = claude(ctx, c.a);
                return r != null ? r : "Ye samajh nahi aaya. 'madad' bolo. Kuch bhi pooch sakne ke liye type karo: api key <key>";
            }
        }
    }

    private static String screen(Cmd c) {
        VeerAccess a = VeerAccess.inst;
        if (a == null) return NEED_ACCESS;
        int[] s = a.size();
        switch (c.type) {
            case "NEXT": a.swipe(true, false); return "Agli reel.";
            case "PREV": a.swipe(false, false); return "Pichli reel.";
            case "SCROLLDN": a.swipe(true, true); return "Neeche scroll kiya.";
            case "SCROLLUP": a.swipe(false, true); return "Upar scroll kiya.";
            case "LIKE": a.doubleTap(s[0] / 2f, s[1] / 2f); return "Like kar diya.";
            case "BACK": a.global(AccessibilityService.GLOBAL_ACTION_BACK); return "Peeche aa gaya.";
            case "HOME": a.global(AccessibilityService.GLOBAL_ACTION_HOME); return "Home screen.";
            case "RECENTS": a.global(AccessibilityService.GLOBAL_ACTION_RECENTS); return "Recent apps khol di.";
            case "LOCK": a.global(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN); return "Screen band kar di.";
            case "NOTIF": a.global(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS); return "Notifications khol di.";
            case "SHOT": a.global(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT); return "Screenshot le liya.";
            case "TAP": a.tap(Float.parseFloat(c.a), Float.parseFloat(c.b)); return "Tap kar diya.";
            default: return null;
        }
    }

    static boolean granted(Context ctx, String perm) {
        return ctx.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean launch(Context ctx, Intent i) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            Context c = VeerAccess.inst != null ? VeerAccess.inst : ctx;
            c.startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ---------- installed apps ----------
    private static synchronized Map<String, String> appMap(Context ctx) {
        long now = System.currentTimeMillis();
        if (appCache == null || now - appCacheAt > 60000) {
            Map<String, String> m = new HashMap<>();
            PackageManager pm = ctx.getPackageManager();
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> list = pm.queryIntentActivities(q, 0);
            for (ResolveInfo ri : list) {
                String label = String.valueOf(ri.loadLabel(pm)).toLowerCase(Locale.ROOT).trim();
                if (!label.isEmpty()) m.put(label, ri.activityInfo.packageName);
            }
            appCache = m;
            appCacheAt = now;
        }
        return appCache;
    }

    private static Set<String> appLabels(Context ctx) {
        return appMap(ctx).keySet();
    }

    // ---------- contacts ----------
    private static String simp(String x) {
        x = x.toLowerCase(Locale.ROOT);
        return x.replace("oo", "u").replace("ee", "i").replace("aa", "a").replace("y", "i").replace("w", "v");
    }

    private static double ratio(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        int max = Math.max(a.length(), b.length());
        return max == 0 ? 1.0 : 1.0 - (double) prev[b.length()] / max;
    }

    private static String contact(Context ctx, String q) {
        String qs = simp(q);
        String bestNum = null;
        double best = 0;
        Cursor cur = ctx.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER},
                null, null, null);
        if (cur == null) return null;
        try {
            while (cur.moveToNext()) {
                String nm = simp(String.valueOf(cur.getString(0)));
                String num = cur.getString(1);
                List<String> cands = new ArrayList<>();
                cands.add(nm);
                for (String w : nm.split(" ")) cands.add(w);
                for (String x : cands) {
                    double r = ratio(qs, x);
                    if (!qs.isEmpty() && x.contains(qs)) r = Math.max(r, 0.9);
                    if (r > best) {
                        best = r;
                        bestNum = num;
                    }
                }
            }
        } finally {
            cur.close();
        }
        return best >= 0.6 ? bestNum : null;
    }

    // ---------- network ----------
    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append('\n');
        br.close();
        return sb.toString();
    }

    private static String weather(String city) {
        try {
            String u = "https://wttr.in/" + URLEncoder.encode(city == null ? "" : city.trim(), "UTF-8").replace("+", "%20") + "?format=3";
            HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
            c.setConnectTimeout(10000);
            c.setReadTimeout(10000);
            c.setRequestProperty("User-Agent", "curl/8.0");
            String r = readAll(c.getInputStream()).trim();
            return r.isEmpty() ? "Mausam nahi mila." : r;
        } catch (Exception e) {
            return "Mausam nahi mila, internet check karo.";
        }
    }

    private static String claude(Context ctx, String text) {
        String key = prefs(ctx).getString("api_key", "");
        if (key.isEmpty()) return null;
        try {
            JSONObject body = new JSONObject();
            body.put("model", "claude-haiku-5-5");
            body.put("max_tokens", 300);
            body.put("system", "Tum " + name(ctx) + " ho, phone assistant. Hinglish mein chhote, seedhe jawab do.");
            JSONArray msgs = new JSONArray();
            msgs.put(new JSONObject().put("role", "user").put("content", text));
            body.put("messages", msgs);
            HttpURLConnection c = (HttpURLConnection) new URL("https://api.anthropic.com/v1/messages").openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            c.setDoOutput(true);
            c.setRequestProperty("x-api-key", key);
            c.setRequestProperty("anthropic-version", "2023-06-01");
            c.setRequestProperty("content-type", "application/json");
            OutputStream os = c.getOutputStream();
            os.write(body.toString().getBytes("UTF-8"));
            os.close();
            int code = c.getResponseCode();
            String resp = readAll(code < 400 ? c.getInputStream() : c.getErrorStream());
            if (code >= 400) return "AI se jawab nahi mila (code " + code + "). API key check karo.";
            return new JSONObject(resp).getJSONArray("content").getJSONObject(0).getString("text");
        } catch (Exception e) {
            return "AI se jawab nahi mila: " + e.getMessage();
        }
    }
}
