package com.example.event_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.models.NotificationTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying and managing notification templates inside a RecyclerView.
 * Supports searching, activating/deactivating templates, editing, and deleting.
 */
public class NotificationTemplateAdapter extends RecyclerView.Adapter<NotificationTemplateAdapter.TemplateViewHolder> {

    private List<NotificationTemplate> templates;
    private List<NotificationTemplate> templatesFiltered;
    private OnTemplateClickListener listener;

    /**
     * Creates a new adapter with empty lists for templates and filtered results.
     */
    public NotificationTemplateAdapter() {
        this.templates = new ArrayList<>();
        this.templatesFiltered = new ArrayList<>();
    }

    /**
     * Inflates the layout for a single notification template entry.
     *
     * @param parent   the parent view group containing the RecyclerView
     * @param viewType the type of view to inflate (unused, single type)
     * @return a new TemplateViewHolder instance
     */
    @NonNull
    @Override
    public TemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_template, parent, false);
        return new TemplateViewHolder(view);
    }

    /**
     * Binds a NotificationTemplate object to the ViewHolder at the given position.
     *
     * @param holder   the ViewHolder containing UI elements
     * @param position index of the template to bind
     */
    @Override
    public void onBindViewHolder(@NonNull TemplateViewHolder holder, int position) {
        NotificationTemplate template = templatesFiltered.get(position);
        holder.bind(template, listener);
    }

    /**
     * Returns the number of templates currently visible after filtering.
     *
     * @return size of filtered template list
     */
    @Override
    public int getItemCount() {
        return templatesFiltered.size();
    }

    /**
     * Replaces the current template list with a new one and refreshes the display.
     *
     * @param templates the complete list of templates to load
     */
    public void setTemplates(List<NotificationTemplate> templates) {
        this.templates = templates;
        this.templatesFiltered = new ArrayList<>(templates);
        notifyDataSetChanged();
    }

    /**
     * Filters the template list by matching name, type, or title using the given query.
     * Search is case-insensitive and updates the RecyclerView accordingly.
     *
     * @param query the text used to filter templates
     */
    public void filter(String query) {
        templatesFiltered.clear();
        if (query.isEmpty()) {
            templatesFiltered.addAll(templates);
        } else {
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            for (NotificationTemplate template : templates) {
                if (template.getName().toLowerCase().contains(lowerQuery) ||
                        template.getType().toLowerCase().contains(lowerQuery) ||
                        template.getTitle().toLowerCase().contains(lowerQuery)) {
                    templatesFiltered.add(template);
                }
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Sets the callback listener for interactions on template items such as edit, toggle, or delete.
     *
     * @param listener the listener invoked when user interacts with a template item
     */
    public void setOnTemplateClickListener(OnTemplateClickListener listener) {
        this.listener = listener;
    }

    /**
     * ViewHolder representing a single notification template item.
     * Displays template name, type, title, message, and supports toggling and deleting.
     */
    class TemplateViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvType, tvTitle, tvMessage;
        Switch switchActive;
        ImageButton btnDelete;

        /**
         * Initializes all UI components used to display a notification template.
         *
         * @param itemView the root view inflated for this list item
         */
        public TemplateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTemplateName);
            tvType = itemView.findViewById(R.id.tvTemplateType);
            tvTitle = itemView.findViewById(R.id.tvTemplateTitle);
            tvMessage = itemView.findViewById(R.id.tvTemplateMessage);
            switchActive = itemView.findViewById(R.id.switchActive);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        /**
         * Binds a NotificationTemplate object to the UI, populating all text fields,
         * setting the active switch state, and assigning click listeners.
         *
         * Behavior includes:
         * <ul>
         *     <li>Click whole row → edit template</li>
         *     <li>Toggle switch → activate or deactivate</li>
         *     <li>Delete button → remove template</li>
         * </ul>
         *
         * @param template the template being displayed
         * @param listener callback listener for user interactions
         */
        public void bind(NotificationTemplate template, OnTemplateClickListener listener) {
            tvName.setText(template.getName());
            tvType.setText(template.getType());
            tvTitle.setText(template.getTitle());
            tvMessage.setText(template.getMessage());
            switchActive.setChecked(template.isActive());

            // Click to edit
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTemplateClick(template);
                }
            });

            // Toggle active/inactive
            switchActive.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onToggleActive(template);
                }
            });

            // Delete button
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteTemplate(template);
                }
            });
        }
    }

    /**
     * Listener interface for handling user interactions on notification templates,
     * such as editing, toggling active state, or deleting a template.
     */
    public interface OnTemplateClickListener {

        /**
         * Called when the user taps a template to edit or view details.
         *
         * @param template the selected notification template
         */
        void onTemplateClick(NotificationTemplate template);

        /**
         * Called when the user toggles the active/inactive switch for a template.
         *
         * @param template the template whose active state was changed
         */
        void onToggleActive(NotificationTemplate template);

        /**
         * Called when the user clicks the delete button for a template.
         *
         * @param template the template selected for deletion
         */
        void onDeleteTemplate(NotificationTemplate template);
    }
}
