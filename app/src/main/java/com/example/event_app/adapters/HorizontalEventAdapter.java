package com.example.event_app.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.event_app.R;
import com.example.event_app.activities.entrant.EventDetailsActivity;
import com.example.event_app.models.Event;
import com.example.event_app.utils.FavoritesManager;
import com.example.event_app.utils.Navigator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying events in a horizontally scrolling list.
 * <p>
 * Used on the home screen for sections like "Happening Soon" and
 * "Popular This Week". Each card shows the poster, name, date, and
 * waiting-list count, and lets the user mark events as favorites.
 */
public class HorizontalEventAdapter extends RecyclerView.Adapter<HorizontalEventAdapter.EventViewHolder> {

    private Context context;
    private List<Event> events;
    private FavoritesManager favoritesManager;

    /**
     * Creates a new {@code HorizontalEventAdapter} with an empty event list.
     *
     * @param context the {@link Context} used to inflate layouts and start activities
     */
    public HorizontalEventAdapter(Context context) {
        this.context = context;
        this.events = new ArrayList<>();
        this.favoritesManager = new FavoritesManager();
    }

    /**
     * Inflates the horizontal event item layout and wraps it in a {@link EventViewHolder}.
     *
     * @param parent   the parent {@link ViewGroup} into which the new view will be added
     * @param viewType the view type of the new view (not used, single view type only)
     * @return a new {@link EventViewHolder} holding the inflated item view
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event_horizontal, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Binds the event at the given position to the provided {@link EventViewHolder}.
     *
     * @param holder   the {@link EventViewHolder} that should be updated
     * @param position the position of the item within the adapter's data set
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event);
    }

    /**
     * Returns the number of events currently held by the adapter.
     *
     * @return the size of the event list
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * Replaces the current list of events and refreshes the UI.
     *
     * @param events the new list of {@link Event} objects to display
     */
    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    /**
     * Clears all events from the adapter and notifies that the data set changed.
     */
    public void clearEvents() {
        this.events.clear();
        notifyDataSetChanged();
    }

    /**
     * ViewHolder representing a single horizontal event card.
     * <p>
     * Holds references to the poster image, event name, date, waiting count,
     * and favorite button, and handles click events for the card and the heart icon.
     */
    class EventViewHolder extends RecyclerView.ViewHolder {

        ImageView ivPoster;
        TextView tvEventName, tvDate, tvWaitingCount;
        ImageButton btnFavorite;  // NEW

        private boolean isFavorited = false;

        /**
         * Creates a new {@code EventViewHolder} and finds all view references
         * from the provided item view.
         *
         * @param itemView the inflated layout for a single horizontal event card
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.ivEventPoster);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvDate = itemView.findViewById(R.id.tvEventDate);
            tvWaitingCount = itemView.findViewById(R.id.tvWaitingCount);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);  // NEW

            // Click listener - navigate to EventDetailsActivity
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Event event = events.get(position);
                    Intent intent = new Intent(context, EventDetailsActivity.class);
                    intent.putExtra(Navigator.EXTRA_EVENT_ID, event.getId() != null ? event.getId() : event.getEventId());
                    context.startActivity(intent);
                }
            });
        }

        /**
         * Binds an {@link Event} to this view holder's UI components.
         * <p>
         * Sets the event name, date, waiting-list count, poster image, and initializes
         * the favorite button state for this item.
         *
         * @param event the {@link Event} whose data should be displayed
         */
        public void bind(Event event) {
            // Event name
            tvEventName.setText(event.getName() != null ? event.getName() : "Untitled Event");

            // Date
            if (event.getEventDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                tvDate.setText(sdf.format(event.getEventDate()));
            } else if (event.getDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                tvDate.setText(sdf.format(event.getDate()));
            } else {
                tvDate.setText("Date TBA");
            }

            // Waiting list count
            int waitingCount = event.getWaitingList() != null ? event.getWaitingList().size() : 0;
            tvWaitingCount.setText(waitingCount + " waiting");

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

            // NEW: Setup favorite button
            setupFavoriteButton(event);
        }

        /**
         * Configures the favorite (heart) button for the given event.
         * <p>
         * Checks whether the event is already in the user's favorites,
         * updates the heart icon accordingly, and sets up click handling to
         * add or remove the event from favorites using {@link FavoritesManager}.
         *
         * @param event the {@link Event} whose favorite state is being controlled
         */
        private void setupFavoriteButton(Event event) {
            String eventId = event.getId() != null ? event.getId() : event.getEventId();

            // Check if event is favorited
            favoritesManager.isFavorite(eventId, isFav -> {
                isFavorited = isFav;
                updateFavoriteIcon();
            });

            // Click listener for favorite button
            btnFavorite.setOnClickListener(v -> {
                // Stop click from propagating to card
                v.setClickable(true);

                // Toggle favorite
                if (isFavorited) {
                    // Remove from favorites
                    favoritesManager.removeFavorite(eventId, new FavoritesManager.FavoriteCallback() {
                        @Override
                        public void onSuccess() {
                            isFavorited = false;
                            updateFavoriteIcon();
                            Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(context, "Failed to remove favorite", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // Add to favorites
                    favoritesManager.addFavorite(eventId, new FavoritesManager.FavoriteCallback() {
                        @Override
                        public void onSuccess() {
                            isFavorited = true;
                            updateFavoriteIcon();
                            Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(context, "Failed to add favorite", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }

        /**
         * Updates the heart icon drawable based on the current {@code isFavorited} state.
         * <p>
         * Shows a filled heart when the event is favorited, and an outline otherwise.
         */
        private void updateFavoriteIcon() {
            if (isFavorited) {
                btnFavorite.setImageResource(R.drawable.ic_heart_filled);
            } else {
                btnFavorite.setImageResource(R.drawable.ic_heart_outline);
            }
        }
    }
}