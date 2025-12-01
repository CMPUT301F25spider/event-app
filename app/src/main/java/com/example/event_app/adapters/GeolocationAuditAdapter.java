package com.example.event_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.models.GeolocationAudit;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * GeolocationAuditAdapter - Displays a list of geolocation audit logs.
 *
 * Used by admins to view:
 * - User name
 * - Event name
 * - Location (lat/lng or resolved address)
 * - Timestamp of action
 * - Type of action (CHECK_IN, CHECK_OUT, LOCATION_DENIED etc.)
 *
 * Supports:
 * - Filtering by username or event name
 * - Row click callbacks
 */
public class GeolocationAuditAdapter extends RecyclerView.Adapter<GeolocationAuditAdapter.AuditViewHolder> {

    private List<GeolocationAudit> audits;
    private List<GeolocationAudit> auditsFiltered;
    private OnAuditClickListener listener;

    /**
     * Constructor initializes empty audit lists.
     */
    public GeolocationAuditAdapter() {
        this.audits = new ArrayList<>();
        this.auditsFiltered = new ArrayList<>();
    }

    /**
     * Inflates a row for a single geolocation audit entry.
     *
     * @param parent the parent view group
     * @param viewType ignored (single view type)
     * @return a new {@link AuditViewHolder}
     */
    @NonNull
    @Override
    public AuditViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_geolocation_audit, parent, false);
        return new AuditViewHolder(view);
    }

    /**
     * Binds a specific geolocation audit entry to a row.
     *
     * @param holder the view holder to bind
     * @param position the position in the filtered list
     */
    @Override
    public void onBindViewHolder(@NonNull AuditViewHolder holder, int position) {
        GeolocationAudit audit = auditsFiltered.get(position);
        holder.bind(audit, listener);
    }

    /**
     * @return total number of displayed (filtered) audit entries
     */
    @Override
    public int getItemCount() {
        return auditsFiltered.size();
    }

    /**
     * Sets a full list of audit entries and refreshes display.
     *
     * @param audits the full list of geolocation audits
     */
    public void setAudits(List<GeolocationAudit> audits) {
        this.audits = audits;
        this.auditsFiltered = new ArrayList<>(audits);
        notifyDataSetChanged();
    }

    /**
     * Filters the audit list by user or event name.
     *
     * @param query search text typed by the user
     */
    public void filter(String query) {
        auditsFiltered.clear();
        if (query.isEmpty()) {
            auditsFiltered.addAll(audits);
        } else {
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            for (GeolocationAudit audit : audits) {
                if (audit.getUserName().toLowerCase().contains(lowerQuery) ||
                        audit.getEventName().toLowerCase().contains(lowerQuery)) {
                    auditsFiltered.add(audit);
                }
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Assigns a click listener for audit row interactions.
     *
     * @param listener callback for row taps
     */
    public void setOnAuditClickListener(OnAuditClickListener listener) {
        this.listener = listener;
    }

    /**
     * ViewHolder representing a single audit entry row.
     */
    class AuditViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvEventName, tvLocation, tvTimestamp, tvAction;

        /**
         * Initializes references to UI elements.
         *
         * @param itemView the inflated row layout
         */
        public AuditViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvAction = itemView.findViewById(R.id.tvAction);
        }

        /**
         * Binds geolocation audit data into UI views.
         *
         * @param audit the audit record to display
         * @param listener row click callback
         */
        public void bind(GeolocationAudit audit, OnAuditClickListener listener) {
            tvUserName.setText(audit.getUserName());
            tvEventName.setText(audit.getEventName());
            tvLocation.setText(audit.getLocationString());
            tvAction.setText(audit.getAction().replace("_", " ").toUpperCase());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            if (audit.getTimestamp() != null) {
                tvTimestamp.setText(sdf.format(audit.getTimestamp()));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAuditClick(audit);
                }
            });
        }
    }

    /**
     * Listener interface for audit row clicks.
     */
    public interface OnAuditClickListener {
        void onAuditClick(GeolocationAudit audit);
    }
}