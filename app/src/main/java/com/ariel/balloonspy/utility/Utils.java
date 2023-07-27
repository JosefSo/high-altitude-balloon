package com.ariel.balloonspy.utility;

import android.app.Activity;
import android.media.ExifInterface;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class Utils {

    private static boolean shouldShowToast = true;

    public static void setShouldShowToast(boolean shouldShowToast) {
        Utils.shouldShowToast = shouldShowToast;
    }

    public static void showToast(final Activity activity, final String text) {
        if (Utils.shouldShowToast) {
            activity.runOnUiThread(() ->
                    Toast.makeText(activity.getApplicationContext(), text, Toast.LENGTH_SHORT).show()
            );
        }
    }

    public static void makeDir(String folderName) {
        try {
            File picFolder = new File(Environment.getExternalStorageDirectory() + File.separator + "prbl" + File.separator + folderName);

            if (!picFolder.exists() && !picFolder.isDirectory()) {
                // create empty directory
                if (picFolder.mkdirs()) {
                    Log.i("CreateDir", "App dir created");
                } else {
                    Log.w("CreateDir", "Unable to create app dir!");
                }
            } else {
                Log.i("CreateDir", "App dir already exists");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveExif(String fileNameWithPath, double latitude, double longitude) {
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(fileNameWithPath);
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convertToDegreeMinuteSeconds(latitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, getLatitudeRef(latitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convertToDegreeMinuteSeconds(longitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, getLongitudeRef(longitude));
            exif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * returns ref for latitude which is S or N.
     *
     * @param latitude
     * @return S or N
     */
    private static String getLatitudeRef(double latitude) {
        return latitude < 0.0d ? "S" : "N";
    }
    /**
     * returns ref for latitude which is S or N.
     *
     * @param longitude
     * @return W or E
     */
    private static String getLongitudeRef(double longitude) {
        return longitude < 0.0d ? "W" : "E";
    }
    /**
     * convert latitude into DMS (degree minute second) format. For instance<br/>
     * -79.948862 becomes<br/>
     * 79/1,56/1,55903/1000<br/>
     * It works for latitude and longitude<br/>
     *
     * @param latitude could be longitude.
     * @return
     */
    private static String convertToDegreeMinuteSeconds(double latitude) {
        latitude = Math.abs(latitude);
        int degree = (int) latitude;
        latitude *= 60;
        latitude -= (degree * 60.0d);
        int minute = (int) latitude;
        latitude *= 60;
        latitude -= (minute * 60.0d);
        int second = (int) (latitude * 1000.0d);
        StringBuilder sb = new StringBuilder();
        sb.append(degree);
        sb.append("/1,");
        sb.append(minute);
        sb.append("/1,");
        sb.append(second);
        sb.append("/1000,");
        return sb.toString();
    }
}
