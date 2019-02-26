package com.test.smartprobe.apicall;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;
import android.widget.Toast;

import com.bluelinelabs.logansquare.LoganSquare;
import com.test.smartprobe.R;
import com.test.smartprobe.activity.SmartProbeActivity;
import com.test.smartprobe.database.SmartProbeDBOpenHelper;
import com.test.smartprobe.model.JSONDetails;
import com.test.smartprobe.model.Probe;
import com.test.smartprobe.util.AppUtil;
import com.test.smartprobe.util.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.List;

/**
 * Created by abitha on 27/6/16.
 */
public class SyncServer {

    private final SmartProbeActivity activity;
    private String username;
    private String password;
    private String lastUpdateTime;
    private final String RESULT_STATUS = "success";
    public String accessKey = null;
    public String secretKey = null;

    public SyncServer(String username, String password, String lastUpdateTime, SmartProbeActivity activity) {
        this.username = username;
        this.password = password;
        this.lastUpdateTime = lastUpdateTime;
        this.activity = activity;
    }

    /**
     * Contain all operation to send data to server.
     * Call web API to send data as POST method.
     */
    public void sendToServer(int limit) {

        //This is to recheck where gps is enabled or not
        if (activity.trackCurrentLocation)
            activity.trackLocation();

        activity.checkDB();
        LogUtil.writeServerStatus(">>>> Sync to server started <<<<");
      /*  //Test
        if (activity.db != null)
            LogUtil.writeServerStatus("Sync to server > Pending records to send : " + readProbeDataCount());
        //Test*/

        if (activity.db != null && activity.isLocationFound() && AppUtil.isConnected(activity)) {
            Cursor curProbe = readProbeData(limit);

            JSONObject jsonProbeLogData;
            JSONArray probeLogArr = new JSONArray();
            LogUtil.writeServerStatus("Sync to server > Number of records to send : " + curProbe.getCount());

            if (curProbe.getCount() > 0) {
                curProbe.moveToFirst();

                while (!curProbe.isAfterLast()) {


                    jsonProbeLogData = new JSONObject();

                    try {

                        //for final record save into server
                        jsonProbeLogData.put(
                                "serial",
                                curProbe.getString(curProbe
                                        .getColumnIndex(SmartProbeDBOpenHelper.PROBESERIALNUMBER)));
                        jsonProbeLogData.put(
                                "temp_high",
                                curProbe.getString(curProbe
                                        .getColumnIndex(SmartProbeDBOpenHelper.TEMPERATURE_HIGH)));
                        jsonProbeLogData.put(
                                "temp_low",
                                curProbe.getString(curProbe
                                        .getColumnIndex(SmartProbeDBOpenHelper.TEMPERATURE_LOW)));
                        jsonProbeLogData.put(
                                "status_1",
                                curProbe.getString(curProbe
                                        .getColumnIndex(SmartProbeDBOpenHelper.STATUS_1)));
                        jsonProbeLogData.put(
                                "status_2",
                                curProbe.getString(curProbe
                                        .getColumnIndex(SmartProbeDBOpenHelper.STATUS_2)));
                        jsonProbeLogData.put(
                                "time",
                                curProbe.getString(curProbe
                                        .getColumnIndex(SmartProbeDBOpenHelper.TIME)));
                        jsonProbeLogData.put(
                                "date",
                                curProbe.getString(curProbe
                                        .getColumnIndex(SmartProbeDBOpenHelper.DATE)));
                        jsonProbeLogData.put(
                                "trip_1",
                                curProbe.getString(curProbe
                                        .getColumnIndex(SmartProbeDBOpenHelper.TRIP_1)));
                        jsonProbeLogData.put(
                                "trip_2",
                                curProbe.getString(curProbe
                                        .getColumnIndex(SmartProbeDBOpenHelper.TRIP_2)));
                        jsonProbeLogData.put(
                                "voltage",
                                curProbe.getString(curProbe
                                        .getColumnIndex(SmartProbeDBOpenHelper.VOLTAGE)));
                        jsonProbeLogData.put(
                                "latitude",
                                activity.gpsTracker.getLatitude());
                        jsonProbeLogData.put(
                                "longitude",
                                activity.gpsTracker.getLongitude());

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    //jArray.put(probeJSONObject);
                    probeLogArr.put(jsonProbeLogData);
                    curProbe.moveToNext();
                }

                /*        // This section is only for final probe log for inserting records into server..
                HttpClient httpClient = new DefaultHttpClient();
                InputStream inputStream;
                String result = "";
                try {
                    //get accessKey, secretKey from DB
                    Cursor cacc = db.query(SmartProbeDBOpenHelper.TABLE_NAME_ACC, null, null, null, null, null, null);
                    if (cacc.moveToFirst()) {
                        accessKey = cacc.getString(cacc
                                .getColumnIndex(SmartProbeDBOpenHelper.ACCESS_KEY));
                        secretKey = cacc.getString(cacc
                                .getColumnIndex(SmartProbeDBOpenHelper.SECRET_KEY));

                        HttpPost httpPost = new HttpPost(data_url);
                        JSONObject data = new JSONObject();
                        try {
                            data.put("type", "probelog");
                            data.put("accesskey", accessKey);
                            data.put("secretkey", secretKey);
                            data.put("probelog", probeLogArr);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        StringEntity se = new StringEntity(data.toString());
                        httpPost.setEntity(se);
                        HttpResponse httpResponse = httpClient.execute(httpPost);
                        inputStream = httpResponse.getEntity().getContent();*/

                // This section is only for final probe log for inserting records into server..
                try {

                    HttpURLConnection conn = AppUtil.getConnection(getUrl());

                    InputStream inputStream;
                    String result = "";
                    Cursor cursor = activity.db.query(SmartProbeDBOpenHelper.TABLE_NAME_ACC, null, SmartProbeDBOpenHelper.USER_NAME + "='" + username + "'", null, null, null, null);
                    if (cursor.moveToFirst()) {
                        accessKey = cursor.getString(cursor
                                .getColumnIndex(SmartProbeDBOpenHelper.ACCESS_KEY));
                        secretKey = cursor.getString(cursor
                                .getColumnIndex(SmartProbeDBOpenHelper.SECRET_KEY));
                        JSONObject data = new JSONObject();
                        try {
                            data.put("type", "probelog");
                            data.put("accesskey", accessKey);
                            data.put("secretkey", secretKey);
                            data.put("probelog", probeLogArr);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        OutputStream os = conn.getOutputStream();
                        os.write(data.toString().getBytes());
                        os.flush();

                        //LogUtil.writeDeviceLog(data.toString());

                        int responseCode = conn.getResponseCode();

                        if (responseCode == HttpURLConnection.HTTP_OK) { //success
                            inputStream = conn.getInputStream();

                            if (inputStream != null) {
                                result = AppUtil.convertInputStreamToString(inputStream);
                                try {
                                    JSONObject jsonResult = new JSONObject(result);
                                    String resultData = jsonResult.getString("result");
                                    /*
                                    Message is checking because sometimes getting status as 'success' and message as 'no record found'
                                    message is 'succressfully imported' we can clear db, other wise data is getting lost
                                     */
                                    //String message= jsonResult.getString("message");
                                    if (resultData.equals(RESULT_STATUS))//&& message.toLowerCase().contains(RESULT_STATUS))
                                        clearAllRead(curProbe);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    showToast("Failed to send data, found invalid response... ");
                                }

                            } else {
                                result = "Did not work!";
                            }
                        } else {

                            LogUtil.writeServerStatus("Failed to connect to server...");
                            showToast("Failed to connect to server...");
                        }
                    } else {
                        result = "accessKey & secretKey Missing.";
                    }
                    cursor.close();

                    LogUtil.writeServerStatus("Sync to server > Result : " + result);
                    //Toast.makeText(getBaseContext(), "Send Interval:-" + result, Toast.LENGTH_SHORT).show();

                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                    LogUtil.writeServerStatus(">>>>> ERROR OCCURRED " + e.getMessage() + " <<<<<<<<< ");
                    showToast("Failed to connect to server...");


                } catch (Exception e) {

                    e.printStackTrace();
                    LogUtil.writeServerStatus(">>>>> ERROR OCCURRED " + e.getMessage() + " <<<<<<<<< ");
                    showToast("Failed to send data to server...");

                }

            }
            if (curProbe != null)
                curProbe.close();

        } else if (!AppUtil.isConnected(activity))
            LogUtil.writeServerStatus(">>>> No Network <<<<");
        LogUtil.writeServerStatus(">>>> Sync to server ended <<<<");
    }

    private void showToast(final String msg) {

        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * After send data to server delete recorde from local table
     *
     * @param c
     */
    private void clearAllRead(Cursor c) {

        c.moveToFirst();

        while (!c.isAfterLast()) {

            try {

                activity.db.execSQL("DELETE FROM "
                        + SmartProbeDBOpenHelper.TABLE_NAME
                        + " WHERE _id="
                        + c.getString(c
                        .getColumnIndex(SmartProbeDBOpenHelper.ID)));
               /* runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(SmartProbeActivity.this, "Sync to server success", Toast.LENGTH_LONG).show();
                    }
                });*/
                // probeJSONObject.put("user_id",
                // c.getString(c.getColumnIndex(SmartProbeDBOpenHelper.USER_ID)));

            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }

            c.moveToNext();

        }
        LogUtil.writeLogTest("Sync to server : Clear all data after synced");
    }

    public Cursor readProbeData(int limit) {

        return activity.db.rawQuery("select * from " + SmartProbeDBOpenHelper.TABLE_NAME + " limit "+limit, null);
    }

    public int readProbeDataCount() {

        return activity.db.rawQuery("select * from " + SmartProbeDBOpenHelper.TABLE_NAME , null).getCount();
    }
    public void retrieveProbeDetailsFromServer(boolean retrieveAll) {

        LogUtil.writeServerStatus("**** Sync from server started ****");
        if (AppUtil.isConnected(activity)) {
            try {


                HttpURLConnection conn = AppUtil.getConnection(activity.getResources().getString(R.string.api_URL_retrieve));

                JSONObject data = new JSONObject();
                data.put("type", "probelist");
                data.put("user", username);
                data.put("pass", password);
                if (retrieveAll)
                    data.put("updatetime", "");
                else
                    data.put("updatetime", lastUpdateTime);

                Log.e("Get ProbeList", "Get problist request : " + data.toString());
                OutputStream os = conn.getOutputStream();
                os.write(data.toString().getBytes());
                os.flush();


                int responseCode = conn.getResponseCode();
                Log.i(LogUtil.TAG, "POST Response Code :: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) { //success
                    InputStream inputStream = conn.getInputStream();

                    if (inputStream != null) {
//                    SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                    String lastUpdate = dateTimeFormat.format(new Date());
//                    String result = AppUtil.convertInputStreamToString(inputStream);
//                    JSONDetails jsonDetails = LoganSquare.parse(result, JSONDetails.class);

                        JSONDetails jsonDetails = LoganSquare.parse(inputStream, JSONDetails.class);

                        processProbeList(retrieveAll,jsonDetails);

                        jsonDetails = null;
                    }
                }

            } catch (SocketTimeoutException e) {
                e.printStackTrace();

                LogUtil.writeServerStatus("**** ERROR OCCURRED " + e.getMessage() + " **** ");
                showToast("Failed to connect to server...");

            } catch (Exception e) {
                e.printStackTrace();
                LogUtil.writeServerStatus("**** ERROR OCCURRED " + e.getMessage() + " **** ");
            }
        } else
            LogUtil.writeServerStatus("**** No Network ****");
        LogUtil.writeServerStatus("**** Sync from server ended ****");

    }

    private void updateLastUpdateTime(String lastUpdateTime) {

        ContentValues values = new ContentValues();
        values.put(SmartProbeDBOpenHelper.LAST_SYNCED, lastUpdateTime);
        activity.db.update(SmartProbeDBOpenHelper.TABLE_NAME_LOG, values, SmartProbeDBOpenHelper.USER_NAME + "='" + username + "'", null);
        this.lastUpdateTime = lastUpdateTime;
        values.clear();
    }

    private void processProbeList(boolean retrieveAll, JSONDetails jsonDetails) {

        try {

            if (jsonDetails != null) {

                activity.checkDB();
                boolean isErrorOccured = false;

                if (jsonDetails.result != null && jsonDetails.result.equals(RESULT_STATUS) && activity.db != null) {
                    List<Probe> probeList = jsonDetails.probes;
                    LogUtil.writeServerStatus("Sync from server > Result status : " + jsonDetails.result);
                    if (probeList != null && probeList.size() > 0) {
                        try {
                            LogUtil.writeServerStatus("Sync from server > Result probes : " + probeList.size());
                            activity.db.beginTransaction();
                            String probeSerialList = "";
                            for (Probe probeObj : probeList) {

                                String probe_serial = probeObj.serialNumber;
                                probeSerialList += (probeSerialList.equals("") ? ("'" + probe_serial + "'") : ("," + "'" + probe_serial + "'"));

                                //Check this probe is exist or not in latest table
                                int probeSerialCount;
                                Cursor mCount = activity.db.rawQuery("select count(*) from "
                                        + SmartProbeDBOpenHelper.TABLE_NAME_LATEST + " where "
                                        + SmartProbeDBOpenHelper.PROBESERIALNUMBER + " = '"
                                        + probe_serial + "'  AND " + SmartProbeDBOpenHelper.USER_NAME + "='" + activity.username + "'", null);
                                if (mCount.moveToFirst()) {
                                    probeSerialCount = mCount.getInt(0);
                                } else {
                                    probeSerialCount = 0;
                                }
                                mCount.close();
                                if (probeSerialCount <= 0) {     //if not exist then insert a new row for this probe

                                    activity.stmtInsertSmartprobeLatest.bindString(1, probe_serial);
                                    if (probeObj.probe_name == null || probeObj.probe_name.equals(null) || probeObj.probe_name.equals("null"))
                                        activity.stmtInsertSmartprobeLatest.bindNull(2);
                                    else
                                        activity.stmtInsertSmartprobeLatest.bindString(2, probeObj.probe_name);
                                    activity.stmtInsertSmartprobeLatest.bindString(3, username);
                                    activity.stmtInsertSmartprobeLatest.bindString(4, probeObj.alertHighLimit);
                                    activity.stmtInsertSmartprobeLatest.bindString(5, probeObj.alertLowLimit);
                                    activity.stmtInsertSmartprobeLatest.bindString(6, probeObj.warningHighLimit);
                                    activity.stmtInsertSmartprobeLatest.bindString(7, probeObj.warningLowLimit);
                                    activity.stmtInsertSmartprobeLatest.bindString(8, probeObj.samplePeriodUnits);
                                    activity.stmtInsertSmartprobeLatest.bindString(9, probeObj.voltagemin);
                                    activity.stmtInsertSmartprobeLatest.bindString(10, probeObj.defaultsensor);

                                    activity.stmtInsertSmartprobeLatest.executeInsert();
                                    activity.stmtInsertSmartprobeLatest.clearBindings();

                                } else {
                                    if (probeObj.probe_name == null || probeObj.probe_name.equals(null) || probeObj.probe_name.equals("null"))
                                        activity.stmtInsertSmartprobeLatest.bindNull(1);
                                    else
                                        activity.stmtUpdateSmartprobeServer.bindString(1, probeObj.probe_name);
                                    activity.stmtUpdateSmartprobeServer.bindString(2, probeObj.alertHighLimit);
                                    activity.stmtUpdateSmartprobeServer.bindString(3, probeObj.alertLowLimit);
                                    activity.stmtUpdateSmartprobeServer.bindString(4, probeObj.warningHighLimit);
                                    activity.stmtUpdateSmartprobeServer.bindString(5, probeObj.warningLowLimit);
                                    activity.stmtUpdateSmartprobeServer.bindString(6, probeObj.samplePeriodUnits);
                                    activity.stmtUpdateSmartprobeServer.bindString(7, probeObj.voltagemin);
                                    activity.stmtUpdateSmartprobeServer.bindString(8, probeObj.defaultsensor);

                                    activity.stmtUpdateSmartprobeServer.bindString(9, probe_serial);
                                    activity.stmtUpdateSmartprobeServer.executeUpdateDelete();
                                    activity.stmtUpdateSmartprobeServer.clearBindings();

                                }
                            }
                            if (lastUpdateTime == null || lastUpdateTime.equals("null") || lastUpdateTime.equals("") || retrieveAll) {
                                String sql = "DELETE from " + SmartProbeDBOpenHelper.TABLE_NAME_LATEST + " WHERE " + SmartProbeDBOpenHelper.PROBESERIALNUMBER + " NOT IN (" + probeSerialList + ")  AND " + SmartProbeDBOpenHelper.USER_NAME + "='" + activity.username + "'";
                                Log.e("Smartprobe", sql);
                                activity.db.execSQL(sql);
                            }
                            // "DELETE from "+SmartProbeDBOpenHelper.TABLE_NAME_LATEST+" WHERE "+SmartProbeDBOpenHelper.PROBESERIALNUMBER +" NOT IN (1,2,3,...,254)  AND "+SmartProbeDBOpenHelper.USER_NAME + "='"+activity.username+"'";

                        } catch (Exception e) {
                            e.printStackTrace();
                            isErrorOccured = true;
                        } finally {
                            activity.db.setTransactionSuccessful();
                            activity.db.endTransaction();
                        }
                    }
//                    insertForTesting("SP-MDK_1001");
//                    insertForTesting("SP-MDK_1002");
//                    insertForTesting("SP-MDK_1003");


                    //To refresh data fast when initial api call....
                    if (lastUpdateTime.equals("") && !isErrorOccured)
                        activity.updateView();

                    //Update last update time to sync with server later.
                    if (!isErrorOccured && jsonDetails.curdate != null)
                        updateLastUpdateTime(jsonDetails.curdate);
                }
            }

        } catch (Exception e) {
            Log.d("Response Data Status:", e.getLocalizedMessage());
        }
    }

    private void insertForTesting(String probe_serial) {


        //Check this probe is exist or not in latest table
        int probeSerialCount;
        Cursor mCount = activity.db.rawQuery("select count(*) from "
                + SmartProbeDBOpenHelper.TABLE_NAME_LATEST + " where "
                + SmartProbeDBOpenHelper.PROBESERIALNUMBER + " = '"
                + probe_serial + "'", null);
        if (mCount.moveToFirst()) {
            probeSerialCount = mCount.getInt(0);
        } else {
            probeSerialCount = 0;
        }
        mCount.close();
        if (probeSerialCount <= 0) {     //if not exist then insert a new row for this probe

            activity.stmtInsertSmartprobeLatest.bindString(1, probe_serial);

            activity.stmtInsertSmartprobeLatest.bindString(2, probe_serial);
            activity.stmtInsertSmartprobeLatest.bindString(3, "100");
            activity.stmtInsertSmartprobeLatest.bindString(4, "90");
            activity.stmtInsertSmartprobeLatest.bindString(5, "80");
            activity.stmtInsertSmartprobeLatest.bindString(6, "70");
            activity.stmtInsertSmartprobeLatest.bindString(7, "fahrenheit");
            activity.stmtInsertSmartprobeLatest.bindString(8, "10");

            activity.stmtInsertSmartprobeLatest.executeInsert();
            activity.stmtInsertSmartprobeLatest.clearBindings();


        } else {

            /*    activity.stmtUpdateSmartprobeServer.bindString(1, "SP-MDK_1001");
            activity.stmtUpdateSmartprobeServer.bindString(2, "100");
            activity.stmtUpdateSmartprobeServer.bindString(3, "60");
            activity.stmtUpdateSmartprobeServer.bindString(4,"80");
            activity.stmtUpdateSmartprobeServer.bindString(5, "70");
            activity.stmtUpdateSmartprobeServer.bindString(6, "fahrenheit");
            activity.stmtUpdateSmartprobeServer.bindString(7,"10");*/

            activity.stmtUpdateSmartprobeServer.bindString(1, probe_serial);
            activity.stmtUpdateSmartprobeServer.bindString(2, "37");
            activity.stmtUpdateSmartprobeServer.bindString(3, "15");
            activity.stmtUpdateSmartprobeServer.bindString(4, "26");
            activity.stmtUpdateSmartprobeServer.bindString(5, "21");
            activity.stmtUpdateSmartprobeServer.bindString(6, "celsius");
            activity.stmtUpdateSmartprobeServer.bindString(7, "10");

            activity.stmtUpdateSmartprobeServer.bindString(8, probe_serial);
            activity.stmtUpdateSmartprobeServer.executeUpdateDelete();
            activity.stmtUpdateSmartprobeServer.clearBindings();

        }

    }

    private String getUrl() {

        return activity.getResources().getString(R.string.api_URL);
    }
}
