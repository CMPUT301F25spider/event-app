package com.example.event_app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.models.Event;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * BrowseEventsActivity
 * --------------------
 * Shows a list of available events that entrants can join.
 * Also supports text filtering (US 01.01.03 & US 01.01.04).
 */
public class BrowseEventsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewEvents;
    private TextView tvEmptyEvents;
    private TextInputEditText searchEvents;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<Event> allEvents = new ArrayList<>();
    private final List<Event> filteredEvents = new ArrayList<>();
    private EventListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Browse Events");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // ← back arrow
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_events);

        recyclerViewEvents = findViewById(R.id.recyclerViewEvents);
        tvEmptyEvents = findViewById(R.id.tvEmptyEvents);
        searchEvents = findViewById(R.id.searchEvents);

        recyclerViewEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventListAdapter();
        recyclerViewEvents.setAdapter(adapter);

        loadEvents();       // fetch data from Firestore
        setupSearch();       // attach filter listener
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * Loads active events from Firestore and fills RecyclerView.
     */
    private void loadEvents() {
        db.collection("events")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allEvents.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Event e = doc.toObject(Event.class);
                        // Ensure eventId is set (it might not be in Firestore)
                        e.setEventId(doc.getId());
                        allEvents.add(e);
                    }
                    applyFilter("");       // show all initially
                })
                .addOnFailureListener(e -> {
                    tvEmptyEvents.setText("Failed to load events: " + e.getMessage());
                    tvEmptyEvents.setVisibility(View.VISIBLE);
                });
    }

    /**
     * Adds text-change listener to filter events by user input.
     */
    private void setupSearch() {
        if (searchEvents == null) return;

        searchEvents.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    /**
     * Filters events by text match in name or description.
     */
    private void applyFilter(@NonNull String query) {
        String lower = query.toLowerCase().trim();
        filteredEvents.clear();

        if (lower.isEmpty()) {
            filteredEvents.addAll(allEvents);
        } else {
            for (Event e : allEvents) {
                if ((e.getName() != null && e.getName().toLowerCase().contains(lower)) ||
                        (e.getDescription() != null && e.getDescription().toLowerCase().contains(lower))) {
                    filteredEvents.add(e);
                }
            }
        }

        if (filteredEvents.isEmpty()) {
            tvEmptyEvents.setText("No matching events");
            tvEmptyEvents.setVisibility(View.VISIBLE);
        } else {
            tvEmptyEvents.setVisibility(View.GONE);
        }

        adapter.setEventList(filteredEvents);
    }
}

