package com.example.event_app.data;

import static com.example.event_app.domain.EntrantsFilter.Accepted;
import static com.example.event_app.domain.EntrantsFilter.Cancelled;
import static com.example.event_app.domain.EntrantsFilter.Confirmed;
import static com.example.event_app.domain.EntrantsFilter.Declined;
import static com.example.event_app.domain.EntrantsFilter.Pending;
import static com.example.event_app.domain.EntrantsFilter.Selected;

import com.example.event_app.domain.EntrantRow;
import com.example.event_app.domain.EntrantsFilter;
import com.google.firebase.FieldValue;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;

import java.util.*;
import java.util.function.Consumer;

public class FirestoreEntrantsRepository implements EntrantsRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final ListenerRegistration NOOP = () -> {};

    private CollectionReference entrants(String eventId) {
        return db.collection("events")
                .document(eventId)
                .collection("entrants");
    }

    private Query getQuery(String eventId, EntrantsFilter filter) {
        CollectionReference col = entrants(eventId);
        // NOTE: The secondary orderBy("uid") is optional, but helps avoid "inequality requires orderBy" issues on ties.
        if (filter.equals(Selected)) {
            return col.whereEqualTo("selected", true)
                    .orderBy("selectionTimestamp", Query.Direction.DESCENDING)
                    .orderBy("uid", Query.Direction.DESCENDING);
        } else if (filter.equals(Pending)) {
            return col.whereEqualTo("status", "pending")
                    .orderBy("selectionTimestamp", Query.Direction.DESCENDING)
                    .orderBy("uid", Query.Direction.DESCENDING);
        } else if (filter.equals(Accepted)) {
            return col.whereEqualTo("status", "accepted")
                    .orderBy("selectionTimestamp", Query.Direction.DESCENDING)
                    .orderBy("uid", Query.Direction.DESCENDING);
        } else if (filter.equals(Declined)) {
            return col.whereEqualTo("status", "declined")
                    .orderBy("selectionTimestamp", Query.Direction.DESCENDING)
                    .orderBy("uid", Query.Direction.DESCENDING);
        } else if (filter.equals(Confirmed)) {
            return col.whereEqualTo("status", "confirmed")
                    .orderBy("confirmationTimestamp", Query.Direction.DESCENDING)
                    .orderBy("uid", Query.Direction.DESCENDING);
        } else if (filter.equals(Cancelled)) {
            return col.whereEqualTo("status", "cancelled")
                    .orderBy("cancellationTimestamp", Query.Direction.DESCENDING)
                    .orderBy("uid", Query.Direction.DESCENDING);
        }
        return col.orderBy("selectionTimestamp", Query.Direction.DESCENDING)
                .orderBy("uid", Query.Direction.DESCENDING);
    }

    @Override
    public ListenerRegistration listenToEntrants(String eventId, EntrantsFilter filter,
                                                 Consumer<List<EntrantRow>> onSuccess,
                                                 Consumer<Throwable> onError) {
        return getQuery(eventId, filter)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) {
                        if (onError != null) onError.accept(e);
                        return;
                    }
                    List<EntrantRow> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        EntrantRow row = EntrantRow.from(d);
                        if (row != null) list.add(row);
                    }
                    onSuccess.accept(list);
                });
    }

    @Override
    public ListenerRegistration listenToCount(String eventId, EntrantsFilter filter,
                                              Consumer<Integer> onSuccess,
                                              Consumer<Throwable> onError) {
        // Use Aggregate COUNT to avoid streaming all docs for a badge number
        Query q;
        if (filter == Selected) {
            q = entrants(eventId).whereEqualTo("selected", true);
        } else if (filter == Confirmed) {
            q = entrants(eventId).whereEqualTo("status", "confirmed");
        } else {
            return NOOP; // no-op instead of null
        }

        AggregateQuery countQuery = q.count();
        // No real-time listener for aggregate; poll by re-invoking when filter changes (that’s fine for a badge).
        countQuery.get(AggregateSource.SERVER)
                .addOnSuccessListener(r -> onSuccess.accept((int) r.getCount()))
                .addOnFailureListener(ex -> { if (onError != null) onError.accept(ex); });

        return NOOP;
    }

    @Override
    public void cancelEntrant(String eventId, String entrantId, String reason,
                              Runnable onSuccess, Consumer<Throwable> onError) {
        // Guard with a transaction: only allow cancel if currently pending
        DocumentReference doc = entrants(eventId).document(entrantId);
        db.runTransaction(trx -> {
                    DocumentSnapshot snap = trx.get(doc);
                    if (!snap.exists()) throw new IllegalStateException("Entrant not found");
                    String status = snap.getString("status");
                    if (status == null || !"pending".equalsIgnoreCase(status)) {
                        throw new IllegalStateException("Only pending entrants can be cancelled");
                    }
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "cancelled");
                    updates.put("selected", false);
                    updates.put("cancellationReason", reason);
                    updates.put("cancellationTimestamp", FieldValue.serverTimestamp());
                    trx.update(doc, updates);
                    return null;
                }).addOnSuccessListener(v -> { if (onSuccess != null) onSuccess.run(); })
                .addOnFailureListener(ex -> { if (onError != null) onError.accept(ex); });
    }

    @Override
    public void promoteNextFromWaitlist(String eventId,
                                        Runnable onSuccess,
                                        Consumer<Throwable> onError) {
        // Stub — hook up your draw/promote logic later (transaction or CF).
        if (onSuccess != null) onSuccess.run();
    }

    // OPTIONAL: pagination overload (use if you expect long lists)
    public ListenerRegistration listenToEntrantsPage(String eventId,
                                                     EntrantsFilter filter,
                                                     int pageSize,
                                                     DocumentSnapshot startAfter,
                                                     Consumer<List<EntrantRow>> onSuccess,
                                                     Consumer<Throwable> onError) {
        Query q = getQuery(eventId, filter).limit(pageSize);
        if (startAfter != null) q = q.startAfter(startAfter);
        return q.addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) {
                if (onError != null) onError.accept(e);
                return;
            }
            List<EntrantRow> list = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                EntrantRow row = EntrantRow.from(d);
                if (row != null) list.add(row);
            }
            onSuccess.accept(list);
        });
    }
}
