package com.test.smartprobe.apicall;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.test.smartprobe.R;
import com.test.smartprobe.activity.SmartProbeActivity;
import com.test.smartprobe.util.AppUtil;
import com.test.smartprobe.util.LogUtil;

import java.io.File;

/**
 * Created by abitha on 15/6/17.
 */

public class UpdateApp extends AsyncTask<String, String, String> {
    private SmartProbeActivity context;
    private ProgressDialog pDialog;
    private String path;
    private boolean isDownloadPaused;

    public UpdateApp(SmartProbeActivity contextf) {
        context = contextf;
    }

    private boolean downloading = true;

    /**
     * Before starting background thread Show Progress Bar Dialog
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        showProgress();
    }

    private void showProgress() {
        pDialog = new ProgressDialog(context);
        pDialog.setTitle("Downloading file");
        pDialog.setIcon(R.drawable.ic_launcher);
        pDialog.setMessage("Please wait...");
        pDialog.setIndeterminate(false);
        pDialog.setMax(100);
        pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pDialog.setCancelable(false);
        pDialog.show();
    }

    /**
     * Downloading file in background thread
     */
    @Override
    protected String doInBackground(String... f_url) {
        try {
            if (AppUtil.isConnected(context)) {

                String fileName = f_url[0].substring(f_url[0].lastIndexOf('/') + 1);
                //set the path where we want to save the file
                File fileLocation = LogUtil.getFileLocation();// Environment.getExternalStorageDirectory();
                //create a new file, to save the downloaded file
                final File file = new File(fileLocation, Environment.DIRECTORY_DOWNLOADS + "/" + fileName);
                if (file.exists())
                    file.delete();

                path = file.getAbsolutePath();

                Uri uri = Uri.parse(f_url[0]);
                DownloadManager.Request request = new DownloadManager.Request(uri);
                request.setDestinationUri(Uri.fromFile(file));
                //  request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                final DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                //  SharedPreferences mSharedPref = context.getSharedPreferences("package", Context.MODE_PRIVATE);
                //mSharedPref.edit().putLong("downloadID", manager.enqueue(request)).commit();


                final long downloadId = manager.enqueue(request);

                new Thread(new Runnable() {

                    @Override
                    public void run() {


                        while (downloading) {

                            DownloadManager.Query q = new DownloadManager.Query();
                            q.setFilterById(downloadId);

                            Cursor cursor = manager.query(q);
                            cursor.moveToFirst();
                            int bytes_downloaded = cursor.getInt(cursor
                                    .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                            checkDownloadStatus(cursor);

                            //final double dl_progress = (bytes_downloaded / bytes_total) * 100;
                            final int dl_progress = (int) ((bytes_downloaded * 100l) / bytes_total);

                            context.runOnUiThread(new Runnable() {

                                @Override
                                public void run() {

                                    pDialog.setProgress((int) dl_progress);

                                }
                            });

                            cursor.close();
                        }

                    }
                }).start();

            }
            else {
                setDownloadStatus(false);
                showToast("Please check your internet connection.");
            }

            return path;



         /*   URL url = new URL("http://smartprobe.com.au/uploads/SmartProbe-2.0.7.apk");//"https://www.nseindia.com/content/historical/DERIVATIVES/2016/AUG/fo05AUG2016bhav.csv.zip");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

           // urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);

            //connect
            urlConnection.connect();

            //set the path where we want to save the file
            File SDCardRoot = Environment.getExternalStorageDirectory();
            //create a new file, to save the downloaded file
            File file = new File(SDCardRoot,"Download/downloaded_file1.apk");

            FileOutputStream fileOutput = new FileOutputStream(file);

            //Stream used for reading the data from the internet
            InputStream inputStream = urlConnection.getInputStream();

            //this is the total size of the file which we are downloading
            int lenghtOfFile = urlConnection.getContentLength();
            Log.e("DEBUG", "urlConnection.getContentLength():"
                    + lenghtOfFile);
            long total = 0;
            //create a buffer...
            byte[] buffer = new byte[1024];
            int bufferLength = 0;

            while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
                // writing data to file
                fileOutput.write(buffer, 0, bufferLength);
                total += bufferLength;
                // publishing the progress....
                // After this onProgressUpdate will be called
                publishProgress("" + (int) ((total * 100) / lenghtOfFile));

            }

            //close the output stream when complete //
            fileOutput.close();
*/

        } catch (Exception e) {
            Log.e("Error: ", e.getMessage());
            setDownloadStatus(false);
        }

        return null;
    }

    private void onFinish(String file_url) {

        if (file_url != null) {

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(file_url)), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // without this flag android returned a intent error!
            context.startActivity(intent);
        }
    }

    private void dismissDialog() {

        if (pDialog != null)
            pDialog.dismiss();
    }


    private String checkDownloadStatus(Cursor c) {
        String msg = "???";

        switch (c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
            case DownloadManager.STATUS_FAILED:
                msg = "Download failed!";
                setDownloadStatus(false);
                showToast(msg);
                break;

            case DownloadManager.STATUS_PAUSED:
                msg = "Download paused!";
                setDownloadStatus(true);

                if(!isDownloadPaused)
                showToast(msg);

                isDownloadPaused =true;
                break;

            case DownloadManager.STATUS_PENDING:
                msg = "Download pending!";
                //setDownloadStatus(false);
                // showToast(msg);
                break;

            case DownloadManager.STATUS_RUNNING:
                msg = "Download in progress!";
                downloading = true;
                break;

            case DownloadManager.STATUS_SUCCESSFUL:
                msg = "Download complete!";

                setDownloadStatus(false);
                showToast(msg);
                onFinish(path);
                break;

            default:
                msg = "Download is nowhere in sight";
                setDownloadStatus(false);
                showToast(msg);
                break;
        }

        return (msg);
    }

    private void setDownloadStatus(boolean status) {

        downloading = status;
        dismissDialog();
    }

    private void showToast(final String msg) {

        context.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
