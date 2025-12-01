package com.example.event_app.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.example.event_app.models.Event;
import com.example.event_app.models.User;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ReportExporter - Generates and exports platform usage reports as CSV files.
 *
 * <p>This utility supports:</p>
 * <ul>
 *     <li>Exporting user and event statistics</li>
 *     <li>Highlighting events with high cancellation rates</li>
 *     <li>Sharing generated CSV files via Android's share sheet</li>
 * </ul>
 *
 * <p>User Story: US 03.13.01 — Export platform usage reports</p>
 */
public class ReportExporter {

    private static final String TAG = "ReportExporter";

    /**
     * Generates a full platform analytics report and exports it as a CSV file.
     * <p>
     * The report includes:
     * <ul>
     *     <li>Total number of users and events</li>
     *     <li>Organizer count</li>
     *     <li>Active event count</li>
     *     <li>Events with high cancellation rates</li>
     *     <li>Summary of all events (selected, attending, cancellation rate)</li>
     * </ul>
     * After generating the file, the method automatically opens the Android
     * share sheet so the file can be sent via email, Drive, WhatsApp, etc.
     * </p>
     *
     * @param context Android context used for file creation and sharing
     * @param events  list of all events on the platform
     * @param users   list of all users on the platform
     */
    public static void exportPlatformReport(Context context,
                                            List<Event> events,
                                            List<User> users) {
        try {
            // Create file
            File file = createReportFile(context, "platform_report");

            // Write CSV content
            FileWriter writer = new FileWriter(file);

            // Header
            writer.append("LuckySpot Platform Usage Report\n");
            writer.append("Generated: ").append(getCurrentDateTime()).append("\n\n");

            // Platform Statistics
            writer.append("=== PLATFORM STATISTICS ===\n");
            writer.append("Total Users,").append(String.valueOf(users.size())).append("\n");
            writer.append("Total Events,").append(String.valueOf(events.size())).append("\n");

            // Count organizers
            int organizerCount = 0;
            for (User user : users) {
                if (user.isOrganizer()) {
                    organizerCount++;
                }
            }
            writer.append("Total Organizers,").append(String.valueOf(organizerCount)).append("\n");

            // Count active events
            int activeCount = 0;
            for (Event event : events) {
                if ("active".equals(event.getStatus())) {
                    activeCount++;
                }
            }
            writer.append("Active Events,").append(String.valueOf(activeCount)).append("\n\n");

            // Events with high cancellation
            writer.append("=== HIGH CANCELLATION EVENTS ===\n");
            writer.append("Event Name,Cancellation Rate,Total Selected,Total Cancelled\n");

            boolean hasHighCancellation = false;
            for (Event event : events) {
                if (event.hasHighCancellationRate()) {
                    hasHighCancellation = true;
                    writer.append(event.getName()).append(",");
                    writer.append(String.format("%.1f%%", event.getCancellationRate())).append(",");
                    writer.append(String.valueOf(event.getTotalSelected())).append(",");
                    writer.append(String.valueOf(event.getTotalCancelled())).append("\n");
                }
            }

            if (!hasHighCancellation) {
                writer.append("No events with high cancellation rate\n");
            }

            writer.append("\n");

            // All Events Summary
            writer.append("=== ALL EVENTS ===\n");
            writer.append("Event Name,Status,Total Selected,Total Attending,Cancellation Rate\n");

            for (Event event : events) {
                writer.append(event.getName()).append(",");
                writer.append(event.getStatus()).append(",");
                writer.append(String.valueOf(event.getTotalSelected())).append(",");
                writer.append(String.valueOf(event.getTotalAttending())).append(",");
                writer.append(String.format("%.1f%%", event.getCancellationRate())).append("\n");
            }

            writer.flush();
            writer.close();

            Log.d(TAG, "Report created: " + file.getAbsolutePath());

            // Share the file
            shareFile(context, file);

        } catch (IOException e) {
            Log.e(TAG, "Error creating report", e);
        }
    }

    /**
     * Creates a timestamped CSV report file in the app's external files directory.
     *
     * @param context the calling context, used to access storage directories
     * @param prefix  filename prefix (e.g., "platform_report")
     * @return a newly created File object pointing to the CSV report
     * @throws IOException if the file cannot be created
     */
    private static File createReportFile(Context context, String prefix) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String fileName = prefix + "_" + timestamp + ".csv";

        File outputDir = context.getExternalFilesDir(null);
        return new File(outputDir, fileName);
    }

    /**
     * Returns the current system date and time formatted as:
     * <pre>yyyy-MM-dd HH:mm:ss</pre>
     *
     * Used for report headers.
     *
     * @return formatted date-time string
     */
    private static String getCurrentDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
    }

    /**
     * Shares the generated CSV file using Android's share sheet.
     * <p>
     * The method uses a FileProvider to securely expose the file URI to
     * external applications, and grants temporary read permission.
     * </p>
     *
     * @param context context used to launch the share intent
     * @param file    the CSV file to be shared
     */
    private static void shareFile(Context context, File file) {
        Uri fileUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                file
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(Intent.createChooser(shareIntent, "Share Report"));
    }
}
