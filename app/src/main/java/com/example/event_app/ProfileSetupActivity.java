package com.example.event_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.event_app.models.User;
import com.example.event_app.utils.UserRole;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * ProfileSetupActivity - Handles new user profile creation
 *
 * US 01.02.01: Provide personal information (name, email, phone)
 * US 01.07.01: Device-based identification
 */
public class ProfileSetupActivity extends AppCompatActivity {

    private static final String TAG = "ProfileSetupActivity";

    // UI Elements
    private TextInputEditText editTextName, editTextEmail, editTextPhone;
    private MaterialCheckBox checkboxEntrant, checkboxOrganizer;
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

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get device ID and user ID from intent
        deviceId = getIntent().getStringExtra("deviceId");
        userId = getIntent().getStringExtra("userId");

        Log.d(TAG, "Device ID: " + deviceId);
        Log.d(TAG, "User ID: " + userId);

        // Initialize views
        initViews();

        // Set up button click
        btnContinue.setOnClickListener(v -> handleContinue());
    }

    /**
     * Initialize all views
     */
    private void initViews() {
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhone = findViewById(R.id.editTextPhone);
        checkboxEntrant = findViewById(R.id.checkboxEntrant);
        checkboxOrganizer = findViewById(R.id.checkboxOrganizer);
        btnContinue = findViewById(R.id.btnContinue);
    }

    /**
     * Handle continue button click
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

        // Validate role selection
        if (!checkboxEntrant.isChecked() && !checkboxOrganizer.isChecked()) {
            Toast.makeText(this, "Please select at least one role", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create and save profile
        createUserProfile(name, email, phone);
    }

    /**
     * Validate user inputs
     */
    private boolean validateInputs(String name, String email) {
        // Validate name
        if (TextUtils.isEmpty(name)) {
            editTextName.setError("Name is required");
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
     * Create user profile and save to Firebase
     */
    private void createUserProfile(String name, String email, String phone) {
        // Show loading message
        Toast.makeText(this, "Creating profile...", Toast.LENGTH_SHORT).show();

        // Get current user ID (should exist from anonymous auth)
        if (userId == null && mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        if (userId == null) {
            Toast.makeText(this, "Authentication error. Please restart app.", Toast.LENGTH_LONG).show();
            return;
        }

        // Create user object
        User user = new User(userId, deviceId, name, email);

        // Add selected roles
        if (checkboxEntrant.isChecked()) {
            user.addRole(UserRole.ENTRANT);
        }
        if (checkboxOrganizer.isChecked()) {
            user.addRole(UserRole.ORGANIZER);
        }

        // Add phone if provided
        if (!TextUtils.isEmpty(phone)) {
            user.setPhoneNumber(phone);
        }

        // Save to Firestore
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Profile created successfully");
                    Toast.makeText(this, "Profile created!", Toast.LENGTH_SHORT).show();

                    // Navigate based on role
                    navigateToHome(user);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error creating profile", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Navigate to appropriate home screen based on user role
     */
    private void navigateToHome(User user) {
        Intent intent;

        // Priority: Admin > Organizer > Entrant
        if (user.isAdmin()) {
            Log.d(TAG, "Navigating to AdminHomeActivity");
            intent = new Intent(this, com.example.event_app.admin.AdminHomeActivity.class);
        } else if (user.isOrganizer()) {
            Log.d(TAG, "Navigating to OrganizerHomeActivity");
            intent = new Intent(this, OrganizerHomeActivity.class);
        } else {
            Log.d(TAG, "Navigating to MainActivity (Entrant)");
            intent = new Intent(this, MainActivity.class);
        }

        startActivity(intent);
        finish(); // Close ProfileSetupActivity
    }
}
