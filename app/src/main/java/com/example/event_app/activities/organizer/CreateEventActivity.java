package com.example.event_app.activities.organizer;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.event_app.R;
import com.example.event_app.activities.entrant.MainActivity;
import com.example.event_app.models.Event;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * CreateEventActivity — Allows organizers to create new events including:
 * • Selecting poster images (US 02.04.01)
 * • Setting event date and registration period (US 02.01.04)
 * • Enabling/disabling geolocation requirement (US 02.02.03)
 * • Setting capacity limits (US 02.03.01)
 * • Generating QR codes for created events (US 02.01.01)
 *
 * Handles poster upload, Firestore event creation, organizer role assignment,
 * and Storage upload of QR codes and posters.
 */
public class CreateEventActivity extends AppCompatActivity {
    // UI Elements
    private TextInputEditText editEventName, editDescription, editLocation, editCapacity;
    private MaterialButton btnSelectPoster, btnSelectEventDate, btnSelectRegStart, btnSelectRegEnd;
    private MaterialButton btnCreateEvent, btnPreview, btnSelectCategory;
    private SwitchMaterial switchGeolocation;
    private ImageView ivPosterPreview;
    private View loadingView, emptyPosterView;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // Data
    private Uri posterUri;
    private Date eventDate, regStartDate, regEndDate;
    private boolean geolocationEnabled = false;
    private String selectedCategory = "Other"; // Default category

    // Image picker launcher
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    posterUri = result.getData().getData();
                    displayPosterPreview();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize views
        initViews();
    }

    /**
     * Initializes all UI elements, sets up button listeners,
     * and configures geolocation toggle behavior.
     */
    private void initViews() {
        // Input fields
        editEventName = findViewById(R.id.editEventName);
        editDescription = findViewById(R.id.editDescription);
        editLocation = findViewById(R.id.editLocation);
        editCapacity = findViewById(R.id.editCapacity);

        // Buttons
        btnSelectPoster = findViewById(R.id.btnSelectPoster);
        btnSelectEventDate = findViewById(R.id.btnSelectEventDate);
        btnSelectRegStart = findViewById(R.id.btnSelectRegStart);
        btnSelectRegEnd = findViewById(R.id.btnSelectRegEnd);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);
        btnPreview = findViewById(R.id.btnPreview);
        btnSelectCategory = findViewById(R.id.btnSelectCategory);

        // Switch
        switchGeolocation = findViewById(R.id.switchGeolocation);

        // Other views
        ivPosterPreview = findViewById(R.id.ivPosterPreview);
        loadingView = findViewById(R.id.loadingView);
        emptyPosterView = findViewById(R.id.emptyPosterView);

        // Button listeners
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSelectPoster.setOnClickListener(v -> selectPoster());
        btnSelectEventDate.setOnClickListener(v -> selectEventDate());
        btnSelectRegStart.setOnClickListener(v -> selectRegistrationStart());
        btnSelectRegEnd.setOnClickListener(v -> selectRegistrationEnd());
        btnCreateEvent.setOnClickListener(v -> createEvent());
        btnPreview.setOnClickListener(v -> showPreview());
        btnSelectCategory.setOnClickListener(v -> showCategoryDialog());

        // Geolocation switch listener
        switchGeolocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            geolocationEnabled = isChecked;
        });
    }

    /**
     * Opens the device gallery to allow the user to select an event poster image.
     * US 02.04.01: Upload event poster.
     */
    private void selectPoster() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    /**
     * Displays the selected poster image in the preview ImageView.
     * Updates the UI to indicate a poster has been chosen.
     */
    private void displayPosterPreview() {
        emptyPosterView.setVisibility(View.GONE);
        ivPosterPreview.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(posterUri)
                .centerCrop()
                .into(ivPosterPreview);
        btnSelectPoster.setText("Change Poster");
    }

    /**
     * Opens a dialog allowing the organizer to select a predefined event category.
     * Updates the selectedCategory value and button text upon selection.
     */
    private void showCategoryDialog() {
        String[] categories = {
                "Food & Dining",
                "Sports & Fitness",
                "Music & Entertainment",
                "Education & Learning",
                "Art & Culture",
                "Technology",
                "Health & Wellness",
                "Business & Networking",
                "Community & Social",
                "Other"
        };

        // Find currently selected index
        int selectedIndex = 9; // Default to "Other"
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(selectedCategory)) {
                selectedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Event Category")
                .setSingleChoiceItems(categories, selectedIndex, (dialog, which) -> {
                    selectedCategory = categories[which];
                    btnSelectCategory.setText(selectedCategory);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Opens a dialog allowing the organizer to select a predefined event category.
     * Updates the selectedCategory value and button text upon selection.
     */
    private void selectEventDate() {
        Calendar calendar = Calendar.getInstance();

        // 1. Initialize DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);

            // 2. Now pick time
            new TimePickerDialog(this, (timeView, hour, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);

                // Final validation: Ensure the final combined time is not in the past.
                // This is needed because the TimePicker doesn't know about the DatePicker's minimum time.
                if (calendar.getTime().before(new Date())) {
                    Toast.makeText(this, "The event time cannot be in the past.", Toast.LENGTH_LONG).show();
                    eventDate = null;
                    btnSelectEventDate.setText("Select Event Date"); // Reset button text
                    return;
                }

                eventDate = calendar.getTime();

                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
                btnSelectEventDate.setText(sdf.format(eventDate));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        // 3. Set Minimum Date Constraint on DatePicker
        // We set the minimum date to be the current time (Calendar.getInstance().getTimeInMillis())
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000); // Subtract 1 second for tolerance

        datePickerDialog.show();
    }

    /**
     * Allows the user to pick the registration start date.
     * US 02.01.04: Set registration period.
     * Applies minimum date constraints to prevent past-date selection.
     */
    private void selectRegistrationStart() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);

            // Clear time components for pure date comparison
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            regStartDate = calendar.getTime();

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            btnSelectRegStart.setText(sdf.format(regStartDate));

            // This ensures the end date isn't suddenly before the new start date.
            checkEndDateValidity();

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        // Set Minimum Date Constraint on DatePicker
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

        datePickerDialog.show();
    }

    /**
     * Allows the user to pick the registration end date.
     * Ensures that the end date is on or after the start date.
     * US 02.01.04: Set registration period.
     */
    private void selectRegistrationEnd() {
        if (regStartDate == null) {
            Toast.makeText(this, "Please set the Registration Start Date first.", Toast.LENGTH_LONG).show();
            return;
        }

        Calendar calendar = Calendar.getInstance();

        // Set the calendar to the current start date for initialization
        calendar.setTime(regStartDate);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);

            // Clear time components for pure date comparison
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            regEndDate = calendar.getTime();

            if (regEndDate.before(regStartDate)) {
                Toast.makeText(this, "Registration End Date must be after the Start Date.", Toast.LENGTH_LONG).show();
                regEndDate = null;
                btnSelectRegEnd.setText("Select Registration End"); // Reset button text
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            btnSelectRegEnd.setText(sdf.format(regEndDate));

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        // Set Minimum Date Constraint on DatePicker to the Registration Start Date
        datePickerDialog.getDatePicker().setMinDate(regStartDate.getTime());

        datePickerDialog.show();
    }

    /**
     * Ensures registration end date is not before the start date.
     * Resets invalid selections and warns the user if necessary.
     */
    private void checkEndDateValidity() {
        if (regStartDate != null && regEndDate != null && regEndDate.before(regStartDate)) {
            regEndDate = null;
            btnSelectRegEnd.setText("Select Registration End");
            Toast.makeText(this, "The Registration End Date was reset because it now occurs before the new Start Date.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Displays a formatted preview of the event details in an AlertDialog
     * before final creation. Includes name, description, dates, category,
     * capacity, poster status, and geolocation state.
     */
    private void showPreview() {
        String name = editEventName.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String location = editLocation.getText().toString().trim();
        String capacityStr = editCapacity.getText().toString().trim();

        // Build preview message
        StringBuilder preview = new StringBuilder();
        preview.append("Event Preview\n\n");

        preview.append("Name: ").append(name.isEmpty() ? "Not set" : name).append("\n\n");
        preview.append("Description: ").append(description.isEmpty() ? "Not set" : description).append("\n\n");
        preview.append("Category: ").append(selectedCategory).append("\n\n");
        preview.append("Location: ").append(location.isEmpty() ? "Not set" : location).append("\n\n");

        if (!capacityStr.isEmpty()) {
            preview.append("Capacity: ").append(capacityStr).append(" spots\n\n");
        } else {
            preview.append("Capacity: Unlimited\n\n");
        }

        if (eventDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
            preview.append("Date: ").append(sdf.format(eventDate)).append("\n\n");
        } else {
            preview.append("Date: Not set\n\n");
        }

        preview.append("Poster: ").append(posterUri != null ? "Selected" : "Not selected").append("\n\n");
        preview.append("Geolocation: ").append(geolocationEnabled ? "Enabled" : "Disabled").append("\n");

        new AlertDialog.Builder(this)
                .setTitle("Event Preview")
                .setMessage(preview.toString())
                .setPositiveButton("Looks Good", null)
                .setNegativeButton("Edit", null)
                .show();
    }

    /**
     * Validates inputs and constructs an Event object.
     * Handles optional capacity parsing, geolocation setting,
     * organizer name retrieval, and branching between:
     * • Creating event with poster upload
     * • Creating event without poster
     *
     * US 02.01.01: Create event
     * US 02.02.03: Enable/disable geolocation
     */
    private void createEvent() {
        // Get values
        String name = editEventName.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String location = editLocation.getText().toString().trim();
        String capacityStr = editCapacity.getText().toString().trim();

        // Validate
        if (!validateInputs(name, description)) {
            return;
        }

        // Parse capacity
        Long capacity = null;
        if (!TextUtils.isEmpty(capacityStr)) {
            try {
                capacity = Long.parseLong(capacityStr);
            } catch (NumberFormatException e) {
                editCapacity.setError("Invalid number");
                return;
            }
        }

        // Show loading
        showLoading();

        // Create event ID
        String eventId = db.collection("events").document().getId();

        // Create event object
        Event event = new Event(eventId, name, description, mAuth.getCurrentUser().getUid());
        event.setLocation(location);
        event.setCategory(selectedCategory); // Set the selected category
        event.setCapacity(capacity);
        event.setEventDate(eventDate);
        event.setRegistrationStartDate(regStartDate);
        event.setRegistrationEndDate(regEndDate);
        event.setWaitingList(new ArrayList<>());
        event.setSignedUpUsers(new ArrayList<>());
        event.setSelectedList(new ArrayList<>());
        event.setDeclinedUsers(new ArrayList<>());
        event.setStatus("active");

        // US 02.02.03: Set geolocation requirement
        event.setGeolocationEnabled(geolocationEnabled);

        // Get organizer name
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String organizerName = userDoc.getString("name");
                        event.setOrganizerName(organizerName);
                    }

                    // Upload poster if selected
                    if (posterUri != null) {
                        uploadPosterAndCreateEvent(eventId, event);
                    } else {
                        saveEventToFirestore(eventId, event);
                    }
                });
    }

    /**
     * Validates the required fields for creating an event.
     *
     * @param name The event name.
     * @param description The event description.
     * @return true if all required fields are valid; false otherwise.
     */
    private boolean validateInputs(String name, String description) {
        if (TextUtils.isEmpty(name)) {
            editEventName.setError("Event name is required");
            editEventName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(description)) {
            editDescription.setError("Description is required");
            editDescription.requestFocus();
            return false;
        }

        if (eventDate == null) {
            Toast.makeText(this, "Please select event date", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * Uploads the event poster to Firebase Storage.
     * On success → retrieves download URL and saves event to Firestore.
     * On failure → proceeds without poster.
     *
     * @param eventId Unique ID of the event.
     * @param event   Event object to be stored.
     */
    private void uploadPosterAndCreateEvent(String eventId, Event event) {
        StorageReference posterRef = storage.getReference()
                .child("event_posters")
                .child(eventId + ".jpg");

        posterRef.putFile(posterUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL
                    posterRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        event.setPosterUrl(uri.toString());
                        saveEventToFirestore(eventId, event);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to upload poster, creating event without it", Toast.LENGTH_SHORT).show();
                    saveEventToFirestore(eventId, event);
                });
    }

    /**
     * Saves the event to Firestore.
     * After saving:
     * • Adds "organizer" role to the user if not already present.
     * • Generates and uploads a QR code.
     *
     * @param eventId ID under which the event will be stored.
     * @param event   Event data to save.
     */
    private void saveEventToFirestore(String eventId, Event event) {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("events").document(eventId)
                .set(event)
                .addOnSuccessListener(aVoid -> {
                    // Add "organizer" role to user if they don't have it
                    addOrganizerRoleToUser(userId);

                    // Generate and upload QR code
                    generateAndUploadQRCode(eventId);
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Toast.makeText(this, "Failed to create event", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Adds the "organizer" role to the user in Firestore
     * if they do not already possess it.
     *
     * @param userId Firebase Auth user ID.
     */
    private void addOrganizerRoleToUser(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> roles = (List<String>) documentSnapshot.get("roles");

                        // Check if user already has organizer role
                        if (roles != null && roles.contains("organizer")) {
                            return;
                        }

                        // Add organizer role
                        db.collection("users").document(userId)
                                .update("roles", com.google.firebase.firestore.FieldValue.arrayUnion("organizer"))
                                .addOnSuccessListener(aVoid -> {
                                })
                                .addOnFailureListener(e -> {
                                });
                    }
                })
                .addOnFailureListener(e -> {
                });
    }

    /**
     * Generates a QR code encoding the eventId using ZXing,
     * converts it to a PNG byte array, and uploads it to Firebase Storage.
     *
     * @param eventId The event identifier encoded in the QR code.
     */
    private void generateAndUploadQRCode(String eventId) {
        try {
            // Generate QR code bitmap
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(eventId, BarcodeFormat.QR_CODE, 512, 512);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            // Convert to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] data = baos.toByteArray();

            // Upload to Firebase Storage
            StorageReference qrRef = storage.getReference()
                    .child("qr_codes")
                    .child(eventId + ".png");

            qrRef.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> {
                        hideLoading();
                        showSuccessAndNavigate();
                    })
                    .addOnFailureListener(e -> {
                        hideLoading();
                        // Still show success even if QR upload fails
                        showSuccessAndNavigate();
                    });

        } catch (WriterException e) {
            hideLoading();
            showSuccessAndNavigate();
        }
    }

    /**
     * Shows a success message and navigates back to MainActivity,
     * clearing the back stack.
     */
    private void showSuccessAndNavigate() {
        Toast.makeText(this, "Event created successfully!", Toast.LENGTH_LONG).show();

        // Go back to main activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    /**
     * Displays the loading view and disables the Create Event button
     * to prevent duplicate submissions.
     */
    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
        btnCreateEvent.setEnabled(false);
    }

    /**
     * Hides the loading indicator and re-enables the Create Event button.
     */
    private void hideLoading() {
        loadingView.setVisibility(View.GONE);
        btnCreateEvent.setEnabled(true);
    }
}
