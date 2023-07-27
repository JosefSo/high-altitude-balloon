/*
 * Copyright 2014 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ariel.balloonspy.utility;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Log {

    public static final int VERBOSE = 2;
    public static final int DEBUG = 3;
    public static final int INFO = 4;
    public static final int WARN = 5;
    public static final int ERROR = 6;
    public static final int ASSERT = 7;

    private static final String TAG = "Log";

    private static boolean DEBUG_ENABLED = false;
    private static String PATH = "ApplicationLog.txt";

    private static long logSize = 1048576; //2^20;
    private static OnLogListener logListener;

    public static void setDebug(boolean debug) {
        DEBUG_ENABLED = debug;
    }

    public static void setLogSize(long logSize) {
        if (logSize > 0) {
            Log.logSize = logSize;
        }
    }

    public static void setPath(String path) {
        if (path.endsWith("/")) {
            PATH = path + "ApplicationLog.txt";
        } else if (!path.endsWith(".txt")) {
            PATH  = path + ".txt";
        } else {
            PATH = path;
        }
    }

    public static void setLogListener(OnLogListener listener) {
        logListener = listener;
    }

    public static void e(String tag, String message) {
        if (DEBUG_ENABLED) {
            int logResult = android.util.Log.e(tag, message);
            if (logResult > 0)
                logToFile(tag, message);
        }
    }

    public static void e(String tag, String message, Throwable error) {
        if (DEBUG_ENABLED) {
            int logResult = android.util.Log.e(tag, message, error);
            if (logResult > 0)
                logToFile(tag, message + "\r\n" + android.util.Log.getStackTraceString(error));
        }
    }

    public static void v(String tag, String message) {
        if (DEBUG_ENABLED) {
            int logResult = android.util.Log.v(tag, message);
            if (logResult > 0)
                logToFile(tag, message);
        }
    }

    public static void v(String tag, String message, Throwable error) {
        if (DEBUG_ENABLED) {
            int logResult = android.util.Log.v(tag, message, error);
            if (logResult > 0)
                logToFile(tag, message + "\r\n" + android.util.Log.getStackTraceString(error));
        }
    }

    public static void d(String tag, String message) {
        if (DEBUG_ENABLED) {
            int logResult = android.util.Log.d(tag, message);
            if (logResult > 0)
                logToFile(tag, message);
        }
    }

    public static void d(String tag, String message, Throwable error) {
        if (DEBUG_ENABLED) {
            int logResult = android.util.Log.d(tag, message, error);
            if (logResult > 0)
                logToFile(tag, message + "\r\n" + android.util.Log.getStackTraceString(error));
        }
    }

    public static void i(String tag, String message) {
        if (DEBUG_ENABLED) {
            int logResult = android.util.Log.i(tag, message);
            if (logResult > 0)
                logToFile(tag, message);
        }
    }

    public static void i(String tag, String message, Throwable error) {
        if (DEBUG_ENABLED) {
            int logResult = android.util.Log.i(tag, message, error);
            if (logResult > 0)
                logToFile(tag, message + "\r\n" + android.util.Log.getStackTraceString(error));
        }
    }

    public static void w(String tag, String message) {
        if (DEBUG_ENABLED) {
            int logResult = android.util.Log.w(tag, message);
            if (logResult > 0)
                logToFile(tag, message);
        }
    }

    public static void w(String tag, String message, Throwable error) {
        if (DEBUG_ENABLED) {
            int logResult = android.util.Log.w(tag, message, error);
            if (logResult > 0)
                logToFile(tag, message + "\r\n" + android.util.Log.getStackTraceString(error));
        }
    }

    public static boolean isLoggable(String string, int num) {
        return true;
    }

    private static String getDateTimeStamp() {
        Date dateNow = Calendar.getInstance().getTime();
        return (DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, Locale.US).format(dateNow));
    }

    private static void logToFile(String tag, String message) {
        try {
            File logFile = new File(Environment.getExternalStorageDirectory(), PATH);
            if (!logFile.exists()) {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            }
            if (logFile.length() > Log.logSize) {
                //relative path rename file
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String newFileName = logFile.getParent() + File.separator + timeStamp + "_" + logFile.getName();
                File newFile = new File(newFileName);
                if(logFile.renameTo(newFile)){
                    //create new file
                    logFile = new File(Environment.getExternalStorageDirectory(), PATH);
                    logFile.createNewFile();
                }else{
                    // continue with the existing
                    System.out.println("File rename failed");
                }
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
            writer.write(String.format("%1s;%2s;%3s\r\n", getDateTimeStamp(), tag, message));
            writer.close();

            if (logListener != null) {
                logListener.onLogged(tag, message);
            }
        } catch (IOException e) {
            android.util.Log.e(TAG, "Unable to log exception to file.", e);
        }
    }
}