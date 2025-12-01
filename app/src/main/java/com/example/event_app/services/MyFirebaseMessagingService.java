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
 * MyFirebaseMessagingService - Handles push notifications and FCM token updates.
 *
 * <p>This service performs three core responsibilities:</p>
 *
 * <ol>
 *     <li><b>Receives push notifications</b> sent through Firebase Cloud Messaging (FCM).</li>
 *     <li><b>Displays notifications</b> to the user using Android’s Notification API.</li>
 *     <li><b>Manages FCM registration tokens</b> by saving them to Firestore and caching them
 *         locally when the user is logged out.</li>
 * </ol>
 *
 * <h3>Token Handling</h3>
 * <p>
 * FCM may generate a new token at any time (first install, reinstallation,
 * app restore, permissions change, or security rotation). This class:
 * </p>
 *
 * <ul>
 *     <li>Saves the token immediately if the user is logged in.</li>
 *     <li>Caches the token in SharedPreferences if the user is logged out.</li>
 *     <li>Provides {@link #checkAndSaveCachedToken(Context)} to sync cached tokens
 *         after login.</li>
 * </ul>
 *
 * <h3>Notification Handling</h3>
 * <p>Notifications may arrive in two forms:</p>
 * <ul>
 *     <li><b>Data messages</b> – custom key–value pairs in {@code remoteMessage.getData()}.</li>
 *     <li><b>Notification messages</b> – standard FCM notification payloads.</li>
 * </ul>
 * <p>This service handles both and displays a system notification using a
 * dedicated notification channel.</p>
 *
 * <h3>Used In:</h3>
 * <ul>
 *     <li>{@code FirebaseMessagingService} lifecycle</li>
 *     <li>Authentication workflows for syncing cached tokens</li>
 *     <li>Event update notifications for entrants and organizers</li>
 * </ul>
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "event_notifications";
    private static final String PREFS_NAME = "fcm_prefs";
    private static final String KEY_CACHED_TOKEN = "cached_fcm_token";

    /**
     * Called by Firebase whenever a new FCM token is generated.
     *
     * <p>This token uniquely identifies the device for push notifications.
     * The method attempts to save the token to Firestore if a user is logged in.
     * If no user is logged in, the token is cached locally for later save.</p>
     *
     * @param token the newly generated FCM registration token
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        //Attempt to save token AND cache it locally.
        saveFCMTokenToFirestore(token);
        cacheFCMToken(token);
    }

    /**
     * Called when the device receives a new FCM message.
     *
     * <p>Supports two types of incoming messages:</p>
     * <ul>
     *     <li><b>Data payload</b> (custom key–value pairs)</li>
     *     <li><b>Notification payload</b> (standard push notification)</li>
     * </ul>
     *
     * <p>The method extracts the title, message body, and optional eventId,
     * then displays a system notification.</p>
     *
     * @param remoteMessage the message received from FCM
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);


        // Check if message contains data payload
        if (remoteMessage.getData().size() > 0) {

            Map<String, String> data = remoteMessage.getData();
            String title = data.get("title");
            String message = data.get("message");
            String eventId = data.get("eventId");

            // Show notification
            showNotification(title, message, eventId);
        }

        // Check if message contains notification payload
        if (remoteMessage.getNotification() != null) {

            String title = remoteMessage.getNotification().getTitle();
            String message = remoteMessage.getNotification().getBody();

            showNotification(title, message, null);
        }
    }

    /**
     * Attempts to save the FCM token into the logged-in user's Firestore document.
     *
     * <p>If no user is logged in, the token is not lost—it is cached locally
     * so it can be saved immediately after login.</p>
     *
     * @param token the FCM token that should be saved
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
     * Stores the FCM token in SharedPreferences so it can be synced later
     * when the user logs in.
     *
     * <p>Passing {@code null} clears the cached token.</p>
     *
     * @param token the token to cache, or null to remove it
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
     * Checks if an FCM token was cached while the user was logged out and,
     * if so, saves it to Firestore for the currently logged-in user.
     *
     * <p>Call this method after login or at the start of MainActivity.</p>
     *
     * @param context context used to access SharedPreferences and Firestore
     */
    public static void checkAndSaveCachedToken(Context context) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            return;
        }

        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cachedToken = sharedPref.getString(KEY_CACHED_TOKEN, null);

        if (cachedToken != null) {
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
     * Builds and displays a system notification for the received push message.
     *
     * <p>If an event ID is included, it is passed to MainActivity so the
     * app can navigate to the corresponding event page.</p>
     *
     * @param title   The notification title
     * @param message The notification body text
     * @param eventId Optional event ID included in the notification payload
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
     * Creates the required notification channel for Android 8.0 (API 26+) and above.
     *
     * <p>This ensures that all event-related notifications appear with the correct
     * priority and settings.</p>
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