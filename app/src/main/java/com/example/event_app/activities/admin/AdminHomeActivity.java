package com.example.event_app.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.event_app.R;
import com.example.event_app.activities.entrant.MainActivity;
import com.example.event_app.models.Event;
import com.example.event_app.models.User;
import com.example.event_app.utils.AccessibilityHelper;
import com.example.event_app.utils.ReportExporter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Administrative dashboard providing moderation tools, platform statistics,
 * and access to compliance features. Administrators can browse and manage
 * events, users, and images; review flagged items; generate reports; and
 * inspect geolocation usage, notification logs, and notification templates.
 *
 * <p>Supports access control validation, platform-wide reporting, and
 * switching between admin and regular user roles.</p>
 *
 * Features implemented include:
 * <ul>
 *     <li>Platform statistics (events, users, organizers, active events)</li>
 *     <li>Browsing events, users, and uploaded images</li>
 *     <li>Report exporting (US 03.13.01)</li>
 *     <li>Flagged event monitoring</li>
 *     <li>Geolocation access auditing</li>
 *     <li>Notification logs and template management</li>
 *     <li>Role switching between admin and entrant/organizer views</li>
 * </ul>
 */
public class AdminHomeActivity extends AppCompatActivity {
    private TextView tvEventsCount, tvUsersCount, tvOrganizersCount, tvActiveCount;

    // UI Components - Cards
    private MaterialCardView cardBrowseEvents, cardBrowseUsers, cardBrowseImages;

    // UI Components - Buttons
    private MaterialButton btnGenerateReports, btnFlaggedItems, btnSwitchToUserMode;
    private Button btnGeolocationAudit, btnNotificationLogs, btnNotificationTemplates;

    private View layoutFlaggedSection;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);


        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check admin access first
        checkAdminAccess();

        // Initialize views
        initViews();

        // Set up button listeners
        setupButtonListeners();

        // Load statistics
        loadStatistics();

        // Load flagged events
        loadFlaggedEvents();
    }

    /**
     * Initializes all UI elements on the admin dashboard, including
     * statistic counters, action cards, management buttons,
     * and the flagged-events section container.
     */
    private void initViews() {
        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Statistics TextViews
        tvEventsCount = findViewById(R.id.tvEventsCount);
        tvUsersCount = findViewById(R.id.tvUsersCount);
        tvOrganizersCount = findViewById(R.id.tvOrganizersCount);
        tvActiveCount = findViewById(R.id.tvActiveCount);

        // Action Cards
        cardBrowseEvents = findViewById(R.id.cardBrowseEvents);
        cardBrowseUsers = findViewById(R.id.cardBrowseUsers);
        cardBrowseImages = findViewById(R.id.cardBrowseImages);

        // Buttons
        btnGenerateReports = findViewById(R.id.btnGenerateReports);
        btnFlaggedItems = findViewById(R.id.btnFlaggedItems);
        btnSwitchToUserMode = findViewById(R.id.btnSwitchToUserMode);
        btnGeolocationAudit = findViewById(R.id.btnGeolocationAudit);
        btnNotificationLogs = findViewById(R.id.btnNotificationLogs);
        btnNotificationTemplates = findViewById(R.id.btnNotificationTemplates);

        // Flagged section
        layoutFlaggedSection = findViewById(R.id.layoutFlaggedSection);
    }

    /**
     * Configures click listeners for all actionable dashboard elements, including:
     * <ul>
     *     <li>browsing events, users, and images</li>
     *     <li>generating reports</li>
     *     <li>opening geolocation audit and notification log screens</li>
     *     <li>viewing flagged events</li>
     *     <li>switching to entrant/organizer mode</li>
     * </ul>
     *
     * Each listener launches its corresponding admin activity or dialog.
     */
    private void setupButtonListeners() {
        // Browse Events Card
        cardBrowseEvents.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminBrowseEventsActivity.class);
            startActivity(intent);
        });

        // Browse Users Card
        cardBrowseUsers.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminBrowseUsersActivity.class);
            startActivity(intent);
        });

        // Browse Images Card
        cardBrowseImages.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminBrowseImagesActivity.class);
            startActivity(intent);
        });

        // Generate Reports Button
        if (btnGenerateReports != null) {
            btnGenerateReports.setOnClickListener(v -> {
                generateAndExportReport();
            });
        }

        // Geolocation Audit Button
        if (btnGeolocationAudit != null) {
            btnGeolocationAudit.setOnClickListener(v -> {
                startActivity(new Intent(this, AdminGeolocationAuditActivity.class));
            });
        }

        // Notification Logs Button
        if (btnNotificationLogs != null) {
            btnNotificationLogs.setOnClickListener(v -> {
                startActivity(new Intent(this, AdminNotificationLogsActivity.class));
            });
        }

        if (btnNotificationTemplates != null) {
            btnNotificationTemplates.setOnClickListener(v -> {
                startActivity(new Intent(this, AdminNotificationTemplatesActivity.class));
            });
        }

        // Flagged Items Button
        if (btnFlaggedItems != null) {
            btnFlaggedItems.setOnClickListener(v -> {
                Toast.makeText(this, "Showing flagged events", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, AdminBrowseEventsActivity.class);
                intent.putExtra("showFlaggedOnly", true);
                startActivity(intent);
            });
        }
        if (btnSwitchToUserMode != null) {
            btnSwitchToUserMode.setOnClickListener(v -> showRoleSwitchDialog());
        }
    }

    /**
     * Verifies that the currently authenticated user has administrator privileges.
     * If the user is not signed in, not found, or does not hold the "admin" role,
     * access to the dashboard is denied and the activity is terminated.
     *
     * Displays error messages for missing authentication or Firestore issues.
     */
    private void checkAdminAccess() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        currentUser = document.toObject(User.class);

                        if (currentUser == null || !currentUser.isAdmin()) {
                            // Not an admin - deny access
                            Toast.makeText(this, "⛔ Admin access required", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error verifying access", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * Loads platform-wide statistics from Firestore and updates the dashboard:
     * <ul>
     *     <li>Total number of events</li>
     *     <li>Total number of users</li>
     *     <li>Total number of organizers</li>
     *     <li>Total number of active events</li>
     * </ul>
     *
     * Each statistic is requested independently to improve resilience
     * in case one query fails.
     */
    private void loadStatistics() {
        // Load Events Count
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvEventsCount.setText(String.valueOf(count));
                })
                .addOnFailureListener(e -> {
                    tvEventsCount.setText("0");
                });

        // Load Users Count
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvUsersCount.setText(String.valueOf(count));
                })
                .addOnFailureListener(e -> {
                    tvUsersCount.setText("0");
                });

        // Load Organizers Count
        db.collection("users")
                .whereArrayContains("roles", "organizer")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvOrganizersCount.setText(String.valueOf(count));
                })
                .addOnFailureListener(e -> {
                    tvOrganizersCount.setText("0");
                });

        // Load Active Events Count
        db.collection("events")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvActiveCount.setText(String.valueOf(count));
                })
                .addOnFailureListener(e -> {
                    tvActiveCount.setText("0");
                });
    }

    /**
     * Retrieves all events and counts how many exhibit a high cancellation rate.
     * Updates the flagged-items UI section with the current flagged count and
     * shows or hides the flagged-events card accordingly.
     */
    private void loadFlaggedEvents() {
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int flaggedCount = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = doc.toObject(Event.class);
                        if (event.hasHighCancellationRate()) {
                            flaggedCount++;
                        }
                    }

                    // Update button text with count
                    if (btnFlaggedItems != null) {
                        btnFlaggedItems.setText("View Flagged (" + flaggedCount + ")");
                    }
                    // Show/hide flagged events section
                    if (flaggedCount > 0 && layoutFlaggedSection != null) {
                        layoutFlaggedSection.setVisibility(View.VISIBLE);
                    } else if (layoutFlaggedSection != null) {
                        layoutFlaggedSection.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (btnFlaggedItems != null) {
                        btnFlaggedItems.setText("View Flagged (0)");
                    }
                });
    }

    /**
     * Generates a platform-wide usage report by fetching all events and all users.
     * Once both collections are retrieved, the data is passed to
     * {@link ReportExporter#exportPlatformReport(android.content.Context, List, List)}.
     *
     * Displays feedback on report generation progress and errors.
     *
     * US 03.13.01: Export platform usage reports.
     */
    private void generateAndExportReport() {
        Toast.makeText(this, "Generating report...", Toast.LENGTH_SHORT).show();
        // Fetch all events
        db.collection("events")
                .get()
                .addOnSuccessListener(eventSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : eventSnapshots) {
                        events.add(doc.toObject(Event.class));
                    }

                    // Fetch all users
                    db.collection("users")
                            .get()
                            .addOnSuccessListener(userSnapshots -> {
                                List<User> users = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : userSnapshots) {
                                    users.add(doc.toObject(User.class));
                                }
                                // Export report
                                ReportExporter.exportPlatformReport(this, events, users);
                                Toast.makeText(this, "Report generated!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error loading users: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading events: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Displays a dialog describing the available non-admin roles
     * (entrant and/or organizer) and allows the administrator to
     * temporarily switch to user mode. If the admin has no other roles,
     * the dialog is not shown.
     */
    private void showRoleSwitchDialog() {
        if (currentUser == null) return;
        // Build role options
        StringBuilder roleMessage = new StringBuilder();
        roleMessage.append("You have multiple roles. Switch to:\n\n");
        boolean hasOtherRoles = false;
        if (currentUser.isEntrant()) {
            roleMessage.append("Entrant View - Join events, manage waiting lists\n\n");
            hasOtherRoles = true;
        }
        if (currentUser.isOrganizer()) {
            roleMessage.append("Organizer View - Create and manage events\n\n");
            hasOtherRoles = true;
        }
        if (!hasOtherRoles) {
            Toast.makeText(this, "You only have admin role", Toast.LENGTH_SHORT).show();
            return;
        }
        roleMessage.append("You can return to Admin Panel anytime from the menu.");
        new AlertDialog.Builder(this)
                .setTitle("Switch View")
                .setMessage(roleMessage.toString())
                .setPositiveButton("Switch to User Mode", (dialog, which) -> switchToUserMode())
                .setNegativeButton("Stay in Admin", null)
                .show();
    }

    /**
     * Switches the current view mode from administrator to user mode
     * (entrant or organizer), launching {@link MainActivity} and clearing
     * the back stack so the admin panel cannot be navigated back to.
     */
    private void switchToUserMode() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Switched to User mode", Toast.LENGTH_SHORT).show();
    }

    /**
     * Reloads admin access validation, platform statistics, and flagged-events
     * data each time the dashboard becomes active, ensuring all values remain
     * up to date.
     */
    @Override
    protected void onResume() {
        super.onResume();
        checkAdminAccess();
        loadStatistics();
        loadFlaggedEvents();
    }
}