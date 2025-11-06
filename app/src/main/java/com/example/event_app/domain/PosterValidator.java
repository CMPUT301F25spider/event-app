package com.example.event_app.domain;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

/** Validates that the selected image is JPG/PNG and ≤ maxBytes. */
public class PosterValidator {
    private final ContentResolver cr;
    private final long maxBytes; // e.g., 5L * 1024 * 1024

    public PosterValidator(ContentResolver cr, long maxBytes) {
        this.cr = cr;
        this.maxBytes = maxBytes;
    }

    /** Returns Result.ok(null) when valid; otherwise Result.err(message/exception). */
    public Result<Void> validate(Uri uri) {
        if (uri == null) return Result.err("No file selected");

        String mime = resolveMime(uri);
        if (mime == null) return Result.err("Unknown file type");
        if (!("image/jpeg".equals(mime) || "image/png".equals(mime))) {
            return Result.err("Only JPG or PNG allowed");
        }

        long size = getFileSize(uri);
        if (size < 0) return Result.err(new IllegalStateException("Unable to read file size"));
        if (size > maxBytes) return Result.err("Max size is " + (maxBytes / (1024 * 1024)) + "MB");

        return Result.ok(null);
    }

    /** Best-effort MIME from CR or URL extension. */
    public String resolveMime(Uri uri) {
        String mime = cr.getType(uri);
        if (mime == null) {
            String ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            if (ext != null) {
                String guess = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(ext.toLowerCase());
                if (guess != null) mime = guess;
            }
        }
        return mime;
    }

    /** Convenience for naming uploads in Storage. */
    public static String extensionForMime(String mime) {
        return "image/png".equals(mime) ? "png" : "jpg";
    }

    private long getFileSize(Uri uri) {
        long size = -1L;
        Cursor c = cr.query(uri, new String[]{OpenableColumns.SIZE}, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.SIZE);
                if (idx >= 0) size = c.getLong(idx);
            }
            c.close();
        }
        return size;
    }
}
