package com.example.event_app.activities.entrant;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.event_app.R;
import com.example.event_app.activities.admin.AdminHomeActivity;
import com.example.event_app.activities.shared.ProfileSetupActivity;
import com.example.event_app.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


/**
 * SplashActivity – Displays the animated splash screen, initializes Firebase,
 * retrieves a unique device ID, and routes the user based on authentication
 * and profile setup status.
 *
 * Behavior:
 * • Shows fade-in animation for the "LuckySpot" title
 * • Authenticates anonymously if no user session exists
 * • Checks whether user profile exists
 * • Routes to AdminHomeActivity, MainActivity, or ProfileSetupActivity
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DURATION = 1500; // 1.5 seconds
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Make fully immersive
        makeFullScreen();

        // Animate app name text
        animateAppName();

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get unique device ID
        deviceId = getUniqueDeviceId();
        // Authenticate after splash duration
        new Handler(Looper.getMainLooper()).postDelayed(
                this::authenticateUser,
                SPLASH_DURATION
        );
    }

    /**
     * Enables fullscreen immersive mode for the splash screen,
     * hiding status and navigation UI elements.
     */
    private void makeFullScreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    /**
     * Applies a fade-in animation to the "LuckySpot" text.
     * Duration: 800 ms.
     *
     * Ensures safe execution by checking if the TextView exists.
     */
    private void animateAppName() {
        TextView appName = findViewById(R.id.tvAppName);
        if (appName != null) {
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(800);
            appName.startAnimation(fadeIn);
        }
    }

    /**
     * Retrieves a unique device identifier using ANDROID_ID.
     * Used to store device information when creating a new user profile.
     *
     * @return A unique device ID string for this Android device.
     */
    @SuppressLint("HardwareIds")
    private String getUniqueDeviceId() {
        return Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
    }

    /**
     * Determines whether a Firebase Auth user session already exists.
     * If logged in → checks profile.
     * If not logged in → signs in anonymously.
     */
    private void authenticateUser() {
        if (mAuth.getCurrentUser() != null) {
            checkUserProfile();
        } else {
            signInAnonymously();
        }
    }

    /**
     * Signs the user into Firebase anonymously.
     *
     * On success:
     * • Calls checkUserProfile() to determine next navigation step.
     *
     * On failure:
     * • Navigates directly to ProfileSetupActivity.
     */
    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnSuccessListener(authResult -> {
                    checkUserProfile();
                })
                .addOnFailureListener(e -> {
                    navigateToProfileSetup();
                });
    }

    /**
     * Retrieves the user's Firestore profile based on FirebaseAuth UID.
     *
     * Behavior:
     * • If profile exists → routeUserByRole(document)
     * • If profile does NOT exist → navigateToProfileSetup()
     * • If Firestore request fails → navigateToProfileSetup()
     */
    private void checkUserProfile() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        routeUserByRole(document);
                    } else {
                        navigateToProfileSetup();
                    }
                })
                .addOnFailureListener(e -> {
                    navigateToProfileSetup();
                });
    }

    /**
     * Routes user to the appropriate home screen depending on role.
     *
     * Admin → AdminHomeActivity
     * Regular user → MainActivity
     *
     * Applies fade-in/out transition and finishes the splash activity.
     *
     * @param document A Firestore snapshot containing the user's profile data.
     */
    private void routeUserByRole(DocumentSnapshot document) {
        User user = document.toObject(User.class);
        Intent intent;

        if (user != null && user.isAdmin()) {
            intent = new Intent(this, AdminHomeActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    /**
     * Navigates to the initial ProfileSetupActivity for first-time users.
     * Passes deviceId and userId (if available) via Intent extras.
     *
     * Applies fade-in/out transition and finishes the splash activity.
     */
    private void navigateToProfileSetup() {
        Intent intent = new Intent(this, ProfileSetupActivity.class);
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("userId", mAuth.getCurrentUser() != null ?
                mAuth.getCurrentUser().getUid() : null);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}