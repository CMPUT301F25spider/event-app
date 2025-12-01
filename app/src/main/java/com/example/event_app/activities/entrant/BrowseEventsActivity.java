package com.example.event_app.activities.entrant;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.adapters.EventAdapter;
import com.example.event_app.models.Event;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;


/**
 * BrowseEventsActivity - Allows entrants to browse all active events.
 *
 * Features:
 * • Displays list of active events retrieved from Firestore
 * • Shows waiting list count for each event
 * • Handles loading, empty state, and error views
 *
 * US 01.01.03: Browse available events
 * US 01.05.04: View waiting list count for each event
 */
public class BrowseEventsActivity extends AppCompatActivity {
    // UI Elements
    private RecyclerView rvEvents;
    private ProgressBar progressBar;
    private TextView tvEmptyState, tvErrorState;
    private MaterialButton btnRetry;
    private View emptyView, errorView;
    private EventAdapter adapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_events);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Load events
        loadEvents();
    }

    /**
     * Initializes all UI components including RecyclerView, loading indicators,
     * empty/error views, and attaches listeners to the Back and Retry buttons.
     */
    private void initViews() {
        rvEvents = findViewById(R.id.rvEvents);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvErrorState = findViewById(R.id.tvErrorState);
        btnRetry = findViewById(R.id.btnRetry);
        emptyView = findViewById(R.id.emptyView);
        errorView = findViewById(R.id.errorView);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Retry button
        btnRetry.setOnClickListener(v -> loadEvents());
    }

    /**
     * Configures the RecyclerView for displaying event cards.
     * Sets the adapter and applies a vertical LinearLayoutManager.
     */
    private void setupRecyclerView() {
        adapter = new EventAdapter(this);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);
    }

    /**
     * Loads all active events from Firestore ordered by creation date.
     * Handles state transitions between loading, empty, error, and success views.
     *
     * Firestore query:
     * • status = "active"
     * • ordered by createdAt DESC
     *
     * US 01.01.03: Browse available events
     *
     */
    private void loadEvents() {
        showLoading();

        db.collection("events")
                .whereEqualTo("status", "active")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        event.setId(document.getId());
                        events.add(event);
                    }

                    if (events.isEmpty()) {
                        showEmpty();
                    } else {
                        showEvents(events);
                    }
                })
                .addOnFailureListener(e -> {
                    showError("Failed to load events. Please try again.");
                });
    }

    /**
     * Displays the loading spinner while hiding all other UI sections
     * (events list, empty state, error state).
     */
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        rvEvents.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
    }

    /**
     * Displays a list of fetched events by updating the adapter and switching
     * visibility to show the RecyclerView only.
     *
     * @param events list of active events retrieved from Firestore
     */
    private void showEvents(List<Event> events) {
        progressBar.setVisibility(View.GONE);
        rvEvents.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        adapter.setEvents(events);
    }

    /**
     * Displays the empty state view when no active events are available.
     * Hides the list and error views.
     */
    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        rvEvents.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
        tvEmptyState.setText("No events available yet.\nCheck back soon!");
    }

    /**
     * Displays the error view with a message explaining what went wrong
     * during event fetching.
     *
     * @param message the error message to display
     */
    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        rvEvents.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
        tvErrorState.setText(message);
    }

    /**
     * Reloads the list of events whenever the user returns to this screen.
     *
     * Ensures the most up-to-date event availability is shown.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Reload events when returning to this screen
        loadEvents();
    }
}
