package com.example.inplacereminder;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class PlaceManager extends AppCompatActivity {

    Button btnAddPlace, btnEditPlace, btnDelete;
    ImageButton ib_back;
    ListView listPlaces;
    DB_OpenHelper dbHelper;
    ArrayAdapter<String> adapter;
    List<String> placeNames;
    List<Long> placeIds;
    int selectedPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.place_manager);

        dbHelper = new DB_OpenHelper(this);
        ib_back = findViewById(R.id.ib_back);
        ib_back.setOnClickListener(v -> finish());

        btnAddPlace = findViewById(R.id.btnAddPlace);
        btnEditPlace = findViewById(R.id.btnEditPlace);
        btnDelete = findViewById(R.id.btnDelete);
        listPlaces = findViewById(R.id.listPlaces);

        placeNames = new ArrayList<>();
        placeIds = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, placeNames);
        listPlaces.setAdapter(adapter);

        // Load places from database
        loadPlaces();

        // Add button click listener
        btnAddPlace.setOnClickListener(v -> {
            Intent intent = new Intent(PlaceManager.this, PlaceEditor.class);
            startActivity(intent);
        });

        // Edit button click listener
        btnEditPlace.setOnClickListener(v -> {
            if (selectedPosition == -1) {
                Toast.makeText(PlaceManager.this, "Please select a place to edit", Toast.LENGTH_SHORT).show();
                return;
            }
            editSelectedPlace();
        });

        // Delete button click listener
        btnDelete.setOnClickListener(v -> {
            if (selectedPosition == -1) {
                Toast.makeText(PlaceManager.this, "Please select a place to delete", Toast.LENGTH_SHORT).show();
                return;
            }
            deleteSelectedPlace();
        });

        // ListView item click listener
        listPlaces.setOnItemClickListener((parent, view, position, id) -> {
            selectedPosition = position;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list when returning from PlaceEditor
        loadPlaces();
    }

    private void loadPlaces() {
        placeNames.clear();
        placeIds.clear();

        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.query(
                     "places",
                     new String[]{"id", "place_name"},
                     null,
                     null,
                     null,
                     null,
                     "place_name ASC"
             )) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    String name = cursor.getString(1);
                    placeIds.add(id);
                    placeNames.add(name);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading places", Toast.LENGTH_SHORT).show();
        }

        adapter.notifyDataSetChanged();
        selectedPosition = -1;
    }

    private void editSelectedPlace() {
        if (selectedPosition >= 0 && selectedPosition < placeIds.size()) {
            long placeId = placeIds.get(selectedPosition);
            String placeName = placeNames.get(selectedPosition);

            // Fetch place details from database
            try (SQLiteDatabase db = dbHelper.getReadableDatabase();
                 Cursor cursor = db.query(
                         "places",
                         new String[]{"lat", "lon"},
                         "id = ?",
                         new String[]{String.valueOf(placeId)},
                         null,
                         null,
                         null
                 )) {
                if (cursor != null && cursor.moveToFirst()) {
                    double latitude = cursor.getDouble(0);
                    double longitude = cursor.getDouble(1);

                    Intent intent = new Intent(PlaceManager.this, PlaceEditor.class);
                    intent.putExtra("placeId", placeId);
                    intent.putExtra("placeName", placeName);
                    intent.putExtra("latitude", latitude);
                    intent.putExtra("longitude", longitude);
                    startActivity(intent);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading place details", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void deleteSelectedPlace() {
        if (selectedPosition >= 0 && selectedPosition < placeIds.size()) {
            long placeId = placeIds.get(selectedPosition);

            try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
                int rows = db.delete("places", "id = ?", new String[]{String.valueOf(placeId)});
                if (rows > 0) {
                    Toast.makeText(this, "Place deleted successfully!", Toast.LENGTH_SHORT).show();
                    loadPlaces();
                } else {
                    Toast.makeText(this, "Failed to delete place", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error deleting place", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
