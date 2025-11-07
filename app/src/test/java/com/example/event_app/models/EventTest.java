package com.example.event_app.models;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Event model business logic
 */
public class EventTest {

    @Test
    public void testGetCancellationRate_ZeroSelected() {
        Event event = new Event();
        event.setTotalSelected(0);
        event.setTotalCancelled(5);
        
        double rate = event.getCancellationRate();
        assertEquals("Cancellation rate should be 0 when totalSelected is 0", 0.0, rate, 0.001);
    }

    @Test
    public void testGetCancellationRate_NoCancellations() {
        Event event = new Event();
        event.setTotalSelected(100);
        event.setTotalCancelled(0);
        
        double rate = event.getCancellationRate();
        assertEquals("Cancellation rate should be 0 when no cancellations", 0.0, rate, 0.001);
    }

    @Test
    public void testGetCancellationRate_FiftyPercent() {
        Event event = new Event();
        event.setTotalSelected(100);
        event.setTotalCancelled(50);
        
        double rate = event.getCancellationRate();
        assertEquals("Cancellation rate should be 50%", 50.0, rate, 0.001);
    }

    @Test
    public void testGetCancellationRate_OneHundredPercent() {
        Event event = new Event();
        event.setTotalSelected(10);
        event.setTotalCancelled(10);
        
        double rate = event.getCancellationRate();
        assertEquals("Cancellation rate should be 100%", 100.0, rate, 0.001);
    }

    @Test
    public void testGetCancellationRate_PartialCancellation() {
        Event event = new Event();
        event.setTotalSelected(200);
        event.setTotalCancelled(75);
        
        double rate = event.getCancellationRate();
        assertEquals("Cancellation rate should be 37.5%", 37.5, rate, 0.001);
    }

    @Test
    public void testHasHighCancellationRate_BelowThreshold() {
        Event event = new Event();
        event.setTotalSelected(100);
        event.setTotalCancelled(29); // 29% cancellation rate
        
        assertFalse("Should not have high cancellation rate below 30%", event.hasHighCancellationRate());
    }

    @Test
    public void testHasHighCancellationRate_AtThreshold() {
        Event event = new Event();
        event.setTotalSelected(100);
        event.setTotalCancelled(30); // Exactly 30% cancellation rate
        
        assertFalse("Should not have high cancellation rate at exactly 30%", event.hasHighCancellationRate());
    }

    @Test
    public void testHasHighCancellationRate_AboveThreshold() {
        Event event = new Event();
        event.setTotalSelected(100);
        event.setTotalCancelled(31); // 31% cancellation rate
        
        assertTrue("Should have high cancellation rate above 30%", event.hasHighCancellationRate());
    }

    @Test
    public void testHasHighCancellationRate_ZeroSelected() {
        Event event = new Event();
        event.setTotalSelected(0);
        event.setTotalCancelled(10);
        
        assertFalse("Should not have high cancellation rate when totalSelected is 0", event.hasHighCancellationRate());
    }

    @Test
    public void testHasHighCancellationRate_OneHundredPercent() {
        Event event = new Event();
        event.setTotalSelected(10);
        event.setTotalCancelled(10); // 100% cancellation rate
        
        assertTrue("Should have high cancellation rate at 100%", event.hasHighCancellationRate());
    }

    @Test
    public void testEventConstructor() {
        String eventId = "test-event-123";
        String name = "Test Event";
        String description = "Test Description";
        String organizerId = "organizer-123";
        
        Event event = new Event(eventId, name, description, organizerId);
        
        assertEquals("Event ID should match", eventId, event.getEventId());
        assertEquals("Name should match", name, event.getName());
        assertEquals("Description should match", description, event.getDescription());
        assertEquals("Organizer ID should match", organizerId, event.getOrganizerId());
        assertEquals("Status should be active", "active", event.getStatus());
        assertEquals("Total selected should be 0", 0, event.getTotalSelected());
        assertEquals("Total cancelled should be 0", 0, event.getTotalCancelled());
        assertEquals("Total attending should be 0", 0, event.getTotalAttending());
        assertTrue("Created at should be set", event.getCreatedAt() > 0);
    }

    @Test
    public void testEventEmptyConstructor() {
        Event event = new Event();
        
        assertNotNull("Event should be created", event);
        // Empty constructor should create event with default values
    }
}
