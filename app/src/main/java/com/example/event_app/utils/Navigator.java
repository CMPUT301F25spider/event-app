package com.example.event_app.utils;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.event_app.activities.entrant.EventDetailsActivity;

/**
 * Navigator - Centralized helper class for handling navigation and user feedback.
 *
 * Features:
 * <ul>
 *     <li>Navigates to event details screens</li>
 *     <li>Displays success and error messages</li>
 *     <li>Handles invalid QR scan feedback</li>
 * </ul>
 *
 * Used throughout the app to keep navigation logic consistent.
 */
public class Navigator {

    public static final String EXTRA_EVENT_ID = "com.example.event_app.EVENT_ID";

    /**
     * Navigate to the Event Details screen for the given event.
     *
     * @param context the calling context used to start the new activity
     * @param eventId the ID of the event to display
     */
    public void navigateToEventDetails(Context context, String eventId) {
        Intent intent = new Intent(context, EventDetailsActivity.class);
        intent.putExtra(EXTRA_EVENT_ID, eventId);
        context.startActivity(intent);
    }

    /**
     * Display an error message when a scanned QR code is not valid.
     *
     * @param context the context used to show the toast message
     */
    public void showInvalidQrError(Context context) {
        Toast.makeText(context, "Invalid QR code. Please scan an event QR code.", Toast.LENGTH_LONG).show();
    }

    /**
     * Display a short error message to the user.
     *
     * @param context the context used to show the toast
     * @param message the message text to display
     */
    public void showError(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Display a short success message to the user.
     *
     * @param context the context used to show the toast
     * @param message the message text to display
     */
    public void showSuccess(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}