package com.example.event_app.organizer;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirestoreEventRepository implements EventRepository {

    private final FirebaseFirestore db;

    /** Use default Firebase app. */
    public FirestoreEventRepository() {
        this(FirebaseFirestore.getInstance());
    }

    /** Inject a specific FirebaseFirestore (useful for tests). */
    public FirestoreEventRepository(FirebaseFirestore db) {
        this.db = db;
    }

    @Override
    public void updatePosterUrl(String eventId, String newUrl, Callback cb) {
        Map<String, Object> update = new HashMap<>();
        update.put("posterUrl", newUrl);

        db.collection("events")
                .document(eventId)
                .update(update)
                .addOnSuccessListener(unused -> cb.onComplete(Result.ok(null)))
                .addOnFailureListener(e -> cb.onComplete(Result.err(e)));
    }
}