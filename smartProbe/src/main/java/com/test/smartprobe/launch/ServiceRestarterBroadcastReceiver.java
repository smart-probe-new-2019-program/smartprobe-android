package com.test.smartprobe.launch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.test.smartprobe.util.LogUtil;

/**
 * Created by fabio on 24/01/2016.
 */
public class ServiceRestarterBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        LogUtil.writeServerStatus("");
        LogUtil.writeServerStatus(">>>>> Service Stops!  restart service <<<<<<<<< ");
        context.startService(new Intent(context, AutoLaunchService.class));;
    }
}
