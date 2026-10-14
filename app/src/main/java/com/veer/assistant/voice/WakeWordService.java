package com.veer.assistant.voice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import com.veer.assistant.MainActivity;
import com.veer.assistant.core.Logger;
import com.veer.assistant.core.Prefs;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;

import java.io.IOException;
import java.util.Locale;

public class WakeWordService extends Service implements RecognitionListener {
    public static final String MODEL_DIR = "vosk-model-small-hi";
    private static volatile boolean running;
    public static boolean isRunning() { return running; }

    private SpeechService speechService;
    private Model model;
    private long lastWake = 0;
    private final Handler h = new Handler(Looper.getMainLooper());

    @Nullable @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            shutdown();
            return START_NOT_STICKY;
        }
        startFg();
        if (!running) {
            running = true;
            loadModel();
        }
        return START_STICKY;
    }

    private void startFg() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("veer_wake", "Veer Wake", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ch);
        }
        Intent stopI = new Intent(this, WakeWordService.class).setAction("STOP");
        PendingIntent stop = PendingIntent.getService(this, 0, stopI,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent open = PendingIntent.getActivity(this, 1,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, "veer_wake")
                : new Notification.Builder(this);
        Notification n = b
                .setContentTitle(Prefs.getString("name", "Veer") + " offline sun raha hai")
                .setContentText("'" + Prefs.getString("name", "Veer") + "' bolo")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(open)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Band", stop)
                .build();
        if (Build.VERSION.SDK_INT >= 30)
            startForeground(8, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        else startForeground(8, n);
    }

    private void loadModel() {
        Logger.i("WakeWord", "loading model");
        StorageService.unpack(this, MODEL_DIR, "model",
                m -> { model = m; Logger.i("WakeWord", "model ready"); startRec(); },
                e -> { Logger.e("WakeWord", "model fail", e); running = false; stopSelf(); });
    }

    private void startRec() {
        try {
            Recognizer rec = new Recognizer(model, 16000.0f);
            speechService = new SpeechService(rec, 16000.0f);
            speechService.startListening(this);
            Logger.i("WakeWord", "listening");
        } catch (IOException e) { Logger.e("WakeWord", "rec fail", e); }
    }

    @Override public void onPartialResult(String h) { check(h); }
    @Override public void onResult(String h) { check(h); }
    @Override public void onFinalResult(String h) { check(h); }
    @Override public void onError(Exception e) { Logger.e("WakeWord", "err", e); restart(); }
    @Override public void onTimeout() { restart(); }

    private void restart() {
        try {
            if (speechService != null) { speechService.stop(); speechService = null; }
            if (model != null) startRec();
        } catch (Exception ignored) {}
    }

    private void check(String json) {
        if (json == null) return;
        String text = extract(json);
        if (text.isEmpty()) return;
        if (isWakeWord(text) && SystemClock.elapsedRealtime() - lastWake > 3000) {
            lastWake = SystemClock.elapsedRealtime();
            onWake();
        }
    }

    private String extract(String json) {
        try {
            int i = json.indexOf("\"partial\"");
            boolean p = i >= 0;
            if (!p) i = json.indexOf("\"text\"");
            if (i < 0) return "";
            int s = json.indexOf('"', i + 8) + 1;
            int e = json.indexOf('"', s);
            if (s <= 0 || e <= s) return "";
            return json.substring(s, e).toLowerCase(Locale.ROOT).trim();
        } catch (Exception e) { return ""; }
    }

    private boolean isWakeWord(String text) {
        for (String w : text.split("\\s+")) {
            w = w.replaceAll("[^a-z]", "");
            if (w.length() < 2 || w.length() > 6) continue;
            if (w.equals("veer") || w.equals("vir") || w.equals("beer")
                    || w.equals("weer") || w.equals("vear") || w.equals("peer")
                    || w.equals("bir") || w.equals("wir") || w.equals("viir")
                    || w.equals("wiir") || w.equals("ver")) return true;
            if (sim(w, "veer") > 0.7) return true;
        }
        return false;
    }

    private double sim(String a, String b) {
        int[] prev = new int[b.length()+1], cur = new int[b.length()+1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int c = a.charAt(i-1) == b.charAt(j-1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j-1]+1, prev[j]+1), prev[j-1]+c);
            }
            int[] t = prev; prev = cur; cur = t;
        }
        int max = Math.max(a.length(), b.length());
        return max == 0 ? 1.0 : 1.0 - (double) prev[b.length()] / max;
    }

    private void onWake() {
        Logger.i("WakeWord", "WAKE!");
        if (speechService != null) speechService.stop();
        Intent i = new Intent(this, VoiceService.class);
        i.setAction("WAKE_TRIGGER");
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
        h.postDelayed(() -> {
            if (running && model != null) startRec();
        }, 6000);
    }

    private void shutdown() {
        running = false;
        if (speechService != null) { try { speechService.stop(); } catch (Exception ignored) {} speechService = null; }
        if (model != null) { try { model.close(); } catch (Exception ignored) {} model = null; }
        stopForeground(true);
        stopSelf();
    }

    @Override public void onDestroy() { running = false; shutdown(); super.onDestroy(); }
}
