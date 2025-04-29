//package com.example.gmailnotifier;
//import android.Manifest;
//import android.app.ActivityManager;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.content.pm.PackageManager;
//import android.os.Build;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.TextView;
//import android.widget.Toast;
//import android.media.AudioAttributes;
//import android.net.Uri;
//
//// For GmailMonitoringService.java - add these imports if not already present
//import android.app.PendingIntent;
//import android.content.Intent;
//import android.net.Uri;
//import androidx.core.app.NotificationCompat;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//
//import com.google.android.gms.auth.api.signin.GoogleSignIn;
//import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
//import com.google.android.gms.auth.api.signin.GoogleSignInClient;
//import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
//import com.google.android.gms.common.api.ApiException;
//import com.google.android.gms.common.api.Scope;
//import com.google.android.gms.tasks.Task;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class MainActivity extends AppCompatActivity {
//    private static final String TAG = "MainActivity";
//    private static final int RC_SIGN_IN_PRIMARY = 9001;
//    private static final int RC_SIGN_IN_SECONDARY = 9002;
//    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
//    private static final String NOTIFICATION_CHANNEL_ID = "gmail_notifications";
//    private static final String PREFS_NAME = "GmailNotifierPrefs";
//    private static final String KEY_SERVICE_RUNNING = "service_running";
//    private static final String KEY_PRIMARY_EMAIL = "primary_email";
//    private static final String KEY_SECONDARY_EMAIL = "secondary_email";
//
//    private GoogleSignInClient mGoogleSignInClient;
//    private TextView primaryStatusText;
//    private TextView secondaryStatusText;
//    private Button signInPrimaryButton;
//    private Button signInSecondaryButton;
//    private Button signOutPrimaryButton;
//    private Button signOutSecondaryButton;
//    private Button startServiceButton;
//    private Button stopServiceButton;
//    private SharedPreferences sharedPreferences;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        primaryStatusText = findViewById(R.id.primary_status_text);
//        secondaryStatusText = findViewById(R.id.secondary_status_text);
//        signInPrimaryButton = findViewById(R.id.sign_in_primary_button);
//        signInSecondaryButton = findViewById(R.id.sign_in_secondary_button);
//        signOutPrimaryButton = findViewById(R.id.sign_out_primary_button);
//        signOutSecondaryButton = findViewById(R.id.sign_out_secondary_button);
//        startServiceButton = findViewById(R.id.start_service_button);
//        stopServiceButton = findViewById(R.id.stop_service_button);
//
//        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
//
//        // Configure sign-in to request the user's ID, email address, and Gmail readonly permission
//        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestEmail()
//                .requestScopes(new Scope("https://www.googleapis.com/auth/gmail.readonly"))
//                .build();
//
//        // Build a GoogleSignInClient with the options
//        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
//
//        signInPrimaryButton.setOnClickListener(v -> signInPrimary());
//        signInSecondaryButton.setOnClickListener(v -> signInSecondary());
//
//        signOutPrimaryButton.setOnClickListener(v -> {
//            Toast.makeText(MainActivity.this, "Signing out primary account...", Toast.LENGTH_SHORT).show();
//            signOutPrimary();
//        });
//
//        signOutSecondaryButton.setOnClickListener(v -> {
//            Toast.makeText(MainActivity.this, "Signing out secondary account...", Toast.LENGTH_SHORT).show();
//            signOutSecondary();
//        });
//
//        startServiceButton.setOnClickListener(v -> {
//            Toast.makeText(MainActivity.this, "Starting monitoring service...", Toast.LENGTH_SHORT).show();
//            startGmailMonitoringService();
//        });
//
//        stopServiceButton.setOnClickListener(v -> {
//            Toast.makeText(MainActivity.this, "Stopping monitoring service...", Toast.LENGTH_SHORT).show();
//            stopGmailMonitoringService();
//        });
//
//        createNotificationChannel();
//
//        // Check and update the actual service running state
//        boolean isServiceActuallyRunning = isServiceRunning(GmailMonitoringService.class);
//        sharedPreferences.edit().putBoolean(KEY_SERVICE_RUNNING, isServiceActuallyRunning).apply();
//
//        // Only start the service if it was previously running but is not currently running
//        boolean wasServiceRunning = sharedPreferences.getBoolean(KEY_SERVICE_RUNNING, false);
//        if (wasServiceRunning && !isServiceActuallyRunning) {
//            startGmailMonitoringService();
//        }
//
//        updateUI();
//    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        // Check if service is actually running and update UI accordingly
//        boolean isServiceActuallyRunning = isServiceRunning(GmailMonitoringService.class);
//        sharedPreferences.edit().putBoolean(KEY_SERVICE_RUNNING, isServiceActuallyRunning).apply();
//        updateUI();
//    }
//
//    /**
//     * Check if a service is currently running
//     */
//    private boolean isServiceRunning(Class<?> serviceClass) {
//        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//            if (serviceClass.getName().equals(service.service.getClassName())) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private void signInPrimary() {
//        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
//        startActivityForResult(signInIntent, RC_SIGN_IN_PRIMARY);
//    }
//
//    private void signInSecondary() {
//        // Force account chooser to appear for secondary account
//        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
//            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
//            startActivityForResult(signInIntent, RC_SIGN_IN_SECONDARY);
//        });
//    }
//
//    private void signOutPrimary() {
//        String primaryEmail = sharedPreferences.getString(KEY_PRIMARY_EMAIL, null);
//        if (primaryEmail != null) {
//            mGoogleSignInClient.revokeAccess().addOnCompleteListener(task -> {
//                sharedPreferences.edit().remove(KEY_PRIMARY_EMAIL).apply();
//                updateUI();
//                Toast.makeText(this, "Primary account signed out", Toast.LENGTH_SHORT).show();
//            });
//        }
//    }
//
//    private void signOutSecondary() {
//        String secondaryEmail = sharedPreferences.getString(KEY_SECONDARY_EMAIL, null);
//        if (secondaryEmail != null) {
//            mGoogleSignInClient.revokeAccess().addOnCompleteListener(task -> {
//                sharedPreferences.edit().remove(KEY_SECONDARY_EMAIL).apply();
//                updateUI();
//                Toast.makeText(this, "Secondary account signed out", Toast.LENGTH_SHORT).show();
//            });
//        }
//    }
//
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == RC_SIGN_IN_PRIMARY) {
//            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
//            handleSignInResult(task, true);
//        } else if (requestCode == RC_SIGN_IN_SECONDARY) {
//            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
//            handleSignInResult(task, false);
//        }
//    }
//
//
//    private void handleSignInResult(Task<GoogleSignInAccount> completedTask, boolean isPrimary) {
//        try {
//            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
//            String email = account.getEmail();
//
//            // Save account email to preferences
//            if (isPrimary) {
//                sharedPreferences.edit().putString(KEY_PRIMARY_EMAIL, email).apply();
//                Toast.makeText(this, "Primary account signed in: " + email, Toast.LENGTH_SHORT).show();
//            } else {
//                sharedPreferences.edit().putString(KEY_SECONDARY_EMAIL, email).apply();
//                Toast.makeText(this, "Secondary account signed in: " + email, Toast.LENGTH_SHORT).show();
//            }
//
//            updateUI();
//        } catch (ApiException e) {
//            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
//            Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private void updateUI() {
//        String primaryEmail = sharedPreferences.getString(KEY_PRIMARY_EMAIL, null);
//        String secondaryEmail = sharedPreferences.getString(KEY_SECONDARY_EMAIL, null);
//        boolean isServiceActuallyRunning = isServiceRunning(GmailMonitoringService.class);
//
//        // Update primary account UI
//        if (primaryEmail != null) {
//            primaryStatusText.setText("Primary: " + primaryEmail);
//            signInPrimaryButton.setVisibility(View.GONE);
//            signOutPrimaryButton.setVisibility(View.VISIBLE);
//        } else {
//            primaryStatusText.setText("Primary account: Not signed in");
//            signInPrimaryButton.setVisibility(View.VISIBLE);
//            signOutPrimaryButton.setVisibility(View.GONE);
//        }
//
//        // Update secondary account UI
//        if (secondaryEmail != null) {
//            secondaryStatusText.setText("Secondary: " + secondaryEmail);
//            signInSecondaryButton.setVisibility(View.GONE);
//            signOutSecondaryButton.setVisibility(View.VISIBLE);
//        } else {
//            secondaryStatusText.setText("Secondary account: Not signed in");
//            signInSecondaryButton.setVisibility(View.VISIBLE);
//            signOutSecondaryButton.setVisibility(View.GONE);
//        }
//
//        // Update service buttons visibility
//        boolean hasAtLeastOneAccount = (primaryEmail != null || secondaryEmail != null);
//
//        // Show start button only if there's at least one account and service isn't running
//        startServiceButton.setVisibility(hasAtLeastOneAccount && !isServiceActuallyRunning ?
//                View.VISIBLE : View.GONE);
//
//        // Show stop button only if there's at least one account and service is running
//        stopServiceButton.setVisibility(hasAtLeastOneAccount && isServiceActuallyRunning ?
//                View.VISIBLE : View.GONE);
//
//        // Update service status text
//        if (isServiceActuallyRunning) {
//            String monitoringText = "Monitoring:";
//            if (primaryEmail != null) monitoringText += " " + primaryEmail;
//            if (secondaryEmail != null) monitoringText += (primaryEmail != null ? " and" : "") + " " + secondaryEmail;
//            Toast.makeText(this, monitoringText, Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private void startGmailMonitoringService() {
//        // Check if the service is already running
//        if (isServiceRunning(GmailMonitoringService.class)) {
//            Log.d(TAG, "Service is already running, not starting again");
//            Toast.makeText(this, "Monitoring service is already running", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
//                return;
//            }
//        }
//
//        try {
//            Intent serviceIntent = new Intent(this, GmailMonitoringService.class);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                startForegroundService(serviceIntent);
//                Log.d(TAG, "Starting foreground service");
//            } else {
//                Log.d(TAG, "Starting service");
//                startService(serviceIntent);
//            }
//
//            // Save service running state
//            sharedPreferences.edit().putBoolean(KEY_SERVICE_RUNNING, true).apply();
//            Toast.makeText(this, "Monitoring service started", Toast.LENGTH_SHORT).show();
//
//            updateUI();
//        } catch (Exception e) {
//            Log.e(TAG, "Error starting service", e);
//            Toast.makeText(this, "Error starting service: " + e.getMessage(), Toast.LENGTH_LONG).show();
//        }
//    }
//
//    private void stopGmailMonitoringService() {
//        // Only attempt to stop if service is actually running
//        if (!isServiceRunning(GmailMonitoringService.class)) {
//            Log.d(TAG, "Service is not running, no need to stop");
//            Toast.makeText(this, "Monitoring service is not running", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        Intent serviceIntent = new Intent(this, GmailMonitoringService.class);
//        stopService(serviceIntent);
//
//        // Update saved state
//        sharedPreferences.edit().putBoolean(KEY_SERVICE_RUNNING, false).apply();
//        Toast.makeText(this, "Monitoring service stopped", Toast.LENGTH_SHORT).show();
//
//        updateUI();
//    }
//
//    private void createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            CharSequence name = "Gmail Notifications";
//            String description = "Notifications for new Gmail messages";
//            int importance = NotificationManager.IMPORTANCE_HIGH;
//            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
//            channel.setDescription(description);
//
//            NotificationManager notificationManager = getSystemService(NotificationManager.class);
//            notificationManager.createNotificationChannel(channel);
//        }
//    }
//}


package com.example.gmailnotifier;

import android.Manifest;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int RC_SIGN_IN_PRIMARY = 9001;
    private static final int RC_SIGN_IN_SECONDARY = 9002;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    private static final String NOTIFICATION_CHANNEL_ID = "gmail_notifications";
    private static final String PREFS_NAME = "GmailNotifierPrefs";
    private static final String KEY_SERVICE_RUNNING = "service_running";
    private static final String KEY_PRIMARY_EMAIL = "primary_email";
    private static final String KEY_SECONDARY_EMAIL = "secondary_email";

    private GoogleSignInClient mGoogleSignInClient;
    private TextView primaryStatusText;
    private TextView secondaryStatusText;
    private Button signInPrimaryButton;
    private Button signInSecondaryButton;
    private Button signOutPrimaryButton;
    private Button signOutSecondaryButton;
    private Button startServiceButton;
    private Button stopServiceButton;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        primaryStatusText = findViewById(R.id.primary_status_text);
        secondaryStatusText = findViewById(R.id.secondary_status_text);
        signInPrimaryButton = findViewById(R.id.sign_in_primary_button);
        signInSecondaryButton = findViewById(R.id.sign_in_secondary_button);
        signOutPrimaryButton = findViewById(R.id.sign_out_primary_button);
        signOutSecondaryButton = findViewById(R.id.sign_out_secondary_button);
        startServiceButton = findViewById(R.id.start_service_button);
        stopServiceButton = findViewById(R.id.stop_service_button);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Configure sign-in to request the user's ID, email address, and Gmail readonly permission
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/gmail.readonly"))
                .build();

        // Build a GoogleSignInClient with the options
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signInPrimaryButton.setOnClickListener(v -> signInPrimary());
        signInSecondaryButton.setOnClickListener(v -> signInSecondary());

        signOutPrimaryButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Signing out primary account...", Toast.LENGTH_SHORT).show();
            signOutPrimary();
        });

        signOutSecondaryButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Signing out secondary account...", Toast.LENGTH_SHORT).show();
            signOutSecondary();
        });

        startServiceButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Starting monitoring service...", Toast.LENGTH_SHORT).show();
            startGmailMonitoringService();
        });

        stopServiceButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Stopping monitoring service...", Toast.LENGTH_SHORT).show();
            stopGmailMonitoringService();
        });

        createNotificationChannel();

        // Check and update the actual service running state
        boolean isServiceActuallyRunning = isServiceRunning(GmailMonitoringService.class);
        sharedPreferences.edit().putBoolean(KEY_SERVICE_RUNNING, isServiceActuallyRunning).apply();

        // Only start the service if it was previously running but is not currently running
        boolean wasServiceRunning = sharedPreferences.getBoolean(KEY_SERVICE_RUNNING, false);
        if (wasServiceRunning && !isServiceActuallyRunning) {
            startGmailMonitoringService();
        }

        updateUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if service is actually running and update UI accordingly
        boolean isServiceActuallyRunning = isServiceRunning(GmailMonitoringService.class);
        sharedPreferences.edit().putBoolean(KEY_SERVICE_RUNNING, isServiceActuallyRunning).apply();
        updateUI();
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void signInPrimary() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN_PRIMARY);
    }

    private void signInSecondary() {
        // Force account chooser to appear for secondary account
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN_SECONDARY);
        });
    }

    private void signOutPrimary() {
        String primaryEmail = sharedPreferences.getString(KEY_PRIMARY_EMAIL, null);
        if (primaryEmail != null) {
            mGoogleSignInClient.revokeAccess().addOnCompleteListener(task -> {
                sharedPreferences.edit().remove(KEY_PRIMARY_EMAIL).apply();
                updateUI();
                Toast.makeText(this, "Primary account signed out", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void signOutSecondary() {
        String secondaryEmail = sharedPreferences.getString(KEY_SECONDARY_EMAIL, null);
        if (secondaryEmail != null) {
            mGoogleSignInClient.revokeAccess().addOnCompleteListener(task -> {
                sharedPreferences.edit().remove(KEY_SECONDARY_EMAIL).apply();
                updateUI();
                Toast.makeText(this, "Secondary account signed out", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN_PRIMARY) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task, true);
        } else if (requestCode == RC_SIGN_IN_SECONDARY) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task, false);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask, boolean isPrimary) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String email = account.getEmail();

            // Save account email to preferences
            if (isPrimary) {
                sharedPreferences.edit().putString(KEY_PRIMARY_EMAIL, email).apply();
                Toast.makeText(this, "Primary account signed in: " + email, Toast.LENGTH_SHORT).show();
            } else {
                sharedPreferences.edit().putString(KEY_SECONDARY_EMAIL, email).apply();
                Toast.makeText(this, "Secondary account signed in: " + email, Toast.LENGTH_SHORT).show();
            }

            updateUI();
        } catch (ApiException e) {
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI() {
        String primaryEmail = sharedPreferences.getString(KEY_PRIMARY_EMAIL, null);
        String secondaryEmail = sharedPreferences.getString(KEY_SECONDARY_EMAIL, null);
        boolean isServiceActuallyRunning = isServiceRunning(GmailMonitoringService.class);

        // Update primary account UI
        if (primaryEmail != null) {
            primaryStatusText.setText("Primary: " + primaryEmail);
            signInPrimaryButton.setVisibility(View.GONE);
            signOutPrimaryButton.setVisibility(View.VISIBLE);
        } else {
            primaryStatusText.setText("Primary account: Not signed in");
            signInPrimaryButton.setVisibility(View.VISIBLE);
            signOutPrimaryButton.setVisibility(View.GONE);
        }

        // Update secondary account UI
        if (secondaryEmail != null) {
            secondaryStatusText.setText("Secondary: " + secondaryEmail);
            signInSecondaryButton.setVisibility(View.GONE);
            signOutSecondaryButton.setVisibility(View.VISIBLE);
        } else {
            secondaryStatusText.setText("Secondary account: Not signed in");
            signInSecondaryButton.setVisibility(View.VISIBLE);
            signOutSecondaryButton.setVisibility(View.GONE);
        }

        // Update service buttons visibility
        boolean hasAtLeastOneAccount = (primaryEmail != null || secondaryEmail != null);

        // Show start button only if there's at least one account and service isn't running
        startServiceButton.setVisibility(hasAtLeastOneAccount && !isServiceActuallyRunning ?
                View.VISIBLE : View.GONE);

        // Show stop button only if there's at least one account and service is running
        stopServiceButton.setVisibility(hasAtLeastOneAccount && isServiceActuallyRunning ?
                View.VISIBLE : View.GONE);

        // Update service status text
        if (isServiceActuallyRunning) {
            String monitoringText = "Monitoring:";
            if (primaryEmail != null) monitoringText += " " + primaryEmail;
            if (secondaryEmail != null) monitoringText += (primaryEmail != null ? " and" : "") + " " + secondaryEmail;
            Toast.makeText(this, monitoringText, Toast.LENGTH_SHORT).show();
        }
    }

    private void startGmailMonitoringService() {
        // Check if the service is already running
        if (isServiceRunning(GmailMonitoringService.class)) {
            Log.d(TAG, "Service is already running, not starting again");
            Toast.makeText(this, "Monitoring service is already running", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
                return;
            }
        }

        try {
            Intent serviceIntent = new Intent(this, GmailMonitoringService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
                Log.d(TAG, "Starting foreground service");
            } else {
                Log.d(TAG, "Starting service");
                startService(serviceIntent);
            }

            // Save service running state
            sharedPreferences.edit().putBoolean(KEY_SERVICE_RUNNING, true).apply();
            Toast.makeText(this, "Monitoring service started", Toast.LENGTH_SHORT).show();

            updateUI();
        } catch (Exception e) {
            Log.e(TAG, "Error starting service", e);
            Toast.makeText(this, "Error starting service: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopGmailMonitoringService() {
        // Only attempt to stop if service is actually running
        if (!isServiceRunning(GmailMonitoringService.class)) {
            Log.d(TAG, "Service is not running, no need to stop");
            Toast.makeText(this, "Monitoring service is not running", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent serviceIntent = new Intent(this, GmailMonitoringService.class);
        stopService(serviceIntent);

        // Update saved state
        sharedPreferences.edit().putBoolean(KEY_SERVICE_RUNNING, false).apply();
        Toast.makeText(this, "Monitoring service stopped", Toast.LENGTH_SHORT).show();

        updateUI();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Gmail Notifications";
            String description = "Notifications for new Gmail messages";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}