package com.example.event_app.organizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.VH> {

    public interface ActionListener {
        void onCancelClicked(EntrantRow e);
    }

    private final ActionListener listener;
    private final List<EntrantRow> items = new ArrayList<>();
    private String currentFilterLabel = "Selected";

    public EntrantAdapter(ActionListener listener) {
        this.listener = listener;
    }

    public void submit(List<EntrantRow> list, String filterLabel) {
        items.clear();
        if (list != null) items.addAll(list);
        currentFilterLabel = filterLabel != null ? filterLabel : "Selected";
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entrant_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        EntrantRow e = items.get(position);

        h.name.setText(e.name != null && !e.name.isEmpty() ? e.name : (e.uid != null ? e.uid : "—"));
        h.email.setText(e.email != null ? e.email : "");
        h.status.setText(e.status != null ? e.status : "—");

        // Meta line (selected or confirmed time)
        if ("Confirmed".equalsIgnoreCase(currentFilterLabel) && e.confirmationTimestamp != null) {
            h.meta.setText("Confirmed: " + DateFormat.getDateTimeInstance()
                    .format(e.confirmationTimestamp.toDate()));
        } else if (e.selectionTimestamp != null) {
            h.meta.setText("Selected: " + DateFormat.getDateTimeInstance()
                    .format(e.selectionTimestamp.toDate()));
        } else {
            h.meta.setText("");
        }

        // Expiry
        if (e.invitationExpiry != null) {
            h.expiry.setVisibility(View.VISIBLE);
            h.expiry.setText("Expires: " + DateFormat.getDateTimeInstance()
                    .format(e.invitationExpiry.toDate()));
        } else {
            h.expiry.setVisibility(View.GONE);
        }

        // Show cancel only for Selected/Pending and when status is pending
        boolean showCancel = (
                "Selected".equalsIgnoreCase(currentFilterLabel)
                        || "Pending".equalsIgnoreCase(currentFilterLabel)
        ) && "pending".equalsIgnoreCase(e.status);

        h.btnCancel.setVisibility(showCancel ? View.VISIBLE : View.GONE);
        h.btnCancel.setOnClickListener(v -> {
            if (listener != null) listener.onCancelClicked(e);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, email, status, meta, expiry;
        Button btnCancel;
        VH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.tvName);
            email = v.findViewById(R.id.tvEmail);
            status = v.findViewById(R.id.tvStatus);
            meta = v.findViewById(R.id.tvMeta);
            expiry = v.findViewById(R.id.tvExpiry);
            btnCancel = v.findViewById(R.id.btnCancel);
        }
    }
}