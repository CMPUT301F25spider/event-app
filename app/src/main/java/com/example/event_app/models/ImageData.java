package com.example.event_app.models;

/**
 * Represents metadata for an uploaded image stored in Firebase Storage.
 * Image records are used for event posters, profile pictures, and other
 * user-generated visual content throughout the LuckySpot system.
 *
 * <p>Each entry tracks the image URL, uploader, usage context, and the
 * associated storage path for file management or cleanup operations.</p>
 */
public class ImageData {
    private String imageId;
    private String imageUrl;
    private String uploadedBy;      // userId who uploaded
    private String associatedWith;  // eventId or userId
    private String type;            // "event_poster" or "profile_picture"
    private long uploadedAt;
    private String storagePath;     // Firebase Storage path (e.g., "event_posters/image.jpg")

    // Empty constructor for Firebase
    public ImageData() {
    }

    /**
     * Creates a new image metadata entry, typically when an image is uploaded
     * to Firebase Storage. The system automatically records the upload time.
     *
     * @param imageId     unique identifier for the image metadata entry
     * @param imageUrl    public or secured URL of the uploaded image
     * @param uploadedBy  user ID of the uploader
     * @param type        category describing the image purpose
     */
    public ImageData(String imageId, String imageUrl, String uploadedBy, String type) {
        this.imageId = imageId;
        this.imageUrl = imageUrl;
        this.uploadedBy = uploadedBy;
        this.type = type;
        this.uploadedAt = System.currentTimeMillis();
    }

    // Getters
    public String getImageId() { return imageId; }
    public String getImageUrl() { return imageUrl; }
    public String getUploadedBy() { return uploadedBy; }
    public String getAssociatedWith() { return associatedWith; }
    public String getType() { return type; }
    public long getUploadedAt() { return uploadedAt; }
    public String getStoragePath() { return storagePath; }

    // Setters
    public void setImageId(String imageId) { this.imageId = imageId; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public void setAssociatedWith(String associatedWith) { this.associatedWith = associatedWith; }
    public void setType(String type) { this.type = type; }
    public void setUploadedAt(long uploadedAt) { this.uploadedAt = uploadedAt; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    /**
     * Convenience helper for retrieving the uploader identity. Provided for
     * clarity and future compatibility with systems that may rename fields.
     *
     * @return the user ID of the image uploader
     */
    public String getUploaderId() {
        return uploadedBy;
    }
}