package com.test.smartprobe.launch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.test.smartprobe.activity.SplashActivity;

/**
 * Created by abitha on 8/4/16.
 *
 * Service to start application on phone reboot.
 */
public class BootReciever extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        Intent myIntent = new Intent(context, SplashActivity.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(myIntent);
    }

}