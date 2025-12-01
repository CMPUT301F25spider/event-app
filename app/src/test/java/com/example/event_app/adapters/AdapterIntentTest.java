package com.example.event_app.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import android.app.Application;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;

import com.example.event_app.activities.admin.AdminEventDetailsActivity;
import com.example.event_app.activities.entrant.EventDetailsActivity;
import com.example.event_app.activities.organizer.OrganizerEventDetailsActivity;
import com.example.event_app.models.Event;
import com.example.event_app.utils.Navigator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.Collections;

/**
 * Intent navigation tests that ensure list adapters pass event identifiers
 * through their startActivity calls. These tests rely on dummy events to
 * avoid touching live data sources.
 */
@ExtendWith(RobolectricTestRunner.class)
@Config(sdk = 34)
class AdapterIntentTest {

    private Application context;

    @BeforeEach
    void setUp() {
        context = ApplicationProvider.getApplicationContext();
        Shadows.shadowOf(context).clearStartedActivities();
    }

    private Event buildEvent(String id) {
        Event event = new Event();
        event.setId(id);
        event.setEventId(id);
        event.setName("Sample");
        return event;
    }

    @Test
    @DisplayName("EventAdapter opens EventDetailsActivity with extra")
    void eventAdapter_launchesEventDetails() {
        Event event = buildEvent("EVT-111");
        EventAdapter adapter = new EventAdapter(context);
        adapter.setEvents(Collections.singletonList(event));

        ViewGroup parent = new FrameLayout(context);
        EventAdapter.EventViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        holder.itemView.findViewById(com.example.event_app.R.id.cardEvent).performClick();

        ShadowApplication shadowApp = Shadows.shadowOf(context);
        Intent startedIntent = shadowApp.getNextStartedActivity();

        assertNotNull(startedIntent);
        assertEquals(EventDetailsActivity.class.getName(), startedIntent.getComponent().getClassName());
        assertEquals("EVT-111", startedIntent.getStringExtra(Navigator.EXTRA_EVENT_ID));
    }

    @Test
    @DisplayName("FullEventAdapter forwards id to EventDetailsActivity")
    void fullEventAdapter_launchesEventDetails() {
        Event event = buildEvent("EVT-222");
        FullEventAdapter adapter = new FullEventAdapter(context);
        adapter.setEvents(Collections.singletonList(event));

        ViewGroup parent = new FrameLayout(context);
        FullEventAdapter.EventViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        holder.itemView.performClick();

        ShadowApplication shadowApp = Shadows.shadowOf(context);
        Intent startedIntent = shadowApp.getNextStartedActivity();

        assertNotNull(startedIntent);
        assertEquals(EventDetailsActivity.class.getName(), startedIntent.getComponent().getClassName());
        assertEquals("EVT-222", startedIntent.getStringExtra(Navigator.EXTRA_EVENT_ID));
    }

    @Test
    @DisplayName("HorizontalEventAdapter passes event id through intent")
    void horizontalEventAdapter_launchesEventDetails() {
        Event event = buildEvent("EVT-333");
        HorizontalEventAdapter adapter = new HorizontalEventAdapter(context);
        adapter.setEvents(Collections.singletonList(event));

        ViewGroup parent = new FrameLayout(context);
        HorizontalEventAdapter.EventViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        holder.itemView.performClick();

        ShadowApplication shadowApp = Shadows.shadowOf(context);
        Intent startedIntent = shadowApp.getNextStartedActivity();

        assertNotNull(startedIntent);
        assertEquals(EventDetailsActivity.class.getName(), startedIntent.getComponent().getClassName());
        assertEquals("EVT-333", startedIntent.getStringExtra(Navigator.EXTRA_EVENT_ID));
    }

    @Test
    @DisplayName("MyEventsAdapter uses Navigator extra for details intent")
    void myEventsAdapter_launchesEventDetails() {
        Event event = buildEvent("EVT-444");
        MyEventsAdapter adapter = new MyEventsAdapter(context);
        adapter.setEvents(Collections.singletonList(event));

        ViewGroup parent = new FrameLayout(context);
        MyEventsAdapter.EventViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        holder.itemView.findViewById(com.example.event_app.R.id.cardEvent).performClick();

        ShadowApplication shadowApp = Shadows.shadowOf(context);
        Intent startedIntent = shadowApp.getNextStartedActivity();

        assertNotNull(startedIntent);
        assertEquals(EventDetailsActivity.class.getName(), startedIntent.getComponent().getClassName());
        assertEquals("EVT-444", startedIntent.getStringExtra(Navigator.EXTRA_EVENT_ID));
    }

    @Test
    @DisplayName("OrganizerEventsAdapter targets organizer details activity")
    void organizerEventsAdapter_launchesOrganizerDetails() {
        Event event = buildEvent("EVT-555");
        OrganizerEventsAdapter adapter = new OrganizerEventsAdapter(context);
        adapter.setEvents(Collections.singletonList(event));

        ViewGroup parent = new FrameLayout(context);
        OrganizerEventsAdapter.EventViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        holder.itemView.performClick();

        ShadowApplication shadowApp = Shadows.shadowOf(context);
        Intent startedIntent = shadowApp.getNextStartedActivity();

        assertNotNull(startedIntent);
        assertEquals(OrganizerEventDetailsActivity.class.getName(), startedIntent.getComponent().getClassName());
        assertEquals("EVT-555", startedIntent.getStringExtra("EVENT_ID"));
    }

    @Test
    @DisplayName("Admin event browse flow includes event id extra")
    void adminBrowseEvents_launchesAdminDetails() {
        Event event = buildEvent("EVT-666");
        AdminEventAdapter adapter = new AdminEventAdapter();
        adapter.setEvents(Collections.singletonList(event));

        ViewGroup parent = new FrameLayout(context);
        AdminEventAdapter.AdminEventViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        holder.itemView.performClick();

        ShadowApplication shadowApp = Shadows.shadowOf(context);
        Intent startedIntent = shadowApp.getNextStartedActivity();

        assertNotNull(startedIntent);
        assertEquals(AdminEventDetailsActivity.class.getName(), startedIntent.getComponent().getClassName());
        assertEquals("EVT-666", startedIntent.getStringExtra(AdminEventDetailsActivity.EXTRA_EVENT_ID));
    }
}
