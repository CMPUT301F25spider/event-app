package com.example.event_app.domain;

import android.net.Uri;

import com.example.event_app.data.EventRepository;
import com.example.event_app.data.PosterStorage;

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
        cb.onProgress(true);

        // 1. Validate image
        Result<Void> valid = validator.validate(newImage);
        if (!valid.isOk()) {
            cb.onProgress(false);
            cb.onError(valid.error.getMessage());
            return;
        }

        // 2. Upload new image
        storage.upload(eventId, newImage, uploadRes -> {
            if (!uploadRes.isOk()) {
                cb.onProgress(false);
                cb.onError("Upload failed: " + uploadRes.error.getMessage());
                return;
            }
            String newUrl = uploadRes.data;

            // 3. Update Firestore event poster URL
            events.updatePosterUrl(eventId, newUrl, updateRes -> {
                if (!updateRes.isOk()) {
                    cb.onProgress(false);
                    cb.onError("Failed to update database: " + updateRes.error.getMessage());
                    return;
                }

                // 4. Delete old poster
                if (oldPosterUrl != null && !oldPosterUrl.isEmpty()) {
                    storage.deleteByUrl(oldPosterUrl, deleteRes -> {
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