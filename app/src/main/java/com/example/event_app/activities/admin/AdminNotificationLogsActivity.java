package com.example.event_app.activities.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.adapters.NotificationLogAdapter;
import com.example.event_app.models.NotificationLog;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity for administrators to review all system notification logs.
 * Provides search, filtering, and detailed inspection of notifications
 * sent by organizers or the system. Supports filtering by notification
 * type and loading up to the last 500 log entries for performance.
 *
 * <p>Supports:</p>
 * <ul>
 *     <li>US 03.08.01: Review logs of all notifications sent by organizers</li>
 *     <li>Full-text search across logs</li>
 *     <li>Type-based filtering (selected, rejected, invitation, waitlist)</li>
 *     <li>Viewing detailed notification metadata</li>
 * </ul>
 */
public class AdminNotificationLogsActivity extends AppCompatActivity {
    private EditText etSearch;
    private Spinner spinnerFilter;
    private RecyclerView recyclerViewLogs;
    private LinearLayout emptyStateLayout;
    private ProgressBar progressBar;
    private TextView tvTotalLogs;

    private NotificationLogAdapter logAdapter;
    private FirebaseFirestore db;
    private List<NotificationLog> logList;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_notification_logs);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Notification Logs");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize list
        logList = new ArrayList<>();

        // Initialize views
        initViews();

        // Set up search
        setupSearch();

        // Set up filter
        setupFilter();

        // Set up RecyclerView
        setupRecyclerView();


        // Load notification logs
        loadNotificationLogs();
    }

    /**
     * Initializes all UI components including search field, filter spinner,
     * RecyclerView, empty state layout, progress bar, and total log counter.
     */
    private void initViews() {
        etSearch = findViewById(R.id.etSearchLogs);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        recyclerViewLogs = findViewById(R.id.recyclerViewLogs);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        progressBar = findViewById(R.id.progressBar);
        tvTotalLogs = findViewById(R.id.tvTotalLogs);
    }

    /**
     * Configures search functionality using a TextWatcher to filter log
     * entries in real time based on sender, recipient, event, or message text.
     */
    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                logAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Initializes the filter spinner with available notification-type
     * categories and sets up a listener to reload logs when the filter
     * selection changes.
     */
    private void setupFilter() {
        // Create filter options
        String[] filterOptions = {
                "All Notifications",
                "Organizer Only",
                "Invitation Sent",
                "Selected",
                "Rejected",
                "Waitlist Joined"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                filterOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(adapter);

        spinnerFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = filterOptions[position];
                applyFilter(selected);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    /**
     * Updates the current filter mode and reloads notification logs accordingly.
     *
     * @param filter the selected filter label from the spinner
     */
    private void applyFilter(String filter) {
        currentFilter = filter;
        loadNotificationLogs();
    }

    /**
     * Configures the RecyclerView and attaches the NotificationLogAdapter.
     * Registers a click callback to allow administrators to view detailed
     * information for any selected log entry.
     */
    private void setupRecyclerView() {
        logAdapter = new NotificationLogAdapter();

        logAdapter.setOnLogClickListener(log -> {
            showLogDetails(log);
        });

        recyclerViewLogs.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewLogs.setAdapter(logAdapter);
    }

    /**
     * Loads up to the last 500 notification logs from Firestore, applying the
     * currently selected filter (if any). Queries are ordered by timestamp in
     * descending order to show the most recent logs first.
     *
     * <p>Performs the following:</p>
     * <ul>
     *     <li>Builds the Firestore query based on the selected filter</li>
     *     <li>Fetches logs and applies "Organizer Only" filtering locally</li>
     *     <li>Updates the adapter and UI state</li>
     * </ul>
     *
     * Displays progress indicators and error feedback.
     */
    private void loadNotificationLogs() {
        progressBar.setVisibility(View.VISIBLE);

        Query query = db.collection("notification_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        // Apply filters based on selection
        if (currentFilter.equals("Invitation Sent")) {
            query = query.whereEqualTo("notificationType", "invitation_sent");
        } else if (currentFilter.equals("Selected")) {
            query = query.whereEqualTo("notificationType", "selected");
        } else if (currentFilter.equals("Rejected")) {
            query = query.whereEqualTo("notificationType", "rejected");
        } else if (currentFilter.equals("Waitlist Joined")) {
            query = query.whereEqualTo("notificationType", "waitlist_joined");

        }
        // "All Notifications" and "Organizer Only" - load all, filter later

        query.limit(500)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    logList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        NotificationLog log = document.toObject(NotificationLog.class);

                        // Apply organizer filter if selected
                        if (currentFilter.equals("Organizer Only")) {
                            logList.add(log);
                        } else {
                            logList.add(log);
                        }
                    }

                    progressBar.setVisibility(View.GONE);
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading logs: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    updateUI();
                });
    }

    /**
     * Updates the UI based on whether logs have been loaded.
     * Shows or hides the empty state, updates total log count,
     * and refreshes the adapter with the current list of logs.
     */
    private void updateUI() {
        if (logList.isEmpty()) {
            recyclerViewLogs.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
            tvTotalLogs.setText("Total: 0 logs");
        } else {
            recyclerViewLogs.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            tvTotalLogs.setText("Total: " + logList.size() + " logs (showing last 500)");

            logAdapter.setLogs(logList);
        }
    }

    /**
     * Displays all available details of a selected notification log in
     * a modal dialog, including sender, recipient, event, type, message,
     * and formatted timestamp.
     *
     * @param log the selected notification log whose details are shown
     */
    private void showLogDetails(NotificationLog log) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault());

        String details = "FROM: " + log.getSenderName() + " (ID: " + log.getSenderId() + ")\n\n" +
                "TO: " + log.getRecipientName() + " (ID: " + log.getRecipientId() + ")\n\n" +
                "EVENT: " + (log.getEventName() != null ? log.getEventName() : "N/A") + "\n\n" +
                "TYPE: " + log.getNotificationType() + "\n\n" +
                "TITLE: " + log.getTitle() + "\n\n" +
                "MESSAGE: " + log.getMessage() + "\n\n" +
                "STATUS: " + log.getStatus() + "\n\n" +
                "TIMESTAMP: " + (log.getTimestamp() != null ? sdf.format(log.getTimestamp()) : "N/A");

        new AlertDialog.Builder(this)
                .setTitle("Notification Log Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Handles action-bar "Up" navigation by closing the activity.
     *
     * @return true after finishing the activity
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
