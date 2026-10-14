package com.veer.assistant.voice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.veer.assistant.core.Logger;
import com.veer.assistant.core.Prefs;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        if (!Prefs.getBool("auto_start", false)) return;
        Logger.i("Boot", "starting Veer");
        Intent i = new Intent(ctx, WakeWordService.class);
        if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i);
        else ctx.startService(i);
    }
}
