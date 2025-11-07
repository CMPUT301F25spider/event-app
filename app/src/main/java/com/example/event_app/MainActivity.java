package com.example.event_app;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class MainActivity extends AppCompatActivity {

    private QRService qrService;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d("MainActivity", "Camera permission granted. Launching scanner.");
                    launchQrScanner();
                } else {
                    Log.d("MainActivity", "Camera permission denied.");
                    Toast.makeText(this, "Camera permission is required to scan QR codes.", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<ScanOptions> qrCodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(MainActivity.this, "Scan cancelled", Toast.LENGTH_LONG).show();
                } else {
                    // Pass the result to the QRService to handle it
                    qrService.processQrCode(this, result.getContents());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Navigator navigator = new Navigator();
        qrService = new QRService(navigator);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void scanQrCode(View v) {
        if (PermissionManager.isCameraPermissionGranted(this)) {
            launchQrScanner();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchQrScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan an event QR code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        qrCodeLauncher.launch(options);
    }

    public void toggle(View v) {
        v.setEnabled(false);
        Log.d("successbro", "button disabled"); //loggging
        // Safe cast with instanceof check to avoid ClassCastException
        if (v instanceof Button) {
            Button button = (Button) v;
            button.setText("Disabled");
        }
    }
    public void handleText(View v){


        Toast.makeText(this,"hi", Toast.LENGTH_LONG).show();
        Log.d("input", "hi"); //This is so cooked

    }
    public void launchSettings(View v){
        //launch a new activity
        Intent i = new Intent(this,SettingsActivity.class );
        startActivity(i);

    }

    /**
     * Opens the Browse Events screen for entrants.
     */
    public void openBrowseEvents(View v) {
        Intent intent = new Intent(this, EntrantBrowseEventsActivity.class);
        startActivity(intent);
    }

}