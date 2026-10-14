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
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
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

import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final int BG = 0xFF0B1020;
    private static final int BLUE = 0xFF2F5BFF;
    private static final int CARD = 0xFF18213F;

    private LinearLayout chat;
    private ScrollView scroll;
    private EditText input;
    private TextView title;
    private TextView status;
    private Button hf;
    private Button mic;
    private SpeechRecognizer sr;

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private Button smallBtn(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        GradientDrawable g = new GradientDrawable();
        g.setColor(CARD);
        g.setCornerRadius(dp(18));
        g.setStroke(dp(1), 0xFF2A3868);
        b.setBackground(g);
        b.setPadding(dp(14), 0, dp(14), 0);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
        lp.setMargins(dp(4), 0, dp(4), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private Button roundBtn(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(20);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(BLUE);
        b.setBackground(g);
        b.setPadding(0, 0, 0, 0);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(48), dp(48));
        lp.setMargins(dp(4), 0, dp(4), 0);
        b.setLayoutParams(lp);
        return b;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Speaker.init(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(dp(16), dp(14), dp(16), dp(2));
        root.addView(title);

        status = new TextView(this);
        status.setTextColor(0xFF9FB0E0);
        status.setTextSize(13);
        status.setPadding(dp(16), 0, dp(16), dp(8));
        root.addView(status);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(8), 0, dp(8), dp(8));
        hf = smallBtn("Hands-free ON karo", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleHandsFree();
            }
        });
        row.addView(hf);
        row.addView(smallBtn("Screen control ON karo", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        }));
        row.addView(smallBtn("App info", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
            }
        }));
        row.addView(smallBtn("Display over apps", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            }
        }));
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
        mic = roundBtn("🎤", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listen();
            }
        });
        bottom.addView(mic);
        input = new EditText(this);
        input.setHint("Bolo ya likho...");
        input.setHintTextColor(0xFF8793B8);
        input.setTextColor(Color.WHITE);
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        GradientDrawable ig = new GradientDrawable();
        ig.setColor(0xFF111936);
        ig.setCornerRadius(dp(24));
        ig.setStroke(dp(1), 0xFF2A3868);
        input.setBackground(ig);
        input.setPadding(dp(16), dp(10), dp(16), dp(10));
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                send(input.getText().toString());
                return true;
            }
        });
        bottom.addView(input, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bottom.addView(roundBtn("➤", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send(input.getText().toString());
            }
        }));
        root.addView(bottom);

        setContentView(root);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        bubble("Namaste! Main " + Brain.name(this) + " hu. Mic dabao ya likho. 'madad' bolo to sab batata hu.", false);
        askPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        title.setText(Brain.name(this));
        status.setText("Screen control: " + (VeerAccess.inst != null ? "ON" : "OFF")
                + "   |   Hands-free: " + (VoiceService.running ? "ON" : "OFF"));
        hf.setText(VoiceService.running ? "Hands-free band karo" : "Hands-free ON karo");
    }

    private boolean has(String p) {
        return checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED;
    }

    private void askPermissions() {
        ArrayList<String> need = new ArrayList<>();
        String[] want = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CALL_PHONE,
                Manifest.permission.SEND_SMS, Manifest.permission.READ_CONTACTS};
        for (String p : want) if (!has(p)) need.add(p);
        if (Build.VERSION.SDK_INT >= 33 && !has(Manifest.permission.POST_NOTIFICATIONS)) {
            need.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!need.isEmpty()) requestPermissions(need.toArray(new String[0]), 1);
    }

    private void toggleHandsFree() {
        if (VoiceService.running) {
            startService(new Intent(this, VoiceService.class).setAction("STOP"));
        } else {
            if (!has(Manifest.permission.RECORD_AUDIO)) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                Toast.makeText(this, "Pehle mic ki permission Allow karo", Toast.LENGTH_SHORT).show();
                return;
            }
            startForegroundService(new Intent(this, VoiceService.class));
        }
        hf.postDelayed(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        }, 700);
    }

    private void bubble(String text, boolean me) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(16);
        tv.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable g = new GradientDrawable();
        g.setColor(me ? BLUE : CARD);
        g.setCornerRadius(dp(16));
        tv.setBackground(g);
        tv.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.82f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = me ? Gravity.END : Gravity.START;
        lp.setMargins(dp(12), dp(4), dp(12), dp(4));
        chat.addView(tv, lp);
        scroll.post(new Runnable() {
            @Override
            public void run() {
                scroll.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private void send(String text) {
        String t = text.trim();
        if (t.isEmpty()) return;
        bubble(t, true);
        input.setText("");
        Brain.ask(this, t, new Brain.Cb() {
            @Override
            public void done(String reply) {
                bubble(reply, false);
                Speaker.say(reply, null);
                refresh();
            }
        });
    }

    private void listen() {
        if (VoiceService.running) {
            Toast.makeText(this, "Hands-free chalu hai, bas '" + Brain.name(this) + "' bolo", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!has(Manifest.permission.RECORD_AUDIO)) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            bubble("Is phone mein speech recognition nahi hai. Google app update karo.", false);
            return;
        }
        if (sr == null) {
            sr = SpeechRecognizer.createSpeechRecognizer(this);
            sr.setRecognitionListener(new RecAdapter() {
                @Override
                public void onResults(Bundle b) {
                    mic.setAlpha(1f);
                    ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (r != null && !r.isEmpty()) send(r.get(0));
                }

                @Override
                public void onError(int e) {
                    mic.setAlpha(1f);
                    if (e != SpeechRecognizer.ERROR_NO_MATCH && e != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        bubble("Mic mein dikkat (code " + e + "). Internet aur mic permission check karo.", false);
                    }
                }
            });
        }
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        mic.setAlpha(0.5f);
        sr.startListening(i);
    }

    @Override
    protected void onDestroy() {
        if (sr != null) {
            sr.destroy();
            sr = null;
        }
        super.onDestroy();
    }
}
