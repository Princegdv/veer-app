package com.veer.assistant.plugins;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.BatteryManager;
import android.provider.ContactsContract;
import android.telephony.SmsManager;

import com.veer.assistant.core.Command;
import java.util.ArrayList;

public class PhonePlugin implements Plugin {
    @Override public String name() { return "phone"; }
    @Override public boolean canHandle(Command c) {
        return "CALL".equals(c.action) || "SMS".equals(c.action) || "BATTERY".equals(c.action);
    }
    @Override public String handle(Context ctx, Command c) {
        switch (c.action) {
            case "BATTERY": {
                Intent b = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (b == null) return "Battery nahi mila.";
                int lvl = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int sc = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                int st = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean ch = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL;
                return "Battery " + (lvl*100/sc) + "%, " + (ch ? "charging." : "not charging.");
            }
            case "CALL": {
                if (ctx.checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
                    return "Call permission do.";
                String q = c.arg(0);
                String num = q.matches("\\+?[\\d ]{10,14}") ? q.replace(" ", "") : findContact(ctx, q);
                if (num == null) return "'" + q + "' nahi mila.";
                Intent i = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + num));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
                return q + " ko call.";
            }
            case "SMS": {
                if (ctx.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
                    return "SMS permission do.";
                SmsManager sm = SmsManager.getDefault();
                ArrayList<String> parts = sm.divideMessage(c.arg(1));
                sm.sendMultipartTextMessage(c.arg(0), null, parts, null, null);
                return "SMS bhej diya.";
            }
        }
        return "?";
    }
    private String findContact(Context ctx, String q) {
        if (ctx.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            return null;
        Cursor cur = ctx.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                             ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
                new String[]{"%" + q + "%"}, null);
        if (cur == null) return null;
        try { if (cur.moveToFirst()) return cur.getString(1); }
        finally { cur.close(); }
        return null;
    }
}
