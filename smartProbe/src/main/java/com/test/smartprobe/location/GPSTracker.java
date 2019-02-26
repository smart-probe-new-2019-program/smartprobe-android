package com.test.smartprobe.location;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import com.test.smartprobe.R;
import com.test.smartprobe.activity.SmartProbeActivity;
import com.test.smartprobe.util.LogUtil;

public class GPSTracker extends Service implements LocationListener {

    private final SmartProbeActivity mContext;

    // flag for GPS status
    boolean isGPSEnabled = false;
    public final int REQUEST_CHECK_SETTINGS = 0x1;
    // flag for network status
    boolean isNetworkEnabled = false;
    Handler mHandler = new Handler();
    // flag for GPS status
    boolean canGetLocation = false;

    Location location; // location
    double latitude; // latitude
    double longitude; // longitude

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

    // Declaring a Location Manager
    protected LocationManager locationManager;
    private AlertDialog alertEnable;

    private boolean isPassiveEnabled;

    public GPSTracker(SmartProbeActivity context) {
        this.mContext = context;
        trackLocation();
    }

    public void trackLocation() {
        try {
            Log.e("SmartProb", "************ trackLocation : ");
            locationManager = (LocationManager) mContext
                    .getSystemService(LOCATION_SERVICE);

            // getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            // getting passive status
            isPassiveEnabled = locationManager
                    .isProviderEnabled(LocationManager.PASSIVE_PROVIDER);


            if (!isGPSEnabled && !isNetworkEnabled && (locationManager.getAllProviders().size() != 1 && isPassiveEnabled)) {
                // no network provider is enabled
                Log.d("Location", "No network provider is enabled");

//               showAlert();

            } else if (isPassiveEnabled && locationManager.getAllProviders().size() == 1) {

                this.canGetLocation = true;
                locationManager.requestLocationUpdates(
                        LocationManager.PASSIVE_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                Log.d("Location", "Passive");
                if (locationManager != null) {
                    location = locationManager
                            .getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                    }
                }
            } else {
                Log.d("Location", "network provider is enabled");
                this.canGetLocation = true;

                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Log.d("Location", "Network");
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        Log.d("Location", "GPS Enabled");
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

   /* private void showAlert() {

        Toast.makeText(mContext,"Location tracking is not supported.",Toast.LENGTH_LONG).show();
        mContext.updateSettings();
       *//* AlertDialog.Builder builder1 = new AlertDialog.Builder(mContext);
        builder1.setTitle("Location tracking");
        builder1.setMessage("Network provider not found to enable location tracking.");
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        mContext.trackCurrentLocation=false;
                    }
                });


        AlertDialog alert11 = builder1.create();
        alert11.show();*//*
    }
*/

    public Location getLocation() {

//        if (location == null)
//            trackLocation();

        return location;
    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     */
    public void stopUsingGPS() {
        if (locationManager != null) {
            locationManager.removeUpdates(GPSTracker.this);
        }

        location = null;
        latitude=0;
        longitude=0;
    }

    /**
     * Function to get latitude
     */
    public double getLatitude() {
        if (location != null) {
            latitude = location.getLatitude();
        }

        // return latitude
        return latitude;
    }

    /**
     * Function to get longitude
     */
    public double getLongitude() {
        if (location != null) {
            longitude = location.getLongitude();
        }

        // return longitude
        return longitude;
    }

    /**
     * Function to check GPS/wifi enabled
     *
     * @return boolean
     */
    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    /**
     * Function to show settings alert dialog
     * On pressing Settings button will lauch Settings Options
     */
    public void showSettingsAlert() {

        mHandler.post(new Runnable() {
            @Override
            public void run() {


                if (alertEnable != null && alertEnable.isShowing()) {

                    alertEnable.dismiss();

                }
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
                alertDialog.setIcon(R.drawable.ic_launcher);
                alertDialog.setCancelable(false);
                // Setting Dialog Title
                alertDialog.setTitle("Track Location");

                // Setting Dialog Message
                alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

                // On pressing Settings button
                alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        ((Activity) mContext).startActivityForResult(intent, REQUEST_CHECK_SETTINGS);
                    }
                });

                // on pressing cancel button
                alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                // Showing Alert Message
                alertEnable = alertDialog.create();
                alertEnable.show();
            }

        });

    }

    @Override
    public void onLocationChanged(Location location) {

        this.location = location;
        Log.d(LogUtil.TAG, "onLocationChanged");
        LogUtil.writeLogTest("Location changed= " + location.getLatitude() + " , " + location.getLongitude());
    }

    @Override
    public void onProviderDisabled(String provider) {

        canGetLocation = false;
        Log.d(LogUtil.TAG, "onProviderDisabled");
    }

    @Override
    public void onProviderEnabled(String provider) {

        canGetLocation = true;
        Log.d(LogUtil.TAG, "onProviderEnabled");
        trackLocation();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(LogUtil.TAG, "on provider StatusChanged");
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}
