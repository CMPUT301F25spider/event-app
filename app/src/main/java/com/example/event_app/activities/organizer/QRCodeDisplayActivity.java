package com.example.event_app.activities.organizer;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.event_app.R;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * QRCodeDisplayActivity - Display QR code after event creation
 * 
 * US 02.01.01: Display QR code for newly created event
 * Each event has a unique QR code generated from its eventId
 */
public class QRCodeDisplayActivity extends AppCompatActivity {

    private static final String TAG = "QRCodeDisplayActivity";

    // UI Elements
    private ImageView ivQRCode;
    private TextView tvEventName, tvInstructions;
    private MaterialButton btnDone, btnShareQR;

    // Data
    private String eventId;
    private String eventName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code_display);
        new AccessibilityHelper(this).applyAccessibilitySettings(this);

        // Get event data from intent
        eventId = getIntent().getStringExtra("eventId");
        eventName = getIntent().getStringExtra("eventName");

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Error: Event ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        generateAndDisplayQRCode();
    }

    private void initViews() {
        ivQRCode = findViewById(R.id.ivQRCode);
        tvEventName = findViewById(R.id.tvEventName);
        tvInstructions = findViewById(R.id.tvInstructions);
        btnDone = findViewById(R.id.btnDone);
        btnShareQR = findViewById(R.id.btnShareQR);

        // Set event name if provided
        if (eventName != null && !eventName.isEmpty()) {
            tvEventName.setText(eventName);
        } else {
            tvEventName.setText("Event QR Code");
        }

        // Button listeners
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnDone.setOnClickListener(v -> finish());
        btnShareQR.setOnClickListener(v -> shareQRCode());
    }

    /**
     * Generate and display QR code for the event
     */
    private void generateAndDisplayQRCode() {
        try {
            // Generate QR code bitmap
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(eventId, BarcodeFormat.QR_CODE, 512, 512);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            // Display QR code
            ivQRCode.setImageBitmap(bitmap);
            Log.d(TAG, "QR code generated successfully for event: " + eventId);

        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR code", e);
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Share QR code (optional feature)
     */
    private void shareQRCode() {
        // TODO: Implement QR code sharing functionality if needed
        Toast.makeText(this, "QR code sharing coming soon", Toast.LENGTH_SHORT).show();
    }
}

