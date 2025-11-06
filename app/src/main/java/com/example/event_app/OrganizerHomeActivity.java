package com.example.event_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;


import com.example.event_app.organizer.EventPosterActivity;
import com.example.event_app.organizer.OrganizerEntrantsListActivity;

public class OrganizerHomeActivity extends AppCompatActivity {

    private Button btnManageEntrants, btnUpdatePoster;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_home);

        btnManageEntrants = findViewById(R.id.btnManageEntrants);
        btnUpdatePoster = findViewById(R.id.btnUpdatePoster);

        // Example: Pass event ID when navigating
        String demoEventId = "event123"; // Replace with dynamic value from Firestore

        btnManageEntrants.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrganizerEntrantsListActivity.class);
            intent.putExtra("eventId", demoEventId);
            intent.putExtra("defaultFilter", "Selected");
            startActivity(intent);
        });

        btnUpdatePoster.setOnClickListener(v -> {
            Intent intent = new Intent(this, EventPosterActivity.class);
            intent.putExtra("eventId", demoEventId);
            startActivity(intent);
        });
    }
}
