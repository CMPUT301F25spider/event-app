package com.example.event_app.activities.entrant;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.adapters.NotificationAdapter;
import com.example.event_app.models.Notification;
import com.example.event_app.services.NotificationService;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationsActivity – Displays all notifications for the current user.
 *
 * Features:
 * • Real-time Firestore listener for instant updates
 * • Mark individual or all notifications as read
 * • Delete single or all notifications
 * • Tap notification to navigate to its related event
 *
 * US 01.04.01: Receive notification when chosen
 * US 01.04.02: Receive notification when not chosen
 */
public class NotificationsActivity extends AppCompatActivity {
    // UI Components
    private RecyclerView rvNotifications;
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private TextView tvEmptyMessage;
    private MaterialButton btnMarkAllRead, btnClearAll;

    // Data
    private NotificationAdapter adapter;
    private NotificationService notificationService;
    private List<Notification> notifications;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    //Firestore listener for real-time updates
    private ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        notificationService = new NotificationService();
        notifications = new ArrayList<>();

        // Initialize views
        initViews();
        setupRecyclerView();
        setupListeners();

        // Load notifications with real-time listener
        startRealtimeNotificationListener();
    }

    /**
     * Initializes all view references and sets up the Back button listener.
     *
     * Responsibilities:
     * • Link layout views to fields
     * • Configure back navigation
     */
    private void initViews() {
        rvNotifications = findViewById(R.id.rvNotifications);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        btnClearAll = findViewById(R.id.btnClearAll);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    /**
     * Configures the RecyclerView with a NotificationAdapter.
     *
     * Handles:
     * • Click on notification → mark as read + navigate to event
     * • Click on delete icon → delete the notification
     */
    private void setupRecyclerView() {
        adapter = new NotificationAdapter(this, notifications, new NotificationAdapter.NotificationClickListener() {
            @Override
            public void onNotificationClick(Notification notification) {
                // Mark as read when clicked
                if (!notification.isRead()) {
                    markAsRead(notification);
                }
                // Navigate to event details
                navigateToEvent(notification.getEventId());
            }
            @Override
            public void onDeleteClick(Notification notification) {
                deleteNotification(notification);
            }
        });

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);
    }

    /**
     * Sets click listeners for:
     * • "Mark All Read"
     * • "Clear All"
     *
     * These actions operate on the entire notification list.
     */
    private void setupListeners() {
        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
        btnClearAll.setOnClickListener(v -> showClearAllConfirmation());
    }

    /**
     * Starts a real-time Firestore listener for the user's notifications.
     *
     * Notifications update instantly without refreshing the screen.
     *
     * Behavior:
     * • Clears and reloads list when Firestore snapshot changes
     * • Shows empty state when there are no notifications
     *
     * Removes previous listener automatically when set up again.
     */
    private void startRealtimeNotificationListener() {
        String userId = mAuth.getCurrentUser().getUid();

        showLoading();

        // addSnapshotListener() for real-time updates
        notificationListener = db.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        showEmpty();
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        notifications.clear();
                        showEmpty();
                        adapter.notifyDataSetChanged();
                        updateButtonStates();
                        return;
                    }

                    //Update list in real-time!
                    notifications.clear();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Notification notification = doc.toObject(Notification.class);
                        notification.setNotificationId(doc.getId());
                        notifications.add(notification);
                    }
                    showNotifications();
                    adapter.notifyDataSetChanged();
                    updateButtonStates();
                });
    }

    /**
     * Performs a one-time load of all notifications for the user.
     *
     * Used as a fallback or manual refresh (not real-time).
     *
     * Shows loading state, then either:
     * • Display notifications
     * • Show empty view
     */
    private void loadNotifications() {
        showLoading();

        String userId = mAuth.getCurrentUser().getUid();

        notificationService.getUserNotifications(userId, new NotificationService.NotificationListCallback() {
            @Override
            public void onSuccess(List<Notification> fetchedNotifications) {
                notifications.clear();
                notifications.addAll(fetchedNotifications);

                if (notifications.isEmpty()) {
                    showEmpty();
                } else {
                    showNotifications();
                }

                adapter.notifyDataSetChanged();
                updateButtonStates();
            }

            @Override
            public void onFailure(String error) {
                showEmpty();
            }
        });
    }

    /**
     * Marks a single notification as read using NotificationService.
     *
     * @param notification The notification to mark as read.
     */
    private void markAsRead(Notification notification) {
        notificationService.markAsRead(notification.getNotificationId(),
                new NotificationService.NotificationCallback() {
                    @Override
                    public void onSuccess() {
                        notification.setRead(true);
                        adapter.notifyDataSetChanged();
                        updateButtonStates();
                    }

                    @Override
                    public void onFailure(String error) {}
                });
    }

    /**
     * Marks all of the user's notifications as read.
     *
     * Updates Firestore and then updates the UI list locally.
     */
    private void markAllAsRead() {
        String userId = mAuth.getCurrentUser().getUid();

        notificationService.markAllAsRead(userId, new NotificationService.NotificationCallback() {
            @Override
            public void onSuccess() {
                // Update local list
                for (Notification notification : notifications) {
                    notification.setRead(true);
                }
                adapter.notifyDataSetChanged();
                updateButtonStates();
            }

            @Override
            public void onFailure(String error) {}
        });
    }

    /**
     * Deletes a single notification from Firestore and updates the RecyclerView.
     *
     * @param notification The notification to delete.
     */
    private void deleteNotification(Notification notification) {
        notificationService.deleteNotification(notification.getNotificationId(),
                new NotificationService.NotificationCallback() {
                    @Override
                    public void onSuccess() {
                        notifications.remove(notification);
                        adapter.notifyDataSetChanged();

                        if (notifications.isEmpty()) {
                            showEmpty();
                        }

                        updateButtonStates();
                    }

                    @Override
                    public void onFailure(String error) {}
                });
    }

    /**
     * Shows a confirmation dialog before deleting all notifications.
     *
     * Prevents accidental bulk deletion.
     */
    private void showClearAllConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Notifications")
                .setMessage("Are you sure you want to delete all notifications? This cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> clearAll())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes all notifications for the current user from Firestore.
     *
     * Clears the local list and updates UI states.
     */
    private void clearAll() {
        String userId = mAuth.getCurrentUser().getUid();

        notificationService.deleteAllNotifications(userId, new NotificationService.NotificationCallback() {
            @Override
            public void onSuccess() {
                notifications.clear();
                adapter.notifyDataSetChanged();
                showEmpty();
                updateButtonStates();
            }

            @Override
            public void onFailure(String error) {}
        });
    }

    /**
     * Navigates to EventDetailsActivity for the event tied to the notification.
     *
     * @param eventId The ID of the related event.
     */
    private void navigateToEvent(String eventId) {
        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra("eventId", eventId);
        startActivity(intent);
    }

    /**
     * Updates the enabled/disabled states of:
     * • "Mark All Read"
     * • "Clear All"
     *
     * Rules:
     * • "Mark All Read" enabled only if there are unread notifications
     * • "Clear All" enabled only if the list is non-empty
     */
    private void updateButtonStates() {
        boolean hasNotifications = !notifications.isEmpty();
        boolean hasUnread = false;

        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                hasUnread = true;
                break;
            }
        }

        btnMarkAllRead.setEnabled(hasUnread);
        btnMarkAllRead.setAlpha(hasUnread ? 1.0f : 0.5f);

        btnClearAll.setEnabled(hasNotifications);
        btnClearAll.setAlpha(hasNotifications ? 1.0f : 0.5f);
    }

    /**
     * Displays loading spinner and hides list & empty state.
     */
    private void showLoading() {
        rvNotifications.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    /**
     * Shows the notifications list and hides loading/empty views.
     */
    private void showNotifications() {
        rvNotifications.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
    }

    /**
     * Shows the empty-state view when no notifications are available.
     */
    private void showEmpty() {
        rvNotifications.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }

    /**
     * Cleans up the Firestore real-time listener to prevent:
     * • Memory leaks
     * • Battery drain
     * • Duplicate listeners
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove listener to prevent memory leaks
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }
}