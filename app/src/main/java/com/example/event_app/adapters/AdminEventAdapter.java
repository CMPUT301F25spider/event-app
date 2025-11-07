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
 * AdminEventAdapter - Displays events for admin with detailed info
 * Shows: name, organizer, date, status, entrant count
 * Click to view event details
 */
public class AdminEventAdapter extends RecyclerView.Adapter<AdminEventAdapter.AdminEventViewHolder> {

    private List<Event> events;
    private OnEventClickListener listener;

    public AdminEventAdapter() {
        this.events = new ArrayList<>();
    }

    @NonNull
    @Override
    public AdminEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_event, parent, false);
        return new AdminEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminEventViewHolder holder, int position) {
        holder.bind(events.get(position));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * Update the list of events
     */
    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    /**
     * Set click listener
     */
    public void setOnEventClickListener(OnEventClickListener listener) {
        this.listener = listener;
    }

    /**
     * ViewHolder for admin event cards
     */
    class AdminEventViewHolder extends RecyclerView.ViewHolder {

        private TextView tvName, tvDescription, tvStatus;
        private TextView tvOrganizer, tvDate, tvEntrantCount, tvWarning;

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
     * Interface for click events
     */
    public interface OnEventClickListener {
        void onEventClick(Event event);
    }
}

//test
