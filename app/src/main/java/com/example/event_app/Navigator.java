package com.example.event_app;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class Navigator {

    public static final String EXTRA_EVENT_ID = "com.example.event_app.EVENT_ID";

    public void navigateToEventDetails(Context context, String eventId) {
        Intent intent = new Intent(context, EventDetailsActivity.class);
        intent.putExtra(EXTRA_EVENT_ID, eventId);
        context.startActivity(intent);
    }

    public void showInvalidQrError(Context context) {
        Toast.makeText(context, "Invalid or unreadable QR code.", Toast.LENGTH_LONG).show();
    }
}
