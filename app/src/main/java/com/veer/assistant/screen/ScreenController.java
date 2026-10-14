package com.veer.assistant.screen;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import com.veer.assistant.core.Logger;

public final class ScreenController {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private ScreenController() {}

    public static boolean isReady() { return VeerAccess.isConnected(); }
    public static int[] size() {
        VeerAccess s = VeerAccess.get();
        if (s == null) return new int[]{1080, 1920};
        DisplayMetrics d = s.getResources().getDisplayMetrics();
        return new int[]{d.widthPixels, d.heightPixels};
    }
    public static boolean gesture(float x1, float y1, float x2, float y2, long ms) {
        VeerAccess s = VeerAccess.get();
        if (s == null) return false;
        Path p = new Path(); p.moveTo(x1, y1);
        if (x1 != x2 || y1 != y2) p.lineTo(x2, y2);
        GestureDescription.StrokeDescription st = new GestureDescription.StrokeDescription(p, 0, Math.max(1, ms));
        GestureDescription gd = new GestureDescription.Builder().addStroke(st).build();
        try { return s.dispatchGesture(gd, null, null); }
        catch (Exception e) { Logger.e("Screen", "gesture fail", e); return false; }
    }
    public static boolean tap(float x, float y) { return gesture(x, y, x, y, 60); }
    public static void doubleTap(float x, float y) {
        tap(x, y);
        MAIN.postDelayed(() -> tap(x, y), 150);
    }
    public static void longPress(float x, float y) { gesture(x, y, x, y, 700); }
    public static boolean swipeUp() {
        int[] s = size();
        return gesture(s[0]*0.5f, s[1]*0.78f, s[0]*0.5f, s[1]*0.22f, 280);
    }
    public static boolean swipeDown() {
        int[] s = size();
        return gesture(s[0]*0.5f, s[1]*0.22f, s[0]*0.5f, s[1]*0.78f, 280);
    }
    public static boolean scrollDown() {
        int[] s = size();
        return gesture(s[0]*0.5f, s[1]*0.65f, s[0]*0.5f, s[1]*0.35f, 200);
    }
    public static boolean scrollUp() {
        int[] s = size();
        return gesture(s[0]*0.5f, s[1]*0.35f, s[0]*0.5f, s[1]*0.65f, 200);
    }
    public static boolean back() { return global(AccessibilityService.GLOBAL_ACTION_BACK); }
    public static boolean home() { return global(AccessibilityService.GLOBAL_ACTION_HOME); }
    public static boolean recents() { return global(AccessibilityService.GLOBAL_ACTION_RECENTS); }
    public static boolean notifications() { return global(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS); }
    public static boolean lockScreen() {
        if (Build.VERSION.SDK_INT >= 28) return global(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
        return false;
    }
    public static boolean screenshot() {
        if (Build.VERSION.SDK_INT >= 30) return global(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT);
        return false;
    }
    private static boolean global(int a) {
        VeerAccess s = VeerAccess.get();
        if (s == null) return false;
        try { return s.performGlobalAction(a); }
        catch (Exception e) { Logger.e("Screen", "global fail", e); return false; }
    }
}
