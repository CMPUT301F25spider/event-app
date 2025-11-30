package com.example.event_app.utils;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

/**
 * FCMTokenManager - Manages FCM tokens
 */
public class FCMTokenManager {

    private static final String TAG = "FCMTokenManager";

    /**
     * Get current FCM token and save to Firestore
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
     * Save FCM token to user document
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