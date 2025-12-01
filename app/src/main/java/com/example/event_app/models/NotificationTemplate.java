package com.example.event_app.models;

import java.util.Date;

/**
 * Represents a reusable notification template used across the LuckySpot system.
 * Templates allow administrators to define consistent notification titles and
 * message bodies that may contain placeholders (e.g., {userName}, {eventName}).
 *
 * <p>These templates make it easier for organizers and admins to send
 * standardized notifications such as invitations, reminders, and announcements
 * without rewriting messages each time. Templates can be activated or
 * deactivated depending on availability or administrative policy.</p>
 */
public class NotificationTemplate {
    private String templateId;
    private String name;               // Template name (e.g., "Invitation Sent")
    private String type;               // Template type/category
    private String title;              // Notification title with placeholders
    private String message;            // Notification message with placeholders
    private boolean isActive;          // Whether template is currently in use
    private Date createdAt;
    private Date updatedAt;
    private String createdBy;          // Admin who created it

    // Placeholders that can be used: {userName}, {eventName}, {organizerName}, {date}, etc.

    // Empty constructor for Firebase
    public NotificationTemplate() {
    }

    /**
     * Creates a new notification template with customizable title,
     * message body, and placeholder support.
     *
     * @param templateId unique identifier for the template
     * @param name       user-friendly template name
     * @param type       category/type of notification this template represents
     * @param title      notification title (may contain placeholders)
     * @param message    message body (may contain placeholders)
     * @param isActive   whether this template is currently available for use
     * @param createdAt  timestamp when the template was created
     * @param updatedAt  timestamp of the latest modification
     * @param createdBy  administrator ID who created the template
     */
    public NotificationTemplate(String templateId, String name, String type,
                                String title, String message, boolean isActive,
                                Date createdAt, Date updatedAt, String createdBy) {
        this.templateId = templateId;
        this.name = name;
        this.type = type;
        this.title = title;
        this.message = message;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
    }

    // Getters and setters
    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Replaces placeholder tokens in the provided template text with actual
     * runtime values. Supported placeholders include:
     * <ul>
     *   <li>{userName}</li>
     *   <li>{eventName}</li>
     *   <li>{organizerName}</li>
     *   <li>{date}</li>
     * </ul>
     *
     * <p>This allows administrators and organizers to generate personalized
     * notifications without modifying the template manually.</p>
     *
     * @param text          template text containing placeholders
     * @param userName      name of the user receiving the notification
     * @param eventName     associated event name
     * @param organizerName name of the event organizer
     * @param date          date string formatted for display
     * @return formatted text with placeholders replaced; empty string if input text is null
     */
    public String applyPlaceholders(String text, String userName, String eventName,
                                    String organizerName, String date) {
        if (text == null) return "";

        return text.replace("{userName}", userName != null ? userName : "")
                .replace("{eventName}", eventName != null ? eventName : "")
                .replace("{organizerName}", organizerName != null ? organizerName : "")
                .replace("{date}", date != null ? date : "");
    }
}