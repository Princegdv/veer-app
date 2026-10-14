package com.veer.assistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.veer.assistant.core.Prefs;
import com.veer.assistant.core.VeerEngine;
import com.veer.assistant.screen.VeerAccess;
import com.veer.assistant.voice.SpeechHelper;
import com.veer.assistant.voice.Speaker;
import com.veer.assistant.voice.VoiceService;
import com.veer.assistant.voice.WakeWordService;

import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final int BG = 0xFF0B1020;
    private static final int BLUE = 0xFF2F5BFF;
    private static final int CARD = 0xFF18213F;

    private LinearLayout chat;
    private ScrollView scroll;
    private EditText input;
    private TextView status;
    private Button hf, wake, mic;
    private SpeechHelper stt;

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private Button pill(String txt, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(txt); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setTextSize(12);
        GradientDrawable g = new GradientDrawable();
        g.setColor(CARD); g.setCornerRadius(dp(18)); g.setStroke(dp(1), 0xFF2A3868);
        b.setBackground(g); b.setPadding(dp(12), 0, dp(12), 0);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
        lp.setMargins(dp(4), 0, dp(4), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private Button circle(String txt, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(txt); b.setTextColor(Color.WHITE); b.setTextSize(20);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL); g.setColor(BLUE);
        b.setBackground(g); b.setPadding(0,0,0,0);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(48), dp(48));
        lp.setMargins(dp(4), 0, dp(4), 0);
        b.setLayoutParams(lp);
        return b;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Speaker.init(this);
        stt = new SpeechHelper(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        TextView title = new TextView(this);
        title.setTextColor(Color.WHITE); title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(dp(16), dp(14), dp(16), dp(2));
        title.setText(Prefs.getString("name", "Veer"));
        root.addView(title);

        status = new TextView(this);
        status.setTextColor(0xFF9FB0E0); status.setTextSize(12);
        status.setPadding(dp(16), 0, dp(16), dp(8));
        root.addView(status);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(8), 0, dp(8), dp(8));

        wake = pill("Wake OFF", v -> toggleWake());
        row.addView(wake);
        hf = pill("Hands-free OFF", v -> toggleHF());
        row.addView(hf);
        row.addView(pill("Screen ON", v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));
        row.addView(pill("Notify ON", v ->
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))));
        row.addView(pill("App info", v ->
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName())))));
        row.addView(pill("Overlay", v ->
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())))));
        hs.addView(row);
        root.addView(hs);

        scroll = new ScrollView(this);
        chat = new LinearLayout(this);
        chat.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(chat);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(dp(8), dp(8), dp(8), dp(8));

        mic = circle("🎤", v -> listen());
        bottom.addView(mic);

        input = new EditText(this);
        input.setHint("Bolo ya likho...");
        input.setHintTextColor(0xFF8793B8); input.setTextColor(Color.WHITE);
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        GradientDrawable ig = new GradientDrawable();
        ig.setColor(0xFF111936); ig.setCornerRadius(dp(24)); ig.setStroke(dp(1), 0xFF2A3868);
        input.setBackground(ig); input.setPadding(dp(16), dp(10), dp(16), dp(10));
        input.setOnEditorActionListener((v, a, e) -> { send(input.getText().toString()); return true; });
        bottom.addView(input, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        bottom.addView(circle("➤", v -> send(input.getText().toString())));
        root.addView(bottom);

        setContentView(root);
        bubble("Namaste! Main " + Prefs.getString("name", "Veer") + " hu. 'madad' bolo.", false);
        askPerms();
    }

    @Override protected void onResume() { super.onResume(); refresh(); }

    private void refresh() {
        status.setText("Screen: " + (VeerAccess.isConnected() ? "ON" : "OFF")
                + " | Voice: " + (VoiceService.isRunning() ? "ON" : "OFF")
                + " | Wake: " + (WakeWordService.isRunning() ? "ON" : "OFF"));
        hf.setText(VoiceService.isRunning() ? "Hands-free ON" : "Hands-free OFF");
        wake.setText(WakeWordService.isRunning() ? "Wake ON" : "Wake OFF");
    }

    private boolean has(String p) { return checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED; }

    private void askPerms() {
        ArrayList<String> need = new ArrayList<>();
        for (String p : new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CALL_PHONE,
                Manifest.permission.SEND_SMS, Manifest.permission.READ_CONTACTS}) {
            if (!has(p)) need.add(p);
        }
        if (Build.VERSION.SDK_INT >= 33 && !has(Manifest.permission.POST_NOTIFICATIONS))
            need.add(Manifest.permission.POST_NOTIFICATIONS);
        if (!need.isEmpty()) requestPermissions(need.toArray(new String[0]), 1);
    }

    private void toggleHF() {
        if (VoiceService.isRunning()) {
            startService(new Intent(this, VoiceService.class).setAction("STOP"));
        } else {
            if (!has(Manifest.permission.RECORD_AUDIO)) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                return;
            }
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(new Intent(this, VoiceService.class));
            else startService(new Intent(this, VoiceService.class));
        }
        hf.postDelayed(this::refresh, 700);
    }

    private void toggleWake() {
        if (WakeWordService.isRunning()) {
            startService(new Intent(this, WakeWordService.class).setAction("STOP"));
        } else {
            if (!has(Manifest.permission.RECORD_AUDIO)) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                return;
            }
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(new Intent(this, WakeWordService.class));
            else startService(new Intent(this, WakeWordService.class));
        }
        wake.postDelayed(this::refresh, 700);
    }

    private void bubble(String text, boolean me) {
        TextView tv = new TextView(this);
        tv.setText(text); tv.setTextColor(Color.WHITE); tv.setTextSize(16);
        tv.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable g = new GradientDrawable();
        g.setColor(me ? BLUE : CARD); g.setCornerRadius(dp(16));
        tv.setBackground(g);
        tv.setMaxWidth((int)(getResources().getDisplayMetrics().widthPixels * 0.82f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = me ? Gravity.END : Gravity.START;
        lp.setMargins(dp(12), dp(4), dp(12), dp(4));
        chat.addView(tv, lp);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void send(String text) {
        String t = text.trim();
        if (t.isEmpty()) return;
        bubble(t, true);
        input.setText("");
        VeerEngine.ask(this, t, reply -> {
            bubble(reply, false);
            Speaker.say(reply, null);
            refresh();
        });
    }

    private void listen() {
        if (VoiceService.isRunning()) {
            Toast.makeText(this, "Hands-free ON, bas naam bolo", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!has(Manifest.permission.RECORD_AUDIO)) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            return;
        }
        if (!stt.isAvailable()) { bubble("Speech recognition nahi hai.", false); return; }
        mic.setAlpha(0.5f);
        stt.start(new SpeechHelper.Callback() {
            @Override public void onResult(String text) { mic.setAlpha(1f); send(text); }
            @Override public void onError(int code) {
                mic.setAlpha(1f);
                if (code != android.speech.SpeechRecognizer.ERROR_NO_MATCH
                        && code != android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                    bubble("Mic error " + code, false);
            }
        });
    }

    @Override protected void onDestroy() {
        if (stt != null) stt.stop();
        super.onDestroy();
    }
}
