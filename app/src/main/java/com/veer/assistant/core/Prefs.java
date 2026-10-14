package com.veer.assistant.core;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public final class Prefs {
    private static SharedPreferences sp;
    public static void init(Context c) {
        if (sp != null) return;
        try {
            MasterKey k = new MasterKey.Builder(c).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
            sp = EncryptedSharedPreferences.create(c, "veer_secure", k,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            Logger.e("Prefs", "encrypted fail, fallback", e);
            sp = c.getSharedPreferences("veer_fallback", Context.MODE_PRIVATE);
        }
    }
    public static String getString(String k, String d) { return sp.getString(k, d); }
    public static void putString(String k, String v) { sp.edit().putString(k, v).apply(); }
    public static boolean getBool(String k, boolean d) { return sp.getBoolean(k, d); }
    public static void putBool(String k, boolean v) { sp.edit().putBoolean(k, v).apply(); }
    public static int getInt(String k, int d) { return sp.getInt(k, d); }
    public static void putInt(String k, int v) { sp.edit().putInt(k, v).apply(); }
}
