package com.test.smartprobe.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.test.smartprobe.R;
import com.test.smartprobe.util.AppUtil;

/**
 * Created by abitha on 8/11/16.
 */
public class LogDetailsActivity extends AppCompatActivity {
    private static final String TAG = "SamrtProbe";
    private WebView webview;
    private ProgressDialog progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtil.setupActivity(this);
        setContentView(R.layout.smart_probe_activity_main);
        String serialno= (String) getIntent().getExtras().getCharSequence("serialno");

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        webview = (WebView) findViewById(R.id.webview);
        WebSettings settings = webview.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webview.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        settings.setDomStorageEnabled(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        settings.setUseWideViewPort(true);
        settings.setSaveFormData(true);
        if (Build.VERSION.SDK_INT >= 19) {
            // chromium, enable hardware acceleration
            webview.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            // older android version, disable hardware acceleration
            webview.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        webview.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webview.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);

        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        progressBar = ProgressDialog.show(LogDetailsActivity.this, "Loading Log Details", "Please wait...");


        Log.i(TAG, "loading URL: " +"http://smartprobe.com.au/log.php?probe_name="+serialno);
        webview.loadUrl("http://smartprobe.com.au/log.php?probe_name="+serialno);
        webview.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.i(TAG, "Processing webview url click...");
                view.loadUrl(url);
                return true;
            }

            public void onPageFinished(WebView view, String url) {
                Log.i(TAG, "Finished loading URL: " +url);
                if (progressBar.isShowing()) {
                    progressBar.dismiss();
                }
            }

        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            Intent mainIntent = new Intent(LogDetailsActivity.this, SmartProbeActivity.class);
            this.startActivity(mainIntent);
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed() {

        Intent mainIntent = new Intent(LogDetailsActivity.this, SmartProbeActivity.class);
        this.startActivity(mainIntent);
        this.finish();
    }
    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub

        webview.clearCache(true);
        webview.clearHistory();
        webview=null;

        super.onDestroy();
    }
}
