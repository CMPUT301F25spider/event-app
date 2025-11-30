package com.example.event_app.activities.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.adapters.UserAdapter;
import com.example.event_app.models.Event;
import com.example.event_app.models.User;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AdminBrowseUsersActivity - Complete with search and event viewing
 * US 03.05.01: Browse all profiles
 * US 03.02.01: Remove profiles
 * US 03.07.01: Remove organizers violating policy
 *
 * UPDATED: Added search bar and "View Events" for organizers
 */
public class AdminBrowseUsersActivity extends AppCompatActivity {

    private static final String TAG = "BrowseUsersActivity";

    private EditText etSearch;  // NEW
    private RecyclerView recyclerViewUsers;
    private LinearLayout emptyStateLayout;
    private UserAdapter userAdapter;

    private FirebaseFirestore db;
    private List<User> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_users);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Browse Users");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize list
        userList = new ArrayList<>();

        // Initialize views
        initViews();

        // Set up search
        setupSearch();

        // Set up RecyclerView
        setupRecyclerView();

        // Load users
        loadUsers();
    }

    /**
     * Initialize views
     */
    private void initViews() {
        etSearch = findViewById(R.id.etSearchUsers);  // NEW
        recyclerViewUsers = findViewById(R.id.recyclerViewUsers);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
    }

    /**
     * NEW: Setup search functionality
     */
    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                userAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Set up RecyclerView with adapter
     */
    private void setupRecyclerView() {
        // Create adapter
        userAdapter = new UserAdapter();

        // Set click listener
        userAdapter.setOnUserClickListener(new UserAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(User user) {
                showUserInfo(user);
            }

            @Override
            public void onDeleteClick(User user) {
                showDeleteConfirmation(user);
            }

            @Override
            public void onRemoveOrganizerClick(User user) {
                showRemoveOrganizerConfirmation(user);
            }

            @Override
            public void onViewEventsClick(User user) {
                // NEW: Show organizer's events
                showOrganizerEvents(user);
            }

            @Override
            public void onLoadEventsCount(User user, UserAdapter.EventsCountCallback callback) {
                // NEW: Load events count
                loadEventsCount(user, callback);
            }
        });

        // Set layout manager and adapter
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewUsers.setAdapter(userAdapter);
    }

    /**
     * Load all users from Firebase
     */
    private void loadUsers() {
        Log.d(TAG, "Loading users from Firebase...");

        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        userList.add(user);
                    }

                    Log.d(TAG, "Loaded " + userList.size() + " users");

                    // Update UI
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading users", e);
                    Toast.makeText(this, "Error loading users: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();

                    // Show empty state
                    updateUI();
                });
    }

    /**
     * NEW: Load events count for organizer
     */
    private void loadEventsCount(User user, UserAdapter.EventsCountCallback callback) {
        db.collection("events")
                .whereEqualTo("organizerId", user.getUserId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    callback.onCountLoaded(count);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading events count", e);
                    callback.onCountLoaded(0);
                });
    }

    /**
     * NEW: Show organizer's events in dialog
     */
    private void showOrganizerEvents(User user) {
        db.collection("events")
                .whereEqualTo("organizerId", user.getUserId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, user.getName() + " has no events",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Build event list
                    StringBuilder eventsList = new StringBuilder();
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    int count = 1;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = doc.toObject(Event.class);
                        eventsList.append(count++).append(". ")
                                .append(event.getName());

                        if (event.getEventDate() != null) {
                            eventsList.append("\n   Date: ").append(sdf.format(event.getEventDate()));
                        }

                        int waitingCount = event.getWaitingList() != null ?
                                event.getWaitingList().size() : 0;
                        eventsList.append("\n   Entrants: ").append(waitingCount);
                        eventsList.append("\n\n");
                    }

                    // Show dialog
                    new AlertDialog.Builder(this)
                            .setTitle("Events Hosted by " + user.getName())
                            .setMessage(eventsList.toString().trim())
                            .setPositiveButton("OK", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading events", e);
                    Toast.makeText(this, "Error loading events", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Update UI based on user list
     */
    private void updateUI() {
        if (userList.isEmpty()) {
            // Show empty state
            recyclerViewUsers.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            // Show users
            recyclerViewUsers.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);

            // Update adapter
            userAdapter.setUsers(userList);
        }
    }

    /**
     * Show user info dialog
     */
    private void showUserInfo(User user) {
        String roles = user.getRoles() != null ? String.join(", ", user.getRoles()) : "None";
        String info = "Name: " + user.getName() + "\n" +
                "Email: " + user.getEmail() + "\n" +
                "Roles: " + roles + "\n" +
                "User ID: " + user.getUserId();

        new AlertDialog.Builder(this)
                .setTitle("User Information")
                .setMessage(info)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Show confirmation before removing organizer role
     * ✨ UPDATED: Now warns that events will be deleted too
     */
    private void showRemoveOrganizerConfirmation(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Organizer & Delete Events")
                .setMessage("Remove organizer privileges from \"" + user.getName() + "\"?\n\n" +
                        "⚠️ THIS WILL:\n" +
                        "• Remove organizer role\n" +
                        "• DELETE ALL their events\n" +
                        "• Remove events from all entrants\n" +
                        "• Keep their account as entrant\n\n" +
                        "This action CANNOT be undone!\n\n" +
                        "Use this for policy violations only.")
                .setPositiveButton("Remove & Delete All", (dialog, which) -> {
                    removeOrganizerRole(user);
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Remove organizer role from user AND delete all their events
     * US 03.07.01: Remove organizers violating policy
     */
    private void removeOrganizerRole(User user) {
        Log.d(TAG, "Removing organizer role and deleting events for: " + user.getUserId());

        // Show progress
        Toast.makeText(this, "Removing organizer privileges and deleting events...",
                Toast.LENGTH_SHORT).show();

        // Step 1: Get all events by this organizer
        db.collection("events")
                .whereEqualTo("organizerId", user.getUserId())
                .get()
                .addOnSuccessListener(eventsSnapshot -> {
                    Log.d(TAG, "Found " + eventsSnapshot.size() + " events to delete");

                    // Step 2: Delete all their events
                    if (!eventsSnapshot.isEmpty()) {
                        deleteOrganizerEvents(eventsSnapshot, user);
                    } else {
                        // No events to delete, just remove role
                        removeOrganizerRoleOnly(user);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading organizer events", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Delete all events created by the organizer
     */
    private void deleteOrganizerEvents(
            com.google.firebase.firestore.QuerySnapshot eventsSnapshot,
            User user) {

        int totalEvents = eventsSnapshot.size();
        int[] deletedCount = {0};

        for (com.google.firebase.firestore.QueryDocumentSnapshot eventDoc : eventsSnapshot) {
            String eventId = eventDoc.getId();

            // Delete event
            db.collection("events").document(eventId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        deletedCount[0]++;
                        Log.d(TAG, "Deleted event: " + eventId);

                        // If all events deleted, remove organizer role
                        if (deletedCount[0] == totalEvents) {
                            removeOrganizerRoleOnly(user);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to delete event: " + eventId, e);
                        deletedCount[0]++;

                        // Continue even if some deletions fail
                        if (deletedCount[0] == totalEvents) {
                            removeOrganizerRoleOnly(user);
                        }
                    });
        }
    }

    /**
     * Remove organizer role from user (after events are deleted)
     */
    private void removeOrganizerRoleOnly(User user) {
        db.collection("users")
                .document(user.getUserId())
                .update("roles", com.google.firebase.firestore.FieldValue.arrayRemove("organizer"))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Organizer role removed successfully");
                    Toast.makeText(this,
                            "Removed organizer role and deleted all events for " + user.getName(),
                            Toast.LENGTH_LONG).show();

                    // Reload users to update UI
                    loadUsers();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error removing organizer role", e);
                    Toast.makeText(this, "Error removing role: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Show confirmation dialog before deleting user
     */
    private void showDeleteConfirmation(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete User Account")
                .setMessage("Permanently delete user \"" + user.getName() + "\"?\n\n" +
                        "This will remove:\n" +
                        "• Their account\n" +
                        "• All their data\n" +
                        "• Cannot be undone\n\n" +
                        "Consider \"Remove Organizer\" instead if they're just violating policy.")
                .setPositiveButton("Delete Account", (dialog, which) -> {
                    deleteUser(user);
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Delete user from Firebase
     */
    private void deleteUser(User user) {
        Log.d(TAG, "Deleting user: " + user.getUserId());
        deleteUserNotificationLogs(user.getUserId());

        db.collection("users")
                .document(user.getUserId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User deleted successfully");
                    Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show();

                    // Remove from list and update UI
                    userList.remove(user);
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting user", e);
                    Toast.makeText(this, "Error deleting user: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * NEW: Delete all notification logs associated with a user
     */
    private void deleteUserNotificationLogs(String userId) {
        // Delete logs where user was sender
        db.collection("notification_logs")
                .whereEqualTo("senderId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        doc.getReference().delete();
                    }
                    Log.d(TAG, "Deleted " + querySnapshot.size() + " notification logs (sender)");
                });

        // Delete logs where user was recipient
        db.collection("notification_logs")
                .whereEqualTo("recipientId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        doc.getReference().delete();
                    }
                    Log.d(TAG, "Deleted " + querySnapshot.size() + " notification logs (recipient)");
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}