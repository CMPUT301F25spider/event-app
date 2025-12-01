package com.example.event_app.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.models.Notification;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/**
 * Adapter for displaying a list of user notifications inside a RecyclerView.
 * Supports click actions, delete actions, unread highlighting, and relative timestamps.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final Context context;
    private final List<Notification> notifications;
    private final NotificationClickListener listener;

    /**
     * Listener interface for notification item interactions.
     */
    public interface NotificationClickListener {
        /**
         * Called when a notification item is tapped.
         *
         * @param notification the notification that was clicked
         */
        void onNotificationClick(Notification notification);

        /**
         * Called when a notification's delete button is tapped.
         *
         * @param notification the notification being deleted
         */
        void onDeleteClick(Notification notification);
    }

    /**
     * Creates a new adapter for rendering notifications.
     *
     * @param context        the context used to inflate layouts and access resources
     * @param notifications  the list of notifications to display
     * @param listener       callback interface for notification click and delete actions
     */
    public NotificationAdapter(Context context, List<Notification> notifications,
                               NotificationClickListener listener) {
        this.context = context;
        this.notifications = notifications;
        this.listener = listener;
    }

    /**
     * Inflates the layout for a single notification item.
     *
     * @param parent   the parent ViewGroup
     * @param viewType unused (single view type)
     * @return a new NotificationViewHolder instance
     */
    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    /**
     * Binds the notification at the given list position to the ViewHolder.
     *
     * @param holder   the ViewHolder responsible for rendering the item
     * @param position the index of the notification in the list
     */
    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.bind(notification);
    }

    /**
     * Returns the number of notifications in the adapter.
     *
     * @return total notification count
     */
    @Override
    public int getItemCount() {
        return notifications.size();
    }

    /**
     * ViewHolder representing a single notification card, including icon,
     * title, message, timestamp, event label, delete button, and unread indicator.
     */
    class NotificationViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView card;
        private final TextView tvIcon, tvTitle, tvMessage, tvTime, tvEventName;
        private final ImageButton btnDelete;
        private final View unreadIndicator;

        /**
         * Initializes all UI components for a notification item.
         *
         * @param itemView the inflated notification layout
         */
        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
        }

        /**
         * Binds a Notification object to the UI by:
         * <ul>
         *     <li>Displaying icon, title, message, and event name (if present)</li>
         *     <li>Formatting timestamp as a relative time</li>
         *     <li>Showing or hiding unread indicator</li>
         *     <li>Applying read/unread styling</li>
         *     <li>Highlighting important unread notifications</li>
         *     <li>Setting click and delete listeners</li>
         * </ul>
         *
         * @param notification the notification to render
         */
        public void bind(Notification notification) {
            // Set icon
            tvIcon.setText(notification.getIcon());

            // Set title
            tvTitle.setText(notification.getTitle());

            // Set message
            tvMessage.setText(notification.getMessage());

            // Set event name
            if (notification.getEventName() != null && !notification.getEventName().isEmpty()) {
                tvEventName.setText(notification.getEventName());
                tvEventName.setVisibility(View.VISIBLE);
            } else {
                tvEventName.setVisibility(View.GONE);
            }

            // Set time (relative time)
            String timeAgo = DateUtils.getRelativeTimeSpanString(
                    notification.getCreatedAt(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
            ).toString();
            tvTime.setText(timeAgo);

            // Show/hide unread indicator
            unreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

            // Style based on read status
            if (notification.isRead()) {
                card.setCardBackgroundColor(context.getColor(android.R.color.white));
                tvTitle.setAlpha(0.7f);
                tvMessage.setAlpha(0.7f);
            } else {
                card.setCardBackgroundColor(context.getColor(android.R.color.white));
                tvTitle.setAlpha(1.0f);
                tvMessage.setAlpha(1.0f);
            }

            // Highlight important notifications
            if (notification.isImportant() && !notification.isRead()) {
                card.setStrokeColor(context.getColor(android.R.color.holo_blue_light));
                card.setStrokeWidth(2);
            } else {
                card.setStrokeColor(context.getColor(android.R.color.transparent));
                card.setStrokeWidth(0);
            }

            // Click listeners
            card.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notification);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(notification);
                }
            });
        }
    }
}