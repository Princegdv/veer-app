package com.veer.assistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Hands-free: lagatar sunta rehta hai. Pehle "Veer" bolo, phir command. */
public class VoiceService extends Service {
    static volatile boolean running;

    private final Handler h = new Handler(Looper.getMainLooper());
    private SpeechRecognizer sr;
    private boolean speaking;
    private boolean stopped;
    private long awakeUntil;
    private final Runnable starter = new Runnable() {
        @Override
        public void run() {
            startListening();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            shutdown();
            return START_NOT_STICKY;
        }
        startFg();
        if (!running) {
            running = true;
            stopped = false;
            Speaker.init(this);
            speak(Brain.name(this) + " sun raha hai. Pehle mera naam lo, phir command.");
        }
        return START_STICKY;
    }

    private void startFg() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel("veer", "Veer hands-free", NotificationManager.IMPORTANCE_LOW);
        nm.createNotificationChannel(ch);
        PendingIntent stop = PendingIntent.getService(this, 0,
                new Intent(this, VoiceService.class).setAction("STOP"),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent open = PendingIntent.getActivity(this, 1, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(this, "veer")
                .setContentTitle(Brain.name(this) + " sun raha hai")
                .setContentText("'" + Brain.name(this) + "' bolke command do")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(open)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Band karo", stop)
                .build();
        if (Build.VERSION.SDK_INT >= 30) {
            startForeground(7, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(7, n);
        }
    }

    private void restart(long ms) {
        h.removeCallbacks(starter);
        if (!stopped) h.postDelayed(starter, ms);
    }

    private void resetRecognizer() {
        if (sr != null) {
            try {
                sr.destroy();
            } catch (Exception ignored) {
            }
            sr = null;
        }
    }

    private void startListening() {
        if (stopped || speaking) return;
        try {
            if (sr == null) {
                sr = SpeechRecognizer.createSpeechRecognizer(this);
                sr.setRecognitionListener(new RecAdapter() {
                    @Override
                    public void onResults(Bundle b) {
                        ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (r != null && !r.isEmpty()) onHeard(r.get(0));
                        else restart(400);
                    }

                    @Override
                    public void onError(int e) {
                        if (e == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                            shutdown();
                        } else if (e == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                            resetRecognizer();
                            restart(1500);
                        } else if (e == SpeechRecognizer.ERROR_NETWORK || e == SpeechRecognizer.ERROR_NETWORK_TIMEOUT
                                || e == SpeechRecognizer.ERROR_SERVER) {
                            restart(3000);
                        } else {
                            restart(500);
                        }
                    }
                });
            }
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
            i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            sr.startListening(i);
        } catch (Exception e) {
            resetRecognizer();
            restart(2500);
        }
    }

    private void onHeard(String heard) {
        String latin = Hindi.toLatin(heard).toLowerCase(Locale.ROOT);
        String nm = Brain.name(this).toLowerCase(Locale.ROOT);
        Matcher m = Pattern.compile("\\b(" + Pattern.quote(nm) + "|veer|vir|beer|bir|weer|vear)\\b").matcher(latin);
        boolean woke = m.find();
        long now = SystemClock.elapsedRealtime();
        if (!woke && now > awakeUntil) {
            restart(300);
            return;
        }
        String rest = woke ? (latin.substring(0, m.start()) + " " + latin.substring(m.end())) : latin;
        rest = rest.replaceAll("\\s+", " ").replaceAll("^[ ,.]+|[ ,.]+$", "");
        if (Pattern.compile("\\b(bye|ruk jao|band ho jao|stop listening)\\b").matcher(rest).find()) {
            speaking = true;
            Speaker.say("Theek hai, bye.", new Runnable() {
                @Override
                public void run() {
                    shutdown();
                }
            });
            return;
        }
        awakeUntil = now + 10000;
        if (rest.isEmpty()) {
            speak("Haan boliye.");
            return;
        }
        Brain.ask(this, rest, new Brain.Cb() {
            @Override
            public void done(String reply) {
                speak(reply);
            }
        });
    }

    private void speak(String s) {
        speaking = true;
        Speaker.say(s, new Runnable() {
            @Override
            public void run() {
                speaking = false;
                restart(300);
            }
        });
    }

    private void shutdown() {
        stopped = true;
        running = false;
        h.removeCallbacksAndMessages(null);
        resetRecognizer();
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopped = true;
        running = false;
        h.removeCallbacksAndMessages(null);
        resetRecognizer();
        super.onDestroy();
    }
}
