package com.example.gmailnotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationManager;

public class NotificationClickReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Extract the notification ID from the intent
        int notificationId = intent.getIntExtra("notification_id", -1);
        if (notificationId != -1) {
            // Get the notification manager and cancel the notification
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
        }
    }
}