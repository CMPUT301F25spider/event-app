package com.example.event_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.models.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SettingsActivity
 *  - View and update entrant profile (name, email, phone)
 *  - Delete profile document from Firestore
 *  - Show history of joined events with ability to leave (US 01.02.03)
 *
 * Covers:
 *  - US 01.02.01 / 01.02.02 (profile provide/update)
 *  - US 01.02.03 (joined events history)
 *  - US 01.02.04 (delete profile)
 */
public class SettingsActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone;
    private Button btnSaveProfile, btnDeleteProfile;

    private RecyclerView recyclerMyEvents;
    private JoinedEventsAdapter joinedEventsAdapter;
    private final List<Event> joinedEvents = new ArrayList<>();

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Action bar title & back arrow (same style as Browse Events)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Profile Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnDeleteProfile = findViewById(R.id.btnDeleteProfile);

        recyclerMyEvents = findViewById(R.id.recyclerMyEvents);
        recyclerMyEvents.setLayoutManager(new LinearLayoutManager(this));
        if (auth.getCurrentUser() != null) {
            joinedEventsAdapter = new JoinedEventsAdapter(joinedEvents,
                    auth.getCurrentUser().getUid(), db);
            recyclerMyEvents.setAdapter(joinedEventsAdapter);
        }

        // Load existing profile info
        loadUserProfile();

        // Load joined events history (US 01.02.03)
        loadJoinedEvents();

        // Save profile changes
        btnSaveProfile.setOnClickListener(v -> saveUserProfile());

        // Delete profile
        btnDeleteProfile.setOnClickListener(v -> deleteProfile());
    }

    // Handle ActionBar back arrow
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // Load profile data from Firestore into EditTexts
    private void loadUserProfile() {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        etName.setText(document.getString("name"));
                        etEmail.setText(document.getString("email"));
                        etPhone.setText(document.getString("phoneNumber"));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    // Save/update profile (merge so we don't wipe other fields like role, createdAt)
    private void saveUserProfile() {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("phoneNumber", phone);

        db.collection("users").document(userId)
                .set(updates, SetOptions.merge())   // keep other fields intact
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show());
    }

    // US 01.02.03 – load events this user has joined (waitingList contains userId)
    private void loadJoinedEvents() {
        if (auth.getCurrentUser() == null || joinedEventsAdapter == null) return;
        String userId = auth.getCurrentUser().getUid();

        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    joinedEvents.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        // For each event, check if user is in its waitingList subcollection
                        DocumentReference waitingRef = doc.getReference()
                                .collection("waitingList")
                                .document(userId);

                        Event event = doc.toObject(Event.class);
                        if (event == null) continue;
                        event.setEventId(doc.getId());

                        waitingRef.get().addOnSuccessListener(waitSnap -> {
                            if (waitSnap.exists()) {
                                // User is in this event's waiting list → add to history
                                joinedEvents.add(event);
                                joinedEventsAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load joined events", Toast.LENGTH_SHORT).show());
    }

    // Delete profile document from Firestore
    // US 01.02.04 – As an entrant, I want to delete my profile
    private void deleteProfile() {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Remove userId from all waiting lists in events (current implementation)
        db.collection("events").get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        // If you're using array-based waitingList, this is fine.
                        // If using subcollections only, you can adjust later.
                        if (document.contains("waitingList")) {
                            document.getReference()
                                    .update("waitingList", com.google.firebase.firestore.FieldValue.arrayRemove(userId));
                        }
                        // Also try subcollection cleanup for safety:
                        document.getReference()
                                .collection("waitingList")
                                .document(userId)
                                .delete();
                    }

                    // Delete the user document
                    db.collection("users").document(userId)
                            .delete()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();
                                // sign out
                                auth.signOut();

                                // Redirect to MainActivity
                                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to delete profile", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to clean up waiting lists", Toast.LENGTH_SHORT).show());
    }

    /**
     * Simple adapter to show joined events in Settings, with a Leave button.
     * When Leave is tapped:
     *  - removes user from event's waitingList subcollection
     *  - removes item from this list
     */
    private static class JoinedEventsAdapter extends RecyclerView.Adapter<JoinedEventsAdapter.JoinedEventViewHolder> {

        private final List<Event> events;
        private final String userId;
        private final FirebaseFirestore db;

        JoinedEventsAdapter(List<Event> events, String userId, FirebaseFirestore db) {
            this.events = events;
            this.userId = userId;
            this.db = db;
        }

        @NonNull
        @Override
        public JoinedEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_event, parent, false);
            return new JoinedEventViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull JoinedEventViewHolder holder, int position) {
            holder.bind(events.get(position));
        }

        @Override
        public int getItemCount() {
            return events.size();
        }

        class JoinedEventViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDescription, tvStatus;
            Button btnJoinLeave;

            JoinedEventViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvEventName);
                tvDescription = itemView.findViewById(R.id.tvEventDescription);
                tvStatus = itemView.findViewById(R.id.tvEventStatus);
                btnJoinLeave = itemView.findViewById(R.id.btnJoinLeave);
            }

            void bind(Event event) {
                tvName.setText(event.getName());
                tvDescription.setText(event.getDescription());
                tvStatus.setText("Status: " + (event.getStatus() != null ? event.getStatus() : "Joined"));

                btnJoinLeave.setText("Leave waiting list");
                btnJoinLeave.setOnClickListener(v -> {
                    // Delete from this event's waitingList/{userId}
                    db.collection("events")
                            .document(event.getEventId())
                            .collection("waitingList")
                            .document(userId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                int pos = getBindingAdapterPosition();
                                if (pos != RecyclerView.NO_POSITION) {
                                    events.remove(pos);
                                    notifyItemRemoved(pos);
                                }
                                Toast.makeText(itemView.getContext(),
                                        "Left " + event.getName(), Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(itemView.getContext(),
                                            "Failed to leave: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                });
            }
        }
    }
}
