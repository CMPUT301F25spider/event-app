package com.example.event_app.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.event_app.R;
import com.example.event_app.models.Event;
import com.example.event_app.models.User;
import com.example.event_app.utils.ReportExporter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * AdminHomeActivity - Main dashboard for administrators
 * Shows platform statistics and provides quick actions
 */
public class AdminHomeActivity extends AppCompatActivity {

    private static final String TAG = "AdminHomeActivity";

    // UI Components - Statistics
    private TextView tvEventsCount;
    private TextView tvUsersCount;
    private TextView tvOrganizersCount;
    private TextView tvActiveCount;

    // UI Components - Buttons
    private Button btnBrowseEvents;
    private Button btnBrowseUsers;
    private Button btnBrowseImages;
    private Button btnGenerateReports;
    private Button btnFlaggedItems;

    private LinearLayout layoutFlaggedEvents;

    // Firebase
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        // Set title using existing action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Dashboard");
        }

        Log.d(TAG, "AdminHomeActivity created");

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

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
     * Initialize all view components
     */
    private void initViews() {
        // Statistics TextViews
        tvEventsCount = findViewById(R.id.tvEventsCount);
        tvUsersCount = findViewById(R.id.tvUsersCount);
        tvOrganizersCount = findViewById(R.id.tvOrganizersCount);
        tvActiveCount = findViewById(R.id.tvActiveCount);

        // Action Buttons
        btnBrowseEvents = findViewById(R.id.btnBrowseEvents);
        btnBrowseUsers = findViewById(R.id.btnBrowseUsers);
        btnBrowseImages = findViewById(R.id.btnBrowseImages);
        btnGenerateReports = findViewById(R.id.btnGenerateReports);
        btnFlaggedItems = findViewById(R.id.btnFlaggedItems);

        layoutFlaggedEvents = findViewById(R.id.layoutFlaggedEvents);
    }

    /**
     * Setup click listeners for all buttons
     */
    private void setupButtonListeners() {
        // Browse Events
        btnBrowseEvents.setOnClickListener(v -> {
            Log.d(TAG, "Browse Events clicked");
            Intent intent = new Intent(this, AdminBrowseEventsActivity.class);
            startActivity(intent);
        });

        // Browse Users
        btnBrowseUsers.setOnClickListener(v -> {
            Log.d(TAG, "Browse Users clicked");
            Intent intent = new Intent(this, AdminBrowseUsersActivity.class);
            startActivity(intent);
        });

        // Browse Images
        btnBrowseImages.setOnClickListener(v -> {
            Log.d(TAG, "Browse Images clicked");
            Intent intent = new Intent(this, AdminBrowseImagesActivity.class);
            startActivity(intent);
        });

        // Generate Reports
        btnGenerateReports.setOnClickListener(v -> {
            Log.d(TAG, "Generate Reports clicked");
            generateAndExportReport();
        });

        // Flagged Items
        btnFlaggedItems.setOnClickListener(v -> {
            Log.d(TAG, "Flagged Items clicked");
            Toast.makeText(this, "Showing flagged events", Toast.LENGTH_SHORT).show();
            // Open browse events filtered to flagged items
            Intent intent = new Intent(this, AdminBrowseEventsActivity.class);
            intent.putExtra("showFlaggedOnly", true);
            startActivity(intent);
        });
    }

    /**
     * Load all platform statistics from Firebase
     */
    private void loadStatistics() {
        Log.d(TAG, "Loading platform statistics...");

        // Load Events Count
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvEventsCount.setText(String.valueOf(count));
                    Log.d(TAG, "Events count: " + count);
                })
                .addOnFailureListener(e -> {
                    tvEventsCount.setText("0");
                    Log.e(TAG, "Error loading events count", e);
                });

        // Load Users Count
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvUsersCount.setText(String.valueOf(count));
                    Log.d(TAG, "Users count: " + count);
                })
                .addOnFailureListener(e -> {
                    tvUsersCount.setText("0");
                    Log.e(TAG, "Error loading users count", e);
                });

        // Load Organizers Count
        db.collection("users")
                .whereArrayContains("roles", "organizer")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvOrganizersCount.setText(String.valueOf(count));
                    Log.d(TAG, "Organizers count: " + count);
                })
                .addOnFailureListener(e -> {
                    tvOrganizersCount.setText("0");
                    Log.e(TAG, "Error loading organizers count", e);
                });

        // Load Active Events Count
        db.collection("events")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvActiveCount.setText(String.valueOf(count));
                    Log.d(TAG, "Active events count: " + count);
                })
                .addOnFailureListener(e -> {
                    tvActiveCount.setText("0");
                    Log.e(TAG, "Error loading active events count", e);
                });
    }

    /**
     * Load and display flagged events (high cancellation rate)
     */
    private void loadFlaggedEvents() {
        Log.d(TAG, "Loading flagged events...");

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
                    btnFlaggedItems.setText("Flagged Items (" + flaggedCount + ")");

                    Log.d(TAG, "Found " + flaggedCount + " flagged events");

                    // Show/hide flagged events section
                    if (flaggedCount > 0 && layoutFlaggedEvents != null) {
                        layoutFlaggedEvents.setVisibility(View.VISIBLE);
                    } else if (layoutFlaggedEvents != null) {
                        layoutFlaggedEvents.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading flagged events", e);
                    btnFlaggedItems.setText("Flagged Items (0)");
                });
    }

    /**
     * Generate and export platform usage report
     * US 03.13.01: Export platform usage reports
     */
    private void generateAndExportReport() {
        Toast.makeText(this, "Generating report...", Toast.LENGTH_SHORT).show();

        Log.d(TAG, "Starting report generation...");

        // Fetch all events
        db.collection("events")
                .get()
                .addOnSuccessListener(eventSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : eventSnapshots) {
                        events.add(doc.toObject(Event.class));
                    }

                    Log.d(TAG, "Loaded " + events.size() + " events for report");

                    // Fetch all users
                    db.collection("users")
                            .get()
                            .addOnSuccessListener(userSnapshots -> {
                                List<User> users = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : userSnapshots) {
                                    users.add(doc.toObject(User.class));
                                }

                                Log.d(TAG, "Loaded " + users.size() + " users for report");

                                // Export report
                                ReportExporter.exportPlatformReport(this, events, users);
                                Toast.makeText(this, "Report generated!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error loading users for report", e);
                                Toast.makeText(this, "Error loading users: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading events for report", e);
                    Toast.makeText(this, "Error loading events: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Reload statistics when activity resumes
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity resumed, reloading statistics");
        loadStatistics();
        loadFlaggedEvents();
    }
}