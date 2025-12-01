package com.example.event_app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButton;

/**
 * AccessibilityHelper - Manages app-wide accessibility settings
 *
 * Features:
 * - Large text mode (increases all text sizes by 30%)
 * - High contrast mode (enables dark theme)
 * - Larger touch targets (increases button sizes by 50%)
 *
 * User Story: As an entrant, I want accessibility options (e.g., large text, high contrast)
 *
 * Usage:
 * 1. In SettingsActivity: Toggle switches save preferences
 * 2. In every Activity onCreate(): Call applyAccessibilitySettings()
 *
 */
public class AccessibilityHelper {

    private static final String PREFS_NAME = "accessibility_prefs";
    private static final String KEY_LARGE_TEXT = "large_text_enabled";
    private static final String KEY_HIGH_CONTRAST = "high_contrast_enabled";
    private static final String KEY_LARGE_BUTTONS = "large_buttons_enabled";

    // Multipliers for accessibility features
    private static final float TEXT_SIZE_MULTIPLIER = 1.3f;  // 30% larger
    private static final float BUTTON_PADDING_MULTIPLIER = 1.5f;  // 50% more padding
    private static final int MIN_TOUCH_TARGET_DP = 48;  // Android accessibility guidelines

    private final Context context;
    private final SharedPreferences prefs;

    /**
     * Creates a new AccessibilityHelper instance.
     * Initializes SharedPreferences used to store user-selected accessibility settings.
     *
     * @param context Application or Activity context used to read resources and preferences
     */
    public AccessibilityHelper(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ========================================================================
    // GETTERS & SETTERS
    // ========================================================================

    /**
     * Returns whether large-text mode is enabled.
     *
     * @return true if large text mode is active, false otherwise
     */
    public boolean isLargeTextEnabled() {
        return prefs.getBoolean(KEY_LARGE_TEXT, false);
    }

    /**
     * Enables or disables large-text mode and saves the preference.
     *
     * @param enabled true = enlarge all text views by the configured multiplier
     */
    public void setLargeTextEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LARGE_TEXT, enabled).apply();
    }

    /**
     * Returns whether high-contrast (dark mode) is enabled.
     *
     * @return true if high-contrast mode is enabled
     */
    public boolean isHighContrastEnabled() {
        return prefs.getBoolean(KEY_HIGH_CONTRAST, false);
    }

    /**
     * Enables or disables high-contrast mode.
     * Immediately applies theme changes via AppCompatDelegate.
     *
     * @param enabled true = enable high-contrast mode (dark theme)
     */
    public void setHighContrastEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_HIGH_CONTRAST, enabled).apply();
        applyHighContrast();
    }

    /**
     * Returns whether larger touch-targets mode is enabled.
     *
     * @return true if larger button padding and minimum size are applied
     */
    public boolean isLargeButtonsEnabled() {
        return prefs.getBoolean(KEY_LARGE_BUTTONS, false);
    }

    /**
     * Enables or disables larger button touch targets.
     *
     * @param enabled true = increase padding/minimum touch size on MaterialButtons
     */
    public void setLargeButtonsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LARGE_BUTTONS, enabled).apply();
    }

    // ========================================================================
    // MAIN APPLY METHOD - Call this in every Activity
    // ========================================================================

    /**
     * Applies all accessibility settings to the given activity.
     * Must be called *after* setContentView() in every Activity.
     *
     * Behavior:
     * <ul>
     *   <li>Applies high-contrast mode (dark theme) instantly</li>
     *   <li>Recursively updates all TextViews & MaterialButtons if large-text
     *       or large-buttons mode is enabled</li>
     * </ul>
     *
     * @param activity Activity whose entire view tree should update accessibility settings
     */
    public void applyAccessibilitySettings(AppCompatActivity activity) {
        // Apply high contrast if enabled
        if (isHighContrastEnabled()) {
            applyHighContrast();
        }

        // Apply text/button changes if enabled
        if (isLargeTextEnabled() || isLargeButtonsEnabled()) {
            View rootView = activity.findViewById(android.R.id.content);
            if (rootView != null) {
                applyToAllViews(rootView);
            }
        }
    }

    // ========================================================================
    // HIGH CONTRAST MODE
    // ========================================================================

    /**
     * Applies the high-contrast theme using AppCompatDelegate.
     * <ul>
     *   <li>High contrast ON → force MODE_NIGHT_YES</li>
     *   <li>High contrast OFF → follow system theme</li>
     * </ul>
     */
    private void applyHighContrast() {
        if (isHighContrastEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    /**
     * Returns an appropriate text color based on high-contrast mode.
     *
     * @return White text when high contrast is enabled, black otherwise
     */
    public int getHighContrastTextColor() {
        return isHighContrastEnabled() ? Color.WHITE : Color.BLACK;
    }

    /**
     * Returns an appropriate background color based on high-contrast mode.
     *
     * @return Black background when high contrast is enabled, white otherwise
     */
    public int getHighContrastBackgroundColor() {
        return isHighContrastEnabled() ? Color.BLACK : Color.WHITE;
    }

    // ========================================================================
    // RECURSIVE VIEW PROCESSING
    // ========================================================================

    /**
     * Recursively applies accessibility rules to the given view and all children.
     *
     * Behavior:
     * <ul>
     *   <li>TextView → enlarged text (if enabled)</li>
     *   <li>MaterialButton → enlarged padding & min size (if enabled)</li>
     *   <li>ViewGroup → recursively processes children</li>
     * </ul>
     *
     * @param view The root view to apply accessibility updates to
     */
    private void applyToAllViews(View view) {
        if (view == null) return;

        // Apply to specific view types
        if (view instanceof TextView) {
            applyToTextView((TextView) view);
        }

        if (view instanceof MaterialButton) {
            applyToButton((MaterialButton) view);
        }

        // Recursively process child views
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                applyToAllViews(child);
            }
        }
    }

    // ========================================================================
    // LARGE TEXT MODE
    // ========================================================================

    /**
     * Enlarges a TextView’s text size according to TEXT_SIZE_MULTIPLIER.
     * Only applied when large-text mode is active.
     *
     * @param textView TextView whose text size should be increased
     */
    private void applyToTextView(TextView textView) {
        if (!isLargeTextEnabled()) return;

        try {
            // Get current text size in SP
            float currentSizePx = textView.getTextSize();
            float currentSizeSp = currentSizePx / context.getResources().getDisplayMetrics().scaledDensity;

            // Apply multiplier
            float newSizeSp = currentSizeSp * TEXT_SIZE_MULTIPLIER;

            // Set new size
            textView.setTextSize(newSizeSp);

        } catch (Exception e) {
            // Silently fail - don't break the app if text size adjustment fails
        }
    }

    // ========================================================================
    // LARGER TOUCH TARGETS MODE
    // ========================================================================

    /**
     * Applies larger touch targets to a MaterialButton.
     *
     * Behavior:
     * <ul>
     *   <li>Increases all paddings using BUTTON_PADDING_MULTIPLIER</li>
     *   <li>Ensures min size is at least 48dp (Android accessibility guideline)</li>
     * </ul>
     *
     * @param button MaterialButton to enlarge
     */
    private void applyToButton(MaterialButton button) {
        if (!isLargeButtonsEnabled()) return;

        try {
            // Increase padding
            int currentPaddingTop = button.getPaddingTop();
            int currentPaddingBottom = button.getPaddingBottom();
            int currentPaddingStart = button.getPaddingStart();
            int currentPaddingEnd = button.getPaddingEnd();

            int newPaddingTop = (int) (currentPaddingTop * BUTTON_PADDING_MULTIPLIER);
            int newPaddingBottom = (int) (currentPaddingBottom * BUTTON_PADDING_MULTIPLIER);
            int newPaddingStart = (int) (currentPaddingStart * BUTTON_PADDING_MULTIPLIER);
            int newPaddingEnd = (int) (currentPaddingEnd * BUTTON_PADDING_MULTIPLIER);

            button.setPadding(newPaddingStart, newPaddingTop, newPaddingEnd, newPaddingBottom);

            // Ensure minimum touch target size (48dp)
            int minTouchTargetPx = dpToPx(MIN_TOUCH_TARGET_DP);
            button.setMinHeight(minTouchTargetPx);
            button.setMinWidth(minTouchTargetPx);

        } catch (Exception e) {
            // Silently fail
        }
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Converts a DP value to pixels based on screen density.
     *
     * @param dp Value in density-independent pixels
     * @return Equivalent pixel value
     */
    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Converts pixel measurement into DP units.
     *
     * @param px Pixel value
     * @return Equivalent DP value
     */
    private int pxToDp(int px) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(px / density);
    }

    /**
     * Clears all saved accessibility settings.
     * Also resets theme mode back to system default.
     *
     * Intended for testing or debug tools.
     */
    public void clearAllSettings() {
        prefs.edit().clear().apply();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    /**
     * Builds a readable multi-line summary of all active accessibility settings.
     *
     * @return Summary string (Large Text, High Contrast, Large Buttons)
     */
    public String getAccessibilitySettingsSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Accessibility Settings:\n");
        summary.append("- Large Text: ").append(isLargeTextEnabled() ? "ON" : "OFF").append("\n");
        summary.append("- High Contrast: ").append(isHighContrastEnabled() ? "ON" : "OFF").append("\n");
        summary.append("- Larger Buttons: ").append(isLargeButtonsEnabled() ? "ON" : "OFF");
        return summary.toString();
    }
}