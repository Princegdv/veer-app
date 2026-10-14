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
import android.speech.SpeechRecognizer;

import com.veer.assistant.MainActivity;
import com.veer.assistant.core.Logger;
import com.veer.assistant.core.Prefs;
import com.veer.assistant.core.VeerEngine;
import com.veer.assistant.lang.Hindi;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoiceService extends Service {
    private static volatile boolean running;
    public static boolean isRunning() { return running; }

    private final Handler h = new Handler(Looper.getMainLooper());
    private SpeechHelper stt;
    private boolean speaking;
    private boolean stopped;
    private boolean handsFree;
    private long awakeUntil;

    private final Runnable listener = () -> listenOnce();

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if ("STOP".equals(action)) { shutdown(); return START_NOT_STICKY; }

        if ("WAKE_TRIGGER".equals(action)) {
            // Wake word मिला, अब command सुनो — foreground ज़रूरी है Android 10+
            if (!running) {
                running = true;
                stopped = false;
                handsFree = false;
                Speaker.init(this);
                stt = new SpeechHelper(this);
                startFg();
            }
            awakeUntil = SystemClock.elapsedRealtime() + 10000;
            h.postDelayed(this::listenOnce, 400);
            return START_STICKY;
        }

        startFg();
        if (!running) {
            running = true;
            stopped = false;
            handsFree = true;
            Speaker.init(this);
            stt = new SpeechHelper(this);
            speak(Prefs.getString("name", "Veer") + " sun raha hai.");
        }
        return START_STICKY;
    }

    private void startFg() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("veer", "Veer", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ch);
        }
        Intent stopI = new Intent(this, VoiceService.class).setAction("STOP");
        PendingIntent stop = PendingIntent.getService(this, 0, stopI,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent open = PendingIntent.getActivity(this, 1, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, "veer")
                : new Notification.Builder(this);
        Notification n = b
                .setContentTitle(Prefs.getString("name", "Veer") + " sun raha hai")
                .setContentText("'" + Prefs.getString("name", "Veer") + "' bolo")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(open)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Band", stop)
                .build();
        if (Build.VERSION.SDK_INT >= 30)
            startForeground(7, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        else startForeground(7, n);
    }

    private void listenOnce() {
        if (stopped || speaking || stt == null) return;
        stt.start(new SpeechHelper.Callback() {
            @Override public void onResult(String text) { onHeard(text); }
            @Override public void onError(int code) {
                long d = 400;
                if (code == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) d = 1500;
                else if (code == SpeechRecognizer.ERROR_NETWORK
                        || code == SpeechRecognizer.ERROR_NETWORK_TIMEOUT
                        || code == SpeechRecognizer.ERROR_SERVER) d = 3000;
                restart(d);
            }
        });
    }

    private void onHeard(String heard) {
        String latin = Hindi.toLatin(heard).toLowerCase(Locale.ROOT);
        String name = Prefs.getString("name", "Veer").toLowerCase(Locale.ROOT);
        Pattern wp = Pattern.compile("\\b(" + Pattern.quote(name) + "|veer|vir|beer|weer|vear|peer)\\b");
        Matcher m = wp.matcher(latin);
        boolean woke = m.find();
        long now = SystemClock.elapsedRealtime();

        if (handsFree && !woke && now > awakeUntil) { restart(300); return; }

        String rest = woke
                ? (latin.substring(0, m.start()) + " " + latin.substring(m.end())).trim()
                : latin;

        if (Pattern.compile("\\b(bye|ruk jao|band ho jao|stop listening)\\b").matcher(rest).find()) {
            speakThenShutdown("Theek hai, bye.");
            return;
        }
        awakeUntil = now + 10000;
        if (rest.isEmpty()) { speak("Haan boliye."); return; }
        Logger.i("Voice", "cmd: " + rest);
        VeerEngine.ask(this, rest, this::speak);
    }

    private void speak(String s) {
        speaking = true;
        Speaker.say(s, () -> {
            speaking = false;
            if (handsFree) restart(300);
            else shutdown();
        });
    }

    private void speakThenShutdown(String s) {
        speaking = true;
        Speaker.say(s, () -> { speaking = false; shutdown(); });
    }

    private void restart(long ms) {
        h.removeCallbacks(listener);
        if (!stopped) h.postDelayed(listener, ms);
    }

    private void shutdown() {
        stopped = true;
        running = false;
        handsFree = false;
        h.removeCallbacksAndMessages(null);
        if (stt != null) { stt.stop(); stt = null; }
        Speaker.stop();
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopped = true;
        running = false;
        h.removeCallbacksAndMessages(null);
        if (stt != null) { stt.stop(); stt = null; }
        super.onDestroy();
    }
}
