package com.example.event_app.activities.organizer;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * CreateEventActivity
 *
 * Allows organizers to create new events with the following features:
 * <ul>
 *   <li>Set event name, description, location, capacity</li>
 *   <li>Select and upload a poster (US 02.04.01)</li>
 *   <li>Pick event date + registration start & end dates (US 02.01.04)</li>
 *   <li>Toggle geolocation requirement for sign-ups (US 02.02.03)</li>
 *   <li>Select from predefined or custom categories</li>
 *   <li>Generate & upload QR code for event (US 02.01.01)</li>
 * </ul>
 *
 * The activity:
 * <ul>
 *   <li>Validates organizer inputs before saving</li>
 *   <li>Uploads poster and QR code to Firebase Storage</li>
 *   <li>Saves Event object to Firestore</li>
 *   <li>Assigns "organizer" user role upon creating first event</li>
 *   <li>Displays a preview and QR-code dialog on success</li>
 * </ul>
 */
public class CreateEventActivity extends AppCompatActivity {

    private static final String TAG = "CreateEventActivity";

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
    private String selectedCategory = "Food & Dining"; // Default category
    private List<String> customCategories = new ArrayList<>(); // User-added categories
    private Bitmap qrBitmap; // Store generated QR code bitmap

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

        // Load custom categories from Firestore
        loadCustomCategories();
    }

    /**
     * Initializes all input fields, buttons, toggles, previews,
     * and attaches click listeners for user interactions.
     *
     * Also attaches listeners for geolocation toggle and navigation.
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
     * Opens the system image picker for selecting an event poster.
     *
     * US 02.04.01: Upload event poster.
     */
    private void selectPoster() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    /**
     * Displays the selected poster in the ImageView preview
     * and updates button text accordingly.
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
     * Loads all custom categories stored in Firestore.
     *
     * Success:
     * - Populates local category list
     * - Sorts categories alphabetically
     *
     * Failure:
     * - Logs error and continues with empty list
     */
    private void loadCustomCategories() {
        db.collection("categories")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    customCategories.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String categoryName = doc.getString("name");
                        if (categoryName != null && !categoryName.trim().isEmpty()) {
                            customCategories.add(categoryName.trim());
                        }
                    }
                    // Sort alphabetically
                    Collections.sort(customCategories);
                    Log.d(TAG, "Loaded " + customCategories.size() + " custom categories");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading custom categories", e);
                    // Continue with empty list if loading fails
                });
    }

    /**
     * Displays a dialog allowing organizers to choose from
     * predefined categories, previously added custom categories,
     * or create a new category.
     */
    private void showCategoryDialog() {
        // Predefined categories
        List<String> predefinedCategories = new ArrayList<>();
        predefinedCategories.add("Food & Dining");
        predefinedCategories.add("Sports & Fitness");
        predefinedCategories.add("Music & Entertainment");
        predefinedCategories.add("Education & Learning");
        predefinedCategories.add("Art & Culture");
        predefinedCategories.add("Technology");
        predefinedCategories.add("Health & Wellness");
        predefinedCategories.add("Business & Networking");
        predefinedCategories.add("Community & Social");

        // Combine predefined and custom categories
        List<String> allCategories = new ArrayList<>(predefinedCategories);
        allCategories.addAll(customCategories);

        // Add "Add New Category"
        allCategories.add("+ Add New Category");

        String[] categoriesArray = allCategories.toArray(new String[0]);

        // Find currently selected index
        int selectedIndex = 0; // Default to first category
        for (int i = 0; i < categoriesArray.length; i++) {
            if (categoriesArray[i].equals(selectedCategory)) {
                selectedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Event Category")
                .setSingleChoiceItems(categoriesArray, selectedIndex, (dialog, which) -> {
                    String selected = categoriesArray[which];

                    // Check if user wants to add a new category
                    if (selected.equals("+ Add New Category")) {
                        dialog.dismiss();
                        showAddCategoryDialog();
                    } else {
                        selectedCategory = selected;
                        btnSelectCategory.setText(selectedCategory);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows a dialog where the organizer can type a custom
     * category name before saving it to Firestore.
     */
    private void showAddCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter category name");
        input.setMaxLines(1);

        new AlertDialog.Builder(this)
                .setTitle("Add New Category")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String categoryName = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(categoryName)) {
                        addCustomCategory(categoryName);
                    } else {
                        Toast.makeText(this, "Category name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Adds a new custom category into Firestore if it does not
     * already exist or conflict with predefined categories.
     *
     * @param categoryName name of the category to be added
     */
    private void addCustomCategory(String categoryName) {
        // Check if category already exists
        if (customCategories.contains(categoryName)) {
            Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show();
            selectedCategory = categoryName;
            btnSelectCategory.setText(selectedCategory);
            return;
        }

        // Check if it's a predefined category
        List<String> predefinedCategories = new ArrayList<>();
        predefinedCategories.add("Food & Dining");
        predefinedCategories.add("Sports & Fitness");
        predefinedCategories.add("Music & Entertainment");
        predefinedCategories.add("Education & Learning");
        predefinedCategories.add("Art & Culture");
        predefinedCategories.add("Technology");
        predefinedCategories.add("Health & Wellness");
        predefinedCategories.add("Business & Networking");
        predefinedCategories.add("Community & Social");

        if (predefinedCategories.contains(categoryName)) {
            Toast.makeText(this, "This is a predefined category", Toast.LENGTH_SHORT).show();
            selectedCategory = categoryName;
            btnSelectCategory.setText(selectedCategory);
            return;
        }

        // Add to Firestore
        Map<String, Object> categoryData = new HashMap<>();
        categoryData.put("name", categoryName);
        categoryData.put("createdAt", System.currentTimeMillis());

        // Generate a safe document ID (remove special characters, keep only alphanumeric and underscores)
        String docId = categoryName.toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")  // Replace non-alphanumeric with underscore
                .replaceAll("_+", "_")          // Replace multiple underscores with single
                .replaceAll("^_|_$", "");      // Remove leading/trailing underscores

        // If docId is empty after cleaning, use a generated ID
        if (docId.isEmpty()) {
            docId = db.collection("categories").document().getId();
        }

        db.collection("categories")
                .document(docId)
                .set(categoryData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Custom category added: " + categoryName);
                    customCategories.add(categoryName);
                    Collections.sort(customCategories);
                    selectedCategory = categoryName;
                    btnSelectCategory.setText(selectedCategory);
                    Toast.makeText(this, "Category added successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding custom category", e);
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && errorMsg.contains("PERMISSION_DENIED")) {
                        Toast.makeText(this, "Permission denied. Please check Firestore rules.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Failed to add category: " + (errorMsg != null ? errorMsg : "Unknown error"), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Opens DatePicker → TimePicker to select event datetime.
     * Ensures that the final selected datetime is not in the past.
     *
     * On valid selection:
     * - Saves the event date
     * - Updates button label with formatted date/time
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
     * Opens a date picker for selecting registration start date.
     * Ensures:
     * - Start date cannot be in the past
     * - End date remains valid
     *
     * US 02.01.04: Set registration start.
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
     * Opens a date picker for selecting registration end date.
     * Ensures:
     * - Start date has already been selected
     * - End date is after or equal to start date
     *
     * US 02.01.04: Set registration end.
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
     * Ensures stored registration end date is not before the
     * newly updated start date. Resets invalid end date.
     */
    private void checkEndDateValidity() {
        if (regStartDate != null && regEndDate != null && regEndDate.before(regStartDate)) {
            regEndDate = null;
            btnSelectRegEnd.setText("Select Registration End");
            Toast.makeText(this, "The Registration End Date was reset because it now occurs before the new Start Date.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Builds and displays a preview dialog summarizing all
     * current event inputs, such as:
     * - name, description, category
     * - capacity
     * - location
     * - selected poster status
     * - event datetime
     * - geolocation toggle state
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
     * Validates all user inputs, parses capacity, prepares an
     * Event object, and begins the creation workflow:
     * <ol>
     *   <li>Upload poster (if provided)</li>
     *   <li>Save event to Firestore</li>
     *   <li>Add organizer role if first-time organizer</li>
     *   <li>Generate and upload QR code</li>
     *   <li>Show success dialog / navigate</li>
     * </ol>
     *
     * US 02.01.01: Create event and generate QR code.
     * US 02.02.03: Enable/disable geolocation requirement.
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
                        uploadPosterAndCreateEvent(eventId, event, name);
                    } else {
                        saveEventToFirestore(eventId, event, name);
                    }
                });
    }

    /**
     * Validates required event fields before creation.
     *
     * @param name event name
     * @param description event description
     * @return true if all mandatory inputs are valid, else false
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
        String capacityStr = editCapacity.getText().toString().trim();
        if (TextUtils.isEmpty(capacityStr)) {
            editCapacity.setError("Capacity is required");
            editCapacity.requestFocus();
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
     * When upload succeeds:
     * - Retrieves the poster URL
     * - Proceeds to save event data in Firestore
     *
     * If upload fails, event is still created without a poster.
     *
     * @param eventId ID used for poster filename
     * @param event Event object to save
     * @param eventName event name for poster metadata
     */
    private void uploadPosterAndCreateEvent(String eventId, Event event, String eventName) {
        StorageReference posterRef = storage.getReference()
                .child("event_posters")
                .child(eventId + ".jpg");

        posterRef.putFile(posterUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL
                    posterRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        event.setPosterUrl(uri.toString());
                        saveEventToFirestore(eventId, event, eventName);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to upload poster, creating event without it", Toast.LENGTH_SHORT).show();
                    saveEventToFirestore(eventId, event, eventName);
                });
    }

    /**
     * Saves the event document to Firestore, assigns organizer role
     * if necessary, then triggers QR code generation and upload.
     *
     * @param eventId Firestore document ID
     * @param event event data object
     * @param eventName event name used in dialogs
     */
    private void saveEventToFirestore(String eventId, Event event, String eventName) {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("events").document(eventId)
                .set(event)
                .addOnSuccessListener(aVoid -> {
                    // Add "organizer" role to user if they don't have it
                    addOrganizerRoleToUser(userId);

                    // Generate and upload QR code
                    generateAndUploadQRCode(eventId, eventName);
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Toast.makeText(this, "Failed to create event", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Adds an "organizer" role to the user profile if they are
     * creating their first event and do not already have the role.
     *
     * @param userId Firebase UID of the organizer
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
     * Generates a QR code bitmap for the event ID, converts it to bytes,
     * uploads it to Firebase Storage, and then displays the success dialog.
     *
     * @param eventId event identifier encoded in the QR code
     * @param eventName name used for display/sharing
     */
    private void generateAndUploadQRCode(String eventId, String eventName) {
        try {
            // Generate QR code bitmap
            generateQRCodeBitmap(eventId);
            Bitmap bitmap = qrBitmap;

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
                        showSuccessAndNavigate(eventId, eventName);
                    })
                    .addOnFailureListener(e -> {
                        hideLoading();
                        // Still show success even if QR upload fails
                        showSuccessAndNavigate(eventId, eventName);
                    });

        } catch (WriterException e) {
            hideLoading();
            // Still navigate to QR code display
            showSuccessAndNavigate(eventId, eventName);
        }
    }

    /**
     * After successful event creation and QR upload, displays a QR dialog.
     * If the generated QR bitmap is missing, attempts regeneration.
     *
     * @param eventId Firestore event ID
     * @param eventName event title for display
     */
    private void showSuccessAndNavigate(String eventId, String eventName) {
        // Show QR code dialog first, then navigate when closed
        if (qrBitmap != null) {
            showQRCodeDialog(eventId, eventName);
        } else {
            // If QR bitmap is not available, try to generate it again
            try {
                generateQRCodeBitmap(eventId);
                if (qrBitmap != null) {
                    showQRCodeDialog(eventId, eventName);
                } else {
                    navigateToQRCodeActivity(eventId, eventName);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error generating QR code for display", e);
                navigateToQRCodeActivity(eventId, eventName);
            }
        }
    }

    /**
     * Generates a QR code bitmap encoding the eventId.
     *
     * @param eventId string to encode into QR code
     * @throws WriterException if QR generation fails
     */
    private void generateQRCodeBitmap(String eventId) throws WriterException {
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

        qrBitmap = bitmap;
    }

    /**
     * Shows a modal dialog displaying the generated QR code, along with:
     * - Save to gallery
     * - Share QR code
     * - Continue workflow button
     *
     * @param eventId unique event ID encoded in QR
     * @param eventName used for file naming and sharing
     */
    private void showQRCodeDialog(String eventId, String eventName) {
        try {
            // Inflate custom dialog layout
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_qr_code, null);

            // Set QR code image
            ImageView ivQrCode = dialogView.findViewById(R.id.ivQrCode);
            ivQrCode.setImageBitmap(qrBitmap);

            // Create dialog
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(false) // Prevent dismissing by clicking outside
                    .create();

            // Setup button listeners
            MaterialButton btnSave = dialogView.findViewById(R.id.btnSaveQr);
            MaterialButton btnShare = dialogView.findViewById(R.id.btnShareQr);
            MaterialButton btnClose = dialogView.findViewById(R.id.btnCloseQr);

            btnSave.setOnClickListener(v -> {
                saveQrCodeToGallery(qrBitmap, eventName);
            });

            btnShare.setOnClickListener(v -> {
                shareQrCode(qrBitmap, eventName);
            });

            btnClose.setOnClickListener(v -> {
                dialog.dismiss();
                navigateToQRCodeActivity(eventId, eventName);
            });

            // Show success toast
            Toast.makeText(this, "Event created successfully!", Toast.LENGTH_LONG).show();
            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing QR code dialog", e);
            navigateToQRCodeActivity(eventId, eventName);
        }
    }

    /**
     * Navigates the user back to MainActivity once QR dialog is closed.
     *
     * @param eventId event identifier
     * @param eventName event name
     */
    private void navigateToQRCodeActivity(String eventId, String eventName) {
        // Navigate to MainActivity after QR code is shown
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    /**
     * Saves the QR code image to device storage.
     * Handles:
     * - Android 10+ scoped storage
     * - Legacy external storage
     *
     * @param qrBitmap bitmap to store
     * @param eventName used for filename
     */
    private void saveQrCodeToGallery(Bitmap qrBitmap, String eventName) {
        try {
            String fileName = (eventName != null ? eventName.replaceAll("[^a-zA-Z0-9]", "_") : "Event") + "_QR.png";

            // For Android 10+ (API 29+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/LuckySpot");

                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    OutputStream outputStream = getContentResolver().openOutputStream(uri);
                    if (outputStream != null) {
                        qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                        outputStream.close();
                        Toast.makeText(this, "QR code saved to gallery!", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                // For older Android versions
                String imagesDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).toString() + "/LuckySpot";
                File dir = new File(imagesDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                File file = new File(dir, fileName);
                FileOutputStream fos = new FileOutputStream(file);
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();

                // Notify gallery
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(Uri.fromFile(file));
                sendBroadcast(mediaScanIntent);

                Toast.makeText(this, "QR code saved to gallery!", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error saving QR code", e);
            Toast.makeText(this, "Failed to save QR code", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shares the QR code by:
     * <ol>
     *   <li>Saving it temporarily in app cache</li>
     *   <li>Creating a content URI via FileProvider</li>
     *   <li>Launching a system share sheet</li>
     * </ol>
     *
     * @param qrBitmap QR image to share
     * @param eventName used for filename + share message
     */
    private void shareQrCode(Bitmap qrBitmap, String eventName) {
        try {
            // Save to cache directory first
            File cachePath = new File(getCacheDir(), "qr_codes");
            cachePath.mkdirs();

            String fileName = (eventName != null ? eventName.replaceAll("[^a-zA-Z0-9]", "_") : "Event") + "_QR.png";
            File file = new File(cachePath, fileName);

            FileOutputStream stream = new FileOutputStream(file);
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            // Get URI using FileProvider
            Uri contentUri = FileProvider.getUriForFile(
                    this,
                    "com.example.event_app.fileprovider",
                    file
            );

            // Create share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "Join \"" + (eventName != null ? eventName : "this event") + "\" by scanning this QR code!");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share QR Code"));
        } catch (IOException e) {
            Log.e(TAG, "Error sharing QR code", e);
            Toast.makeText(this, "Failed to share QR code", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows a loading overlay and disables the Create Event button.
     */
    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
        btnCreateEvent.setEnabled(false);
    }

    /**
     * Hides the loading overlay and re-enables the Create Event button.
     */
    private void hideLoading() {
        loadingView.setVisibility(View.GONE);
        btnCreateEvent.setEnabled(true);
    }
}
