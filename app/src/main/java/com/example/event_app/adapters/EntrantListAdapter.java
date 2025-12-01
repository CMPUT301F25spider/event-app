package com.example.event_app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.models.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * EntrantListAdapter
 *
 * RecyclerView adapter responsible for displaying users (entrants)
 * belonging to a specific event list such as:
 * <ul>
 *     <li>Waiting List</li>
 *     <li>Selected List</li>
 *     <li>Attending List</li>
 * </ul>
 *
 * Functionality:
 * <ul>
 *     <li>Fetches user data from Firestore based on userId</li>
 *     <li>Binds name, email, and phone number (if present)</li>
 *     <li>Supports multiple tabs via {@code listType}</li>
 * </ul>
 *
 * Used by: ViewEntrantsActivity (Organizer side)
 */
public class EntrantListAdapter extends RecyclerView.Adapter<EntrantListAdapter.EntrantViewHolder> {

    private Context context;
    private List<String> userIds;
    private List<User> users;
    private FirebaseFirestore db;
    private String eventId;
    private String listType; // waiting, selected, attending

    /**
     * Creates an adapter for displaying entrants of a specific event list.
     *
     * @param context Android context for inflation and resource access
     * @param eventId ID of the event whose entrant list is being displayed
     */
    public EntrantListAdapter(Context context, String eventId) {
        this.context = context;
        this.eventId = eventId;
        this.userIds = new ArrayList<>();
        this.users = new ArrayList<>();
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Inflates the entrant row layout and creates a ViewHolder.
     *
     * @param parent   RecyclerView parent
     * @param viewType unused; single view type used
     * @return EntrantViewHolder containing inflated row layout
     */
    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_entrant, parent, false);
        return new EntrantViewHolder(view);
    }

    /**
     * Binds a user entry by:
     * <ul>
     *     <li>Loading the user document from Firestore</li>
     *     <li>Converting it to a User model</li>
     *     <li>Delegating UI binding to the ViewHolder's bind() method</li>
     * </ul>
     *
     * Firestore loads asynchronously, ensuring smooth UI scrolling.
     *
     * @param holder   ViewHolder instance
     * @param position adapter position of the item
     */
    @Override
    public void onBindViewHolder(@NonNull EntrantViewHolder holder, int position) {
        String userId = userIds.get(position);

        // Load user data
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            holder.bind(user);
                        }
                    }
                });
    }

    /**
     * @return number of user IDs in the list
     */
    @Override
    public int getItemCount() {
        return userIds.size();
    }

    /**
     * Replaces the current list of users and reloads UI.
     *
     * @param userIds  List of user document IDs (waiting/selected/attending)
     * @param listType Label indicating which tab's data we are viewing
     */
    public void setUserIds(List<String> userIds, String listType) {
        this.userIds = userIds != null ? userIds : new ArrayList<>();
        this.listType = listType;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder representing a single entrant row.
     * Responsible for binding:
     * <ul>
     *     <li>User name</li>
     *     <li>Email</li>
     *     <li>Phone (shown conditionally)</li>
     * </ul>
     */
    static class EntrantViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvEmail, tvPhone;

        /**
         * Creates a ViewHolder for the entrant row layout.
         *
         * @param itemView inflated layout for each entrant
         */
        public EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvPhone = itemView.findViewById(R.id.tvPhone);
        }

        /**
         * Populates UI elements with a User object's data.
         *
         * @param user the User model fetched from Firestore
         */
        public void bind(User user) {
            tvName.setText(user.getName());
            tvEmail.setText(user.getEmail());

            if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                tvPhone.setVisibility(View.VISIBLE);
                tvPhone.setText(user.getPhoneNumber());
            } else {
                tvPhone.setVisibility(View.GONE);
            }
        }
    }
}