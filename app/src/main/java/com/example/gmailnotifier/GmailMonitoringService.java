


package com.example.gmailnotifier;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import android.media.AudioAttributes;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GmailMonitoringService extends Service {
    private static final String TAG = "GmailMonitorService";
    private static final int FOREGROUND_SERVICE_ID = 101;
    private static final String MONITORING_CHANNEL_ID = "gmail_monitoring";
    private static final String NEW_EMAILS_CHANNEL_ID = "gmail_new_emails";
    private static final long POLLING_INTERVAL_MS = 60000; // Poll every minute
    private static final String PREFS_NAME = "GmailNotifierPrefs";
    private static final String KEY_PRIMARY_EMAIL = "primary_email";
    private static final String KEY_SECONDARY_EMAIL = "secondary_email";
    private static final String KEY_LAST_CHECK_PREFIX = "last_check_timestamp_";


    private ExecutorService executorService;
    private Handler handler;
    private Map<String, Gmail> gmailServices = new HashMap<>();
    private Map<String, Long> lastCheckTimestamps = new HashMap<>(); // Store last check timestamp for each account
    private boolean isRunning = false;
    private Runnable pollingRunnable;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private SharedPreferences sharedPreferences;
    private List<String> monitoredEmails = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate started");

        // Initialize components
        executorService = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
        notificationManager = getSystemService(NotificationManager.class);
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Create notification channels
        createNotificationChannels();

        Log.d(TAG, "Service onCreate completed");
    }

//    private void createNotificationChannels() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            // Monitoring channel (low priority, silent)
//            NotificationChannel monitoringChannel = new NotificationChannel(
//                    MONITORING_CHANNEL_ID,
//                    "Gmail Monitoring Service",
//                    NotificationManager.IMPORTANCE_LOW
//            );
//            monitoringChannel.setDescription("Shows Gmail monitoring status");
//            monitoringChannel.enableLights(false);
//            monitoringChannel.enableVibration(false);
//            monitoringChannel.setSound(null, null);
//            notificationManager.createNotificationChannel(monitoringChannel);
//
//            // New emails channel (high priority, with sound and vibration)
//            NotificationChannel emailChannel = new NotificationChannel(
//                    NEW_EMAILS_CHANNEL_ID,
//                    "New Gmail Notifications",
//                    NotificationManager.IMPORTANCE_HIGH
//            );
//            emailChannel.setDescription("Alerts for new Gmail messages");
//            emailChannel.enableLights(true);
//            emailChannel.enableVibration(true);
//            notificationManager.createNotificationChannel(emailChannel);
//        }
//    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Monitoring channel (low priority, silent)
            NotificationChannel monitoringChannel = new NotificationChannel(
                    MONITORING_CHANNEL_ID,
                    "Gmail Monitoring Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            monitoringChannel.setDescription("Shows Gmail monitoring status");
            monitoringChannel.enableLights(false);
            monitoringChannel.enableVibration(false);
            monitoringChannel.setSound(null, null);
            notificationManager.createNotificationChannel(monitoringChannel);

            // New emails channel (high priority, with custom sound)
            NotificationChannel emailChannel = new NotificationChannel(
                    NEW_EMAILS_CHANNEL_ID,
                    "New Gmail Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            emailChannel.setDescription("Alerts for new Gmail messages");
            emailChannel.enableLights(true);
            emailChannel.enableVibration(true);

            // Set custom sound for email notification channel
            Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.notify);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            emailChannel.setSound(soundUri, audioAttributes);

            notificationManager.createNotificationChannel(emailChannel);
        }
    }

    private void initializeGmailServices() {
        // Clear previous services
        gmailServices.clear();
        monitoredEmails.clear();
        lastCheckTimestamps.clear(); // Clear previous timestamps

        // Get account emails from preferences
        String primaryEmail = sharedPreferences.getString(KEY_PRIMARY_EMAIL, null);
        String secondaryEmail = sharedPreferences.getString(KEY_SECONDARY_EMAIL, null);

        // Current time for initial timestamp
        long currentTime = System.currentTimeMillis();

        // Initialize services for available accounts
        if (primaryEmail != null) {
            initializeGmailServiceForAccount(primaryEmail);
            monitoredEmails.add(primaryEmail);

            // Get last saved timestamp or use current time as initial value
            long lastCheck = sharedPreferences.getLong(KEY_LAST_CHECK_PREFIX + primaryEmail, currentTime);
            lastCheckTimestamps.put(primaryEmail, lastCheck);
        }

        if (secondaryEmail != null) {
            initializeGmailServiceForAccount(secondaryEmail);
            monitoredEmails.add(secondaryEmail);

            // Get last saved timestamp or use current time as initial value
            long lastCheck = sharedPreferences.getLong(KEY_LAST_CHECK_PREFIX + secondaryEmail, currentTime);
            lastCheckTimestamps.put(secondaryEmail, lastCheck);
        }

        Log.d(TAG, "Initialized Gmail services for " + monitoredEmails.size() + " accounts");
    }

    private void initializeGmailServiceForAccount(String email) {
        try {
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                            this, Collections.singletonList("https://www.googleapis.com/auth/gmail.readonly"))
                    .setBackOff(new ExponentialBackOff());

            // Set the account by email
            credential.setSelectedAccountName(email);

            Gmail gmailService = new Gmail.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName("Gmail Notifier")
                    .build();

            gmailServices.put(email, gmailService);
            showToast("Gmail monitoring initialized for: " + email);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Gmail service for " + email, e);
            showToast("Error initializing Gmail for " + email + ": " + e.getMessage());
        }
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand started");

        // Start as foreground service with a basic notification
        Notification notification = createBasicNotification("Gmail Notifier starting...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(FOREGROUND_SERVICE_ID, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        }

        // Initialize Gmail services for all accounts
        initializeGmailServices();

        StringBuilder accountsText = new StringBuilder("Monitoring:");
        for (String email : monitoredEmails) {
            accountsText.append(" ").append(email);
        }

        // Update with detailed foreground notification
        Notification betterNotification = createForegroundNotification(accountsText.toString());
        notificationManager.notify(FOREGROUND_SERVICE_ID, betterNotification);

        showToast(accountsText.toString());

        if (!isRunning && !monitoredEmails.isEmpty()) {
            isRunning = true;
            startPollingEmails();
        } else if (monitoredEmails.isEmpty()) {
            showToast("No accounts to monitor. Please sign in first.");
            stopSelf();
        }

        Log.d(TAG, "Service onStartCommand completed");
        return START_STICKY;
    }

    private Notification createBasicNotification(String text) {
        return new NotificationCompat.Builder(this, MONITORING_CHANNEL_ID)
                .setContentTitle("Gmail Notifier")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private Notification createForegroundNotification(String accountInfo) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        notificationBuilder = new NotificationCompat.Builder(this, MONITORING_CHANNEL_ID)
                .setContentTitle("Gmail Notifier Active")
                .setContentText(accountInfo + " - Waiting for next check")
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET) // Hidden from lock screen
                .setShowWhen(false);

        return notificationBuilder.build();
    }

    private void startPollingEmails() {
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                executorService.execute(() -> {
                    updateNotification("Checking for new emails...");
                    checkForNewEmails();
                    if (isRunning) {
                        updateNotification("Waiting for next check...");
                        handler.postDelayed(this, POLLING_INTERVAL_MS);
                    }
                });
            }
        };
        handler.post(pollingRunnable);
    }

    private void checkForNewEmails() {
        for (String email : monitoredEmails) {
            Gmail gmailService = gmailServices.get(email);
            if (gmailService == null) continue;

            try {
                // Get the timestamp for this account (in seconds for Gmail API)
                long lastCheckMs = lastCheckTimestamps.get(email);
                long lastCheckSeconds = lastCheckMs / 1000;

                // Create query to find only emails after the last check time
                String query = "in:inbox after:" + lastCheckSeconds;
                List<Message> messages = listMessages(gmailService, query, 20);

                for (Message message : messages) {
                    Message fullMessage = gmailService.users().messages().get("me", message.getId()).execute();
                    String subject = getSubject(fullMessage);
                    String sender = getSender(fullMessage);
                    showEmailNotification(email, message.getId(), subject, sender);
                }

                // Update last check time to now
                long currentTime = System.currentTimeMillis();
                lastCheckTimestamps.put(email, currentTime);

                // Save this timestamp to preferences for persistence
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putLong(KEY_LAST_CHECK_PREFIX + email, currentTime);
                editor.apply();

                Log.d(TAG, "Checked " + email + ", found " + messages.size() + " new messages");

            } catch (IOException e) {
                Log.e(TAG, "Error checking for new emails for " + email, e);
            }
        }
    }

    private List<Message> listMessages(Gmail gmailService, String query, int maxResults) throws IOException {
        Gmail.Users.Messages.List request = gmailService.users().messages().list("me");
        request.setQ(query);
        request.setMaxResults((long) maxResults);

        ListMessagesResponse response = request.execute();
        return response.getMessages() != null ? response.getMessages() : new ArrayList<>();
    }

    private String getSubject(Message message) {
        if (message.getPayload() == null || message.getPayload().getHeaders() == null) {
            return "(No subject)";
        }

        return message.getPayload().getHeaders().stream()
                .filter(header -> "Subject".equals(header.getName()))
                .findFirst()
                .map(MessagePartHeader::getValue)
                .orElse("(No subject)");
    }

    private String getSender(Message message) {
        if (message.getPayload() == null || message.getPayload().getHeaders() == null) {
            return "Unknown sender";
        }

        return message.getPayload().getHeaders().stream()
                .filter(header -> "From".equals(header.getName()))
                .findFirst()
                .map(MessagePartHeader::getValue)
                .orElse("Unknown sender");
    }

    private void showEmailNotification(String accountEmail, String messageId, String subject, String sender) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, messageId.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE);

        // Include account email in notification to distinguish between accounts
        String notificationTitle = "[" + accountEmail + "] " + sender;

        Notification notification = new NotificationCompat.Builder(this, NEW_EMAILS_CHANNEL_ID)
                .setContentTitle(notificationTitle)
                .setContentText(subject)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_EMAIL)
                .setDefaults(NotificationCompat.DEFAULT_ALL) // Sound, vibration, lights
                .build();

        // Use a combination of email and message ID for unique notification ID
        int notificationId = (accountEmail + messageId).hashCode();
        notificationManager.notify(notificationId, notification);
    }

//    private void showEmailNotification(String accountEmail, String messageId, String subject, String sender) {
//        Intent intent = new Intent(this, MainActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(
//                this, messageId.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE);
//
//        // Include account email in notification to distinguish between accounts
//        String notificationTitle = "[" + accountEmail + "] " + sender;
//
//        // Set custom notification sound
//        Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.notify);
//
//        // Create AudioAttributes for the sound
//        AudioAttributes audioAttributes = new AudioAttributes.Builder()
//                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
//                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
//                .build();
//
//        Notification notification = new NotificationCompat.Builder(this, NEW_EMAILS_CHANNEL_ID)
//                .setContentTitle(notificationTitle)
//                .setContentText(subject)
//                .setSmallIcon(android.R.drawable.ic_dialog_email)
//                .setContentIntent(pendingIntent)
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setAutoCancel(true)
//                .setCategory(NotificationCompat.CATEGORY_EMAIL)
//                .setSound(soundUri) // Set custom sound
//                .build();
//
//        // Use a combination of email and message ID for unique notification ID
//        int notificationId = (accountEmail + messageId).hashCode();
//        notificationManager.notify(notificationId, notification);
//    }
  private void updateNotification(String status) {
        if (notificationBuilder != null && notificationManager != null) {
            StringBuilder accountsText = new StringBuilder("Monitoring:");
            for (String email : monitoredEmails) {
                accountsText.append(" ").append(email);
            }

            notificationBuilder.setContentText(accountsText + " - " + status);
            notificationManager.notify(FOREGROUND_SERVICE_ID, notificationBuilder.build());
        }
    }

    private void showToast(String message) {
        handler.post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroy() {
        showToast("Stopping Gmail monitoring service");
        isRunning = false;
        if (handler != null && pollingRunnable != null) {
            handler.removeCallbacks(pollingRunnable);
        }
        if (executorService != null) {
            executorService.shutdown();
        }
        Log.d(TAG, "Service destroyed");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}




