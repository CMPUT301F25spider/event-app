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
 * ProfileFragment - User profile with stats and organized actions
 *
 * Features:
 * - Sticky header (Profile + Settings)
 * - User info display
 * - Tappable quick stats (waiting, selected, attending counts)
 * - Single "My Events" button
 * - Organizer actions (always visible)
 *
 * US 01.02.02: Update profile information
 * US 01.02.03: View event history
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

    // NEW: Accessibility helper
    private AccessibilityHelper accessibilityHelper;

    // NEW: Admin unlock Easter egg
    private int tapCount = 0;
    private long lastTapTime = 0;
    private static final String ADMIN_SECRET_CODE = "1234";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

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
     * Initialize all views
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

        // NEW: Settings elements
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
     * Setup all click listeners
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

        // NEW: Notifications toggle
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentUser != null) {
                updateNotificationPreference(isChecked);
            }
        });

        // NEW: Accessibility expand/collapse
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

        // NEW: Accessibility toggles
        initAccessibilitySwitches();

        // NEW: Admin unlock
        if (btnUnlockAdmin != null) {
            btnUnlockAdmin.setOnClickListener(v -> showAdminCodeDialog());
        }

        // NEW: App version (Easter egg - tap 7 times)
        if (layoutAppVersion != null) {
            layoutAppVersion.setOnClickListener(v -> handleVersionTap());
        }

        // NEW: Terms & Conditions
        if (btnTermsConditions != null) {
            btnTermsConditions.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Terms & Conditions", Toast.LENGTH_SHORT).show();
                // TODO: Open terms page
            });
        }

        // NEW: Privacy Policy
        if (btnPrivacyPolicy != null) {
            btnPrivacyPolicy.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Privacy Policy", Toast.LENGTH_SHORT).show();
                // TODO: Open privacy page
            });
        }
    }

    /**
     * Load user profile and calculate stats
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
     * Display user information
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
     * Load event statistics for this user
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
     * Display statistics
     */
    private void displayStats() {
        // Update counts in stats boxes
        tvWaitingCount.setText(String.valueOf(waitingCount));
        tvSelectedCount.setText(String.valueOf(selectedCount));
        tvAttendingCount.setText(String.valueOf(attendingCount));
    }

    /**
     * Show loading indicator
     */
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Hide loading indicator
     */
    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    /**
     * NEW: Initialize accessibility switches
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
     * NEW: Show restart dialog
     */
    private void showRestartDialog(String feature) {
        new AlertDialog.Builder(requireContext())
                .setTitle(feature + " Enabled")
                .setMessage("Changes will take full effect when you restart the app.")
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Update notification preference
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
     * Hidden admin unlock - tap version 3 times rapidly
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
     * Show admin code entry dialog
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
     * Verify admin code and grant access
     */
    private void verifyAdminCode(String enteredCode) {
        if (enteredCode.equals(ADMIN_SECRET_CODE)) {
            grantAdminAccess();
        } else {
            Toast.makeText(requireContext(), "Invalid admin code", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Grant admin access to current user
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
     * NEW: Update admin UI based on user role
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


    @Override
    public void onResume() {
        super.onResume();
        // Reload profile when returning to this fragment
        loadUserProfile();
    }
}