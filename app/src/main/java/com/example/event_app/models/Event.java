package com.example.event_app.models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;

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
    private String posterUrl; // Added for poster image

    // Registration and Capacity
    private Long capacity;
    private List<String> waitingList;
    private List<String> signedUpUsers;

    // Timestamps
    @ServerTimestamp
    private Date date;
    @ServerTimestamp
    private Date registrationStartDate;
    @ServerTimestamp
    private Date registrationEndDate;

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
    }

    // Getters
    public String getEventId() {
        return eventId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public String getStatus() {
        return status;
    }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public Date getRegistrationEndDate() {return registrationEndDate;}

    public List<String> getWaitingList() {return waitingList;}

    public List<String> getSignedUpUsers() {return signedUpUsers;}

    public Date getRegistrationStartDate() {return registrationStartDate;}

    public long getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}


