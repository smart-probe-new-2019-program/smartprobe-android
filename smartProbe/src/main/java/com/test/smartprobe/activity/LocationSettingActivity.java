package com.test.smartprobe.activity;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.test.smartprobe.R;
import com.test.smartprobe.database.SmartProbeDBOpenHelper;
import com.test.smartprobe.util.AppUtil;
import com.test.smartprobe.util.LogUtil;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

public class LocationSettingActivity extends AppCompatActivity {

    Button location_submit;
    EditText location_id;
    EditText location_pass;
    public static String location_id_val;
    public static String location_pass_val;

    private SQLiteDatabase mDB = null;
    private SmartProbeDBOpenHelper mDbHelper;
    private String username = "";
    private boolean isKeyboardVisible;
//    private ScrollView scrollView;
    public boolean isPassFocused;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtil.setupActivity(this);

        setContentView(R.layout.location_setting);

        // Creating database for SmartProbe
        mDbHelper = new SmartProbeDBOpenHelper(this);
        mDB = mDbHelper.getWritableDatabase();
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getUserName();

        Cursor c = mDB.query(SmartProbeDBOpenHelper.TABLE_NAME_ACC, null, SmartProbeDBOpenHelper.USER_NAME + "='" + username + "'", null, null, null, null);
        if (c.moveToFirst()) {
            location_id = (EditText) findViewById(R.id.location_id);
            location_pass = (EditText) findViewById(R.id.location_pass);
            location_id.setText(c.getString(2));
            location_pass.setText(c.getString(3));
        }
        c.close();

//        scrollView = (ScrollView) findViewById(R.id.scroll_layout);

        location_pass = (EditText) findViewById(R.id.location_pass);

//        location_pass.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//
//
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//
//                if (hasFocus && isKeyboardVisible) {
//                    isPassFocused = true;
//                    scrollToFocusedView();
//                } else
//                    isPassFocused = false;
//            }
//        });
        location_submit = (Button) findViewById(R.id.location_submit);
        location_submit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                location_id = (EditText) findViewById(R.id.location_id);

                location_id_val = location_id.getText().toString();
                location_pass_val = location_pass.getText().toString();
                if ((location_id_val.equals("")) || (location_pass_val.equals(""))) {
                    if (location_id_val.equals("")) {
                        location_id.setError("Provide a valid location id");
                    }
                    if (location_pass_val.equals("")) {
                        location_pass.setError("Provide a valid location password");
                    }
                } else {
                    hideKeyboard();
                    if (AppUtil.isConnected(LocationSettingActivity.this)) {
                        new HttpAsyncTaskProbeList().execute(getResources().getString(R.string.api_URL));
                    } else {
                        Toast.makeText(getBaseContext(), "Please check your internet connection.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        setForKeyboard();
    }

    private void scrollToFocusedView() {

//        scrollView.post(new Runnable() {
//            public void run() {
//                scrollView.scrollTo(0, getWindow().getCurrentFocus().getTop());
//            }
//        });
    }

    private void setForKeyboard() {

        final RelativeLayout loginlayout = (RelativeLayout) findViewById(R.id.main_layout);


        loginlayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {

                        Rect r = new Rect();
                        loginlayout.getWindowVisibleDisplayFrame(r);

                        int screenHeight = loginlayout.getRootView()
                                .getHeight();
                        int heightDifference = screenHeight
                                - (r.bottom - r.top);

                        boolean visible = false;
                        if (heightDifference > (screenHeight / 3)) {
                            // keyboard is visible
                            visible = true;
                        }

                        if (visible) {
                            isKeyboardVisible = true;
                            // move the layout up
                            //loginlayout.setY(190);
                            loginlayout.setPadding(0, 0, 0, 500);
                            if (isPassFocused)
                                scrollToFocusedView();

                            //  loginlayout.setY(getResources().getDimension(R.dimen.login_scroll_up));
                        } else {
                            isKeyboardVisible = false;
                            // reset view to its original position
                            loginlayout.setPadding(0, 0, 0, 0);

//                            scrollView.post(new Runnable() {
//                                public void run() {
//                                    scrollView.scrollTo(0, 0);
//                                }
//                            });
                        }

                    }
                });
    }

    private void hideKeyboard() {

        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void getUserName() {

        Cursor c = mDB.query(SmartProbeDBOpenHelper.TABLE_NAME_LOG, null, null, null, null, null, null);
        if (c.moveToFirst()) {
            username = c.getString(1);
        }
        c.close();
    }

    private class HttpAsyncTaskProbeList extends AsyncTask<String, Void, Boolean> {

        private ProgressDialog progressDialog;

        @Override
        protected Boolean doInBackground(String... urls) {
            boolean isSuccess = false;
            try {

                String responseData = POSTData(urls[0]);

                JSONObject jsonObject = new JSONObject(responseData);
                if (jsonObject.getString("result").equals("success")) {
                    isSuccess = true;
                    LogUtil.writeServerStatus("**** Added Location information successfully ****");
                    LogUtil.writeServerStatus("**** ACCESS_KEY : " +location_id_val+" ****");
                    ContentValues values = new ContentValues();
                    values.put(SmartProbeDBOpenHelper.USER_NAME, username);
                    values.put(SmartProbeDBOpenHelper.ACCESS_KEY, location_id_val);
                    values.put(SmartProbeDBOpenHelper.SECRET_KEY, location_pass_val);

                    if (checkUserKey(username)) {
                        mDB.update(SmartProbeDBOpenHelper.TABLE_NAME_ACC, values,
                                SmartProbeDBOpenHelper.USER_NAME + " = '"
                                        + username + "'", null);
                    } else {
                        mDB.insert(SmartProbeDBOpenHelper.TABLE_NAME_ACC, null, values);
                    }

                    values.clear();


                    mDB.close();

                }
            } catch (Exception e) {
                isSuccess = false;
                e.printStackTrace();
            }
            return isSuccess;
        }

        @Override
        protected void onPreExecute() {

            progressDialog = new ProgressDialog(LocationSettingActivity.this);
            progressDialog.setMessage("Please wait..");
            progressDialog.setCancelable(true);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }

        protected void onPostExecute(Boolean responseData) {
            try {
                if (progressDialog != null)
                    progressDialog.dismiss();

                if (responseData) {
                    Toast.makeText(getBaseContext(), "Location verification success", Toast.LENGTH_LONG).show();
                    /*Intent mainIntent = new Intent(LocationSettingActivity.this, SmartProbeActivity.class);
                    LocationSettingActivity.this.startActivity(mainIntent);
                    LocationSettingActivity.this.finish();*/
                    onBackPressed();
                } else {
                    TextView textViewLocationStatus = (TextView) findViewById(R.id.location_status);
                    textViewLocationStatus.setTextColor(Color.RED);
                    textViewLocationStatus.setText("Location info does not match, try again");
                }
            } catch (Exception e) {
                Log.d("Response Data Status:", e.getLocalizedMessage());
            }
        }
    }

    private boolean checkUserKey(String username) {
        int userCount = 0;
        Cursor mCount = mDB.rawQuery("select count(*) from "
                + SmartProbeDBOpenHelper.TABLE_NAME_ACC + " where "
                + SmartProbeDBOpenHelper.USER_NAME + " = '"
                + username + "'", null);
        if (mCount.moveToFirst()) {
            userCount = mCount.getInt(0);
        } else {
            userCount = 0;
        }
        mCount.close();
        if (userCount <= 0) {
            return false;
        } else {
            return true;
        }

    }

    public String POSTData(String urlStr) {
        InputStream inputStream = null;
        String result = "";
        try {
      /*      HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
            JSONObject data = new JSONObject();
            data.put("type", "location");
            data.put("accesskey", location_id_val);
            data.put("secretkey", location_pass_val);
            StringEntity se = new StringEntity(data.toString());
            httpPost.setEntity(se);
            HttpResponse httpResponse = httpClient.execute(httpPost);
            inputStream = httpResponse.getEntity().getContent();*/


            HttpURLConnection conn = AppUtil.getConnection(urlStr);


            JSONObject data = new JSONObject();
            data.put("type", "location");
            data.put("accesskey", location_id_val);
            data.put("secretkey", location_pass_val);

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




    @Override
    public void onBackPressed() {
        super.onBackPressed();

       /* Intent mainIntent = new Intent(LocationSettingActivity.this, SmartProbeActivity.class);
        LocationSettingActivity.this.startActivity(mainIntent);
        LocationSettingActivity.this.finish();*/
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
