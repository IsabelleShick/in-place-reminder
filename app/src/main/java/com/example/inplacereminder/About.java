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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        Button btnEffect = findViewById(R.id.btnEffect);
        ib_back = findViewById(R.id.ib_back);

        ib_back.setOnClickListener(v -> finish());

        ImageView iv_thanking_kitty = findViewById(R.id.iv_thanking_kitty);
        Glide.with(this)
                .asGif()
                .load(R.drawable.thanking_kitty)
                .dontTransform()
                .into(iv_thanking_kitty);

        btnEffect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Code to show effect information
            }
        });
    }
}