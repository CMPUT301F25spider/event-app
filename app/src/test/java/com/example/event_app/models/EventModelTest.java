package com.example.event_app.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventModelTest {

    @Test
    @DisplayName("spots remaining is capacity minus signed-up users")
    void getSpotsRemaining_accountsForAttendees() {
        Event event = new Event("EVT-1", "Music Night", "", "org-1");
        event.setCapacity(5L);
        event.setSignedUpUsers(new ArrayList<>(Arrays.asList("u1", "u2")));

        assertEquals(3, event.getSpotsRemaining());
    }

    @Test
    @DisplayName("unlimited capacity returns max value")
    void getSpotsRemaining_handlesUnlimitedEvents() {
        Event event = new Event("EVT-1", "Music Night", "", "org-1");

        assertEquals(Integer.MAX_VALUE, event.getSpotsRemaining());
    }

    @Test
    @DisplayName("cancellation rate reflects total selected and cancelled")
    void getCancellationRate_tracksCancelledRatio() {
        Event event = new Event("EVT-1", "Music Night", "", "org-1");
        event.setTotalSelected(10);
        event.setTotalCancelled(4);

        assertEquals(40.0, event.getCancellationRate());
        assertTrue(event.hasHighCancellationRate());
    }

    @Test
    @DisplayName("capacity full guards event enrollment")
    void isCapacityFull_checksAgainstCapacity() {
        Event event = new Event("EVT-1", "Music Night", "", "org-1");
        event.setCapacity(2L);
        event.setSignedUpUsers(new ArrayList<>(List.of("u1", "u2")));

        assertTrue(event.isCapacityFull());
    }

    @Test
    @DisplayName("replacement pool is flagged only when populated")
    void hasReplacementPool_requiresEntrants() {
        Event event = new Event("EVT-1", "Music Night", "", "org-1");
        event.setNotSelectedList(List.of("u3"));

        assertTrue(event.hasReplacementPool());
    }

    @Test
    @DisplayName("event is past when eventDate is before now")
    void isPast_usesEventDateWhenAvailable() {
        Event event = new Event("EVT-1", "Music Night", "", "org-1");
        event.setEventDate(new Date(System.currentTimeMillis() - 1_000));

        assertTrue(event.isPast());
    }
}
