package com.example.event_app.models;

import java.util.Date;

/**
 * Represents a geolocation audit entry used for privacy compliance and
 * administrative monitoring. Each audit record captures when a user’s location
 * was accessed or logged during interactions with an event.
 *
 * <p>Geolocation audits help track actions such as joining a waiting list,
 * scanning a QR code, or verifying event attendance. These logs support
 * transparency, administrative review, and compliance with data-handling
 * requirements.</p>
 */
public class GeolocationAudit {

    private String auditId;
    private String userId;
    private String userName;
    private String eventId;
    private String eventName;
    private double latitude;
    private double longitude;
    private Date timestamp;
    private String action;  // "joined_waiting_list", "scanned_qr", etc.

    // Empty constructor for Firebase
    public GeolocationAudit() {
    }

    /**
     * Creates a new geolocation audit log entry capturing the user, event,
     * location data, action performed, and timestamp of the event.
     *
     * @param auditId    unique identifier for the audit entry
     * @param userId     ID of the user whose location was logged
     * @param userName   display name of the user
     * @param eventId    associated event ID
     * @param eventName  name of the associated event
     * @param latitude   recorded latitude value
     * @param longitude  recorded longitude value
     * @param timestamp  date and time when the location was captured
     * @param action     description of the user action that triggered logging
     */
    public GeolocationAudit(String auditId, String userId, String userName,
                            String eventId, String eventName,
                            double latitude, double longitude,
                            Date timestamp, String action) {
        this.auditId = auditId;
        this.userId = userId;
        this.userName = userName;
        this.eventId = eventId;
        this.eventName = eventName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.action = action;
    }

    // Getters and setters
    public String getAuditId() {
        return auditId;
    }

    public void setAuditId(String auditId) {
        this.auditId = auditId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Formats the latitude and longitude into a readable coordinate string.
     *
     * @return a string representing the location in "lat, lon" format rounded to 4 decimals
     */
    public String getLocationString() {
        return String.format("%.4f, %.4f", latitude, longitude);
    }
}
