package com.veer.assistant.plugins;
import android.content.Context;
import com.veer.assistant.core.Command;
import com.veer.assistant.screen.ScreenController;

public class ScreenPlugin implements Plugin {
    private static final String NEED = "Screen control OFF. App ke upar 'Screen control ON' dabao.";
    @Override public String name() { return "screen"; }
    @Override public boolean canHandle(Command c) {
        return c.action.startsWith("SCREEN_");
    }
    @Override public String handle(Context ctx, Command c) {
        if (!ScreenController.isReady()) return NEED;
        switch (c.action) {
            case "SCREEN_NEXT": return ScreenController.swipeUp() ? "Agla." : "Fail.";
            case "SCREEN_PREV": return ScreenController.swipeDown() ? "Pichla." : "Fail.";
            case "SCREEN_SCROLL_DOWN": return ScreenController.scrollDown() ? "Neeche." : "Fail.";
            case "SCREEN_SCROLL_UP": return ScreenController.scrollUp() ? "Upar." : "Fail.";
            case "SCREEN_LIKE": {
                int[] s = ScreenController.size();
                ScreenController.doubleTap(s[0]/2f, s[1]/2f);
                return "Like.";
            }
            case "SCREEN_BACK": return ScreenController.back() ? "Back." : "Fail.";
            case "SCREEN_HOME": return ScreenController.home() ? "Home." : "Fail.";
            case "SCREEN_RECENTS": return ScreenController.recents() ? "Recents." : "Fail.";
            case "SCREEN_NOTIF": return ScreenController.notifications() ? "Notifications." : "Fail.";
            case "SCREEN_LOCK": return ScreenController.lockScreen() ? "Lock." : "Android 9+ chahiye.";
            case "SCREEN_SHOT": return ScreenController.screenshot() ? "Screenshot." : "Android 11+ chahiye.";
            case "SCREEN_LONGPRESS": {
                int[] s = ScreenController.size();
                ScreenController.longPress(s[0]/2f, s[1]/2f);
                return "Long press.";
            }
            case "SCREEN_TAP": {
                try {
                    float x = Float.parseFloat(c.arg(0)), y = Float.parseFloat(c.arg(1));
                    return ScreenController.tap(x, y) ? "Tap." : "Fail.";
                } catch (Exception e) { return "x y do."; }
            }
        }
        return "?";
    }
}
