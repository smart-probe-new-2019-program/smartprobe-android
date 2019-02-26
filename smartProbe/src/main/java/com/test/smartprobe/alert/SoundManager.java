package com.test.smartprobe.alert;

import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;

import com.test.smartprobe.R;
import com.test.smartprobe.activity.SmartProbeActivity;
import com.test.smartprobe.custom.MyApplication;
import com.test.smartprobe.util.AppUtil;
import com.test.smartprobe.util.LogUtil;

/**
 * Created by abitha on 28/10/16.
 */
public class SoundManager {

    private final SmartProbeActivity activity;
    private Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
    private MediaPlayer tempAlertSound;
    private MediaPlayer networkAlertSound;
    /**
     * Flag to check network reset state...if reset, it will be set to true...so no sound will played.
     */
    public boolean isNetworkReset = false;
    /**
     * Flag to check temperature reset state...if reset, it will be set to true...so no sound will played .
     * After 5 min this flag will reset to false to play notificvation again
     */
    public boolean isTempReset = false;
    private final Handler handler = new Handler();
    //Invoke this after 5 mins ,if not fixed temp warning state after reset .
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {

            isTempReset = false;
        }
    };

    private boolean isTempPaused;
    private boolean isNetworkPaused;
    /**
     * Bug is getting when all temp went to normal temp and app goes into background after alarm paused.
     * In that case song is getting played if app goes into foreground even if temp is normal.
     */
    public boolean stoppedWakeUpAlert;

    public SoundManager(SmartProbeActivity activity) {
        this.activity = activity;
    }

    //    public void startWakeUpAlert() {
//
//        if (isTempReset) {
//            if (tempAlertSound == null)
//                tempAlertSound = RingtoneManager.getRingtone(activity, notification);
//
//            if (!tempAlertSound.isPlaying()) {
//                tempAlertSound.play();
//
//            }
//            //set temperature connection flag
//            isTempReset = false;
//        }
//    }
    public void startWakeUpAlert() {

        MyApplication myApp = (MyApplication)activity.getApplication();

        if (activity.isEnabledAlarm && !isTempPaused && !myApp.wasInBackground) {

            if (tempAlertSound == null)
                tempAlertSound = MediaPlayer.create(activity, R.raw.temp_alarm);

            tempAlertSound.setLooping(true);
            tempAlertSound.start();
            LogUtil.writeLogTest("+++++  Temperature alarm notification started ++++");
            if (mRunnable != null)
                handler.removeCallbacks(mRunnable);
            //set temperature connection flag
            isTempReset = false;
            stoppedWakeUpAlert=false;
        }
    }

    public void stopWakeUpAlert() {

        if (tempAlertSound != null && tempAlertSound.isPlaying()) {

            tempAlertSound.stop();
            if (mRunnable != null)
                handler.removeCallbacks(mRunnable);

            tempAlertSound = null;
            LogUtil.writeLogTest("+++++  Temperature alarm notification stopped ++++");
        }
    }

    public void pauseWakeUpAlert() {

        if (tempAlertSound != null && tempAlertSound.isPlaying()) {

            tempAlertSound.pause();
            LogUtil.writeLogTest("+++++  Temperature alarm notification paused ++++");
            if (mRunnable != null)
                handler.removeCallbacks(mRunnable);

            //   tempAlertSound = null;
            isTempPaused = true;
        }
    }

    public void resumeWakeUpAlert() {

        if (isTempPaused) {
            LogUtil.writeLogTest("+++++  Resumed WakeUp Alert ++++");
            isTempPaused =false;
            isTempReset = false;
            if(!stoppedWakeUpAlert)
                startWakeUpAlert();
        }

    }
    public void pauseNetworkAlert() {

        try {
            if (networkAlertSound != null && networkAlertSound.isPlaying()) {

                networkAlertSound.pause();
                LogUtil.writeLogTest("+++++  Network alarm notification paused ++++");
                // networkAlertSound = null;
                isNetworkPaused=true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void resumeNetworkAlert() {

        if (isNetworkPaused) {
            LogUtil.writeLogTest("+++++  Resumed Network Alert ++++");
            isNetworkPaused=false;
            isNetworkReset = false;
            startNetworkAlert();
        }

    }
    private boolean isWakeUpAlertOn() {

        if (tempAlertSound != null)
            return tempAlertSound.isPlaying();

        return false;
    }

    public void resetTempAlarm() {

        if (isWakeUpAlertOn()) {
            stopWakeUpAlert();
            isTempReset = true;
            handler.postDelayed(mRunnable,300000);//120000);
        }
    }

    public void startNetworkAlert() {

        if (activity.isEnabledAlarm && !isNetworkReset &&   !AppUtil.isConnected(activity)) {
            try {
                  /*  Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(activity, notification);
                    r.play();*/
                MyApplication myApp = (MyApplication)activity.getApplication();

                if (!isNetworkPaused && !myApp.wasInBackground) {
                    if (networkAlertSound == null)
                        networkAlertSound = MediaPlayer.create(activity, R.raw.network_alarm);

                    networkAlertSound.setLooping(true);
                    networkAlertSound.start();
                    LogUtil.writeLogTest("+++++  Network alarm notification started ++++");
                    //set network connection flag
                    isNetworkReset = false;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stopNetworkAlert() {

        try {
            if (networkAlertSound != null && networkAlertSound.isPlaying()) {

                networkAlertSound.stop();
                networkAlertSound = null;
                LogUtil.writeLogTest("+++++  Network alarm notification stopped ++++");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isNetworkAlertOn() {

        if (networkAlertSound != null)
            return networkAlertSound.isPlaying();

        return false;
    }

    public void resetNetworkAlarm() {
        if (isNetworkAlertOn()) {
            stopNetworkAlert();
            isNetworkReset = true;
        }

    }

//    public void checkState() {
//
//        if (orangeState && redState) {
//            isTempReset = true;
//        }
//    }

//    public void setOrangeState() {
//
//        orangeState = true;
//        checkState();
//        redState = false;
//    }
//
//    public void setRedState() {
//
//        redState = true;
//        checkState();
//        orangeState = false;
//    }
}
