package com.example.event_app.admin;

import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

/**
 * BrowseImagesActivity - Admin can view all uploaded images
 * US 03.06.01: As an administrator, I want to browse images uploaded by users
 * US 03.03.01: As an administrator, I want to remove uploaded images
 */
public class AdminBrowseImagesActivity extends AppCompatActivity {

    private static final String TAG = "BrowseImagesActivity";

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

        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Browse Images");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize list
        imageList = new ArrayList<>();

        // Initialize views
        initViews();

        // Set up RecyclerView
        setupRecyclerView();

        // Load images
        loadImages();
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
                // For now, just show a toast
                // Later: Open full screen image viewer
                Toast.makeText(AdminBrowseImagesActivity.this,
                        "Image: " + imageData.getType(),
                        Toast.LENGTH_SHORT).show();
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
     * Load all images from Firebase
     * For now, we'll create dummy data since images collection doesn't exist yet
     */
    private void loadImages() {
        Log.d(TAG, "Loading images from Firebase...");

        // Show loading
        progressBar.setVisibility(View.VISIBLE);

        // Try to load from Firestore (if images collection exists)
        db.collection("images")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    imageList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        // No images in Firestore, create dummy data
                        createDummyImages();
                    } else {
                        // Load real images
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            ImageData imageData = document.toObject(ImageData.class);
                            imageList.add(imageData);
                        }
                    }

                    Log.d(TAG, "Loaded " + imageList.size() + " images");

                    // Hide loading
                    progressBar.setVisibility(View.GONE);

                    // Update UI
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading images", e);

                    // Hide loading
                    progressBar.setVisibility(View.GONE);

                    // Create dummy data for testing
                    createDummyImages();
                    updateUI();
                });
    }

    /**
     * Create dummy images for testing (temporary)
     * Remove this once real image upload is implemented
     */
    private void createDummyImages() {
        // Create some dummy images for testing
        imageList.add(new ImageData("img1", "https://via.placeholder.com/300", "user1", "event_poster"));
        imageList.add(new ImageData("img2", "https://via.placeholder.com/300", "user2", "profile_picture"));
        imageList.add(new ImageData("img3", "https://via.placeholder.com/300", "user3", "event_poster"));

        Log.d(TAG, "Created " + imageList.size() + " dummy images");
    }

    /**
     * Update UI based on image list
     */
    private void updateUI() {
        if (imageList.isEmpty()) {
            // Show empty state
            recyclerViewImages.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            // Show images
            recyclerViewImages.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);

            // Update adapter
            imageAdapter.setImages(imageList);
        }
    }

    /**
     * Show confirmation dialog before deleting image
     */
    private void showDeleteConfirmation(ImageData imageData) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Image")
                .setMessage("Are you sure you want to delete this image?\n\n" +
                        "Type: " + imageData.getType())
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteImage(imageData);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Delete image from Firebase Storage and Firestore
     * US 03.03.01: Remove images
     */
    private void deleteImage(ImageData imageData) {
        Log.d(TAG, "Deleting image: " + imageData.getImageId());

        // For dummy data, just remove from list
        if (imageData.getImageUrl().contains("placeholder")) {
            imageList.remove(imageData);
            updateUI();
            Toast.makeText(this, "Dummy image removed", Toast.LENGTH_SHORT).show();
            return;
        }

        // For real images: Delete from Storage first, then Firestore
        StorageReference imageRef = storage.getReferenceFromUrl(imageData.getImageUrl());

        imageRef.delete()
                .addOnSuccessListener(aVoid -> {
                    // Image deleted from storage, now delete from Firestore
                    db.collection("images")
                            .document(imageData.getImageId())
                            .delete()
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d(TAG, "Image deleted successfully");
                                Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show();

                                // Remove from list and update UI
                                imageList.remove(imageData);
                                updateUI();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error deleting image from Firestore", e);
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting image from Storage", e);
                    Toast.makeText(this, "Error deleting image: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Handle back button in action bar
        finish();
        return true;
    }
}

