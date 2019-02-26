package com.test.smartprobe.custom;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.test.smartprobe.activity.SplashActivity;
import com.test.smartprobe.util.LogUtil;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by abitha on 10/11/16.
 */
public class MyApplication  extends Application {

    private Timer mActivityTransitionTimer;
    private TimerTask mActivityTransitionTimerTask;
    public boolean wasInBackground;
    private static MyApplication s_instance;
    private Thread.UncaughtExceptionHandler defaultUEH;
    private final long MAX_ACTIVITY_TRANSITION_TIME_MS = 2000;

    @Override
    public void onCreate() {


        super.onCreate();

        init();
    }

    public void init() {
        s_instance = this;

        // defaultUEH = Thread.getDefaultUncaughtExceptionHandler();

        // setup handler for uncaught exception
        //Thread.setDefaultUncaughtExceptionHandler(_unCaughtExceptionHandler);
    }

    public static MyApplication getInstance() {
        return s_instance;
    }

    public void startActivityTransitionTimer() {
        this.mActivityTransitionTimer = new Timer();
        this.mActivityTransitionTimerTask = new TimerTask() {
            public void run() {
                MyApplication.this.wasInBackground = true;
            }
        };

        this.mActivityTransitionTimer.schedule(mActivityTransitionTimerTask,
                MAX_ACTIVITY_TRANSITION_TIME_MS);
    }

    public void stopActivityTransitionTimer() {
        if (this.mActivityTransitionTimerTask != null) {
            this.mActivityTransitionTimerTask.cancel();
        }

        if (this.mActivityTransitionTimer != null) {
            this.mActivityTransitionTimer.cancel();
        }

        this.wasInBackground = false;
    }
    public static Context getContext() {
        return s_instance;
    }
    // handler listener
    private Thread.UncaughtExceptionHandler _unCaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable exception) {

            // here I do logging of exception to a db
            PendingIntent myActivity = PendingIntent.getActivity(getContext(), 0, new Intent(s_instance, SplashActivity.class),
                    PendingIntent.FLAG_ONE_SHOT);
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, myActivity);
            System.exit(1);

            // re-throw critical exception further to the os (important)
            defaultUEH.uncaughtException(thread, exception);


        }
    };

    public static void restartApp(Activity activity) {

        Intent intent = new Intent(activity, SplashActivity.class);
        intent.putExtra("crash", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(MyApplication.getInstance().getBaseContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager mgr = (AlarmManager) MyApplication.getInstance().getBaseContext().getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent);

        LogUtil.writeLogTest(">>>>>>>>>>>>>>> RESTARTING APP <<<<<<<<<<<<<<<<<<");
        activity.finish();
        System.exit(2);
    }
}
