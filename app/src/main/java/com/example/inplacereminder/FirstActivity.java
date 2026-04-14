package com.example.inplacereminder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class FirstActivity extends AppCompatActivity {

    ImageView ivGif;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This line links the Java code to your layout file.
        setContentView(R.layout.first_activity);

        ivGif = findViewById(R.id.ivGif);

        // This part is for displaying your spinning cat GIF.
        Glide.with(this)
                .asGif()
                .load(R.drawable.cat_spinning)
                .dontTransform()
                .into(ivGif);

        // --- THIS IS THE DELAY LOGIC ---
        // It creates a new background thread to handle the delay.
        Thread t = new Thread() {
            public void run() {
                try {
                    // 1. Pause the thread for 3000 milliseconds (3 seconds).
                    sleep(3000);

                    // 2. After the 3-second sleep, prepare to start the next activity.
                    Intent i = new Intent(FirstActivity.this, MainActivity.class);
                    startActivity(i);

                    // 3. Destroy the current activity (FirstActivity).
                    // This is important so the user can't press the back button
                    // to return to the splash screen.
                    finish();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        // 4. Start the thread, which begins the 3-second countdown.
        t.start();
    }
}