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
 * ReportExporter - Utility for exporting platform reports
 * US 03.13.01: Export platform usage reports
 */
public class ReportExporter {

    private static final String TAG = "ReportExporter";

    /**
     * Export platform statistics to CSV file
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
     * Create a file for the report
     */
    private static File createReportFile(Context context, String prefix) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String fileName = prefix + "_" + timestamp + ".csv";

        File outputDir = context.getExternalFilesDir(null);
        return new File(outputDir, fileName);
    }

    /**
     * Get current date and time as string
     */
    private static String getCurrentDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
    }

    /**
     * Share the report file
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
