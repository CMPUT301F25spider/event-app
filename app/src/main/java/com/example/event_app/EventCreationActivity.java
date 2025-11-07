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
        boolean isValid = true;

        // Safe null checks for EditText.getText()
        if (inputEventName.getText() == null || TextUtils.isEmpty(inputEventName.getText())) {
            inputEventName.setError("Event name required");
            isValid = false;
        }
        if (inputLocation.getText() == null || TextUtils.isEmpty(inputLocation.getText())) {
            inputLocation.setError("Location required");
            isValid = false;
        }
        if (inputEntrantsCount.getText() == null || TextUtils.isEmpty(inputEntrantsCount.getText())) {
            inputEntrantsCount.setError("Number of entrants required");
            isValid = false;
        } else {
            // Validate that entrants count is a positive number
            try {
                long count = Long.parseLong(inputEntrantsCount.getText().toString());
                if (count <= 0) {
                    inputEntrantsCount.setError("Number of entrants must be greater than 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                inputEntrantsCount.setError("Invalid number");
                isValid = false;
            }
        }

        // Validate dates - check if they're set (not empty and not just hint text)
        String startDateText = inputStartDate.getText() != null ? inputStartDate.getText().toString() : "";
        if (TextUtils.isEmpty(startDateText) || startDateText.equals(inputStartDate.getHint())) {
            inputStartDate.setError("Start date required");
            isValid = false;
        }

        String endDateText = inputEndDate.getText() != null ? inputEndDate.getText().toString() : "";
        if (TextUtils.isEmpty(endDateText) || endDateText.equals(inputEndDate.getHint())) {
            inputEndDate.setError("End date required");
            isValid = false;
        } else if (!TextUtils.isEmpty(startDateText) && !startDateText.equals(inputStartDate.getHint())) {
            // Validate that end date is after start date
            try {
                Date startDate = dateFmt.parse(startDateText);
                Date endDate = dateFmt.parse(endDateText);
                if (endDate.before(startDate)) {
                    inputEndDate.setError("End date must be after start date");
                    isValid = false;
                }
            } catch (Exception e) {
                // Date parsing error will be caught elsewhere
            }
        }

        String regOpenText = inputRegOpens.getText() != null ? inputRegOpens.getText().toString() : "";
        if (TextUtils.isEmpty(regOpenText) || regOpenText.equals(inputRegOpens.getHint())) {
            inputRegOpens.setError("Registration open date required");
            isValid = false;
        }

        String regCloseText = inputRegCloses.getText() != null ? inputRegCloses.getText().toString() : "";
        if (TextUtils.isEmpty(regCloseText) || regCloseText.equals(inputRegCloses.getHint())) {
            inputRegCloses.setError("Registration close date required");
            isValid = false;
        } else if (!TextUtils.isEmpty(regOpenText) && !regOpenText.equals(inputRegOpens.getHint())) {
            // Validate that registration close is after registration open
            try {
                Date regOpen = dateFmt.parse(regOpenText);
                Date regClose = dateFmt.parse(regCloseText);
                if (regClose.before(regOpen)) {
                    inputRegCloses.setError("Registration close must be after registration open");
                    isValid = false;
                }
            } catch (Exception e) {
                // Date parsing error will be caught elsewhere
            }
        }

        return isValid;

    }

    /**
     * Build an Event object from the form fields.
     */
    private Event buildEventFromForm() {
        Event event = new Event();

        // Safe text extraction with null checks
        String eventName = inputEventName.getText() != null ? inputEventName.getText().toString() : "";
        String location = inputLocation.getText() != null ? inputLocation.getText().toString() : "";
        String entrantsCountStr = inputEntrantsCount.getText() != null ? inputEntrantsCount.getText().toString() : "";

        event.setName(eventName);
        event.setDescription("Draft created via form"); // optional: add description field in UI
        event.setOrganizerId("organizer123"); // TODO: replace with actual logged-in organizer ID
        event.setPosterUrl("https://example.com/poster.png"); // TODO: replace with actual poster upload URL

        try {
            event.setCapacity(Long.parseLong(entrantsCountStr));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number of entrants", Toast.LENGTH_SHORT).show();
            event.setCapacity(0L);
        }

        // Parse dates - combine date and time for start date
        try {
            String startDateText = inputStartDate.getText() != null ? inputStartDate.getText().toString() : "";
            String startTimeText = inputStartTime.getText() != null ? inputStartTime.getText().toString() : "";

            if (!TextUtils.isEmpty(startDateText) && !startDateText.equals(inputStartDate.getHint())) {
                Date startDate = dateFmt.parse(startDateText);
                if (!TextUtils.isEmpty(startTimeText) && !startTimeText.equals(inputStartTime.getHint())) {
                    // Combine date and time
                    Calendar startCalCombined = Calendar.getInstance();
                    startCalCombined.setTime(startDate);
                    startCalCombined.set(Calendar.HOUR_OF_DAY, startCal.get(Calendar.HOUR_OF_DAY));
                    startCalCombined.set(Calendar.MINUTE, startCal.get(Calendar.MINUTE));
                    startCalCombined.set(Calendar.SECOND, 0);
                    startCalCombined.set(Calendar.MILLISECOND, 0);
                    event.setDate(startCalCombined.getTime());
                } else {
                    // Only date, no time
                    event.setDate(startDate);
                }
            }

            // Parse end date (if provided)
            String endDateText = inputEndDate.getText() != null ? inputEndDate.getText().toString() : "";
            if (!TextUtils.isEmpty(endDateText) && !endDateText.equals(inputEndDate.getHint())) {
                Date endDate = dateFmt.parse(endDateText);
                // Combine with end time if provided
                String endTimeText = inputEndTime.getText() != null ? inputEndTime.getText().toString() : "";
                if (!TextUtils.isEmpty(endTimeText) && !endTimeText.equals(inputEndTime.getHint())) {
                    Calendar endCalCombined = Calendar.getInstance();
                    endCalCombined.setTime(endDate);
                    endCalCombined.set(Calendar.HOUR_OF_DAY, endCal.get(Calendar.HOUR_OF_DAY));
                    endCalCombined.set(Calendar.MINUTE, endCal.get(Calendar.MINUTE));
                    endCalCombined.set(Calendar.SECOND, 0);
                    endCalCombined.set(Calendar.MILLISECOND, 0);
                    event.setEventDate(endCalCombined.getTime());
                } else {
                    event.setEventDate(endDate);
                }
            }

            // Parse registration dates
            String regOpenText = inputRegOpens.getText() != null ? inputRegOpens.getText().toString() : "";
            if (!TextUtils.isEmpty(regOpenText) && !regOpenText.equals(inputRegOpens.getHint())) {
                Date regOpen = dateFmt.parse(regOpenText);
                event.setRegistrationStartDate(regOpen);
            }

            String regCloseText = inputRegCloses.getText() != null ? inputRegCloses.getText().toString() : "";
            if (!TextUtils.isEmpty(regCloseText) && !regCloseText.equals(inputRegCloses.getHint())) {
                Date regClose = dateFmt.parse(regCloseText);
                event.setRegistrationEndDate(regClose);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Invalid date format: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        // Set location
        event.setLocation(location);

        return event;
    }
}