package com.test.smartprobe.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bluelinelabs.logansquare.LoganSquare;
import com.test.smartprobe.R;
import com.test.smartprobe.database.SmartProbeDBOpenHelper;
import com.test.smartprobe.model.JSONDetails;
import com.test.smartprobe.util.AppUtil;
import com.test.smartprobe.util.LogUtil;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;


/**
 * Created by BananiCorp on 4/16/2015.
 * Modified by calpine on 14/8/2017
 */
public class LoginActivity extends AppCompatActivity {
    JavaScriptWebInterface JSInterface;
    WebView wv;

    private SQLiteDatabase mDB = null;
    private SmartProbeDBOpenHelper mDbHelper;
    private String username, password;
    private EditText etUsername, etPassword;
    private Button btnLogin;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        loginAction(getIntent().getExtras());

        if (!isConnected()) {
            Toast.makeText(getBaseContext(), "Please check your internet connection.", Toast.LENGTH_LONG).show();
        }
//
//        etUsername = (EditText)findViewById(R.id.et_login_username);
//        etPassword = (EditText)findViewById(R.id.et_login_password);
//        btnLogin = (Button)findViewById(R.id.btn_login);
//
//        btnLogin.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (isConnected()) {
//                    username = etUsername.getText().toString().trim();
//                    password = etPassword.getText().toString().trim();
//                    new LoginToServer().execute();
//                } else {
//                    Toast.makeText(getBaseContext(), "Please check your internet connection.", Toast.LENGTH_LONG).show();
//                }
//            }
//        });
//
//
//

        wv = (WebView) findViewById(R.id.loginform);
        wv.getSettings().setJavaScriptEnabled(true);
        //register class containing methods to be exposed to JavaScript
        JSInterface = new JavaScriptWebInterface(this);
        wv.addJavascriptInterface(JSInterface, "JSInterface");
        wv.loadUrl("file:///android_asset/login.html");

    }

    private void loginAction(Bundle extras) {
        if(extras!=null)
        {
            if(extras.containsKey("Username") && extras.containsKey("Password"))
            {
                login(extras.getString("Username"),extras.getString("Password"));
            }
        }
    }


    public class JavaScriptWebInterface {

        Context mContext;
        private ProgressDialog progressDialog;
        /// Instantiate the interface and set the context
        JavaScriptWebInterface(Context c) {
            mContext = c;
        }

        //@JavascriptInterface
        public void changeActivity(String username, String password) {
            login(username,password);
        }

        public void loadProgressDialog(){
            progressDialog = new ProgressDialog(mContext);
            progressDialog.setMessage("Authenticating");
            progressDialog.setCancelable(true);
            progressDialog.show();

            try {
                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }

        public void dismissProgressDialog(){
            progressDialog.dismiss();
        }
    }

    private void login(String username, String password) {

        if (username.equals(null) || password.equals(null)) {} else {
            // Creating database for SmartProbe
            mDbHelper = new SmartProbeDBOpenHelper(LoginActivity.this);
            mDB = mDbHelper.getWritableDatabase();

            mDB.delete(SmartProbeDBOpenHelper.TABLE_NAME_LOG, null, null);
            ContentValues values = new ContentValues();
            values.put(SmartProbeDBOpenHelper.USER_NAME, username);
            values.put(SmartProbeDBOpenHelper.PASS_WORD, password);
            values.put(SmartProbeDBOpenHelper.LAST_SYNCED, "");
            mDB.insert(SmartProbeDBOpenHelper.TABLE_NAME_LOG, null, values);
            values.clear();
            mDB.close();

            Intent i = new Intent(LoginActivity.this, SmartProbeActivity.class);
            LoginActivity.this.startActivity(i);
            LoginActivity.this.finish();
        }
    }

    public boolean isConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }

    private class LoginToServer extends AsyncTask<String, Void, Void>{

        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(LoginActivity.this);
            progressDialog.setMessage("Authenticating");
            progressDialog.setCancelable(true);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(String... strings) {

            try {
                HttpURLConnection conn = AppUtil.getConnection(getResources().getString(R.string.api_URL));

                JSONObject data = new JSONObject();
                data.put("type", "authenticate");
                data.put("user", username);
                data.put("pass", "6bd2f4e4706c32ed24a35708d7d3981650814cd0eb6ce4e9ebd759b03aa5d9439d84cfd29f1cccb24eaf586f199baebc7e102e4d57358d93232c5f3f65442b19");

                OutputStream os = conn.getOutputStream();
                os.write(data.toString().getBytes());
                os.flush();

                int responseCode = conn.getResponseCode();
                Log.i(LogUtil.TAG, "POST Response Code :: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) { //success
                    InputStream inputStream = conn.getInputStream();
                    if (inputStream != null) {
                        String res = convertInputStream(inputStream);
                        Log.i(LogUtil.TAG, "POST Response :: " + res);

                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Failed to login to server...", Toast.LENGTH_SHORT).show();
                }

            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                LogUtil.writeServerStatus("**** ERROR OCCURRED " + e.getMessage() + " **** ");
                Toast.makeText(LoginActivity.this, "Failed to connect to server...", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                e.printStackTrace();
                LogUtil.writeServerStatus("**** ERROR OCCURRED " + e.getMessage() + " **** ");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
        }

        private String convertInputStream(InputStream inputStream) throws UnsupportedEncodingException {
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            int result = 0;
            try {
                result = bis.read();
                while(result != -1) {
                    buf.write((byte) result);
                    result = bis.read();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return buf.toString("UTF-8");
        }
    }

}
