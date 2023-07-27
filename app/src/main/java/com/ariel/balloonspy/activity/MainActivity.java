package com.ariel.balloonspy.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.StrictMode;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ariel.balloonspy.R;
import com.ariel.balloonspy.service.APictureCapturingService;
import com.ariel.balloonspy.service.MyExceptionHandler;
import com.ariel.balloonspy.service.MyService;
import com.ariel.balloonspy.service.PictureCapturingListener;
import com.ariel.balloonspy.service.VideoCapturingServiceImpl;
import com.ariel.balloonspy.utility.LoRaSendMessage;
import com.ariel.balloonspy.utility.Log;
import com.ariel.balloonspy.utility.Utils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity implements PictureCapturingListener,
        ActivityCompat.OnRequestPermissionsResultCallback, SensorEventListener {

    private static final String[] requiredPermissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            //Manifest.permission.RECEIVE_BOOT_COMPLETED,
            //Manifest.permission.WAKE_LOCK,
            //Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.RECORD_AUDIO
    };

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_CODE = 1;
    public static Context context;
    private static MainActivity instance;

    //Intent mServiceIntent;
    //private SensorService mSensorService;

    public static SensorManager sensorManager;

    //private EZCam mCamera;
    private boolean cameraClosed = true;
    private TextureView mTextureView;

    //The capture service
    //private APictureCapturingService pictureService;
    private APictureCapturingService videoService;
    //private Intent soundMeterService;
    private Location lastLocation;

    private SharedPreferences mPrefs;
    private CountDownTimer countDownTimer;

    private long lastTimeVideoTakenMillis;
    private long lastTimeCameraTakenMillis;

    private BroadcastReceiver mBroadcastReceiver;
    // Alt 1
    private double alt1;
    // Diff 1
    private double diff1;
    // Alt 2
    private double alt2;
    // Diff 2
    private double diff2;
    // Alt 3
    private double alt3;
    // Diff 3
    private double diff3;
    // Min GPS Update
    private int minGSPUpdate = 1; //1 minutes
    // Min Capture Vid
    private int minCaptureVid = 5; //1 minutes
    private int minCapturePic = 1; //1 minutes
    // TextNowCheckBox
    //private boolean textNowCheckBox;
    // showToastCheckBox
    private boolean showToastCheckBox;
    //Battery Level
    private int batteryLevel = -1;
    private int minBatLevel = 5;
    private float lastBatTemp = 0F;
    // ipAddr CheckBox
    private boolean ipAddrCheckBox;
    // geigerSns CheckBox
    private boolean geigerSnsCheckBox;
    // IPAddr
    private String ipAddr;
    // vidTime
    private int vidTime;
    private int videoTime = 1;
    // picTime
    private int picTime;

    //private String lastGeiger = null;
    private String lastPres = null;
    private TextView view_gps_lat;
    private TextView view_gps_lng;
    private TextView view_gps_alt;
    private TextView view_gps_spd;
    private TextView view_gps_hed;
    private TextView view_gps_accuracy;
    private TextView view_gps_extra;

    private ImageChangeBroadcastReceiver imageChangeBroadcastReceiver;
    private ImageView interceptedNotificationImageView;
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    private AlertDialog enableNotificationListenerAlertDialog;


    public static MainActivity getInstance() {
        return instance;
    }

    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }

    public Context getContext() {
        return context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        context = instance = this;

        setLayout();

        checkPermissions();
        getPreferences();

        Thread.setDefaultUncaughtExceptionHandler(new MyExceptionHandler(this));
        if (getIntent().getBooleanExtra("crash", false)) {
            Toast.makeText(this, "App restarted after crash", Toast.LENGTH_SHORT).show();
        }

        String message = "MAIN_CREATE";
        Log.d("MAN", message);
        LoRaSendMessage.performPostCall(message, 5);

        Log.setPath("prbl/log/log.txt");
        Log.setDebug(true);

        Utils.makeDir("pic");
        Utils.makeDir("vid");
        Utils.makeDir("log");
        Utils.makeDir("vid/compressed");

        // Register receiver with Action and no need to define Broadcast in manifest.xml
        registerReceiver(BatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_LOW));

        startService(new Intent(getContext(), MyService.class));

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        videoService = VideoCapturingServiceImpl.getInstance(this);
        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);

        initGPS();

        usingCountDownTimer();

        imageChangeBroadcastReceiver = new ImageChangeBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.myapplication");
        registerReceiver(imageChangeBroadcastReceiver, intentFilter);
        interceptedNotificationImageView
                = (ImageView) this.findViewById(R.id.intercepted_notification_logo);

        if (!isNotificationServiceEnabled()) {
            enableNotificationListenerAlertDialog = buildNotificationServiceAlertDialog();
            enableNotificationListenerAlertDialog.show();
        }

        // Disable the NotificationListenerService
        ComponentName componentName = new ComponentName(this, MyNotificationListenerService.class);
        PackageManager packageManager = getPackageManager();
        packageManager.setComponentEnabledSetting(componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        // Re-enable the service if the app is meshApp
        String packageName = getPackageName();
        String meshApp = "com.geeksville.mesh";
        if (!packageName.equals(meshApp)) {
            packageManager.setComponentEnabledSetting(componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        }

    }

    @Override
    protected void onDestroy() {
        //stopService(mServiceIntent);

        try {
            unregisterReceiver(BatteryReceiver);
            unregisterReceiver(mBroadcastReceiver);
            unregisterReceiver(imageChangeBroadcastReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        String message = "MAIN_DESTROYED";
        Log.i("MAN", message);
        LoRaSendMessage.performPostCall(message, 5);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Code to refresh preferences
        getPreferences();
        //initGeiger();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_CODE: {
                if (!(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    checkPermissions();
                }
            }
        }
    }


    /**
     * checking  permissions at Runtime.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        final List<String> neededPermissions = new ArrayList<>();
        for (final String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    permission) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(permission);
            }
        }
        if (!neededPermissions.isEmpty()) {
            requestPermissions(neededPermissions.toArray(new String[]{}),
                    MY_PERMISSIONS_REQUEST_ACCESS_CODE);
        }
    }

    // Set the main screen layout
    private void setLayout() {
        // Set cuurent Bat Level N/A
        int level = getBatteryPercentage(getApplicationContext());
        ((TextView) findViewById(R.id.BAT)).setText(level + "%");

        view_gps_lat = (TextView) findViewById(R.id.LAT);
        view_gps_lng = (TextView) findViewById(R.id.LON);
        view_gps_alt = (TextView) findViewById(R.id.ALT);
        view_gps_spd = (TextView) findViewById(R.id.SPD);
        view_gps_hed = (TextView) findViewById(R.id.HED);
        view_gps_accuracy = (TextView) findViewById(R.id.display_gps_accuracy);
        view_gps_extra = (TextView) findViewById(R.id.display_gps_extra);

        // Settings
        Button buttonSettings = (Button) findViewById(R.id.main_settings);
        buttonSettings.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent);
            }
        });

        // Capture Video
        Button buttonStartRec = (Button) findViewById(R.id.main_video);
        buttonStartRec.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (videoService != null) {
                    cameraClosed = false;
                    videoService.setVidTime(vidTime);
                    videoService.startCapturing(MainActivity.this);
                }
            }
        });

        // Capture Pic (2 sec video)
        Button buttonTakePic = (Button) findViewById(R.id.main_picture);
        buttonTakePic.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (videoService != null) {
                    cameraClosed = false;
                    videoService.setVidTime(picTime);
                    videoService.startCapturing(MainActivity.this);
                }
            }
        });
    }

    // Init Device GPS
    private void initGPS() {
        LocationManager locationManagerGPS = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManagerGPS.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationListenerGPS.onProviderEnabled("GPS");
        } else {
            locationListenerGPS.onProviderDisabled("GPS");
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            return;
        }
        /*
        If it is greater than 0 then the location provider will only send your application an update
        when the location has changed by at least minDistance meters,
        AND at least minTime milliseconds have passed
         */
        locationManagerGPS.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                minGSPUpdate * 60 * 1000,
                0,
                locationListenerGPS);
    }

    // Listen for gps changes
    LocationListener locationListenerGPS = new LocationListener() {
        public void onProviderEnabled(String provider) {
            final TextView view_gps = (TextView) findViewById(R.id.display_gps_status_enabled);
            view_gps.setText(provider + " enabled");
        }

        public void onProviderDisabled(String provider) {
            final TextView view_gps = (TextView) findViewById(R.id.display_gps_status_enabled);
            view_gps.setText(provider + " disabled");
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            final TextView view_gps = (TextView) findViewById(R.id.display_gps_status);
            String str = "Status (" + provider + "): ";
            if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
                str += "temporarily unavailable";
            } else if (status == LocationProvider.OUT_OF_SERVICE) {
                str += "out of service";
            } else if (status == LocationProvider.AVAILABLE) {
                str += "available";
            } else {
                str += "unknown";
            }
            view_gps.setText(str);
        }

        public void onLocationChanged(Location location) {
            String lat = String.valueOf(location.getLatitude());
            String lon = String.valueOf(location.getLongitude());
            String alt = String.valueOf(location.getAltitude());
            String acc = String.valueOf(location.getAccuracy());
            int speed = (int) ((location.getSpeed() * 3600) / 1000);
            String spd = String.valueOf(speed);
            String hed = String.valueOf(location.getBearing());

            view_gps_lat.setText(lat);
            view_gps_lng.setText(lon);
            view_gps_alt.setText(alt);
            view_gps_spd.setText(spd);
            view_gps_hed.setText(hed);
            view_gps_accuracy.setText("accuracy:" + acc);

            if (location.getExtras() != null) {
                Bundle extra = location.getExtras();
                String estr = "";
                for (String s : extra.keySet()) {
                    estr += "  " + s + ": " + extra.get(s).toString() + "\n";
                }
                view_gps_extra.setText(estr);
            }

            updatedLastUpdated((TextView) findViewById(R.id.display_gps_updated));

            String message = lat + ";" + lon + ";" + alt + ";" + acc + ";" + spd + ";" + hed;
            Log.i("GPS", message);
            Utils.showToast(MainActivity.this, "GPS:" + message);
            LoRaSendMessage.performPostCall(message, 4);


            if (null != lastLocation) {
                float distanceInMeters = lastLocation.distanceTo(location);
                Log.i("GPS", "Distance In Meters:" + distanceInMeters);
                boolean takePic = shouldTakePicByMeters(location.getAltitude(), distanceInMeters) &&
                        shouldDoActionByPower();

                if (null != videoService /*&& cameraClosed*/) {
                    if (takePic) {
                        cameraClosed = false;
                        videoService.setVidTime(picTime);
                        lastTimeCameraTakenMillis = SystemClock.elapsedRealtime();
                        Log.i("PIC", "Take Pic");

                        String metadata = "_" + lat + "_" + lon + "_" + alt;
                        videoService.setMetadata(metadata);
                        videoService.startCapturing(MainActivity.this);
                    }
                }
            }

            lastLocation = new Location(location);
        }
    };

    // Make sure the app is always running
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i("isMyServiceRunning?", true + "");
                return true;
            }
        }
        Log.i("isMyServiceRunning?", false + "");
        return false;
    }


    protected void updatedLastUpdated(TextView t) {
        String date = DateFormat.getDateTimeInstance().format(System.currentTimeMillis());
        t.setText("updated: " + date);
    }

    // shouldTakePicByMeters: Check if the altitude and the difference in meters is met
    private boolean shouldTakePicByMeters(double alt, double meters) {
        if (alt >= alt1) { // Alt is between alt1 and  alt2
            if (meters >= diff1) {
                return true;
            }
        } else if (alt > alt2 && alt <= alt3) { // Alt is between alt2 and alt3
            if (meters >= diff2) {
                return true;
            }
        } else if (alt > alt3) { // Alt is above alt3
            if (meters >= diff3) {
                return true;
            }
        }

        // if in 10 mintues no puc taken, take one.
        boolean timePass = SystemClock.elapsedRealtime() - lastTimeCameraTakenMillis > 10 * 60 * 1000;
        if (timePass) {
            cameraClosed = true;
            return true;
        }
        return false;
    }

    // shouldDoActionByPower: Check if the altitude and the diffrence in meters is met
    private boolean shouldDoActionByPower() {
        if (batteryLevel <= 0 || batteryLevel >= minBatLevel) {
            return true;
        }

        return false;
    }

    // Battery Level
    private BroadcastReceiver BatteryReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            boolean batteryLow = intent.getAction().equals(Intent.ACTION_BATTERY_LOW);
            batteryLevel = getBatteryPercentage(getApplicationContext());

            TextView batterLevel = (TextView) findViewById(R.id.BAT);
            batterLevel.setText(batteryLevel + "%");
            Log.i("BAT", batteryLevel + "%");
        }
    };

    // Battery Percentage
    public static int getBatteryPercentage(Context context) {

        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);

        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

        float batteryPct = level / (float) scale;

        return (int) (batteryPct * 100);
    }

    // Battery Temperature
    public float batteryTemperature() {
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        float temp = ((float) intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)) / 10;
        return temp;
    }

    public void onCaptureDone(String pictureUrl, byte[] pictureData) {

    }

    public void onDoneCapturingAllPhotos(TreeMap<String, byte[]> picturesTaken) {

    }

    // Counter
    public void usingCountDownTimer() {
        countDownTimer = new CountDownTimer(Long.MAX_VALUE, 1 * 60 * 1000) {
            // This is called after every 1 min interval.
            public void onTick(long millisUntilFinished) {
                // Pressure
                List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_PRESSURE);
                if (sensors.size() > 0) {
                    Sensor sensor = sensors.get(0);
                    sensorManager.registerListener(MainActivity.this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
                }

                // Battery
                int newBatteryLevel = getBatteryPercentage(getApplicationContext());
                if (newBatteryLevel != batteryLevel) {
                    batteryLevel = newBatteryLevel;

                    TextView batterLevel = (TextView) findViewById(R.id.BAT);
                    String message = batteryLevel + "%";
                    batterLevel.setText(message);
                    Log.i("BAT", message);
                    LoRaSendMessage.performPostCall(message, 4);
                }

                // Temperature
                float newBatTemp = batteryTemperature();
                if (newBatTemp != lastBatTemp) {
                    lastBatTemp = newBatTemp;

                    String message = lastBatTemp + "C";
                    TextView geigerTextView = (TextView) findViewById(R.id.GEIG);
                    geigerTextView.setText(message);
                    Log.i("TEM", message);
                    LoRaSendMessage.performPostCall(message, 4);
                }

                if (shouldDoActionByPower()) {
//                    takePicVidByTime(); //TODO: Verify
                }

                // if bat lost, stop sound meter
                //if (!shouldDoActionByPower()) {
                //    stopService(soundMeterService);
                //}

                /*if (null != lastGeiger) {
                    LoRaSendMessage.performPostCall(lastGeiger, 2);
                }*/
            }

            public void onFinish() {
                start();
            }
        }.start();
    }

    public void takePicVidByTime() {
        boolean takePic = (SystemClock.elapsedRealtime() - lastTimeCameraTakenMillis) > (minCapturePic * 60 * 1000);
        boolean takeVid = (SystemClock.elapsedRealtime() - lastTimeVideoTakenMillis) > (minCaptureVid * 60 * 1000);

        if (null != videoService /*&& cameraClosed*/) {
            if (takeVid || takePic) {
                cameraClosed = false;
                if (takeVid) {
                    videoService.setVidTime(vidTime);
                    lastTimeVideoTakenMillis = SystemClock.elapsedRealtime();
                    Log.i("VDO", "Take Vid");
                } else {
                    videoService.setVidTime(picTime);
                    lastTimeCameraTakenMillis = SystemClock.elapsedRealtime();
                    Log.i("PIC", "Take Pic");
                }
                videoService.startCapturing(MainActivity.this);
            }
        }
    }


    // Pressure Sensor
    @Override
    public void onSensorChanged(SensorEvent event) {

        Sensor sensor = event.sensor;
        if ((sensor.getType() == Sensor.TYPE_PRESSURE)) {
            sensorManager.unregisterListener(MainActivity.this);
            String pres = String.valueOf(event.values[0]);
            String tPres = String.valueOf((long) event.values[0] / 10); // filter for larger changes

            if (null == this.lastPres || !this.lastPres.equalsIgnoreCase(tPres)) {
                TextView change_pres = (TextView) findViewById(R.id.PRES);
                change_pres.setText(pres);
                Log.i("PRE", pres);
                this.lastPres = tPres;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // Preferences
    private void getPreferences() {
        mPrefs = getSharedPreferences("SpyBalloon", MODE_PRIVATE);
        if (null != mPrefs) {
            // Alt 1
            String sAlt1 = mPrefs.getString("alt1", "");
            if (null != sAlt1 && sAlt1.length() > 0) {
                alt1 = Double.valueOf(sAlt1);
            } else {
                alt1 = 0.0;
            }

            // Diff 1
            String sDiff1 = mPrefs.getString("diff1", "");
            if (null != sDiff1 && sDiff1.length() > 0) {
                diff1 = Double.valueOf(sDiff1);
            } else {
                diff1 = 0.0;
            }

            // Alt 2
            String sAlt2 = mPrefs.getString("alt2", "");
            if (null != sAlt2 && sAlt2.length() > 0) {
                alt2 = Double.valueOf(sAlt2);
            } else {
                alt2 = 0.0;
            }

            // Diff 2
            String sDiff2 = mPrefs.getString("diff2", "");
            if (null != sDiff2 && sDiff2.length() > 0) {
                diff2 = Double.valueOf(sDiff2);
            } else {
                diff2 = 0.0;
            }

            // Alt 3
            String sAlt3 = mPrefs.getString("alt3", "");
            if (null != sAlt3 && sAlt3.length() > 0) {
                alt3 = Double.valueOf(sAlt3);
            } else {
                alt3 = 0.0;
            }

            // Diff 3
            String sDiff3 = mPrefs.getString("diff3", "");
            if (null != sDiff3 && sDiff3.length() > 0) {
                diff3 = Double.valueOf(sDiff3);
            } else {
                diff3 = 0.0;
            }

            // TextNow CheckBox
            //textNowCheckBox = mPrefs.getBoolean("textNowCheckBox", false);

            // showToast CheckBox
            showToastCheckBox = mPrefs.getBoolean("showToastCheckBox", true);

            // LogSize
            String sLogSize = mPrefs.getString("logSize", "");
            if (null != sLogSize && sLogSize.length() > 0) {
                Log.setLogSize(Long.valueOf(sLogSize));
            }

            // minGPSUpdate
            String sMinGPSUpdate = mPrefs.getString("minGPSUpdate", "");
            if (null != sMinGPSUpdate && sMinGPSUpdate.length() > 0) {
                minGSPUpdate = Integer.valueOf(sMinGPSUpdate);
            } else {
                minGSPUpdate = 1;
            }

            // minCaptureVid
            String sMinCaptureVid = mPrefs.getString("minCaptureVid", "");
            if (null != sMinGPSUpdate && sMinCaptureVid.length() > 0) {
                minCaptureVid = Integer.valueOf(sMinCaptureVid);
            } else {
                minCaptureVid = 5;
            }

            // minBatLevel
            String sMinBatLevel = mPrefs.getString("batLevel", "");
            if (null != sMinBatLevel && sMinBatLevel.length() > 0) {
                minBatLevel = Integer.valueOf(sMinBatLevel);
            } else {
                minBatLevel = 5;
            }

            // IPAddr CheckBox
            ipAddrCheckBox = mPrefs.getBoolean("ipAddrCheckBox", true);

            // geigerSns CheckBox
            geigerSnsCheckBox = mPrefs.getBoolean("geigerSnsCheckBox", true);

            // IPAddrTextView
            String sIpAddr = mPrefs.getString("ipAddr", "");
            if (null != sIpAddr && sIpAddr.length() > 0) {
                ipAddr = sIpAddr;
            } else {
                ipAddr = "192.168.4.1";
            }

            // Vid Time
            String sVidTime = mPrefs.getString("vidTime", "");
            if (null != sVidTime && sVidTime.length() > 0) {
                vidTime = Integer.valueOf(sVidTime);
            } else {
                vidTime = 5;
            }

            // Pic Time
            String sPicTime = mPrefs.getString("picTime", "");
            if (null != sPicTime && sPicTime.length() > 0) {
                picTime = Integer.valueOf(sPicTime);
            } else {
                picTime = 5;
            }

        }

        LoRaSendMessage.setIpAddr(ipAddr);
        LoRaSendMessage.setShouldSendMessage(ipAddrCheckBox);

        Utils.setShouldShowToast(showToastCheckBox);
    }


    private AlertDialog buildNotificationServiceAlertDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(R.string.notification_listener_service);
        alertDialogBuilder.setMessage(R.string.notification_listener_service_explanation);
        alertDialogBuilder.setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    }
                });
        alertDialogBuilder.setNegativeButton(R.string.no,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // If you choose to not enable the notification listener
                        // the app. will not work as expected
                    }
                });
        return (alertDialogBuilder.create());
    }


    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Change Intercepted Notification Image
    private void changeInterceptedNotificationImage(int notificationCode) {
        switch (notificationCode) {
            case MyNotificationListenerService.InterceptedNotificationCode.FACEBOOK_CODE:
                interceptedNotificationImageView.setImageResource(R.drawable.facebook_logo);
                break;
            case MyNotificationListenerService.InterceptedNotificationCode.INSTAGRAM_CODE:
                interceptedNotificationImageView.setImageResource(R.drawable.instagram_logo);
                break;
            case MyNotificationListenerService.InterceptedNotificationCode.WHATSAPP_CODE:
                interceptedNotificationImageView.setImageResource(R.drawable.whatsapp_logo);
                break;
            case MyNotificationListenerService.InterceptedNotificationCode.MESHTASTIC_CODE:
                interceptedNotificationImageView.setImageResource(R.drawable.meshtastic_logo);
                break;
            case MyNotificationListenerService.InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE:
                interceptedNotificationImageView.setImageResource(R.drawable.other_notification_logo);
                break;
        }
    }

    public class ImageChangeBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int receivedNotificationCode = intent.getIntExtra("Notification Code", -1);
            String receivedNotificationText = intent.getStringExtra("Notification Text");
            changeInterceptedNotificationImage(receivedNotificationCode);
            if (receivedNotificationText.equals("Photo")) {
                if (videoService != null) {
                    cameraClosed = false;
                    videoService.setVidTime(picTime);
                    videoService.startCapturing(MainActivity.this);
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
                }
            } else if (receivedNotificationText.startsWith("Video")) {
                String[] parts = receivedNotificationText.split("-");
                if ((parts.length == 2)) {
                    String videoTimeText = parts[1];
                    int videoTime = Integer.parseInt(videoTimeText);
                    if (videoService != null) {
                        cameraClosed = false;
                        videoService.setVidTime(videoTime);
                        videoService.startCapturing(MainActivity.this);
                    }
                } else {
                    if (videoService != null) {
                        cameraClosed = false;
                        videoService.setVidTime(vidTime);
                        videoService.startCapturing(MainActivity.this);
                    }
                }
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
            } else if (receivedNotificationText.equals("Gallery")) {
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
            }
        }
    }


}