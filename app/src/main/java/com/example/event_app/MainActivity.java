package com.example.event_app;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    public void toggle(View v) {
       v.setEnabled(false);
       Log.d("successbro", "button disabled"); //loggging
        Button button = (Button) v;
        button.setText("Disabled");
    }
    public void handleText(View v){
        TextView t = findViewById(R.id.editText);
        String input  = t.getText().toString();
        ((TextView)findViewById(R.id.output)).setText(input);
        Toast.makeText(this, input, Toast.LENGTH_LONG).show();
        Log.d("input", input);

    }
}