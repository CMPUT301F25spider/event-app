package com.example.event_app.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.event_app.TestDataLoader;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EventModelTest {

    @Test
    public void constructorSetsDefaultsAndTimestamps() throws IOException {
        Map<String, String> data = TestDataLoader.loadRecord("test-data/events.csv", "EVT1001");

        long before = System.currentTimeMillis();
        Event event = new Event(data.get("eventId"), data.get("name"), data.get("description"), data.get("organizerId"));
        long after = System.currentTimeMillis();

        assertEquals("active", event.getStatus());
        assertTrue(event.getCreatedAt() >= before && event.getCreatedAt() <= after);
        assertEquals(0, event.getTotalSelected());
        assertEquals(0, event.getTotalCancelled());
        assertEquals(0, event.getTotalAttending());
    }

    @Test
    public void settersUpdateOptionalFields() throws IOException {
        Map<String, String> data = TestDataLoader.loadRecord("test-data/events.csv", "EVT2002");
        Event event = new Event(data.get("eventId"), data.get("name"), data.get("description"), data.get("organizerId"));

        event.setPosterUrl(data.get("posterUrl"));
        event.setLocation(data.get("location"));
        event.setCapacity(Long.parseLong(data.get("capacity")));

        List<String> waiting = Arrays.asList("USR3001", "USR4002");
        List<String> signedUp = Arrays.asList("USR3001");
        event.setWaitingList(waiting);
        event.setSignedUpUsers(signedUp);

        assertEquals(data.get("posterUrl"), event.getPosterUrl());
        assertEquals(data.get("location"), event.getLocation());
        assertEquals(Long.parseLong(data.get("capacity")), event.getCapacity().longValue());
        assertEquals(waiting, event.getWaitingList());
        assertEquals(signedUp, event.getSignedUpUsers());
    }

    @Test
    public void settersUpdateTotals() {
        Event event = new Event("EVT3003", "Local Hackathon", "48 hour buildathon", "ORG900");

        event.setTotalSelected(25);
        event.setTotalCancelled(3);
        event.setTotalAttending(18);

        assertEquals(25, event.getTotalSelected());
        assertEquals(3, event.getTotalCancelled());
        assertEquals(18, event.getTotalAttending());
    }
}
