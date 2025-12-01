package com.example.event_app.activities.entrant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.event_app.R;
import com.example.event_app.models.Event;
import com.example.event_app.models.GeolocationAudit;
import com.example.event_app.models.Notification;
import com.example.event_app.services.NotificationService;
import com.example.event_app.utils.AccessibilityHelper;
import com.example.event_app.utils.Navigator;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * EventDetailsActivity
 * Displays full event details for entrants, including poster, description,
 * organizer info, waiting list status, lottery information, and invitation actions.
 *
 * <p>Implements multiple user stories:
 * <ul>
 *   <li>US 01.01.01: Join waiting list (with geolocation capture)</li>
 *   <li>US 01.01.02: Leave waiting list</li>
 *   <li>US 01.05.02: Accept invitation</li>
 *   <li>US 01.05.03: Decline invitation</li>
 *   <li>US 01.06.01: View event from QR code</li>
 *   <li>US 01.05.04: Display total entrants count</li>
 *   <li>US 01.05.05: Show lottery selection criteria</li>
 *   <li>US 02.02.02: Capture location for organizer map view</li>
 * </ul>
 *
 * <p>Provides real-time updates via a Firestore snapshot listener.
 * Also logs geolocation data for privacy-compliant auditing.
 */
public class EventDetailsActivity extends AppCompatActivity {
    // UI Elements
    private ImageView ivPoster;
    private TextView tvEventName, tvDescription, tvOrganizer, tvLocation;
    private TextView tvEventDate, tvCapacity, tvWaitingListCount, tvInvitationStatus;
    private MaterialButton btnJoinWaitingList, btnLeaveWaitingList;
    private MaterialButton btnAcceptInvitation, btnDeclineInvitation;
    private MaterialCardView cardLocation, cardInvitation;
    //  Lottery info card
    private MaterialCardView cardLotteryInfo;
    private View loadingView, contentView, errorView;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private NotificationService notificationService;
    private FusedLocationProviderClient fusedLocationClient;

    //  Real-time listener for event updates
    private com.google.firebase.firestore.ListenerRegistration eventListener;

    // Data
    private String eventId;
    private Event event;
    private boolean isOnWaitingList = false;
    private boolean isSelected = false;
    private boolean hasAccepted = false;

    private static final String TAG = "EventDetailsActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Get event ID from intent
        eventId = getIntent().getStringExtra(Navigator.EXTRA_EVENT_ID);
        if (eventId == null) {
            Toast.makeText(this, "Error: No event ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        notificationService = new NotificationService();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Initialize views
        initViews();
        // Load event data
        loadEventDetails();
    }

    /**
     * Initializes all UI components, sets up click listeners for user actions,
     * and wires navigation, invitation buttons, and location card actions.
     *
     * <p>This method does not load data — it only binds views and listeners.
     */
    private void initViews() {
        // Views
        ivPoster = findViewById(R.id.ivEventPoster);
        tvEventName = findViewById(R.id.tvEventName);
        tvDescription = findViewById(R.id.tvEventDescription);
        tvOrganizer = findViewById(R.id.tvOrganizer);
        tvLocation = findViewById(R.id.tvLocation);
        tvEventDate = findViewById(R.id.tvEventDate);
        tvCapacity = findViewById(R.id.tvCapacity);
        tvWaitingListCount = findViewById(R.id.tvWaitingListCount);
        btnJoinWaitingList = findViewById(R.id.btnJoinWaitingList);
        btnLeaveWaitingList = findViewById(R.id.btnLeaveWaitingList);
        cardLocation = findViewById(R.id.cardLocation);
        loadingView = findViewById(R.id.loadingView);
        contentView = findViewById(R.id.contentView);
        errorView = findViewById(R.id.errorView);

        // Try to find invitation UI elements (add to layout if needed)
        tvInvitationStatus = findViewById(R.id.tvInvitationStatus);
        btnAcceptInvitation = findViewById(R.id.btnAcceptInvitation);
        btnDeclineInvitation = findViewById(R.id.btnDeclineInvitation);
        cardInvitation = findViewById(R.id.cardInvitation);

        // Lottery info card
        cardLotteryInfo = findViewById(R.id.cardLotteryInfo);

        // Button listeners
        btnJoinWaitingList.setOnClickListener(v -> joinWaitingList());
        btnLeaveWaitingList.setOnClickListener(v -> leaveWaitingList());

        if (btnAcceptInvitation != null) {
            btnAcceptInvitation.setOnClickListener(v -> showAcceptConfirmation());
        }
        if (btnDeclineInvitation != null) {
            btnDeclineInvitation.setOnClickListener(v -> showDeclineConfirmation());
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnRetry).setOnClickListener(v -> loadEventDetails());

        // Location card click listener
        cardLocation.setOnClickListener(v -> openLocationInMaps());

        // Lottery info card listener
        if (cardLotteryInfo != null) {
            cardLotteryInfo.setOnClickListener(v -> showLotteryInfoDialog());
        }
    }

    /**
     * Loads event details using a real-time Firestore snapshot listener.
     * Automatically updates the UI whenever the organizer modifies the event.
     *
     * <p>This method:
     * <ul>
     *   <li>Shows the loading view</li>
     *   <li>Attaches a Firestore listener</li>
     *   <li>Updates UI when event data changes</li>
     * </ul>
     *
     * Handles missing/invalid event IDs and Firestore errors.
     */
    private void loadEventDetails() {
        showLoading();

        // Remove old listener if exists
        if (eventListener != null) {
            eventListener.remove();
        }

        // Real-time listener - Updates automatically when event changes!
        eventListener = db.collection("events").document(eventId)
                .addSnapshotListener((document, error) -> {
                    if (error != null) {
                        showError("Failed to load event");
                        return;
                    }

                    if (document == null || !document.exists()) {
                        showError("Event not found");
                        return;
                    }

                    event = document.toObject(Event.class);
                    if (event != null) {
                        event.setId(document.getId());
                        checkIfOrganizer();
                        displayEventDetails();
                        checkUserStatus();
                    }
                });
    }

    /**
     * Populates UI fields with event information including:
     * name, description, organizer, date, capacity, poster image,
     * and waiting list count.
     *
     * <p>Also handles optional fields gracefully (e.g., missing poster or location).
     * Shows the content view after binding UI elements.
     */
    private void displayEventDetails() {
        tvEventName.setText(event.getName());

        tvDescription.setText(event.getDescription() != null ?
                event.getDescription() : "No description available");

        tvOrganizer.setText(event.getOrganizerName() != null ?
                event.getOrganizerName() : "Event Organizer");

        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            tvLocation.setText(event.getLocation());
            cardLocation.setVisibility(View.VISIBLE);
        } else {
            cardLocation.setVisibility(View.GONE);
        }

        if (event.getEventDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
            tvEventDate.setText(sdf.format(event.getEventDate()));
        } else if (event.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
            tvEventDate.setText(sdf.format(event.getDate()));
        }

        if (event.getCapacity() != null) {
            tvCapacity.setText("Capacity: " + event.getCapacity() + " spots");
        } else {
            tvCapacity.setText("Capacity: Unlimited");
        }

        int waitingCount = event.getWaitingList() != null ? event.getWaitingList().size() : 0;
        tvWaitingListCount.setText(waitingCount + (waitingCount == 1 ? " person" : " people") + " on waiting list");

        if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
            Glide.with(this)
                    .load(event.getPosterUrl())
                    .centerCrop()
                    .into(ivPoster);
        }

        showContent();
    }

    /**
     * Checks whether the current user is the organizer of this event.
     * If true, the user is redirected to the organizer event management screen
     * instead of the entrant view.
     *
     * <p>This prevents organizers from accidentally viewing the entrant interface.
     */
    private void checkIfOrganizer() {
        if (mAuth.getCurrentUser() == null || event == null) {
            return;
        }

        String currentUserId = mAuth.getCurrentUser().getUid();
        String organizerId = event.getOrganizerId();

        if (currentUserId.equals(organizerId)) {
            // User IS the organizer - redirect to management view
            Intent intent = new Intent(this, com.example.event_app.activities.organizer.OrganizerEventDetailsActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            startActivity(intent);
            finish();  // Close this activity so back button doesn't return here
        }
        // If not organizer, do nothing - continue showing entrant view
    }

    /**
     * Opens the event's location in Google Maps (preferred) or a browser fallback
     * if the Maps app is not available.
     *
     * <p>Handles missing or invalid location strings. Uses a geo URI query to allow
     * users to view or navigate to the event location.
     */
    private void openLocationInMaps() {
        if (event == null || event.getLocation() == null || event.getLocation().isEmpty()) {
            Toast.makeText(this, "No location available", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String location = event.getLocation();
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(location));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Uri browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(location));
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, browserUri);
                startActivity(browserIntent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not open maps", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Determines the current user's state with respect to the event:
     * <ul>
     *     <li>On waiting list</li>
     *     <li>Selected</li>
     *     <li>Accepted/Registered</li>
     *     <li>Declined (internal)</li>
     * </ul>
     *
     * <p>Also invokes data-integrity checks to ensure that the user is not
     * incorrectly listed in multiple states simultaneously.
     *
     * Updates UI buttons accordingly via {@link #updateButtonState()}.
     */
    private void checkUserStatus() {
        if (mAuth.getCurrentUser() == null) {
            isOnWaitingList = false;
            isSelected = false;
            hasAccepted = false;
            updateButtonState();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();

        isOnWaitingList = event.getWaitingList() != null && event.getWaitingList().contains(userId);
        isSelected = event.getSelectedList() != null && event.getSelectedList().contains(userId);
        hasAccepted = event.getSignedUpUsers() != null && event.getSignedUpUsers().contains(userId);

        // Validate data integrity: user should only be in ONE list
        validateUserListIntegrity(userId);

        updateButtonState();
    }

    /**
     * Validates that the user appears in at most one event list.
     * A user should only be in one of:
     * waitingList, selectedList, signedUpUsers, or declinedUsers.
     *
     * <p>If inconsistencies are detected (e.g., user appears in multiple lists),
     * a warning is shown and {@link #fixUserListCorruption(String)} is called.
     *
     * @param userId The ID of the user whose list membership is being checked.
     */
    private void validateUserListIntegrity(String userId) {
        int listCount = 0;
        StringBuilder listsFound = new StringBuilder();

        if (isOnWaitingList) {
            listCount++;
            listsFound.append("waiting list");
        }
        if (isSelected) {
            listCount++;
            if (listsFound.length() > 0) listsFound.append(", ");
            listsFound.append("selected list");
        }
        if (hasAccepted) {
            listCount++;
            if (listsFound.length() > 0) listsFound.append(", ");
            listsFound.append("signed up list");
        }

        // Also check declined list (not displayed but could cause issues)
        boolean isDeclined = event.getDeclinedUsers() != null && event.getDeclinedUsers().contains(userId);
        if (isDeclined) {
            listCount++;
            if (listsFound.length() > 0) listsFound.append(", ");
            listsFound.append("declined list");
        }

        if (listCount > 1) {
            Toast.makeText(this,
                    "Warning: Your registration status appears in multiple lists. Please contact support.",
                    Toast.LENGTH_LONG).show();
            fixUserListCorruption(userId);
        } else if (listCount == 0) {
            Log.d(TAG, "User not in any event lists - can join waiting list");
        } else {
            Log.d(TAG, "User status OK: in " + listsFound);
        }
    }

    /**
     * Attempts to automatically resolve data corruption where a user appears
     * in multiple event lists simultaneously.
     *
     * <p>Prioritizes lists in the following order:
     * <ol>
     *     <li>signedUpUsers (highest priority)</li>
     *     <li>selectedList</li>
     *     <li>waitingList</li>
     *     <li>declinedUsers (lowest priority)</li>
     * </ol>
     *
     * <p>Removes the user from inappropriate lists based on this priority.
     *
     * @param userId The ID of the affected user.
     */
    private void fixUserListCorruption(String userId) {
        Map<String, Object> updates = new HashMap<>();

        // Priority order: If accepted, remove from all other lists
        if (hasAccepted) {
            updates.put("waitingList", FieldValue.arrayRemove(userId));
            updates.put("selectedList", FieldValue.arrayRemove(userId));
            updates.put("declinedUsers", FieldValue.arrayRemove(userId));
        }
        // If selected but not accepted, remove from waiting list
        else if (isSelected) {
            updates.put("waitingList", FieldValue.arrayRemove(userId));
            updates.put("declinedUsers", FieldValue.arrayRemove(userId));
        }
        // If on waiting list, remove from selected/declined
        else if (isOnWaitingList) {
            updates.put("selectedList", FieldValue.arrayRemove(userId));
            updates.put("declinedUsers", FieldValue.arrayRemove(userId));
        }
        if (!updates.isEmpty()) {
            db.collection("events").document(eventId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Registration status corrected", Toast.LENGTH_SHORT).show();
                        loadEventDetails();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Could not fix registration status. Please contact support.", Toast.LENGTH_LONG).show();
                    });
        }
    }

    /**
     * Updates the visibility and enabled state of action buttons
     * (join, leave, accept, decline) based on the user's event status.
     *
     * <p>Supports the following UI flows:
     * <ul>
     *     <li>Joined waiting list → show "Leave"</li>
     *     <li>Selected → show "Accept" and "Decline"</li>
     *     <li>Accepted → show confirmation message only</li>
     *     <li>Not registered → show "Join waiting list"</li>
     * </ul>
     *
     * <p>Ensures that only appropriate actions appear for each workflow state.
     */
    private void updateButtonState() {
        // Hide all buttons first
        btnJoinWaitingList.setVisibility(View.GONE);
        btnLeaveWaitingList.setVisibility(View.GONE);
        if (btnAcceptInvitation != null) btnAcceptInvitation.setVisibility(View.GONE);
        if (btnDeclineInvitation != null) btnDeclineInvitation.setVisibility(View.GONE);
        if (cardInvitation != null) cardInvitation.setVisibility(View.GONE);

        if (hasAccepted) {
            // User has accepted - show status
            if (tvInvitationStatus != null) {
                tvInvitationStatus.setText("You're registered for this event!");
                tvInvitationStatus.setVisibility(View.VISIBLE);
            }
        } else if (isSelected) {
            // User is selected - show accept/decline buttons
            if (cardInvitation != null) {
                cardInvitation.setVisibility(View.VISIBLE);
            }
            if (tvInvitationStatus != null) {
                tvInvitationStatus.setText("You've been selected! Accept or decline your invitation:");
                tvInvitationStatus.setVisibility(View.VISIBLE);
            }
            if (btnAcceptInvitation != null) btnAcceptInvitation.setVisibility(View.VISIBLE);
            if (btnDeclineInvitation != null) btnDeclineInvitation.setVisibility(View.VISIBLE);
        } else if (isOnWaitingList) {
            // User is on waiting list
            btnLeaveWaitingList.setVisibility(View.VISIBLE);
        } else {
            // User can join waiting list
            btnJoinWaitingList.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Attempts to add the current user to the waiting list.
     *
     * <p>Implements:
     * <ul>
     *     <li>US 01.01.01 — Join waiting list</li>
     *     <li>US 02.02.02 — Capture location if required by the event</li>
     * </ul>
     *
     * <p>If the event requires geolocation, this method initiates the permission
     * check and location retrieval sequence. If geolocation is not required,
     * the user is added directly via {@link #addToWaitingList(String, android.location.Location)}.
     *
     * Disables the join button during processing to prevent duplicate actions.
     */
    private void joinWaitingList() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to join", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        btnJoinWaitingList.setEnabled(false);

        // Check if event requires geolocation
        if (event.isGeolocationEnabled()) {
            checkLocationPermissionAndJoin(userId);
        } else {
            // Just join without location
            addToWaitingList(userId, null);
        }
    }

    /**
     * Verifies whether fine location permission has been granted.
     *
     * <p>If permission is already granted, the method proceeds to retrieve the
     * user's current location and join the waiting list.
     * Otherwise, a permission request dialog is triggered.
     *
     * @param userId The ID of the user attempting to join the waiting list.
     */
    private void checkLocationPermissionAndJoin(String userId) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission granted - get location
            getCurrentLocationAndJoin(userId);
        } else {
            // Need to request permission
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    /**
     * Handles the result of the location permission request.
     *
     * <p>If permission is granted, location retrieval is attempted via
     * {@link #getCurrentLocationAndJoin(String)}.
     * If permission is denied, the user is shown an option to join without
     * location data via {@link #showLocationDeniedDialog(String)}.
     *
     * @param requestCode  The request code for permission verification.
     * @param permissions  The permissions requested.
     * @param grantResults The results for each requested permission.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (mAuth.getCurrentUser() == null) return;
            String userId = mAuth.getCurrentUser().getUid();

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted - get location
                getCurrentLocationAndJoin(userId);
            } else {
                // Permission denied - ask if they want to join without location
                showLocationDeniedDialog(userId);
            }
        }
    }

    /**
     * Displays a confirmation dialog when the user denies location access.
     *
     * <p>This dialog explains that the event requires location tracking but
     * allows the user to join the waiting list without providing location
     * data if they choose to proceed.
     *
     * @param userId The ID of the user attempting to join the waiting list.
     */
    private void showLocationDeniedDialog(String userId) {
        new AlertDialog.Builder(this)
                .setTitle("Location Permission Denied")
                .setMessage("This event requires location tracking. You can still join, but your location won't be recorded.\n\nJoin without location?")
                .setPositiveButton("Join Anyway", (dialog, which) -> {
                    addToWaitingList(userId, null);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    btnJoinWaitingList.setEnabled(true);
                })
                .show();
    }

    /**
     * Attempts to retrieve the user's current location using the
     * fused location provider.
     *
     * <p>If successful, the location is passed to
     * {@link #addToWaitingList(String, android.location.Location)}.
     * If unavailable or an error occurs, the method falls back to requesting
     * the last known location via {@link #getLastKnownLocationAndJoin(String)}.
     *
     * @param userId The ID of the user joining the waiting list.
     */
    private void getCurrentLocationAndJoin(String userId) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            addToWaitingList(userId, null);
            return;
        }

        Toast.makeText(this, "Getting your location...", Toast.LENGTH_SHORT).show();

        fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        new CancellationToken() {
                            @Override
                            public boolean isCancellationRequested() {
                                return false;
                            }

                            @Override
                            public CancellationToken onCanceledRequested(OnTokenCanceledListener listener) {
                                return this;
                            }
                        })
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        addToWaitingList(userId, location);
                    } else {
                        getLastKnownLocationAndJoin(userId);
                    }
                })
                .addOnFailureListener(e -> {
                    getLastKnownLocationAndJoin(userId);
                });
    }

    /**
     * Attempts to retrieve the device's last known location.
     *
     * <p>If a valid location is obtained, it is used when adding the user to
     * the waiting list. If not, the user is joined without location data.
     * This is a fallback when real-time location retrieval fails.
     *
     * @param userId The ID of the user joining the waiting list.
     */
    private void getLastKnownLocationAndJoin(String userId) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            addToWaitingList(userId, null);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        addToWaitingList(userId, location);
                    } else {
                        Toast.makeText(this, "Couldn't get location, joining without it", Toast.LENGTH_SHORT).show();
                        addToWaitingList(userId, null);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Couldn't get location, joining without it", Toast.LENGTH_SHORT).show();
                    addToWaitingList(userId, null);
                });
    }

    /**
     * Adds the user to the event waiting list and optionally stores their
     * geolocation if the event requires location tracking.
     *
     * <p>Implements:
     * <ul>
     *     <li>US 01.01.01 — Join waiting list</li>
     *     <li>US 02.02.02 — Save entrant geolocation for organizer map</li>
     * </ul>
     *
     * <p>If valid location data is provided, the user's latitude and longitude
     * are saved under `entrantLocations.{userId}`.
     *
     * <p>After saving, a notification is sent to the user and the UI is refreshed.
     *
     * @param userId   The ID of the user joining.
     * @param location The user's location, or {@code null} if unavailable.
     */
    private void addToWaitingList(String userId, Location location) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("waitingList", FieldValue.arrayUnion(userId));

        // If we have location and geolocation is enabled, save it
        if (location != null && event.isGeolocationEnabled()) {
            Map<String, Object> locationData = new HashMap<>();
            locationData.put("latitude", location.getLatitude());
            locationData.put("longitude", location.getLongitude());

            String locationPath = "entrantLocations." + userId;
            updates.put(locationPath, locationData);
        }

        // Update Firebase
        db.collection("events").document(eventId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            "Joined waiting list!" + (location != null ? " 📍" : ""),
                            Toast.LENGTH_SHORT).show();

                    //Log geolocation access for audit (if location was captured)
                    if (location != null && event.isGeolocationEnabled()) {
                        logGeolocationAccess(userId, location);
                    }
                    // Send notification
                    notificationService.sendNotification(
                            userId,
                            eventId,
                            event.getName(),
                            Notification.TYPE_WAITLIST_JOINED,
                            "Joined Waiting List",
                            "You've successfully joined the waiting list for " + event.getName() + ". Good luck!",
                            null
                    );

                    loadEventDetails();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to join waiting list", Toast.LENGTH_SHORT).show();
                    btnJoinWaitingList.setEnabled(true);
                });
    }

    /**
     * Records an audit entry whenever a user's location is captured during
     * the waiting list registration process.
     *
     * <p>Implements privacy compliance by storing:
     * <ul>
     *     <li>User ID and name</li>
     *     <li>Event ID and event name</li>
     *     <li>Latitude and longitude</li>
     *     <li>Timestamp</li>
     *     <li>Action taken (e.g., "joined_waiting_list")</li>
     * </ul>
     *
     * <p>The audit entry is stored in the `geolocation_audits` collection.
     * Any failure is silently ignored since logging does not affect user flow.
     *
     * @param userId   The ID of the user whose location was captured.
     * @param location The captured location object.
     */
    private void logGeolocationAccess(String userId, Location location) {
        // Get user name from Firebase Auth (or use userId as fallback)
        String userName = mAuth.getCurrentUser() != null &&
                mAuth.getCurrentUser().getDisplayName() != null ?
                mAuth.getCurrentUser().getDisplayName() : userId;

        // Create audit ID
        String auditId = db.collection("geolocation_audits").document().getId();

        // Create audit record
        GeolocationAudit audit = new GeolocationAudit(
                auditId,
                userId,
                userName,
                eventId,
                event.getName(),
                location.getLatitude(),
                location.getLongitude(),
                new Date(),
                "joined_waiting_list"
        );

        // Save to Firebase
        db.collection("geolocation_audits")
                .document(auditId)
                .set(audit)
                .addOnSuccessListener(aVoid -> {
                })
                .addOnFailureListener(e -> {
                    // Don't show error to user - this is background logging
                });
    }

    /**
     * Removes the current user from the event's waiting list.
     *
     * <p>Implements:
     * <ul>
     *     <li>US 01.01.02 — Leave waiting list</li>
     * </ul>
     *
     * <p>The user is removed from the `waitingList` array in Firestore, and the
     * UI is updated to reflect the change. Displays an error message if the
     * operation fails.
     */
    private void leaveWaitingList() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        btnLeaveWaitingList.setEnabled(false);

        db.collection("events").document(eventId)
                .update("waitingList", FieldValue.arrayRemove(userId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Left waiting list", Toast.LENGTH_SHORT).show();
                    loadEventDetails();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to leave waiting list", Toast.LENGTH_SHORT).show();
                    btnLeaveWaitingList.setEnabled(true);
                });
    }

    /**
     * Displays a confirmation dialog allowing the user to accept their
     * invitation to the event.
     *
     * <p>Accepting the dialog triggers {@link #acceptInvitation()}, while
     * cancelling dismisses the dialog with no further action.
     */
    private void showAcceptConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Accept Invitation")
                .setMessage("Confirm your registration for " + event.getName() + "?")
                .setPositiveButton("Accept", (dialog, which) -> acceptInvitation())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Confirms the user's attendance for the event.
     *
     * <p>Implements:
     * <ul>
     *     <li>US 01.05.02 — Accept invitation</li>
     * </ul>
     *
     * <p>The method:
     * <ol>
     *     <li>Removes the user from the `selectedList`</li>
     *     <li>Adds the user to `signedUpUsers`</li>
     *     <li>Sends a confirmation notification</li>
     *     <li>Reloads event details with updated state</li>
     * </ol>
     *
     * <p>Buttons are temporarily disabled during the update to prevent
     * duplicate submissions.
     */
    private void acceptInvitation() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        if (btnAcceptInvitation != null) {
            btnAcceptInvitation.setEnabled(false);
        }
        if (btnDeclineInvitation != null) {
            btnDeclineInvitation.setEnabled(false);
        }

        db.collection("events").document(eventId)
                .update(
                        "selectedList", FieldValue.arrayRemove(userId),
                        "signedUpUsers", FieldValue.arrayUnion(userId)
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Registration confirmed!", Toast.LENGTH_SHORT).show();

                    notificationService.sendNotification(
                            userId,
                            eventId,
                            event.getName(),
                            Notification.TYPE_INVITATION_SENT,
                            "Registration Confirmed",
                            "You're all set for " + event.getName() + "! We're looking forward to seeing you!",
                            null
                    );

                    loadEventDetails();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to accept invitation", Toast.LENGTH_SHORT).show();
                    if (btnAcceptInvitation != null) btnAcceptInvitation.setEnabled(true);
                    if (btnDeclineInvitation != null) btnDeclineInvitation.setEnabled(true);
                });
    }

    /**
     * Displays a confirmation dialog asking the user to verify their decision
     * to decline an event invitation.
     *
     * <p>If confirmed, {@link #declineInvitation()} is executed. Declining an
     * invitation is irreversible and removes the user from selection.
     */
    private void showDeclineConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Decline Invitation")
                .setMessage("Are you sure you want to decline this invitation? This cannot be undone.")
                .setPositiveButton("Decline", (dialog, which) -> declineInvitation())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Declines the user's invitation for the event.
     *
     * <p>Implements:
     * <ul>
     *     <li>US 01.05.03 — Decline invitation</li>
     * </ul>
     *
     * <p>The method updates Firestore by:
     * <ol>
     *     <li>Removing the user from the `selectedList`</li>
     *     <li>Adding them to the `declinedUsers` list</li>
     * </ol>
     *
     * <p>A notification is sent to confirm the decline, and the activity
     * finishes afterward. Failure restores button interactivity.
     */
    private void declineInvitation() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        if (btnAcceptInvitation != null) {
            btnAcceptInvitation.setEnabled(false);
        }
        if (btnDeclineInvitation != null) {
            btnDeclineInvitation.setEnabled(false);
        }

        db.collection("events").document(eventId)
                .update(
                        "selectedList", FieldValue.arrayRemove(userId),
                        "declinedUsers", FieldValue.arrayUnion(userId)
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Invitation declined", Toast.LENGTH_SHORT).show();

                    notificationService.sendNotification(
                            userId,
                            eventId,
                            event.getName(),
                            Notification.TYPE_INVITATION_DECLINED,
                            "Invitation Declined",
                            "You've declined the invitation for " + event.getName() + ". Thanks for letting us know!",
                            null
                    );

                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to decline invitation", Toast.LENGTH_SHORT).show();
                    if (btnAcceptInvitation != null) btnAcceptInvitation.setEnabled(true);
                    if (btnDeclineInvitation != null) btnDeclineInvitation.setEnabled(true);
                });
    }

    /**
     * Displays a dialog explaining how the event's lottery system works.
     *
     * <p>Implements:
     * <ul>
     *     <li>US 01.05.05 — Show lottery selection criteria</li>
     * </ul>
     *
     * <p>The dialog includes:
     * <ul>
     *     <li>How entrants are selected</li>
     *     <li>Registration timeline</li>
     *     <li>Capacity and remaining spots</li>
     *     <li>Current waiting and selected counts</li>
     * </ul>
     *
     * <p>This information ensures transparency in lottery-based selection.
     */
    private void showLotteryInfoDialog() {
        // Build the lottery info message
        StringBuilder info = new StringBuilder();

        info.append("📋 How the Lottery Works\n\n");

        info.append("Selection Process:\n");
        info.append("• All entrants have an equal chance\n");
        info.append("• Winners selected randomly after registration closes\n");
        info.append("• Fair and transparent process\n");
        info.append("• No first-come-first-served advantage\n\n");

        info.append("Timeline:\n");
        if (event.getRegistrationEndDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            info.append("• Registration closes: ").append(sdf.format(event.getRegistrationEndDate())).append("\n");
            info.append("• Lottery runs after registration closes\n");
        } else {
            info.append("• Lottery runs when organizer decides\n");
        }
        info.append("• You'll be notified of results\n\n");

        info.append("✅ If You're Selected:\n");
        info.append("• You'll receive a notification\n");
        info.append("• Accept or decline your invitation\n");
        info.append("• If you decline, another entrant gets your spot\n\n");

        info.append("📊 Current Status:\n");
        int waitingCount = event.getWaitingList() != null ? event.getWaitingList().size() : 0;
        int selectedCount = event.getSelectedList() != null ? event.getSelectedList().size() : 0;

        info.append("• People on waiting list: ").append(waitingCount).append("\n");

        if (event.getCapacity() != null) {
            info.append("• Total spots available: ").append(event.getCapacity()).append("\n");
            info.append("• Already selected: ").append(selectedCount).append("\n");
            int remaining = event.getCapacity().intValue() - selectedCount;
            info.append("• Spots remaining: ").append(Math.max(0, remaining)).append("\n");
        } else {
            info.append("• Capacity: Unlimited\n");
        }

        info.append("\n Equal opportunity for everyone!");

        // Show dialog
        new AlertDialog.Builder(this)
                .setTitle("Lottery Selection Criteria")
                .setMessage(info.toString())
                .setPositiveButton("Got it!", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    /**
     * Displays the loading view while hiding the main content and error views.
     *
     * <p>Used when fetching event details or performing slow operations.
     */
    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
    }

    /**
     * Displays the main content view after data has successfully loaded.
     *
     * <p>Hides both the loading view and the error view.
     */
    private void showContent() {
        loadingView.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
    }

    /**
     * Displays an error state with a message explaining what went wrong.
     *
     * <p>Hides the loading and content views. The message is shown in the
     * dedicated error TextView.
     *
     * @param message The error message to display to the user.
     */
    private void showError(String message) {
        loadingView.setVisibility(View.GONE);
        contentView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);

        TextView tvError = findViewById(R.id.tvError);
        if (tvError != null) {
            tvError.setText(message);
        }
    }

    /**
     * Cleans up resources before the activity is destroyed.
     *
     * <p>Specifically removes the Firestore real-time listener to prevent:
     * <ul>
     *     <li>Memory leaks</li>
     *     <li>Orphaned listeners</li>
     *     <li>Unwanted UI updates when activity is no longer active</li>
     * </ul>
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        //  Clean up real-time listener to prevent memory leaks
        if (eventListener != null) {
            eventListener.remove();
            eventListener = null;
        }
    }
}