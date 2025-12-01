package com.example.event_app.utils;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * FavoritesManager - Helper class to manage user's favorite events
 *
 * Features:
 * - Add event to favorites
 * - Remove event from favorites
 * - Check if event is favorited
 * - Get all favorite event IDs
 */
public class FavoritesManager {

    private static final String TAG = "FavoritesManager";

    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;

    /**
     * Creates a FavoritesManager instance and initializes Firebase Auth
     * and Firestore references used for updating user favorite lists.
     */
    public FavoritesManager() {
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
    }

    /**
     * Adds the given event ID to the logged-in user's list of favorite events.
     *
     * Behavior:
     * <ul>
     *   <li>Fails immediately if no user is authenticated</li>
     *   <li>Updates the Firestore array using FieldValue.arrayUnion()</li>
     *   <li>Invokes callback onSuccess() or onFailure()</li>
     * </ul>
     *
     * @param eventId ID of the event to add to favorites
     * @param callback Callback triggered on success or failure
     */
    public void addFavorite(String eventId, FavoriteCallback callback) {
        if (mAuth.getCurrentUser() == null) {
            callback.onFailure("User not logged in");
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .update("favoriteEvents", FieldValue.arrayUnion(eventId))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event added to favorites: " + eventId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add favorite", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Removes the given event ID from the logged-in user's favorite events.
     *
     * Behavior:
     * <ul>
     *   <li>Fails immediately if user is not logged in</li>
     *   <li>Uses FieldValue.arrayRemove() to update Firestore list</li>
     *   <li>Invokes callback onSuccess() or onFailure()</li>
     * </ul>
     *
     * @param eventId ID of the event to remove from favorites
     * @param callback Callback triggered when operation completes
     */
    public void removeFavorite(String eventId, FavoriteCallback callback) {
        if (mAuth.getCurrentUser() == null) {
            callback.onFailure("User not logged in");
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .update("favoriteEvents", FieldValue.arrayRemove(eventId))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event removed from favorites: " + eventId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove favorite", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Checks whether the given event ID is present in the user's favorites.
     *
     * Behavior:
     * <ul>
     *   <li>Returns false immediately if user is not logged in</li>
     *   <li>Fetches user's document and inspects "favoriteEvents" array</li>
     *   <li>Invokes callback with true or false</li>
     * </ul>
     *
     * @param eventId ID of the event to check
     * @param callback Callback returning true if the event is favorited
     */
    public void isFavorite(String eventId, IsFavoriteCallback callback) {
        if (mAuth.getCurrentUser() == null) {
            callback.onResult(false);
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        java.util.List<String> favorites = (java.util.List<String>) document.get("favoriteEvents");
                        boolean isFav = favorites != null && favorites.contains(eventId);
                        callback.onResult(isFav);
                    } else {
                        callback.onResult(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check favorite", e);
                    callback.onResult(false);
                });
    }

    /**
     * Callback for add/remove favorites operations.
     *
     * onSuccess() is invoked when Firestore update succeeds,
     * onFailure(error) provides an error message if the operation fails.
     */
    public interface FavoriteCallback {
        void onSuccess();
        void onFailure(String error);
    }

    /**
     * Callback for checking whether an event is marked as a favorite.
     */
    public interface IsFavoriteCallback {
        void onResult(boolean isFavorite);
    }
}