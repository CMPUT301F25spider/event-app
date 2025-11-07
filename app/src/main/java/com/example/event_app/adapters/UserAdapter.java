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

/**
 * Adapter for displaying users in RecyclerView
 * Used in BrowseUsersActivity for Admin to view all users
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> users;
    private OnUserClickListener listener;

    public UserAdapter() {
        this.users = new ArrayList<>();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    /**
     * Update the list of users
     */
    public void setUsers(List<User> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    /**
     * Set the click listener
     */
    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
    }

    /**
     * ViewHolder for user items
     */
    static class UserViewHolder extends RecyclerView.ViewHolder {

        private TextView tvUserName;
        private TextView tvUserEmail;
        private TextView tvUserRoles;
        private MaterialButton btnDeleteUser;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);

            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvUserRoles = itemView.findViewById(R.id.tvUserRoles);
            btnDeleteUser = itemView.findViewById(R.id.btnDeleteUser);
        }

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

            // Set delete button click listener
            btnDeleteUser.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(user);
                }
            });

            // Set item click listener (optional - for viewing user details)
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });
        }
    }

    /**
     * Interface for handling click events
     */
    public interface OnUserClickListener {
        void onUserClick(User user);
        void onDeleteClick(User user);
    }
}