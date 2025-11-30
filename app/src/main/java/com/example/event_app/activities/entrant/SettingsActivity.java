package com.example.event_app.activities.entrant;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.event_app.R;
import com.example.event_app.models.User;
import com.example.event_app.utils.AccessibilityHelper;
import com.example.event_app.utils.UserRole;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * SettingsActivity - Edit profile, manage account, admin access, and accessibility
 *
 * Features:
 * - US 01.02.02: Update profile information
 * - US 01.02.04: Delete profile
 * - US 01.04.03: Opt out of notifications
 * - US 01.08.01 Accessibility options (large text, high contrast)
 * - Admin code unlock (secret: 1234)
 */
public class SettingsActivity extends AppCompatActivity {
    private static final String ADMIN_SECRET_CODE = "1234"; //Secret code
    // UI Elements - Profile
    private TextInputEditText editName, editEmail, editPhone;
    private MaterialButton btnSave, btnBecomeOrganizer, btnDeleteAccount;
    private View loadingView, contentView;
    // UI Elements - Admin
    private MaterialButton btnUnlockAdmin;
    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private String userId;
    private User currentUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initViews();

        // Load user data
        loadUserProfile();
    }

    private void initViews() {
        // Profile views
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        btnSave = findViewById(R.id.btnSave);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        loadingView = findViewById(R.id.loadingView);
        contentView = findViewById(R.id.contentView);

        btnUnlockAdmin = findViewById(R.id.btnUnlockAdmin);


        // Button listeners
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveProfile());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());

    }





    private void loadUserProfile() {
        showLoading();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        currentUser = document.toObject(User.class);
                        if (currentUser != null) {
                            displayUserData();
                        }
                    }
                    showContent();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
                    showContent();
                });
    }

    private void displayUserData() {
        // Display profile info
        editName.setText(currentUser.getName());
        editEmail.setText(currentUser.getEmail());
        editPhone.setText(currentUser.getPhoneNumber());
    }


    /**
     * US 01.02.02: Update profile information
     */
    private void saveProfile() {
        // Get values
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();

        // Validate
        if (!validateInputs(name, email)) {
            return;
        }

        // Disable button
        btnSave.setEnabled(false);

        // Update user object
        currentUser.setName(name);
        currentUser.setEmail(email);
        currentUser.setPhoneNumber(phone);
        currentUser.setUpdatedAt(System.currentTimeMillis());

        // Save to Firestore
        db.collection("users").document(userId)
                .set(currentUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                });
    }

    private boolean validateInputs(String name, String email) {
        // Validate name
        if (TextUtils.isEmpty(name)) {
            editName.setError("Name is required");
            editName.requestFocus();
            return false;
        }

        if (name.length() < 2) {
            editName.setError("Name must be at least 2 characters");
            editName.requestFocus();
            return false;
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            editEmail.setError("Email is required");
            editEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Please enter a valid email");
            editEmail.requestFocus();
            return false;
        }

        return true;
    }
    /**
     * US 01.02.04: Delete profile
     */
    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteAccount() {
        btnDeleteAccount.setEnabled(false);

        // Delete user document from Firestore
        db.collection("users").document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Delete Firebase Auth account
                    mAuth.getCurrentUser().delete()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();
                                // Sign out and go back to splash
                                mAuth.signOut();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Account partially deleted. Please contact support.", Toast.LENGTH_LONG).show();
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show();
                    btnDeleteAccount.setEnabled(true);
                });
    }

    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.GONE);
    }

    private void showContent() {
        loadingView.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload user data to check for role changes
        if (userId != null) {
            loadUserProfile();
        }
    }
}