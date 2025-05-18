package com.example.jupitertheaterapp.database;

import android.app.Application;
import android.content.Context;

/**
 * Custom Application class to maintain global application state
 * Used to provide context to background threads for database initialization
 */
public class TheaterApplication extends Application {
    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }

    public static Context getAppContext() {
        return appContext;
    }
}
