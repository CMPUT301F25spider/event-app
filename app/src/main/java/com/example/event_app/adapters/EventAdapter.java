package com.example.event_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.models.Event;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Universal EventAdapter - Works for Admin, Entrant, and Organizer
 *
 * Usage:
 * - Admin: new EventAdapter(Mode.ADMIN)
 * - Entrant: new EventAdapter(Mode.ENTRANT, userId)
 * - Organizer: new EventAdapter(Mode.ORGANIZER)
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    /**
     * Adapter modes for different user roles
     */
    public enum Mode {
        ADMIN,      // Shows "Delete" button - removes events
        ENTRANT,    // Shows "Join/Leave" button - join waiting list
        ORGANIZER   // Shows "Manage" button - manage event participants
    }

    private List<Event> events;
    private OnEventActionListener listener;
    private Mode mode;
    private String currentUserId; // For entrant mode
    private FirebaseFirestore db;

    /**
     * Constructor for Admin/Organizer (no userId needed)
     */
    public EventAdapter(Mode mode) {
        this.events = new ArrayList<>();
        this.mode = mode;
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Constructor for Entrant (needs userId for join/leave)
     */
    public EventAdapter(Mode mode, String currentUserId) {
        this.events = new ArrayList<>();
        this.mode = mode;
        this.currentUserId = currentUserId;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * Update the list of events
     */
    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    /**
     * Set the action listener
     */
    public void setOnEventActionListener(OnEventActionListener listener) {
        this.listener = listener;
    }

    /**
     * ViewHolder - adapts UI based on mode
     */
    class EventViewHolder extends RecyclerView.ViewHolder {

        private TextView tvEventName;
        private TextView tvEventDescription;
        private TextView tvEventStatus;
        private MaterialButton btnEventAction;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);

            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvEventDescription = itemView.findViewById(R.id.tvEventDescription);
            tvEventStatus = itemView.findViewById(R.id.tvEventStatus);
            btnEventAction = itemView.findViewById(R.id.btnEventAction);
        }

        public void bind(Event event) {
            // Set event name
            tvEventName.setText(event.getName());

            // Set event description
            String description = event.getDescription();

            // Add warning if high cancellation rate (Admin only)
            if (mode == Mode.ADMIN && event.hasHighCancellationRate()) {
                description = "⚠️ HIGH CANCELLATION (" +
                        String.format("%.0f", event.getCancellationRate()) + "%) - " +
                        description;
                tvEventName.setTextColor(itemView.getContext().getColor(android.R.color.holo_red_dark));
            } else {
                tvEventName.setTextColor(itemView.getContext().getColor(R.color.black));
            }

            tvEventDescription.setText(description);

            // Set event status
            String status = event.getStatus() != null ? event.getStatus() : "active";
            tvEventStatus.setText(status.toUpperCase());

            // Configure button based on mode
            configureActionButton(event);

            // Set item click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventClick(event);
                }
            });
        }

        /**
         * Configure button appearance and behavior based on mode
         */
        private void configureActionButton(Event event) {
            switch (mode) {
                case ADMIN:
                    setupAdminButton(event);
                    break;

                case ENTRANT:
                    setupEntrantButton(event);
                    break;

                case ORGANIZER:
                    setupOrganizerButton(event);
                    break;
            }
        }

        /**
         * ADMIN MODE: Delete button
         */
        private void setupAdminButton(Event event) {
            btnEventAction.setText("Delete");
            btnEventAction.setTextColor(itemView.getContext().getColor(android.R.color.holo_red_dark));
            btnEventAction.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAdminDelete(event);
                }
            });
        }

        /**
         * ENTRANT MODE: Join/Leave waiting list button
         */
        private void setupEntrantButton(Event event) {
            if (currentUserId == null) {
                btnEventAction.setText("Sign-in required");
                btnEventAction.setEnabled(false);
                return;
            }

            // Reference to this entrant's waiting list entry
            DocumentReference waitingListRef = db.collection("events")
                    .document(event.getEventId())
                    .collection("waitingList")
                    .document(currentUserId);

            // Check if user already joined
            waitingListRef.get().addOnSuccessListener(snapshot -> {
                boolean isJoined = snapshot.exists();
                btnEventAction.setText(isJoined ? "Leave" : "Join");
                btnEventAction.setTextColor(itemView.getContext().getColor(
                        isJoined ? android.R.color.holo_red_dark : android.R.color.holo_green_dark
                ));
            });

            // Handle join/leave click
            btnEventAction.setOnClickListener(v -> {
                waitingListRef.get().addOnSuccessListener(snapshot -> {
                    boolean isJoined = snapshot.exists();
                    if (isJoined) {
                        // Leave waiting list
                        leaveWaitingList(waitingListRef, event);
                    } else {
                        // Join waiting list
                        joinWaitingList(waitingListRef, event);
                    }
                });
            });
        }

        /**
         * ORGANIZER MODE: Manage button
         */
        private void setupOrganizerButton(Event event) {
            btnEventAction.setText("Manage");
            btnEventAction.setTextColor(itemView.getContext().getColor(R.color.black));
            btnEventAction.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOrganizerManage(event);
                }
            });
        }

        /**
         * Join waiting list (Entrant mode)
         */
        private void joinWaitingList(DocumentReference waitingListRef, Event event) {
            Map<String, Object> waitingEntry = new HashMap<>();
            waitingEntry.put("userId", currentUserId);
            waitingEntry.put("joinedAt", System.currentTimeMillis());

            waitingListRef.set(waitingEntry)
                    .addOnSuccessListener(aVoid -> {
                        btnEventAction.setText("Leave");
                        btnEventAction.setTextColor(itemView.getContext().getColor(android.R.color.holo_red_dark));
                        if (listener != null) {
                            listener.onEntrantJoin(event, currentUserId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (listener != null) {
                            listener.onError("Failed to join: " + e.getMessage());
                        }
                    });
        }

        /**
         * Leave waiting list (Entrant mode)
         */
        private void leaveWaitingList(DocumentReference waitingListRef, Event event) {
            waitingListRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        btnEventAction.setText("Join");
                        btnEventAction.setTextColor(itemView.getContext().getColor(android.R.color.holo_green_dark));
                        if (listener != null) {
                            listener.onEntrantLeave(event, currentUserId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (listener != null) {
                            listener.onError("Failed to leave: " + e.getMessage());
                        }
                    });
        }
    }

    /**
     * Interface for handling all possible actions
     * Each role only needs to implement the methods they use
     */
    public interface OnEventActionListener {
        // Common for all roles
        void onEventClick(Event event);

        // Admin-specific
        default void onAdminDelete(Event event) {}

        // Entrant-specific
        default void onEntrantJoin(Event event, String userId) {}
        default void onEntrantLeave(Event event, String userId) {}

        // Organizer-specific
        default void onOrganizerManage(Event event) {}

        // Error handling
        default void onError(String message) {}
    }
}