package com.example.event_app.activities.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.event_app.R;
import com.example.event_app.models.Event;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Displays detailed information for a single event, including poster,
 * description, organizer details, event date, capacity, entrant count,
 * and cancellation-rate warnings. Administrators may also permanently
 * delete the event from the system.
 *
 * <p>Supports:
 * <ul>
 *   <li><b>US 03.01.01</b> – Remove events</li>
 *   <li><b>US 03.04.01</b> – Browse events (detailed view)</li>
 * </ul>
 *
 * The activity loads the event from Firestore using an event ID passed
 * through the intent and renders all available data. Deletion actions are
 * irreversible and require confirmation via dialog.
 */
public class AdminEventDetailsActivity extends AppCompatActivity {
    public static final String EXTRA_EVENT_ID = "event_id";

    // Views
    private CardView cardPoster;
    private ImageView ivEventPoster;
    private TextView tvDetailEventName;
    private TextView tvDetailStatus;
    private TextView tvDetailDescription;
    private TextView tvDetailOrganizer;
    private TextView tvDetailEventDate;
    private TextView tvDetailEntrantCount;
    private TextView tvDetailCapacity;
    private TextView tvDetailEventId;
    private CardView cardWarning;
    private TextView tvWarningMessage;
    private LinearLayout layoutCapacity;
    private MaterialButton btnDeleteEvent;

    // Firebase
    private FirebaseFirestore db;

    // Data
    private Event currentEvent;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_event_details);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Event Details");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Get event ID from intent
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Error: Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // Initialize views
        initViews();
        // Set up delete button
        setupDeleteButton();
        // Load event data
        loadEventDetails();
    }

    /**
     * Initializes all UI components used to display event details, including
     * text views, image cards, warning cards, and the delete button.
     */
    private void initViews() {
        cardPoster = findViewById(R.id.cardPoster);
        ivEventPoster = findViewById(R.id.ivEventPoster);
        tvDetailEventName = findViewById(R.id.tvDetailEventName);
        tvDetailStatus = findViewById(R.id.tvDetailStatus);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvDetailOrganizer = findViewById(R.id.tvDetailOrganizer);
        tvDetailEventDate = findViewById(R.id.tvDetailEventDate);
        tvDetailEntrantCount = findViewById(R.id.tvDetailEntrantCount);
        tvDetailCapacity = findViewById(R.id.tvDetailCapacity);
        tvDetailEventId = findViewById(R.id.tvDetailEventId);
        cardWarning = findViewById(R.id.cardWarning);
        tvWarningMessage = findViewById(R.id.tvWarningMessage);
        layoutCapacity = findViewById(R.id.layoutCapacity);
        btnDeleteEvent = findViewById(R.id.btnDeleteEvent);
    }

    /**
     * Attaches a click listener to the delete button. When triggered, it opens
     * a confirmation dialog to prevent accidental event deletion.
     */
    private void setupDeleteButton() {
        btnDeleteEvent.setOnClickListener(v -> showDeleteConfirmation());
    }

    /**
     * Fetches the event document from Firestore using the event ID provided
     * through the activity intent. If the document exists, it is converted
     * into an {@link Event} object and passed to {@link #displayEventDetails()}.
     *
     * Displays an error and closes the activity if the event cannot be loaded.
     */
    private void loadEventDetails() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    currentEvent = documentSnapshot.toObject(Event.class);
                    if (currentEvent != null) {
                        currentEvent.setEventId(documentSnapshot.getId());
                        displayEventDetails();
                    } else {
                        Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * Renders all event fields into the UI, including name, status, organizer,
     * description, event date, entrant count, capacity, and poster image.
     *
     * <p>Conditional UI behavior:
     * <ul>
     *     <li>Hides capacity layout if capacity is null or zero</li>
     *     <li>Shows a warning card if cancellation rate is high</li>
     *     <li>Hides poster card if no poster URL is provided</li>
     * </ul>
     *
     * Fields are displayed with fallbacks when values are missing.
     */
    private void displayEventDetails() {
        // Event name
        tvDetailEventName.setText(currentEvent.getName());

        // Status
        String status = currentEvent.getStatus() != null
                ? currentEvent.getStatus().toUpperCase()
                : "ACTIVE";
        tvDetailStatus.setText(status);

        // Status color
        int statusColor;
        switch (status.toLowerCase()) {
            case "active":
                statusColor = android.R.color.holo_green_dark;
                break;
            case "inactive":
                statusColor = android.R.color.darker_gray;
                break;
            case "completed":
                statusColor = android.R.color.holo_blue_dark;
                break;
            default:
                statusColor = android.R.color.holo_orange_dark;
        }
        tvDetailStatus.setBackgroundColor(getColor(statusColor));

        // Description
        String description = currentEvent.getDescription();
        if (description != null && !description.isEmpty()) {
            tvDetailDescription.setText(description);
        } else {
            tvDetailDescription.setText("No description provided");
        }

        // Organizer
        String organizer = currentEvent.getOrganizerName();
        if (organizer != null && !organizer.isEmpty()) {
            tvDetailOrganizer.setText(organizer);
        } else {
            tvDetailOrganizer.setText("Unknown");
        }

        // Event date
        Date eventDate = currentEvent.getEventDate();
        if (eventDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            tvDetailEventDate.setText(dateFormat.format(eventDate));
        } else {
            tvDetailEventDate.setText("No date specified");
        }

        // Entrant count
        tvDetailEntrantCount.setText(String.valueOf(currentEvent.getEntrantCount()));

        // Capacity (if available)
        Long capacity = currentEvent.getCapacity();
        if (capacity != null && capacity > 0) {
            layoutCapacity.setVisibility(View.VISIBLE);
            tvDetailCapacity.setText(String.valueOf(capacity));
        } else {
            layoutCapacity.setVisibility(View.GONE);
        }

        // Event ID
        tvDetailEventId.setText(currentEvent.getEventId());

        // Poster (if available)
        String posterUrl = currentEvent.getPosterUrl();
        if (posterUrl != null && !posterUrl.isEmpty()) {
            cardPoster.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(posterUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivEventPoster);
        } else {
            cardPoster.setVisibility(View.GONE);
        }

        // Warning for high cancellation
        if (currentEvent.hasHighCancellationRate()) {
            cardWarning.setVisibility(View.VISIBLE);
            tvWarningMessage.setText(String.format(
                    "This event has a high cancellation rate of %.1f%% (%d cancelled out of %d selected)",
                    currentEvent.getCancellationRate(),
                    currentEvent.getTotalCancelled(),
                    currentEvent.getTotalSelected()
            ));
        } else {
            cardWarning.setVisibility(View.GONE);
        }
    }

    /**
     * Displays a confirmation dialog warning the administrator that deleting
     * the event is permanent and will remove all associated data. If confirmed,
     * invokes {@link #deleteEvent()}.
     */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete \"" + currentEvent.getName() + "\"?\n\n" +
                        "This action cannot be undone. All event data, including entrants and waiting lists, will be permanently deleted.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Permanently deletes the event from Firestore. The delete button is disabled
     * during processing to avoid duplicate actions. On successful deletion, the
     * activity closes.
     *
     * Shows error messages and re-enables the button if the delete request fails.
     */
    private void deleteEvent() {
        // Show loading
        btnDeleteEvent.setEnabled(false);
        btnDeleteEvent.setText("Deleting...");

        db.collection("events")
                .document(eventId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();

                    // Return to previous screen
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting event: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    // Re-enable button
                    btnDeleteEvent.setEnabled(true);
                    btnDeleteEvent.setText("Delete Event");
                });
    }

    /**
     * Handles action-bar "up" navigation by closing the activity.
     *
     * @return true always, indicating the event was consumed
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
