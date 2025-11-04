package com.example.event_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.models.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * EventListAdapter
 * Displays each event (name, description, status)
 * and allows entrant to join or leave its waiting list.
 */
public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.EventViewHolder> {

    // Data sources
    private List<Event> eventList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String currentUserId;

    public EventListAdapter() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    /** Replace adapter data */
    public void setEventList(List<Event> events) {
        this.eventList = events;
        notifyDataSetChanged();
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
        holder.bind(eventList.get(position));
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    // ViewHolder
    class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDescription, tvStatus;
        Button btnJoinLeave;

        EventViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEventName);
            tvDescription = itemView.findViewById(R.id.tvEventDescription);
            tvStatus = itemView.findViewById(R.id.tvEventStatus);
            btnJoinLeave = itemView.findViewById(R.id.btnJoinLeave);
        }

        void bind(Event event) {
            tvName.setText(event.getName());
            tvDescription.setText(event.getDescription());
            tvStatus.setText("Status: " + event.getStatus());

            if (currentUserId == null) {
                btnJoinLeave.setEnabled(false);
                btnJoinLeave.setText("Sign-in required");
                return;
            }

            // Reference to this entrant’s waiting-list doc
            DocumentReference entryRef = db.collection("events")
                    .document(event.getEventId())
                    .collection("waitingList")
                    .document(currentUserId);

            // Check if entrant already joined
            entryRef.get().addOnSuccessListener(snapshot -> {
                boolean joined = snapshot.exists();
                btnJoinLeave.setText(joined ? "Leave waiting list" : "Join waiting list");
            });

            // Handle click: toggle join/leave
            btnJoinLeave.setOnClickListener(v -> entryRef.get().addOnSuccessListener(snapshot -> {
                boolean joined = snapshot.exists();
                if (joined) {
                    leaveWaitingList(entryRef);
                } else {
                    joinWaitingList(entryRef, event);
                }
            }));
        }

        // Helper methods

        /** Join event waiting list */
        private void joinWaitingList(DocumentReference entryRef, Event event) {
            entryRef.set(new WaitingEntry(currentUserId, System.currentTimeMillis()))
                    .addOnSuccessListener(aVoid -> {
                        btnJoinLeave.setText("Leave waiting list");
                        Toast.makeText(itemView.getContext(),
                                "Joined " + event.getName(),
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(itemView.getContext(),
                            "Join failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }

        /** Leave event waiting list */
        private void leaveWaitingList(DocumentReference entryRef) {
            entryRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        btnJoinLeave.setText("Join waiting list");
                        Toast.makeText(itemView.getContext(),
                                "Left waiting list", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(itemView.getContext(),
                            "Leave failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    // Simple Firestore DTO for waiting-list entry
    static class WaitingEntry {
        public String userId;
        public long joinedAt;

        public WaitingEntry() { }       // needed for Firestore
        public WaitingEntry(String userId, long joinedAt) {
            this.userId = userId;
            this.joinedAt = joinedAt;
        }
    }
}

