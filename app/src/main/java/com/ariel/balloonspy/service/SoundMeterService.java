package com.ariel.balloonspy.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.io.IOException;

public class SoundMeterService extends Service {

    private SoundMeterService mSensor;
    private static int mThreshold = 10;
    private static int mHitThreshold = 1;
    private static int mHitCount = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }

    static final private double EMA_FILTER = 0.6;

    private MediaRecorder mRecorder = null;
    private double mEMA = 0.0;

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            try {
                mSensor = new SoundMeterService();
                mSensor.start();
                long start = System.currentTimeMillis();
                long timeElapsedMsec = System.currentTimeMillis() - start;
                long timeElpasedMin = timeElapsedMsec / (60L * 1000L);

                String message = "0:0";
                sendBroadcastMessage("UPDATE_HIT_COUNT", message,"CPM");

                while (true) {
                    double amp = mSensor.getAmplitudeDB();
                    int counter = 0;
                    while (amp > mThreshold && counter < mHitThreshold) {
                        counter++;
                        amp = mSensor.getAmplitudeDB();
                        //Log.d("SNG", amp + ":" + counter);
                    }

                    if (counter >= mHitThreshold) {
                        mHitCount++;
                        timeElapsedMsec = System.currentTimeMillis() - start;
                        timeElpasedMin = timeElapsedMsec / (60L * 1000L);

                        message = mHitCount + ":" + timeElpasedMin;
                        sendBroadcastMessage("UPDATE_HIT_COUNT", message,"CPM");
                    }
                }

            } catch (IOException e) {
                mSensor.stop();
                // Restore interrupt status.
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work doesn't disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }



    public void start() throws IOException {
        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile("/dev/null");
            mRecorder.prepare();
            mRecorder.start();
            mEMA = 0.0;
        }
    }

    public void stop() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public double getAmplitude() {
        if (mRecorder != null)
            return  (mRecorder.getMaxAmplitude()/2700.0);
        else
            return 0;

    }

    public double getMaxAmplitude() {
        if (mRecorder != null)
            return  mRecorder.getMaxAmplitude();
        else
            return 0;

    }

    public double getAmplitudeDB() {
        if (mRecorder != null)
            return   20 * Math.log10(mRecorder.getMaxAmplitude() / 2700.0);
        else
            return 0;

    }

    public double getAmplitudeEMA() {
        double amp = getAmplitude();
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
        return mEMA;
    }

    private void sendBroadcastMessage(String intentFilterName, String arg1, String extraKey) {
        Intent intent = new Intent(intentFilterName);
        if (arg1 != null && extraKey != null) {
            intent.putExtra(extraKey, arg1);
        }
        sendBroadcast(intent);
    }

    /*public void smartGeiger(){
        mSensor = new SoundMeterService();

        Runnable mSensorTask = new Runnable() {
            public void run() {
                try {
                    mSensor.start();
                } catch (IOException e) {
                    e.printStackTrace();
                    mSensor.stop();
                }

                long start = System.currentTimeMillis();
                while (true){
                    double amp = mSensor.getAmplitude();
                    int counter = 0;
                    while (amp > mThreshold){
                        counter++;
                        amp = mSensor.getAmplitude();
                    }

                    if (counter >= 4) {
                        mHitCount++;
                        long timeElapsedMsec = System.currentTimeMillis() - start;
                        long timeElpasedMin = timeElapsedMsec / (60L * 1000L);

                        performPostCall(mHitCount + ":" + timeElpasedMin);

                        Log.i("SNG", mHitCount + ":" + timeElpasedMin);
                    }
                }
            }
        };
        Thread t = new Thread(mSensorTask);
        // Lets run Thread in background..
        // Sometimes you need to run thread in background for your Timer application..
        t.start();
    }*/
}