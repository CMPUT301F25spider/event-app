package com.example.event_app.domain;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * UI-facing row model for a single entrant in the list.
 * Mirrors Firestore structure under: events/{eventId}/entrants/{entrantId}
 */
public class EntrantRow {

    public String id;        // Document ID
    public String uid;       // Entrant's user ID
    public String name;      // Display name
    public String email;     // Entrant email
    public String status;    // pending, accepted, declined, confirmed, cancelled

    // Relevant timestamps
    public Timestamp selectionTimestamp;
    public Timestamp confirmationTimestamp;
    public Timestamp invitationExpiry;
    public Timestamp cancellationTimestamp;

    /**
     * Builds EntrantRow from Firestore DocumentSnapshot.
     */
    public static EntrantRow from(DocumentSnapshot d) {
        if (d == null || !d.exists()) return null;

        EntrantRow r = new EntrantRow();
        r.id = d.getId();
        r.uid = d.getString("uid");
        r.name = d.getString("name");
        r.email = d.getString("email");
        r.status = d.getString("status");

        r.selectionTimestamp = d.getTimestamp("selectionTimestamp");
        r.confirmationTimestamp = d.getTimestamp("confirmationTimestamp");
        r.invitationExpiry = d.getTimestamp("invitationExpiry");
        r.cancellationTimestamp = d.getTimestamp("cancellationTimestamp");

        return r;
    }
}
