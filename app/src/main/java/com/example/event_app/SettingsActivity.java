package com.example.event_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * SettingsActivity
 *  - View and update entrant profile (name, email, phone)
 *  - Deletes profile document from Firestore
 * Covers:
 *  - US 01.02.01 / 01.02.02 (profile provide/update)
 *  - US 01.02.04 (delete profile)
 */
public class SettingsActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone;
    private Button btnSaveProfile, btnDeleteProfile;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Action bar title & back arrow (same style as Browse Events)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Profile Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnDeleteProfile = findViewById(R.id.btnDeleteProfile);

        // Load existing profile info
        loadUserProfile();

        // Save profile changes
        btnSaveProfile.setOnClickListener(v -> saveUserProfile());

        // Delete profile
        btnDeleteProfile.setOnClickListener(v -> deleteProfile());
    }

    // Handle ActionBar back arrow
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // Load profile data from Firestore into EditTexts
    private void loadUserProfile() {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        etName.setText(document.getString("name"));
                        etEmail.setText(document.getString("email"));
                        etPhone.setText(document.getString("phoneNumber"));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    // Save/update profile (merge so we don't wipe other fields like role, createdAt)
    private void saveUserProfile() {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("phoneNumber", phone);

        db.collection("users").document(userId)
                .set(updates, SetOptions.merge())   // keep other fields intact
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show());
    }

    // Delete profile document from Firestore
    // US 01.02.04 – As an entrant, I want to delete my profile
    private void deleteProfile() {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Delete user from all event waiting lists (subcollection version)
        db.collection("events").get()
                .addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot eventDoc : querySnapshot.getDocuments()) {
                        eventDoc.getReference()
                                .collection("waitingList")
                                .document(userId)
                                .delete();
                    }

                    // Delete the user document
                    db.collection("users").document(userId)
                            .delete()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();

                                // Delete FirebaseAuth user too!
                                auth.getCurrentUser().delete()
                                        .addOnCompleteListener(task -> {
                                            // Redirect to MainActivity
                                            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(intent);
                                            finish();
                                        });
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to delete profile", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to clean up waiting lists", Toast.LENGTH_SHORT).show());
    }


}
