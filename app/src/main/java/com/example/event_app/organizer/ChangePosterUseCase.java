package com.example.event_app.organizer;

import android.net.Uri;

public class ChangePosterUseCase {

    public interface Callback {
        void onProgress(boolean loading); // Tell UI to show/hide loading
        void onSuccess(String newUrl);    // Tell UI new poster URL is ready
        void onError(String message);     // Tell UI an error happened
    }

    private final PosterValidator validator;
    private final PosterStorage storage;
    private final EventRepository events;

    public ChangePosterUseCase(PosterValidator validator, PosterStorage storage, EventRepository events) {
        this.validator = validator;
        this.storage = storage;
        this.events = events;
    }

    public void execute(String eventId, String oldPosterUrl, Uri newImage, Callback cb) {
        if (cb == null) return; // nothing to notify
        cb.onProgress(true);

        // Basic input guard
        if (eventId == null || eventId.isEmpty()) {
            cb.onProgress(false);
            cb.onError("Missing eventId");
            return;
        }

        // 1) Validate
        Result<Void> valid = validator.validate(newImage);
        if (!valid.isOk()) {
            cb.onProgress(false);
            cb.onError(valid.error != null ? valid.error.getMessage() : "Invalid image");
            return;
        }

        // 2) Upload new image
        storage.upload(eventId, newImage, uploadRes -> {
            if (!uploadRes.isOk()) {
                cb.onProgress(false);
                cb.onError("Upload failed" + (uploadRes.error != null ? (": " + uploadRes.error.getMessage()) : ""));
                return;
            }

            String newUrl = uploadRes.data;

            // 3) Update Firestore with new URL
            events.updatePosterUrl(eventId, newUrl, updateRes -> {
                if (!updateRes.isOk()) {
                    // Best-effort cleanup: delete the newly uploaded poster to avoid orphaned files
                    storage.deleteByUrl(newUrl, __ -> {
                        cb.onProgress(false);
                        cb.onError("Failed to update database" + (updateRes.error != null ? (": " + updateRes.error.getMessage()) : ""));
                    });
                    return;
                }

                // 4) Delete old poster (best-effort), skip if same URL or empty
                if (oldPosterUrl != null && !oldPosterUrl.isEmpty() && !oldPosterUrl.equals(newUrl)) {
                    storage.deleteByUrl(oldPosterUrl, __ -> {
                        // Regardless of delete result, succeed the flow
                        cb.onProgress(false);
                        cb.onSuccess(newUrl);
                    });
                } else {
                    cb.onProgress(false);
                    cb.onSuccess(newUrl);
                }
            });
        });
    }
}
