package com.example.event_app.activities.entrant;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.event_app.R;
import com.example.event_app.activities.admin.AdminHomeActivity;
import com.example.event_app.fragments.EventsFragment;
import com.example.event_app.fragments.HomeFragment;
import com.example.event_app.fragments.ProfileFragment;
import com.example.event_app.models.User;
import com.example.event_app.services.MyFirebaseMessagingService;
import com.example.event_app.utils.AccessibilityHelper;
import com.example.event_app.utils.FCMTokenManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;


/**
 * MainActivity - Entrant home screen with bottom navigation.
 *
 * Features:
 * • Home: Quick actions such as QR scanning and shortcuts
 * • Events: Browse and join events
 * • Profile: User information and settings
 * • Admin access: Profile screen shows admin panel if user is an admin
 * • FCM integration: Handles push notification permissions and token management
 *
 * This activity hosts three fragments and initializes the user's Firestore
 * information, notification permissions, and FCM token lifecycle.
 */
public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private User currentUser;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Request notification permission
        requestNotificationPermission();

        // Initialize FCM token
        FCMTokenManager.initializeFCMToken();

        // Load user data (for admin check) and check for cached FCM token
        loadCurrentUser();

        // Initialize bottom navigation
        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_events) {
                selectedFragment = new EventsFragment();
            } else if (itemId == R.id.navigation_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });

        // Load default fragment (Home)
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.navigation_home);
        }
    }

    /**
     * Loads the currently authenticated user's Firestore profile.
     *
     * <p>This is required for:
     * <ul>
     *     <li>Checking admin privileges</li>
     *     <li>Ensuring cached FCM tokens are synchronized</li>
     * </ul>
     *
     * <p>Once the user document is confirmed to exist, this method triggers
     * {@link MyFirebaseMessagingService#checkAndSaveCachedToken(android.content.Context)}
     * to handle token delivery if the device generated a token before login.
     */
    private void loadCurrentUser() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        currentUser = document.toObject(User.class);
                        //After confirming the user exists and is logged in,
                        // check if a token was generated while they were logged out and save it.
                        MyFirebaseMessagingService.checkAndSaveCachedToken(getApplicationContext());
                    }
                })
                .addOnFailureListener(e -> {
                });
    }

    /**
     * Requests Android 13+ POST_NOTIFICATIONS permission.
     *
     * <p>For devices running below Android 13, this method logs that no runtime
     * permission is needed. For Android 13 and above, it checks whether the
     * permission is granted and requests it if not.
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            } else {
                Log.d(TAG, "Notification permission already granted");
            }
        } else {
            Log.d(TAG, "Android version < 13, no notification permission needed");
        }
    }

    /**
     * Handles the result of the POST_NOTIFICATIONS permission request.
     *
     * @param requestCode  Identifier for the permission request.
     * @param permissions  The permissions requested.
     * @param grantResults Results for each requested permission.
     *
     * Logs whether the user granted or denied notification access. No user-facing
     * UI changes occur here—the app continues functioning normally regardless.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted");
            } else {
                Log.d(TAG, "Notification permission denied");
            }
        }
    }

    /**
     * Opens the admin dashboard.
     *
     * <p>Called from ProfileFragment when a user with admin privileges selects
     * the "Admin Panel" option. Launches {@link AdminHomeActivity}.
     */
    public void openAdminPanel() {
        Intent intent = new Intent(this, AdminHomeActivity.class);
        startActivity(intent);
    }

    /**
     * Returns the currently loaded User object.
     *
     * @return The logged-in {@link User}, or null if not yet loaded.
     *
     * <p>Fragments use this method to check role and personalize UI.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Checks whether the currently logged-in user is an admin.
     *
     * @return true if admin, false otherwise.
     *
     * <p>Used by ProfileFragment to show or hide admin options.
     */
    public boolean isCurrentUserAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    /**
     * Reloads the current user's Firestore document when returning to
     * MainActivity.
     *
     * <p>This ensures that:
     * <ul>
     *     <li>Role changes (e.g., promoted to admin) are reflected</li>
     *     <li>Updated FCM token caches are synchronized</li>
     * </ul>
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Reload user data when returning to activity
        loadCurrentUser();
    }
}