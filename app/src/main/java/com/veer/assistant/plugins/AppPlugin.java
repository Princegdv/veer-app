package com.veer.assistant.plugins;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import com.veer.assistant.core.Command;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppPlugin implements Plugin {
    private static Map<String, String> cache;
    private static long cacheAt;
    @Override public String name() { return "app"; }
    @Override public boolean canHandle(Command c) {
        return "OPEN_APP".equals(c.action) || "OPEN_URL".equals(c.action);
    }
    @Override public String handle(Context ctx, Command c) {
        if ("OPEN_URL".equals(c.action)) {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(c.arg(0)));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { ctx.startActivity(i); return "Khol diya."; }
            catch (Exception e) { return "URL nahi khula."; }
        }
        String label = c.arg(0);
        String pkg = appMap(ctx).get(label);
        if (pkg == null) return label + " nahi mila.";
        Intent li = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
        if (li == null) return label + " launch nahi ho sakta.";
        li.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { ctx.startActivity(li); return label + " khol diya."; }
        catch (Exception e) { return label + " nahi khula."; }
    }
    private synchronized Map<String, String> appMap(Context ctx) {
        long now = System.currentTimeMillis();
        if (cache != null && now - cacheAt < 60000) return cache;
        Map<String, String> m = new HashMap<>();
        PackageManager pm = ctx.getPackageManager();
        Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(q, 0);
        for (ResolveInfo ri : list) {
            String l = String.valueOf(ri.loadLabel(pm)).toLowerCase(Locale.ROOT).trim();
            if (!l.isEmpty()) m.put(l, ri.activityInfo.packageName);
        }
        cache = m; cacheAt = now;
        return m;
    }
}
