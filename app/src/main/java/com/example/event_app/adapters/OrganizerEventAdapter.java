package com.example.event_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.models.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * OrganizerEventAdapter
 *
 * Adapter class for displaying organizer-created events in a RecyclerView.
 * Each item shows the event name, details, and attendee count.
 */
public class OrganizerEventAdapter extends RecyclerView.Adapter<OrganizerEventAdapter.EventViewHolder> {

    /** List of events to display. */
    private final List<Event> events = new ArrayList<>();

    /** Callback for item clicks. */
    private final OnEventClickListener listener;

    /** Track the last selected event. */
    private Event selectedEvent;

    /**
     * Interface for handling event item clicks.
     */
    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    /**
     * Constructs the adapter with a click listener.
     *
     * @param listener callback invoked when an event item is clicked
     */
    public OrganizerEventAdapter(OnEventClickListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the adapter with a new list of events.
     *
     * @param newEvents list of events to display
     */
    public void submitList(List<Event> newEvents) {
        events.clear();
        events.addAll(newEvents);
        notifyDataSetChanged();
    }

    /** Returns the last selected event (or null if none selected). */
    public Event getSelectedEvent() {
        return selectedEvent;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_organizer_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        holder.bind(events.get(position));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder class for event items.
     */
    class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtEventName;
        private final TextView txtEventDetails;
        private final TextView txtAttendeesCount;
        private final ImageView imgThumbnail;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            txtEventName = itemView.findViewById(R.id.txt_event_name);
            txtEventDetails = itemView.findViewById(R.id.txt_event_details);
            txtAttendeesCount = itemView.findViewById(R.id.txt_attendees_count);
            imgThumbnail = itemView.findViewById(R.id.img_event_thumbnail);
        }

        /**
         * Binds an Event object to the UI elements.
         *
         * @param event the event to display
         */
        void bind(Event event) {
            txtEventName.setText(event.getName());
            txtEventDetails.setText(event.getDescription());
            txtAttendeesCount.setText("Entrants: " + event.getEntrantCount());

            // TODO: Load thumbnail with Glide/Picasso if you have a posterUrl
            itemView.setOnClickListener(v -> {
                selectedEvent = event; // remember which event was clicked
                listener.onEventClick(event);
            });
        }
    }
}