package com.ariel.balloonspy.utility;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.ariel.balloonspy.activity.MainActivity;

import java.io.IOException;
import java.util.ArrayList;

public class Sender {


    public static ArrayList<Node> list = new ArrayList<>();
    public static Context context;

    public static void main(String[] r) {
        Sender s = new Sender();
    }

    public Sender() {
        context = MainActivity.context;
    }

    public static boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
        if (null != connectivityManager) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }


    public void sendNode()
    {
        if(list.size() == 0){
            return;
        }

        Node node = list.get(list.size()-1);
        TextNow textnow = new TextNow();
        try {
            textnow.sendMessage("roey.sdomi",node.picName);
            String url = MultipartUtility.uplouadPic(node.picName);
            textnow.sendMessage("roey.sdomi",url);
            textnow.sendMessage("roey.sdomi",node.GPSLocation);
            list.remove(list.size()-1);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
