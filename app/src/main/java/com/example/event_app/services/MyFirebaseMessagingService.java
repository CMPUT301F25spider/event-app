package com.example.event_app.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences; // Added for caching
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.event_app.R;
import com.example.event_app.activities.entrant.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * MyFirebaseMessagingService - Handles FCM push notifications
 *
 * This service:
 * 1. Receives push notifications from Firebase
 * 2. Displays them to the user
 * 3. Stores FCM tokens for targeting
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "event_notifications";
    private static final String PREFS_NAME = "fcm_prefs";         // New constant for prefs name
    private static final String KEY_CACHED_TOKEN = "cached_fcm_token"; // New constant for token key

    /**
     * Called when a new FCM token is generated
     * This happens on first install and when token is refreshed
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM Token: " + token);

        // 💡 CRITICAL FIX: Attempt to save token AND cache it locally.
        saveFCMTokenToFirestore(token);
        cacheFCMToken(token);
    }

    /**
     * Called when a push notification is received
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        // Check if message contains data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data: " + remoteMessage.getData());

            Map<String, String> data = remoteMessage.getData();
            String title = data.get("title");
            String message = data.get("message");
            String eventId = data.get("eventId");

            // Show notification
            showNotification(title, message, eventId);
        }

        // Check if message contains notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message notification: " + remoteMessage.getNotification().getBody());

            String title = remoteMessage.getNotification().getTitle();
            String message = remoteMessage.getNotification().getBody();

            showNotification(title, message, null);
        }
    }

    /**
     * Save FCM token to Firestore
     *
     * This method saves the token to Firestore ONLY if the user is logged in.
     */
    private void saveFCMTokenToFirestore(String token) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.w(TAG, "No user logged in, token is being cached for later save.");
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        Map<String, Object> updates = new HashMap<>();
        // The field name 'fcmToken' must match the one used in NotificationService
        updates.put("fcmToken", token);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "FCM token saved for user: " + userId);
                    // Clear the local cache after successful save to Firestore
                    cacheFCMToken(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save FCM token", e);
                });
    }

    /**
     * 💡 NEW METHOD: Saves the token to SharedPreferences for persistence
     * until the user logs in. Use null to clear the cache.
     */
    private void cacheFCMToken(String token) {
        SharedPreferences sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        if (token != null) {
            editor.putString(KEY_CACHED_TOKEN, token);
            Log.d(TAG, "FCM token cached.");
        } else {
            editor.remove(KEY_CACHED_TOKEN);
            Log.d(TAG, "FCM token cache cleared.");
        }
        editor.apply();
    }

    /**
     * 💡 NEW PUBLIC METHOD: Call this from your login/on start activity
     * to check if a token was generated while the user was logged out.
     */
    public static void checkAndSaveCachedToken(Context context) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            return; // Only proceed if a user is logged in
        }

        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cachedToken = sharedPref.getString(KEY_CACHED_TOKEN, null);

        if (cachedToken != null) {
            Log.d(TAG, "Cached token found on login. Attempting save to Firestore.");

            // Note: Since this is a static method, we need to manually create
            // the logic for saving the token without relying on the service instance.
            String userId = auth.getCurrentUser().getUid();
            Map<String, Object> updates = new HashMap<>();
            updates.put("fcmToken", cachedToken);

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "FCM token saved from cache for user: " + userId);
                        // Clear the cache manually after successful save
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.remove(KEY_CACHED_TOKEN);
                        editor.apply();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save cached FCM token", e);
                    });
        }
    }

    /**
     * Display notification to user
     */
    private void showNotification(String title, String message, String eventId) {
        // Create notification channel (required for Android 8.0+)
        createNotificationChannel();

        // Create intent to open app when notification is tapped
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (eventId != null) {
            intent.putExtra("eventId", eventId);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // You'll need to add this icon
                .setContentTitle(title != null ? title : "LuckySpot")
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        // Show notification
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(0, builder.build());
        }
    }

    /**
     * Create notification channel (Android 8.0+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Event Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for event updates");

            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}