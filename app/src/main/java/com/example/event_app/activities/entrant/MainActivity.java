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
import com.example.event_app.services.MyFirebaseMessagingService; // 👈 NEW IMPORT
import com.example.event_app.utils.AccessibilityHelper;
import com.example.event_app.utils.FCMTokenManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;


/**
 * MainActivity - Main app with bottom navigation
 *
 * Features:
 * - Home: Quick actions (scan QR, shortcuts)
 * - Events: Browse and join events
 * - Profile: User settings and info
 * - Admin access: Via Profile menu (if user is admin)
 * - ✨ FCM: Push notification setup
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 100;

    private BottomNavigationView bottomNav;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // ✨ STEP 5: Request notification permission (Android 13+)
        requestNotificationPermission();

        // ✨ STEP 6: Initialize FCM token (This part is fine, but the logic in MyFirebaseMessagingService is now key)
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
     * Load current user to check admin status
     *
     * 💡 FIX: Now calls checkAndSaveCachedToken() as soon as user is confirmed to be logged in.
     */
    private void loadCurrentUser() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        currentUser = document.toObject(User.class);
                        Log.d(TAG, "User loaded, isAdmin: " + (currentUser != null && currentUser.isAdmin()));

                        // ✨ CRITICAL FIX: After confirming the user exists and is logged in,
                        // check if a token was generated while they were logged out and save it.
                        MyFirebaseMessagingService.checkAndSaveCachedToken(getApplicationContext());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user", e);
                });
    }

    /**
     * ✨ STEP 5: Request notification permission for Android 13+
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                Log.d(TAG, "Requesting notification permission...");
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
     * ✨ Handle permission request result
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ Notification permission granted");
            } else {
                Log.d(TAG, "❌ Notification permission denied");
            }
        }
    }

    /**
     * ✨ Open Admin Panel
     * Called from ProfileFragment when admin clicks the option
     */
    public void openAdminPanel() {
        Intent intent = new Intent(this, AdminHomeActivity.class);
        startActivity(intent);
    }

    /**
     * Get current user (for fragments to check admin status)
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Check if current user is admin
     */
    public boolean isCurrentUserAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload user data when returning to activity
        loadCurrentUser();
    }
}