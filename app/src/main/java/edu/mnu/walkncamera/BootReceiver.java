package edu.mnu.walkncamera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent i = new Intent(context, BootService.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startForegroundService(i);
        }


    }
}