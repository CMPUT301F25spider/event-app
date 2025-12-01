package com.example.event_app.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

/**
 * PermissionManager - Utility class for handling runtime permissions.
 *
 * Features:
 * <ul>
 *     <li>Check if specific permissions (e.g., camera) are granted</li>
 *     <li>Centralized permission helper used across the app</li>
 * </ul>
 *
 * Used by features requiring runtime permissions such as QR scanning.
 */
public class PermissionManager {

    /**
     * Check whether the camera permission has been granted.
     *
     * @param activity the activity context used to check permission status
     * @return true if the camera permission is granted, false otherwise
     */
    public static boolean isCameraPermissionGranted(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
}
