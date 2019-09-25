package com.test.smartprobe.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.test.smartprobe.R;
import com.test.smartprobe.apicall.SyncServer;
import com.test.smartprobe.apicall.UpdateApp;
import com.test.smartprobe.apicall.VersionCheck;
import com.test.smartprobe.custom.MyApplication;
import com.test.smartprobe.database.SmartProbeDBOpenHelper;
import com.test.smartprobe.location.GPSTracker;
import com.test.smartprobe.util.AppUtil;
import com.test.smartprobe.util.LogUtil;
import com.test.smartprobe.util.USBUtil;
import com.test.smartprobe.webDisplay.CreateProbeList;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SmartProbeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    private static final int DEFAULT_LIMIT = 250;
    public int readingInterval = 1;
    public int sendingInterval = 1;
    boolean stop = false;
    private ProbeReadAndSaveClass prasc = null;
    public SQLiteDatabase db = null;
    private SmartProbeDBOpenHelper dbHelper;
    final String TAG = "SmartProbeApp";
    int MaxDevices = 0;
    final int[] myUSB_PID = {0x1058, 0xf668};
    int MaxPCounter = 0;
    private Thread updateUIProbeThread;
    public static Thread sendServerDataThread=new Thread();
    private Thread retriveServerDataThread;
    boolean isRunning = true;
    public WebView webView;
    public String username = null;
    public String password = null;
    private String lastUpdateTime = "";
    private HttpAsyncTaskProbeList httpAsyncTaskProbeList;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private String newline = System.getProperty("line.separator");
    private AlertDialog alertLocation;
    public GPSTracker gpsTracker;
    public static UsbSerialPort sPort = null;
    private BroadcastReceiver detachReceiver;
    private SQLiteStatement stmtInsertSmartprobe;
    private SQLiteStatement stmtUpdateSmartprobeLatest;
    public SQLiteStatement stmtInsertSmartprobeLatest;
    public SQLiteStatement stmtUpdateSmartprobeServer;
    private SyncServer syncServer;
    private SerialInputOutputManager mSerialIoManager;
    private StringBuilder readDataBuilder = new StringBuilder();
    public boolean trackCurrentLocation;
    private USBUtil usb;
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd\\MM\\yyyy HH:mm:ss");
    private CreateProbeList createWebContentProbeList;
    public boolean isEnabledAlarm = false;
    public static String lastAutoResartTime = "";

    private DrawerLayout dl;
    private ActionBarDrawerToggle t;
    private NavigationView nv;

    /**
     * Declare listener to keep listen to new data received event from smart probe device.
     */
    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {

                    /*Report bug if exception arise when data read*/
                    Crashlytics.logException(e);
                    LogUtil.writeLogTest("Exception occurred runner stopped.");
                    Log.d(TAG, "Runner stopped.");
                    stopIoManager();
                    /*
                    Restart app if any exception occurred while usb is connected...
                    Otherwise no data will get read until we restart application...
                     */
                    //MyApplication.restartApp(SmartProbeActivity.this);

                }

                @Override
                public void onNewData(final byte[] data) {
                            /*
                            Process data if new data received from USB device,
                             */
                    SmartProbeActivity.this.updateReceivedData(data);

                }
            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtil.setupActivity(this);
        setContentView(R.layout.smart_probe_activity_main);

        checkPermission();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

//        toolbar.setNavigationIcon(R.drawable.ic_launcher);

        /*
        Declare broadcast event to keep track of usb disconnect event.
         */
        setBroadcastEvent();

        gpsTracker = new GPSTracker(this);
        webView = (WebView) findViewById(R.id.webview);

        dl = (DrawerLayout)findViewById(R.id.activity_main);
        t = new ActionBarDrawerToggle(this, dl,R.string.Open, R.string.Close);

        dl.addDrawerListener(t);
        t.syncState();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        nv = (NavigationView)findViewById(R.id.nv);
        nv.setNavigationItemSelectedListener(this);

        // Creating database for SmartProbe
        checkDB();
        getUserInfo();

        if (username == null || password == null) {
            Intent mainIntent = new Intent(SmartProbeActivity.this, LoginActivity.class);
            SmartProbeActivity.this.startActivity(mainIntent);
            SmartProbeActivity.this.finish();
        } else if (!isLocationFound()) {

            // showAllLocations();
            showLocationSetAlert();
        }
        initResources();
    }

    private void checkPermission() {

        if (Build.VERSION.SDK_INT >= 21) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        1);

            }

        }
    }


    /**
     * Initialize required resources to process
     */
    private void initResources() {

        LogUtil.writeServerStatus("********************** INIT RESOURCES **********************");
        getReadandSendInterval();
        // loadProbeList(false);

        createWebContentProbeList = new CreateProbeList(this, webView);
        syncServer = new SyncServer(username, password, lastUpdateTime, this);

        syncFromServer();
        //new CheckTimeIntervalsFromLocalDB().execute();
        startUIRefreshThread();
        syncToServer();
        checkAppVersion(false);
    }

    /**
     * Initiate thread to refresh UI
     */
    int count = 0;

    private void startUIRefreshThread() {

        updateUIProbeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                LogUtil.writeLogTest("** updateUIProbeThread stop = " + stop);
                while (!stop) {

                    ////////TEST
                 /* if (prasc == null)
                        prasc = new ProbeReadAndSaveClass();

                    String[] s={"SP-MDK_1001","23.7","a","_","2.0","a","-","0262"};
                    if(prasc!=null)
                    prasc.insertData(s);

                    prasc.processReadData("sss,23.7,a,_,2.0,a,-,0262");
                    //shogun user
                    prasc.processReadData("sss1,23.7,a,_,2.0,a,-,0262");
                    prasc.processReadData("sss2,23.7,a,_,2.0,a,-,0262");
                    prasc.processReadData("sss3,23.7,a,_,2.0,a,-,0262");


                    //calpine user
                    prasc.processReadData("SP-MDK_1001,25,a,_,2.0,a,-,0262");
                    prasc.processReadData("SSP-MDK_1001,23.7,a,_,2.0,a,-,0262");
                    prasc.processReadData("pppp,23.7,a,_,2.0,a,-,0262");
                    prasc.processReadData("SP-SHN_0003,23.7,a,_,2.0,a,-,0262");
//                    prasc.processReadData("SP-AIQ_0014,15.4,a,_,3.4,a,-,0262");
//                    prasc.processReadData("SP-MDK_1002,23.7,a,_,2.0,a,-,0262");
//                    prasc.processReadData("SP-MKP_0011,23.7,a,_,2.0,a,-,0262");

//                    prasc.processReadData("SP-MDK_1001,23.7,a,_,2.0,a,-,0262");
//                    prasc.processReadData("SP-MDK_1002,23.7,a,_,2.0,a,-,0262");
//                    prasc.processReadData("SP-MDK_1003,23.7,a,_,2.0,a,-,0262");

                 /*   count++;
                    if (count == 1) {
                        prasc.processReadData("SP-MDK_1001,23.7,a,_,2.0,a,-,0262");
                        prasc.processReadData("SP-MDK_1002,23.7,a,_,2.0,a,-,0262");
                        prasc.processReadData("SP-MDK_1003,23.7,a,_,2.0,a,-,0262");
                    } else if (count == 2)
                        prasc.processReadData("SP-MDK_1001,18,a,_,2.0,a,-,0262");
                    else if (count == 3) {
                        prasc.processReadData("SP-MDK_1001,19,a,_,2.0,a,-,0262");
                        prasc.processReadData("SP-MDK_1001,23.7,a,_,2.0,a,-,0262");
                        prasc.processReadData("SP-MDK_1002,23.7,a,_,2.0,a,-,0262");
                        prasc.processReadData("SP-MDK_1003,23.7,a,_,2.0,a,-,0262");
                    } else if (count == 4) {
                        prasc.processReadData("SP-MDK_1001,20,a,_,2.0,a,-,0262");
                    } else if (count == 5)
                        prasc.processReadData("SP-MDK_1001,18,a,_,2.0,a,-,0262");
                    else if (count == 6)
                        prasc.processReadData("SP-MDK_1001,19,a,_,2.0,a,-,0262");
                    else if (count == 7)
                        prasc.processReadData("SP-MDK_1001,20,a,_,2.0,a,-,0262");
                    else if (count == 8)
                        prasc.processReadData("SP-MDK_1001,18,a,_,2.0,a,-,0262");
                    else if (count == 9)
                        prasc.processReadData("SP-MDK_1001,55,a,_,2.0,a,-,0262");
                    else if (count == 10)
                        prasc.processReadData("SP-MDK_1001,23,a,_,2.0,a,-,0262");
                    else if (count == 11)
                        prasc.processReadData("SP-MDK_1002,12,a,_,2.0,a,-,0262");
                    else if (count == 12)
                        prasc.processReadData("SP-MDK_1001,23,a,_,2.0,a,-,0262");
                    else if (count == 13)
                        prasc.processReadData("SP-MDK_1001,18,a,_,2.0,a,-,0262");
                    else if (count == 14)
                        prasc.processReadData("SP-MDK_1002,23,a,_,2.0,a,-,0262");
                    else if (count == 15)
                        prasc.processReadData("SP-MDK_1001,23,a,_,2.0,a,-,0262");
                    else if (count == 16)
                        prasc.processReadData("SP-MDK_1003,12,a,_,2.0,a,-,0262");
                    else if (count == 17) {
                        prasc.processReadData("SP-MDK_1003,23,a,_,2.0,a,-,0262");
                        count = 0;
                    }*/
                    ////////TEST

                    updateView();
                    createWebContentProbeList.checkAutoLogout();
                    sleep(readingInterval);
                }

            }

            private void sleep(int value) {
                try {
                    LogUtil.writeLogTest("sleep for =" + value * 60 * 1000);
                    Thread.sleep(value * 60 * 1000);
                } catch (InterruptedException e) {
                    // TODO: handle exception
                    e.printStackTrace();
                }
            }
        });

        if (!updateUIProbeThread.isAlive()) {
            updateUIProbeThread.start();
        }


    }

    /**
     * To load probe list from server
     */
    private void loadProbeList(boolean isShown,boolean retrieveAll) {

        if (httpAsyncTaskProbeList != null && !httpAsyncTaskProbeList.isCancelled())
            httpAsyncTaskProbeList.cancel(true);
        httpAsyncTaskProbeList = new HttpAsyncTaskProbeList(isShown,retrieveAll);
        httpAsyncTaskProbeList.execute();
//        if (AppUtil.isConnected(SmartProbeActivity.this))
//            httpAsyncTaskProbeList.execute(getUrl());
//        else
//            Toast.makeText(getBaseContext(), "Please check your internet connection.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Get User information
     */
    private void getUserInfo() {

        Cursor clog = db.query(SmartProbeDBOpenHelper.TABLE_NAME_LOG, null, null, null, null, null, null);
        if (clog.moveToFirst()) {
            username = clog.getString(1);
            password = clog.getString(2);
            lastUpdateTime = clog.getString(3);
        }
        clog.close();
    }

    /**
     * Process data if new data received from USB device,
     *
     * @param data
     */
    private void updateReceivedData(byte[] data) {
        final String datast = new String(data, 0, data.length);

        readDataBuilder.append(datast);

        /*To check line break,if line break found process probe details */
        if (datast.equals(newline)) {
            LogUtil.writeLogTest("start processReadData");
            /*To process data line by line*/
            prasc.processReadData(readDataBuilder.toString());

            //after read data reset builder
            readDataBuilder.setLength(0);
            LogUtil.writeLogTest("finish processReadData");
        }
    }


    /*
           Declare broadcast event to keep track of usb disconnect event.
    */
    private void setBroadcastEvent() {

        detachReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                    Toast.makeText(SmartProbeActivity.this, "USB Disconnected", Toast.LENGTH_LONG).show();
                    clearUsbResource();

                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(detachReceiver, filter);
    }

    /*
       To show alert if location not found when activity loaded for first time
    */
    public void showVersionUpdate(String latestVersion,final String url) {

        AlertDialog.Builder builder1 = new AlertDialog.Builder(this)
                .setTitle("Update available")
                .setMessage("A new version "+latestVersion+" is available.")
                .setIcon(R.drawable.ic_launcher)
                .setCancelable(false)
                .setPositiveButton(R.string.updateNow, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {

                        UpdateApp updateApp = new UpdateApp(SmartProbeActivity.this);
                        updateApp.execute(url);

                    }
                })
                .setNegativeButton(android.R.string.no, null);

        AlertDialog alert = builder1.create();
        alert.show();

    }
    /*
      To show alert if location not found when activity loaded for first time
       */
    public void noUpdateDialog() {

        AlertDialog.Builder builder1 = new AlertDialog.Builder(this)
                .setTitle("No update available.")
                .setMessage("You are using latest version.")
                .setIcon(R.drawable.ic_launcher)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, null)
                ;

        AlertDialog alert = builder1.create();
        alert.show();

    }

    private void checkAppVersion(boolean showProgress) {

        if (AppUtil.isConnected(this)) {

            VersionCheck versionCheck=new VersionCheck(this,showProgress);
            versionCheck.execute("");
        } else {
            Toast.makeText(getBaseContext(), "Please check your internet connection.", Toast.LENGTH_LONG).show();
        }

    }

    /*
    To show alert if location not found when activity loaded for first time
     */
    private void showLocationSetAlert() {

        AlertDialog.Builder builder1 = new AlertDialog.Builder(this)
                .setTitle("Location not found")
                .setMessage("Location not found for current user, do you want to set now?")
                .setIcon(R.drawable.ic_launcher)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {

                        Intent myIntentLocation = new Intent(SmartProbeActivity.this,
                                LocationSettingActivity.class);
                        startActivity(myIntentLocation);
                        //SmartProbeActivity.this.finish();
                        dialog.cancel();

                    }
                })
                .setNegativeButton(android.R.string.no, null);

        alertLocation = builder1.create();
        alertLocation.show();

    }

    /*
    Check location is added for particular user
     */
    public boolean isLocationFound() {

        Cursor c = db.query(SmartProbeDBOpenHelper.TABLE_NAME_ACC, null, SmartProbeDBOpenHelper.USER_NAME + "='" + username + "'", null, null, null, null);
        if (c.moveToFirst()) {
            return true;
        }
        c.close();
        return false;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        dl.closeDrawer(GravityCompat.START);
        int id = item.getItemId();
        switch(id)
        {
            case R.id.nav_location:
                try {
                    webView.clearCache(true);
                    webView.clearHistory();
                    Intent myIntentLocation = new Intent(this, LocationSettingActivity.class);
                    startActivity(myIntentLocation);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.nav_settings:
                try {
                    Intent myIntent = new Intent(this, SettingActivity.class);
                    startActivityForResult(myIntent, SettingActivity.REQUEST_CODE);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                }
                break;
            case R.id.nav_reload:
                try {
                    loadProbeList(true, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.nav_reset:
                try {
                    LogUtil.writeLogTest("+++++  Reset alarm ++++");
                    createWebContentProbeList.resetAlarm();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.nav_check_update:
                try {
                    checkAppVersion(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.nav_logout:
                try {
                    logout(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                return true;
        }
        return false;
    }


    /*
     Get active probe list with given username and password...
      */
    private class HttpAsyncTaskProbeList extends AsyncTask<String, Void, Boolean> {

        private final boolean showProgress;
        private final boolean retrieveAll;
        private ProgressDialog progressDialog;

        public HttpAsyncTaskProbeList(boolean showProgress,boolean retrieveAll) {
            this.showProgress = showProgress;
            this.retrieveAll=retrieveAll;
        }

        @Override
        protected Boolean doInBackground(String... urls) {

            boolean isConnected = true;
            try {
                if (AppUtil.isConnected(SmartProbeActivity.this))
                    syncServer.retrieveProbeDetailsFromServer(retrieveAll);
                else
                    isConnected = false;

            } catch (Exception e) {
                e.printStackTrace();
            }

            return isConnected;
        }

        @Override
        protected void onPreExecute() {

            if (showProgress) {
                progressDialog = new ProgressDialog(SmartProbeActivity.this);
                progressDialog.setMessage("Loading data...");
                progressDialog.setCancelable(true);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();
            }
        }

        @Override
        protected void onPostExecute(Boolean isConnected) {

            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();

            createWebContentProbeList.displayLatestData();

            if (!isConnected)
                Toast.makeText(getBaseContext(), "Sync from server failed, please check your internet connection.", Toast.LENGTH_SHORT).show();
        }


    }


    /*
   Before application logout make sure all data is send to server.
   If all data is send successfully, clear all resources and exit.
    */
    private class LogoutTask extends AsyncTask<String, Void, String> {
        private boolean isAutoLogout = false;
        private ProgressDialog progressDialog;

        public LogoutTask(boolean isAutoLogout) {
            this.isAutoLogout = isAutoLogout;
        }

        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            try {
                db.delete(SmartProbeDBOpenHelper.TABLE_NAME_LOG, null, null);
                Cursor c = syncServer.readProbeData(DEFAULT_LIMIT);
                LogUtil.writeLogTest("********* Logout process started *********");
                LogUtil.writeLogTest("Logout : read probe data count = " + c.getCount());
                /*Check any dat left to send to server*/
                if (c.getCount() > 0) {

                    /*
                    If data still exist before logout we need to send all data to server.
                     */
                    stopThread();
                    LogUtil.writeLogTest("Logout : sync data before logout");
                    syncServer.sendToServer(500);
                    /*
                    Clear all resource after send data to server.
                     */
                    clearAllReadFromLatest();
                } else {
                    db.delete(SmartProbeDBOpenHelper.TABLE_NAME_LATEST, null, null);
                    LogUtil.writeLogTest("********* Logout : clear all records ");
                }
                c.close();

            } catch (Exception e) {
                result = e.getLocalizedMessage();
            }

            //Log.d(TAG, result);

            return result;
        }

        @Override
        protected void onPreExecute() {

            progressDialog = new ProgressDialog(SmartProbeActivity.this);
            progressDialog.setMessage("Please wait..");
            progressDialog.setCancelable(true);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }

        protected void onPostExecute(String responseData) {

            if (progressDialog != null)
                progressDialog.dismiss();

            logoutSuccess(isAutoLogout);
        }
    }

    private void logoutSuccess(boolean isAutoLogout) {

        LogUtil.writeLogTest(">>>>> LOGOUT SUCCESS <<<<<<<<< ");
        LogUtil.writeLogTest("");

        Intent myIntentLogout = new Intent(SmartProbeActivity.this, LoginActivity.class);
        if (isAutoLogout) {
            myIntentLogout.putExtra("Username", username);
            myIntentLogout.putExtra("Password", password);
            lastAutoResartTime = dateTimeFormat.format(new Date());
        }

        startActivity(myIntentLogout);
        SmartProbeActivity.this.finish();
    }

    /*
    Initialize database required object before use.
    Initialize prepare statement to perform bulk operations.
     */
    public void checkDB() {

        if (isRunning && db == null) {
            dbHelper = new SmartProbeDBOpenHelper(this);
            db = dbHelper.getWritableDatabase();

            //Create prepared statements
            stmtInsertSmartprobe = db.compileStatement("INSERT INTO " + SmartProbeDBOpenHelper.TABLE_NAME + "(" + SmartProbeDBOpenHelper.PROBESERIALNUMBER + ", " + SmartProbeDBOpenHelper.TIME + ", " + SmartProbeDBOpenHelper.DATE + ", "
                    + SmartProbeDBOpenHelper.TEMPERATURE_HIGH + ", " + SmartProbeDBOpenHelper.STATUS_1 + ", " + SmartProbeDBOpenHelper.STATUS_2 + ", " + SmartProbeDBOpenHelper.TEMPERATURE_LOW + ", " +
                    SmartProbeDBOpenHelper.TRIP_1 + ", " + SmartProbeDBOpenHelper.TRIP_2 + ", " + SmartProbeDBOpenHelper.VOLTAGE + ") VALUES(?,?,?,?,?,?,?,?,?,?)");

            stmtInsertSmartprobeLatest = db.compileStatement("INSERT INTO " + SmartProbeDBOpenHelper.TABLE_NAME_LATEST + "(" + SmartProbeDBOpenHelper.PROBESERIALNUMBER + ", " +
                    SmartProbeDBOpenHelper.PROBE_NAME + ", " + SmartProbeDBOpenHelper.USER_NAME + ", " + SmartProbeDBOpenHelper.ALERT_HIGH + ", " + SmartProbeDBOpenHelper.ALERT_LOW + ", " +
                    SmartProbeDBOpenHelper.WARNING_HIGH + ", " + SmartProbeDBOpenHelper.WARNING_LOW + ", " + SmartProbeDBOpenHelper.UNIT + ", " + SmartProbeDBOpenHelper.LOW_VOLTAGE + ", " + SmartProbeDBOpenHelper.DEFAULT_SENSOR + ") VALUES(?,?,?,?,?,?,?,?,?,?)");

            stmtUpdateSmartprobeLatest = db.compileStatement("UPDATE " + SmartProbeDBOpenHelper.TABLE_NAME_LATEST + " SET " + SmartProbeDBOpenHelper.PROBESERIALNUMBER + "=?," + SmartProbeDBOpenHelper.TIME + "=?," + SmartProbeDBOpenHelper.DATE + "=?,"
                    + SmartProbeDBOpenHelper.TEMPERATURE_HIGH + "=?," + SmartProbeDBOpenHelper.STATUS_1 + "=?," + SmartProbeDBOpenHelper.STATUS_2 + "=?," + SmartProbeDBOpenHelper.TEMPERATURE_LOW + "=?," +
                    SmartProbeDBOpenHelper.TRIP_1 + "=?," + SmartProbeDBOpenHelper.TRIP_2 + "=?," + SmartProbeDBOpenHelper.VOLTAGE + "=? WHERE  " + SmartProbeDBOpenHelper.PROBESERIALNUMBER + " =?");

            stmtUpdateSmartprobeServer = db.compileStatement("UPDATE " + SmartProbeDBOpenHelper.TABLE_NAME_LATEST + " SET " + SmartProbeDBOpenHelper.PROBE_NAME + "=?," + SmartProbeDBOpenHelper.ALERT_HIGH + "=?," + SmartProbeDBOpenHelper.ALERT_LOW + "=?,"
                    + SmartProbeDBOpenHelper.WARNING_HIGH + "=?," + SmartProbeDBOpenHelper.WARNING_LOW + "=?," + SmartProbeDBOpenHelper.UNIT + "=?," + SmartProbeDBOpenHelper.LOW_VOLTAGE + "=?," + SmartProbeDBOpenHelper.DEFAULT_SENSOR + "=?  WHERE  " + SmartProbeDBOpenHelper.PROBESERIALNUMBER + " =?");
        }
    }


    /*
    Inside AsyncTask updateUIProbeThread is started to update UI.
    Async task is using to load data faster when application loaded for first time.
    Otherwise white screen will show for long time.
     */
    class CheckTimeIntervalsFromLocalDB extends AsyncTask<Void, Void, Void> {

        // * @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            // super.onPreExecute();
            // prasc = new ProbeReadAndSaveClass();
            LogUtil.writeLogTest("** updateUIProbeThread onPreExecute **");
        }

        // * /
        @Override
        protected Void doInBackground(Void... params) {
            // TODO Auto-generated method stub
            LogUtil.writeLogTest("** updateUIProbeThread doInBackground **");
            updateUIProbeThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    LogUtil.writeLogTest("** updateUIProbeThread stop = " + stop);
                    while (!stop) {

                        updateView();
                        sleep(readingInterval);
                    }

                }
            });

            if (!updateUIProbeThread.isAlive()) {
                updateUIProbeThread.start();
            }

            return null;
        }

        private void sleep(int value) {
            try {
                LogUtil.writeLogTest("sleep for update =" + value * 60 * 1000);
                Thread.sleep(value * 60 * 1000);
            } catch (InterruptedException e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }

    }


    /**
     * While USB is connected this method is invoked to initialize usb port and startlistenerr to read data.
     */
    public void loadUSBDevices() {
        if (prasc == null)
            prasc = new ProbeReadAndSaveClass();

//        if (sPort == null) {

        usb = new USBUtil();
        ArrayList<String> deviceList = usb.GetUSBSerialDevices(SmartProbeActivity.this);
        MaxDevices = deviceList.size();
        // sPort = null;

        Log.d(TAG, "Connected CCS Equipment: " + MaxDevices);
        LogUtil.writeLogTest("Connected CCS Equipment: " + MaxDevices);

        for (int i = 0; i < MaxDevices; i++) {

            LogUtil.writeLogTest("Dev name: " + deviceList.get(i) + "PID: " + usb.getProductId(i));
            Log.i(TAG, "Dev name: " + deviceList.get(i) + "PID: " + usb.getProductId(i));
            for (int j = 0; j < myUSB_PID.length; j++) {
                if (usb.getProductId(i) == myUSB_PID[j]) {
                    MaxPCounter++;
                    Log.i(TAG, "Selected Dev name: " + deviceList.get(i) + "PID: " + usb.getProductId(i));
                    LogUtil.writeLogTest("Selected Dev name: " + deviceList.get(i) + "PID: " + usb.getProductId(i));
                    break;
                }
            }
            LogUtil.writeLogTest("MaxPCounter =" + MaxPCounter);
            if (MaxPCounter > 0) // Max Probes instead of file
            // list
            {

                if (usb.openPort(0)) {
                    sPort = usb.getPort();

                    LogUtil.writeLogTest("openPort");
                } else {

                    Toast.makeText(this, R.string.msg_device_not_connected, Toast.LENGTH_LONG).show();
                    clearUsbResource();
                    LogUtil.writeLogTest("not openPort");
                }

            }
        }
//        } else
//            MaxPCounter = 1;

        if (MaxPCounter > 0 && sPort != null)
            onDeviceStateChange();
    }

    /*
    Class to process read data and save the data to local db.
    The process data is send to server later when seperate thread invoked.
     */
    private class ProbeReadAndSaveClass {

        /**
         * Called when the activity is first created.
         */
        public ProbeReadAndSaveClass() {
            Log.d(TAG, "Smart Probe searches for compatible USB devices ...");

            Log.d(TAG, "End of On create ....");
        }

        /**
         * To process read data from USB device
         *
         * @param recData
         */
        private synchronized void processReadData(String recData) {


            Log.d(TAG, "No of records  = " + recData.split(newline).length);
            String recRecords[] = recData.split(newline);

            db.beginTransaction();
            try {
                //If contain multi probe record
                for (String rec : recRecords) {


                    String[] Splited_data = rec.split(",");

                    Calendar c = Calendar.getInstance();
                    String seconds = c.getTime().toString();
                    System.out.println("START: " + seconds);
                    // ToDo Get Probe Trip ?


                    //Here length is minimum 7 because we have inserted seven column minimum from this array..
                    if ((Splited_data.length > 6)) {
                        seconds = c.getTime().toString();
                        System.out.println("END: " + seconds);


                        //If data start with ',' we need to remove it
                        if (Splited_data[0] == null || Splited_data[0].trim().isEmpty()) {
                            //to remove unwanted data keep new array
                            String[] dataArray = new String[Splited_data.length - 1];
                            //copy values
                            System.arraycopy(Splited_data, 1, dataArray, 0, Splited_data.length - 1);

                            Splited_data = dataArray;
                        }

//                    for (int j = 0; j < Splited_data.length; j++) {
//                        Log.d(TAG, "No " + j + " node: " + Splited_data[j]);
//                    }

//                            //Insert data to DB
//                            insertData(Splited_data);


                        //To check probe is valid
                        if (Splited_data[0] != null && Splited_data[0].toUpperCase().startsWith("SP-") && Splited_data.length < 10) {
                            LogUtil.writeDeviceLog("Received Data : " + rec);
                            //Insert data to DB
                            insertData(Splited_data);
                        } else
                            LogUtil.writeDeviceLog(">>> Received Bad Data : " + rec);
                    } else
                        LogUtil.writeDeviceLog(">>> Received Bad Data : " + rec);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {

                db.setTransactionSuccessful();
                db.endTransaction();
            }
        }

        /**
         * This method is used to save readed probe details into table.
         *
         * @param data
         */
        private void insertData(String[] data) {
            try {

                bindDataToStatement(stmtInsertSmartprobe, data);


                stmtInsertSmartprobe.executeInsert();
                stmtInsertSmartprobe.clearBindings();

                boolean checkSerial = isProbeExist(data[0]);
                if (checkSerial) {
                    bindDataToStatement(stmtUpdateSmartprobeLatest, data);
                    stmtUpdateSmartprobeLatest.bindString(11, data[0]);
                    stmtUpdateSmartprobeLatest.executeUpdateDelete();
                    stmtUpdateSmartprobeLatest.clearBindings();
                }
                //No need to show smart probe on display list ,if its not authorized by current user.
               /* else {

                    bindDataToStatement(stmtInsertSmartprobeLatest, data);

                    //Fill extra fields
                    stmtInsertSmartprobeLatest.bindString(11, "");
                    stmtInsertSmartprobeLatest.bindString(12, "");
                    stmtInsertSmartprobeLatest.bindString(13, "");
                    stmtInsertSmartprobeLatest.bindString(14, "");
                    stmtInsertSmartprobeLatest.bindString(15, "");

                    stmtInsertSmartprobeLatest.executeInsert();
                    stmtInsertSmartprobeLatest.clearBindings();
                }*/

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private boolean isProbeExist(String probeSerial) {
            int probeSerialCount;
            Cursor mCount = db.rawQuery("select count(*) from "
                    + SmartProbeDBOpenHelper.TABLE_NAME_LATEST + " where "
                    + SmartProbeDBOpenHelper.PROBESERIALNUMBER + " = '"
                    + probeSerial + "'", null);
            if (mCount.moveToFirst()) {
                probeSerialCount = mCount.getInt(0);
            } else {
                probeSerialCount = 0;
            }
            mCount.close();
            if (probeSerialCount <= 0) {
                return false;
            } else {
                return true;
            }

        }

    }

    /**
     * For bulk data execution,we can to bind arguments.
     *
     * @param statement
     * @param data
     */
    private void bindDataToStatement(SQLiteStatement statement, String[] data) {

        Calendar c = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

        Date now = new Date();
        Date alsoNow = Calendar.getInstance().getTime();
        String dateFormat = new SimpleDateFormat("dd\\MM\\yyyy")
                .format(now);

        statement.bindString(1, data[0]);
        statement.bindString(2, timeFormat.format(c.getTime())
                .toString());
        statement.bindString(3, dateFormat);
        statement.bindString(4, data[1]);
        statement.bindString(5, data[2]);
        statement.bindString(6, data[5]);
        statement.bindString(7, data[4]);
        statement.bindString(8, data[3]);
        statement.bindString(9, data[6]);
        if (data.length >= 8)
            statement.bindString(10, data[7]);
        else
            statement.bindString(10, "");

    }

    public synchronized void updateView() {
        LogUtil.writeLogTest("start updateView");

              /*  if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();
                progressDialog = new ProgressDialog(SmartProbeActivity.this);
                progressDialog.setMessage("Please wait..");
                progressDialog.setCancelable(true);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();*/

        LogUtil.writeLogTest("start updateView inside handler");
        // Display data from latest table
        createWebContentProbeList.displayLatestData();

    }

	/*
    Thread to send data to server if new data came.

     * @Override protected Void doInBackground(Void... params) {
	 */

    private void syncToServer() {

        sendServerDataThread = new Thread(new Runnable() {


            @Override
            public void run() {
                // TODO Auto-generated method stub

                // TODO Auto-generated method stub
                while (!stop) {   //while start here

                    try {
                        syncServer.sendToServer(DEFAULT_LIMIT);
                    } catch (Exception e) {
                        LogUtil.writeServerStatus(">>>>> ERROR OCCURRED INSIDE THREAD : " + e.getMessage() + " <<<<<<<<< ");
                    }

                    sleep(sendingInterval);
                }   //While loop end here
            }

            private void sleep(int value) {
                try {
                    LogUtil.writeLogTest("sleep for =" + value * 60 * 1000);
                    Thread.sleep(value * 60 * 1000);
                } catch (InterruptedException e) {
                    // TODO: handle exception
                    e.printStackTrace();
                }
            }
        });
        if (!sendServerDataThread.isAlive()) {
            sendServerDataThread.start();
            //Log.d("sendServerDataThread Status: ", "Now starting..!");
        }
        // return null;
    }
    /*
    Thread to send data from server if new data came.

     * @Override protected Void doInBackground(Void... params) {
	 */

    private void syncFromServer() {

        retriveServerDataThread = new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                Log.e("SmartProbe", "syncFromServer inside thread " + stop);

                // TODO Auto-generated method stub
                while (!stop) {   //while start here
                    Log.e("SmartProbe", "syncFromServer start thread ");
                    if (AppUtil.isConnected(SmartProbeActivity.this))
                        syncServer.retrieveProbeDetailsFromServer(false);

                    Log.e("SmartProbe", "syncFromServer stop thread ");
                    sleep();
                }   //While loop end here
            }

            private void sleep() {
                try {
                    //Sleep for 1 hour
                    Thread.sleep(60 * 60 * 1000);
                } catch (InterruptedException e) {
                    // TODO: handle exception
                    e.printStackTrace();
                }
            }
        });
        if (!retriveServerDataThread.isAlive()) {
            retriveServerDataThread.start();

        }
        // return null;
    }


    public void trackLocation() {

        if (!gpsTracker.canGetLocation()) {
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gpsTracker.showSettingsAlert();
        } else if (trackCurrentLocation && gpsTracker != null && gpsTracker.getLocation() == null)
            gpsTracker.trackLocation();
        else
            LogUtil.writeLogTest("Location = " + gpsTracker.getLatitude() + " , " + gpsTracker.getLongitude());
    }


    private void clearAllReadFromLatest() {
        // db.delete(SmartProbeDBOpenHelper.TABLE_NAME, "1", null);
        // db.execSQL("DELETE * FROM "+SmartProbeDBOpenHelper.TABLE_NAME);

        db.execSQL("DELETE FROM "
                + SmartProbeDBOpenHelper.TABLE_NAME_LATEST
                + " WHERE " + SmartProbeDBOpenHelper.PROBESERIALNUMBER + " NOT IN (select distinct " + SmartProbeDBOpenHelper.PROBESERIALNUMBER + " from " + SmartProbeDBOpenHelper.TABLE_NAME + ")");
        LogUtil.writeLogTest("********* Logout : clear synced records ");

    }
    // }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (t.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);

    }

    private void logout(boolean isAutoLogout) {

        try {
            webView.clearCache(true);
            webView.clearHistory();
            webView.clearView();
            new LogoutTask(isAutoLogout).execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void autoLogout() {

        stopThread();
        logoutSuccess(true);

        LogUtil.writeLogTest(">>>>>>>>>>>>>>> Invoked auto logout  <<<<<<<<<<<<<<<<<<<<<");
    }

    /*
    Get read and send time interval to set thread.
     */
    private void getReadandSendInterval() {

        if (db != null) {
            Cursor c = db.query(SmartProbeDBOpenHelper.TABLE_NAME_SETTING, null, null, null, null, null, null);
            if (c.moveToFirst()) {
                readingInterval = Integer.parseInt(c.getString(1));
                sendingInterval = Integer.parseInt(c.getString(2));
                trackCurrentLocation = (c.getInt(3) == 0) ? false : true;
                isEnabledAlarm = (c.getInt(4) == 0) ? false : true;
            }
            c.close();
        }
    }

    /**
     * Stop listener to read data from usb port if usb device is removed.
     */
    private void stopIoManager() {

        if (mSerialIoManager != null) {

            Log.i(TAG, "Stopping io manager ..");
            LogUtil.writeLogTest("Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    /**
     * Start listener to read data from usb port
     */
    private void startIoManager() {

        if (sPort != null) {

            Log.i(TAG, "Starting io manager ..");
            LogUtil.writeLogTest("Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);
        } else
            LogUtil.writeLogTest("Not Starting io manager ..");
    }

    /**
     * To check device connection status
     *
     * @return is connected or not
     */
    public boolean isAttached() {

        boolean status = false;
        if (MaxPCounter > 0 && sPort != null && sPort.isOpen())
            status = true;
        return status;
    }

    private void onDeviceStateChange() {

        LogUtil.writeLogTest("onDeviceStateChange");
        stopIoManager();
        startIoManager();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (gpsTracker != null && requestCode == gpsTracker.REQUEST_CHECK_SETTINGS && gpsTracker.getLocation() == null) {
            gpsTracker.trackLocation();
        } else if (requestCode == SettingActivity.REQUEST_CODE && resultCode == SettingActivity.RESULT_CODE) {
               /* Reset read and send time inteval*/
            getReadandSendInterval();
            Log.e("SmartProbe", "************on onActivityResult trackCurrentLocation : " + trackCurrentLocation);
            if (trackCurrentLocation)
                trackLocation();
            else if (!trackCurrentLocation && gpsTracker != null && gpsTracker.getLocation() != null)
                gpsTracker.stopUsingGPS();

        }
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        //stop = false;
        isRunning = true;
        ((MyApplication) this.getApplication()).stopActivityTransitionTimer();
        Log.e("SmartProb", "************on Resume " + MaxPCounter);
        // Creating database for SmartProbe
        if (db == null) {
            dbHelper = new SmartProbeDBOpenHelper(this);
            db = dbHelper.getWritableDatabase();
        }


        if (MaxPCounter == 0) {
            loadUSBDevices();
            createWebContentProbeList.displayLatestData();
        }

        createWebContentProbeList.resume();
        super.onResume();
    }


    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        //stop = true;
        // isRunning = false;
        ((MyApplication) this.getApplication()).startActivityTransitionTimer();
        createWebContentProbeList.pause();
        super.onPause();
    }

    @Override
    public void onBackPressed() {

        moveTaskToBack(true);
    }
//    @Override
//    public void onNewIntent(Intent newIntent) {
//        super.onNewIntent(newIntent);
//        MaxPCounter = 0;
//    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        Log.e("SmartProb", "************on Destroy *****");
        LogUtil.writeLogTest("****on Destroy *");
        clearResource();

        super.onDestroy();
    }

    private void clearUsbResource() {

        MaxPCounter = 0;

       /* if(usb!=null && usb.connection!=null)
            usb.connection.close();*/
        if (sPort != null && sPort.isOpen())
            try {
                sPort.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        sPort = null;
    }

    /**
     * Free all resource when application finish .
     */
    private void clearResource() {

        //db.close();
        stopThread();

        if (gpsTracker != null && gpsTracker.getLocation() != null)
            gpsTracker.stopUsingGPS();
        gpsTracker = null;
        try {

            createWebContentProbeList.resetAlarm();

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (httpAsyncTaskProbeList != null && !httpAsyncTaskProbeList.isCancelled())
            httpAsyncTaskProbeList.cancel(true);
        if (db != null && db.isOpen())
            db.close();
        db = null;

        syncServer = null;
        createWebContentProbeList = null;
        dbHelper = null;
        httpAsyncTaskProbeList = null;

        if (alertLocation != null) {
            alertLocation.dismiss();
            alertLocation = null;
        }

        stopIoManager();
        if (sPort != null && sPort.isOpen())
            try {
                sPort.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

//        if(usb!=null && usb.connection!=null)
//            usb.connection.close();
        MaxPCounter = 0;
        unregisterReceiver(detachReceiver);
        detachReceiver = null;
        if (stmtInsertSmartprobe != null)
            stmtInsertSmartprobe.clearBindings();
        stmtInsertSmartprobe = null;

        if (stmtUpdateSmartprobeServer != null)
            stmtUpdateSmartprobeServer.clearBindings();
        stmtUpdateSmartprobeServer = null;

        if (stmtUpdateSmartprobeLatest != null)
            stmtUpdateSmartprobeLatest.clearBindings();
        stmtUpdateSmartprobeLatest = null;

        if (stmtInsertSmartprobeLatest != null)
            stmtInsertSmartprobeLatest.clearBindings();
        stmtInsertSmartprobeLatest = null;

        System.gc();
    }

    private void stopThread() {

        isRunning = false;
        stop = true;
        LogUtil.writeServerStatus(">>>>> STOPPING THREAD <<<<<<<<< ");
    }

}