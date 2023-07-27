package com.ariel.balloonspy.reciever;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ariel.balloonspy.activity.MainActivity;

public class BootReciever extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent activityIntent = new Intent(context, MainActivity.class);
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(activityIntent);
        }
    }
}
