package com.example.event_app.activities.organizer;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.adapters.EntrantListAdapter;
import com.example.event_app.models.Event;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ViewEntrantsActivity
 *
 * Displays all entrants for a given event, separated into five logical tabs:
 * <ul>
 *   <li><b>Waiting</b> — Users who joined the event</li>
 *   <li><b>Selected</b> — Users chosen by the lottery</li>
 *   <li><b>Attending</b> — Users who accepted their invitation</li>
 *   <li><b>Declined</b> — Users who declined their invitation</li>
 *   <li><b>Log</b> — Replacement draw history with timestamps and reasons</li>
 * </ul>
 *
 * User Stories:
 * <ul>
 *   <li>US 02.02.01 — View waiting list</li>
 *   <li>US 02.06.01 — View selected entrants</li>
 *   <li>US 02.06.02 — View declined entrants</li>
 *   <li>US 02.06.03 — View final attending list</li>
 *   <li>US 02.06.04 — View replacement draw log</li>
 * </ul>
 *
 *
 */
public class ViewEntrantsActivity extends AppCompatActivity {

    private static final String TAG = "ViewEntrants";

    // UI Elements
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private RecyclerView rvEntrants;
    private TextView tvListCount;
    private LinearLayout emptyView;
    private View loadingView;

    // Data
    private FirebaseFirestore db;
    private String eventId;
    private Event event;
    private EntrantListAdapter adapter;
    private String currentTab = "waiting";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_entrants);
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

        // Initialize views
        initViews();

        // Setup tabs
        setupTabs();

        // Setup RecyclerView
        setupRecyclerView();

        // Load event
        loadEventDetails();
    }

    /**
     * Initializes toolbar, RecyclerView container, tab layout,
     * loading view, and empty-state container.
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        rvEntrants = findViewById(R.id.rvEntrants);
        tvListCount = findViewById(R.id.tvListCount);
        emptyView = findViewById(R.id.emptyView);
        loadingView = findViewById(R.id.loadingView);

        // Toolbar
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * Configures the TabLayout with five tabs.
     */
    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Waiting"));
        tabLayout.addTab(tabLayout.newTab().setText("Selected"));
        tabLayout.addTab(tabLayout.newTab().setText("Attending"));
        tabLayout.addTab(tabLayout.newTab().setText("Declined"));
        tabLayout.addTab(tabLayout.newTab().setText("Log"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentTab = "waiting"; break;
                    case 1: currentTab = "selected"; break;
                    case 2: currentTab = "attending"; break;
                    case 3: currentTab = "declined"; break;
                    case 4: currentTab = "log"; break;
                }
                displayEntrants();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    /**
     * Sets up RecyclerView with adapter.
     */
    private void setupRecyclerView() {
        adapter = new EntrantListAdapter(this, eventId);
        rvEntrants.setLayoutManager(new LinearLayoutManager(this));
        rvEntrants.setAdapter(adapter);
    }

    /**
     * Loads Event from Firestore.
     */
    private void loadEventDetails() {
        showLoading();

        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        event = document.toObject(Event.class);
                        if (event != null) {
                            event.setId(document.getId());
                            displayEntrants();
                        }
                    }
                    hideLoading();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show();
                    hideLoading();
                });
    }

    /**
     * Displays entrants with proper emptyView clearing
     */
    private void displayEntrants() {
        if (event == null) return;

        List<String> userIds = new ArrayList<>();

        switch (currentTab) {
            case "waiting":
                if (event.getWaitingList() != null) {
                    userIds = event.getWaitingList();
                }
                break;
            case "selected":
                if (event.getSelectedList() != null) {
                    userIds = event.getSelectedList();
                }
                break;
            case "attending":
                if (event.getSignedUpUsers() != null) {
                    userIds = event.getSignedUpUsers();
                }
                break;
            case "declined":
                if (event.getDeclinedUsers() != null) {
                    userIds = event.getDeclinedUsers();
                }
                break;
            case "log":
                displayReplacementLog();
                return;  // Exit early for log tab
        }

        emptyView.removeAllViews();

        // Update count
        int count = userIds.size();
        String tabName = getTabDisplayName(currentTab);
        tvListCount.setText(count + (count == 1 ? " entrant" : " entrants") + " in " + tabName);

        // Show/hide views
        if (userIds.isEmpty()) {
            rvEntrants.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);

            // Add empty message
            TextView emptyText = new TextView(this);
            emptyText.setText("No " + tabName.toLowerCase() + " yet");
            emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            emptyText.setPadding(32, 32, 32, 32);
            emptyText.setTextSize(16);
            emptyText.setTextColor(getResources().getColor(android.R.color.darker_gray));
            emptyView.addView(emptyText);
        } else {
            rvEntrants.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            adapter.setUserIds(userIds, currentTab);
        }
    }

    /**
     * Converts tab key to display name.
     */
    private String getTabDisplayName(String tab) {
        switch (tab) {
            case "waiting": return "Waiting List";
            case "selected": return "Selected";
            case "attending": return "Attending";
            case "declined": return "Declined";
            case "log": return "Replacement Log";
            default: return "";
        }
    }

    /**
     * Displays replacement log
     */
    private void displayReplacementLog() {
        if (event == null || event.getReplacementLog() == null || event.getReplacementLog().isEmpty()) {
            // No replacements yet
            rvEntrants.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            tvListCount.setText("No replacements drawn yet");

            // Clear previous content
            emptyView.removeAllViews();

            // Create empty message
            TextView emptyText = new TextView(this);
            emptyText.setText("No replacements have been drawn yet.\n\nWhen someone declines and you draw a replacement, it will show here.");
            emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            emptyText.setPadding(32, 32, 32, 32);
            emptyText.setTextSize(16);
            emptyText.setTextColor(getResources().getColor(android.R.color.darker_gray));
            emptyView.addView(emptyText);
            return;
        }

        // Show log entries
        rvEntrants.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);

        int count = event.getReplacementLog().size();
        tvListCount.setText(count + (count == 1 ? " replacement" : " replacements"));

        // Build log display WITHOUT emojis
        StringBuilder logText = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd 'at' h:mm a", Locale.getDefault());

        // Show newest first
        List<Map<String, Object>> log = new ArrayList<>(event.getReplacementLog());
        Collections.reverse(log);

        int entryNum = count;
        for (Map<String, Object> entry : log) {
            String replacementId = (String) entry.get("replacementUserId");
            Long timestamp = (Long) entry.get("timestamp");
            String reason = (String) entry.get("reason");

            if (replacementId != null && timestamp != null) {
                String dateStr = sdf.format(new Date(timestamp));

                logText.append("━━━━━━━━━━━━━━━━━━━━━━\n");
                logText.append("REPLACEMENT #").append(entryNum).append("\n\n");
                logText.append("USER SELECTED\n");
                logText.append("User ID: ").append(replacementId.substring(0, Math.min(12, replacementId.length()))).append("...\n");
                logText.append("Time: ").append(dateStr).append("\n");
                if (reason != null && !reason.isEmpty()) {
                    logText.append("Reason: ").append(reason).append("\n");
                }
                logText.append("\n");
                entryNum--;
            }
        }

        // Clear previous content
        emptyView.removeAllViews();

        // Display log
        TextView logDisplay = new TextView(this);
        logDisplay.setPadding(32, 32, 32, 32);
        logDisplay.setTextSize(14);
        logDisplay.setTextColor(getResources().getColor(android.R.color.black));
        logDisplay.setLineSpacing(4, 1.0f);  // Better readability
        logDisplay.setText(logText.toString());
        emptyView.addView(logDisplay);
    }

    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        loadingView.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (event != null) {
            loadEventDetails();
        }
    }
}