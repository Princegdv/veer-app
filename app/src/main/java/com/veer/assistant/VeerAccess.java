package com.veer.assistant;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityEvent;

/** Screen control: swipe, tap, back, home... Wi-Fi ya adb ki zarurat nahi. */
public class VeerAccess extends AccessibilityService {
    static volatile VeerAccess inst;

    @Override
    protected void onServiceConnected() {
        inst = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public boolean onUnbind(Intent intent) {
        inst = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        inst = null;
        super.onDestroy();
    }

    int[] size() {
        DisplayMetrics d = getResources().getDisplayMetrics();
        return new int[]{d.widthPixels, d.heightPixels};
    }

    /** fingerUp=true: agli reel / neeche scroll. false: pichli reel / upar scroll. */
    boolean swipe(boolean fingerUp, boolean shortSwipe) {
        int[] s = size();
        float x = s[0] / 2f;
        float a = shortSwipe ? 0.62f : 0.75f;
        float b = shortSwipe ? 0.38f : 0.25f;
        if (!fingerUp) {
            float t = a;
            a = b;
            b = t;
        }
        return gesture(x, s[1] * a, x, s[1] * b, 220);
    }

    boolean gesture(float x1, float y1, float x2, float y2, long dur) {
        Path p = new Path();
        p.moveTo(x1, y1);
        if (x1 != x2 || y1 != y2) p.lineTo(x2, y2);
        GestureDescription.Builder b = new GestureDescription.Builder();
        b.addStroke(new GestureDescription.StrokeDescription(p, 0, dur));
        return dispatchGesture(b.build(), null, null);
    }

    boolean tap(float x, float y) {
        return gesture(x, y, x, y, 50);
    }

    void doubleTap(final float x, final float y) {
        tap(x, y);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                tap(x, y);
            }
        }, 130);
    }

    boolean global(int action) {
        return performGlobalAction(action);
    }
}
