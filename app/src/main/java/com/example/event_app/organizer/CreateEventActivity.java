package com.example.event_app.organizer;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.event_app.R;
import com.example.event_app.models.Event;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Calendar;
import java.util.UUID;

/**
 * CreateEventActivity
 *
 * Allows organizers to create a new event with poster, details, dates/times,
 * registration windows, and geolocation toggle. Saves event data to Firestore
 * and uploads poster to Firebase Storage.
 */
public class CreateEventActivity extends AppCompatActivity {

    private ImageView imgPoster;
    private TextView txtInsertPicture;
    private EditText inputName, inputLocation, inputCapacity;
    private Button btnStartDate, btnEndDate, btnStartTime, btnEndTime,
            btnRegistrationOpen, btnRegistrationClose, btnSaveEvent, btnCancelEvent;
    private Switch switchGeolocation;

    private Uri posterUri;
    private Calendar startDate, endDate, startTime, endTime, regOpen, regClose;

    private FirebaseFirestore db;
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("posters");

        // Bind views
        imgPoster = findViewById(R.id.img_event_poster);
        txtInsertPicture = findViewById(R.id.txt_insert_picture);
        inputName = findViewById(R.id.input_event_name);
        inputLocation = findViewById(R.id.input_event_location);
        inputCapacity = findViewById(R.id.input_event_capacity);
        btnStartDate = findViewById(R.id.btn_start_date);
        btnEndDate = findViewById(R.id.btn_end_date);
        btnStartTime = findViewById(R.id.btn_start_time);
        btnEndTime = findViewById(R.id.btn_end_time);
        btnRegistrationOpen = findViewById(R.id.btn_registration_open);
        btnRegistrationClose = findViewById(R.id.btn_registration_close);
        switchGeolocation = findViewById(R.id.switch_geolocation);
        btnSaveEvent = findViewById(R.id.btn_save_event);
        btnCancelEvent = findViewById(R.id.btn_cancel_event);

        // Poster upload
        imgPoster.setOnClickListener(v -> selectPosterImage());
        txtInsertPicture.setOnClickListener(v -> selectPosterImage());

        // Date/time pickers
        btnStartDate.setOnClickListener(v -> pickDate("start"));
        btnEndDate.setOnClickListener(v -> pickDate("end"));
        btnRegistrationOpen.setOnClickListener(v -> pickDate("regOpen"));
        btnRegistrationClose.setOnClickListener(v -> pickDate("regClose"));

        btnStartTime.setOnClickListener(v -> pickTime("start"));
        btnEndTime.setOnClickListener(v -> pickTime("end"));

        // Save / Cancel
        btnSaveEvent.setOnClickListener(v -> saveEvent());
        btnCancelEvent.setOnClickListener(v -> finish());
    }

    private void selectPosterImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 100);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            posterUri = data.getData();

            // Validate poster before showing/using
            PosterValidator validator = new PosterValidator(getContentResolver(), 5L * 1024 * 1024); // 5 MB limit
            Result<Void> result = validator.validate(posterUri);

            if (result.isOk()) {
                // Safe to show preview
                imgPoster.setImageURI(posterUri);
                txtInsertPicture.setText(""); // hide overlay text
            } else {
                // Reject invalid file
                Toast.makeText(this, "Invalid poster: " + result.getErrorMessage(), Toast.LENGTH_LONG).show();
                posterUri = null; // reset so it won’t upload
            }
        }
    }

    private void pickDate(String type) {
        final Calendar c = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, y, m, d) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(y, m, d);
                    switch (type) {
                        case "start": startDate = chosen; btnStartDate.setText("Start Date: " + d + "/" + (m+1) + "/" + y); break;
                        case "end": endDate = chosen; btnEndDate.setText("End Date: " + d + "/" + (m+1) + "/" + y); break;
                        case "regOpen": regOpen = chosen; btnRegistrationOpen.setText("Registration Opens: " + d + "/" + (m+1) + "/" + y); break;
                        case "regClose": regClose = chosen; btnRegistrationClose.setText("Registration Closes: " + d + "/" + (m+1) + "/" + y); break;
                    }
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void pickTime(String type) {
        final Calendar c = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(this,
                (view, h, min) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(Calendar.HOUR_OF_DAY, h);
                    chosen.set(Calendar.MINUTE, min);
                    switch (type) {
                        case "start": startTime = chosen; btnStartTime.setText("Start Time: " + h + ":" + min); break;
                        case "end": endTime = chosen; btnEndTime.setText("End Time: " + h + ":" + min); break;
                    }
                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);
        dialog.show();
    }

    private void saveEvent() {
        String name = inputName.getText().toString().trim();
        String location = inputLocation.getText().toString().trim();
        String capacityStr = inputCapacity.getText().toString().trim();

        if (name.isEmpty() || location.isEmpty() || capacityStr.isEmpty() ||
                startDate == null || endDate == null || startTime == null || endTime == null ||
                regOpen == null || regClose == null) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int capacity = Integer.parseInt(capacityStr);
        String eventId = UUID.randomUUID().toString();

        Event event = new Event(eventId, name, "Event created", "ORG1"); // adjust organizerId
        event.setLocation(location);
        event.setCapacity((long) capacity);
        event.setEventDate(startDate.getTime()); // you can extend Event model with more fields
        event.setGeolocationEnabled(switchGeolocation.isChecked());

        if (posterUri != null) {
            StorageReference posterRef = storageRef.child(eventId + ".jpg");
            posterRef.putFile(posterUri)
                    .addOnSuccessListener(taskSnapshot ->
                            posterRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                event.setPosterUrl(uri.toString());
                                saveEventToFirestore(event);
                            }))
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Poster upload failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
        } else {
            saveEventToFirestore(event);
        }
    }

    private void saveEventToFirestore(Event event) {
        db.collection("events").document(event.getEventId())
                .set(event)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Event created successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to create event: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}