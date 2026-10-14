package com.veer.assistant.notify;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.veer.assistant.core.Logger;

public class VeerNotificationListener extends NotificationListenerService {
    private static volatile VeerNotificationListener inst;
    public static boolean isConnected() { return inst != null; }

    @Override public void onListenerConnected() {
        super.onListenerConnected();
        inst = this;
        Logger.i("Notify", "connected");
    }
    @Override public void onListenerDisconnected() {
        inst = null;
        super.onListenerDisconnected();
    }
    @Override public void onDestroy() {
        inst = null;
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        try {
            String pkg = sbn.getPackageName();
            String app = appName(pkg);
            Bundle ex = sbn.getNotification().extras;
            String title = str(ex.get(Notification.EXTRA_TITLE));
            String text = str(ex.get(Notification.EXTRA_TEXT));
            if (text == null) text = str(ex.get(Notification.EXTRA_BIG_TEXT));
            if (title == null && text == null) return;
            if (app == null) app = pkg;
            NotifyStore.add(app, title, text);
            Logger.d("Notify", app + ": " + title + " — " + text);
        } catch (Exception e) {
            Logger.e("Notify", "post fail", e);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // ignore
    }

    public static void clearAll() {
        VeerNotificationListener s = inst;
        if (s == null) return;
        try { s.cancelAllNotifications(); }
        catch (Exception ignored) {}
        NotifyStore.clear();
    }

    private String appName(String pkg) {
        try {
            return String.valueOf(getPackageManager().getApplicationLabel(
                    getPackageManager().getApplicationInfo(pkg, 0)));
        } catch (Exception e) { return null; }
    }

    private String str(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }
}
