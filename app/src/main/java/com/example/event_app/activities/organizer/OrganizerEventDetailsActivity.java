package com.example.event_app.activities.organizer;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.example.event_app.R;
import com.example.event_app.activities.organizer.ViewEntrantsActivity;
import com.example.event_app.activities.organizer.ViewEntrantMapActivity;
import com.example.event_app.models.Event;
import com.example.event_app.models.Notification;
import com.example.event_app.models.User;
import com.example.event_app.services.NotificationService;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.android.material.button.MaterialButton;

/**
 * OrganizerEventDetailsActivity — Provides full event-management functionality
 * for organizers, including:
 * • Running the lottery and drawing replacements (US 02.05.02, US 02.05.03)
 * • Viewing entrant lists and entrant locations on a map (US 02.02.01–02)
 * • Generating, saving, and sharing event QR codes (US 02.01.01)
 * • Sending notifications to entrants (US 02.07.01–03)
 * • Updating the event poster (US 02.04.02)
 * • Exporting waiting/selected/attending lists as CSV (US 02.06.05)
 * • Cancelling the event and notifying all entrants
 *
 * Includes full real-time Firestore updates so organizers always see live data.
 */
public class OrganizerEventDetailsActivity extends AppCompatActivity {
    // UI Elements
    private Toolbar toolbar;
    private TextView tvEventName, tvCapacity;
    private TextView tvWaitingCount, tvSelectedCount, tvAttendingCount;
    private MaterialButton btnRunLottery, btnViewEntrants, btnViewMap, btnGenerateQR;
    private MaterialButton btnSendNotification, btnExportCSV, btnUpdatePoster, btnCancelEvent;
    private MaterialButton btnSendReminder; // Button for event reminders
    private MaterialButton btnDrawReplacement; //Replacement lottery button
    private View loadingView;

    // Data
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private NotificationService notificationService;
    private String eventId;
    private Event event;

    private static final String TAG = "OrganizerEventDetails";

    // Real-time listener for event updates
    private com.google.firebase.firestore.ListenerRegistration eventListener;

    // Image picker
    private Uri newPosterUri;
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    newPosterUri = result.getData().getData();
                    updateEventPoster();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_event_details);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Get event ID
        eventId = getIntent().getStringExtra("EVENT_ID");
        if (eventId == null) {
            Toast.makeText(this, "Error: No event ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        notificationService = new NotificationService();

        // Initialize views
        initViews();

        // Load event
        loadEventDetails();
    }

    /**
     * Initializes all UI components, toolbar, and buttons.
     * Binds click listeners for all event-management actions:
     * lottery, viewing entrants, QR generation, notifications,
     * CSV export, poster update, cancellation, and replacement drawing.
     */
    private void initViews() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Text views
        tvEventName = findViewById(R.id.tvEventName);
        tvCapacity = findViewById(R.id.tvCapacity);
        tvWaitingCount = findViewById(R.id.tvWaitingCount);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        tvAttendingCount = findViewById(R.id.tvAttendingCount);

        // Action buttons
        btnRunLottery = findViewById(R.id.btnRunLottery);
        btnViewEntrants = findViewById(R.id.btnViewEntrants);
        btnViewMap = findViewById(R.id.btnViewMap);
        btnGenerateQR = findViewById(R.id.btnGenerateQR);
        btnSendNotification = findViewById(R.id.btnSendNotification);
        btnExportCSV = findViewById(R.id.btnExportCSV);
        btnUpdatePoster = findViewById(R.id.btnUpdatePoster);
        btnCancelEvent = findViewById(R.id.btnCancelEvent);
        btnDrawReplacement = findViewById(R.id.btnDrawReplacement);

        // Other views
        loadingView = findViewById(R.id.loadingView);

        // Button listeners
        btnRunLottery.setOnClickListener(v -> showLotteryDialog());
        btnViewEntrants.setOnClickListener(v -> openViewEntrantsActivity());
        btnViewMap.setOnClickListener(v -> showEntrantMap());
        btnGenerateQR.setOnClickListener(v -> showQRCode());
        btnSendNotification.setOnClickListener(v -> showMessageDialog());
        btnExportCSV.setOnClickListener(v -> exportToCSV());
        btnUpdatePoster.setOnClickListener(v -> selectNewPoster());
        btnCancelEvent.setOnClickListener(v -> showCancelEventDialog());
        btnDrawReplacement.setOnClickListener(v -> showDrawReplacementDialog());

        if (btnSendReminder != null) {
            btnSendReminder.setOnClickListener(v -> sendEventReminders());
        }
    }

    /**
     * Displays event information (name, capacity, waiting/selected/attending counts)
     * and updates button enabled states based on the event’s current state.
     */
    private void displayEventInfo() {
        tvEventName.setText(event.getName());

        if (event.getCapacity() != null) {
            tvCapacity.setText(String.format("Capacity: %d spots", event.getCapacity()));
        } else {
            tvCapacity.setText("Capacity: Unlimited");
        }

        int waitingCount = event.getWaitingList() != null ? event.getWaitingList().size() : 0;
        int selectedCount = event.getSelectedList() != null ? event.getSelectedList().size() : 0;
        int attendingCount = event.getSignedUpUsers() != null ? event.getSignedUpUsers().size() : 0;

        tvWaitingCount.setText(String.valueOf(waitingCount));
        tvSelectedCount.setText(String.valueOf(selectedCount));
        tvAttendingCount.setText(String.valueOf(attendingCount));

        btnRunLottery.setEnabled(event.getCapacity() != null && waitingCount > 0);
        btnViewMap.setEnabled(event.isGeolocationEnabled());

        // Update button visibility based on lottery state
        updateLotteryButtonVisibility();
    }

    /**
     * Controls which lottery button is shown:
     * • Before lottery → shows “Run Lottery”
     * • After lottery → shows “Draw Replacement”
     *
     * Also disables the replacement button if:
     * • capacity is full, or
     * • the replacement pool is empty.
     */
    /**
     * Controls which lottery button is shown:
     * • Before lottery → shows "Run Lottery"
     * • After lottery → shows "Draw Replacement"
     *
     * Also disables the replacement button if:
     * • all selected spots are filled (selected + attending = capacity), or
     * • the replacement pool is empty.
     *
     *
     */
    private void updateLotteryButtonVisibility() {
        if (event.isLotteryRun()) {
            btnRunLottery.setVisibility(View.GONE);
            btnDrawReplacement.setVisibility(View.VISIBLE);

            int selectedCount = event.getSelectedList() != null ? event.getSelectedList().size() : 0;
            int attendingCount = event.getSignedUpUsers() != null ? event.getSignedUpUsers().size() : 0;
            int capacity = event.getCapacity() != null ? event.getCapacity().intValue() : 0;
            int poolSize = event.getNotSelectedList() != null ? event.getNotSelectedList().size() : 0;

            // Total slots taken = people selected + people attending
            int totalSlotsTaken = selectedCount + attendingCount;

            // Can draw more if:
            // 1. Total slots taken < capacity (means someone declined)
            // 2. Replacement pool is not empty
            boolean canDrawMore = (totalSlotsTaken < capacity) && poolSize > 0;

            btnDrawReplacement.setEnabled(canDrawMore);

            if (!canDrawMore) {
                if (totalSlotsTaken >= capacity) {
                    btnDrawReplacement.setText("All Slots Filled");
                } else if (poolSize == 0) {
                    btnDrawReplacement.setText("Pool Empty");
                }
            } else {
                int slotsAvailable = capacity - totalSlotsTaken;
                btnDrawReplacement.setText("Draw Replacement (" + slotsAvailable + " slot" +
                        (slotsAvailable == 1 ? "" : "s") + ")");
            }
        } else {
            // Lottery not run yet - show run lottery button
            btnRunLottery.setVisibility(View.VISIBLE);
            btnDrawReplacement.setVisibility(View.GONE);
        }
    }

    /**
     * Opens ViewEntrantsActivity to show waiting, selected, and attending lists.
     */
    private void openViewEntrantsActivity() {
        Intent intent = new Intent(this, ViewEntrantsActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        startActivity(intent);
    }

    /**
     * Loads event details using a real-time Firestore listener.
     * Automatically updates the UI whenever event data changes.
     *
     * US 02.02.01–04: Organizers see live entrant list changes.
     */
    private void loadEventDetails() {
        showLoading();

        // Remove old listener if exists
        if (eventListener != null) {
            eventListener.remove();
        }

        //Real-time listener - Updates automatically when event changes!
        eventListener = db.collection("events").document(eventId)
                .addSnapshotListener((document, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to event", error);
                        Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show();
                        hideLoading();
                        return;
                    }

                    if (document == null || !document.exists()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        hideLoading();
                        finish();
                        return;
                    }

                    event = document.toObject(Event.class);
                    if (event != null) {
                        event.setId(document.getId());
                        displayEventInfo();
                        Log.d(TAG, "⚡ Real-time update: Event details refreshed");
                    }
                    hideLoading();
                });
    }

    /**
     * Opens ViewEntrantMapActivity to display entrant locations on a map.
     *
     * Validates:
     * • event is loaded
     * • geolocation is enabled
     * • entrant location data exists
     *
     * US 02.02.02: View entrant map.
     */
    private void showEntrantMap() {
        // Check if event is loaded
        if (event == null) {
            Toast.makeText(this, "Please wait for event to load...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check geolocation enabled
        if (!event.isGeolocationEnabled()) {
            Toast.makeText(this, "Geolocation not enabled for this event", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for location data
        if (event.getEntrantLocations() == null || event.getEntrantLocations().isEmpty()) {
            Toast.makeText(this, "No entrant locations available yet. Entrants need to join first!", Toast.LENGTH_LONG).show();
            return;
        }
        // Launch map activity
        Intent intent = new Intent(this, ViewEntrantMapActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        startActivity(intent);
    }

    /**
     * Generates a QR code for the event ID and displays it in a custom dialog.
     * Provides options to save the QR code to the gallery or share it externally.
     *
     * US 02.01.01: Generate event QR code.
     */
    private void showQRCode() {
        try {
            // Generate QR code bitmap
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(eventId, BarcodeFormat.QR_CODE, 512, 512);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            final Bitmap qrBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    qrBitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            // Inflate custom dialog layout
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_qr_code, null);

            // Set QR code image
            ImageView ivQrCode = dialogView.findViewById(R.id.ivQrCode);
            ivQrCode.setImageBitmap(qrBitmap);

            // Create dialog
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create();

            // Setup button listeners
            MaterialButton btnSave = dialogView.findViewById(R.id.btnSaveQr);
            MaterialButton btnShare = dialogView.findViewById(R.id.btnShareQr);
            MaterialButton btnClose = dialogView.findViewById(R.id.btnCloseQr);

            btnSave.setOnClickListener(v -> {
                saveQrCodeToGallery(qrBitmap);
            });

            btnShare.setOnClickListener(v -> {
                shareQrCode(qrBitmap);
            });

            btnClose.setOnClickListener(v -> dialog.dismiss());

            dialog.show();

        } catch (WriterException e) {
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Saves the generated QR code image to the device’s gallery.
     *
     * Handles both:
     * • Android Q+ scoped storage
     * • Legacy external storage paths
     *
     * @param qrBitmap QR code bitmap to save
     */
    private void saveQrCodeToGallery(Bitmap qrBitmap) {
        try {
            String fileName = event.getName().replaceAll("[^a-zA-Z0-9]", "_") + "_QR.png";

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
     * Shares the QR code via external apps.
     * Saves a temporary file, exposes it via FileProvider,
     * and attaches it to an ACTION_SEND intent.
     *
     * @param qrBitmap QR code bitmap to share
     */
    private void shareQrCode(Bitmap qrBitmap) {
        try {
            // Save to cache directory first
            File cachePath = new File(getCacheDir(), "qr_codes");
            cachePath.mkdirs();

            String fileName = event.getName().replaceAll("[^a-zA-Z0-9]", "_") + "_QR.png";
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
                    "Join \"" + event.getName() + "\" by scanning this QR code!");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share QR Code"));

        } catch (IOException e) {
            Toast.makeText(this, "Failed to share QR code", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows confirmation dialog before running the lottery.
     * Prevents re-running and validates waiting list + capacity.
     *
     * US 02.05.02: Run lottery.
     */
    private void showLotteryDialog() {
        // Prevent running lottery twice
        if (event.isLotteryRun()) {
            Toast.makeText(this, "Lottery already run! Use 'Draw Replacement' instead.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (event.getCapacity() == null) {
            Toast.makeText(this, "No capacity set for this event", Toast.LENGTH_SHORT).show();
            return;
        }

        int waitingCount = event.getWaitingList() != null ? event.getWaitingList().size() : 0;
        int capacity = event.getCapacity().intValue();

        if (waitingCount == 0) {
            Toast.makeText(this, "No one on waiting list", Toast.LENGTH_SHORT).show();
            return;
        }

        int toSelect = Math.min(waitingCount, capacity);

        new AlertDialog.Builder(this)
                .setTitle("Run Lottery")
                .setMessage(String.format(
                        "Select %d winners from %d people on waiting list?\n\n" +
                                "⚠️ This can only be done ONCE.\n" +
                                "Remaining entrants will go into replacement pool.\n\n" +
                                "Notifications will be sent to all participants.",
                        toSelect, waitingCount))
                .setPositiveButton("Run Lottery", (dialog, which) -> runLottery(toSelect))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Randomly selects winners from the waiting list, assigns the non-winners
     * to the replacement pool, updates Firestore, and sends notifications.
     *
     * @param numberOfWinners number of users to select as winners
     */
    private void runLottery(int numberOfWinners) {
        btnRunLottery.setEnabled(false);

        List<String> waitingList = new ArrayList<>(event.getWaitingList());
        Collections.shuffle(waitingList);

        // Select winners
        List<String> winners = waitingList.subList(0, Math.min(numberOfWinners, waitingList.size()));

        // Get non-winners (replacement pool)
        List<String> notSelected = new ArrayList<>(waitingList);
        notSelected.removeAll(winners);

        if (event.getSelectedList() == null) {
            event.setSelectedList(new ArrayList<>());
        }

        // Add winners to selected list
        for (String winner : winners) {
            if (!event.getSelectedList().contains(winner)) {
                event.getSelectedList().add(winner);
            }
        }

        // Update Firebase with notSelectedList and lotteryRun flag
        db.collection("events").document(eventId)
                .update(
                        "selectedList", event.getSelectedList(),
                        "notSelectedList", notSelected,           // Save replacement pool
                        "lotteryRun", true,                       // Mark lottery as run
                        "lotteryDate", System.currentTimeMillis(), // Save when lottery ran
                        "totalSelected", event.getSelectedList().size()
                )
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Lottery completed: " + winners.size() + " winners, " +
                            notSelected.size() + " in replacement pool");
                    Toast.makeText(this, winners.size() + " winners selected! Pool: " +
                            notSelected.size(), Toast.LENGTH_LONG).show();

                    // Send notifications to winners and non-winners
                    sendLotteryNotifications(winners, notSelected);

                    loadEventDetails();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error running lottery", e);
                    Toast.makeText(this, "Failed to run lottery", Toast.LENGTH_SHORT).show();
                    btnRunLottery.setEnabled(true);
                });
    }

    /**
     * Shows confirmation dialog before drawing a single replacement from
     * the replacement pool after the main lottery.
     *
     * US 02.05.03: Draw replacement applicant.
     */
    private void showDrawReplacementDialog() {
        int poolSize = event.getNotSelectedList() != null ? event.getNotSelectedList().size() : 0;
        int spotsRemaining = event.getSpotsRemaining();

        if (poolSize == 0) {
            Toast.makeText(this, "No one in replacement pool", Toast.LENGTH_SHORT).show();
            return;
        }

        if (spotsRemaining <= 0) {
            Toast.makeText(this, "Capacity is full!", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Draw Replacement")
                .setMessage(String.format(
                        "Draw 1 replacement from pool of %d?\n\n" +
                                "Spots remaining: %d\n" +
                                "Pool available: %d",
                        poolSize, spotsRemaining, poolSize))
                .setPositiveButton("Draw", (dialog, which) -> drawReplacement())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Randomly selects one user from notSelectedList,
     * moves them into selectedList, logs the action,
     * and sends a winner notification.
     *
     * US 02.05.03: Draw replacement.
     */
    private void drawReplacement() {
        btnDrawReplacement.setEnabled(false);

        List<String> pool = new ArrayList<>(event.getNotSelectedList());

        if (pool.isEmpty()) {
            Toast.makeText(this, "Replacement pool is empty", Toast.LENGTH_SHORT).show();
            btnDrawReplacement.setEnabled(true);
            return;
        }

        if (event.isCapacityFull()) {
            Toast.makeText(this, "Event is at full capacity", Toast.LENGTH_SHORT).show();
            btnDrawReplacement.setEnabled(true);
            return;
        }

        Collections.shuffle(pool);
        String replacementUserId = pool.get(0);

        // Create log entry
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("replacementUserId", replacementUserId);
        logEntry.put("timestamp", System.currentTimeMillis());
        logEntry.put("reason", "Manual draw by organizer");

        db.collection("events").document(eventId)
                .update(
                        "selectedList", FieldValue.arrayUnion(replacementUserId),
                        "notSelectedList", FieldValue.arrayRemove(replacementUserId),
                        "replacementLog", FieldValue.arrayUnion(logEntry)  // ✨ NEW: Log it
                )
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Replacement drawn and logged: " + replacementUserId);
                    Toast.makeText(this, "Replacement selected! Sending notification...",
                            Toast.LENGTH_SHORT).show();

                    notificationService.sendNotification(
                            replacementUserId,
                            eventId,
                            event.getName(),
                            Notification.TYPE_LOTTERY_WON,
                            "🎉 Good News - You've Been Selected!",
                            "A spot opened up for " + event.getName() +
                                    "! You've been selected from the waiting list. Check your invitations to accept or decline.",
                            null
                    );

                    loadEventDetails();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error drawing replacement", e);
                    Toast.makeText(this, "Failed to draw replacement", Toast.LENGTH_SHORT).show();
                    btnDrawReplacement.setEnabled(true);
                });
    }

    /**
     * Sends bulk notifications to:
     * • Lottery winners
     * • Non-selected entrants (placed in replacement pool)
     *
     * @param winners list of user IDs selected as winners
     * @param notSelected list of user IDs placed in replacement pool
     */
    private void sendLotteryNotifications(List<String> winners, List<String> notSelected) {
        String eventName = event.getName();

        // Send winner notifications
        if (!winners.isEmpty()) {
            notificationService.sendBulkNotifications(
                    winners,
                    eventId,
                    eventName,
                    Notification.TYPE_LOTTERY_WON,
                    "🎉 You've Been Selected!",
                    "Congratulations! You've been selected for " + eventName + ". Check your invitations to accept or decline.",
                    (successCount, failureCount) -> {
                        Log.d(TAG, "Sent " + successCount + " winner notifications");
                        runOnUiThread(() -> {
                            Toast.makeText(this,
                                    "Notified " + successCount + " winners!",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
            );
        }

        // Send not-selected notifications
        if (!notSelected.isEmpty()) {
            notificationService.sendBulkNotifications(
                    notSelected,
                    eventId,
                    eventName,
                    Notification.TYPE_LOTTERY_LOST,
                    "Lottery Results",
                    "You weren't selected for " + eventName + " this time. You may still have a chance if spots become available!",
                    (successCount, failureCount) -> {
                        Log.d(TAG, "Sent " + successCount + " not-selected notifications");
                    }
            );
        }
    }

    /**
     * Opens a dialog allowing the organizer to choose which entrant list
     * to export (waiting, selected, attending). Delegates to performExport().
     *
     * US 02.06.05: Export entrants to CSV.
     */
    private void exportToCSV() {
        String[] options = {"Waiting List", "Selected", "Attending"};

        new AlertDialog.Builder(this)
                .setTitle("Export List")
                .setItems(options, (dialog, which) -> {
                    String listType = "";
                    String listName = "";
                    switch (which) {
                        case 0:
                            listType = "waiting";
                            listName = "waiting_list";
                            break;
                        case 1:
                            listType = "selected";
                            listName = "selected";
                            break;
                        case 2:
                            listType = "attending";
                            listName = "attending";
                            break;
                    }
                    performExport(listType, listName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Retrieves the selected group of entrants from the event and fetches
     * all corresponding User documents from Firestore. Once all users are
     * loaded, createCSVFile() is called to generate the CSV file.
     *
     * @param listType one of "waiting", "selected", or "attending"
     * @param listName filename suffix to distinguish CSV exports
     */
    private void performExport(String listType, String listName) {
        btnExportCSV.setEnabled(false);

        List<String> userIdsList = new ArrayList<>();

        switch (listType) {
            case "waiting":
                if (event.getWaitingList() != null) {
                    userIdsList.addAll(event.getWaitingList());
                }
                break;
            case "selected":
                if (event.getSelectedList() != null) {
                    userIdsList.addAll(event.getSelectedList());
                }
                break;
            case "attending":
                if (event.getSignedUpUsers() != null) {
                    userIdsList.addAll(event.getSignedUpUsers());
                }
                break;
        }

        final List<String> userIds = new ArrayList<>(userIdsList);

        if (userIds.isEmpty()) {
            Toast.makeText(this, "No entrants to export", Toast.LENGTH_SHORT).show();
            btnExportCSV.setEnabled(true);
            return;
        }

        List<User> users = new ArrayList<>();
        final int totalUsers = userIds.size();
        final int[] completed = {0};

        for (String userId : userIds) {
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                users.add(user);
                            }
                        }

                        completed[0]++;

                        if (completed[0] == totalUsers) {
                            createCSVFile(users, listName);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching user", e);
                        completed[0]++;

                        if (completed[0] == totalUsers) {
                            createCSVFile(users, listName);
                        }
                    });
        }
    }

    /**
     * Creates a CSV file containing entrant information (name, email, phone)
     * and stores it in the device's Downloads directory.
     *
     * @param users list of User objects to export
     * @param listName identifier for filename (waiting_list, selected, attending)
     */
    private void createCSVFile(List<User> users, String listName) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = event.getName().replaceAll("[^a-zA-Z0-9]", "_") + "_" + listName + "_" + timestamp + ".csv";

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File csvFile = new File(downloadsDir, fileName);

            FileWriter writer = new FileWriter(csvFile);
            writer.append("Name,Email,Phone\n");

            for (User user : users) {
                writer.append(user.getName() != null ? user.getName() : "").append(",");
                writer.append(user.getEmail() != null ? user.getEmail() : "").append(",");
                writer.append(user.getPhoneNumber() != null ? user.getPhoneNumber() : "").append("\n");
            }

            writer.flush();
            writer.close();

            Toast.makeText(this, "Exported " + users.size() + " entrants to Downloads", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to export CSV", Toast.LENGTH_SHORT).show();
        }

        btnExportCSV.setEnabled(true);
    }

    /**
     * Launches the system image picker to allow the organizer to choose
     * a new event poster image from device storage.
     */
    private void selectNewPoster() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    /**
     * Uploads the newly selected event poster to Firebase Storage, retrieves
     * its public URL, updates the Firestore event document, and refreshes UI.
     *
     * Disables the button during upload to prevent duplicate submissions.
     */
    private void updateEventPoster() {
        if (newPosterUri == null) return;

        btnUpdatePoster.setEnabled(false);
        Toast.makeText(this, "Uploading new poster...", Toast.LENGTH_SHORT).show();

        StorageReference posterRef = storage.getReference()
                .child("event_posters")
                .child(eventId + ".jpg");

        posterRef.putFile(newPosterUri)
                .addOnSuccessListener(taskSnapshot -> {
                    posterRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        db.collection("events").document(eventId)
                                .update("posterUrl", uri.toString())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Poster updated successfully!", Toast.LENGTH_SHORT).show();
                                    event.setPosterUrl(uri.toString());
                                    btnUpdatePoster.setEnabled(true);
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error uploading poster", e);
                    Toast.makeText(this, "Failed to update poster", Toast.LENGTH_SHORT).show();
                    btnUpdatePoster.setEnabled(true);
                });
    }

    /**
     * Displays a dialog allowing the organizer to choose which group of
     * entrants (waiting, selected, attending) should receive a message.
     *
     * US 02.07.01–03: Send notifications to entrants.
     */
    private void showMessageDialog() {
        String[] options = {"Waiting List", "Selected", "Attending"};

        new AlertDialog.Builder(this)
                .setTitle("Send Message To")
                .setItems(options, (dialog, which) -> {
                    String group = "";
                    switch (which) {
                        case 0: group = "waiting"; break;
                        case 1: group = "selected"; break;
                        case 2: group = "attending"; break;
                    }
                    showMessageInputDialog(group);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows an input dialog where the organizer types a custom message.
     * Once confirmed, sendMessageToEntrants() is called.
     *
     * @param group target group for message ("waiting", "selected", "attending")
     */
    private void showMessageInputDialog(String group) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_send_message, null);
        EditText editMessage = dialogView.findViewById(R.id.editMessage);

        new AlertDialog.Builder(this)
                .setTitle("Send Message to " + capitalizeFirst(group))
                .setView(dialogView)
                .setPositiveButton("Send", (dialog, which) -> {
                    String message = editMessage.getText().toString().trim();
                    if (!message.isEmpty()) {
                        sendMessageToEntrants(message, group);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Sends a custom notification message to all users in the selected group.
     * Uses NotificationService to issue bulk notifications.
     *
     * @param message text content of the message
     * @param group one of "waiting", "selected", or "attending"
     */
    private void sendMessageToEntrants(String message, String group) {
        List<String> userIds = new ArrayList<>();

        switch (group) {
            case "waiting":
                userIds = event.getWaitingList() != null ? event.getWaitingList() : new ArrayList<>();
                break;
            case "selected":
                userIds = event.getSelectedList() != null ? event.getSelectedList() : new ArrayList<>();
                break;
            case "attending":
                userIds = event.getSignedUpUsers() != null ? event.getSignedUpUsers() : new ArrayList<>();
                break;
        }

        if (userIds.isEmpty()) {
            Toast.makeText(this, "No entrants to message", Toast.LENGTH_SHORT).show();
            return;
        }

        // Send notifications
        notificationService.sendBulkNotifications(
                userIds,
                eventId,
                event.getName(),
                Notification.TYPE_ORGANIZER_MESSAGE,
                "Message from Organizer",
                message,
                (successCount, failureCount) -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this,
                                "Message sent to " + successCount + " entrants!",
                                Toast.LENGTH_LONG).show();
                    });
                    Log.d(TAG, "Message sent: " + message + " to " + successCount + " users");
                }
        );
    }

    /**
     * Sends reminder notifications to all attendees for the event.
     * Triggered manually by the organizer.
     *
     * US 02.07.03: Send event reminders.
     */
    private void sendEventReminders() {
        String eventName = event.getName();
        List<String> attendees = event.getSignedUpUsers();

        if (attendees == null || attendees.isEmpty()) {
            Toast.makeText(this, "No attendees to remind", Toast.LENGTH_SHORT).show();
            return;
        }

        notificationService.sendBulkNotifications(
                attendees,
                eventId,
                eventName,
                Notification.TYPE_EVENT_REMINDER,
                "⏰ Event Reminder",
                "Don't forget! " + eventName + " is happening soon. We're looking forward to seeing you!",
                (successCount, failureCount) -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this,
                                "Sent reminders to " + successCount + " attendees",
                                Toast.LENGTH_SHORT).show();
                    });
                }
        );
    }

    /**
     * Shows a confirmation dialog before cancelling the event.
     * Cancellation is irreversible and affects all entrants.
     */
    private void showCancelEventDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Event")
                .setMessage("Are you sure you want to cancel this event? This action cannot be undone.")
                .setPositiveButton("Yes, Cancel Event", (dialog, which) -> cancelEvent())
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Updates the event status to "cancelled" in Firestore, records timestamp,
     * disables UI, and triggers notifications to all affected entrants.
     *
     * Calls notifyEntrantsOfCancellation() after success.
     */
    private void cancelEvent() {
        btnCancelEvent.setEnabled(false);

        db.collection("events").document(eventId)
                .update(
                        "status", "cancelled",
                        "cancelledAt", System.currentTimeMillis()
                )
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Event cancelled");

                    // ✨ NEW: Notify all entrants
                    notifyEntrantsOfCancellation();

                    Toast.makeText(this,
                            "Event cancelled. All entrants will be notified.",
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error cancelling event", e);
                    Toast.makeText(this, "Failed to cancel event", Toast.LENGTH_SHORT).show();
                    btnCancelEvent.setEnabled(true);
                });
    }

    /**
     * Collects all entrants (waiting, selected, attending) without duplicates
     * and sends a bulk cancellation notification to each.
     *
     * Notifies users that the event is no longer happening.
     */
    private void notifyEntrantsOfCancellation() {
        List<String> allEntrants = new ArrayList<>();

        // Collect all entrants (waiting, selected, attending)
        if (event.getWaitingList() != null) {
            allEntrants.addAll(event.getWaitingList());
        }
        if (event.getSelectedList() != null) {
            allEntrants.addAll(event.getSelectedList());
        }
        if (event.getSignedUpUsers() != null) {
            allEntrants.addAll(event.getSignedUpUsers());
        }

        // Remove duplicates using Set
        List<String> uniqueEntrants = new ArrayList<>(new java.util.HashSet<>(allEntrants));

        if (uniqueEntrants.isEmpty()) {
            Log.d(TAG, "No entrants to notify");
            return;
        }

        // Send cancellation notifications
        notificationService.sendBulkNotifications(
                uniqueEntrants,
                eventId,
                event.getName(),
                "event_cancelled",  // Or use Notification.TYPE_EVENT_CANCELLED
                "❌ Event Cancelled",
                "Unfortunately, \"" + event.getName() +
                        "\" has been cancelled by the organizer. We apologize for any inconvenience.",
                (successCount, failureCount) -> {
                    Log.d(TAG, "📧 Sent cancellation notification to " +
                            successCount + " entrants");
                }
        );
    }

    /**
     * Capitalizes the first letter of a string.
     *
     * @param str input string
     * @return string with first letter capitalized, or original if null/empty
     */
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Displays the loading overlay while network operations are in progress.
     */
    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
    }

    /**
     * Hides the loading overlay when operations complete.
     */
    private void hideLoading() {
        loadingView.setVisibility(View.GONE);
    }

    /**
     * Cleans up the Firestore event listener to prevent memory leaks
     * when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ✨ Clean up real-time listener to prevent memory leaks
        if (eventListener != null) {
            eventListener.remove();
            eventListener = null;}
    }
}