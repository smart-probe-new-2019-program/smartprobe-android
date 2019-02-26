package com.test.smartprobe.util;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.test.smartprobe.BuildConfig;
import com.test.smartprobe.R;
import com.test.smartprobe.custom.MyExceptionHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by abitha on 22/4/16.
 */
public class AppUtil {

    public static void setupActivity(AppCompatActivity activity)
    {
        Thread.setDefaultUncaughtExceptionHandler(new MyExceptionHandler(activity));

//        activity.setTitle(activity.getResources().getString(R.string.app_name)+" Ver "+ BuildConfig.VERSION_NAME+activity.getResources().getString(R.string.app_name_subtitle));
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ActionBar mActionBar = activity.getSupportActionBar();
        if(mActionBar!=null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
//            mActionBar.setHomeAsUpIndicator(R.drawable.ic_launcher);
        }
    }

    /**
     * To get response , we need to convert  inputstream to string while api call
     *
     * @param inputStream
     * @return response string
     * @throws IOException
     */
    public static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while ((line = bufferedReader.readLine()) != null)
            result += line;
        inputStream.close();
        return result;
    }

    /**
     * Celcius to Fahrenhiet convertion method
     *
     * @param c Celcius value
     * @return Fahrenhiet
     */
    public static float c2f(float c)
    {
        return (c*9)/5+32;
    }


    /**
     * Fahrenhiet to Celcius convertion method
     *
     * @param f  Fahrenhiet value
     * @return Celcius
     */
    public static float f2c(float f)
    {
        return (f-32)*5/9;
    }


    /**
     * To check network is available or not.
     *
     * @return network status
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null)
            return networkInfo.isConnected();
        else
            return false;
    }

    public static HttpURLConnection getConnection(String url) throws Exception {

        int timeout = 1000 * 90;
        URL dataurl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) dataurl.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setRequestProperty("Content-Type", "application/json");

        return  conn;
    }
}
