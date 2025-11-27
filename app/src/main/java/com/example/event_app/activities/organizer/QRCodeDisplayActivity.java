package com.example.event_app.activities.organizer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.event_app.R;
import com.example.event_app.utils.AccessibilityHelper;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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
    private Bitmap qrCodeBitmap;

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

            // Store bitmap for sharing
            qrCodeBitmap = bitmap;
            
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
     * Share QR code image
     */
    private void shareQRCode() {
        if (qrCodeBitmap == null) {
            Toast.makeText(this, "QR code not available", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Save bitmap to temporary file
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            
            String fileName = "qr_code_" + eventId + ".png";
            File file = new File(cachePath, fileName);
            
            FileOutputStream outputStream = new FileOutputStream(file);
            qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            // Get URI using FileProvider
            Uri fileUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );

            // Create share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, 
                    "QR Code for event: " + (eventName != null ? eventName : "Event") + 
                    "\n\nScan this QR code to join the event!");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Start share chooser
            startActivity(Intent.createChooser(shareIntent, "Share QR Code"));

        } catch (IOException e) {
            Log.e(TAG, "Error sharing QR code", e);
            Toast.makeText(this, "Failed to share QR code", Toast.LENGTH_SHORT).show();
        }
    }
}

