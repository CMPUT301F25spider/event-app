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
                            Log.d(TAG, "Notifications disabled for user: " + userId);

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
                    Log.d(TAG, "Notification sent to user: " + userId);

                    logNotificationAfterSend(notificationId, userId, eventId, eventName,
                            type, title, message, "sent");

                    sendFCMPushNotification(userId, title, message, eventId, eventName);

                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send notification", e);

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
                            Log.d(TAG, "📱 FCM token found, calling Cloud Function...");
                            Log.d(TAG, "🔍 Token length: " + fcmToken.length());

                            callCloudFunctionToSendFCM(fcmToken, title, message, eventId);

                        } else {
                            Log.w(TAG, "No FCM token for user: " + userId);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get FCM token for user", e);
                });
    }

    /**
     * ✨ CRITICAL FIX: Ensure all values are explicitly converted to String
     */
    private void callCloudFunctionToSendFCM(String token, String title, String message, String eventId) {
        Log.d(TAG, "🔍 callCloudFunctionToSendFCM called");
        Log.d(TAG, "   token: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "NULL"));
        Log.d(TAG, "   token length: " + (token != null ? token.length() : 0));
        Log.d(TAG, "   title: " + title);
        Log.d(TAG, "   message: " + message);
        Log.d(TAG, "   eventId: " + eventId);

        FirebaseFunctions functions = FirebaseFunctions.getInstance();

        // ✨ CRITICAL FIX: Use String.valueOf() to ensure proper serialization
        Map<String, Object> data = new HashMap<>();
        data.put("token", String.valueOf(token));  // ← Force string conversion
        data.put("title", String.valueOf(title));  // ← Force string conversion
        data.put("message", String.valueOf(message));  // ← Force string conversion
        data.put("eventId", eventId != null ? String.valueOf(eventId) : "");  // ← Force string conversion

        Log.d(TAG, "🔍 Calling function with data size: " + data.size());

        functions
                .getHttpsCallable("sendFCMNotification")
                .call(data)
                .addOnSuccessListener(result -> {
                    Log.d(TAG, "✅ Cloud Function called successfully!");
                    Log.d(TAG, "📱 Push notification sent!");
                    Log.d(TAG, "✅ Result: " + result.getData());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Cloud Function call failed", e);
                    Log.e(TAG, "Error details: " + e.getMessage());
                    if (e.getCause() != null) {
                        Log.e(TAG, "Cause: " + e.getCause().getMessage());
                    }
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
                    Log.d(TAG, "✅ Notification logged for audit: " + logId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error logging notification for audit", e);
                });
    }

    public void sendBulkNotifications(List<String> userIds, String eventId, String eventName,
                                      String type, String title, String message,
                                      BulkNotificationCallback callback) {

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

                    Log.d(TAG, "Fetched " + notifications.size() + " notifications for user: " + userId);

                    if (callback != null) {
                        callback.onSuccess(notifications);
                    }
                })
                .addOnFailureListener(e -> {
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
                    Log.d(TAG, "Unread notifications for user " + userId + ": " + count);

                    if (callback != null) {
                        callback.onSuccess(count);
                    }
                })
                .addOnFailureListener(e -> {
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
                    Log.d(TAG, "Notification marked as read: " + notificationId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
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

                    Log.d(TAG, "Marked " + count + " notifications as read");
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
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
                    Log.d(TAG, "Notification deleted: " + notificationId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
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
                    queryDocumentSnapshots.forEach(doc -> doc.getReference().delete());

                    Log.d(TAG, "Deleted all notifications for user: " + userId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
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