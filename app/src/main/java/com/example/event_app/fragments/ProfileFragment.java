package com.example.event_app.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.event_app.R;
import com.example.event_app.activities.admin.AdminHomeActivity;
import com.example.event_app.activities.entrant.MyEventsActivity;
import com.example.event_app.activities.entrant.SettingsActivity;
import com.example.event_app.activities.organizer.CreateEventActivity;
import com.example.event_app.activities.organizer.OrganizerEventsActivity;
import com.example.event_app.activities.shared.ProfileSetupActivity;
import com.example.event_app.models.Event;
import com.example.event_app.models.User;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * ProfileFragment - Displays user profile, statistics, and app settings.
 *
 * Features:
 * <ul>
 *     <li>User info and editable profile</li>
 *     <li>Tappable event statistics (waiting, selected, attending)</li>
 *     <li>Organizer actions (create event, view organized events)</li>
 *     <li>Notification toggle</li>
 *     <li>Accessibility settings (large text, contrast, touch targets)</li>
 *     <li>Hidden admin unlock via secret code</li>
 *     <li>App version Easter egg</li>
 * </ul>
 *
 * Supports:
 * US 01.02.02 - Update profile information
 * US 01.02.03 - View event history
 */
public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    // UI Components - User Info
    private ImageButton btnSettings;
    private TextView tvProfileName, tvProfileEmail, tvProfileRole;
    private MaterialButton btnEditProfile;

    // UI Components - Stats (Tappable)
    private LinearLayout statsWaiting, statsSelected, statsAttending;
    private TextView tvWaitingCount, tvSelectedCount, tvAttendingCount;

    // UI Components - Actions
    private LinearLayout btnMyEvents;
    private LinearLayout btnCreateEvent;
    private LinearLayout btnMyOrganizedEvents;

    // Loading
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;



    // Data
    private User currentUser;
    private int waitingCount = 0;
    private int selectedCount = 0;
    private int attendingCount = 0;

    // NEW: Settings elements
    private SwitchMaterial switchNotifications;
    private LinearLayout btnAccessibility, layoutAccessibilityOptions;
    private TextView tvAccessibilityArrow;
    private SwitchMaterial switchLargeText, switchHighContrast, switchLargeButtons;
    private LinearLayout layoutAdminSection;
    private MaterialButton btnUnlockAdmin;
    private TextView tvAdminStatus;
    private LinearLayout layoutAppVersion;
    private TextView tvAppVersion, btnTermsConditions, btnPrivacyPolicy;

    // Accessibility helper
    private AccessibilityHelper accessibilityHelper;

    // Admin unlock Easter egg
    private int tapCount = 0;
    private long lastTapTime = 0;
    private static final String ADMIN_SECRET_CODE = "1234";

    /**
     * Inflates the ProfileFragment layout.
     *
     * @param inflater LayoutInflater used to inflate the fragment UI
     * @param container Optional parent view
     * @param savedInstanceState Previously saved fragment state
     * @return The inflated view for the profile screen
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    /**
     * Initializes Firebase, binds views, attaches listeners, and loads user data.
     *
     * @param view Root fragment view
     * @param savedInstanceState Previously saved state
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initViews(view);

        // Setup listeners
        setupListeners();

        // Load user data
        loadUserProfile();
    }

    /**
     * Initializes all UI components including:
     * <ul>
     *     <li>User info</li>
     *     <li>Stats</li>
     *     <li>Event actions</li>
     *     <li>Settings toggles</li>
     *     <li>Accessibility switches</li>
     *     <li>Admin unlock section</li>
     * </ul>
     *
     * @param view Root view containing UI elements
     */
    private void initViews(View view) {
        // User Info
        btnSettings = view.findViewById(R.id.btnSettings);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfileRole = view.findViewById(R.id.tvProfileRole);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        // Stats (Tappable)
        statsWaiting = view.findViewById(R.id.statsWaiting);
        statsSelected = view.findViewById(R.id.statsSelected);
        statsAttending = view.findViewById(R.id.statsAttending);
        tvWaitingCount = view.findViewById(R.id.tvWaitingCount);
        tvSelectedCount = view.findViewById(R.id.tvSelectedCount);
        tvAttendingCount = view.findViewById(R.id.tvAttendingCount);

        // Actions
        btnMyEvents = view.findViewById(R.id.btnMyEvents);
        btnCreateEvent = view.findViewById(R.id.btnCreateEvent);
        btnMyOrganizedEvents = view.findViewById(R.id.btnMyOrganizedEvents);

        // Loading
        progressBar = view.findViewById(R.id.progressBar);
        // Loading
        progressBar = view.findViewById(R.id.progressBar);

        // Settings elements
        switchNotifications = view.findViewById(R.id.switchNotifications);
        btnAccessibility = view.findViewById(R.id.btnAccessibility);
        layoutAccessibilityOptions = view.findViewById(R.id.layoutAccessibilityOptions);
        tvAccessibilityArrow = view.findViewById(R.id.tvAccessibilityArrow);
        switchLargeText = view.findViewById(R.id.switchLargeText);
        switchHighContrast = view.findViewById(R.id.switchHighContrast);
        switchLargeButtons = view.findViewById(R.id.switchLargeButtons);
        layoutAdminSection = view.findViewById(R.id.layoutAdminSection);
        btnUnlockAdmin = view.findViewById(R.id.btnUnlockAdmin);
        tvAdminStatus = view.findViewById(R.id.tvAdminStatus);
        layoutAppVersion = view.findViewById(R.id.layoutAppVersion);
        tvAppVersion = view.findViewById(R.id.tvAppVersion);
        btnTermsConditions = view.findViewById(R.id.btnTermsConditions);
        btnPrivacyPolicy = view.findViewById(R.id.btnPrivacyPolicy);

        // Initialize accessibility helper
        accessibilityHelper = new AccessibilityHelper(requireContext());
    }

    /**
     * Attaches click listeners for:
     * <ul>
     *     <li>Settings navigation</li>
     *     <li>Edit profile</li>
     *     <li>Stats navigation (waiting, selected, attending)</li>
     *     <li>My Events / Organized Events</li>
     *     <li>Create Event</li>
     *     <li>Notification toggle</li>
     *     <li>Accessibility panel expand/collapse</li>
     *     <li>Accessibility settings</li>
     *     <li>Admin unlock popup</li>
     *     <li>App version Easter egg</li>
     * </ul>
     */
    private void setupListeners() {
        // Settings
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SettingsActivity.class);
            startActivity(intent);
        });

        // Edit Profile
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SettingsActivity.class);
            startActivity(intent);
        });

        // Tappable Stats - Navigate to filtered views
        statsWaiting.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MyEventsActivity.class);
            intent.putExtra("FILTER", "waiting");
            startActivity(intent);
        });

        statsSelected.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MyEventsActivity.class);
            intent.putExtra("FILTER", "selected");
            startActivity(intent);
        });

        statsAttending.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MyEventsActivity.class);
            intent.putExtra("FILTER", "attending");
            startActivity(intent);
        });

        // My Events (all events - no filter)
        btnMyEvents.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MyEventsActivity.class);
            startActivity(intent);
        });

        // Create Event
        if (btnCreateEvent != null) {
            btnCreateEvent.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), CreateEventActivity.class);
                startActivity(intent);
            });
        }

        // My Organized Events
        btnMyOrganizedEvents.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), OrganizerEventsActivity.class);
            startActivity(intent);
        });

        // Notifications toggle
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentUser != null) {
                updateNotificationPreference(isChecked);
            }
        });

        // Accessibility expand/collapse
        btnAccessibility.setOnClickListener(v -> {
            if (layoutAccessibilityOptions.getVisibility() == View.GONE) {
                // Expand
                layoutAccessibilityOptions.setVisibility(View.VISIBLE);
                tvAccessibilityArrow.setText("▲");
            } else {
                // Collapse
                layoutAccessibilityOptions.setVisibility(View.GONE);
                tvAccessibilityArrow.setText("▼");
            }
        });

        // Accessibility toggles
        initAccessibilitySwitches();

        // Admin unlock
        if (btnUnlockAdmin != null) {
            btnUnlockAdmin.setOnClickListener(v -> showAdminCodeDialog());
        }

        // App version (Easter egg - tap 7 times)
        if (layoutAppVersion != null) {
            layoutAppVersion.setOnClickListener(v -> handleVersionTap());
        }

        // Terms & Conditions
        if (btnTermsConditions != null) {
            btnTermsConditions.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Terms & Conditions", Toast.LENGTH_SHORT).show();
                // TODO: Open terms page
            });
        }

        // Privacy Policy
        if (btnPrivacyPolicy != null) {
            btnPrivacyPolicy.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Privacy Policy", Toast.LENGTH_SHORT).show();
                // TODO: Open privacy page
            });
        }
    }

    /**
     * Loads the user's profile document from Firestore.
     * Displays user info and triggers event stats loading.
     *
     * Shows a loading indicator during fetch.
     */
    private void loadUserProfile() {
        // Check if user is signed in
        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        showLoading();

        // Load user info
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        currentUser = document.toObject(User.class);
                        if (currentUser != null) {
                            displayUserInfo();
                            // Load event stats
                            loadEventStats(userId);
                        }
                    } else {
                        hideLoading();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
                    hideLoading();
                });
    }

    /**
     * Populates UI fields with the user's profile data:
     * <ul>
     *     <li>Name</li>
     *     <li>Email</li>
     * </ul>
     *
     * Hides role display by design.
     */
    private void displayUserInfo() {
        // Name
        tvProfileName.setText(currentUser.getName() != null ? currentUser.getName() : "User");

        // Email
        tvProfileEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");

        // Hide role display - not needed
        tvProfileRole.setVisibility(View.GONE);
    }

    /**
     * Computes event participation statistics for the user by scanning
     * all active events:
     * <ul>
     *     <li>Waiting list count</li>
     *     <li>Selected list count</li>
     *     <li>Attending (signed-up) count</li>
     * </ul>
     *
     * Updates UI when finished and hides loading state.
     *
     * @param userId ID of the current user
     */
    private void loadEventStats(String userId) {
        // Load all active events
        db.collection("events")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    waitingCount = 0;
                    selectedCount = 0;
                    attendingCount = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = doc.toObject(Event.class);

                        // Check if user is in waiting list
                        if (event.getWaitingList() != null && event.getWaitingList().contains(userId)) {
                            waitingCount++;
                        }

                        // Check if user is selected
                        if (event.getSelectedList() != null && event.getSelectedList().contains(userId)) {
                            selectedCount++;
                        }

                        // Check if user is attending (signed up)
                        if (event.getSignedUpUsers() != null && event.getSignedUpUsers().contains(userId)) {
                            attendingCount++;
                        }
                    }
                    // Update UI
                    displayStats();
                    hideLoading();
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                });
    }

    /**
     * Updates the UI counters for waiting, selected, and attending event stats.
     */
    private void displayStats() {
        // Update counts in stats boxes
        tvWaitingCount.setText(String.valueOf(waitingCount));
        tvSelectedCount.setText(String.valueOf(selectedCount));
        tvAttendingCount.setText(String.valueOf(attendingCount));
    }

    /**
     * Shows the progress bar to indicate loading state.
     */
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Hides the progress bar when loading finishes.
     */
    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    /**
     * Initializes accessibility preference switches:
     * <ul>
     *     <li>Large text</li>
     *     <li>High contrast</li>
     *     <li>Large touch targets</li>
     * </ul>
     *
     * Loads stored settings and attaches listeners for updating preferences.
     */
    private void initAccessibilitySwitches() {
        if (switchLargeText == null || switchHighContrast == null || switchLargeButtons == null) {
            return;
        }

        // Load current settings
        switchLargeText.setChecked(accessibilityHelper.isLargeTextEnabled());
        switchHighContrast.setChecked(accessibilityHelper.isHighContrastEnabled());
        switchLargeButtons.setChecked(accessibilityHelper.isLargeButtonsEnabled());

        // Large Text toggle
        switchLargeText.setOnCheckedChangeListener((buttonView, isChecked) -> {
            accessibilityHelper.setLargeTextEnabled(isChecked);
            showRestartDialog("Large Text");
        });

        // High Contrast toggle
        switchHighContrast.setOnCheckedChangeListener((buttonView, isChecked) -> {
            accessibilityHelper.setHighContrastEnabled(isChecked);
            requireActivity().recreate();
        });

        // Larger Buttons toggle
        switchLargeButtons.setOnCheckedChangeListener((buttonView, isChecked) -> {
            accessibilityHelper.setLargeButtonsEnabled(isChecked);
            showRestartDialog("Larger Touch Targets");
        });
    }

    /**
     * Displays a dialog informing the user that the selected accessibility feature
     * will take full effect after restarting the app.
     *
     * @param feature Name of the feature activated
     */
    private void showRestartDialog(String feature) {
        new AlertDialog.Builder(requireContext())
                .setTitle(feature + " Enabled")
                .setMessage("Changes will take full effect when you restart the app.")
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Updates the user's notification preference in Firestore.
     *
     * @param enabled Whether push notifications are allowed
     */
    private void updateNotificationPreference(boolean enabled) {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .update("notificationsEnabled", enabled)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(),
                            enabled ? "Notifications enabled" : "Notifications disabled",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    switchNotifications.setChecked(!enabled);
                });
    }

    /**
     * Easter egg listener for app version text.
     * If tapped rapidly 3 times, triggers the admin code dialog.
     *
     * Also shows progress messages (e.g., "1 more tap to unlock").
     */
    private void handleVersionTap() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastTapTime > 1000) {
            tapCount = 0;
        }

        lastTapTime = currentTime;
        tapCount++;

        if (tapCount >= 3) {
            tapCount = 0;
            showAdminCodeDialog();
            Toast.makeText(requireContext(), "Developer mode activated! 🔓", Toast.LENGTH_SHORT).show();
        } else if (tapCount >= 2) {
            Toast.makeText(requireContext(), (3 - tapCount) + " more taps to unlock admin", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows a dialog prompting the user to enter the secret admin unlock code.
     * On submit, verifies the code.
     */
    private void showAdminCodeDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_admin_code, null);
        EditText etAdminCode = dialogView.findViewById(R.id.etAdminCode);

        new AlertDialog.Builder(requireContext())
                .setTitle("Enter Admin Code")
                .setView(dialogView)
                .setPositiveButton("Unlock", (dialog, which) -> {
                    String enteredCode = etAdminCode.getText().toString().trim();
                    verifyAdminCode(enteredCode);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Validates the entered admin unlock code.
     * If correct, grants admin access. Otherwise shows an error toast.
     *
     * @param enteredCode Code entered by the user
     */
    private void verifyAdminCode(String enteredCode) {
        if (enteredCode.equals(ADMIN_SECRET_CODE)) {
            grantAdminAccess();
        } else {
            Toast.makeText(requireContext(), "Invalid admin code", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Grants admin privileges to the current user:
     * <ul>
     *     <li>Adds "admin" to their roles</li>
     *     <li>Saves updated user document</li>
     *     <li>Updates admin UI</li>
     *     <li>Redirects user to AdminHomeActivity</li>
     * </ul>
     */
    private void grantAdminAccess() {
        if (currentUser == null || mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        btnUnlockAdmin.setEnabled(false);

        currentUser.addRole("admin");
        currentUser.setUpdatedAt(System.currentTimeMillis());

        db.collection("users").document(userId)
                .set(currentUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Admin access granted!", Toast.LENGTH_LONG).show();
                    updateAdminUI();

                    //Navigate to AdminHomeActivity
                    Intent intent = new Intent(requireContext(), AdminHomeActivity.class);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error granting admin access", Toast.LENGTH_SHORT).show();
                    btnUnlockAdmin.setEnabled(true);
                });
    }

    /**
     * Updates UI components based on whether the user has admin privileges.
     * Shows green status text and hides unlock button for admins.
     */
    private void updateAdminUI() {
        if (currentUser == null) return;

        if (currentUser.isAdmin()) {
            tvAdminStatus.setText("Admin privileges active");
            tvAdminStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            btnUnlockAdmin.setVisibility(View.GONE);
        } else {
            tvAdminStatus.setText("Admin access locked");
            tvAdminStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
            btnUnlockAdmin.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Reloads user profile whenever returning to this fragment,
     * ensuring settings and stats stay up-to-date.
     */
    @Override
    public void onResume() {
        super.onResume();
        // Reload profile when returning to this fragment
        loadUserProfile();
    }
}