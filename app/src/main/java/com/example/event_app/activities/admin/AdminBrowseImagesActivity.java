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
 * Activity that allows administrators to browse, inspect, and delete uploaded images.
 *
 * <p>This supports:
 * <ul>
 *   <li><b>US 03.06.01</b>: Browse images uploaded by users.</li>
 *   <li><b>US 03.03.01</b>: Remove uploaded images from the system.</li>
 * </ul>
 *
 * Images are retrieved directly from Firebase Storage (rather than Firestore),
 * and additional metadata (event or user association) is dynamically resolved
 * when an image is selected. Admins may view details, detect orphaned files,
 * and permanently delete the image and its references.
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
     * Initializes UI components displayed on this screen.
     */
    private void initViews() {
        recyclerViewImages = findViewById(R.id.recyclerViewImages);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        progressBar = findViewById(R.id.progressBar);
    }

    /**
     * Configures the RecyclerView with a grid layout and attaches an adapter.
     * Handles click actions for opening details and deleting images.
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
     * Begins loading images from Firebase Storage.
     * Currently loads images from the {@code event_posters} folder.
     */
    private void loadImagesFromStorage() {
        progressBar.setVisibility(View.VISIBLE);
        imageList.clear();
        // Load from event_posters folder
        loadFromStorageFolder("event_posters", "event_poster");
    }

    /**
     * Loads all images inside the given Firebase Storage folder and adds them
     * to the display list.
     *
     * @param folderPath path of the storage folder (e.g., "event_posters")
     * @param imageType type associated with the images stored in this folder
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
     * Determines what the image is associated with (event poster, profile picture,
     * or orphaned file), then displays a details dialog.
     *
     * @param imageData the selected image
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
     * Checks whether the selected image is being used as a profile picture.
     * If not, it is marked as an orphaned image.
     *
     * @param imageUrl URL of the image file
     * @param imageData metadata about the image
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
     * Displays a dialog with image usage information and management actions.
     *
     * @param imageType classification (poster, profile picture, orphaned)
     * @param usedFor where the image is used
     * @param uploader uploader name
     * @param uploaderId uploader UID (may be null)
     * @param imageData image metadata
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
     * Updates the screen depending on whether images exist.
     * Shows the empty state if the list is empty.
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
     * Displays a confirmation dialog before permanently deleting an image.
     *
     * @param imageData the image to delete
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
     * Deletes the image from Firebase Storage and removes it from the UI.
     * Also triggers removal of Firestore references.
     *
     * @param imageData image to delete
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
     * Removes references to a deleted image from Firestore documents
     * (e.g., clearing poster URLs or profile picture URLs).
     *
     * @param imageData image whose references should be removed
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