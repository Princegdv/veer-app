package com.veer.assistant.core;
import android.util.Log;
public final class Logger {
    private static final String TAG = "Veer";
    private static boolean debug = true;
    public static void init(boolean d) { debug = d; }
    public static void d(String t, String m) { if (debug) Log.d(TAG, "[" + t + "] " + m); }
    public static void i(String t, String m) { Log.i(TAG, "[" + t + "] " + m); }
    public static void w(String t, String m) { Log.w(TAG, "[" + t + "] " + m); }
    public static void e(String t, String m) { Log.e(TAG, "[" + t + "] " + m); }
    public static void e(String t, String m, Throwable x) { Log.e(TAG, "[" + t + "] " + m, x); }
}
