package com.example.event_app.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.adapters.EventAdapter;
import com.example.event_app.models.Event;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * BrowseEventsActivity - Admin can view all events
 * US 03.04.01: As an administrator, I want to browse all events
 * US 03.01.01: As an administrator, I want to remove events
 */
public class AdminBrowseEventsActivity extends AppCompatActivity {

    private static final String TAG = "BrowseEventsActivity";

    private RecyclerView recyclerViewEvents;
    private LinearLayout emptyStateLayout;
    private EventAdapter eventAdapter;

    private FirebaseFirestore db;
    private List<Event> eventList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_events);

        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Browse Events");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize list
        eventList = new ArrayList<>();

        // Initialize views
        initViews();

        // Set up RecyclerView
        setupRecyclerView();

        // Load events
        loadEvents();
    }

    /**
     * Initialize views
     */
    private void initViews() {
        recyclerViewEvents = findViewById(R.id.recyclerViewEvents);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
    }

    /**
     * Set up RecyclerView with adapter
     * UPDATED: Uses unified adapter in ADMIN mode
     */
    private void setupRecyclerView() {
        // Create adapter in ADMIN mode
        eventAdapter = new EventAdapter(EventAdapter.Mode.ADMIN);

        // Set action listener
        eventAdapter.setOnEventActionListener(new EventAdapter.OnEventActionListener() {
            @Override
            public void onEventClick(Event event) {
                // For now, just show a toast
                // Later: Open event details screen
                Toast.makeText(AdminBrowseEventsActivity.this,
                        "Event: " + event.getName(),
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdminDelete(Event event) {
                // Show confirmation dialog
                showDeleteConfirmation(event);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AdminBrowseEventsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });

        // Set layout manager and adapter
        recyclerViewEvents.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewEvents.setAdapter(eventAdapter);
    }

    /**
     * Load all events from Firebase
     */
    private void loadEvents() {
        Log.d(TAG, "Loading events from Firebase...");

        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        eventList.add(event);
                    }

                    Log.d(TAG, "Loaded " + eventList.size() + " events");

                    // Update UI
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading events", e);
                    Toast.makeText(this, "Error loading events: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();

                    // Show empty state
                    updateUI();
                });
    }

    /**
     * Update UI based on event list
     */
    private void updateUI() {
        if (eventList.isEmpty()) {
            // Show empty state
            recyclerViewEvents.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            // Show events
            recyclerViewEvents.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);

            // Update adapter
            eventAdapter.setEvents(eventList);
        }
    }

    /**
     * Show confirmation dialog before deleting event
     */
    private void showDeleteConfirmation(Event event) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete \"" + event.getName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteEvent(event);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Delete event from Firebase
     * US 03.01.01: Remove events
     */
    private void deleteEvent(Event event) {
        Log.d(TAG, "Deleting event: " + event.getEventId());

        db.collection("events")
                .document(event.getEventId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event deleted successfully");
                    Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();

                    // Remove from list and update UI
                    eventList.remove(event);
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting event", e);
                    Toast.makeText(this, "Error deleting event: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Handle back button in action bar
        finish();
        return true;
    }
}