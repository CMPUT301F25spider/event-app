package com.example.event_app;
import com.example.event_app.models.Event;


import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.event_app.data.FirestoreEntrantsRepository;
import com.example.event_app.models.Event;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * EventCreationActivity
 *
 * Displays the event creation form with poster at the top and
 * event details in the bottom half. Delegates saving to FirestoreEntrantsRepository.
 */
public class EventCreationActivity extends AppCompatActivity {

    // Inputs
    private EditText inputEventName, inputLocation, inputEntrantsCount;
    private TextView inputStartDate, inputEndDate, inputStartTime, inputEndTime;
    private TextView inputRegOpens, inputRegCloses;
    private Switch switchGeolocation;

    // Buttons
    private Button btnSaveDraft, btnCancel;

    // Calendar state
    private final Calendar startCal = Calendar.getInstance();
    private final Calendar endCal = Calendar.getInstance();
    private final Calendar regOpenCal = Calendar.getInstance();
    private final Calendar regCloseCal = Calendar.getInstance();

    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

    // Repository
    private FirestoreEntrantsRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        repository = new FirestoreEntrantsRepository();

        bindViews();
        wirePickers();
        wireActions();
    }

    private void bindViews() {
        inputEventName = findViewById(R.id.inputEventName);
        inputLocation = findViewById(R.id.inputLocation);
        inputEntrantsCount = findViewById(R.id.inputEntrantsCount);
        inputStartDate = findViewById(R.id.inputStartDate);
        inputEndDate = findViewById(R.id.inputEndDate);
        inputStartTime = findViewById(R.id.inputStartTime);
        inputEndTime = findViewById(R.id.inputEndTime);
        inputRegOpens = findViewById(R.id.inputRegOpens);
        inputRegCloses = findViewById(R.id.inputRegCloses);
        switchGeolocation = findViewById(R.id.switchGeolocation);
        btnSaveDraft = findViewById(R.id.btnSaveDraft);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void wirePickers() {
        inputStartDate.setOnClickListener(v -> showDatePicker(startCal, inputStartDate));
        inputEndDate.setOnClickListener(v -> showDatePicker(endCal, inputEndDate));
        inputRegOpens.setOnClickListener(v -> showDatePicker(regOpenCal, inputRegOpens));
        inputRegCloses.setOnClickListener(v -> showDatePicker(regCloseCal, inputRegCloses));

        inputStartTime.setOnClickListener(v -> showTimePicker(startCal, inputStartTime));
        inputEndTime.setOnClickListener(v -> showTimePicker(endCal, inputEndTime));
    }

    private void wireActions() {
        btnSaveDraft.setOnClickListener(v -> {
            if (validateForm()) {
                Event draft = buildEventFromForm();
                repository.saveEventDraft(draft,
                        () -> {
                            Toast.makeText(this, "Draft saved to Firestore!", Toast.LENGTH_SHORT).show();
                            finish();
                        },
                        error -> Toast.makeText(this, "Error saving draft: " + error.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });

        btnCancel.setOnClickListener(v -> finish());
    }

    private void showDatePicker(Calendar cal, TextView target) {
        new DatePickerDialog(this, (view, year, month, day) -> {
            cal.set(year, month, day);
            target.setText(dateFmt.format(cal.getTime()));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(Calendar cal, TextView target) {
        new TimePickerDialog(this, (view, hour, minute) -> {
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            target.setText(timeFmt.format(cal.getTime()));
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
    }

    private boolean validateForm() {
        if (TextUtils.isEmpty(inputEventName.getText())) {
            inputEventName.setError("Event name required");
            return false;
        }
        if (TextUtils.isEmpty(inputLocation.getText())) {
            inputLocation.setError("Location required");
            return false;
        }
        if (TextUtils.isEmpty(inputEntrantsCount.getText())) {
            inputEntrantsCount.setError("Number of entrants required");
            return false;
        }
        return true;
    }

    /**
     * Build an Event object from the form fields.
     */
    private Event buildEventFromForm() {
        Event event = new Event();
        event.setName(inputEventName.getText().toString());
        event.setDescription("Draft created via form"); // optional: add description field in UI
        event.setOrganizerId("organizer123"); // TODO: replace with actual logged-in organizer ID
        event.setPosterUrl("https://example.com/poster.png"); // TODO: replace with actual poster upload URL
        event.setCapacity(Long.parseLong(inputEntrantsCount.getText().toString()));

        try {
            Date startDate = dateFmt.parse(inputStartDate.getText().toString());
            Date endDate = dateFmt.parse(inputEndDate.getText().toString());
            Date regOpen = dateFmt.parse(inputRegOpens.getText().toString());
            Date regClose = dateFmt.parse(inputRegCloses.getText().toString());

            event.setDate(startDate);
            event.setRegistrationStartDate(regOpen);
            event.setRegistrationEndDate(regClose);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
        }

        return event;
    }
}