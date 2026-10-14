package com.veer.assistant;

import android.app.Application;
import com.veer.assistant.core.Logger;
import com.veer.assistant.core.Prefs;
import com.veer.assistant.core.VeerEngine;

public class VeerApp extends Application {
    private static VeerApp instance;
    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        Prefs.init(this);
        Logger.init(BuildConfig.DEBUG);
        VeerEngine.init(this);
        Logger.i("Veer", "App started v" + BuildConfig.VERSION_NAME);
    }
    public static VeerApp get() { return instance; }
}
