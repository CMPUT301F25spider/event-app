package com.example.event_app.organizer;

public interface EventRepository {
    interface Callback { void onComplete(Result<Void> result); }

    /** Update posterUrl for the given eventId. */
    void updatePosterUrl(String eventId, String newUrl, Callback cb);
}
