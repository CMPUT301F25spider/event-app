package com.example.event_app.data;

import android.net.Uri;

import com.example.event_app.domain.Result;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


public class FirebasePosterStorage implements PosterStorage {

    private final FirebaseStorage storage;

    /**
     * Use default Firebase app.
     */
    public FirebasePosterStorage() {
        this(FirebaseStorage.getInstance());
    }

    /**
     * Inject a specific FirebaseStorage (useful for tests).
     */
    public FirebasePosterStorage(FirebaseStorage storage) {
        this.storage = storage;
    }

    @Override
    public void upload(String eventId, Uri file, Callback<String> cb) {
        // Best-effort extension detection (defaults to .jpg)
        String lower = file.toString().toLowerCase();
        String ext = lower.endsWith(".png") ? "png" : "jpg";
        String fileName = "poster_" + System.currentTimeMillis() + "." + ext;

        StorageReference ref = storage
                .getReference("event_posters/" + eventId + "/" + fileName);

        ref.putFile(file)
                .addOnFailureListener(e -> cb.onComplete(Result.err(e)))
                .addOnSuccessListener(snapshot -> {
                    // Get the download URL
                    Task<Uri> urlTask = ref.getDownloadUrl();
                    urlTask.addOnSuccessListener(uri -> cb.onComplete(Result.ok(uri.toString())))
                            .addOnFailureListener(e -> cb.onComplete(Result.err(e)));
                });
    }

    @Override
    public void deleteByUrl(String fileUrl, Callback<Void> cb) {
        try {
            storage.getReferenceFromUrl(fileUrl)
                    .delete()
                    .addOnSuccessListener(unused -> cb.onComplete(Result.ok(null)))
                    .addOnFailureListener(e -> cb.onComplete(Result.err(e)));
        } catch (Exception e) {
            // Not a Firebase Storage URL or malformed → treat as no-op success
            cb.onComplete(Result.ok(null));
        }
    }
}