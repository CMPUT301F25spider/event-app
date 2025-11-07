package com.example.event_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.adapters.OrganizerEventAdapter;
import com.example.event_app.models.Event;

import java.util.ArrayList;
import java.util.List;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
/**
 * OrganizerCreateEventActivity
 *
 * <p>This activity provides the organizer interface for managing events.
 * It includes buttons for creating events, viewing entrants, and sending
 * notifications, as well as a RecyclerView to display events created by
 * the organizer. Search, filter, and sort controls are also provided.</p>
 *
 * <p>Layout: {@code res/layout/activity_organizer_create_event.xml}</p>
 */
public class OrganizerCreateEventActivity extends AppCompatActivity {

    private RecyclerView recyclerEvents;
    private OrganizerEventAdapter adapter;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_creates_event_new);
        db = FirebaseFirestore.getInstance(); // initialize Firestore


        initToolbar();
        setupButtons();
        setupRecycler();
        loadOrganizerEvents();
        setupSearchFilterSort();
    }

    /** Initializes the toolbar/title section of the activity. */
    private void initToolbar() {
        TextView title = findViewById(R.id.organizer_title);
        title.setText(getString(R.string.organizer_view_title));
    }

    /** Sets up click listeners for the top action buttons. */
    private void setupButtons() {
        Button btnCreateEvent = findViewById(R.id.btn_create_event);
        Button btnViewEntrants = findViewById(R.id.btn_view_entrants);
        Button btnSendNotification = findViewById(R.id.btn_send_notification);

        btnCreateEvent.setOnClickListener(v -> onCreateEventClick());
        btnViewEntrants.setOnClickListener(v -> onViewEntrantsClick());
        btnSendNotification.setOnClickListener(v -> onSendNotificationClick());
    }

    /** Configures the RecyclerView that displays the organizer's events. */
    private void setupRecycler() {
        recyclerEvents = findViewById(R.id.recycler_events);
        recyclerEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrganizerEventAdapter(this::onEventClicked);
        recyclerEvents.setAdapter(adapter);
    }

    /** Loads events created by the organizer. */
    private void loadOrganizerEvents() {
        db.collection("events")
                .get()
                .addOnSuccessListener((QuerySnapshot querySnapshot) -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            events.add(event);
                        }
                    }
                    adapter.submitList(events);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load events: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /** Sets up search, filter, and sort controls. */
    private void setupSearchFilterSort() {
        EditText search = findViewById(R.id.input_search);
        Button btnFilter = findViewById(R.id.btn_filter);
        Button btnSort = findViewById(R.id.btn_sort);

        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                onSearchQueryChanged(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                // not needed
            }
        });
        btnFilter.setOnClickListener(v -> onFilterClick());
        btnSort.setOnClickListener(v -> onSortClick());
    }

    /** Handles Create Event button click. */
    private void onCreateEventClick() {
        // Example: navigate to a CreateEventActivity where you add a new event to Firestore
        Intent intent = new Intent(this, com.example.event_app.organizer.CreateEventActivity.class);
        startActivity(intent);
    }

    /** Handles View Entrants button click. */
    private void onViewEntrantsClick() {
        // For now, you can pick the first event from your adapter or pass a demo ID.
        // Ideally, you’d pass the event the organizer selected in the RecyclerView.
        Event selectedEvent = adapter.getSelectedEvent(); // <-- implement in your adapter if needed

        if (selectedEvent == null) {
            Toast.makeText(this, "Please select an event first", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, com.example.event_app.organizer.EventEntrantsActivityGskakar.class);
        intent.putExtra("eventId", selectedEvent.getId());       // Firestore document ID
        intent.putExtra("eventTitle", selectedEvent.getName());  // Event name/title
        startActivity(intent);
    }

    /** Handles Send Notification button click. */
    private void onSendNotificationClick() { /* TODO */ }

    /** Handles event item click in the RecyclerView. */
    private void onEventClicked(Event event) { /* TODO */ }

    /** Filters the event list based on the search query. */
    private void onSearchQueryChanged(String query) { /* TODO */ }

    /** Opens filter dialog for events. */
    private void onFilterClick() { /* TODO */ }

    /** Opens sort dialog for events. */
    private void onSortClick() { /* TODO */ }
}