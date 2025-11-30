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
 * EventDetailsActivity - View event details, join waiting list, accept/decline invitations
 *
 * US 01.01.01: Join waiting list (with location capture)
 * US 01.01.02: Leave waiting list
 * US 01.05.02: Accept invitation
 * US 01.05.03: Decline invitation
 * US 01.06.01: View event from QR code
 * US 01.05.04: See total entrants count
 * US 01.05.05: Show lottery selection criteria (NEW)
 * US 02.02.02: Capture location when joining (for organizer map view)
 *
 * UPDATED: Added geolocation audit logging for privacy compliance
 */
public class EventDetailsActivity extends AppCompatActivity {

    private static final String TAG = "EventDetailsActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // UI Elements
    private ImageView ivPoster;
    private TextView tvEventName, tvDescription, tvOrganizer, tvLocation;
    private TextView tvEventDate, tvCapacity, tvWaitingListCount, tvInvitationStatus;
    private MaterialButton btnJoinWaitingList, btnLeaveWaitingList;
    private MaterialButton btnAcceptInvitation, btnDeclineInvitation;
    private MaterialCardView cardLocation, cardInvitation;
    // ✨ NEW: Lottery info card
    private MaterialCardView cardLotteryInfo;
    private View loadingView, contentView, errorView;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private NotificationService notificationService;
    private FusedLocationProviderClient fusedLocationClient;

    // ✨ Real-time listener for event updates
    private com.google.firebase.firestore.ListenerRegistration eventListener;

    // Data
    private String eventId;
    private Event event;
    private boolean isOnWaitingList = false;
    private boolean isSelected = false;
    private boolean hasAccepted = false;

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

        // ✨ NEW: Lottery info card
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

        // ✨ NEW: Lottery info card listener
        if (cardLotteryInfo != null) {
            cardLotteryInfo.setOnClickListener(v -> showLotteryInfoDialog());
        }
    }

    /**
     * ✨ UPDATED: Real-time event details - Updates automatically!
     * If organizer changes event info, entrants see it instantly
     */
    private void loadEventDetails() {
        showLoading();

        // Remove old listener if exists
        if (eventListener != null) {
            eventListener.remove();
        }

        // ✨ Real-time listener - Updates automatically when event changes!
        eventListener = db.collection("events").document(eventId)
                .addSnapshotListener((document, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to event", error);
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

                        Log.d(TAG, "⚡ Real-time update: Event details refreshed");
                    }
                });
    }



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
     * ✨ FIX #4: Check if current user is organizer and auto-redirect
     * Organizers shouldn't see the entrant view - redirect to management
     */
    private void checkIfOrganizer() {
        if (mAuth.getCurrentUser() == null || event == null) {
            return;
        }

        String currentUserId = mAuth.getCurrentUser().getUid();
        String organizerId = event.getOrganizerId();

        if (currentUserId.equals(organizerId)) {
            // User IS the organizer - redirect to management view
            Log.d(TAG, "✅ User is organizer - redirecting to OrganizerEventDetailsActivity");

            Intent intent = new Intent(this, com.example.event_app.activities.organizer.OrganizerEventDetailsActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            startActivity(intent);
            finish();  // Close this activity so back button doesn't return here
        }
        // If not organizer, do nothing - continue showing entrant view
    }

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
            Log.e(TAG, "Error opening maps", e);
            Toast.makeText(this, "Could not open maps", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Check user's status: waiting list, selected, or accepted
     * UPDATED: Added data integrity validation to prevent users in multiple lists
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
     * Validate that user is only in ONE list at a time
     * This prevents data corruption where users appear in multiple lists
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
            // DATA CORRUPTION DETECTED
            Log.e(TAG, "DATA INTEGRITY ERROR: User " + userId + " is in " + listCount + " lists: " + listsFound);
            Toast.makeText(this,
                "Warning: Your registration status appears in multiple lists. Please contact support.",
                Toast.LENGTH_LONG).show();

            // Attempt to fix by prioritizing the most advanced state
            fixUserListCorruption(userId);
        } else if (listCount == 0) {
            Log.d(TAG, "User not in any event lists - can join waiting list");
        } else {
            Log.d(TAG, "User status OK: in " + listsFound);
        }
    }

    /**
     * Attempt to fix data corruption by prioritizing the correct list
     * Priority: signed up > selected > waiting list > declined
     */
    private void fixUserListCorruption(String userId) {
        Log.w(TAG, "Attempting to fix data corruption for user " + userId);

        Map<String, Object> updates = new HashMap<>();

        // Priority order: If accepted, remove from all other lists
        if (hasAccepted) {
            Log.d(TAG, "Fixing: Keeping user in signedUpUsers, removing from others");
            updates.put("waitingList", FieldValue.arrayRemove(userId));
            updates.put("selectedList", FieldValue.arrayRemove(userId));
            updates.put("declinedUsers", FieldValue.arrayRemove(userId));
        }
        // If selected but not accepted, remove from waiting list
        else if (isSelected) {
            Log.d(TAG, "Fixing: Keeping user in selectedList, removing from others");
            updates.put("waitingList", FieldValue.arrayRemove(userId));
            updates.put("declinedUsers", FieldValue.arrayRemove(userId));
        }
        // If on waiting list, remove from selected/declined
        else if (isOnWaitingList) {
            Log.d(TAG, "Fixing: Keeping user in waitingList, removing from others");
            updates.put("selectedList", FieldValue.arrayRemove(userId));
            updates.put("declinedUsers", FieldValue.arrayRemove(userId));
        }

        // Apply fixes to Firebase
        if (!updates.isEmpty()) {
            db.collection("events").document(eventId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Data corruption fixed successfully");
                    Toast.makeText(this, "Registration status corrected", Toast.LENGTH_SHORT).show();
                    loadEventDetails(); // Reload to reflect fixes
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fix data corruption", e);
                    Toast.makeText(this, "Could not fix registration status. Please contact support.", Toast.LENGTH_LONG).show();
                });
        }
    }

    /**
     * Update UI based on user's status
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
                tvInvitationStatus.setText("✅ You're registered for this event!");
                tvInvitationStatus.setVisibility(View.VISIBLE);
            }
        } else if (isSelected) {
            // User is selected - show accept/decline buttons
            if (cardInvitation != null) {
                cardInvitation.setVisibility(View.VISIBLE);
            }
            if (tvInvitationStatus != null) {
                tvInvitationStatus.setText("🎉 You've been selected! Accept or decline your invitation:");
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
     * US 01.01.01: Join waiting list with optional location capture
     * US 02.02.02: Capture location for organizer map view
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
            Log.d(TAG, "📍 Geolocation required - checking permissions");
            checkLocationPermissionAndJoin(userId);
        } else {
            // Just join without location
            Log.d(TAG, "No geolocation required - joining directly");
            addToWaitingList(userId, null);
        }
    }

    /**
     * Check location permission and request if needed
     */
    private void checkLocationPermissionAndJoin(String userId) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission granted - get location
            Log.d(TAG, "✅ Location permission granted");
            getCurrentLocationAndJoin(userId);
        } else {
            // Need to request permission
            Log.d(TAG, "⚠️ Requesting location permission");
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    /**
     * Handle permission request result
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (mAuth.getCurrentUser() == null) return;
            String userId = mAuth.getCurrentUser().getUid();

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted - get location
                Log.d(TAG, "✅ User granted location permission");
                getCurrentLocationAndJoin(userId);
            } else {
                // Permission denied - ask if they want to join without location
                Log.w(TAG, "⚠️ User denied location permission");
                showLocationDeniedDialog(userId);
            }
        }
    }

    /**
     * Show dialog when location permission is denied
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
     * Get current location and join waiting list
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
                        Log.d(TAG, "✅ Got location: " + location.getLatitude() + ", " + location.getLongitude());
                        addToWaitingList(userId, location);
                    } else {
                        Log.w(TAG, "⚠️ Location is null - trying last known location");
                        getLastKnownLocationAndJoin(userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to get current location", e);
                    getLastKnownLocationAndJoin(userId);
                });
    }

    /**
     * Fallback: Try to get last known location
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
                        Log.d(TAG, "✅ Got last known location: " + location.getLatitude() + ", " + location.getLongitude());
                        addToWaitingList(userId, location);
                    } else {
                        Log.w(TAG, "⚠️ No location available - joining without location");
                        Toast.makeText(this, "Couldn't get location, joining without it", Toast.LENGTH_SHORT).show();
                        addToWaitingList(userId, null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to get last known location", e);
                    addToWaitingList(userId, null);
                });
    }

    /**
     * Add user to waiting list with optional location
     * UPDATED: Now logs geolocation access for privacy compliance audit
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

            Log.d(TAG, "📍 Saving location for user: " + userId +
                    " at (" + location.getLatitude() + ", " + location.getLongitude() + ")");
        }

        // Update Firebase
        db.collection("events").document(eventId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Successfully joined waiting list" +
                            (location != null ? " with location" : ""));
                    Toast.makeText(this,
                            "Joined waiting list!" + (location != null ? " 📍" : ""),
                            Toast.LENGTH_SHORT).show();

                    // NEW: Log geolocation access for audit (if location was captured)
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
                    Log.e(TAG, "❌ Error joining waiting list", e);
                    Toast.makeText(this, "Failed to join waiting list", Toast.LENGTH_SHORT).show();
                    btnJoinWaitingList.setEnabled(true);
                });
    }

    /**
     * NEW: Log geolocation access for privacy compliance audit
     * Records when and where a user's location was captured for event registration
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
                    Log.d(TAG, "📋 Geolocation access logged for audit (Privacy Compliance)");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error logging geolocation audit", e);
                    // Don't show error to user - this is background logging
                });
    }

    /**
     * US 01.01.02: Leave waiting list
     */
    private void leaveWaitingList() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        btnLeaveWaitingList.setEnabled(false);

        db.collection("events").document(eventId)
                .update("waitingList", FieldValue.arrayRemove(userId))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Left waiting list");
                    Toast.makeText(this, "Left waiting list", Toast.LENGTH_SHORT).show();
                    loadEventDetails();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error leaving waiting list", e);
                    Toast.makeText(this, "Failed to leave waiting list", Toast.LENGTH_SHORT).show();
                    btnLeaveWaitingList.setEnabled(true);
                });
    }

    /**
     * Show confirmation dialog before accepting
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
     * US 01.05.02: Accept invitation with notification
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
                    Log.d(TAG, "Invitation accepted");
                    Toast.makeText(this, "Registration confirmed!", Toast.LENGTH_SHORT).show();

                    notificationService.sendNotification(
                            userId,
                            eventId,
                            event.getName(),
                            Notification.TYPE_INVITATION_SENT,
                            "✅ Registration Confirmed",
                            "You're all set for " + event.getName() + "! We're looking forward to seeing you!",
                            null
                    );

                    loadEventDetails();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error accepting invitation", e);
                    Toast.makeText(this, "Failed to accept invitation", Toast.LENGTH_SHORT).show();
                    if (btnAcceptInvitation != null) btnAcceptInvitation.setEnabled(true);
                    if (btnDeclineInvitation != null) btnDeclineInvitation.setEnabled(true);
                });
    }

    /**
     * Show confirmation dialog before declining
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
     * US 01.05.03: Decline invitation with notification
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
                    Log.d(TAG, "Invitation declined");
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
                    Log.e(TAG, "Error declining invitation", e);
                    Toast.makeText(this, "Failed to decline invitation", Toast.LENGTH_SHORT).show();
                    if (btnAcceptInvitation != null) btnAcceptInvitation.setEnabled(true);
                    if (btnDeclineInvitation != null) btnDeclineInvitation.setEnabled(true);
                });
    }

    /**
     * ✨ US 01.05.05: Show lottery selection criteria
     */
    private void showLotteryInfoDialog() {
        // Build the lottery info message
        StringBuilder info = new StringBuilder();

        info.append("📋 How the Lottery Works\n\n");

        info.append("🎲 Selection Process:\n");
        info.append("• All entrants have an equal chance\n");
        info.append("• Winners selected randomly after registration closes\n");
        info.append("• Fair and transparent process\n");
        info.append("• No first-come-first-served advantage\n\n");

        info.append("📅 Timeline:\n");
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

        info.append("\n💡 Equal opportunity for everyone!");

        // Show dialog
        new AlertDialog.Builder(this)
                .setTitle("Lottery Selection Criteria")
                .setMessage(info.toString())
                .setPositiveButton("Got it!", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
    }

    private void showContent() {
        loadingView.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
    }

    private void showError(String message) {
        loadingView.setVisibility(View.GONE);
        contentView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);

        TextView tvError = findViewById(R.id.tvError);
        if (tvError != null) {
            tvError.setText(message);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ✨ Clean up real-time listener to prevent memory leaks
        if (eventListener != null) {
            eventListener.remove();
            eventListener = null;
            Log.d(TAG, "✅ Event listener cleaned up");
        }
    }
}