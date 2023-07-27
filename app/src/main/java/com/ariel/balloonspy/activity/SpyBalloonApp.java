package com.ariel.balloonspy.activity;

import android.app.Application;
import android.content.Context;

public class SpyBalloonApp extends Application {
    public static SpyBalloonApp instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }
    public static SpyBalloonApp getInstance() {
        return instance;
    }
}
