package com.example.event_app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone;
    private Button btnSaveProfile, btnDeleteProfile;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Action bar styling like Browse Events
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Profile Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Firebase init
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnDeleteProfile = findViewById(R.id.btnDeleteProfile);

        // Load basic user data (if any)
        loadUserProfile();

        // Save button
        btnSaveProfile.setOnClickListener(v -> saveUserProfile());

        // Delete profile (placeholder)
        btnDeleteProfile.setOnClickListener(v ->
                Toast.makeText(this, "Delete feature coming soon", Toast.LENGTH_SHORT).show()
        );
    }

    // Back arrow support
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // Load profile from Firestore
    private void loadUserProfile() {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        etName.setText(document.getString("name"));
                        etEmail.setText(document.getString("email"));
                        etPhone.setText(document.getString("phone"));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    // Save profile updates
    private void saveUserProfile() {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", etName.getText().toString().trim());
        updates.put("email", etEmail.getText().toString().trim());
        updates.put("phone", etPhone.getText().toString().trim());

        db.collection("users").document(userId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show());
    }
}
