package com.example.event_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.models.NotificationLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying a searchable list of notification logs inside a RecyclerView.
 * Supports filtering by sender, recipient, or title, and provides click callbacks.
 */
public class NotificationLogAdapter extends RecyclerView.Adapter<NotificationLogAdapter.LogViewHolder> {

    private List<NotificationLog> logs;
    private List<NotificationLog> logsFiltered;
    private OnLogClickListener listener;

    /**
     * Creates a new adapter with empty log lists.
     * Initializes both main and filtered lists.
     */
    public NotificationLogAdapter() {
        this.logs = new ArrayList<>();
        this.logsFiltered = new ArrayList<>();
    }

    /**
     * Inflates the layout for a single notification log entry.
     *
     * @param parent   the parent ViewGroup hosting the RecyclerView
     * @param viewType the type of view (unused, single type)
     * @return a new LogViewHolder instance
     */
    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_log, parent, false);
        return new LogViewHolder(view);
    }

    /**
     * Binds the notification log at the specified position to the ViewHolder.
     *
     * @param holder   the ViewHolder responsible for binding UI elements
     * @param position index of the log entry to display
     */
    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        NotificationLog log = logsFiltered.get(position);
        holder.bind(log, listener);
    }

    /**
     * Returns the number of logs currently displayed (after filtering).
     *
     * @return filtered log count
     */
    @Override
    public int getItemCount() {
        return logsFiltered.size();
    }

    /**
     * Replaces the current log list with a new one.
     * Also resets the filtered list and refreshes the RecyclerView.
     *
     * @param logs the complete list of notification logs
     */
    public void setLogs(List<NotificationLog> logs) {
        this.logs = logs;
        this.logsFiltered = new ArrayList<>(logs);
        notifyDataSetChanged();
    }

    /**
     * Filters the visible logs based on a search query.
     * Matches sender name, recipient name, or notification title.
     *
     * @param query text used to filter log entries (case-insensitive)
     */
    public void filter(String query) {
        logsFiltered.clear();
        if (query.isEmpty()) {
            logsFiltered.addAll(logs);
        } else {
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            for (NotificationLog log : logs) {
                if (log.getSenderName().toLowerCase().contains(lowerQuery) ||
                        log.getRecipientName().toLowerCase().contains(lowerQuery) ||
                        log.getTitle().toLowerCase().contains(lowerQuery)) {
                    logsFiltered.add(log);
                }
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Sets a callback listener for log item clicks.
     *
     * @param listener the listener invoked when a log entry is tapped
     */
    public void setOnLogClickListener(OnLogClickListener listener) {
        this.listener = listener;
    }

    /**
     * ViewHolder representing a single notification log entry.
     * Displays sender, recipient, title, type, and timestamp.
     */
    class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvSenderName, tvRecipientName, tvTitle, tvType, tvTimestamp;

        /**
         * Initializes all UI components for rendering a log entry.
         *
         * @param itemView the root view of the list item layout
         */
        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvRecipientName = itemView.findViewById(R.id.tvRecipientName);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvType = itemView.findViewById(R.id.tvType);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }

        /**
         * Binds a NotificationLog object to the UI by:
         * <ul>
         *     <li>Displaying sender and recipient names</li>
         *     <li>Showing notification title and type</li>
         *     <li>Formatting the timestamp using "MMM dd, HH:mm"</li>
         *     <li>Setting click listener to notify adapter's callback</li>
         * </ul>
         *
         * @param log      the notification log entry being displayed
         * @param listener callback invoked when the item is clicked
         */
        public void bind(NotificationLog log, OnLogClickListener listener) {
            tvSenderName.setText("From: " + log.getSenderName());
            tvRecipientName.setText("To: " + log.getRecipientName());
            tvTitle.setText(log.getTitle());
            tvType.setText(log.getNotificationType().replace("_", " ").toUpperCase());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            if (log.getTimestamp() != null) {
                tvTimestamp.setText(sdf.format(log.getTimestamp()));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLogClick(log);
                }
            });
        }
    }

    /**
     * Listener interface for handling clicks on notification log entries.
     */
    public interface OnLogClickListener {

        /**
         * Called when a log entry is selected.
         *
         * @param log the log entry that was clicked
         */
        void onLogClick(NotificationLog log);
    }
}
