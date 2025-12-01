package com.example.event_app.activities.organizer;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.event_app.R;
import com.example.event_app.models.Event;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ViewEntrantMapActivity
 *
 * Displays a Google Map showing the geographic locations from which entrants
 * joined an event's waiting list.
 *
 * User Story:
 * - US 02.02.02 — Organizer views entrant join locations on a map.
 *
 * Features:
 * <ul>
 *     <li>Loads entrant geolocation data from Firestore</li>
 *     <li>Displays markers and visibility circles for each location</li>
 *     <li>Auto-adjusts camera to fit all entrant markers</li>
 *     <li>Handles single and multiple marker cases</li>
 *     <li>Validates coordinates and ignores malformed data</li>
 *     <li>Gracefully handles geolocation-disabled events</li>
 * </ul>
 *
 * Architecture:
 * - Fetches Event document from Firestore
 * - Extracts entrantLocations: { "userId": { "latitude": X, "longitude": Y } }
 * - Converts them to LatLng objects
 * - Renders markers on Google Maps API
 */
public class ViewEntrantMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "ViewEntrantMap";
    private static final float DEFAULT_ZOOM = 10f;
    private static final float SINGLE_LOCATION_ZOOM = 12f;
    private static final int MARKER_RADIUS_METERS = 500;
    private static final int MAP_PADDING_PX = 120;

    // UI Elements
    private ImageButton btnBack;
    private GoogleMap mMap;

    // Data
    private FirebaseFirestore db;
    private String eventId;
    private Event event;
    private List<LatLng> entrantLocations = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_entrant_map);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Get event ID from intent
        eventId = getIntent().getStringExtra("EVENT_ID");
        if (eventId == null) {
            Toast.makeText(this, "Error: No event ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize UI
        initializeUI();

        // Initialize map
        initializeMap();

        // Load event data
        loadEventData();
    }

    /**
     * Initializes basic UI components including the back button
     * and assigns click listeners.
     */
    private void initializeUI() {
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    /**
     * Retrieves the SupportMapFragment and asynchronously requests
     * the Google Map instance via getMapAsync().
     *
     * Shows a user-facing error if map initialization fails.
     */
    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Error initializing map", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Loads the Event document from Firestore using the eventId.
     *
     * - Validates existence of the event
     * - Checks whether geolocation tracking is enabled
     * - Extracts and processes entrantLocations
     *
     * On failure, the activity exits gracefully with a toast message.
     */
    private void loadEventData() {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        event = document.toObject(Event.class);
                        if (event != null) {
                            event.setId(document.getId());

                            // Check if geolocation is enabled
                            if (!event.isGeolocationEnabled()) {
                                Toast.makeText(this, "Geolocation not enabled for this event",
                                        Toast.LENGTH_LONG).show();
                                finish();
                                return;
                            }

                            // Process entrant locations
                            processEntrantLocations();
                        }
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading event data", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * Processes the entrantLocations field from the Event object.
     *
     * Expected structure:
     * {
     *   "userId": { "latitude": <Double>, "longitude": <Double> },
     *   ...
     * }
     *
     * Validates coordinate ranges, converts each valid entry into LatLng,
     * and prepares the map for rendering markers.
     */
    private void processEntrantLocations() {
        entrantLocations.clear();

        Map<String, Map<String, Double>> locations = event.getEntrantLocations();
        if (locations == null || locations.isEmpty()) {
            Toast.makeText(this, "No entrant locations to display", Toast.LENGTH_LONG).show();
            return;
        }

        // Extract LatLng objects from the map
        int validLocations = 0;
        for (Map.Entry<String, Map<String, Double>> entry : locations.entrySet()) {
            Map<String, Double> coords = entry.getValue();
            if (coords != null && coords.containsKey("latitude") && coords.containsKey("longitude")) {
                Double lat = coords.get("latitude");
                Double lng = coords.get("longitude");

                // Validate coordinates
                if (lat != null && lng != null && isValidCoordinate(lat, lng)) {
                    entrantLocations.add(new LatLng(lat, lng));
                    validLocations++;
                } else {
                    Log.w(TAG, "Invalid coordinates for user " + entry.getKey());
                }
            }
        }
        // Update map if ready
        if (mMap != null) {
            displayLocationsOnMap();
        }
    }

    /**
     * Validates latitude and longitude ranges.
     *
     * @param lat latitude value to validate
     * @param lng longitude value to validate
     * @return true if both coordinates are within geographic bounds
     */
    private boolean isValidCoordinate(double lat, double lng) {
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }

    /**
     * Callback invoked when the Google Map is ready.
     *
     * Configures UI settings, default map type, and renders entrant markers
     * if location data has already been loaded.
     *
     * @param googleMap GoogleMap instance provided by the Maps API
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Configure map settings
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(true);

        // Set map type to normal
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Display locations if already loaded
        if (!entrantLocations.isEmpty()) {
            displayLocationsOnMap();
        } else {
            // Default view (Calgary, Alberta - Dev's location)
            LatLng defaultLocation = new LatLng(51.0447, -114.0719);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
        }
    }

    /**
     * Renders each entrant location on the map by:
     * - Adding a marker
     * - Drawing a visibility circle
     * - Adjusting camera bounds to fit all markers
     *
     * Also displays a toast summarizing how many locations were rendered.
     */
    private void displayLocationsOnMap() {
        if (mMap == null) {
            Log.w(TAG, "Map not ready yet");
            return;
        }

        if (entrantLocations.isEmpty()) {
            Log.d(TAG, "No locations to display");
            return;
        }

        // Clear existing markers
        mMap.clear();

        // Add markers for each entrant location
        for (int i = 0; i < entrantLocations.size(); i++) {
            LatLng location = entrantLocations.get(i);

            // Add marker with custom styling
            mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("Entrant #" + (i + 1))
                    .snippet("Joined waiting list from here")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

            // Add circle around marker for better visibility
            mMap.addCircle(new CircleOptions()
                    .center(location)
                    .radius(MARKER_RADIUS_METERS)
                    .strokeColor(Color.RED)
                    .strokeWidth(2)
                    .fillColor(Color.argb(70, 255, 0, 0))); // Semi-transparent red
        }

        // Adjust camera to show all markers
        adjustCameraToShowAllMarkers();

        // Show success message
        String message = entrantLocations.size() == 1
                ? "1 entrant location displayed"
                : entrantLocations.size() + " entrant locations displayed";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Adjusts the Google Map camera depending on how many locations exist.
     *
     * - If one location → center and zoom in
     * - If multiple → build LatLngBounds and fit map to show all markers
     *
     * Handles IllegalStateException if bounds cannot be computed.
     */
    private void adjustCameraToShowAllMarkers() {
        if (entrantLocations.size() == 1) {
            // Single location - center on it with appropriate zoom
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    entrantLocations.get(0),
                    SINGLE_LOCATION_ZOOM));
        } else {
            // Multiple locations - fit all in view with bounds
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng location : entrantLocations) {
                builder.include(location);
            }

            try {
                LatLngBounds bounds = builder.build();
                // Animate camera with padding around markers
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, MAP_PADDING_PX));
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error building bounds", e);
                // Fallback to first location
                if (!entrantLocations.isEmpty()) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            entrantLocations.get(0),
                            DEFAULT_ZOOM));
                }
            }
        }
    }
}