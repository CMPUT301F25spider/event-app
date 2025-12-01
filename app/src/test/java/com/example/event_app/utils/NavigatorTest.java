package com.example.event_app.utils;

import android.app.Application;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.example.event_app.activities.entrant.EventDetailsActivity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;

import static org.junit.jupiter.api.Assertions.*;

class NavigatorTest {

    @Test
    @DisplayName("navigateToEventDetails launches intent with event extra")
    void navigateToEventDetails_sendsEventId() {
        Navigator navigator = new Navigator();
        Application context = ApplicationProvider.getApplicationContext();

        navigator.navigateToEventDetails(context, "EVT-123456789");

        ShadowApplication shadowApp = Shadows.shadowOf(context);
        Intent startedIntent = shadowApp.getNextStartedActivity();

        assertNotNull(startedIntent, "Expected an intent to be launched for EventDetailsActivity");
        assertEquals(EventDetailsActivity.class.getName(), startedIntent.getComponent().getClassName());
        assertEquals("EVT-123456789", startedIntent.getStringExtra(Navigator.EXTRA_EVENT_ID));
    }
}
