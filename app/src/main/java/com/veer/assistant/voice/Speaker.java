package com.veer.assistant.voice;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import com.veer.assistant.core.Logger;

import java.util.Locale;

public final class Speaker {
    private static TextToSpeech tts;
    private static volatile boolean ready;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void init(Context c) {
        if (tts != null) return;
        tts = new TextToSpeech(c.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS && tts != null) {
                int r = tts.setLanguage(new Locale("hi", "IN"));
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED)
                    tts.setLanguage(Locale.getDefault());
                ready = true;
                Logger.i("TTS", "ready");
            }
        });
    }

    public static void say(String text, Runnable onDone) {
        if (text == null) text = "";
        if (text.length() > 400) text = text.substring(0, 400);
        if (tts == null || !ready) {
            if (onDone != null) MAIN.postDelayed(onDone, 300);
            return;
        }
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) {}
            @Override public void onDone(String id) { if (onDone != null) MAIN.post(onDone); }
            @Override public void onError(String id) { if (onDone != null) MAIN.post(onDone); }
        });
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "v" + System.nanoTime());
    }

    public static void stop() { if (tts != null) tts.stop(); }
}
