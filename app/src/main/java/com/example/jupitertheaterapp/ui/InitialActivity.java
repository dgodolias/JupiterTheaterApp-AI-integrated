// app/src/main/java/com/example/jupitertheaterapp/ui/InitialActivity.java
package com.example.jupitertheaterapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jupitertheaterapp.R;

public class InitialActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial);

        ImageView logoImageView = findViewById(R.id.logoImageView);
        logoImageView.setAlpha(0f);
        logoImageView.animate().alpha(1f).setDuration(1500);

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(InitialActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, 2000);
    }
}