package com.example.event_app.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.event_app.R;
import com.example.event_app.activities.organizer.OrganizerEventDetailsActivity;
import com.example.event_app.models.Event;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter used by organizers to view their created events.
 * Displays event poster, dates, attendance stats, and cancellation status.
 */
public class OrganizerEventsAdapter extends RecyclerView.Adapter<OrganizerEventsAdapter.EventViewHolder> {

    private Context context;
    private List<Event> events;

    /**
     * Creates a new OrganizerEventsAdapter.
     *
     * @param context the context used to inflate layouts and start activities
     */
    public OrganizerEventsAdapter(Context context) {
        this.context = context;
        this.events = new ArrayList<>();
    }

    /**
     * Inflates the organizer event item layout and creates a ViewHolder.
     *
     * @param parent   the parent ViewGroup holding the RecyclerView
     * @param viewType the view type (unused since there is one type)
     * @return a new EventViewHolder instance
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_organizer_event, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Binds the event at the given position to the ViewHolder.
     *
     * @param holder   the ViewHolder to populate
     * @param position the index of the event in the list
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event);
    }

    /**
     * Returns the total number of organizer events being displayed.
     *
     * @return number of events in the list
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * Replaces the current list of events with a new list and refreshes the UI.
     *
     * @param events the updated list of events to display
     */
    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder representing a single organizer event card.
     * Displays stats such as waiting count, selected count, and attending count,
     * and handles navigation to the organizer event details screen.
     */
    class EventViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView cardEvent;
        ImageView ivPoster;
        TextView tvEventName, tvDate, tvWaitingCount, tvSelectedCount, tvAttendingCount;
        TextView tvCancelledTag;

        /**
         * Initializes UI components for an organizer event card.
         *
         * @param itemView the inflated layout for one event item
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            cardEvent = itemView.findViewById(R.id.cardEvent);
            ivPoster = itemView.findViewById(R.id.ivEventPoster);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvDate = itemView.findViewById(R.id.tvEventDate);
            tvWaitingCount = itemView.findViewById(R.id.tvWaitingCount);
            tvSelectedCount = itemView.findViewById(R.id.tvSelectedCount);
            tvAttendingCount = itemView.findViewById(R.id.tvAttendingCount);
            tvCancelledTag = itemView.findViewById(R.id.tvCancelledTag);
        }

        /**
         * Binds an Event object to the UI elements inside the ViewHolder.
         * Populates:
         * <ul>
         *     <li>Event name</li>
         *     <li>Event date</li>
         *     <li>Waiting, selected, and attending counts</li>
         *     <li>Poster image</li>
         *     <li>Cancelled status indicator</li>
         * </ul>
         *
         * Also assigns a click listener that opens OrganizerEventDetailsActivity.
         *
         * @param event the event being bound to this ViewHolder
         */
        public void bind(Event event) {
            // Event name
            tvEventName.setText(event.getName());

            // Date
            if (event.getEventDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                tvDate.setText(sdf.format(event.getEventDate()));
            } else {
                tvDate.setText("Date TBA");
            }

            // Stats
            int waitingCount = event.getWaitingList() != null ? event.getWaitingList().size() : 0;
            int attendingCount = event.getSignedUpUsers() != null ? event.getSignedUpUsers().size() : 0;

            tvWaitingCount.setText(String.format(Locale.getDefault(), "%d waiting", waitingCount));
            tvSelectedCount.setText(String.format(Locale.getDefault(), "%d selected", event.getTotalSelected()));
            tvAttendingCount.setText(String.format(Locale.getDefault(), "%d attending", attendingCount));

            // Load poster
            if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                Glide.with(context)
                        .load(event.getPosterUrl())
                        .centerCrop()
                        .into(ivPoster);
            } else {
                ivPoster.setImageResource(R.drawable.ic_event_placeholder);
            }

            // Show cancelled tag if event is cancelled
            if ("cancelled".equals(event.getStatus())) {
                tvCancelledTag.setVisibility(View.VISIBLE);
                // Gray out the card
                cardEvent.setAlpha(0.6f);
            } else {
                tvCancelledTag.setVisibility(View.GONE);
                cardEvent.setAlpha(1.0f);
            }

            // Click listener
            cardEvent.setOnClickListener(v -> {
                Log.d("OrganizerAdapter", "Opening details for event: " + event.getId());
                Intent intent = new Intent(context, OrganizerEventDetailsActivity.class);
                intent.putExtra("EVENT_ID", event.getId());
                context.startActivity(intent);
            });
        }
    }
}