package com.example.event_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.models.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * AdminEventAdapter
 *
 * RecyclerView adapter for displaying events in the admin dashboard.
 * Shows:
 * <ul>
 *   <li>Event name and description</li>
 *   <li>Organizer name and event date</li>
 *   <li>Status with color coding</li>
 *   <li>Total entrant count and high cancellation warnings</li>
 * </ul>
 *
 * Used by admin screens to inspect event quality and drill into details.
 */
public class AdminEventAdapter extends RecyclerView.Adapter<AdminEventAdapter.AdminEventViewHolder> {

    private List<Event> events;
    private OnEventClickListener listener;

    /**
     * Creates a new adapter with an empty backing list.
     * Call {@link #setEvents(List)} to populate data.
     */
    public AdminEventAdapter() {
        this.events = new ArrayList<>();
    }

    /**
     * Inflates an admin event card and wraps it in a ViewHolder.
     *
     * @param parent   the parent RecyclerView
     * @param viewType the view type for this item (unused, single view type)
     * @return a new {@link AdminEventViewHolder} instance
     */
    @NonNull
    @Override
    public AdminEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_event, parent, false);
        return new AdminEventViewHolder(view);
    }

    /**
     * Binds an {@link Event} to a ViewHolder at the given position.
     *
     * @param holder   the ViewHolder to bind
     * @param position position of the item in the adapter
     */
    @Override
    public void onBindViewHolder(@NonNull AdminEventViewHolder holder, int position) {
        holder.bind(events.get(position));
    }

    /**
     * Returns the number of events currently displayed.
     *
     * @return item count for the RecyclerView
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * Replaces the current list of events and refreshes the adapter.
     *
     * @param events list of Event objects to display
     */
    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    /**
     * Registers a callback triggered when an event card is tapped.
     *
     * @param listener handler that responds to event selection
     */
    public void setOnEventClickListener(OnEventClickListener listener) {
        this.listener = listener;
    }

    /**
     * ViewHolder representing a single admin event card.
     * Responsible for:
     * <ul>
     *   <li>Binding event metadata to the UI</li>
     *   <li>Applying status and warning styling</li>
     *   <li>Forwarding click events to {@link OnEventClickListener}</li>
     * </ul>
     */
    class AdminEventViewHolder extends RecyclerView.ViewHolder {

        private TextView tvName, tvDescription, tvStatus;
        private TextView tvOrganizer, tvDate, tvEntrantCount, tvWarning;

        /**
         * Creates a ViewHolder for the admin event card layout.
         *
         * @param itemView the inflated card view
         */
        public AdminEventViewHolder(@NonNull View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tvAdminEventName);
            tvDescription = itemView.findViewById(R.id.tvAdminEventDescription);
            tvStatus = itemView.findViewById(R.id.tvAdminEventStatus);
            tvOrganizer = itemView.findViewById(R.id.tvAdminOrganizer);
            tvDate = itemView.findViewById(R.id.tvAdminEventDate);
            tvEntrantCount = itemView.findViewById(R.id.tvAdminEntrantCount);
            tvWarning = itemView.findViewById(R.id.tvAdminWarning);
        }

        /**
         * Populates this card with the given event's data and applies
         * admin-specific formatting.
         *
         * Responsibilities:
         * <ul>
         *   <li>Set event name and optional description</li>
         *   <li>Show organizer name or "Unknown"</li>
         *   <li>Format and show event date, or "No date"</li>
         *   <li>Render status text with a colored background</li>
         *   <li>Display total entrant count</li>
         *   <li>Highlight high cancellation events with a warning badge</li>
         *   <li>Attach item click handler to notify the adapter listener</li>
         * </ul>
         *
         * @param event the Event to bind to this view
         */
        public void bind(Event event) {
            // Event name
            tvName.setText(event.getName());

            // Description
            String description = event.getDescription();
            if (description != null && !description.isEmpty()) {
                tvDescription.setText(description);
                tvDescription.setVisibility(View.VISIBLE);
            } else {
                tvDescription.setVisibility(View.GONE);
            }

            // Status
            String status = event.getStatus() != null ? event.getStatus().toUpperCase() : "ACTIVE";
            tvStatus.setText(status);

            // Status color
            int statusColor;
            switch (status.toLowerCase()) {
                case "active":
                    statusColor = android.R.color.holo_green_dark;
                    break;
                case "inactive":
                    statusColor = android.R.color.darker_gray;
                    break;
                case "completed":
                    statusColor = android.R.color.holo_blue_dark;
                    break;
                default:
                    statusColor = android.R.color.holo_orange_dark;
            }
            tvStatus.setBackgroundColor(itemView.getContext().getColor(statusColor));

            // Organizer
            String organizer = event.getOrganizerName();
            if (organizer != null && !organizer.isEmpty()) {
                tvOrganizer.setText(organizer);
            } else {
                tvOrganizer.setText("Unknown");
            }

            // Date
            Date eventDate = event.getEventDate();
            if (eventDate != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                tvDate.setText(dateFormat.format(eventDate));
            } else {
                tvDate.setText("No date");
            }

            // Entrant count
            tvEntrantCount.setText(String.valueOf(event.getEntrantCount()));

            // Warning for high cancellation
            if (event.hasHighCancellationRate()) {
                tvWarning.setVisibility(View.VISIBLE);
                tvWarning.setText(String.format("⚠️ High cancellation (%.0f%%)",
                        event.getCancellationRate()));
                tvName.setTextColor(itemView.getContext().getColor(android.R.color.holo_red_dark));
            } else {
                tvWarning.setVisibility(View.GONE);
                tvName.setTextColor(itemView.getContext().getColor(android.R.color.black));
            }

            // Click listener - open event details
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventClick(event);
                }
            });
        }
    }

    /**
     * Listener interface for admin event item clicks.
     * Implemented by screens that need to open a detailed view
     * when an event card is selected.
     */
    public interface OnEventClickListener {
        void onEventClick(Event event);
    }
}
