package com.example.event_app.ui;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.example.event_app.R;

import java.util.HashMap;
import java.util.Map;

public class EventPosterActivity extends AppCompatActivity {

    private ImageView posterImageView;
    private Button pickImageButton;
    private ProgressBar progressBar;

    private String eventId = "sampleEvent123"; // TODO: pass via Intent

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> { if (uri != null) uploadToFirebase(uri); });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_poster);

        posterImageView = findViewById(R.id.imagePoster);
        pickImageButton = findViewById(R.id.btnPickImage);
        progressBar = findViewById(R.id.progressBar);

        // If provided by caller
        String passedEventId = getIntent().getStringExtra("eventId");
        if (passedEventId != null && !passedEventId.isEmpty()) eventId = passedEventId;

        pickImageButton.setOnClickListener(v -> imagePicker.launch("image/*"));
    }

    private void uploadToFirebase(Uri imageUri) {
        setLoading(true);

        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("event_posters/" + eventId + "/" + System.currentTimeMillis() + ".jpg");

        storageRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return storageRef.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    String url = downloadUri.toString();
                    saveUrlToFirestore(url);
                    displayPoster(url);
                    setLoading(false);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUrlToFirestore(String posterUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> map = new HashMap<>();
        map.put("posterUrl", posterUrl);

        db.collection("events")
                .document(eventId)
                .update(map)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Poster updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save URL", Toast.LENGTH_SHORT).show());
    }

    private void displayPoster(String url) {
        try {
            posterImageView.setImageURI(Uri.parse(url));
        } catch (Exception e) {
            Toast.makeText(this, "Cannot load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
        pickImageButton.setEnabled(!loading);
    }
}
