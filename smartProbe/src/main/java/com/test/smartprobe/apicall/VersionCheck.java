package com.test.smartprobe.apicall;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

import com.test.smartprobe.BuildConfig;
import com.test.smartprobe.R;
import com.test.smartprobe.activity.SmartProbeActivity;
import com.test.smartprobe.util.AppUtil;
import com.test.smartprobe.util.LogUtil;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

/**
 * Created by abitha on 15/6/17.
 */

public class VersionCheck extends AsyncTask<String, Void, String> {

    private final SmartProbeActivity activity;
    private final boolean showProgress;
    private ProgressDialog progressDialog;

    public VersionCheck(SmartProbeActivity activity, boolean showProgress) {
        this.activity = activity;
        this.showProgress=showProgress;
    }
    @Override
    protected void onPreExecute() {

        if(showProgress) {
            progressDialog = new ProgressDialog(activity);
            progressDialog.setMessage("Checking for update....");
            progressDialog.setCancelable(true);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }
    }

    @Override
    protected String doInBackground(String... urls) {
        try {
            return POSTData(getUrl());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String POSTData(String urlStr) {
        InputStream inputStream = null;
        String result = "";
        try {

            HttpURLConnection conn = AppUtil.getConnection(urlStr);

            JSONObject data = new JSONObject();
            data.put("type", "checkversion");
            data.put("package", activity.getPackageName());
            data.put("currentversion", BuildConfig.VERSION_NAME);

            OutputStream os = conn.getOutputStream();
            os.write(data.toString().getBytes());
            os.flush();


            int responseCode = conn.getResponseCode();
            Log.i(LogUtil.TAG, "POST Response Code :: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) { //success
                inputStream = conn.getInputStream();

                if (inputStream != null) {
                    result = AppUtil.convertInputStreamToString(inputStream);
                } else {
                    result = "Did not work!";
                }
            }
        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }
        return result;
    }

    protected void onPostExecute(String responseData) {
        try {
            if (progressDialog != null)
                progressDialog.dismiss();

            if (responseData != null) {
                JSONObject jsonObject = new JSONObject(responseData);
                if (!jsonObject.getBoolean("isLatest")) {
                    activity.showVersionUpdate(jsonObject.getString("appVersion"),jsonObject.getString("url"));
                }
                else if(showProgress)
                    activity.noUpdateDialog();
            }

        } catch (Exception e) {
            Log.d("Response Data Status:", e.getLocalizedMessage());
        }
    }
    private String getUrl() {

        return activity.getResources().getString(R.string.api_URL_versioncheck);
    }
}
