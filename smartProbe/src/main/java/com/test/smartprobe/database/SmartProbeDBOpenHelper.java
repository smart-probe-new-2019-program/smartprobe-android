package com.test.smartprobe.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SmartProbeDBOpenHelper extends SQLiteOpenHelper {

    public final static String TABLE_NAME = "smartprobe"; //To store all readed probes from master
    public final static String TABLE_NAME_SETTING = "setting"; //To store settings information
    public final static String TABLE_NAME_LATEST = "smartprobelatest"; //To store probe details of user
    public final static String TABLE_NAME_LOG = "smartprobelog"; //To store login information
    public final static String TABLE_NAME_ACC = "smartprobeacc"; //To store location information

    public final static String ID = "_id";
    public final static String USER_ID = "user_id";
    public final static String PROBESERIALNUMBER = "probeSerialNumber";
    public final static String TEMPERATURE_HIGH = "temperature_high";
    public final static String TEMPERATURE_LOW = "temperature_low";
    public final static String STATUS_1 = "status_1";
    public final static String STATUS_2 = "status_2";
    public final static String TIME = "time";
    public final static String DATE = "date";
    public final static String TRIP_1 = "trip_1";
    public final static String TRIP_2 = "trip_2";
    public final static String VOLTAGE = "voltage";

    //Added new fields
    public final static String PROBE_NAME = "probe_name";
    public final static String ALERT_HIGH = "alertHighLimit";
    public final static String ALERT_LOW = "alertLowLimit";
    public final static String WARNING_HIGH = "warningHighLimit";
    public final static String WARNING_LOW = "warningLowLimit";
    public final static String UNIT = "samplePeriodUnits";
    public final static String LOW_VOLTAGE = "voltagemin";
    public final static String DEFAULT_SENSOR = "defaultsensor";

    public final static String READ_TIME = "read_time";
    public final static String SEND_TIME = "send_time";
    public final static String TRACK_LOCATION = "track_location";
    public final static String ENABLE_ALARM = "isAlarm_enabled";

    public final static String USER_NAME = "user_name";
    public final static String PASS_WORD = "pass_word";
    public final static String LAST_SYNCED = "last_synced_time";
    public final static String ACCESS_KEY = "access_key";
    public final static String SECRET_KEY = "secret_key";

    public final static String[] columns = {ID, USER_ID, PROBESERIALNUMBER, TEMPERATURE_HIGH, TEMPERATURE_LOW,
            STATUS_1, STATUS_2, TIME, DATE, TRIP_1, TRIP_2};

    public final static String[] columnsSetting = {ID, READ_TIME, SEND_TIME};

    final private static String CREATE_CMD =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                    + USER_ID + " INTEGER, "
                    + PROBESERIALNUMBER + " TEXT, "
                    + TEMPERATURE_HIGH + " TEXT, "
                    + TEMPERATURE_LOW + " TEXT, "
                    + STATUS_1 + " TEXT, "
                    + STATUS_2 + " TEXT, "
                    + TIME + " TEXT, "
                    + DATE + " TEXT, "
                    + TRIP_1 + " TEXT, "
                    + TRIP_2 + " TEXT, "
                    + VOLTAGE + " TEXT )";

    final private static String CREATE_TABLE_LATEST =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_LATEST + " ("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                    + USER_ID + " INTEGER, "
                    + USER_NAME + " TEXT, "
                    + PROBESERIALNUMBER + " TEXT, "
                    + TEMPERATURE_HIGH + " TEXT, "
                    + TEMPERATURE_LOW + " TEXT, "
                    + STATUS_1 + " TEXT, "
                    + STATUS_2 + " TEXT, "
                    + TIME + " TEXT, "
                    + DATE + " TEXT, "
                    + TRIP_1 + " TEXT, "
                    + TRIP_2 + " TEXT, "
                    + VOLTAGE + " TEXT, "

                    + PROBE_NAME + " TEXT, "
                    + ALERT_HIGH + " TEXT, "
                    + ALERT_LOW + " TEXT, "
                    + WARNING_HIGH + " TEXT, "
                    + WARNING_LOW + " TEXT, "
                    + UNIT + " TEXT, "
                    + LOW_VOLTAGE + " TEXT,"
                    + DEFAULT_SENSOR + " TEXT )";

    final private static String CREATE_CMD_SETTING =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_SETTING + " ("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                    + READ_TIME + " TEXT, "
                    + SEND_TIME + " TEXT, "
                    + TRACK_LOCATION + " INTEGER, "
                    + ENABLE_ALARM + " INTEGER )";

    final private static String CREATE_TABLE_LOG =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_LOG + " ("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                    + USER_NAME + " TEXT, "
                    + PASS_WORD + " TEXT, "
                    + LAST_SYNCED + " TEXT )";

    final private static String CREATE_TABLE_ACC =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_ACC + " ("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                    + USER_NAME + " TEXT, "
                    + ACCESS_KEY + " TEXT, "
                    + SECRET_KEY + " TEXT )";

    final private static String DB_NAME = "smartprobe_db";
    final private static Integer VERSION = 4;
    final private Context mContext;


    public SmartProbeDBOpenHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
        // TODO Auto-generated constructor stub
        this.mContext = context;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_LATEST);
            db.execSQL(CREATE_TABLE_LATEST);

            //clear last update time to get all data from server
            ContentValues values = new ContentValues();
            values.put(SmartProbeDBOpenHelper.LAST_SYNCED, "");
            db.update(SmartProbeDBOpenHelper.TABLE_NAME_LOG, values,null, null);
            values.clear();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL(CREATE_CMD);
        db.execSQL(CREATE_CMD_SETTING);
        db.execSQL(CREATE_TABLE_LATEST);
        db.execSQL("INSERT INTO " + TABLE_NAME_SETTING + "(" + READ_TIME + ", " + SEND_TIME + "," + TRACK_LOCATION + "," + ENABLE_ALARM + ") VALUES('1', '1', 0,1)");
        db.execSQL(CREATE_TABLE_LOG);
        db.execSQL(CREATE_TABLE_ACC);
    }


}
