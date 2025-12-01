package com.example.event_app.models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Represents an event within the LuckySpot system. This model stores all event-related
 * information used by entrants, organizers, and administrators, including registration
 * lists, lottery selections, geolocation data, capacity tracking, and replacement-draw
 * history.
 *
 * <p>The event life cycle includes: waiting list registration, lottery selection,
 * invitation response (accept/decline), replacement draws, capacity verification, and
 * archival once the event date has passed. The model also supports tracking of
 * geolocation audits and various participant groups.</p>
 */
public class Event {

    // Firestore document ID (set manually when loading from Firestore)
    private String id;

    // Basic event information
    private boolean geolocationEnabled;
    private Map<String, Map<String, Double>> entrantLocations;
    private String eventId;          // optional: if you also store ID inside the document
    private String name;
    private String description;
    private String organizerId;
    private String status;           // "active", "cancelled", "completed"
    private long createdAt;
    private String posterUrl;
    private String location;
    private String category;         // Event category: "Food", "Sports", "Music", "Education", "Art", "Technology", "Health", "Other"

    // Registration and Capacity
    private Long capacity;
    private List<String> waitingList;
    private List<String> signedUpUsers;       // Users who ACCEPTED invitation
    private List<String> selectedList;        // Users selected by lottery (waiting for response)
    private List<String> declinedUsers;       // Users who declined invitation
    private List<String> notSelectedList;     // Users who lost lottery (replacement pool)
    private List<Map<String, Object>> replacementLog;  //Track replacements
    private String organizerName;
    private Date eventDate;
    private int entrantCount;

    // Timestamps
    @ServerTimestamp
    private Date date;
    @ServerTimestamp
    private Date registrationStartDate;
    @ServerTimestamp
    private Date registrationEndDate;

    //  Lottery tracking
    private boolean lotteryRun;               // Has lottery been run?
    private long lotteryDate;                 // When was lottery run?
    private boolean archived;                 // Is event past/archived?

    // Lottery statistics
    private int totalSelected;
    private int totalCancelled;
    private int totalAttending;

    // Empty constructor required for Firebase
    public Event() {}

    /**
     * Creates a new event with the basic identifying information. Additional fields
     * such as capacity, category, registration window, and poster URL may be set later.
     *
     * @param eventId     optional event identifier stored in the document
     * @param name        name of the event shown to users
     * @param description brief text describing the event
     * @param organizerId Firestore ID of the organizer who created the event
     */
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
        this.lotteryRun = false;              //Initialize lottery tracking
        this.archived = false;                 //Not archived by default
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getEventId() { return eventId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getOrganizerId() { return organizerId; }
    public String getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public String getPosterUrl() { return posterUrl; }
    public String getLocation() { return location; }
    public String getCategory() { return category; }
    public Long getCapacity() { return capacity; }
    public List<String> getWaitingList() { return waitingList; }
    public List<String> getSignedUpUsers() { return signedUpUsers; }
    public List<String> getSelectedList() { return selectedList; }
    public List<String> getDeclinedUsers() { return declinedUsers; }
    public List<String> getNotSelectedList() { return notSelectedList; }
    public List<Map<String, Object>> getReplacementLog() { return replacementLog; }
    public Date getDate() { return date; }
    public Date getRegistrationStartDate() { return registrationStartDate; }
    public Date getRegistrationEndDate() { return registrationEndDate; }
    public int getTotalSelected() { return totalSelected; }
    public int getTotalCancelled() { return totalCancelled; }
    public int getTotalAttending() { return totalAttending; }
    public String getOrganizerName() { return organizerName; }
    public Date getEventDate() { return eventDate; }
    public int getEntrantCount() { return entrantCount; }
    public boolean isGeolocationEnabled() { return geolocationEnabled; }
    public boolean isLotteryRun() { return lotteryRun; }
    public long getLotteryDate() { return lotteryDate; }
    public boolean isArchived() { return archived; }

    // --- Setters ---
    public void setId(String id) { this.id = id; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public void setLocation(String location) { this.location = location; }
    public void setCategory(String category) { this.category = category; }
    public void setCapacity(Long capacity) { this.capacity = capacity; }
    public void setWaitingList(List<String> waitingList) { this.waitingList = waitingList; }
    public void setSignedUpUsers(List<String> signedUpUsers) { this.signedUpUsers = signedUpUsers; }
    public void setSelectedList(List<String> selectedList) { this.selectedList = selectedList; }
    public void setDeclinedUsers(List<String> declinedUsers) { this.declinedUsers = declinedUsers; }
    public void setNotSelectedList(List<String> notSelectedList) { this.notSelectedList = notSelectedList; }
    public void setReplacementLog(List<Map<String, Object>> replacementLog) {
        this.replacementLog = replacementLog;
    }
    public void setDate(Date date) { this.date = date; }
    public void setRegistrationStartDate(Date registrationStartDate) { this.registrationStartDate = registrationStartDate; }
    public void setRegistrationEndDate(Date registrationEndDate) { this.registrationEndDate = registrationEndDate; }
    public void setTotalSelected(int totalSelected) { this.totalSelected = totalSelected; }
    public void setTotalCancelled(int totalCancelled) { this.totalCancelled = totalCancelled; }
    public void setTotalAttending(int totalAttending) { this.totalAttending = totalAttending; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }
    public void setEventDate(Date eventDate) { this.eventDate = eventDate; }
    public void setEntrantCount(int entrantCount) { this.entrantCount = entrantCount; }
    public void setGeolocationEnabled(boolean geolocationEnabled) { this.geolocationEnabled = geolocationEnabled; }
    public void setLotteryRun(boolean lotteryRun) { this.lotteryRun = lotteryRun; }
    public void setLotteryDate(long lotteryDate) { this.lotteryDate = lotteryDate; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public Map<String, Map<String, Double>> getEntrantLocations() { return entrantLocations; }
    public void setEntrantLocations(Map<String, Map<String, Double>> entrantLocations) {
        this.entrantLocations = entrantLocations;
    }

    // --- Logic Methods ---

    /**
     * Computes the cancellation rate as a percentage of users who were selected
     * but later declined or cancelled. If no users were selected, this returns 0.0.
     *
     * @return cancellation rate between 0.0 and 100.0
     */
    public double getCancellationRate() {
        if (totalSelected == 0) return 0.0;
        return (double) totalCancelled / totalSelected * 100;
    }

    /**
     * Determines whether the cancellation rate exceeds the threshold considered
     * "high" for analytics or administrative review.
     *
     * @return true if the cancellation rate is greater than 30%, otherwise false
     */
    public boolean hasHighCancellationRate() {
        return getCancellationRate() > 30.0;
    }

    /**
     * Calculates the number of remaining available spots based on capacity and
     * the number of users who have accepted their invitation. If no capacity is
     * defined, the event is treated as having unlimited space.
     *
     * @return number of remaining spots, or Integer.MAX_VALUE if unlimited
     */
    public int getSpotsRemaining() {
        if (capacity == null) return Integer.MAX_VALUE;
        int attending = signedUpUsers != null ? signedUpUsers.size() : 0;
        return capacity.intValue() - attending;
    }

    /**
     * Checks whether the current number of attending users has reached or exceeded
     * the event's capacity.
     *
     * @return true if the event is full, otherwise false
     */
    public boolean isCapacityFull() {
        if (capacity == null) return false;
        int attending = signedUpUsers != null ? signedUpUsers.size() : 0;
        return attending >= capacity.intValue();
    }

    /**
     * Determines whether the event has a non-empty replacement pool consisting
     * of users who did not win the initial lottery.
     *
     * @return true if the replacement pool contains at least one user
     */
    public boolean hasReplacementPool() {
        return notSelectedList != null && !notSelectedList.isEmpty();
    }

    /**
     * Checks whether the event date (or server timestamp if event date is null)
     * occurs before the current time, indicating that the event is in the past.
     *
     * @return true if the event date has passed, otherwise false
     */
    public boolean isPast() {
        if (eventDate == null && date == null) return false;
        Date compareDate = eventDate != null ? eventDate : date;
        return compareDate.before(new Date());
    }
}