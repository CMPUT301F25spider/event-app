package com.example.event_app.activities.shared;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.event_app.R;
import com.example.event_app.activities.entrant.MainActivity;
import com.example.event_app.models.User;
import com.example.event_app.utils.AccessibilityHelper;
import com.example.event_app.utils.UserRole;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * ProfileSetupActivity
 *
 * Onboarding screen where new users provide basic personal information
 * before accessing the app.
 *
 * Supports:
 * <ul>
 *   <li>US 01.02.01 – User enters name, email, phone number</li>
 *   <li>US 01.07.01 – Device-based identification</li>
 *   <li>Assigning default ENTRANT role</li>
 *   <li>Navigating to unified MainActivity after setup</li>
 * </ul>
 *
 * Flow:
 * <ol>
 *   <li>User enters personal details</li>
 *   <li>User profile is created in Firestore</li>
 *   <li>Device ID is linked for identification</li>
 *   <li>User is directed to main app</li>
 * </ol>
 */
public class ProfileSetupActivity extends AppCompatActivity {

    private static final String TAG = "ProfileSetupActivity";

    // UI Elements
    private TextInputEditText editTextName, editTextEmail, editTextPhone;
    private MaterialButton btnContinue;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data from SplashActivity
    private String deviceId;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get device ID and user ID from intent
        deviceId = getIntent().getStringExtra("deviceId");
        userId = getIntent().getStringExtra("userId");

        // Fallback to ANDROID_ID if not provided
        if (deviceId == null || deviceId.isEmpty()) {
            deviceId = android.provider.Settings.Secure.getString(
                    getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID
            );
            Log.w(TAG, "Device ID not provided, using ANDROID_ID: " + deviceId);
        }
        // Initialize views
        initViews();

        // Set up button click
        btnContinue.setOnClickListener(v -> handleContinue());
    }

    /**
     * Initializes all input fields and the continue button.
     */
    private void initViews() {
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhone = findViewById(R.id.editTextPhone);
        btnContinue = findViewById(R.id.btnContinue);
    }

    /**
     * Handles the Continue button click:
     * <ol>
     *   <li>Reads input values</li>
     *   <li>Validates name & email</li>
     *   <li>Disables the button to prevent duplicate submissions</li>
     *   <li>Creates the user profile in Firestore</li>
     * </ol>
     */
    private void handleContinue() {
        // Get input values
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(name, email)) {
            return;
        }

        // Disable button to prevent double-click
        btnContinue.setEnabled(false);

        // Create and save profile
        createUserProfile(name, email, phone);
    }

    /**
     * Validates required onboarding fields.
     * Ensures:
     * <ul>
     *   <li>Name is not empty and at least 2 characters</li>
     *   <li>Email is provided and matches a valid pattern</li>
     * </ul>
     *
     * @param name the user’s name
     * @param email the user’s email
     * @return true if all input fields are valid; false otherwise
     */
    private boolean validateInputs(String name, String email) {
        // Validate name
        if (TextUtils.isEmpty(name)) {
            editTextName.setError("Name is required");
            editTextName.requestFocus();
            return false;
        }

        if (name.length() < 2) {
            editTextName.setError("Name must be at least 2 characters");
            editTextName.requestFocus();
            return false;
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Please enter a valid email");
            editTextEmail.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Creates a new User profile in Firestore using the provided details.
     *
     * Behavior:
     * <ul>
     *   <li>Uses Firebase UID as the unique identifier</li>
     *   <li>Binds deviceId for device-based identification (US 01.07.01)</li>
     *   <li>Assigns ENTRANT role to every new user by default</li>
     *   <li>Includes phone only if provided</li>
     *   <li>On success: navigates to MainActivity</li>
     * </ul>
     *
     * @param name user's full name
     * @param email user’s email address
     * @param phone optional phone number
     */
    private void createUserProfile(String name, String email, String phone) {
        // Get current user ID
        if (userId == null && mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        if (userId == null) {
            Toast.makeText(this, "Authentication error. Please restart app.", Toast.LENGTH_LONG).show();
            btnContinue.setEnabled(true);
            return;
        }

        // Create user object
        User user = new User(userId, deviceId, name, email);

        // Everyone starts as ENTRANT by default
        user.addRole(UserRole.ENTRANT);

        // Add phone if provided
        if (!TextUtils.isEmpty(phone)) {
            user.setPhoneNumber(phone);
        }

        // Save to Firestore
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Welcome to LuckySpot!", Toast.LENGTH_SHORT).show();

                    // Navigate to MainActivity
                    navigateToHome();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnContinue.setEnabled(true);
                });
    }

    /**
     * Navigates to MainActivity and clears the back stack so the user
     * cannot return to onboarding screens.
     */
    private void navigateToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}