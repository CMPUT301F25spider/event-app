package com.example.event_app.activities.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.adapters.NotificationTemplateAdapter;
import com.example.event_app.models.NotificationTemplate;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Activity allowing administrators to manage reusable notification templates
 * used throughout the platform. Supports creating, editing, activating/
 * deactivating, deleting, and searching templates.
 *
 * <p>Features include:</p>
 * <ul>
 *     <li>Search across template name, type, title, and message</li>
 *     <li>Create new templates via dialog UI</li>
 *     <li>Edit existing templates</li>
 *     <li>Enable/disable templates for system use</li>
 *     <li>Delete templates permanently</li>
 *     <li>Display total active templates for audit purposes</li>
 * </ul>
 *
 * Used primarily by platform administrators to standardize communication
 * sent to entrants and organizers across the system.
 */
public class AdminNotificationTemplatesActivity extends AppCompatActivity {
    private EditText etSearch;
    private RecyclerView recyclerViewTemplates;
    private LinearLayout emptyStateLayout;
    private ProgressBar progressBar;
    private TextView tvTotalTemplates;
    private FloatingActionButton fabAddTemplate;

    private NotificationTemplateAdapter templateAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<NotificationTemplate> templateList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_notification_templates);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Notification Templates");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize list
        templateList = new ArrayList<>();

        // Initialize views
        initViews();

        // Set up search
        setupSearch();

        // Set up RecyclerView
        setupRecyclerView();

        // Set up FAB
        setupFAB();

        // Load templates
        loadTemplates();
    }

    /**
     * Initializes UI components including search bar, RecyclerView, empty state
     * container, progress bar, template counter, and the FAB for adding templates.
     */
    private void initViews() {
        etSearch = findViewById(R.id.etSearchTemplates);
        recyclerViewTemplates = findViewById(R.id.recyclerViewTemplates);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        progressBar = findViewById(R.id.progressBar);
        tvTotalTemplates = findViewById(R.id.tvTotalTemplates);
        fabAddTemplate = findViewById(R.id.fabAddTemplate);
    }

    /**
     * Configures live search using a TextWatcher. All text changes trigger
     * filtering of templates in the adapter based on the entered query.
     */
    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                templateAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Initializes the RecyclerView, attaches the NotificationTemplateAdapter,
     * and registers click listeners for:
     * <ul>
     *     <li>Opening the edit dialog for a template</li>
     *     <li>Toggling active/inactive state</li>
     *     <li>Deleting a template</li>
     * </ul>
     */
    private void setupRecyclerView() {
        templateAdapter = new NotificationTemplateAdapter();

        templateAdapter.setOnTemplateClickListener(new NotificationTemplateAdapter.OnTemplateClickListener() {
            @Override
            public void onTemplateClick(NotificationTemplate template) {
                showEditTemplateDialog(template);
            }

            @Override
            public void onToggleActive(NotificationTemplate template) {
                toggleTemplateActive(template);
            }

            @Override
            public void onDeleteTemplate(NotificationTemplate template) {
                showDeleteConfirmation(template);
            }
        });

        recyclerViewTemplates.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewTemplates.setAdapter(templateAdapter);
    }

    /**
     * Configures the floating action button to open the "Create Template" dialog
     * allowing administrators to build a new notification template.
     */
    private void setupFAB() {
        fabAddTemplate.setOnClickListener(v -> showCreateTemplateDialog());
    }

    /**
     * Fetches all notification templates from Firestore, populates the internal
     * list, updates UI state, and refreshes the adapter.
     *
     * <p>Shows a progress bar while loading and displays an error message if
     * retrieval fails.</p>
     */
    private void loadTemplates() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("notification_templates")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    templateList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        NotificationTemplate template = document.toObject(NotificationTemplate.class);
                        templateList.add(template);
                    }
                    progressBar.setVisibility(View.GONE);
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading templates: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    updateUI();
                });
    }

    /**
     * Updates the visibility of UI components based on whether templates exist.
     * Shows or hides the empty state layout and updates the total count label.
     */
    private void updateUI() {
        if (templateList.isEmpty()) {
            recyclerViewTemplates.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
            tvTotalTemplates.setText("Total: 0 templates");
        } else {
            recyclerViewTemplates.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            tvTotalTemplates.setText("Total: " + templateList.size() + " templates");

            templateAdapter.setTemplates(templateList);
        }
    }

    /**
     * Displays a dialog allowing the administrator to create a new notification
     * template. Validates required fields and passes input to
     * {@link #createTemplate(String, String, String, String)}.
     */
    private void showCreateTemplateDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_notification_template, null);

        EditText etName = dialogView.findViewById(R.id.etTemplateName);
        EditText etType = dialogView.findViewById(R.id.etTemplateType);
        EditText etTitle = dialogView.findViewById(R.id.etTemplateTitle);
        EditText etMessage = dialogView.findViewById(R.id.etTemplateMessage);
        TextView tvPlaceholders = dialogView.findViewById(R.id.tvPlaceholders);

        tvPlaceholders.setText("Available placeholders:\n{userName}, {eventName}, {organizerName}, {date}");

        new AlertDialog.Builder(this)
                .setTitle("Create Template")
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String type = etType.getText().toString().trim();
                    String title = etTitle.getText().toString().trim();
                    String message = etMessage.getText().toString().trim();

                    if (name.isEmpty() || title.isEmpty() || message.isEmpty()) {
                        Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    createTemplate(name, type, title, message);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Displays a dialog pre-filled with an existing template’s fields, allowing
     * administrators to modify and save updates.
     *
     * @param template the template being edited
     */
    private void showEditTemplateDialog(NotificationTemplate template) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_notification_template, null);

        EditText etName = dialogView.findViewById(R.id.etTemplateName);
        EditText etType = dialogView.findViewById(R.id.etTemplateType);
        EditText etTitle = dialogView.findViewById(R.id.etTemplateTitle);
        EditText etMessage = dialogView.findViewById(R.id.etTemplateMessage);
        TextView tvPlaceholders = dialogView.findViewById(R.id.tvPlaceholders);

        // Pre-fill with existing data
        etName.setText(template.getName());
        etType.setText(template.getType());
        etTitle.setText(template.getTitle());
        etMessage.setText(template.getMessage());
        tvPlaceholders.setText("Available placeholders:\n{userName}, {eventName}, {organizerName}, {date}");

        new AlertDialog.Builder(this)
                .setTitle("Edit Template")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String type = etType.getText().toString().trim();
                    String title = etTitle.getText().toString().trim();
                    String message = etMessage.getText().toString().trim();

                    if (name.isEmpty() || title.isEmpty() || message.isEmpty()) {
                        Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateTemplate(template.getTemplateId(), name, type, title, message);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Creates a new notification template in Firestore.
     *
     * @param name     the human-readable template name
     * @param type     a logical category label for grouping templates
     * @param title    the template notification title
     * @param message  the message body supporting placeholders
     */
    private void createTemplate(String name, String type, String title, String message) {
        String templateId = db.collection("notification_templates").document().getId();
        String adminId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "unknown";

        NotificationTemplate template = new NotificationTemplate(
                templateId,
                name,
                type,
                title,
                message,
                true,  // Active by default
                new Date(),
                new Date(),
                adminId
        );

        db.collection("notification_templates")
                .document(templateId)
                .set(template)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Template created!", Toast.LENGTH_SHORT).show();
                    loadTemplates();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Updates an existing template's fields and modification timestamp in
     * Firestore.
     *
     * @param templateId the ID of the template to update
     * @param name       updated template name
     * @param type       updated template category/type
     * @param title      updated notification title
     * @param message    updated notification message body
     */
    private void updateTemplate(String templateId, String name, String type, String title, String message) {
        db.collection("notification_templates")
                .document(templateId)
                .update(
                        "name", name,
                        "type", type,
                        "title", title,
                        "message", message,
                        "updatedAt", new Date()
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Template updated!", Toast.LENGTH_SHORT).show();
                    loadTemplates();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Toggles a template’s active status and persists the change to Firestore.
     * Inactive templates are hidden from organizers using template selection.
     *
     * @param template the template whose active state is being toggled
     */
    private void toggleTemplateActive(NotificationTemplate template) {
        boolean newStatus = !template.isActive();

        db.collection("notification_templates")
                .document(template.getTemplateId())
                .update("isActive", newStatus, "updatedAt", new Date())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            newStatus ? "Template activated" : "Template deactivated",
                            Toast.LENGTH_SHORT).show();
                    loadTemplates();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Displays a confirmation dialog asking the administrator to confirm
     * permanent deletion of a template.
     *
     * @param template the template selected for deletion
     */
    private void showDeleteConfirmation(NotificationTemplate template) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Template")
                .setMessage("Delete \"" + template.getName() + "\"?\n\nThis action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteTemplate(template);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Permanently removes a template from Firestore.
     *
     * @param template the template to delete
     */
    private void deleteTemplate(NotificationTemplate template) {
        db.collection("notification_templates")
                .document(template.getTemplateId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Template deleted", Toast.LENGTH_SHORT).show();
                    loadTemplates();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Handles action bar "Up" navigation by closing the activity.
     *
     * @return true once the activity is finished
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
