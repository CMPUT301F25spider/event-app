package com.example.event_app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.activities.organizer.CreateEventActivity;
import com.example.event_app.adapters.OrganizerEventsAdapter;
import com.example.event_app.models.Event;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * MyOrganizedEventsTabFragment - Displays all events created by the current user.
 *
 * Features:
 * <ul>
 *     <li>Real-time listener for organizer's events</li>
 *     <li>List of events organized by logged-in user</li>
 *     <li>Quick "Create Event" button</li>
 *     <li>Automatic UI states: loading, empty, populated</li>
 * </ul>
 */
public class MyOrganizedEventsTabFragment extends Fragment {
    // UI Components
    private RecyclerView rvMyEvents;
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private TextView tvEmptyMessage;
    private MaterialButton btnCreateEvent;

    // Data
    private OrganizerEventsAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<Event> myEvents;

    // Real-time listener for organizer's events
    private com.google.firebase.firestore.ListenerRegistration eventsListener;

    /**
     * Inflates the layout for the organized-events tab.
     *
     * @param inflater LayoutInflater used to inflate the UI
     * @param container Optional parent container
     * @param savedInstanceState Previously saved state
     * @return The inflated root view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_organized_events_tab, container, false);
    }

    /**
     * Initializes Firebase, sets up UI, RecyclerView, listeners, and starts
     * loading real-time organized event data.
     *
     * @param view Fragment root view
     * @param savedInstanceState Previously saved state
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        myEvents = new ArrayList<>();

        // Initialize views
        initViews(view);

        // Setup RecyclerView
        setupRecyclerView();

        // Setup listeners
        setupListeners();

        // Load events
        loadMyOrganizedEvents();
    }

    /**
     * Initializes UI components for:
     * <ul>
     *     <li>RecyclerView for organizer events</li>
     *     <li>Loading indicator</li>
     *     <li>Empty state layout</li>
     *     <li>Create Event button</li>
     * </ul>
     *
     * @param view Root view containing the fragment UI elements
     */
    private void initViews(View view) {
        rvMyEvents = view.findViewById(R.id.rvMyEvents);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        btnCreateEvent = view.findViewById(R.id.btnCreateEvent);
    }

    /**
     * Configures the RecyclerView with:
     * <ul>
     *     <li>LinearLayoutManager</li>
     *     <li>OrganizerEventsAdapter</li>
     * </ul>
     * Ensures events display in vertical scrolling list format.
     */
    private void setupRecyclerView() {
        adapter = new OrganizerEventsAdapter(requireContext());
        rvMyEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMyEvents.setAdapter(adapter);
    }

    /**
     * Attaches interaction listeners including:
     * <ul>
     *     <li>"Create Event" button → opens CreateEventActivity</li>
     * </ul>
     */
    private void setupListeners() {
        btnCreateEvent.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreateEventActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Loads events organized by the current user using a real-time Firestore listener.
     *
     * Behavior:
     * <ul>
     *     <li>If user not logged in → empty state with message</li>
     *     <li>Shows loading indicator while fetching</li>
     *     <li>Listens for live updates to the user's events</li>
     *     <li>Populates list or shows empty message accordingly</li>
     * </ul>
     *
     * Real-time:
     * Automatically updates when events are created, edited, or deleted.
     */
    private void loadMyOrganizedEvents() {
        if (mAuth.getCurrentUser() == null) {
            showEmpty("Please sign in to view your events");
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        showLoading();

        if (eventsListener != null) {
            eventsListener.remove();
        }

        // Real-time listener - Updates automatically when events are created/modified!
        eventsListener = db.collection("events")
                .whereEqualTo("organizerId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        showEmpty("Failed to load events. Please try again.");
                        return;
                    }

                    if (queryDocumentSnapshots == null) {
                        showEmpty("You haven't organized any events yet");
                        return;
                    }

                    myEvents.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = doc.toObject(Event.class);
                        event.setId(doc.getId());
                        myEvents.add(event);
                    }

                    if (myEvents.isEmpty()) {
                        showEmpty("You haven't organized any events yet");
                    } else {
                        showEvents();
                        adapter.setEvents(myEvents);
                    }
                });
    }

    /**
     * Displays the loading state while hiding list and empty view.
     */
    private void showLoading() {
        rvMyEvents.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    /**
     * Displays the populated events list and hides loading/empty states.
     */
    private void showEvents() {
        rvMyEvents.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
    }

    /**
     * Shows an empty state message when no organized events are available.
     *
     * @param message Text explaining why the list is empty
     */
    private void showEmpty(String message) {
        rvMyEvents.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        tvEmptyMessage.setText(message);
    }

    /**
     * Cleans up real-time Firestore listener to prevent memory leaks
     * when the fragment's view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Clean up real-time listener to prevent memory leaks
        if (eventsListener != null) {
            eventsListener.remove();
            eventsListener = null;
        }
    }
}
