package com.test.smartprobe.custom;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.test.smartprobe.activity.SplashActivity;

/**
 * Created by abitha on 10/8/17.
 */

public class MyExceptionHandler implements Thread.UncaughtExceptionHandler {
    private Activity activity;
    public MyExceptionHandler(Activity a) {
        activity = a;
    }
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        MyApplication.restartApp(activity);
    }
}