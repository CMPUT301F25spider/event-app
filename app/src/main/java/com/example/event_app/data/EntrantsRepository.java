package com.example.event_app.data;

import com.example.event_app.domain.EntrantRow;
import com.example.event_app.domain.EntrantsFilter;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.function.Consumer;

public interface EntrantsRepository {

    /**
     * Listen to entrants based on filter (Selected, Pending, Confirmed etc.)
     */
    ListenerRegistration listenToEntrants(
            String eventId,
            EntrantsFilter filter,
            Consumer<List<EntrantRow>> onSuccess,
            Consumer<Throwable> onError
    );

    /**
     * Listen to count of filtered entrants.
     * (Used for showing "Selected: X" / "Confirmed: Y")
     */
    ListenerRegistration listenToCount(
            String eventId,
            EntrantsFilter filter,
            Consumer<Integer> onSuccess,
            Consumer<Throwable> onError
    );

    /**
     * Cancel a pending entrant (US 02.06.04)
     */
    void cancelEntrant(
            String eventId,
            String entrantId,
            String reason,
            Runnable onSuccess,
            Consumer<Throwable> onError
    );

    /**
     * Optional replacement draw stub — left empty for now as discussed.
     */
    void promoteNextFromWaitlist(
            String eventId,
            Runnable onSuccess,
            Consumer<Throwable> onError
    );
}
