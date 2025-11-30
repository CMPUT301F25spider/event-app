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
 * MyOrganizedEventsTabFragment - View events organized by current user
 *
 * Features:
 * - List of user's organized events
 * - Quick create event button
 * - Navigate to event details for management
 */
public class MyOrganizedEventsTabFragment extends Fragment {

    private static final String TAG = "MyOrganizedEventsTab";

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

    // ✨ Real-time listener for organizer's events
    private com.google.firebase.firestore.ListenerRegistration eventsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_organized_events_tab, container, false);
    }

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

    private void initViews(View view) {
        rvMyEvents = view.findViewById(R.id.rvMyEvents);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        btnCreateEvent = view.findViewById(R.id.btnCreateEvent);
    }

    private void setupRecyclerView() {
        adapter = new OrganizerEventsAdapter(requireContext());
        rvMyEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMyEvents.setAdapter(adapter);
    }

    private void setupListeners() {
        btnCreateEvent.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreateEventActivity.class);
            startActivity(intent);
        });
    }

    /**
     * ✨ UPDATED: Real-time updates for organizer's events
     * New events appear instantly when created!
     */
    private void loadMyOrganizedEvents() {
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "No user is currently signed in");
            showEmpty("Please sign in to view your events");
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        showLoading();

        // Remove old listener if exists
        if (eventsListener != null) {
            eventsListener.remove();
        }

        // ✨ Real-time listener - Updates automatically when events are created/modified!
        eventsListener = db.collection("events")
                .whereEqualTo("organizerId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to organized events", error);
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

                    Log.d(TAG, "⚡ Real-time update: " + myEvents.size() + " organized events");

                    if (myEvents.isEmpty()) {
                        showEmpty("You haven't organized any events yet");
                    } else {
                        showEvents();
                        adapter.setEvents(myEvents);
                    }
                });
    }

    private void showLoading() {
        rvMyEvents.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    private void showEvents() {
        rvMyEvents.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
    }

    private void showEmpty(String message) {
        rvMyEvents.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        tvEmptyMessage.setText(message);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // ✨ Clean up real-time listener to prevent memory leaks
        if (eventsListener != null) {
            eventsListener.remove();
            eventsListener = null;
            Log.d(TAG, "✅ Events listener cleaned up");
        }
    }
}
