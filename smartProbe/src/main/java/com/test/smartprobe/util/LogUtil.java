package com.test.smartprobe.util;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by abitha on 22/4/16.
 * <p/>
 * Class to writeDeviceLog log to file in sdcard.
 */
public class LogUtil {

    public static final String TAG = "SmartProbe";
    private static boolean isDebuging = false;

    public static File getFileLocation() {

        //if there is no SD card, create new directory objects to make directory on device
        if (Environment.getExternalStorageState() == null) {
            //create new file directory object
            return Environment.getDataDirectory();
        } else
            return Environment.getExternalStorageDirectory();

    }

    public static String getFileLocation(String filename) {
        String fileLoc = getFileLocation() + "/SmartProbe/Log";

        File logFile = new File(fileLoc);

        if (!logFile.exists()) {
            logFile.mkdirs();
        }
        return fileLoc + filename;
    }

    public static void writeDeviceLog(String text) {

       File logFile = new File(getFileLocation("/device_readings.txt"));
          write(logFile, text);
    }

    public static void writeServerStatus(String text) {

        File logFile = new File(getFileLocation("/server_connection.txt"));
        write(logFile, text);
    }


    public static void writeLogTest(String text) {

        File logFile = new File(getFileLocation("/app_log.txt"));
          write(logFile, text);
    }


    private static void write(File logFile, String text) {

        if (isDebuging) {

            if (!logFile.exists()) {
                try {
                    logFile.createNewFile();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            writeToFile(logFile, text);
        } else {

            if (logFile.exists()) {
                logFile.delete();
            }
        }
    }


    private static void writeToFile(File logFile, String text) {

        try {
            Log.e("SmartProbe",text);
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));

            SimpleDateFormat dateFormat = new SimpleDateFormat(("dd-MM-yyyy hh:mm:ss a"));
            String localDateTime = dateFormat.format(new Date());

            buf.append(localDateTime + " : " + text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
