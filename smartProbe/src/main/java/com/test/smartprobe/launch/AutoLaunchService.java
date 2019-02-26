package com.test.smartprobe.launch;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.test.smartprobe.activity.LocationSettingActivity;
import com.test.smartprobe.activity.LoginActivity;
import com.test.smartprobe.activity.SettingActivity;
import com.test.smartprobe.activity.SmartProbeActivity;
import com.test.smartprobe.activity.SplashActivity;
import com.test.smartprobe.util.LogUtil;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Updated by abitha on 8/4/16.
 * <p>
 * Service to start application if application killed.
 */
public class AutoLaunchService extends Service {
    public AutoLaunchService() {
        LogUtil.writeServerStatus("");
        LogUtil.writeServerStatus(">>>>>>>>>>>>>>>  SERVICE CREATED   >>>>>>>>>>>>>>>");
    }

    private static Timer timer = new Timer();

    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        LogUtil.writeServerStatus(">>>>>>>>>>>>>>>  SERVICE DESTROYED   >>>>>>>>>>>>>>>");
        //send broadcast to start service, when service destroyed
        Intent broadcastIntent = new Intent("com.test.smartprobe.launch.ServiceRestarterBroadcastReceiver");
        sendBroadcast(broadcastIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    public void onCreate() {
        super.onCreate();
        startService();
    }

    private void startService() {
        LogUtil.writeServerStatus("************start service ************");
//        Intent dialogIntent = new Intent(getBaseContext(), SplashActivity.class);
//        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(dialogIntent);
        timer.scheduleAtFixedRate(new mainTask(), 0, 30 * 1000);
    }

    private class mainTask extends TimerTask {
        public void run() {
            LogUtil.writeServerStatus("************isActivityRunning() "+isActivityRunning());
            if (isActivityRunning() == false) {

                Intent dialogIntent = new Intent(getBaseContext(), SplashActivity.class);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(dialogIntent);
            }
        }
    }

    public Boolean isActivityRunning() {

        Boolean ret = false;
        try {

            ActivityManager activityManager = (ActivityManager) getBaseContext().getSystemService(Context.ACTIVITY_SERVICE);


            for (ActivityManager.RunningTaskInfo task : activityManager.getRunningTasks(Integer.MAX_VALUE)) {
                if (SplashActivity.class.getCanonicalName().equalsIgnoreCase(task.baseActivity.getClassName()) || LoginActivity.class.getCanonicalName().equalsIgnoreCase(task.baseActivity.getClassName()) || SmartProbeActivity.class.getCanonicalName().equalsIgnoreCase(task.baseActivity.getClassName()) || LocationSettingActivity.class.getCanonicalName().equalsIgnoreCase(task.baseActivity.getClassName()) || SettingActivity.class.getCanonicalName().equalsIgnoreCase(task.baseActivity.getClassName())) {
                    ret = true;

                    //Restart app if thread is not running
                    if (SmartProbeActivity.class.getCanonicalName().equalsIgnoreCase(task.baseActivity.getClassName())) {
                        if (SmartProbeActivity.sendServerDataThread == null || !SmartProbeActivity.sendServerDataThread.isAlive()) {
                            ret = false;
                            LogUtil.writeServerStatus(">>>>> Thread not running, restart service <<<<<<<<< ");
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            LogUtil.writeServerStatus("IsActivityRunning() Exception Occurred >>" + e.getLocalizedMessage());

        }
        return ret;
    }
}
