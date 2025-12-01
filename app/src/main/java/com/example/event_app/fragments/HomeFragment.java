package com.example.event_app.fragments;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.activities.entrant.BrowseEventsActivity;
import com.example.event_app.activities.entrant.EventDetailsActivity;
import com.example.event_app.activities.entrant.MyEventsActivity;
import com.example.event_app.activities.entrant.NotificationsActivity;
import com.example.event_app.activities.organizer.CreateEventActivity;
import com.example.event_app.adapters.HorizontalEventAdapter;
import com.example.event_app.models.Event;
import com.example.event_app.services.NotificationService;
import com.example.event_app.utils.Navigator;
import com.example.event_app.utils.PermissionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


/**
 * HomeFragment - Main discovery and quick-actions dashboard.
 *
 * Features:
 * <ul>
 *     <li>Real-time notification badge</li>
 *     <li>QR scanner for event check-ins</li>
 *     <li>Happening Soon, Popular, and Favorites event sections</li>
 *     <li>Real-time Firestore listeners for all event rows</li>
 *     <li>Shortcuts: My Events, Create Event, Browse Events</li>
 * </ul>
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    // UI Components
    private MaterialCardView cardScanQr;
    private ImageButton btnSearch, btnNotifications;
    private TextView tvNotificationBadge; // Badge showing count
    private RecyclerView rvHappeningSoon, rvPopular;
    private LinearLayout emptyHappeningSoon, emptyPopular;
    private MaterialButton btnMyEvents, btnCreateEvent;
    private ProgressBar progressBar;
    private RecyclerView rvFavorites;
    private LinearLayout emptyFavorites, sectionFavorites;
    private HorizontalEventAdapter favoritesAdapter;


    // Adapters
    private HorizontalEventAdapter happeningSoonAdapter;
    private HorizontalEventAdapter popularAdapter;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Notification service
    private NotificationService notificationService;

    // Real-time listeners
    private ListenerRegistration badgeListener;
    private ListenerRegistration favoritesListener;
    private ListenerRegistration happeningSoonListener;
    private ListenerRegistration popularListener;

    // Permission launcher for camera
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchQrScanner();
                } else {
                    Toast.makeText(requireContext(), "Camera permission required to scan QR codes",
                            Toast.LENGTH_SHORT).show();
                }
            });

    // QR scanner launcher
    private final ActivityResultLauncher<ScanOptions> qrCodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    String eventId = result.getContents();
                    Intent intent = new Intent(requireContext(), EventDetailsActivity.class);
                    intent.putExtra(Navigator.EXTRA_EVENT_ID, eventId);
                    startActivity(intent);
                }
            });

    /**
     * Inflates the layout for the HomeFragment.
     *
     * @param inflater the LayoutInflater used to inflate the view
     * @param container optional parent view
     * @param savedInstanceState previously saved state
     * @return the inflated fragment view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    /**
     * Initializes Firebase, views, adapters, listeners,
     * and triggers initial event + notification loading.
     *
     * @param view root fragment view
     * @param savedInstanceState previously saved state
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize notification service
        notificationService = new NotificationService();

        // Initialize views
        initViews(view);

        // Setup RecyclerViews
        setupRecyclerViews();

        // Setup listeners
        setupListeners();

        // Load events
        loadHappeningSoonEvents();
        loadPopularEvents();
        loadFavoriteEvents();  //Load favorites

        // Update notification badge
        updateNotificationBadge();
    }

    /**
     * Finds and initializes all UI components belonging to the Home screen.
     *
     * @param view the root fragment view containing UI elements
     */
    private void initViews(View view) {
        cardScanQr = view.findViewById(R.id.cardScanQr);
        btnSearch = view.findViewById(R.id.btnSearch);
        btnNotifications = view.findViewById(R.id.btnNotifications);
        tvNotificationBadge = view.findViewById(R.id.tvNotificationBadge); //Badge

        rvHappeningSoon = view.findViewById(R.id.rvHappeningSoon);
        rvPopular = view.findViewById(R.id.rvPopular);
        emptyHappeningSoon = view.findViewById(R.id.emptyHappeningSoon);
        emptyPopular = view.findViewById(R.id.emptyPopular);
        btnMyEvents = view.findViewById(R.id.btnMyEvents);
        btnCreateEvent = view.findViewById(R.id.btnCreateEvent);
        progressBar = view.findViewById(R.id.progressBar);
        rvFavorites = view.findViewById(R.id.rvFavorites);
        emptyFavorites = view.findViewById(R.id.emptyFavorites);
        sectionFavorites = view.findViewById(R.id.sectionFavorites);
    }

    /**
     * Sets up three horizontal RecyclerViews:
     * <ul>
     *     <li>Happening Soon</li>
     *     <li>Popular Events</li>
     *     <li>Favorites</li>
     * </ul>
     * Each list uses HorizontalEventAdapter.
     */
    private void setupRecyclerViews() {
        happeningSoonAdapter = new HorizontalEventAdapter(requireContext());
        LinearLayoutManager layoutManager1 = new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false);
        rvHappeningSoon.setLayoutManager(layoutManager1);
        rvHappeningSoon.setAdapter(happeningSoonAdapter);

        popularAdapter = new HorizontalEventAdapter(requireContext());
        LinearLayoutManager layoutManager2 = new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false);
        rvPopular.setLayoutManager(layoutManager2);
        rvPopular.setAdapter(popularAdapter);

        //Favorites adapter
        favoritesAdapter = new HorizontalEventAdapter(requireContext());
        LinearLayoutManager layoutManager3 = new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false);
        rvFavorites.setLayoutManager(layoutManager3);
        rvFavorites.setAdapter(favoritesAdapter);
    }

    /**
     * Attaches click listeners for:
     * <ul>
     *     <li>QR scanner</li>
     *     <li>Search</li>
     *     <li>Notifications</li>
     *     <li>My Events</li>
     *     <li>Create Event</li>
     * </ul>
     */
    private void setupListeners() {
        cardScanQr.setOnClickListener(v -> handleQrScan());

        btnSearch.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), BrowseEventsActivity.class);
            startActivity(intent);
        });

        // Notifications button - Opens notification center
        btnNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), NotificationsActivity.class);
            startActivity(intent);
        });


        btnMyEvents.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MyEventsActivity.class);
            startActivity(intent);
        });

        btnCreateEvent.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreateEventActivity.class);
            startActivity(intent);
        });

    }

    /**
     * Adds a real-time Firestore listener that watches for unread
     * notifications for this user. The badge updates instantly,
     * similar to Instagram/WhatsApp.
     */
    private void updateNotificationBadge() {
        if (mAuth.getCurrentUser() == null) {
            hideBadge();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        // Remove old listener if exists
        if (badgeListener != null) {
            badgeListener.remove();
        }

        // Set up real-time listener
        badgeListener = db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        hideBadge();
                        return;
                    }

                    if (snapshots == null) {
                        hideBadge();
                        return;
                    }

                    int unreadCount = snapshots.size();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (unreadCount > 0) {
                                showBadge(unreadCount);
                            } else {
                                hideBadge();
                            }
                        });
                    }
                });
    }

    /**
     * Displays the unread notification badge.
     * If count > 9, shows "9+".
     *
     * @param count number of unread notifications
     */
    private void showBadge(int count) {
        if (tvNotificationBadge != null) {
            // Show count (9+ if more than 9)
            String displayText = count > 9 ? "9+" : String.valueOf(count);
            tvNotificationBadge.setText(displayText);
            tvNotificationBadge.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hides the notification badge UI element.
     */
    private void hideBadge() {
        if (tvNotificationBadge != null) {
            tvNotificationBadge.setVisibility(View.GONE);
        }
    }

    /**
     * Handles QR scan flow:
     * <ul>
     *     <li>Checks camera permission</li>
     *     <li>Requests permission if needed</li>
     *     <li>Launches the scanner when granted</li>
     * </ul>
     */
    private void handleQrScan() {
        if (PermissionManager.isCameraPermissionGranted(requireActivity())) {
            launchQrScanner();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * Launches the JourneyApps QR scanner with customized options.
     * When scanning succeeds, navigates directly to EventDetailsActivity.
     */
    private void launchQrScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan an event QR code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        qrCodeLauncher.launch(options);
    }

    /**
     * Loads events occurring within the next 7 days.
     * Uses a **real-time Firestore listener**, meaning:
     * <ul>
     *     <li>New events appear instantly</li>
     *     <li>Date changes reflect immediately</li>
     * </ul>
     *
     * Updates the "Happening Soon" RecyclerView.
     */
    private void loadHappeningSoonEvents() {
        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, 7);
        Date weekFromNow = calendar.getTime();

        if (happeningSoonListener != null) {
            happeningSoonListener.remove();
        }

        // Real-time listener - Updates automatically!
        happeningSoonListener = db.collection("events")
                .whereEqualTo("status", "active")
                .whereGreaterThanOrEqualTo("eventDate", today)
                .whereLessThanOrEqualTo("eventDate", weekFromNow)
                .orderBy("eventDate", Query.Direction.ASCENDING)
                .limit(10)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        showEmptyState(rvHappeningSoon, emptyHappeningSoon);
                        return;
                    }

                    if (queryDocumentSnapshots == null) {
                        showEmptyState(rvHappeningSoon, emptyHappeningSoon);
                        return;
                    }

                    List<Event> events = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = doc.toObject(Event.class);
                        event.setId(doc.getId());
                        events.add(event);
                    }

                    if (events.isEmpty()) {
                        showEmptyState(rvHappeningSoon, emptyHappeningSoon);
                    } else {
                        showEvents(rvHappeningSoon, emptyHappeningSoon);
                        happeningSoonAdapter.setEvents(events);
                    }
                });
    }

    /**
     * Loads the latest events, sorted dynamically by waiting list size.
     * Real-time listener ensures:
     * <ul>
     *     <li>Popularity updates instantly when lists grow</li>
     *     <li>Users see changing trends without refreshing</li>
     * </ul>
     */
    private void loadPopularEvents() {

        if (popularListener != null) {
            popularListener.remove();
        }

        // Real-time listener - Updates automatically!
        popularListener = db.collection("events")
                .whereEqualTo("status", "active")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        showEmptyState(rvPopular, emptyPopular);
                        return;
                    }

                    if (queryDocumentSnapshots == null) {
                        showEmptyState(rvPopular, emptyPopular);
                        return;
                    }

                    List<Event> events = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = doc.toObject(Event.class);
                        event.setId(doc.getId());
                        events.add(event);
                    }

                    // Sort by waiting list size (most popular first)
                    events.sort((e1, e2) -> {
                        int size1 = e1.getWaitingList() != null ? e1.getWaitingList().size() : 0;
                        int size2 = e2.getWaitingList() != null ? e2.getWaitingList().size() : 0;
                        return Integer.compare(size2, size1);
                    });


                    if (events.isEmpty()) {
                        showEmptyState(rvPopular, emptyPopular);
                    } else {
                        showEvents(rvPopular, emptyPopular);
                        popularAdapter.setEvents(events);
                    }
                });
    }

    /**
     * Loads the user's favorite events using:
     * <ul>
     *     <li>A real-time listener on the user's document</li>
     *     <li>A Firestore whereIn query to fetch event details</li>
     * </ul>
     *
     * If the user has no favorites, hides the entire Favorites section.
     */
    private void loadFavoriteEvents() {
        if (mAuth.getCurrentUser() == null) {
            hideFavoritesSection();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        // Remove old listener if exists
        if (favoritesListener != null) {
            favoritesListener.remove();
        }

        //  Set up real-time listener (like Instagram!)
        favoritesListener = db.collection("users").document(userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        hideFavoritesSection();
                        return;
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        Log.d(TAG, "User document doesn't exist");
                        hideFavoritesSection();
                        return;
                    }

                    List<String> favoriteIds = (List<String>) snapshot.get("favoriteEvents");

                    if (favoriteIds == null || favoriteIds.isEmpty()) {
                        hideFavoritesSection();
                        return;
                    }


                    // Firestore whereIn has a limit of 10 items
                    if (favoriteIds.size() > 10) {
                        favoriteIds = favoriteIds.subList(0, 10);
                    }

                    // Load favorite events
                    db.collection("events")
                            .whereIn("__name__", favoriteIds)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                List<Event> events = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                    Event event = doc.toObject(Event.class);
                                    event.setId(doc.getId());
                                    events.add(event);
                                }


                                if (events.isEmpty()) {
                                    hideFavoritesSection();
                                } else {
                                    showFavoritesSection();
                                    favoritesAdapter.setEvents(events);
                                }
                            })
                            .addOnFailureListener(e -> {
                                hideFavoritesSection();
                            });
                });
    }

    /**
     * Displays the Favorites section and associated RecyclerView.
     */
    private void showFavoritesSection() {
        if (sectionFavorites != null) {
            sectionFavorites.setVisibility(View.VISIBLE);
        }
        if (rvFavorites != null) {
            rvFavorites.setVisibility(View.VISIBLE);
        }
        if (emptyFavorites != null) {
            emptyFavorites.setVisibility(View.GONE);
        }
    }

    /**
     * Hides the Favorites section, used when there are no favorites or the user is logged out.
     */
    private void hideFavoritesSection() {
        if (sectionFavorites != null) {
            sectionFavorites.setVisibility(View.GONE);
        }
    }

    /**
     * Shows a RecyclerView and hides its empty-state placeholder layout.
     *
     * @param recyclerView list to show
     * @param emptyView empty-state view to hide
     */
    private void showEvents(RecyclerView recyclerView, LinearLayout emptyView) {
        recyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    /**
     * Hides a RecyclerView and shows its empty-state placeholder layout.
     *
     * @param recyclerView list to hide
     * @param emptyView empty-state view to show
     */
    private void showEmptyState(RecyclerView recyclerView, LinearLayout emptyView) {
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }

    /**
     * Cleans up all Firestore snapshot listeners to prevent:
     * <ul>
     *     <li>memory leaks</li>
     *     <li>duplicate listeners</li>
     *     <li>UI updates after fragment destruction</li>
     * </ul>
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Clean up ALL real-time listeners to prevent memory leaks
        if (badgeListener != null) {
            badgeListener.remove();
            badgeListener = null;
        }

        if (favoritesListener != null) {
            favoritesListener.remove();
            favoritesListener = null;
        }

        if (happeningSoonListener != null) {
            happeningSoonListener.remove();
            happeningSoonListener = null;
        }

        if (popularListener != null) {
            popularListener.remove();
            popularListener = null;
        }

        Log.d(TAG, " All real-time listeners cleaned up");
    }
}