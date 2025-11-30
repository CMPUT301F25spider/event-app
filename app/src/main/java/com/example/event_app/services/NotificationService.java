package com.example.event_app.services;

import android.util.Log;

import com.example.event_app.models.Notification;
import com.example.event_app.models.NotificationLog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NotificationService - Handles all notification operations
 */
public class NotificationService {

    private static final String TAG = "NotificationService";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";
    private static final String COLLECTION_NOTIFICATION_LOGS = "notification_logs";

    private final FirebaseFirestore db;

    public NotificationService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void sendNotification(String userId, String eventId, String eventName,
                                 String type, String title, String message,
                                 NotificationCallback callback) {

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean notificationsEnabled = documentSnapshot.getBoolean("notificationsEnabled");

                        if (notificationsEnabled != null && !notificationsEnabled) {
                            // Log only the block event as a warning or info
                            Log.w(TAG, "Notifications disabled for user: " + userId + ". Blocking send.");

                            logNotification(null, "System", userId,
                                    documentSnapshot.getString("name"),
                                    eventId, eventName, type, title, message, "blocked_user_preference");

                            if (callback != null) {
                                callback.onSuccess();
                            }
                            return;
                        }
                    }

                    createAndSendNotification(userId, eventId, eventName, type, title, message, callback);
                })
                .addOnFailureListener(e -> {
                    // Keep this warning as it indicates a DB read failure but we proceed
                    Log.w(TAG, "Could not check notification preference, sending anyway", e);
                    createAndSendNotification(userId, eventId, eventName, type, title, message, callback);
                });
    }

    private void createAndSendNotification(String userId, String eventId, String eventName,
                                           String type, String title, String message,
                                           NotificationCallback callback) {
        Notification notification = new Notification(userId, eventId, eventName, type, title, message);

        String notificationId = db.collection(COLLECTION_NOTIFICATIONS).document().getId();
        notification.setNotificationId(notificationId);

        db.collection(COLLECTION_NOTIFICATIONS)
                .document(notificationId)
                .set(notification)
                .addOnSuccessListener(aVoid -> {
                    // Keep a success log for the database write
                    Log.i(TAG, "Notification DB record created for user: " + userId + " (ID: " + notificationId + ")");

                    logNotificationAfterSend(notificationId, userId, eventId, eventName,
                            type, title, message, "sent");

                    sendFCMPushNotification(userId, title, message, eventId, eventName);

                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    // Keep this error log
                    Log.e(TAG, "Failed to create notification DB record", e);

                    logNotification(null, "System", userId, null,
                            eventId, eventName, type, title, message, "failed");

                    if (callback != null) {
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    private void sendFCMPushNotification(String userId, String title, String message,
                                         String eventId, String eventName) {

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fcmToken = documentSnapshot.getString("fcmToken");

                        if (fcmToken != null && !fcmToken.isEmpty()) {
                            // Keep an info log that the function call is being made
                            Log.i(TAG, "FCM token found for " + userId + ", initiating Cloud Function call.");
                            callCloudFunctionToSendFCM(fcmToken, title, message, eventId);

                        } else {
                            // Keep this warning log
                            Log.w(TAG, "No FCM token for user: " + userId);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Keep this error log
                    Log.e(TAG, "Failed to get FCM token for user: " + userId, e);
                });
    }

    /**
     * Ensure all values are explicitly converted to String
     */
    private void callCloudFunctionToSendFCM(String token, String title, String message, String eventId) {
        FirebaseFunctions functions = FirebaseFunctions.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("token", String.valueOf(token));
        data.put("title", String.valueOf(title));
        data.put("message", String.valueOf(message));
        data.put("eventId", eventId != null ? String.valueOf(eventId) : "");

        functions
                .getHttpsCallable("sendFCMNotification")
                .call(data)
                .addOnSuccessListener(result -> {
                    // Keep a single info log for push success
                    Log.i(TAG, "Cloud Function 'sendFCMNotification' called successfully.");
                })
                .addOnFailureListener(e -> {
                    // Keep detailed error logging
                    Log.e(TAG, "Cloud Function call failed", e);
                });
    }

    private void logNotificationAfterSend(String notificationId, String recipientId,
                                          String eventId, String eventName,
                                          String type, String title, String message,
                                          String status) {
        db.collection("users").document(recipientId).get()
                .addOnSuccessListener(userDoc -> {
                    String recipientName = userDoc.exists() ? userDoc.getString("name") : "Unknown User";

                    logNotification(null, "System", recipientId, recipientName,
                            eventId, eventName, type, title, message, status, notificationId);
                })
                .addOnFailureListener(e -> {
                    logNotification(null, "System", recipientId, "Unknown User",
                            eventId, eventName, type, title, message, status, notificationId);
                });
    }

    private void logNotification(String senderId, String senderName,
                                 String recipientId, String recipientName,
                                 String eventId, String eventName,
                                 String type, String title, String message,
                                 String status) {
        logNotification(senderId, senderName, recipientId, recipientName,
                eventId, eventName, type, title, message, status, null);
    }

    private void logNotification(String senderId, String senderName,
                                 String recipientId, String recipientName,
                                 String eventId, String eventName,
                                 String type, String title, String message,
                                 String status, String notificationId) {

        String logId = db.collection(COLLECTION_NOTIFICATION_LOGS).document().getId();

        NotificationLog log = new NotificationLog(
                logId,
                notificationId,
                senderId != null ? senderId : "system",
                senderName != null ? senderName : "System",
                recipientId,
                recipientName != null ? recipientName : "Unknown",
                eventId,
                eventName,
                type,
                title,
                message,
                new Date(),
                status
        );

        db.collection(COLLECTION_NOTIFICATION_LOGS)
                .document(logId)
                .set(log)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Notification logged for audit: " + logId + " (Status: " + status + ")");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error logging notification for audit", e);
                });
    }

    public void sendBulkNotifications(List<String> userIds, String eventId, String eventName,
                                      String type, String title, String message,
                                      BulkNotificationCallback callback) {
        // ... bulk send logic remains the same ...
        int total = userIds.size();
        int[] completed = {0};
        int[] failed = {0};

        for (String userId : userIds) {
            sendNotification(userId, eventId, eventName, type, title, message, new NotificationCallback() {
                @Override
                public void onSuccess() {
                    completed[0]++;
                    checkCompletion();
                }

                @Override
                public void onFailure(String error) {
                    completed[0]++;
                    failed[0]++;
                    checkCompletion();
                }

                private void checkCompletion() {
                    if (completed[0] == total) {
                        if (callback != null) {
                            // Log the final bulk operation result
                            Log.i(TAG, "Bulk notification send complete. Successes: " + (total - failed[0]) + ", Failures: " + failed[0]);
                            callback.onComplete(total - failed[0], failed[0]);
                        }
                    }
                }
            });
        }
    }

    public void getUserNotifications(String userId, NotificationListCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Notification> notifications = new ArrayList<>();
                    queryDocumentSnapshots.forEach(doc -> {
                        Notification notification = doc.toObject(Notification.class);
                        notification.setNotificationId(doc.getId());
                        notifications.add(notification);
                    });

                    // Changed to Log.i
                    Log.i(TAG, "Fetched " + notifications.size() + " notifications for user: " + userId);

                    if (callback != null) {
                        callback.onSuccess(notifications);
                    }
                })
                .addOnFailureListener(e -> {
                    // Keep error log
                    Log.e(TAG, "Failed to fetch notifications", e);
                    if (callback != null) {
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    public void getUnreadCount(String userId, UnreadCountCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    // Changed to Log.i
                    Log.i(TAG, "Unread notifications for user " + userId + ": " + count);

                    if (callback != null) {
                        callback.onSuccess(count);
                    }
                })
                .addOnFailureListener(e -> {
                    // Keep error log
                    Log.e(TAG, "Failed to get unread count", e);
                    if (callback != null) {
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    public void markAsRead(String notificationId, NotificationCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .document(notificationId)
                .update("read", true)
                .addOnSuccessListener(aVoid -> {
                    // Changed to Log.i
                    Log.i(TAG, "Notification marked as read: " + notificationId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    // Keep error log
                    Log.e(TAG, "Failed to mark notification as read", e);
                    if (callback != null) {
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    public void markAllAsRead(String userId, NotificationCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    if (count == 0) {
                        if (callback != null) {
                            callback.onSuccess();
                        }
                        return;
                    }

                    queryDocumentSnapshots.forEach(doc -> {
                        doc.getReference().update("read", true);
                    });

                    // Changed to Log.i
                    Log.i(TAG, "Marked " + count + " notifications as read for user: " + userId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    // Keep error log
                    Log.e(TAG, "Failed to mark all as read", e);
                    if (callback != null) {
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    public void deleteNotification(String notificationId, NotificationCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .document(notificationId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Changed to Log.i
                    Log.i(TAG, "Notification deleted: " + notificationId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    // Keep error log
                    Log.e(TAG, "Failed to delete notification", e);
                    if (callback != null) {
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    public void deleteAllNotifications(String userId, NotificationCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    queryDocumentSnapshots.forEach(doc -> doc.getReference().delete());

                    // Changed to Log.i
                    Log.i(TAG, "Deleted " + count + " notifications for user: " + userId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    // Keep error log
                    Log.e(TAG, "Failed to delete all notifications", e);
                    if (callback != null) {
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    // Callback interfaces
    public interface NotificationCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface NotificationListCallback {
        void onSuccess(List<Notification> notifications);
        void onFailure(String error);
    }

    public interface UnreadCountCallback {
        void onSuccess(int count);
        void onFailure(String error);
    }

    public interface BulkNotificationCallback {
        void onComplete(int successCount, int failureCount);
    }
}