package com.ariel.balloonspy.utility;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class LoRaSendMessage {

    // IPAddr
    private static String ipAddr;
    private static boolean shouldSendMessage;

    public static void setIpAddr(String ipAddr) {
        if (ipAddr.length() > 0) {
            LoRaSendMessage.ipAddr = ipAddr;
        }
    }

    public static void setShouldSendMessage(boolean shouldSendMessage) {
        LoRaSendMessage.shouldSendMessage = shouldSendMessage;
    }

    public static void performPostCall(String message, int id) {

        if (!shouldSendMessage) return;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://" + ipAddr + "/PRBL/" + id + ";" + message + "/M");
                    HttpURLConnection connection = null;
                    try {
                        connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");
                        //BufferedReader in = new BufferedReader(
                        //        new InputStreamReader(connection.getInputStream()));
                        //Do something with this InputStream
                        // handle the response
                        int status = connection.getResponseCode();
                        // If response is not success
                        if (status != 200) {
                            throw new IOException("Post failed with error code " + status);
                        }

                        Log.i("LOR", url.toString());

                    } finally {
                        if(connection != null) // Make sure the connection is not null.
                            connection.disconnect();
                    }
                } catch (Exception e) {
                    e.toString();
                }
            }
        });

        Random rand = new Random();
        int randomNum = 1 + rand.nextInt(15);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                // this code will be executed after 2 seconds
                thread.start();
            }
        }, randomNum * 1000);
    }

}
