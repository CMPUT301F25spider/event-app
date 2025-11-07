package com.example.event_app;

import android.content.Context;
import android.text.TextUtils;

/**
 * Parses scanned QR codes and collaborates with the Navigator to trigger navigation.
 */
public class QRService {

    private final Navigator navigator;

    public QRService(Navigator navigator) {
        this.navigator = navigator;
    }

    /**
     * Processes the raw text content from a scanned QR code.
     * It expects the content to be a non-empty event ID.
     */
    public void processQrCode(Context context, String qrContent) {
        if (isValidEventId(qrContent)) {
            navigator.navigateToEventDetails(context, qrContent);
        } else {
            navigator.showInvalidQrError(context);
        }
    }

    /**
     * Validates the format of the scanned content.
     */
    private boolean isValidEventId(String eventId) {
        // TODO: Implement proper validation logic.
        return !TextUtils.isEmpty(eventId);
    }
}
