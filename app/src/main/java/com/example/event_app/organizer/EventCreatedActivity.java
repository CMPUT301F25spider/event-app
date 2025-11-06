package com.example.event_app.organizer;//ani

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.event_app.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.OutputStream;

/**
 * EventCreatedActivity
 *
 * Displays a confirmation message, generates a QR code for the event,
 * and provides options to save or share the QR code.
 */
public class EventCreatedActivity extends AppCompatActivity {

    private ImageView ivQrCode;
    private Button btnDone, btnSave, btnShare;
    private Bitmap qrBitmap; // store generated QR for reuse

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_created);

        ivQrCode = findViewById(R.id.ivQrCode);
        btnDone = findViewById(R.id.btnDone);
        btnSave = findViewById(R.id.btnSave);
        btnShare = findViewById(R.id.btnShare);

        // Example event data (replace with your actual event info)
        String eventData = "https://myevent.com/details?id=123";

        // Generate QR code
        generateQrCode(eventData);

        // Button actions
        btnDone.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            if (qrBitmap != null) {
                saveBitmapToGallery(qrBitmap);
            }
        });

        btnShare.setOnClickListener(v -> {
            // TODO: implement sharing QR code via Intent
            Toast.makeText(this, "Share clicked", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Generates a QR code bitmap from the given event data string and displays it.
     *
     * @param eventData The string containing event information to encode in the QR code.
     */
    private void generateQrCode(String eventData) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            qrBitmap = barcodeEncoder.encodeBitmap(eventData, BarcodeFormat.QR_CODE, 250, 250);
            ivQrCode.setImageBitmap(qrBitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save a Bitmap image (QR code) to the device's gallery using MediaStore.
     *
     * @param bitmap The QR code bitmap to save.
     */
    private void saveBitmapToGallery(Bitmap bitmap) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyEvents");
                values.put(MediaStore.Images.Media.IS_PENDING, true);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    OutputStream fos = getContentResolver().openOutputStream(uri);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    if (fos != null) fos.close();

                    values.put(MediaStore.Images.Media.IS_PENDING, false);
                    getContentResolver().update(uri, values, null, null);

                    Toast.makeText(this, "QR code saved to gallery!", Toast.LENGTH_SHORT).show();
                }
            } else {
                // For older Android versions
                String savedImageURL = MediaStore.Images.Media.insertImage(
                        getContentResolver(),
                        bitmap,
                        "Event QR",
                        "QR code for event"
                );
                if (savedImageURL != null) {
                    Toast.makeText(this, "QR code saved to gallery!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error saving QR code!", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save QR code!", Toast.LENGTH_SHORT).show();
        }
    }
}