package com.example.event_app.organizer;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.event_app.R;
import com.example.event_app.adapters.EntrantAdapter;

public class OrganizerEntrantsListActivity extends AppCompatActivity {

    private String eventId = "demoEvent";
    private String defaultFilter = "Selected";

    private Spinner filterSpinner;
    private TextView tvCount;
    private TextView tvFinalBadge;
    private ProgressBar progress;
    private EntrantAdapter adapter;
    private EntrantsListViewModel vm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrants_list);

        if (getIntent() != null) {
            if (getIntent().hasExtra("eventId")) eventId = getIntent().getStringExtra("eventId");
            if (getIntent().hasExtra("defaultFilter")) defaultFilter = getIntent().getStringExtra("defaultFilter");
        }

        // Views
        filterSpinner = findViewById(R.id.filterSpinner);
        tvCount       = findViewById(R.id.tvCount);
        tvFinalBadge  = findViewById(R.id.tvFinalBadge);
        progress      = findViewById(R.id.progress);
        RecyclerView rv = findViewById(R.id.recycler);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EntrantAdapter(this::confirmCancel);
        rv.setAdapter(adapter);

        // Spinner setup
        String[] filters = new String[]{"Selected","Pending","Accepted","Declined","Confirmed","Cancelled","All"};
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, filters);
        filterSpinner.setAdapter(a);
        int idx = Math.max(0, a.getPosition(defaultFilter));
        filterSpinner.setSelection(idx);

        // ViewModel (simple factory; swap to DI later if you want)
        vm = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(Class<T> modelClass) {
                return (T) new EntrantsListViewModel(new FirestoreEntrantsRepository());
            }
        }).get(EntrantsListViewModel.class);

        vm.init(eventId, EntrantsFilter.fromLabel(defaultFilter));

        // Observe
        vm.entrants().observe(this, list ->
                adapter.submit(list, (String) filterSpinner.getSelectedItem())
        );
        vm.countLabel().observe(this, label -> tvCount.setText(label));
        vm.loading().observe(this, isLoading -> progress.setVisibility(isLoading ? View.VISIBLE : View.GONE));
        vm.toast().observe(this, msg -> { if (msg != null && !msg.isEmpty()) toast(msg); });

        // Spinner listener
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String label = (String) parent.getItemAtPosition(position);
                vm.setFilter(EntrantsFilter.fromLabel(label));
                tvFinalBadge.setVisibility("Confirmed".equalsIgnoreCase(label) ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void confirmCancel(EntrantRow e) {
        if (e == null) return;
        if (e.status == null || !"pending".equalsIgnoreCase(e.status)) {
            toast("Only pending entrants can be cancelled");
            return;
        }

        final EditText input = new EditText(this);
        input.setHint("Reason (optional)");

        new AlertDialog.Builder(this)
                .setTitle("Cancel entrant?")
                .setMessage("This will mark the entrant as cancelled.")
                .setView(input)
                .setNegativeButton("Back", null)
                .setPositiveButton("Cancel Entrant", (dialog, which) -> {
                    String reason = input.getText() != null ? input.getText().toString().trim() : "";
                    // For now, alsoPromote = true. Replace with a small dialog to choose if you want.
                    vm.cancel(e.id, reason, true);
                })
                .show();
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
