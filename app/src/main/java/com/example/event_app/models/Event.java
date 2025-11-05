package com.example.event_app.models;

/**
 * Event Model - Simplified version for halfway checkpoint
 * Represents an event in the LuckySpot system
 */
public class Event {

    // Basic event information
    private String eventId;
    private String name;
    private String description;
    private String organizerId;
    private String status;  // "active", "cancelled", "completed"
    private long createdAt;

    // Lottery statistics (for cancellation tracking)
    private int totalSelected;      // Total number selected in lottery
    private int totalCancelled;     // Number who cancelled/declined
    private int totalAttending;     // Number confirmed attending

    // Empty constructor required for Firebase
    public Event() {
    }

    // Constructor for creating new events
    public Event(String eventId, String name, String description, String organizerId) {
        this.eventId = eventId;
        this.name = name;
        this.description = description;
        this.organizerId = organizerId;
        this.status = "active";
        this.createdAt = System.currentTimeMillis();
        this.totalSelected = 0;
        this.totalCancelled = 0;
        this.totalAttending = 0;
    }

    // Calculate cancellation rate
    public double getCancellationRate() {
        if (totalSelected == 0) {
            return 0.0;
        }
        return (double) totalCancelled / totalSelected * 100;
    }

    // Check if cancellation rate is high (>30%)
    public boolean hasHighCancellationRate() {
        return getCancellationRate() > 30.0;
    }

    // Getters
    public String getEventId() { return eventId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getOrganizerId() { return organizerId; }
    public String getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public int getTotalSelected() { return totalSelected; }
    public int getTotalCancelled() { return totalCancelled; }
    public int getTotalAttending() { return totalAttending; }

    // Setters
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setTotalSelected(int totalSelected) { this.totalSelected = totalSelected; }
    public void setTotalCancelled(int totalCancelled) { this.totalCancelled = totalCancelled; }
    public void setTotalAttending(int totalAttending) { this.totalAttending = totalAttending; }
}
