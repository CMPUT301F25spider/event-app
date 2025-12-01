package com.example.event_app.utils;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

/**
 * FCMTokenManager - Handles retrieval and storage of the user's Firebase Cloud Messaging (FCM) token.
 *
 * Features:
 * <ul>
 *     <li>Fetches the current device FCM token</li>
 *     <li>Saves the token in Firestore under the user's document</li>
 *     <li>Logs success or failure for debugging</li>
 * </ul>
 *
 * This ensures push notifications can be delivered to the correct device.
 */
public class FCMTokenManager {

    private static final String TAG = "FCMTokenManager";

    /**
     * Retrieves the current FCM device token and stores it in Firestore.
     *
     * Behavior:
     * <ul>
     *     <li>If no user is logged in, token retrieval is skipped</li>
     *     <li>Uses FirebaseMessaging.getToken() to obtain the FCM token</li>
     *     <li>On success, token is passed to saveFCMToken()</li>
     *     <li>Logs failures for debugging</li>
     * </ul>
     *
     * Should be called at user login or app startup.
     */
    public static void initializeFCMToken() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.w(TAG, "No user logged in, skipping FCM token");
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Failed to get FCM token", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);

                    // Save to Firestore
                    saveFCMToken(userId, token);
                });
    }

    /**
     * Saves the retrieved FCM token to the user's Firestore document.
     *
     * Firestore Field:
     * <ul>
     *     <li><b>fcmToken</b> — contains the latest device token for push notifications</li>
     * </ul>
     *
     * @param userId ID of the logged-in Firebase user
     * @param token  The FCM token retrieved from FirebaseMessaging
     */
    private static void saveFCMToken(String userId, String token) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", token);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "FCM token saved for user: " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save FCM token", e);
                });
    }
}