package com.veer.assistant.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import com.veer.assistant.core.Logger;

import java.util.ArrayList;

public final class SpeechHelper {
    public interface Callback {
        void onResult(String text);
        void onError(int code);
    }
    private SpeechRecognizer sr;
    private final Context ctx;
    public SpeechHelper(Context c) { this.ctx = c.getApplicationContext(); }
    public boolean isAvailable() { return SpeechRecognizer.isRecognitionAvailable(ctx); }

    public void start(final Callback cb) {
        stop();
        try {
            sr = SpeechRecognizer.createSpeechRecognizer(ctx);
            sr.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle p) {}
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float v) {}
                @Override public void onBufferReceived(byte[] b) {}
                @Override public void onEndOfSpeech() {}
                @Override public void onPartialResults(Bundle b) {}
                @Override public void onEvent(int t, Bundle b) {}
                @Override public void onResults(Bundle b) {
                    ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (r != null && !r.isEmpty()) cb.onResult(r.get(0));
                    else cb.onError(SpeechRecognizer.ERROR_NO_MATCH);
                }
                @Override public void onError(int e) { Logger.w("STT", "err " + e); cb.onError(e); }
            });
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
            i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            sr.startListening(i);
        } catch (Exception e) {
            Logger.e("STT", "start fail", e);
            cb.onError(-1);
        }
    }

    public void stop() {
        if (sr != null) {
            try { sr.stopListening(); } catch (Exception ignored) {}
            try { sr.destroy(); } catch (Exception ignored) {}
            sr = null;
        }
    }
}
