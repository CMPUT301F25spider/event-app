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
 * HomeFragment - Discovery and Quick Actions
 *
 * Features:
 * - Notification badge showing unread count (like real apps!)
 * - Scan QR code
 * - Browse events
 * - Category filtering
 * - ✨ Favorites section
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

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
     *Real-time notification badge - Updates instantly like Instagram!
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
     * Show badge with count (like Instagram, Facebook, etc.)
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
     * Hide badge when no notifications
     */
    private void hideBadge() {
        if (tvNotificationBadge != null) {
            tvNotificationBadge.setVisibility(View.GONE);
        }
    }

    private void handleQrScan() {
        if (PermissionManager.isCameraPermissionGranted(requireActivity())) {
            launchQrScanner();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchQrScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan an event QR code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        qrCodeLauncher.launch(options);
    }

    /**
     * Real-time updates for happening soon events
     * New events appear instantly without refresh!
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
     * Real-time updates for popular events
     * Events update automatically as waiting lists grow!
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
     * Load user's favorite events with REAL-TIME updates
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
     * Show favorites section
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
     * Hide favorites section
     */
    private void hideFavoritesSection() {
        if (sectionFavorites != null) {
            sectionFavorites.setVisibility(View.GONE);
        }
    }

    private void showEvents(RecyclerView recyclerView, LinearLayout emptyView) {
        recyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    private void showEmptyState(RecyclerView recyclerView, LinearLayout emptyView) {
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }

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