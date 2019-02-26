package com.test.smartprobe.webDisplay;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.test.smartprobe.activity.SmartProbeActivity;
import com.test.smartprobe.alert.SoundManager;
import com.test.smartprobe.database.SmartProbeDBOpenHelper;
import com.test.smartprobe.util.AppUtil;
import com.test.smartprobe.util.LogUtil;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by abitha on 28/6/16.
 */
public class CreateProbeList {

    private final int AUTO_RESTART_TIME = 15;
    private final SmartProbeActivity activity;
    private final WebView webView;
    private final SoundManager soundManager;
    private StringBuilder builder = new StringBuilder();
    private float voltage = 0f;
    private float low_voltage = 0f;
    private float temp = 0f;
    private float alertHigh = 0f;
    private float alertLow = 0f;
    private float warningHigh = 0f;
    private float warningLow = 0f;
    private String unit = "";
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd\\MM\\yyyy HH:mm:ss");
    private Handler mHandler = new Handler();
    private long timeDiff;
    private ArrayList<String> probesInAlertList;
    private String sensor;


    public CreateProbeList(SmartProbeActivity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
        soundManager = new SoundManager(activity);
        probesInAlertList = new ArrayList<String>();
    }

    /**
     * Invoke by thread to update UI
     * Webview is using to load probe list.
     */
    public synchronized void displayLatestData() {

        activity.checkDB();

        if (activity.db != null) {

            Calendar calendar = Calendar.getInstance();

            String localDateTime = dateTimeFormat.format(new Date());

            builder.setLength(0);
            builder.append("<html><HEAD><LINK href=\"style.css\" type=\"text/css\" rel=\"stylesheet\"/><script src='jquery.min.js'></script><script src='logdetails.js'></script><meta name='viewport' content='width=device-width, height=device-height'></HEAD>");
            builder.append("<body>");

            timeDiff = 0;

            isNewDataFound(localDateTime);

            builder.append("<div><ul>");
            createProbeInfoListItem();

//          Cursor cursor = db.query(SmartProbeDBOpenHelper.TABLE_NAME_LATEST,
//                    null, SmartProbeDBOpenHelper.DATE + "!=''", null, null, null, SmartProbeDBOpenHelper.DATE + " DESC," + SmartProbeDBOpenHelper.TIME + " DESC");
          /*  Cursor cursor = activity.db.query(SmartProbeDBOpenHelper.TABLE_NAME_LATEST,
                    null, SmartProbeDBOpenHelper.DATE + "!=''", null, null, null, null);*/

          //To show all probes
           /* Cursor cursor = activity.db.query(SmartProbeDBOpenHelper.TABLE_NAME_LATEST,
                    null, SmartProbeDBOpenHelper.USER_NAME + "='"+activity.username+"'", null, null, null, null);

            if (cursor.moveToFirst()) {
                do {
                    try {
                        //Create each probe item
                        if (cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.DATE)) != null && !cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.DATE)).equals("")) {
                            createProbeListItem(cursor);
                        } else {
                            createNoDataProbeItem(cursor);

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } while (cursor.moveToNext());
            } else {

                createNoDataListItem();

            }*/
            Cursor cursor = activity.db.query(SmartProbeDBOpenHelper.TABLE_NAME_LATEST,
                    null, SmartProbeDBOpenHelper.DATE + "!='' AND "+SmartProbeDBOpenHelper.USER_NAME + "='"+activity.username+"'", null, null, null, null);

            if (cursor.moveToFirst()) {
                do {
                    //Create each probe item
                    createProbeListItem(cursor);

                } while (cursor.moveToNext());
            } else {

                createNoDataListItem();

            }
            checkTempStatus();
            builder.append("</ul></div></body></html>");

            cursor.close();

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    webView.clearCache(true);
                    webView.clearHistory();
                    // webView.clearView();

                    webView.setWebViewClient(new WebViewClient());
                    webView.setWebChromeClient(new WebChromeClient());
                    WebSettings webSettings = webView.getSettings();
                    webSettings.setJavaScriptEnabled(true);
                    webSettings.setDomStorageEnabled(true);
                    webView.getSettings().setJavaScriptEnabled(true);
                    webView.getSettings().setDomStorageEnabled(true);
                    webView.addJavascriptInterface(new WebViewJavaScriptInterface(activity), "app");
                    webView.loadDataWithBaseURL("file:///android_asset/",
                            builder.toString(), "text/html", "utf-8", null);
                    //webView.loadUrl("file:///android_asset/probecopy.html");

                }
            });
        }
    }

    private void checkTempStatus() {


        if (probesInAlertList.size() == 0) {
            soundManager.isTempReset = false;
            soundManager.stopWakeUpAlert();
            soundManager.stoppedWakeUpAlert = true;
        } else if (probesInAlertList.size() != 0 && !soundManager.isTempReset) {

            soundManager.startWakeUpAlert();
        }

    }

    /*
            * JavaScript Interface. Web code can access methods in here
            * (as long as they have the @JavascriptInterface annotation)
            */
    public class WebViewJavaScriptInterface {

        private Context context;

        /*
         * Need a reference to the context in order to sent a post message
         */
        public WebViewJavaScriptInterface(Context context) {
            this.context = context;
        }

        /*
         * This method can be called from Android. @JavascriptInterface
         * required after SDK version 17.
         */
        @JavascriptInterface
        public void makeToast(String serialno) {
            //  Toast.makeText(context, serialno,  Toast.LENGTH_LONG).show();

            SharedPreferences prefs = activity.getSharedPreferences("SmartProbePrefs", activity.MODE_PRIVATE);
            int probe = prefs.getInt("chk_probe", 0);
            if(probe==1) {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://smartprobe.com.au/log.php?probe_name=" + serialno)));
            }
           /* Intent intent = new Intent(activity, LogDetailsActivity.class);
            intent.putExtra("serialno", serialno);
            activity.startActivity(intent);*/

        }
    }

    /**
     * Create No data list item if no probe read.
     */
    private void createNoDataListItem() {

        String nowAsString = dateTimeFormat.format(new Date());

        //builder.append("<li><p class='name'>No Device</p><p class='temp'>No Data</p><p class='alarm'>No Alarm</p><p class='date'>" + nowAsString + "</p>");
        builder.append("<li>\n" +
                "<div class='column'>\n" +
                "<div class='part1' id='part1'><div id='part1-content'>No Device</div></div>\n" +
                "<div class='part2'><p>No Data</p></div>\n" +
                "<div class='part3 '><p>No Alarm</p></div>\n" +
                "<div class='part4 '><p>" + nowAsString + "</p></div>\n" +
                "</div> </li>");
    }

    /**
     * Create probe list from information stored in table
     *
     * @param cursor
     */
    private void createProbeListItem(Cursor cursor) {

        voltage = getFloat(cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.VOLTAGE)));
        low_voltage = getFloat(cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.LOW_VOLTAGE)));

        unit = cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.UNIT));
        sensor = cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.DEFAULT_SENSOR));
        getTempBasedOnSensor(cursor);
        alertHigh = getFloat(cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.ALERT_HIGH)));
        alertLow = getFloat(cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.ALERT_LOW)));
        warningHigh = getFloat(cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.WARNING_HIGH)));
        warningLow = getFloat(cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.WARNING_LOW)));
        temp = convertTemp(temp);


//        builder.append("<li onclick=\"showToast('hi...>');\">");
        // builder.append("<li onclick=\"showToast('"+cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.PROBESERIALNUMBER))+"');\">");

        builder.append("<li id='" + cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.PROBESERIALNUMBER)) + "' class='clicked'>");
        builder.append("<div class='column'>");

        displayProbeName(cursor);
        displayTemp();
        displayTempStatus(cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.PROBESERIALNUMBER)));
        displayTimeAndVoltage(cursor);

        builder.append("</div></li>");
    }

    private void createNoDataProbeItem(Cursor cursor) {

        builder.append("<li id='" + cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.PROBESERIALNUMBER)) + "' class='clicked'>");
        builder.append("<div class='column'>");
        displayProbeName(cursor);
        builder.append("<div class='part2new'><p>No Data</p></div>\n" +
                "");
       /* builder.append("<div class='part4'><p class='textRed'> No data yet </p></div>");*/
        builder.append("</div></li>");
    }

    private void getTempBasedOnSensor(Cursor cursor) {

        //If sensor value not set default sensor well be sensor 1
        if (sensor == null || sensor.equals("") || sensor.equals("null") || sensor.toLowerCase().equals("sensor 1") || sensor.toLowerCase().equals("sensor1"))
            temp = getFloat(cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.TEMPERATURE_HIGH)));
        else
            temp = getFloat(cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.TEMPERATURE_LOW)));
    }

    /**
     * display probe name if exist other wise serial no.
     *
     * @param cursor
     */
    private void displayProbeName(Cursor cursor) {

        builder.append("<div class='part1' id='part1'><div id='part1-content'>");
        String probeName = cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.PROBE_NAME));

        if (probeName == null || probeName.equals("") || probeName.equals("null"))
            builder.append(cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.PROBESERIALNUMBER)));
        else
            builder.append(probeName);

        builder.append("</div>");

        // showLowVoltage();
        builder.append("</div>");
    }

    /**
     * Show low voltage blink indication
     */
    private void showLowVoltage() {

        if (voltage <= low_voltage)
            builder.append("<img class='voltage blink' src='low-battery.png'/>");
    }

    /**
     * Display probe read time and voltage read
     *
     * @param cursor
     */
    private void displayTimeAndVoltage(Cursor cursor) {

        //Find time difference of each probe
        timeDiff = getInActiveTime(cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.DATE)), cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.TIME)));
        builder.append("<div class='part4'>");

        displayTimeDiff(cursor);
        displayVoltage();
        showLowVoltage();
        builder.append("</div>");
    }

    private String formatFloat(int x) {

        if (x != 0) {
            DecimalFormat df = new DecimalFormat("#.00"); // Set your desired format here.
            return df.format(x / 100.0);
        } else
            return String.valueOf(x);
    }

    /**
     * Display volatge value of probe
     */
    private void displayVoltage() {

        //If invalid value set to 400 as max voltage
        if (voltage > 400)
            voltage = 400;

        if (voltage <= low_voltage)
            builder.append("<p class='voltage vol textRed move' src='low-battery.png'> " + formatFloat(((int) voltage)) + " V</p>");
        else
            builder.append("<p class='voltage vol' src='low-battery.png'> " + formatFloat(((int) voltage)) + " V</p>");
    }

    /**
     * Calculate time diffrence and show in list
     *
     * @param cursor
     */
    private void displayTimeDiff(Cursor cursor) {

        if (timeDiff >= 5 && timeDiff < 10)
            builder.append("<p class='textOrange left'>");
        else if (timeDiff >= 10)
            builder.append("<p class='textRed left'>");
        else
            builder.append("<p class='textWhite left'>");

        if (timeDiff == 0)
            builder.append("Just now");
        else if (timeDiff <= 60)
            builder.append(timeDiff + " mins ago");
        else if (isCurrentDate(cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.DATE))))
            builder.append("1+ hrs ago");
        else
            builder.append(cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.DATE)) + " " + cursor.getString(cursor.getColumnIndex(SmartProbeDBOpenHelper.TIME)));

        builder.append("</p>");
    }

    /**
     * Check temperature status and ,show status in list
     *
     * @param serialno
     */
    private void displayTempStatus(String serialno) {
        Log.e("Temp", "Temp - " + temp);

        if (temp >= alertHigh) {

            addTempStatus(serialno);
            builder.append("<div class='part3 red'>" +
                    "<p>Status: Very High");
        } else if (temp >= warningHigh) {

            addTempStatus(serialno);
            builder.append("<div class='part3 orange'>" +
                    "<p>Status: High ");
        } else if (temp < warningHigh && temp > warningLow) {

            if (probesInAlertList.contains(serialno))
                probesInAlertList.remove(serialno);

            builder.append("<div class='part3 green'>" +
                    "<p>Status: Normal ");


        } else if (temp <= warningLow && temp > alertLow) {

            addTempStatus(serialno);
            builder.append("<div class='part3 orange'>" + "<p>Status: Low ");
        } else if (temp <= alertLow) {

            addTempStatus(serialno);
            builder.append("<div class='part3 red'>" +
                    "<p>Status: Very Low ");
        }
//                    builder.append("<div class='part3 green'><p>Status: ");
//                    builder.append(cursor.getString(5));
        builder.append("</p></div>");
    }

    private void addTempStatus(String serialno) {

        if (!probesInAlertList.contains(serialno)) {
            probesInAlertList.add(serialno);
            soundManager.isTempReset = false;
        }
    }


    /**
     * Display temperature read from probe
     */
    private void displayTemp() {

        builder.append("<div class='part2 '><p>");
        builder.append(String.format("%.2f", temp));

        if (unit.equals("fahrenheit"))
            builder.append("&deg;F</p>");
        else
            builder.append("&deg;C</p>");


        builder.append("</div>");
    }

    /**
     * Display main probe information list ,it is common.
     */
    private void createProbeInfoListItem() {

        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd\\MM\\yyyy hh:mm a");
        String localDateTime = dateTimeFormat.format(new Date());

        builder.append("<li>");
        builder.append("<div class='column'>");
        builder.append("<div class='part1' id='part1'><div id='part1-content'><img src='logo.png'/></div></div>");
        builder.append("<div class='datetime'><p>" + localDateTime + "</p></div> ");
        builder.append("<div class='infopart2'><p>info@smartprobe.com.au</p></div> ");

        //Show Network Status
        if (AppUtil.isConnected(activity)) {


            soundManager.stopNetworkAlert();
            //Set network connection flag
            soundManager.isNetworkReset = false;

            builder.append("<div class='part3 green'><p>Network : Ok</p></div>");
        } else {
            //To check network connection changed , notify by  adding buzzer sound
            soundManager.startNetworkAlert();

            builder.append("<div class='part3 red'><p>Network : Fail</p></div>");

        }

        //Show master probe connection status
        if (activity.isAttached())
            builder.append("<div class='part4 green'><p>Master : Active</p></div>");
        else
            builder.append("<div class='part4 red'><p>Master : Not Active</p></div>");

        builder.append("</div></li>");
    }


    /**
     * Check any new data found for long time and do operations based on.
     *
     */

    public boolean checkAutoLogout() {

        String localDateTime = dateTimeFormat.format(new Date());
        timeDiff = getLastReadTime(SmartProbeDBOpenHelper.TABLE_NAME,localDateTime);

        if(activity.lastAutoResartTime==null || activity.lastAutoResartTime.equals(""))
            activity.lastAutoResartTime=localDateTime;
        long lastRestart = checkTimeDiffernce(localDateTime, activity.lastAutoResartTime);
        Log.e("Smartprobe", "time diff = " + timeDiff + " , lastrestat = " + lastRestart);

        /*
        If master is active and no data found for 15 mins auto logout app
         */
        if (timeDiff >= AUTO_RESTART_TIME && lastRestart >= AUTO_RESTART_TIME && activity.isAttached()){

            LogUtil.writeLogTest(">>>>>>>>>>>>>>> No data found for 15 min <<<<<<<<<<<<<<<<<<<<<");
            //activity.logout(true);
            activity.autoLogout();
            return  true;

        } else if (timeDiff >= AUTO_RESTART_TIME && lastRestart >= AUTO_RESTART_TIME && !activity.isAttached()){

            LogUtil.writeLogTest(">>>>>>>>>>>>>>> No data found for 15 min MASTER INACTIVE<<<<<<<<<<<<<<<<<<<<<");
            //activity.logout(true);
            activity.autoLogout();
            return  true;

        }
        return  false;
    }


    /**
     * Check any new data found for long time and do operations based on.
     *
     * @param localDateTime
     */
    private void isNewDataFound(String localDateTime) {

        timeDiff = getLastReadTime(SmartProbeDBOpenHelper.TABLE_NAME_LATEST,localDateTime);

        if (timeDiff > 45 && timeDiff < 90) {
            builder.append("<div class='orangediv'>");
            builder.append("<p> Could not find any new data for more than 45 minutes. </p>");
            builder.append("</div>");
        }

        if (timeDiff > 90) {
            builder.append("<div class='reddiv'>");
            builder.append("<p> Could not find any new data for more than 90 minutes. </p>");
            builder.append("</div>");
        }

         /* if (timeDiff > 180) {
                db.execSQL("DELETE FROM " + SmartProbeDBOpenHelper.TABLE_NAME_LATEST);
            }*/

    }

    private long getLastReadTime(String tablename,String localDateTime) {

        // Cursor cursorLastRow = db.query(SmartProbeDBOpenHelper.TABLE_NAME_LATEST, null, null, null, null, null, SmartProbeDBOpenHelper.DATE + " DESC," + SmartProbeDBOpenHelper.TIME + " DESC", "1");
        Cursor cursorLastRow = activity.db.query(tablename, null, null, null, null, null, SmartProbeDBOpenHelper.DATE + " DESC," + SmartProbeDBOpenHelper.TIME + " DESC", "1");
        if (cursorLastRow.moveToFirst()) {
            String dbDateTime = cursorLastRow.getString(cursorLastRow.getColumnIndex(SmartProbeDBOpenHelper.DATE)) + " " + cursorLastRow.getString(cursorLastRow.getColumnIndex(SmartProbeDBOpenHelper.TIME));

            timeDiff = checkTimeDiffernce(localDateTime, dbDateTime);
        }
        if (cursorLastRow != null)
            cursorLastRow.close();

        return timeDiff;
    }

    private long checkTimeDiffernce(String localDateTime, String dbDateTime) {
        long timeDiff = 0;
        try {
            if (!dbDateTime.trim().equals("")) {
                Date localDate = dateTimeFormat.parse(localDateTime);
                Date dbDate = dateTimeFormat.parse(dbDateTime);
                timeDiff = Math.abs(localDate.getTime() - dbDate.getTime());
                timeDiff = (timeDiff / (1000 * 60));
            }
        } catch (java.text.ParseException e) {

            e.printStackTrace();
        }
        return timeDiff;
    }


    private float getFloat(String string) {

        try {

            return Float.parseFloat(string);

        } catch (Exception e) {
            Log.e("Smartprobe", "NumberFormatException occured while convert to float");
            e.printStackTrace();
            return 0f;
        }
    }

    private float convertTemp(float value) {

        if (unit.equals("fahrenheit"))
            return AppUtil.c2f(value);
        else
            return value;
    }

    private long getInActiveTime(String date, String time) {

        long timeDiff = 0;
        String dbDateTime = date + " " + time;
        String localDateTime = dateTimeFormat.format(new Date());
        try {
            if (!dbDateTime.trim().equals("")) {
                Date localDate = dateTimeFormat.parse(localDateTime);
                Date dbDate = dateTimeFormat.parse(dbDateTime);
                timeDiff = Math.abs(localDate.getTime() - dbDate.getTime());
                timeDiff = (timeDiff / (1000 * 60));
            }
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }
        return timeDiff;
    }

    private boolean isCurrentDate(String dbDateTime) {

        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd\\MM\\yyyy");
        String localDateTime = dateTimeFormat.format(new Date());
        try {
            if (!dbDateTime.trim().equals("")) {
                Date localDate = dateTimeFormat.parse(localDateTime);
                Date dbDate = dateTimeFormat.parse(dbDateTime);

                if (localDate.compareTo(dbDate) == 0)
                    return true;
                else
                    return false;
            }
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void resetAlarm() {

        soundManager.resetNetworkAlarm();
        soundManager.resetTempAlarm();
    }

    public void pause() {

        soundManager.pauseNetworkAlert();
        soundManager.pauseWakeUpAlert();
    }

    public void resume() {

        soundManager.resumeNetworkAlert();
        soundManager.resumeWakeUpAlert();
    }
}
