package com.veer.assistant.plugins;

import android.content.Context;
import com.veer.assistant.core.Command;
import com.veer.assistant.notify.NotifyStore;
import com.veer.assistant.notify.VeerNotificationListener;

public class NotifyPlugin implements Plugin {
    @Override public String name() { return "notify"; }
    @Override public boolean canHandle(Command c) {
        return "NOTIFY_READ".equals(c.action) || "NOTIFY_CLEAR".equals(c.action);
    }
    @Override public String handle(Context ctx, Command c) {
        if (!VeerNotificationListener.isConnected())
            return "Notification access OFF. Settings > Notifications > Device & app notifications > Veer ko ON karo.";
        if ("NOTIFY_CLEAR".equals(c.action)) {
            VeerNotificationListener.clearAll();
            return "Notifications clear.";
        }
        String summary = NotifyStore.summaryRecent(5);
        return summary.isEmpty() ? "Koi naya notification nahi." : summary;
    }
}
