package com.example.inplacereminder;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class PlaceManager extends AppCompatActivity {

    Button btnAddPlace;
    ImageButton ib_back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.place_manager);

        ib_back = findViewById(R.id.ib_back);
        ib_back.setOnClickListener(v -> finish());
    }
}
