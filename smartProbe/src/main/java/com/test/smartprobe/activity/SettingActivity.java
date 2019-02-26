package com.test.smartprobe.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.test.smartprobe.R;
import com.test.smartprobe.database.SmartProbeDBOpenHelper;
import com.test.smartprobe.util.AppUtil;

public class SettingActivity extends AppCompatActivity {

    public static final int REQUEST_CODE = 20;
    public static final int RESULT_CODE = 200;
    Button saveSettingButton;
    Spinner readTime;
    Spinner sendTime;
    TextView textViewread_time;
    TextView textViewsend_time;

    private SQLiteDatabase mDB = null;
    private SmartProbeDBOpenHelper mDbHelper;
    private CheckBox trackLocation;
    private CheckBox enableAlarm,enableProbe;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        AppUtil.setupActivity(this);
        setContentView(R.layout.settings);

        // Creating database for SmartProbe
        mDbHelper = new SmartProbeDBOpenHelper(this);
        mDB = mDbHelper.getWritableDatabase();

        readTime = (Spinner) findViewById(R.id.readTime);
        sendTime = (Spinner) findViewById(R.id.sendTime);
        textViewread_time = (TextView) findViewById(R.id.read_time);
        textViewsend_time = (TextView) findViewById(R.id.send_time);
        trackLocation = (CheckBox) findViewById(R.id.track_location);
        enableAlarm = (CheckBox) findViewById(R.id.enable_alarm);
        enableProbe = (CheckBox) findViewById(R.id.enable_probe);
        toolbar = (Toolbar)findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Cursor c = mDB.query(SmartProbeDBOpenHelper.TABLE_NAME_SETTING, null, null, null, null, null, null);
        if (c.moveToFirst()) {
            textViewread_time.setText("Current read time interval: " + c.getString(1) + " Min");
            textViewsend_time.setText("Current send time interval: " + c.getString(2) + " Min");
            boolean isChecked = (c.getInt(3) == 0) ? false : true;
            trackLocation.setChecked(isChecked);

            boolean isAlarmChecked = (c.getInt(4) == 0) ? false : true;
            enableAlarm.setChecked(isAlarmChecked);
        }
        c.close();

        SharedPreferences prefs = getSharedPreferences("SmartProbePrefs", MODE_PRIVATE);
        int probe = prefs.getInt("chk_probe", 0); //0 is the default value.

        boolean isprobeChecked = (probe == 0) ? false : true;
        enableProbe.setChecked(isprobeChecked);


        saveSettingButton = (Button) findViewById(R.id.settingButton);
        saveSettingButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                try {
                    String readVal = readTime.getSelectedItem().toString();
                    String sendVal = sendTime.getSelectedItem().toString();
                    int ischecked = (trackLocation.isChecked()) ? 1 : 0;
                    int isAlarmEnabled = (enableAlarm.isChecked()) ? 1 : 0;
                    int isProbeEnabled = (enableProbe.isChecked()) ? 1 : 0;

                    mDB.delete(SmartProbeDBOpenHelper.TABLE_NAME_SETTING, null, null);
                    ContentValues values = new ContentValues();
                    values.put(SmartProbeDBOpenHelper.READ_TIME, readVal);
                    values.put(SmartProbeDBOpenHelper.SEND_TIME, sendVal);
                    values.put(SmartProbeDBOpenHelper.TRACK_LOCATION, ischecked);
                    values.put(SmartProbeDBOpenHelper.ENABLE_ALARM, isAlarmEnabled);
                    mDB.insert(SmartProbeDBOpenHelper.TABLE_NAME_SETTING, null, values);

                    SharedPreferences.Editor editor = getSharedPreferences("SmartProbePrefs", MODE_PRIVATE).edit();
                    editor.putInt("chk_probe", isProbeEnabled).commit();


                    values.clear();
                    mDB.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }


                Intent mainIntent = new Intent(SettingActivity.this, SmartProbeActivity.class);
//                SettingActivity.this.startActivity(mainIntent);
                setResult(RESULT_CODE, mainIntent);
                SettingActivity.this.finish();

            }
        });


    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
      /*  Intent mainIntent = new Intent(SettingActivity.this, SmartProbeActivity.class);
        SettingActivity.this.startActivity(mainIntent);
        SettingActivity.this.finish();*/
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mDB != null && mDB.isOpen())
            mDB.close();

        mDB = null;
        mDbHelper = null;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }
}
