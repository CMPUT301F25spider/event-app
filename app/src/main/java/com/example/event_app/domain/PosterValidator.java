package com.example.event_app.domain;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

/**
 * Validates that the selected image is JPG/PNG and <= maxBytes.
 */
public class PosterValidator {
    private final ContentResolver cr;
    private final long maxBytes;

    public PosterValidator(ContentResolver cr, long maxBytes) {
        this.cr = cr;
        this.maxBytes = maxBytes;
    }

    /** Returns Result.ok(null) when valid, otherwise Result.err(reason). */
    public Result<Void> validate(Uri uri) {
        String mime = cr.getType(uri);
        if (mime == null) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            if (extension != null) {
                mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            }
        }
        if (!( "image/jpeg".equals(mime) || "image/png".equals(mime) )) {
            return Result.err(new IllegalArgumentException("Only JPG/PNG allowed"));
        }

        long size = getFileSize(uri);
        if (size < 0) return Result.err(new IllegalStateException("Unable to read file size"));
        if (size > maxBytes) return Result.err(new IllegalArgumentException("Max size 5MB"));

        return Result.ok(null);
    }

    private long getFileSize(Uri uri) {
        long size = -1L;
        Cursor c = cr.query(uri, null, null, null, null);
        if (c != null) {
            int idx = c.getColumnIndex(OpenableColumns.SIZE);
            if (idx != -1 && c.moveToFirst()) size = c.getLong(idx);
            c.close();
        }
        return size;
    }
}
