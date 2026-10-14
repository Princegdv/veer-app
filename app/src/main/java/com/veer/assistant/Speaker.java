package com.veer.assistant;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;

/** Bolkar jawab dene wala hissa (Text-To-Speech). */
final class Speaker {
    private static TextToSpeech tts;
    private static volatile boolean ready;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    static void init(Context c) {
        if (tts != null) return;
        tts = new TextToSpeech(c.getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS && tts != null) {
                    int r = tts.setLanguage(new Locale("hi", "IN"));
                    if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts.setLanguage(Locale.getDefault());
                    }
                    ready = true;
                }
            }
        });
    }

    static void say(String text, final Runnable onDone) {
        if (text == null) text = "";
        if (text.length() > 400) text = text.substring(0, 400);
        if (tts == null || !ready) {
            if (onDone != null) MAIN.postDelayed(onDone, 400);
            return;
        }
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String id) {
            }

            @Override
            public void onDone(String id) {
                if (onDone != null) MAIN.post(onDone);
            }

            @Override
            public void onError(String id) {
                if (onDone != null) MAIN.post(onDone);
            }
        });
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "veer" + System.nanoTime());
    }
}

/** RecognitionListener ke khaali methods; sirf onResults aur onError likhne padte hain. */
abstract class RecAdapter implements RecognitionListener {
    @Override
    public void onReadyForSpeech(Bundle params) {
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onRmsChanged(float rmsdB) {
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
    }

    @Override
    public void onEndOfSpeech() {
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
    }
}
