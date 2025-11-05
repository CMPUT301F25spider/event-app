
package com.example.event_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.event_app.admin.AdminHomeActivity;
import com.example.event_app.utils.UserRole;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * SplashActivity - Handles device-based authentication
 *
 * Flow:
 * 1. Show splash screen (2 seconds)
 * 2. Get device ID
 * 3. Sign in anonymously to Firebase
 * 4. Check if user profile exists
 * 5. Route to ProfileSetup (new user) or Home (existing user)
 */
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DURATION = 2000; // 2 seconds

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get unique device ID (US 01.07.01)
        deviceId = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        Log.d(TAG, "Device ID: " + deviceId);

        // Wait for splash duration, then check authentication
        new Handler().postDelayed(() -> checkUserAuthentication(), SPLASH_DURATION);
    }

    /**
     * Step 1: Check if user is already signed in
     */
    private void checkUserAuthentication() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // User is signed in, check if profile exists
            Log.d(TAG, "User already signed in: " + currentUser.getUid());
            checkUserProfile(currentUser.getUid());
        } else {
            // No user signed in, sign in anonymously with device ID
            Log.d(TAG, "No user signed in, signing in anonymously...");
            signInAnonymously();
        }
    }

    /**
     * Step 2: Sign in anonymously (device-based auth)
     * Firebase Auth is used to get a unique user ID
     */
    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Anonymous sign-in successful");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserProfile(user.getUid());
                        }
                    } else {
                        Log.e(TAG, "❌ Anonymous sign-in failed", task.getException());
                        Toast.makeText(this, "Authentication failed. Please restart app.",
                                Toast.LENGTH_LONG).show();
                        // Still try to go to profile setup
                        navigateToProfileSetup();
                    }
                });
    }

    /**
     * Step 3: Check if user profile exists in Firestore
     */
    private void checkUserProfile(String userId) {
        Log.d(TAG, "Checking if profile exists for userId: " + userId);

        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Profile exists, get role and navigate
                            Log.d(TAG, "✅ Profile exists");
                            navigateBasedOnRole(document);
                        } else {
                            // No profile, go to setup
                            Log.d(TAG, "No profile found, going to setup");
                            navigateToProfileSetup();
                        }
                    } else {
                        Log.e(TAG, "Error checking profile", task.getException());
                        // On error, go to profile setup
                        navigateToProfileSetup();
                    }
                });
    }

    /**
     * Step 4: Navigate to Profile Setup for new users
     */
    private void navigateToProfileSetup() {
        Intent intent = new Intent(SplashActivity.this, ProfileSetupActivity.class);
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("userId", mAuth.getCurrentUser() != null ?
                mAuth.getCurrentUser().getUid() : null);
        startActivity(intent);
        finish();
    }

    /**
     * Step 5: Navigate based on user role for existing users
     */
    private void navigateBasedOnRole(DocumentSnapshot document) {
        // Get roles from document
        Object rolesObj = document.get("roles");

        Intent intent;

        // Check if user is admin first (highest priority)
        if (rolesObj instanceof java.util.List) {
            java.util.List<?> rolesList = (java.util.List<?>) rolesObj;

            if (rolesList.contains(UserRole.ADMIN)) {
                Log.d(TAG, "User is admin, going to AdminHomeActivity");
                intent = new Intent(this, AdminHomeActivity.class);
            } else if (rolesList.contains(UserRole.ORGANIZER)) {
                Log.d(TAG, "User is organizer, going to OrganizerHomeActivity");
                intent = new Intent(this, OrganizerHomeActivity.class);
            } else {
                // Default to entrant
                Log.d(TAG, "User is entrant, going to MainActivity");
                intent = new Intent(this, MainActivity.class);
            }
        } else {
            // No roles found, default to entrant
            Log.d(TAG, "No roles found, defaulting to MainActivity");
            intent = new Intent(this, MainActivity.class);
        }

        startActivity(intent);
        finish();
    }
}