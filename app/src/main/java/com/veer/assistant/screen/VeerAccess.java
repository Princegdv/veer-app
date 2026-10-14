package com.veer.assistant.screen;
import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import com.veer.assistant.core.Logger;

public class VeerAccess extends AccessibilityService {
    private static volatile VeerAccess inst;
    public static VeerAccess get() { return inst; }
    public static boolean isConnected() { return inst != null; }
    @Override protected void onServiceConnected() { super.onServiceConnected(); inst = this; Logger.i("Access", "connected"); }
    @Override public void onAccessibilityEvent(AccessibilityEvent e) {}
    @Override public void onInterrupt() {}
    @Override public boolean onUnbind(Intent i) { inst = null; return super.onUnbind(i); }
    @Override public void onDestroy() { inst = null; super.onDestroy(); }
}
