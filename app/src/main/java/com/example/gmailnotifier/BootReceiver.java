package com.example.gmailnotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {

            Log.d(TAG, "Boot or app update completed, starting service");

            // Check if service should be running based on saved preferences
            boolean shouldStart = context.getSharedPreferences("GmailNotifierPrefs", Context.MODE_PRIVATE)
                    .getBoolean("service_running", false);

            if (shouldStart) {
                Intent serviceIntent = new Intent(context, GmailMonitoringService.class);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }

                Log.d(TAG, "Service started after device boot or app update");
            }
        }
    }
}