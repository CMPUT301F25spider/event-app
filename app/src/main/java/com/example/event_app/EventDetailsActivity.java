package com.example.event_app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;

import com.bumptech.glide.Glide; // Needed to load images from internet
import com.example.event_app.models.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

public class EventDetailsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String eventId;
    private String currentUserId;
    private Event currentEvent;
    private ProgressBar progressBar;
    private Group contentGroup;
    private ImageView imageEventPoster;
    private TextView textEventName, textEventDescription;
    private Button buttonJoinEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUserId = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        progressBar = findViewById(R.id.progressBar_event_details);
        contentGroup = findViewById(R.id.content_group);
        imageEventPoster = findViewById(R.id.image_event_poster);
        textEventName = findViewById(R.id.text_event_name);
        textEventDescription = findViewById(R.id.text_event_description);
        buttonJoinEvent = findViewById(R.id.button_join_event);

        // Get event ID from QR scan
        eventId = getIntent().getStringExtra(Navigator.EXTRA_EVENT_ID);
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event ID is missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        fetchEventDetails();
    }

    /**
     * Fetches event data from Firestore and displays it.
     */
    private void fetchEventDetails() {
        showLoading(true);
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        showError("Event not found.");
                        return;
                    }
                    currentEvent = documentSnapshot.toObject(Event.class);
                    if (currentEvent == null) {
                        showError("Could not read event data.");
                        return;
                    }
                    currentEvent.setEventId(documentSnapshot.getId());
                    displayEventData();
                })
                .addOnFailureListener(e -> showError("Failed to load event: " + e.getMessage()));
    }

    /**
     * Populates the UI with the event data.
     */
    private void displayEventData() {
        textEventName.setText(currentEvent.getName());
        textEventDescription.setText(currentEvent.getDescription());
        if (currentEvent.getPosterUrl() != null && !currentEvent.getPosterUrl().isEmpty()) {
            Glide.with(this).load(currentEvent.getPosterUrl()).into(imageEventPoster);
        }
        updateJoinButtonState();
        buttonJoinEvent.setOnClickListener(v -> joinEventWaitingList());
        showLoading(false);
    }

    /**
     * Updates the "Join" button based on registration status and user status.
     */
    private void updateJoinButtonState() {
        Date now = new Date();

        // Check if user is already signed up or on the waiting list
        boolean isUserOnWaitingList = currentEvent.getWaitingList() != null && currentEvent.getWaitingList().contains(currentUserId);
        boolean isUserSignedUp = currentEvent.getSignedUpUsers() != null && currentEvent.getSignedUpUsers().contains(currentUserId);

        if (isUserSignedUp) {
            buttonJoinEvent.setText("You are Signed Up");
            buttonJoinEvent.setEnabled(false);
        } else if (isUserOnWaitingList) {
            buttonJoinEvent.setText("You are on the Waiting List");
            buttonJoinEvent.setEnabled(false);
        } else if (currentEvent.getRegistrationEndDate() != null && now.after(currentEvent.getRegistrationEndDate())) {
            buttonJoinEvent.setText("Registration Closed");
            buttonJoinEvent.setEnabled(false);
        } else if (currentEvent.getRegistrationStartDate() != null && now.before(currentEvent.getRegistrationStartDate())) {
            buttonJoinEvent.setText("Registration Not Yet Open");
            buttonJoinEvent.setEnabled(false);
        } else {
            buttonJoinEvent.setText("Join Waiting List");
            buttonJoinEvent.setEnabled(true);
        }
    }

    /**
     * Adds the current user to the event's waiting list in Firestore.
     */
    private void joinEventWaitingList() {
        if (currentUserId == null) {
            Toast.makeText(this, "You need to be logged in to join.", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonJoinEvent.setEnabled(false); // Prevent multiple clicks
        Toast.makeText(this, "Joining...", Toast.LENGTH_SHORT).show();

        db.collection("events").document(eventId)
                .update("waitingList", FieldValue.arrayUnion(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Successfully joined waiting list!", Toast.LENGTH_SHORT).show();
                    fetchEventDetails();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to join: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    buttonJoinEvent.setEnabled(true); // Re-enable button
                });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        contentGroup.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }
}
