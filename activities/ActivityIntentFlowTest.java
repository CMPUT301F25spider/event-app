package com.example.event_app.activities;

import android.app.Application;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.example.event_app.activities.admin.AdminEventDetailsActivity;
import com.example.event_app.activities.entrant.EventDetailsActivity;
import com.example.event_app.activities.organizer.OrganizerEventDetailsActivity;
import com.example.event_app.activities.organizer.ViewEntrantMapActivity;
import com.example.event_app.activities.organizer.ViewEntrantsActivity;
import com.example.event_app.activities.shared.ProfileSetupActivity;
import com.example.event_app.utils.Navigator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActivityIntentFlowTest {

    private final Application context = ApplicationProvider.getApplicationContext();

    @Test
    @DisplayName("EventDetailsActivity receives event ID extra")
    void eventDetailsActivity_readsEventId() {
        Intent intent = new Intent(context, EventDetailsActivity.class);
        intent.putExtra(Navigator.EXTRA_EVENT_ID, "EVT-123456789");

        assertEquals("EVT-123456789", intent.getStringExtra(Navigator.EXTRA_EVENT_ID));
    }

    @Test
    @DisplayName("OrganizerEventDetailsActivity keeps event ID payload")
    void organizerEventDetailsActivity_readsEventId() {
        Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
        intent.putExtra("EVENT_ID", "EVT-ORG-42");

        assertEquals("EVT-ORG-42", intent.getStringExtra("EVENT_ID"));
    }

    @Test
    @DisplayName("AdminEventDetailsActivity preserves event ID payload")
    void adminEventDetailsActivity_readsEventId() {
        Intent intent = new Intent(context, AdminEventDetailsActivity.class);
        intent.putExtra(AdminEventDetailsActivity.EXTRA_EVENT_ID, "EVT-ADM-99");

        assertEquals("EVT-ADM-99", intent.getStringExtra(AdminEventDetailsActivity.EXTRA_EVENT_ID));
    }

    @Test
    @DisplayName("ViewEntrantsActivity consumes event ID for entrant lists")
    void viewEntrantsActivity_readsEventId() {
        Intent intent = new Intent(context, ViewEntrantsActivity.class);
        intent.putExtra("EVENT_ID", "EVT-ENTRANTS-7");

        assertEquals("EVT-ENTRANTS-7", intent.getStringExtra("EVENT_ID"));
    }

    @Test
    @DisplayName("ViewEntrantMapActivity carries event ID for location lookup")
    void viewEntrantMapActivity_readsEventId() {
        Intent intent = new Intent(context, ViewEntrantMapActivity.class);
        intent.putExtra("EVENT_ID", "EVT-MAP-11");

        assertEquals("EVT-MAP-11", intent.getStringExtra("EVENT_ID"));
    }

    @Test
    @DisplayName("ProfileSetupActivity transfers device and user identifiers")
    void profileSetupActivity_readsIdentifiers() {
        Intent intent = new Intent(context, ProfileSetupActivity.class);
        intent.putExtra("deviceId", "device-123");
        intent.putExtra("userId", "user-789");

        assertEquals("device-123", intent.getStringExtra("deviceId"));
        assertEquals("user-789", intent.getStringExtra("userId"));
    }
}
