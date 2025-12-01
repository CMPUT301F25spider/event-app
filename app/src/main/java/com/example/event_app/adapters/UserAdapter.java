package com.example.event_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.models.User;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying users with search, role display,
 * and organizer event statistics. Supports filtering and admin actions
 * such as deleting users and removing organizer privileges.
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> users;
    private List<User> usersFiltered;  // NEW: For search
    private OnUserClickListener listener;

    /**
     * Creates a new UserAdapter with empty lists for users and filtered users.
     */
    public UserAdapter() {
        this.users = new ArrayList<>();
        this.usersFiltered = new ArrayList<>();
    }

    /**
     * Inflates the layout for a single user row and creates a ViewHolder.
     *
     * @param parent   the parent ViewGroup containing the RecyclerView
     * @param viewType the view type (unused since there is a single type)
     * @return a new UserViewHolder instance
     */
    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    /**
     * Binds a User object at the given position to the ViewHolder.
     *
     * @param holder   the ViewHolder to bind
     * @param position the index of the user in the filtered list
     */
    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = usersFiltered.get(position);
        holder.bind(user, listener);
    }

    /**
     * Returns the number of users currently visible (after filtering).
     *
     * @return total size of the filtered user list
     */
    @Override
    public int getItemCount() {
        return usersFiltered.size();
    }

    /**
     * Replaces the current user list and resets the filtered list.
     *
     * @param users the full list of users to display
     */
    public void setUsers(List<User> users) {
        this.users = users;
        this.usersFiltered = new ArrayList<>(users);
        notifyDataSetChanged();
    }

    /**
     * Filters the user list based on a search query, matching name or email.
     *
     * @param query the text entered in the search field
     */
    public void filter(String query) {
        usersFiltered.clear();

        if (query.isEmpty()) {
            usersFiltered.addAll(users);
        } else {
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            for (User user : users) {
                // Search by name or email
                if (user.getName().toLowerCase(Locale.getDefault()).contains(lowerQuery) ||
                        user.getEmail().toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
                    usersFiltered.add(user);
                }
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Assigns a click listener for user-level actions such as selecting,
     * removing organizer access, or deleting users.
     *
     * @param listener the listener implementation to assign
     */
    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
    }

    /**
     * ViewHolder representing a single user row in the list.
     * Displays user identity, roles, event-hosting stats, and admin action buttons.
     */
    class UserViewHolder extends RecyclerView.ViewHolder {

        private TextView tvUserName;
        private TextView tvUserEmail;
        private TextView tvUserRoles;
        private TextView tvEventsHosted;  // NEW
        private MaterialButton btnDeleteUser;
        private MaterialButton btnRemoveOrganizer;

        /**
         * Initializes UI components for a user card row.
         *
         * @param itemView the inflated row layout for a user item
         */
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);

            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvUserRoles = itemView.findViewById(R.id.tvUserRoles);
            tvEventsHosted = itemView.findViewById(R.id.tvEventsHosted);  // NEW
            btnDeleteUser = itemView.findViewById(R.id.btnDeleteUser);
            btnRemoveOrganizer = itemView.findViewById(R.id.btnRemoveOrganizer);
        }

        /**
         * Binds a User object's data to the ViewHolder UI components.
         * Handles:
         * <ul>
         *   <li>Displaying name, email, and roles</li>
         *   <li>Showing hosted events count for organizers</li>
         *   <li>Admin actions: delete user, remove organizer role</li>
         *   <li>Item tap to view user details</li>
         * </ul>
         *
         * @param user     the User object being bound
         * @param listener the listener handling tap and admin interactions
         */
        public void bind(User user, OnUserClickListener listener) {
            // Set user name
            tvUserName.setText(user.getName());

            // Set user email
            tvUserEmail.setText(user.getEmail());

            // Set user roles
            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                String rolesText = String.join(", ", user.getRoles());
                tvUserRoles.setText(rolesText.toUpperCase());
            } else {
                tvUserRoles.setText("NO ROLES");
            }

            // Show events hosted count for organizers
            if (user.isOrganizer()) {
                tvEventsHosted.setVisibility(View.VISIBLE);
                tvEventsHosted.setText("Loading events...");

                // Click to see event details
                tvEventsHosted.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onViewEventsClick(user);
                    }
                });

                // Trigger loading events count
                if (listener != null) {
                    listener.onLoadEventsCount(user, count -> {
                        if (count == 0) {
                            tvEventsHosted.setText("No events hosted");
                        } else {
                            tvEventsHosted.setText(count + (count == 1 ? " event hosted" : " events hosted") + " - Tap to view");
                        }
                    });
                }
            } else {
                tvEventsHosted.setVisibility(View.GONE);
            }

            // Show/hide "Remove Organizer" button
            if (user.isOrganizer()) {
                btnRemoveOrganizer.setVisibility(View.VISIBLE);
                btnRemoveOrganizer.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onRemoveOrganizerClick(user);
                    }
                });
            } else {
                btnRemoveOrganizer.setVisibility(View.GONE);
            }

            // Delete button
            btnDeleteUser.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(user);
                }
            });

            // Item click
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });
        }
    }

    /**
     * Listener interface for user interaction events in the adapter.
     */
    public interface OnUserClickListener {

        /**
         * Called when the admin taps a user row.
         *
         * @param user the selected user
         */
        void onUserClick(User user);

        /**
         * Called when the admin clicks the delete button for a user.
         *
         * @param user the user to delete
         */
        void onDeleteClick(User user);

        /**
         * Called when the admin clicks the "Remove Organizer" button.
         *
         * @param user the organizer whose role is being revoked
         */
        void onRemoveOrganizerClick(User user);

        /**
         * Called when the admin taps the event count badge to view all events
         * created by an organizer.
         *
         * @param user the organizer whose events should be viewed
         */
        void onViewEventsClick(User user);  // NEW: View organizer's events

        /**
         * Called when the adapter needs the number of events hosted by a user.
         *
         * @param user     the organizer
         * @param callback callback used to supply the loaded count
         */
        void onLoadEventsCount(User user, EventsCountCallback callback);  // NEW: Load count
    }

    /**
     * Callback interface for returning the number of events hosted by a user.
     */
    public interface EventsCountCallback {

        /**
         * Called when the event count is retrieved.
         *
         * @param count number of events hosted by the user
         */
        void onCountLoaded(int count);
    }
}