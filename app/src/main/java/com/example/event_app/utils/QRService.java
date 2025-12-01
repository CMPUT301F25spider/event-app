package com.example.event_app.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

/**
 * QRService - Handles processing and validation of scanned QR codes.
 *
 * Responsibilities:
 * <ul>
 *     <li>Validate QR content format</li>
 *     <li>Navigate to event details if valid</li>
 *     <li>Show appropriate user-facing error messages on invalid scans</li>
 * </ul>
 *
 * Supports dependency injection for Navigator to improve testability.
 */
public class QRService {

    private static final String TAG = "QRService";
    private final Navigator navigator;

    /**
     * Creates a QRService instance using a default Navigator implementation.
     */
    public QRService() {
        this.navigator = new Navigator();
    }

    /**
     * Creates a QRService with a provided Navigator.
     *
     * @param navigator the Navigator instance used to handle UI navigation and messages
     */
    public QRService(Navigator navigator) {
        this.navigator = navigator;
    }

    /**
     * Processes scanned QR code content.
     * <p>
     * If the QR content matches the expected event ID format, the user is
     * navigated to the Event Details screen. Otherwise, an error message
     * is shown indicating an invalid QR scan.
     * </p>
     *
     * @param context   the context used for navigation and Toast messages
     * @param qrContent the raw string content extracted from the QR code
     */
    public void processQrCode(Context context, String qrContent) {
        Log.d(TAG, "Processing QR code: " + qrContent);

        if (isValidEventId(qrContent)) {
            Log.d(TAG, "Valid event ID, navigating to details");
            navigator.navigateToEventDetails(context, qrContent);
        } else {
            Log.w(TAG, "Invalid QR code format");
            navigator.showInvalidQrError(context);
        }
    }

    /**
     * Validates whether the scanned QR content is a properly formatted event ID.
     * <p>
     * Rules:
     * <ul>
     *     <li>Must not be null or empty</li>
     *     <li>Length must be between 10 and 50 characters</li>
     *     <li>Allowed characters: letters, digits, underscore, hyphen</li>
     * </ul>
     * </p>
     *
     * @param eventId the QR content to validate
     * @return true if the event ID is valid, false otherwise
     */
    private boolean isValidEventId(String eventId) {
        if (TextUtils.isEmpty(eventId)) {
            return false;
        }

        eventId = eventId.trim();

        if (eventId.length() < 10 || eventId.length() > 50) {
            return false;
        }

        if (!eventId.matches("[a-zA-Z0-9_-]+")) {
            return false;
        }

        return true;
    }
}