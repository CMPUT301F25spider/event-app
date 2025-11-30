package com.example.event_app.activities.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.adapters.ImageAdapter;
import com.example.event_app.models.ImageData;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

/**
 * AdminBrowseImagesActivity - Admin can view and delete uploaded images
 * US 03.06.01: As an administrator, I want to browse images uploaded by users
 * US 03.03.01: As an administrator, I want to remove uploaded images
 */
public class AdminBrowseImagesActivity extends AppCompatActivity {
    private RecyclerView recyclerViewImages;
    private LinearLayout emptyStateLayout;
    private ProgressBar progressBar;
    private ImageAdapter imageAdapter;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private List<ImageData> imageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_images);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);
        // Set up back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        // Initialize list
        imageList = new ArrayList<>();
        // Initialize views
        initViews();
        // Set up RecyclerView
        setupRecyclerView();
        // Load images from Firebase Storage
        loadImagesFromStorage();
    }

    /**
     * Initialize views
     */
    private void initViews() {
        recyclerViewImages = findViewById(R.id.recyclerViewImages);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        progressBar = findViewById(R.id.progressBar);
    }

    /**
     * Set up RecyclerView with adapter
     */
    private void setupRecyclerView() {
        // Create adapter
        imageAdapter = new ImageAdapter();

        // Set click listener
        imageAdapter.setOnImageClickListener(new ImageAdapter.OnImageClickListener() {
            @Override
            public void onImageClick(ImageData imageData) {
                showImageDetails(imageData);
            }

            @Override
            public void onDeleteClick(ImageData imageData) {
                // Show confirmation dialog
                showDeleteConfirmation(imageData);
            }
        });

        // Use GridLayoutManager for 2 columns
        recyclerViewImages.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerViewImages.setAdapter(imageAdapter);
    }

    /**
     * Load images directly from Firebase Storage folders
     */
    private void loadImagesFromStorage() {
        progressBar.setVisibility(View.VISIBLE);
        imageList.clear();
        // Load from event_posters folder
        loadFromStorageFolder("event_posters", "event_poster");
    }

    /**
     * Load images from a specific Storage folder
     */
    private void loadFromStorageFolder(String folderPath, String imageType) {
        StorageReference folderRef = storage.getReference().child(folderPath);

        folderRef.listAll()
                .addOnSuccessListener(listResult -> {
                    for (StorageReference item : listResult.getItems()) {
                        // Get download URL for each image
                        item.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            String fileName = item.getName();

                            // Create ImageData object
                            ImageData imageData = new ImageData(
                                    fileName,           // imageId
                                    imageUrl,           // imageUrl
                                    "unknown",          // uploadedBy
                                    imageType           // type
                            );

                            // Store the storage reference path for deletion
                            imageData.setStoragePath(folderPath + "/" + fileName);
                            imageList.add(imageData);
                            // Update UI
                            updateUI();
                        }).addOnFailureListener(e -> {
                        });
                    }

                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    updateUI();
                });
    }
    /**
     * Show detailed information about an image
     */
    private void showImageDetails(ImageData imageData) {
        progressBar.setVisibility(View.VISIBLE);
        // Determine what this image is for
        String imageUrl = imageData.getImageUrl();
        // Check if it's an event poster
        db.collection("events")
                .whereEqualTo("posterUrl", imageUrl)
                .get()
                .addOnSuccessListener(eventSnapshots -> {
                    if (!eventSnapshots.isEmpty()) {
                        QueryDocumentSnapshot eventDoc = (QueryDocumentSnapshot) eventSnapshots.getDocuments().get(0);
                        String eventName = eventDoc.getString("name");
                        String organizerId = eventDoc.getString("organizerId");
                        String organizerName = eventDoc.getString("organizerName");
                        showImageDetailsDialog(
                                "Event Poster",
                                eventName,
                                organizerName != null ? organizerName : "Unknown",
                                organizerId,
                                imageData
                        );
                    } else {
                        // Check if it's a profile picture
                        checkProfilePicture(imageUrl, imageData);
                    }
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading details", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Check if image is a profile picture
     */
    private void checkProfilePicture(String imageUrl, ImageData imageData) {
        db.collection("users")
                .whereEqualTo("profileImageUrl", imageUrl)
                .get()
                .addOnSuccessListener(userSnapshots -> {
                    if (!userSnapshots.isEmpty()) {
                        QueryDocumentSnapshot userDoc = (QueryDocumentSnapshot) userSnapshots.getDocuments().get(0);
                        String userName = userDoc.getString("name");
                        String userId = userDoc.getId();

                        showImageDetailsDialog(
                                "Profile Picture",
                                "User Profile",
                                userName != null ? userName : "Unknown",
                                userId,
                                imageData
                        );
                    } else {
                        // Orphaned image
                        showImageDetailsDialog(
                                "Orphaned Image",
                                "No associated event or user",
                                "Unknown",
                                null,
                                imageData
                        );
                    }
                });
    }

    /**
     * Show dialog with image details
     */
    private void showImageDetailsDialog(String imageType, String usedFor, String uploader, String uploaderId, ImageData imageData) {
        String details = "TYPE: " + imageType + "\n\n" +
                "USED FOR: " + usedFor + "\n\n" +
                "UPLOADED BY: " + uploader + "\n\n" +
                "UPLOADER ID: " + (uploaderId != null ? uploaderId : "N/A") + "\n\n" +
                "FILE NAME: " + imageData.getImageId() + "\n\n" +
                "STORAGE PATH: " + imageData.getStoragePath();

        new AlertDialog.Builder(this)
                .setTitle("Image Details")
                .setMessage(details)
                .setPositiveButton("Delete Image", (dialog, which) -> {
                    showDeleteConfirmation(imageData);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    /**
     * Update UI based on image list
     */
    private void updateUI() {
        runOnUiThread(() -> {
            if (imageList.isEmpty()) {
                // Show empty state
                recyclerViewImages.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.VISIBLE);
            } else {
                // Show images
                recyclerViewImages.setVisibility(View.VISIBLE);
                emptyStateLayout.setVisibility(View.GONE);

                // Update adapter
                imageAdapter.setImages(new ArrayList<>(imageList));
            }
        });
    }

    /**
     * Show confirmation dialog before deleting image
     * US 03.03.01: Remove uploaded images
     */
    private void showDeleteConfirmation(ImageData imageData) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Image?")
                .setMessage("Are you sure you want to delete this image?\n\nThis action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteImage(imageData);
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Delete image from Firebase Storage
     * US 03.03.01: Remove images
     */
    private void deleteImage(ImageData imageData) {
        progressBar.setVisibility(View.VISIBLE);
        // Delete from Firebase Storage using the storage path
        StorageReference imageRef = storage.getReference().child(imageData.getStoragePath());

        imageRef.delete()
                .addOnSuccessListener(aVoid -> {
                    removeImageReferences(imageData);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Image deleted successfully", Toast.LENGTH_SHORT).show();
                    imageList.remove(imageData);
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to delete image: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Remove image URL references from Firestore documents
     */
    private void removeImageReferences(ImageData imageData) {
        String imageUrl = imageData.getImageUrl();

        // Check events collection for this poster URL
        if (imageData.getType().equals("event_poster")) {
            db.collection("events")
                    .whereEqualTo("posterUrl", imageUrl)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            document.getReference().update("posterUrl", null);
                        }
                    })
                    .addOnFailureListener(e -> {
                    });
        }

        // Check users collection for profile pictures
        if (imageData.getType().equals("profile_picture")) {
            db.collection("users")
                    .whereEqualTo("profileImageUrl", imageUrl)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            document.getReference().update("profileImageUrl", null);
                        }
                    })
                    .addOnFailureListener(e -> {
                    });
        }
    }
}