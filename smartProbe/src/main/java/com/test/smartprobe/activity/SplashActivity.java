package com.test.smartprobe.activity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.test.smartprobe.BuildConfig;
import com.test.smartprobe.R;
import com.test.smartprobe.database.SmartProbeDBOpenHelper;
import com.test.smartprobe.launch.AutoLaunchService;
import com.test.smartprobe.util.AppUtil;

import java.io.IOException;

import io.fabric.sdk.android.Fabric;

public class SplashActivity extends AppCompatActivity {

    private final int SPLASH_DISPLAY_LENGTH = 3000;
    public static String username = null;
    public static String password = null;

    private SQLiteDatabase mDB = null;
    private SmartProbeDBOpenHelper mDbHelper;
    private Runnable checkLoginDetails;
    private Handler handler;
    private  TextView version;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        AppUtil.setupActivity(this);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.splash);

        // Creating database for SmartProbe
        mDbHelper = new SmartProbeDBOpenHelper(this);
        mDB = mDbHelper.getWritableDatabase();

        Intent intent = new Intent(this, AutoLaunchService.class);
        startService(intent);

        checkLoginDetails = new Runnable() {

            @Override
            public void run() {

                if (SmartProbeActivity.sPort != null && SmartProbeActivity.sPort.isOpen())
                    try {
                        SmartProbeActivity.sPort.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                SmartProbeActivity.sPort=null;

                Cursor c = mDB.query(SmartProbeDBOpenHelper.TABLE_NAME_LOG, null, null, null, null, null, null);
                if (c.moveToFirst()) {
                    username = c.getString(1);
                    password = c.getString(2);
                }
                c.close();

                if (username == null || password == null) {
                    Intent mainIntent = new Intent(SplashActivity.this, LoginActivity.class);
                    SplashActivity.this.startActivity(mainIntent);
                    SplashActivity.this.finish();
                } else {
                    Intent i = new Intent(SplashActivity.this, SmartProbeActivity.class);
                    SplashActivity.this.startActivity(i);
                    SplashActivity.this.finish();
                }
            }
        };
        handler = new Handler();
        handler.postDelayed(checkLoginDetails, SPLASH_DISPLAY_LENGTH);

        version = (TextView)findViewById(R.id.tv_version);
        version.setText("Version "+ BuildConfig.VERSION_NAME);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (handler != null)
            handler.removeCallbacks(checkLoginDetails);

        if (mDB != null && mDB.isOpen())
            mDB.close();

    }
}
