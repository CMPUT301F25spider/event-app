package com.example.event_app.organizer;

/**
 * Filter options to view different entrant statuses in organizer UI.
 */
public enum EntrantsFilter {
    Selected,     // selected/invited entrants (selected == true)
    Pending,      // status = "pending"
    Accepted,     // status = "accepted"
    Declined,     // status = "declined"
    Confirmed,    // status = "confirmed" (final attendees)
    Cancelled,    // status = "cancelled"
    All;          // no filter, show all

    /**
     * Convert a string (e.g., from Spinner) to enum safely.
     */
    public static EntrantsFilter fromLabel(String label) {
        if (label == null) return Selected;
        for (EntrantsFilter f : values()) {
            if (f.name().equalsIgnoreCase(label.trim())) {
                return f;
            }
        }
        return Selected;
    }
}
