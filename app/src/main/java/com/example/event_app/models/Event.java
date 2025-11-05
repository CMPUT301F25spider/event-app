package com.example.event_app.models;

import com.google.firebase.firestore.ServerTimestamp;import java.util.Date;
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

    // --- Getters ---

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

    public long getCreatedAt() {
        return createdAt;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public Long getCapacity() {
        return capacity;
    }

    public List<String> getWaitingList() {
        return waitingList;
    }

    public List<String> getSignedUpUsers() {
        return signedUpUsers;
    }

    public Date getDate() {
        return date;
    }

    public Date getRegistrationStartDate() {
        return registrationStartDate;
    }

    public Date getRegistrationEndDate() {
        return registrationEndDate;
    }

    public int getTotalSelected() {
        return totalSelected;
    }

    public int getTotalCancelled() {
        return totalCancelled;
    }

    public int getTotalAttending() {
        return totalAttending;
    }


    // --- Setters ---

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

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public void setCapacity(Long capacity) {
        this.capacity = capacity;
    }

    public void setWaitingList(List<String> waitingList) {
        this.waitingList = waitingList;
    }

    public void setSignedUpUsers(List<String> signedUpUsers) {
        this.signedUpUsers = signedUpUsers;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setRegistrationStartDate(Date registrationStartDate) {
        this.registrationStartDate = registrationStartDate;
    }

    public void setRegistrationEndDate(Date registrationEndDate) {
        this.registrationEndDate = registrationEndDate;
    }

    public void setTotalSelected(int totalSelected) {
        this.totalSelected = totalSelected;
    }

    public void setTotalCancelled(int totalCancelled) {
        this.totalCancelled = totalCancelled;
    }

    public void setTotalAttending(int totalAttending) {
        this.totalAttending = totalAttending;
    }


    // --- Logic Methods ---

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
}
