package com.example.event_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.event_app.models.Event;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AdminHomeActivity extends AppCompatActivity {

    private static final String TAG = "AdminHomeActivity";

    private TextView tvEventsCount, tvUsersCount, tvOrganizersCount, tvActiveCount;
    private MaterialButton btnBrowseEvents, btnBrowseUsers, btnBrowseImages;
    private MaterialButton btnTestFirebase; // TEST BUTTON - will remove later

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
        tvEventsCount = findViewById(R.id.tvEventsCount);
        tvUsersCount = findViewById(R.id.tvUsersCount);
        tvOrganizersCount = findViewById(R.id.tvOrganizersCount);
        tvActiveCount = findViewById(R.id.tvActiveCount);

        btnBrowseEvents = findViewById(R.id.btnBrowseEvents);
        btnBrowseUsers = findViewById(R.id.btnBrowseUsers);
        btnBrowseImages = findViewById(R.id.btnBrowseImages);
        btnTestFirebase = findViewById(R.id.btnTestFirebase);

        // Set up button clicks
        setupButtonListeners();

        // Load statistics
        loadStatistics();
    }

    private void setupButtonListeners() {
        // Test Firebase Button - TEMPORARY
        btnTestFirebase.setOnClickListener(v -> testFirebaseConnection());

        btnBrowseEvents.setOnClickListener(v -> {
            Toast.makeText(this, "Browse Events - Coming soon", Toast.LENGTH_SHORT).show();
        });

        btnBrowseUsers.setOnClickListener(v -> {
            Toast.makeText(this, "Browse Users - Coming soon", Toast.LENGTH_SHORT).show();
        });

        btnBrowseImages.setOnClickListener(v -> {
            Toast.makeText(this, "Browse Images - Coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * TEST METHOD - Step 2: Test Firebase Connection
     * This will create a test event and verify Firebase works
     */
    private void testFirebaseConnection() {
        Log.d(TAG, "Testing Firebase connection...");
        Toast.makeText(this, "Testing Firebase...", Toast.LENGTH_SHORT).show();

        // Create a test event
        String testId = "test_event_" + System.currentTimeMillis();
        Event testEvent = new Event(
                testId,
                "Test Event",
                "This is a test event to verify Firebase works",
                "test_organizer"
        );

        // Write to Firebase
        db.collection("events")
                .document(testId)
                .set(testEvent)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ SUCCESS: Event written to Firebase!");
                    Toast.makeText(this, "✅ Firebase Write Success!", Toast.LENGTH_SHORT).show();

                    // Now try to read it back
                    readTestEvent(testId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ FAILED: Could not write to Firebase", e);
                    Toast.makeText(this, "❌ Firebase Write Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Read back the test event to verify Firebase read works
     */
    private void readTestEvent(String testId) {
        db.collection("events")
                .document(testId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        Log.d(TAG, "✅ SUCCESS: Event read from Firebase!");
                        Log.d(TAG, "Event name: " + event.getName());
                        Toast.makeText(this, "✅ Firebase Read Success! Event: " + event.getName(), Toast.LENGTH_LONG).show();
                    } else {
                        Log.e(TAG, "❌ Event not found");
                        Toast.makeText(this, "❌ Event not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ FAILED: Could not read from Firebase", e);
                    Toast.makeText(this, "❌ Firebase Read Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

    @Override
    protected void onResume() {
        super.onResume();
        loadStatistics();
    }
}