package com.example.event_app.utils;

/**
 * UserRole - Defines the role constants used across the application.
 *
 * <p>These values are stored in Firestore and used for access control,
 * UI permissions, and feature availability checks.</p>
 *
 * <ul>
 *     <li>{@link #ENTRANT} — Standard user attending events</li>
 *     <li>{@link #ORGANIZER} — User with permissions to create and manage events</li>
 *     <li>{@link #ADMIN} — User with elevated access for platform-wide management</li>
 * </ul>
 */
public class UserRole {
    public static final String ENTRANT = "entrant";
    public static final String ORGANIZER = "organizer";
    public static final String ADMIN = "admin";
}
