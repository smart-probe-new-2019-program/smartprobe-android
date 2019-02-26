package com.test.smartprobe.activity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.test.smartprobe.R;
import com.test.smartprobe.database.SmartProbeDBOpenHelper;
import com.test.smartprobe.util.AppUtil;


/**
 * Created by BananiCorp on 4/16/2015.
 * Modified by calpine on 14/8/2017
 */
public class LoginActivity extends AppCompatActivity {
    JavaScriptWebInterface JSInterface;
    WebView wv;

    private SQLiteDatabase mDB = null;
    private SmartProbeDBOpenHelper mDbHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtil.setupActivity(this);
        setContentView(R.layout.login);
        loginAction(getIntent().getExtras());
        if (!isConnected()) {
            Toast.makeText(getBaseContext(), "Please check your internet connection.", Toast.LENGTH_LONG).show();
        }
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

       /* EditText editUsername = (EditText)findViewById(R.id.usename);
        EditText editPassword = (EditText)findViewById(R.id.passwd);
        TextView errorText=(TextView)findViewById(R.id.error);

        String username=editUsername.getText().toString();
        String password=editPassword.getText().toString();


        if (username == "" && password == "") {
            errorText.setVisibility(View.VISIBLE);
            errorText.setText("Please submit valid username & password");
        } else if (username == "") {
            errorText.setVisibility(View.VISIBLE);
            errorText.setText("Please submit valid username");
        } else if (password == "") {
            errorText.setVisibility(View.VISIBLE);
            errorText.setText("Please submit valid password");
        } else {
            errorText.setVisibility(View.INVISIBLE);
            errorText.setText("Please submit valid username & password");
            password = btoa(CryptoJS.SHA512(password));
            var dataUrl = "http://smartprobe.com.au/cav/apprequest.php/";
            var dataStringLogin = {
                    type: 'authenticate',
                    user: username,
                    pass: password
            };
            //var dataStringProbeList = {type: 'probelist', user: username, pass: password};
            $.ajax({
                    type: "POST",
                    url: dataUrl,
                    data: JSON.stringify(dataStringLogin),
                    dataType: 'json',
                    async: false,
                    success: function(responseData) {
                if (responseData.result == "success") {
                    JSInterface.changeActivity(username, password);
                            *//*  $.ajax({
                                  type: "POST",
                                  url: dataUrl,
                                  data: JSON.stringify(dataStringProbeList),
                                  dataType: 'json',
                                  async: false,
                                  success: function(responseDataProbeList) {
                                      if (responseDataProbeList.result == "success") {
                                          JSInterface.changeActivity();
                                      } else {
                                          document.getElementById('result').style.display = 'block';
                                          document.getElementById('result').innerHTML = 'Something went wrong, try again!';
                                      }
                                  }
                              });  *//*
                } else if (responseData.result == "fail") {
                    document.getElementById('result').style.display = 'block';
                    document.getElementById('result').innerHTML = 'Your username or password incorrect, try again!';
                } else {
                    document.getElementById('result').style.display = 'block';
                    document.getElementById('result').innerHTML = 'Something went wrong, try again!';
                }
            }
            });
        }*/
    }


    public class JavaScriptWebInterface {

        Context mContext;

        /// Instantiate the interface and set the context
        JavaScriptWebInterface(Context c) {
            mContext = c;
        }

        //@JavascriptInterface
        public void changeActivity(String username, String password) {

            login(username,password);
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

        // Checks the orientation of the screen
//        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
//        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
//            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
//        }
    }

}
