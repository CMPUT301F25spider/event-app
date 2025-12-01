package com.example.event_app.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.event_app.R;
import com.example.event_app.activities.entrant.EventDetailsActivity;
import com.example.event_app.models.Event;
import com.example.event_app.utils.Navigator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * FullEventAdapter - Displays events in a vertical list with full details.
 *
 * Used in:
 * - EventsFragment
 * - BrowseEventsActivity
 *
 * Provides:
 * - Poster image
 * - Event name
 * - Organizer
 * - Date & Time
 * - Location
 * - Waiting list count
 * - Capacity
 *
 * Supports filtering via search.
 */
public class FullEventAdapter extends RecyclerView.Adapter<FullEventAdapter.EventViewHolder> {

    private Context context;
    private List<Event> events;
    private List<Event> eventsFiltered; // For search/filter

    /**
     * Constructor for FullEventAdapter.
     *
     * @param context the context used for inflating layouts and navigation
     */
    public FullEventAdapter(Context context) {
        this.context = context;
        this.events = new ArrayList<>();
        this.eventsFiltered = new ArrayList<>();
    }

    /**
     * Inflates the full event card layout.
     *
     * @param parent the parent view group
     * @param viewType type of view (unused)
     * @return an {@link EventViewHolder} for the event row
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event_full, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Binds event data to the ViewHolder at a given position.
     *
     * @param holder the ViewHolder to bind to
     * @param position index of the event in the filtered list
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventsFiltered.get(position);
        holder.bind(event);
    }

    /**
     * Returns the number of filtered events.
     *
     * @return count of displayed events
     */
    @Override
    public int getItemCount() {
        return eventsFiltered.size();
    }

    /**
     * Updates the adapter with a new list of events.
     *
     * @param events the full list of events to display
     */
    public void setEvents(List<Event> events) {
        this.events = new ArrayList<>(events);
        this.eventsFiltered = new ArrayList<>(events);
        notifyDataSetChanged();
    }

    /**
     * Filters events based on a search query.
     *
     * @param query the text used to filter by name, description, organizer, or location
     */
    public void filter(String query) {
        eventsFiltered.clear();

        if (query == null || query.trim().isEmpty()) {
            eventsFiltered.addAll(events);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (Event event : events) {
                // Search by name, description, organizer, or location
                if ((event.getName() != null && event.getName().toLowerCase().contains(lowerQuery)) ||
                        (event.getDescription() != null && event.getDescription().toLowerCase().contains(lowerQuery)) ||
                        (event.getOrganizerName() != null && event.getOrganizerName().toLowerCase().contains(lowerQuery)) ||
                        (event.getLocation() != null && event.getLocation().toLowerCase().contains(lowerQuery))) {
                    eventsFiltered.add(event);
                }
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Gets the number of events currently visible (after filtering).
     *
     * @return count of filtered events
     */
    public int getFilteredCount() {
        return eventsFiltered.size();
    }

    /**
     * Clears all event lists.
     */
    public void clearEvents() {
        this.events.clear();
        this.eventsFiltered.clear();
        notifyDataSetChanged();
    }

    /**
     * ViewHolder class for full event cards.
     */
    class EventViewHolder extends RecyclerView.ViewHolder {

        ImageView ivPoster;
        TextView tvEventName, tvOrganizer, tvDate, tvLocation, tvWaitingCount, tvCapacity;
        LinearLayout layoutLocation, layoutCapacity;

        /**
         * Initializes UI components for the event card.
         *
         * @param itemView the inflated row layout
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);

            ivPoster = itemView.findViewById(R.id.ivEventPoster);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvOrganizer = itemView.findViewById(R.id.tvOrganizer);
            tvDate = itemView.findViewById(R.id.tvEventDate);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvWaitingCount = itemView.findViewById(R.id.tvWaitingCount);
            tvCapacity = itemView.findViewById(R.id.tvCapacity);
            layoutLocation = itemView.findViewById(R.id.layoutLocation);
            layoutCapacity = itemView.findViewById(R.id.layoutCapacity);

            // Click listener - navigate to EventDetailsActivity
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Event event = eventsFiltered.get(position);
                    Intent intent = new Intent(context, EventDetailsActivity.class);
                    intent.putExtra(Navigator.EXTRA_EVENT_ID, event.getId() != null ? event.getId() : event.getEventId());
                    context.startActivity(intent);
                }
            });
        }

        /**
         * Binds event data to the ViewHolder UI.
         *
         * @param event the Event object to display
         */
        public void bind(Event event) {
            // Event name
            tvEventName.setText(event.getName() != null ? event.getName() : "Untitled Event");

            // Organizer
            tvOrganizer.setText(event.getOrganizerName() != null ?
                    event.getOrganizerName() : "Event Organizer");

            // Date
            if (event.getEventDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
                tvDate.setText(sdf.format(event.getEventDate()));
            } else if (event.getDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
                tvDate.setText(sdf.format(event.getDate()));
            } else {
                tvDate.setText("Date TBA");
            }

            // Location
            if (event.getLocation() != null && !event.getLocation().isEmpty()) {
                layoutLocation.setVisibility(View.VISIBLE);
                tvLocation.setText(event.getLocation());
            } else {
                layoutLocation.setVisibility(View.GONE);
            }

            // Waiting list count
            int waitingCount = event.getWaitingList() != null ? event.getWaitingList().size() : 0;
            tvWaitingCount.setText(waitingCount + " waiting");

            // Capacity
            if (event.getCapacity() != null) {
                layoutCapacity.setVisibility(View.VISIBLE);
                tvCapacity.setText(event.getCapacity() + " spots");
            } else {
                layoutCapacity.setVisibility(View.GONE);
            }

            // Load poster image
            if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                Glide.with(context)
                        .load(event.getPosterUrl())
                        .centerCrop()
                        .placeholder(R.color.gray_light)
                        .into(ivPoster);
            } else {
                // Set a default placeholder if no poster
                ivPoster.setImageResource(R.color.gray_light);
            }
        }
    }
}
