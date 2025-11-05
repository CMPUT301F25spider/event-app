package com.example.event_app.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.event_app.R;
import com.example.event_app.models.Event;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import com.example.event_app.utils.ReportExporter;
import com.example.event_app.models.User;
import java.util.ArrayList;
import java.util.List;

import com.google.firebase.firestore.QueryDocumentSnapshot;
public class AdminHomeActivity extends AppCompatActivity {

    private static final String TAG = "AdminHomeActivity";

    private TextView tvEventsCount, tvUsersCount, tvOrganizersCount, tvActiveCount;
    private MaterialButton btnBrowseEvents, btnBrowseUsers, btnBrowseImages, btnGenerateReports;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Dashboard");
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initViews();

        // Set up button clicks
        setupButtonListeners();

        // Load statistics
        loadStatistics();
    }

    private void initViews() {
        tvEventsCount = findViewById(R.id.tvEventsCount);
        tvUsersCount = findViewById(R.id.tvUsersCount);
        tvOrganizersCount = findViewById(R.id.tvOrganizersCount);
        tvActiveCount = findViewById(R.id.tvActiveCount);

        btnBrowseEvents = findViewById(R.id.btnBrowseEvents);
        btnBrowseUsers = findViewById(R.id.btnBrowseUsers);
        btnBrowseImages = findViewById(R.id.btnBrowseImages);
        btnGenerateReports = findViewById(R.id.btnGenerateReports);
    }

    private void setupButtonListeners() {
        btnBrowseEvents.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminBrowseEventsActivity.class);
            startActivity(intent);
        });

        btnBrowseUsers.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminBrowseUsersActivity.class);
            startActivity(intent);
        });

        btnBrowseImages.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminBrowseImagesActivity.class);
            startActivity(intent);
        });

        btnGenerateReports.setOnClickListener(v -> {
            generateAndExportReport();
        });
    }

    private void loadStatistics() {
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
     * Generate and export platform usage report
     * US 03.13.01: Export platform usage reports
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

    @Override
    protected void onResume() {
        super.onResume();
        loadStatistics();
    }
}