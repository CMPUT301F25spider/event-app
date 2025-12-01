package com.example.event_app.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.adapters.FullEventAdapter;
import com.example.event_app.models.Event;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Fragment for browsing and filtering all available events.
 *
 * Supports:
 * <ul>
 *     <li>Search by name, description, organizer, location, or category</li>
 *     <li>Filtering by built-in and custom categories</li>
 *     <li>Sorting by date, name, or popularity</li>
 *     <li>Real-time Firestore updates for event changes</li>
 * </ul>
 *
 * Implements:
 * <br>US 01.01.03 – Browse available events<br>
 * US 01.01.04 – Filter events by interests<br>
 * US 01.05.04 – Display waiting list count
 */
public class BrowseEventsTabFragment extends Fragment {

    private static final String TAG = "BrowseEventsTab";

    // UI Components
    private EditText searchBox;
    private ImageButton btnClearSearch;
    private ChipGroup chipGroupFilters;
    private Chip chipAll;
    private Chip chipFood, chipSports, chipMusic, chipEducation, chipArt, chipTech, chipHealth, chipBusiness, chipCommunity, chipOther;
    private TextView tvResultsCount, btnSort;
    private RecyclerView rvEvents;
    private ProgressBar progressBar;
    private LinearLayout emptyView, errorView;
    private TextView tvEmptyMessage, tvErrorMessage;
    private MaterialButton btnRetry;

    // Data
    private FullEventAdapter adapter;
    private FirebaseFirestore db;
    private List<Event> allEvents;
    private String currentSearchQuery = "";
    private String currentCategoryFilter = "all";
    private SortOption currentSort = SortOption.DATE_ASC;
    private List<String> customCategories = new ArrayList<>(); // User-added categories
    private List<Chip> customCategoryChips = new ArrayList<>(); // Dynamically created chips

    //Real-time listener for all events
    private com.google.firebase.firestore.ListenerRegistration eventsListener;

    // Sort options
    private enum SortOption {
        DATE_ASC("Date (Soonest First)"),
        DATE_DESC("Date (Latest First)"),
        NAME_ASC("Name (A-Z)"),
        NAME_DESC("Name (Z-A)"),
        POPULAR("Most Popular");

        private final String displayName;

        SortOption(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Inflates the layout for the Browse Events tab.
     *
     * @param inflater LayoutInflater used to inflate the fragment UI
     * @param container parent view the fragment attaches to
     * @param savedInstanceState saved state bundle
     * @return the inflated fragment view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_browse_events_tab, container, false);
    }

    /**
     * Called after the view is created.
     * Initializes Firestore, sets up UI elements, listeners, adapters,
     * loads custom categories, and begins real-time event loading.
     *
     * @param view the fragment's root view
     * @param savedInstanceState saved state bundle
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        allEvents = new ArrayList<>();

        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadCustomCategories();
        loadAllEvents();
    }

    /**
     * Initializes all UI components in the fragment.
     *
     * @param view the root view containing UI references
     */
    private void initViews(View view) {
        searchBox = view.findViewById(R.id.searchBox);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        chipGroupFilters = view.findViewById(R.id.chipGroupFilters);
        chipAll = view.findViewById(R.id.chipAll);
        chipFood = view.findViewById(R.id.chipFood);
        chipSports = view.findViewById(R.id.chipSports);
        chipMusic = view.findViewById(R.id.chipMusic);
        chipEducation = view.findViewById(R.id.chipEducation);
        chipArt = view.findViewById(R.id.chipArt);
        chipTech = view.findViewById(R.id.chipTech);
        chipHealth = view.findViewById(R.id.chipHealth);
        chipBusiness = view.findViewById(R.id.chipBusiness);
        chipCommunity = view.findViewById(R.id.chipCommunity);
        chipOther = view.findViewById(R.id.chipOther);
        tvResultsCount = view.findViewById(R.id.tvResultsCount);
        btnSort = view.findViewById(R.id.btnSort);
        rvEvents = view.findViewById(R.id.rvEvents);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        errorView = view.findViewById(R.id.errorView);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage);
        btnRetry = view.findViewById(R.id.btnRetry);
    }

    /**
     * Configures the RecyclerView and attaches the FullEventAdapter.
     */
    private void setupRecyclerView() {
        adapter = new FullEventAdapter(requireContext());
        rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEvents.setAdapter(adapter);
    }

    /**
     * Sets up listeners for:
     * <ul>
     *     <li>Search box updates</li>
     *     <li>Category chip filters</li>
     *     <li>Sort button</li>
     *     <li>Error retry button</li>
     * </ul>
     */
    private void setupListeners() {
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                applyFiltersAndSort();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            searchBox.setText("");
            currentSearchQuery = "";
        });

        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                chipAll.setChecked(true);
                return;
            }

            int checkedId = checkedIds.get(0);

            if (checkedId == R.id.chipAll) {
                currentCategoryFilter = "all";
            } else if (checkedId == R.id.chipFood) {
                currentCategoryFilter = "Food & Dining";
            } else if (checkedId == R.id.chipSports) {
                currentCategoryFilter = "Sports & Fitness";
            } else if (checkedId == R.id.chipMusic) {
                currentCategoryFilter = "Music & Entertainment";
            } else if (checkedId == R.id.chipEducation) {
                currentCategoryFilter = "Education & Learning";
            } else if (checkedId == R.id.chipArt) {
                currentCategoryFilter = "Art & Culture";
            } else if (checkedId == R.id.chipTech) {
                currentCategoryFilter = "Technology";
            } else if (checkedId == R.id.chipHealth) {
                currentCategoryFilter = "Health & Wellness";
            } else if (checkedId == R.id.chipBusiness) {
                currentCategoryFilter = "Business & Networking";
            } else if (checkedId == R.id.chipCommunity) {
                currentCategoryFilter = "Community & Social";
            } else if (checkedId == R.id.chipOther) {
                currentCategoryFilter = "Other";
            } else {
                // Check if it's a custom category chip
                for (Chip customChip : customCategoryChips) {
                    if (checkedId == customChip.getId()) {
                        currentCategoryFilter = customChip.getText().toString();
                        break;
                    }
                }
            }

            applyFiltersAndSort();
        });

        btnSort.setOnClickListener(v -> showSortDialog());
        btnRetry.setOnClickListener(v -> loadAllEvents());
    }

    /**
     * Loads additional user-defined categories from Firestore
     * and triggers dynamic chip creation.
     */
    private void loadCustomCategories() {
        db.collection("categories")
                .orderBy("name")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    customCategories.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String categoryName = doc.getString("name");
                        if (categoryName != null && !categoryName.trim().isEmpty()) {
                            customCategories.add(categoryName.trim());
                        }
                    }
                    Log.d(TAG, "Loaded " + customCategories.size() + " custom categories");
                    addCustomCategoryChips();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading custom categories", e);
                    // Continue with empty list if loading fails
                });
    }

    /**
     * Dynamically creates filter Chips for all custom categories,
     * removes previous custom chips, and hides the default "Other" chip.
     */
    private void addCustomCategoryChips() {
        // Remove existing custom category chips
        for (Chip chip : customCategoryChips) {
            chipGroupFilters.removeView(chip);
        }
        customCategoryChips.clear();

        // Hide the "Other" chip since we're replacing it with custom categories
        if (chipOther != null) {
            chipOther.setVisibility(View.GONE);
        }

        // Add new chips for custom categories
        for (String category : customCategories) {
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setText(category);
            chip.setCheckable(true);
            chip.setClickable(true);

            // Add click listener
            chip.setOnClickListener(v -> {
                currentCategoryFilter = category;
                applyFiltersAndSort();
            });

            chipGroupFilters.addView(chip);
            customCategoryChips.add(chip);
        }
    }

    /**
     * Loads all active events using a Firestore real-time listener.
     * Automatically updates when events are created, edited, or deleted.
     *
     * Clears old listeners to prevent memory leaks.
     */
    private void loadAllEvents() {
        showLoading();
        if (eventsListener != null) {
            eventsListener.remove();
        }

        //Real-time listener - Updates automatically when events are created/modified!
        eventsListener = db.collection("events")
                .whereEqualTo("status", "active")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to events", error);
                        showError("Failed to load events. Please try again.");
                        return;
                    }

                    if (queryDocumentSnapshots == null) {
                        allEvents.clear();
                        applyFiltersAndSort();
                        return;
                    }

                    allEvents.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = doc.toObject(Event.class);
                        event.setId(doc.getId());
                        allEvents.add(event);
                    }

                    Log.d(TAG, "⚡ Real-time update: " + allEvents.size() + " events");
                    applyFiltersAndSort();
                });
    }

    /**
     * Applies all active filters and sorting rules to the events list.
     * Steps:
     * <ol>
     *     <li>Search filtering</li>
     *     <li>Category filtering</li>
     *     <li>Sorting (date, name, popularity)</li>
     * </ol>
     * Updates the RecyclerView and empty/error states accordingly.
     */
    private void applyFiltersAndSort() {
        List<Event> filtered = new ArrayList<>(allEvents);

        // Search filter
        if (!currentSearchQuery.isEmpty()) {
            filtered = filterBySearch(filtered, currentSearchQuery);
        }

        // Category filter
        if (!currentCategoryFilter.equals("all")) {
            filtered = filterByCategory(filtered, currentCategoryFilter);
        }

        // Sort
        filtered = sortEvents(filtered, currentSort);

        // Update UI
        updateResultsCount(filtered.size());

        if (filtered.isEmpty()) {
            showEmpty(getEmptyMessage());
        } else {
            showEvents();
            adapter.setEvents(filtered);
        }
    }

    /**
     * Filters events by a search query that matches:
     * name, description, organizer, location, or category.
     *
     * @param events the list to filter
     * @param query the user's search text
     * @return filtered list matching the query
     */
    private List<Event> filterBySearch(List<Event> events, String query) {
        List<Event> result = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (Event event : events) {
            String name = event.getName() != null ? event.getName().toLowerCase() : "";
            String desc = event.getDescription() != null ? event.getDescription().toLowerCase() : "";
            String organizer = event.getOrganizerName() != null ? event.getOrganizerName().toLowerCase() : "";
            String location = event.getLocation() != null ? event.getLocation().toLowerCase() : "";
            String category = event.getCategory() != null ? event.getCategory().toLowerCase() : "";

            if (name.contains(lowerQuery) || desc.contains(lowerQuery) ||
                    organizer.contains(lowerQuery) || location.contains(lowerQuery) ||
                    category.contains(lowerQuery)) {
                result.add(event);
            }
        }

        return result;
    }

    /**
     * Filters events to those occurring within a given time window:
     * "week", "month", or no filtering.
     *
     * @param events list of events
     * @param timeFilter type of filter ("week", "month", or none)
     * @return filtered list of events
     */
    private List<Event> filterByTime(List<Event> events, String timeFilter) {
        List<Event> result = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        Date endDate;

        if (timeFilter.equals("week")) {
            calendar.add(Calendar.DAY_OF_YEAR, 7);
            endDate = calendar.getTime();
        } else if (timeFilter.equals("month")) {
            calendar.add(Calendar.MONTH, 1);
            endDate = calendar.getTime();
        } else {
            return events;
        }

        for (Event event : events) {
            Date eventDate = event.getEventDate() != null ? event.getEventDate() : event.getDate();
            if (eventDate != null && eventDate.after(now) && eventDate.before(endDate)) {
                result.add(event);
            }
        }

        return result;
    }

    /**
     * Filters events by exact category match.
     *
     * @param events list to filter
     * @param category selected category name
     * @return events belonging to the category
     */
    private List<Event> filterByCategory(List<Event> events, String category) {
        List<Event> result = new ArrayList<>();

        for (Event event : events) {
            String eventCategory = event.getCategory();

            // Match the category exactly
            if (eventCategory != null && eventCategory.equals(category)) {
                result.add(event);
            }
        }

        return result;
    }

    /**
     * Sorts a list of events based on the selected SortOption.
     *
     * Supported options:
     * <ul>
     *     <li>DATE_ASC – earliest first</li>
     *     <li>DATE_DESC – latest first</li>
     *     <li>NAME_ASC – alphabetical</li>
     *     <li>NAME_DESC – reverse alphabetical</li>
     *     <li>POPULAR – most users waiting</li>
     * </ul>
     *
     * @param events list to sort
     * @param sortOption user-selected sort preference
     * @return sorted list
     */
    private List<Event> sortEvents(List<Event> events, SortOption sortOption) {
        List<Event> sorted = new ArrayList<>(events);

        switch (sortOption) {
            case DATE_ASC:
                Collections.sort(sorted, (e1, e2) -> {
                    Date d1 = e1.getEventDate() != null ? e1.getEventDate() : e1.getDate();
                    Date d2 = e2.getEventDate() != null ? e2.getEventDate() : e2.getDate();
                    if (d1 == null) return 1;
                    if (d2 == null) return -1;
                    return d1.compareTo(d2);
                });
                break;

            case DATE_DESC:
                Collections.sort(sorted, (e1, e2) -> {
                    Date d1 = e1.getEventDate() != null ? e1.getEventDate() : e1.getDate();
                    Date d2 = e2.getEventDate() != null ? e2.getEventDate() : e2.getDate();
                    if (d1 == null) return 1;
                    if (d2 == null) return -1;
                    return d2.compareTo(d1);
                });
                break;

            case NAME_ASC:
                Collections.sort(sorted, (e1, e2) -> {
                    String n1 = e1.getName() != null ? e1.getName() : "";
                    String n2 = e2.getName() != null ? e2.getName() : "";
                    return n1.compareToIgnoreCase(n2);
                });
                break;

            case NAME_DESC:
                Collections.sort(sorted, (e1, e2) -> {
                    String n1 = e1.getName() != null ? e1.getName() : "";
                    String n2 = e2.getName() != null ? e2.getName() : "";
                    return n2.compareToIgnoreCase(n1);
                });
                break;

            case POPULAR:
                Collections.sort(sorted, (e1, e2) -> {
                    int size1 = e1.getWaitingList() != null ? e1.getWaitingList().size() : 0;
                    int size2 = e2.getWaitingList() != null ? e2.getWaitingList().size() : 0;
                    return Integer.compare(size2, size1);
                });
                break;
        }

        return sorted;
    }

    /**
     * Displays a dialog allowing the user to choose a sort method.
     * Updates sort state and re-applies all filters.
     */
    private void showSortDialog() {
        String[] options = {
                SortOption.DATE_ASC.getDisplayName(),
                SortOption.DATE_DESC.getDisplayName(),
                SortOption.NAME_ASC.getDisplayName(),
                SortOption.NAME_DESC.getDisplayName(),
                SortOption.POPULAR.getDisplayName()
        };

        int currentIndex = currentSort.ordinal();

        new AlertDialog.Builder(requireContext())
                .setTitle("Sort Events")
                .setSingleChoiceItems(options, currentIndex, (dialog, which) -> {
                    currentSort = SortOption.values()[which];
                    btnSort.setText("Sort: " + getSortShortName(currentSort) + " â–¼");
                    applyFiltersAndSort();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Provides a short label for the sorting option for use on UI buttons.
     *
     * @param option the selected sort option
     * @return a short display name
     */
    private String getSortShortName(SortOption option) {
        switch (option) {
            case DATE_ASC: return "Date";
            case DATE_DESC: return "Latest";
            case NAME_ASC: return "Name";
            case NAME_DESC: return "Name (Z-A)";
            case POPULAR: return "Popular";
            default: return "Date";
        }
    }

    /**
     * Updates the text showing how many events match the filters.
     *
     * @param count number of events displayed
     */
    private void updateResultsCount(int count) {
        tvResultsCount.setText(count + (count == 1 ? " event" : " events"));
    }

    /**
     * Produces a context-aware empty state message depending on:
     * search query, selected category, or no events at all.
     *
     * @return user-friendly empty state message
     */
    private String getEmptyMessage() {
        if (!currentSearchQuery.isEmpty()) {
            return "No events match \"" + currentSearchQuery + "\"";
        }
        if (!currentCategoryFilter.equals("all")) {
            return "No " + currentCategoryFilter + " events found";
        }
        return "No events available yet";
    }

    /**
     * Shows loading spinner and hides all other UI sections.
     */
    private void showLoading() {
        rvEvents.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
    }

    /**
     * Displays the RecyclerView of events and hides placeholders.
     */
    private void showEvents() {
        rvEvents.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
    }

    /**
     * Shows an empty-state message when filtering yields no results.
     *
     * @param message message to display
     */
    private void showEmpty(String message) {
        rvEvents.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
        tvEmptyMessage.setText(message);
    }

    /**
     * Displays an error state when event loading fails.
     *
     * @param message error explanation
     */
    private void showError(String message) {
        rvEvents.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
        tvErrorMessage.setText(message);
    }

    /**
     * Cleans up Firestore listeners to prevent memory leaks
     * when the fragment view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Clean up real-time listener to prevent memory leaks
        if (eventsListener != null) {
            eventsListener.remove();
            eventsListener = null;
        }
    }
}