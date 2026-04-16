package com.example.inplacereminder;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class About extends AppCompatActivity {

    ImageButton ib_back;
    Button btnEffect;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        Button btnEffect = findViewById(R.id.btnEffect);
        ib_back = findViewById(R.id.ib_back);

        ib_back.setOnClickListener(v -> finish());

        ImageView iv_thanking_kitty = findViewById(R.id.ivThanking_kitty);
        ImageView ivConfiti = findViewById(R.id.ivConfiti);
        // make the confiti gif invisible
        ivConfiti.setVisibility(View.INVISIBLE);

        Glide.with(this)
                .asGif()
                .load(R.drawable.thanking_kitty)
                .dontTransform()
                .into(iv_thanking_kitty);

        btnEffect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // make the confiti gif visible
                ivConfiti.setVisibility(View.VISIBLE);

                // Load and display the GIF using Glide
                Glide.with(About.this)
                        .asGif()
                        .load(R.drawable.confiti)
                        .dontTransform()
                        .into(ivConfiti);

                // make the ivConfiti invisible after 3 seconds(3000 milliseconds)
                ivConfiti.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ivConfiti.setVisibility(View.INVISIBLE);
                    }
                }, 3000);

            }
        });
    }
}