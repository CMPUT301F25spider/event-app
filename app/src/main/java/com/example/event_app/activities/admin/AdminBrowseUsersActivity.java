package com.example.event_app.activities.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity allowing administrators to browse, search, inspect, and manage all user profiles.
 *
 * <p>Supports:
 * <ul>
 *   <li><b>US 03.05.01</b>: Browse all user profiles.</li>
 *   <li><b>US 03.02.01</b>: Remove user accounts.</li>
 *   <li><b>US 03.07.01</b>: Remove organizers violating policy (including deleting their events).</li>
 * </ul>
 *
 * Administrators can:
 * <ul>
 *   <li>Search users by name or email.</li>
 *   <li>View user information.</li>
 *   <li>Delete user accounts.</li>
 *   <li>View all events hosted by an organizer.</li>
 *   <li>Remove organizer privileges and automatically delete all their events.</li>
 * </ul>
 */
public class AdminBrowseUsersActivity extends AppCompatActivity {
    private EditText etSearch;
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
     * Initializes the screen's editable search bar and list container views.
     */
    private void initViews() {
        etSearch = findViewById(R.id.etSearchUsers);  // NEW
        recyclerViewUsers = findViewById(R.id.recyclerViewUsers);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
    }

    /**
     * Sets up the live search functionality so the displayed list updates as the admin types.
     * Filtering is delegated to {@link UserAdapter#filter(String)}.
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
     * Configures the RecyclerView and attaches listeners for user-level admin actions:
     * viewing user info, deleting accounts, removing organizer privileges,
     * and loading events hosted by a user.
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
                // Show organizer's events
                showOrganizerEvents(user);
            }
            @Override
            public void onLoadEventsCount(User user, UserAdapter.EventsCountCallback callback) {
                // Load events count
                loadEventsCount(user, callback);
            }
        });
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewUsers.setAdapter(userAdapter);
    }

    /**
     * Loads all user profiles from Firestore and updates the UI.
     * Errors show an empty state with a toast.
     */
    private void loadUsers() {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        userList.add(user);
                    }
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading users: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    updateUI();
                });
    }

    /**
     * Loads the number of events an organizer has created.
     * Used for showing counts in the user list.
     *
     * @param user organizer whose events are counted
     * @param callback returns the count asynchronously
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
                    callback.onCountLoaded(0);
                });
    }

    /**
     * Displays all events hosted by a selected organizer in a single dialog.
     * Includes event name, date, and number of entrants.
     *
     * @param user organizer whose events are displayed
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
                    Toast.makeText(this, "Error loading events", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Updates the UI depending on whether users exist.
     * Shows an empty state when no users are found.
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
     * Displays a dialog containing detailed information about the selected user.
     * Shown fields include name, email, roles, and user ID.
     *
     * @param user the user whose information is being displayed
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
     * Shows a high-severity confirmation dialog warning the administrator that
     * removing an organizer will also permanently delete all events created
     * by that organizer. This supports policy-violation enforcement.
     *
     * @param user the organizer being reviewed for removal
     */
    private void showRemoveOrganizerConfirmation(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Organizer & Delete Events")
                .setMessage("Remove organizer privileges from \"" + user.getName() + "\"?\n\n" +
                        "THIS WILL:\n" +
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
     * Begins the removal process for an organizer. This method first queries
     * all events owned by the organizer. If any events exist, they are deleted
     * before the organizer role is removed.
     *
     * @param user the organizer whose role is being revoked
     */

    private void removeOrganizerRole(User user) {
        // Show progress
        Toast.makeText(this, "Removing organizer privileges and deleting events...",
                Toast.LENGTH_SHORT).show();

        // Get all events by this organizer
        db.collection("events")
                .whereEqualTo("organizerId", user.getUserId())
                .get()
                .addOnSuccessListener(eventsSnapshot -> {
                    if (!eventsSnapshot.isEmpty()) {
                        deleteOrganizerEvents(eventsSnapshot, user);
                    } else {
                        removeOrganizerRoleOnly(user);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Deletes every event created by the organizer. Once all deletions
     * (successful or failed) are processed, the organizer role is removed.
     *
     * @param eventsSnapshot Firestore snapshot of all events created by the organizer
     * @param user the organizer whose events are being deleted
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
                        // If all events deleted, remove organizer role
                        if (deletedCount[0] == totalEvents) {
                            removeOrganizerRoleOnly(user);
                        }
                    })
                    .addOnFailureListener(e -> {
                        deletedCount[0]++;

                        // Continue even if some deletions fail
                        if (deletedCount[0] == totalEvents) {
                            removeOrganizerRoleOnly(user);
                        }
                    });
        }
    }

    /**
     * Removes the "organizer" role from the given user after all their events
     * have been deleted. The user remains in the system as an entrant.
     *
     * @param user the user whose organizer role is being removed
     */
    private void removeOrganizerRoleOnly(User user) {
        db.collection("users")
                .document(user.getUserId())
                .update("roles", com.google.firebase.firestore.FieldValue.arrayRemove("organizer"))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            "Removed organizer role and deleted all events for " + user.getName(),
                            Toast.LENGTH_LONG).show();

                    // Reload users to update UI
                    loadUsers();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error removing role: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Displays a confirmation dialog asking the administrator whether to
     * permanently delete the selected user account. This action cannot be undone
     * and removes all associated data.
     *
     * @param user the user whose account may be deleted
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
     * Permanently deletes a user from the Firestore database. All notification logs
     * associated with the user are deleted first to maintain data consistency.
     *
     * @param user the user to delete
     */
    private void deleteUser(User user) {
        deleteUserNotificationLogs(user.getUserId());

        db.collection("users")
                .document(user.getUserId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show();

                    // Remove from list and update UI
                    userList.remove(user);
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting user: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Deletes all notification log entries where the user acted as either the sender
     * or recipient. This is performed before the user account itself is removed.
     *
     * @param userId the ID of the user whose notification logs should be deleted
     */
    private void deleteUserNotificationLogs(String userId) {
        db.collection("notification_logs")
                .whereEqualTo("senderId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        doc.getReference().delete();
                    }
                });
        // Delete logs where user was recipient
        db.collection("notification_logs")
                .whereEqualTo("recipientId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        doc.getReference().delete();
                    }
                });
    }

    /**
     * Handles the action bar "Back" navigation event by closing the activity.
     *
     * @return true indicating the event was consumed
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}