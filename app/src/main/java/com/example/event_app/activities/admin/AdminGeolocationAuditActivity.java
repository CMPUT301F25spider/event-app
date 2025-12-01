package com.example.event_app.activities.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.adapters.GeolocationAuditAdapter;
import com.example.event_app.models.GeolocationAudit;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Displays and manages the administrator audit interface for reviewing
 * geolocation access logs. Records include user identity, event details,
 * captured coordinates, and timestamps. This screen supports searching,
 * browsing, and inspecting individual audit entries for privacy compliance.
 *
 * <p>Shows the most recent 500 audit records, sorted by timestamp.
 * Administrators may also open a specific audit location in Google Maps.</p>
 */

public class AdminGeolocationAuditActivity extends AppCompatActivity {
    private EditText etSearch;
    private RecyclerView recyclerViewAudits;
    private LinearLayout emptyStateLayout;
    private ProgressBar progressBar;
    private TextView tvTotalRecords;
    private GeolocationAuditAdapter auditAdapter;
    private FirebaseFirestore db;
    private List<GeolocationAudit> auditList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_geolocation_audit);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);
        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Geolocation Audit");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        // Initialize list
        auditList = new ArrayList<>();
        // Initialize views
        initViews();
        // Set up search
        setupSearch();
        // Set up RecyclerView
        setupRecyclerView();

        // Load audit records
        loadAuditRecords();
    }

    /**
     * Initializes all UI components, including the search field, RecyclerView,
     * empty-state container, progress bar, and total-records counter.
     */
    private void initViews() {
        etSearch = findViewById(R.id.etSearchAudits);
        recyclerViewAudits = findViewById(R.id.recyclerViewAudits);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        progressBar = findViewById(R.id.progressBar);
        tvTotalRecords = findViewById(R.id.tvTotalRecords);
    }

    /**
     * Configures the search bar to perform live filtering. As the administrator
     * types, the list in {@link GeolocationAuditAdapter} is filtered based on
     * username, event name, or action fields.
     */
    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                auditAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Sets up the RecyclerView used to display geolocation audit entries.
     * Initializes the adapter and attaches a click listener that opens a
     * detailed dialog for the selected audit record.
     */
    private void setupRecyclerView() {
        auditAdapter = new GeolocationAuditAdapter();

        auditAdapter.setOnAuditClickListener(audit -> {
            showAuditDetails(audit);
        });

        recyclerViewAudits.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAudits.setAdapter(auditAdapter);
    }

    /**
     * Fetches up to the most recent 500 geolocation audit records from Firestore,
     * ordered by timestamp in descending order. Loaded records are stored locally
     * and rendered through {@link #updateUI()}.
     *
     * Displays an error message if the fetch request fails.
     */
    private void loadAuditRecords() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("geolocation_audits")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(500)  // Limit to last 500 records for performance
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    auditList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        GeolocationAudit audit = document.toObject(GeolocationAudit.class);
                        auditList.add(audit);
                    }
                    progressBar.setVisibility(View.GONE);
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading records: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    updateUI();
                });
    }

    /**
     * Updates the visibility of UI components based on whether audit records
     * are available. If records exist, the RecyclerView is displayed and the
     * total-records count is updated. Otherwise, an empty-state message is shown.
     */
    private void updateUI() {
        if (auditList.isEmpty()) {
            recyclerViewAudits.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
            tvTotalRecords.setText("Total: 0 records");
        } else {
            recyclerViewAudits.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            tvTotalRecords.setText("Total: " + auditList.size() + " records (showing last 500)");

            auditAdapter.setAudits(auditList);
        }
    }

    /**
     * Displays a dialog containing full details for the selected audit entry,
     * including user identity, event information, location coordinates, and a
     * formatted timestamp. Also includes an option to open the coordinates
     * in Google Maps.
     *
     * @param audit the geolocation audit record selected by the administrator
     */
    private void showAuditDetails(GeolocationAudit audit) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault());

        String details = "User: " + audit.getUserName() + "\n" +
                "User ID: " + audit.getUserId() + "\n\n" +
                "Event: " + audit.getEventName() + "\n" +
                "Event ID: " + audit.getEventId() + "\n\n" +
                "Action: " + audit.getAction() + "\n" +
                "Location: " + audit.getLocationString() + "\n" +
                "Timestamp: " + (audit.getTimestamp() != null ? sdf.format(audit.getTimestamp()) : "N/A");

        new AlertDialog.Builder(this)
                .setTitle("Geolocation Audit Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .setNeutralButton("View on Map", (dialog, which) -> {
                    openLocationOnMap(audit);
                })
                .show();
    }

    /**
     * Opens the latitude/longitude of the given audit record in Google Maps.
     * If Google Maps is not installed, falls back to a browser-based map view.
     *
     * @param audit the audit record whose location should be displayed on a map
     */
    private void openLocationOnMap(GeolocationAudit audit) {
        double latitude = audit.getLatitude();
        double longitude = audit.getLongitude();

        if (latitude == 0 && longitude == 0) {
            Toast.makeText(this, "No location data available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create map intent with marker
        String label = audit.getEventName() + " - " + audit.getUserName();
        String uriString = String.format(Locale.ENGLISH,
                "geo:%f,%f?q=%f,%f(%s)",
                latitude, longitude, latitude, longitude,
                android.net.Uri.encode(label));

        android.content.Intent mapIntent = new android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(uriString));

        mapIntent.setPackage("com.google.android.apps.maps");

        // Check if Google Maps is installed
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Fallback to browser if Maps not installed
            String browserUri = String.format(Locale.ENGLISH,
                    "https://www.google.com/maps/search/?api=1&query=%f,%f",
                    latitude, longitude);

            android.content.Intent browserIntent = new android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(browserUri));

            startActivity(browserIntent);
        }
    }

    /**
     * Handles ActionBar navigation by closing the activity.
     *
     * @return always returns true to indicate the event was consumed
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}