package com.example.event_app.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.event_app.R;
import com.example.event_app.activities.entrant.EventDetailsActivity;
import com.example.event_app.models.Event;
import com.example.event_app.utils.Navigator;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * EventAdapter
 *
 * RecyclerView adapter responsible for displaying event cards in the
 * "Browse Events" screen. Each card shows:
 * <ul>
 *     <li>Poster</li>
 *     <li>Event name</li>
 *     <li>Organizer name</li>
 *     <li>Date</li>
 *     <li>Capacity (or unlimited)</li>
 *     <li>Waiting list count</li>
 * </ul>
 *
 * Clicking an event card navigates to {@link EventDetailsActivity}
 * using {@link Navigator#EXTRA_EVENT_ID}.
 *
 * Used by: HomeFragment, EventsFragment
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private Context context;
    private List<Event> events;

    /**
     * Constructor initializes adapter with empty list.
     *
     * @param context calling context for layout inflation and navigation
     */
    public EventAdapter(Context context) {
        this.context = context;
        this.events = new ArrayList<>();
    }

    /**
     * Inflates the layout for each event card.
     *
     * @param parent the parent ViewGroup
     * @param viewType unused view type parameter
     * @return a new {@link EventViewHolder} instance
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Binds event data to the ViewHolder.
     *
     * @param holder ViewHolder instance
     * @param position position in data list
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event);
    }

    /**
     * @return how many events are displayed
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * Updates the list of events and refreshes UI.
     *
     * @param events new list of Event objects
     */
    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    /**
     * Clears event list (used when applying new filters).
     */
    public void clearEvents() {
        this.events.clear();
        notifyDataSetChanged();
    }

    /**
     * ViewHolder for each individual event card.
     * Handles:
     * <ul>
     *     <li>Poster display</li>
     *     <li>Name, organizer, date</li>
     *     <li>Capacity formatting</li>
     *     <li>Waiting list count</li>
     *     <li>Click to open details</li>
     * </ul>
     */
    class EventViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView cardEvent;
        ImageView ivPoster;
        TextView tvEventName, tvOrganizer, tvDate, tvCapacity, tvWaitingListCount;

        /**
         * Initializes UI components for the event card.
         *
         * @param itemView the inflated row layout
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            cardEvent = itemView.findViewById(R.id.cardEvent);
            ivPoster = itemView.findViewById(R.id.ivEventPoster);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvOrganizer = itemView.findViewById(R.id.tvOrganizer);
            tvDate = itemView.findViewById(R.id.tvEventDate);
            tvCapacity = itemView.findViewById(R.id.tvCapacity);
            tvWaitingListCount = itemView.findViewById(R.id.tvWaitingListCount);
        }

        /**
         * Binds Event model fields to the UI widgets inside the card.
         *
         * @param event Event instance containing display data
         */
        public void bind(Event event) {
            // Event name
            tvEventName.setText(event.getName());

            // Organizer
            tvOrganizer.setText(event.getOrganizerName() != null ?
                    event.getOrganizerName() : "Event Organizer");

            // Date
            if (event.getEventDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                tvDate.setText(sdf.format(event.getEventDate()));
            } else {
                tvDate.setText("Date TBA");
            }

            // Capacity
            if (event.getCapacity() != null) {
                tvCapacity.setText(String.format(Locale.getDefault(),
                        "%d spots", event.getCapacity()));
            } else {
                tvCapacity.setText("Unlimited");
            }

            // Waiting list count
            int waitingCount = event.getWaitingList() != null ?
                    event.getWaitingList().size() : 0;
            tvWaitingListCount.setText(String.format(Locale.getDefault(),
                    "%d waiting", waitingCount));

            // Load poster
            if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                Glide.with(context)
                        .load(event.getPosterUrl())
                        .centerCrop()
                        .into(ivPoster);
            } else {
                ivPoster.setImageResource(R.drawable.ic_event_placeholder);
            }

            // Click listener - navigate to event details
            cardEvent.setOnClickListener(v -> {
                Intent intent = new Intent(context, EventDetailsActivity.class);
                intent.putExtra(Navigator.EXTRA_EVENT_ID, event.getId());
                context.startActivity(intent);
            });
        }
    }
}