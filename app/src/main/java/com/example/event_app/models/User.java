package com.example.event_app.models;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user within the LuckySpot system. A user is uniquely identified
 * by their device ID rather than a traditional password-based account. Users may
 * participate as entrants, organizers, or administrators depending on their roles.
 *
 * <p>This model stores profile information, notification preferences, role
 * assignments, and favourite events. It also holds the user’s Firebase Cloud
 * Messaging (FCM) token used for sending personalized push notifications.</p>
 */
public class User {

    private String userId; // Firebase Auth UID
    private String deviceId; // Android device ID
    private String name;
    private String email;
    private String phoneNumber; // Optional
    private List<String> roles; // ["entrant"], ["organizer"], or both
    private boolean notificationsEnabled;
    private long createdAt;
    private long updatedAt;

    // Favorite events
    private List<String> favoriteEvents; // List of event IDs marked as favorite

    // FCM push notification token
    private String fcmToken; // Firebase Cloud Messaging token for push notifications

    // Empty constructor for Firebase
    public User() {
        this.roles = new ArrayList<>();
        this.favoriteEvents = new ArrayList<>();
    }

    /**
     * Creates a new user profile with the required identifying information.
     * Timestamps and default notification settings are assigned automatically.
     *
     * @param userId    Firebase Auth UID
     * @param deviceId  unique device identifier for authentication
     * @param name      display name chosen by the user
     * @param email     email associated with the account
     */
    public User(String userId, String deviceId, String name, String email) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.name = name;
        this.email = email;
        this.roles = new ArrayList<>();
        this.favoriteEvents = new ArrayList<>();
        this.notificationsEnabled = true;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Assigns a new role to the user if it is not already present.
     *
     * @param role role to be added (e.g., "entrant", "organizer", "admin")
     */
    public void addRole(String role) {
        if (roles == null) {
            roles = new ArrayList<>();
        }
        if (!roles.contains(role)) {
            roles.add(role);
        }
    }

    /**
     * Checks whether the user currently holds a particular role.
     *
     * @param role role name to check
     * @return true if the user has this role, false otherwise
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * @return true if the user is an administrator.
     */
    public boolean isAdmin() {
        return hasRole("admin");
    }

    /**
     * @return true if the user is an event organizer.
     */
    public boolean isOrganizer() {
        return hasRole("organizer");
    }

    /**
     * @return true if the user participates as an entrant.
     */
    public boolean isEntrant() {
        return hasRole("entrant");
    }

    // Favorite helper methods

    /**
     * Marks an event as a favourite for quick access and recommendations.
     *
     * @param eventId ID of the event to add
     */
    public void addFavorite(String eventId) {
        if (favoriteEvents == null) {
            favoriteEvents = new ArrayList<>();
        }

        if (!favoriteEvents.contains(eventId)) {
            favoriteEvents.add(eventId);
        }
    }

    /**
     * Removes an event from the user's favourite list.
     *
     * @param eventId ID of the event to remove
     */
    public void removeFavorite(String eventId) {
        if (favoriteEvents != null) {
            favoriteEvents.remove(eventId);
        }
    }

    /**
     * Checks whether an event is marked as favourite by the user.
     *
     * @param eventId event identifier
     * @return true if the event is favourited, false otherwise
     */
    public boolean isFavorite(String eventId) {
        return favoriteEvents != null && favoriteEvents.contains(eventId);
    }

    // Getters
    public String getUserId() { return userId; }
    public String getDeviceId() { return deviceId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public List<String> getRoles() { return roles; }
    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public List<String> getFavoriteEvents() { return favoriteEvents; }
    public String getFcmToken() { return fcmToken; } // ✨ NEW

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public void setFavoriteEvents(List<String> favoriteEvents) { this.favoriteEvents = favoriteEvents; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
}

