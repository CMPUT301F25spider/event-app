package com.example.event_app.organizer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.example.event_app.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class EventPosterActivity extends AppCompatActivity {

    private ImageView posterImageView;
    private Button pickImageButton;
    private ProgressBar progressBar;

    private String eventId = "sampleEvent123"; // pass via Intent "eventId"
    private PosterValidator validator;

    // cap for displaying the downloaded image bytes (no Glide)
    private static final long DISPLAY_BYTES_CAP = 5L * 1024L * 1024L; // 5 MB

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> { if (uri != null) startFlow(uri); });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_poster);

        posterImageView = findViewById(R.id.imagePoster);
        pickImageButton = findViewById(R.id.btnPickImage);
        progressBar     = findViewById(R.id.progressBar);

        // 5 MB validator per user stories
        validator = new PosterValidator(getContentResolver(), 5L * 1024L * 1024L);

        String passedEventId = getIntent().getStringExtra("eventId");
        if (passedEventId != null && !passedEventId.isEmpty()) eventId = passedEventId;

        pickImageButton.setOnClickListener(v -> imagePicker.launch("image/*"));
    }

    private void startFlow(Uri imageUri) {
        Result<Void> res = validator.validate(imageUri);
        if (!res.isOk()) {
            toast(res.getErrorMessage() != null ? res.getErrorMessage() : "Invalid image");
            return;
        }
        setLoading(true);

        // Read current posterUrl so we can delete it after successful update
        FirebaseFirestore.getInstance()
                .collection("events").document(eventId).get()
                .addOnSuccessListener(snap -> {
                    String oldUrl = (snap != null) ? snap.getString("posterUrl") : null;
                    uploadToFirebase(imageUri, oldUrl);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Could not read event");
                });
    }

    private void uploadToFirebase(Uri imageUri, String oldUrl) {
        // Pick correct contentType/extension (jpg/png)
        String mime = validator.resolveMime(imageUri);
        if (mime == null) mime = "image/jpeg";
        String ext  = PosterValidator.extensionForMime(mime);

        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("event_posters/" + eventId + "/poster." + ext);

        StorageMetadata meta = new StorageMetadata.Builder()
                .setContentType("image/png".equals(mime) ? "image/png" : "image/jpeg")
                .build();

        storageRef.putFile(imageUri, meta)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return storageRef.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    String newUrl = downloadUri.toString();
                    saveUrlToFirestore(newUrl, oldUrl);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Upload failed: " + e.getMessage());
                });
    }

    private void saveUrlToFirestore(String posterUrl, String oldUrl) {
        Map<String, Object> map = new HashMap<>();
        map.put("posterUrl", posterUrl);

        FirebaseFirestore.getInstance()
                .collection("events").document(eventId).update(map)
                .addOnSuccessListener(unused -> {
                    // Best-effort delete of previous poster if changed
                    if (oldUrl != null && !oldUrl.isEmpty() && !oldUrl.equals(posterUrl)) {
                        try {
                            FirebaseStorage.getInstance().getReferenceFromUrl(oldUrl)
                                    .delete()
                                    .addOnCompleteListener(__ -> finishSuccess(posterUrl));
                        } catch (Exception ignored) {
                            finishSuccess(posterUrl);
                        }
                    } else {
                        finishSuccess(posterUrl);
                    }
                })
                .addOnFailureListener(e -> {
                    // Optional cleanup: remove newly uploaded file to avoid orphans
                    try { FirebaseStorage.getInstance().getReferenceFromUrl(posterUrl).delete(); } catch (Exception ignore) {}
                    setLoading(false);
                    toast("Failed to save URL");
                });
    }

    private void finishSuccess(String url) {
        setLoading(false);
        toast("Poster updated!");
        displayPoster(url); // no-Glide display
        // Optionally: setResult(RESULT_OK); finish();
    }

    /** Loads a Firebase download URL without Glide: fetch bytes and decode. */
    private void displayPoster(String downloadUrl) {
        try {
            StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(downloadUrl);
            ref.getBytes(DISPLAY_BYTES_CAP)
                    .addOnSuccessListener(bytes -> {
                        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        posterImageView.setImageBitmap(bmp);
                    })
                    .addOnFailureListener(e -> toast("Cannot load image"));
        } catch (Exception e) {
            toast("Cannot load image");
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
        pickImageButton.setEnabled(!loading);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
