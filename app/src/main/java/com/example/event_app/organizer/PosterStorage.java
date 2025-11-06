package com.example.event_app.organizer;

import android.net.Uri;

public interface PosterStorage {
    interface Callback<T> { void onComplete(Result<T> result); }

    /** Uploads file and returns the download URL in Callback. */
    void upload(String eventId, Uri file, Callback<String> cb);

    /** Deletes a file by its public URL. */
    void deleteByUrl(String fileUrl, Callback<Void> cb);
}
