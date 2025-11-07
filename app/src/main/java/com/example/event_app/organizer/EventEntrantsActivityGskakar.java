package com.example.event_app.organizer;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.adapters.UserEntryAdapterGskakar;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EventEntrantsActivityGskakar extends AppCompatActivity {

    private enum Tab { SELECTED, WAITLISTED, CANCELLED }

    // UI
    private TextView tvTitle, tvSection;
    private Button btnSelected, btnWaitlisted, btnCancelled, btnTopAction;
    private RecyclerView recyclerView;
    private EditText etSearch;
    private ImageButton btnBack;

    // Adapter
    private UserEntryAdapterGskakar userEntryAdapter;

    // Firebase Storage
    private FirebaseStorage storage;
    private StorageReference entrantsRef;
    private static final long MAX_BYTES = 1024 * 1024; // 1 MB
    private String eventId;

    // Data (filled from Storage JSON)
    private final List<String> selectedUsers   = new ArrayList<>();
    private final List<String> waitlistedUsers = new ArrayList<>();
    private final List<String> cancelledUsers  = new ArrayList<>();

    // State
    private Tab activeTab = Tab.SELECTED;
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrants_gskakar);

        initializeViews();
        setupRecyclerView();
        setupTabButtons();
        setupSearch();

        // Read event title/id
        String eventTitle = getIntent().getStringExtra("eventTitle");
        if (eventTitle != null) tvTitle.setText(eventTitle);
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) eventId = "demoEvent";

        // Firebase Storage path: events/{eventId}/entrants.json
        storage = FirebaseStorage.getInstance();
        entrantsRef = storage.getReference()
                .child("events")
                .child(eventId)
                .child("entrants.json");

        // Load data from Storage, then render current tab
        loadEntrantsFromStorage();

        // Wire the top action: Draw Replace only when on Cancelled tab
        btnTopAction.setOnClickListener(v -> {
            if (activeTab == Tab.CANCELLED) {
                drawReplacement();
            }
        });

        showSelectedEntrants();
    }

    private void initializeViews() {
        tvTitle      = findViewById(R.id.tvTitle);
        tvSection    = findViewById(R.id.tvSection);
        btnTopAction = findViewById(R.id.btnTopAction);
        btnSelected  = findViewById(R.id.btnSelected);
        btnWaitlisted= findViewById(R.id.btnWaitlisted);
        btnCancelled = findViewById(R.id.btnCancelled);
        recyclerView = findViewById(R.id.rvEntrants);
        etSearch     = findViewById(R.id.etSearch);
        btnBack      = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userEntryAdapter = new UserEntryAdapterGskakar();
        recyclerView.setAdapter(userEntryAdapter);
    }

    private void setupTabButtons() {
        btnSelected.setOnClickListener(v -> showSelectedEntrants());
        btnWaitlisted.setOnClickListener(v -> showWaitlistedEntrants());
        btnCancelled.setOnClickListener(v -> showCancelledEntrants());
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s == null ? "" : s.toString();
                applyFilter();
            }
        });
    }

    /** --- Firebase Storage: load entrants.json into lists --- */
    private void loadEntrantsFromStorage() {
        entrantsRef.getBytes(MAX_BYTES)
                .addOnSuccessListener(bytes -> {
                    try {
                        String json = new String(bytes, StandardCharsets.UTF_8);
                        JSONObject root = new JSONObject(json);

                        selectedUsers.clear();
                        waitlistedUsers.clear();
                        cancelledUsers.clear();

                        readArrayIntoList(root.optJSONArray("selected"),   selectedUsers);
                        readArrayIntoList(root.optJSONArray("waitlisted"), waitlistedUsers);
                        readArrayIntoList(root.optJSONArray("cancelled"),  cancelledUsers);
                    } catch (Exception ignore) {
                        // keep lists as-is if malformed
                    }
                    applyFilter(); // refresh view either way
                })
                .addOnFailureListener(err -> {
                    // If download fails, show empty/current lists
                    applyFilter();
                });
    }

    private void readArrayIntoList(JSONArray arr, List<String> out) {
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            String v = arr.optString(i, null);
            if (v != null && !v.isEmpty()) out.add(v);
        }
    }

    /** Persist current lists back to Storage (entrants.json) */
    private void saveEntrantsToStorage() {
        try {
            JSONObject root = new JSONObject();
            root.put("selected",   new JSONArray(selectedUsers));
            root.put("waitlisted", new JSONArray(waitlistedUsers));
            root.put("cancelled",  new JSONArray(cancelledUsers));
            byte[] data = root.toString().getBytes(StandardCharsets.UTF_8);

            entrantsRef.putBytes(data)
                    .addOnSuccessListener(task -> applyFilter())
                    .addOnFailureListener(err -> {
                        // no-op; keep UI as-is
                    });
        } catch (Exception ignore) {
            // no-op
        }
    }

    /** Tabs */
    private void showSelectedEntrants() {
        activeTab = Tab.SELECTED;
        highlightActiveTab(btnSelected, btnWaitlisted, btnCancelled);
        tvSection.setText("Selected Entrants");
        tvSection.setVisibility(View.VISIBLE);
        btnTopAction.setVisibility(View.VISIBLE);  // keep visible per your current design
        applyFilter();
    }

    private void showWaitlistedEntrants() {
        activeTab = Tab.WAITLISTED;
        highlightActiveTab(btnWaitlisted, btnSelected, btnCancelled);
        tvSection.setText("Waitlisted Entrants");
        tvSection.setVisibility(View.VISIBLE);
        btnTopAction.setVisibility(View.VISIBLE);  // keep visible per your current design
        applyFilter();
    }

    private void showCancelledEntrants() {
        activeTab = Tab.CANCELLED;
        highlightActiveTab(btnCancelled, btnSelected, btnWaitlisted);
        tvSection.setText("Cancelled Entrants");
        tvSection.setVisibility(View.VISIBLE);
        btnTopAction.setVisibility(View.VISIBLE);
        btnTopAction.setText("Draw Replace");
        applyFilter();
    }

    /** Filtering */
    private void applyFilter() {
        List<String> source = getSourceForActiveTab();
        String q = currentQuery.trim().toLowerCase(Locale.ROOT);

        if (q.isEmpty()) {
            userEntryAdapter.updateUserList(source);
            return;
        }
        List<String> filtered = new ArrayList<>();
        for (String name : source) {
            if (name.toLowerCase(Locale.ROOT).contains(q)) filtered.add(name);
        }
        userEntryAdapter.updateUserList(filtered);
    }

    private List<String> getSourceForActiveTab() {
        switch (activeTab) {
            case WAITLISTED: return waitlistedUsers;
            case CANCELLED:  return cancelledUsers;
            case SELECTED:
            default:         return selectedUsers;
        }
    }

    /** Button styles */
    private void highlightActiveTab(Button active, Button other1, Button other2) {
        setTabStyle(active, true);
        setTabStyle(other1, false);
        setTabStyle(other2, false);
    }

    private void setTabStyle(Button button, boolean isActive) {
        if (isActive) {
            button.setBackgroundColor(Color.BLACK);
            button.setTextColor(Color.WHITE);
        } else {
            button.setBackgroundColor(Color.parseColor("#E5E5E5"));
            button.setTextColor(Color.BLACK);
        }
    }

    /** ---------------- Draw Replace ---------------- */
    private void drawReplacement() {
        // Build pool: waitlisted users not already selected/cancelled
        List<String> pool = new ArrayList<>();
        for (String u : waitlistedUsers) {
            if (!selectedUsers.contains(u) && !cancelledUsers.contains(u)) {
                pool.add(u);
            }
        }
        if (pool.isEmpty()) {

            return;
        }

        int idx = (int) (Math.random() * pool.size());
        String chosen = pool.get(idx);


        waitlistedUsers.remove(chosen);
        if (!selectedUsers.contains(chosen)) selectedUsers.add(chosen);


        saveEntrantsToStorage();
    }
}
