package com.example.inplacereminder;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PlaceEditor extends AppCompatActivity {
    private long placeId = -1;
    private EditText etPlaceName;
    private EditText etLatitude;
    private EditText etLongitude;
    private Button btnSave;
    private Button btnDelete;
    private Button btnThisPlace;
    private Button btnSelectFromMap;
    private ImageButton ib_back;
    private DB_OpenHelper dbHelper;
    private LocationManager locationManager;
    private LocationListener locationListener;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int MAP_SELECTOR_REQUEST_CODE = 101;
    private static final long LOCATION_UPDATE_TIMEOUT = 15000; // 15 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.place_editor);

        dbHelper = new DB_OpenHelper(this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        etPlaceName = findViewById(R.id.etPlaceName);
        etLatitude = findViewById(R.id.etLatitude);
        etLongitude = findViewById(R.id.etLongitude);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        btnThisPlace = findViewById(R.id.btnThisPlace);
        btnSelectFromMap = findViewById(R.id.btnSelectFromMap);
        ib_back = findViewById(R.id.ib_back);

        ib_back.setOnClickListener(v -> finish());

        // Check if editing existing place
        if (getIntent() != null && getIntent().hasExtra("placeId")) {
            placeId = getIntent().getLongExtra("placeId", -1);
            String placeName = getIntent().getStringExtra("placeName");
            double latitude = getIntent().getDoubleExtra("latitude", 0.0);
            double longitude = getIntent().getDoubleExtra("longitude", 0.0);

            if (placeName != null) etPlaceName.setText(placeName);
            if (latitude != 0.0) etLatitude.setText(String.valueOf(latitude));
            if (longitude != 0.0) etLongitude.setText(String.valueOf(longitude));

            btnDelete.setOnClickListener(v -> deletePlace());
        } else {
            btnDelete.setEnabled(false);
        }

        btnThisPlace.setOnClickListener(v -> fetchCurrentLocation());
        btnSelectFromMap.setOnClickListener(v -> openMapSelector());
        btnSave.setOnClickListener(v -> savePlace());
    }

    private void fetchCurrentLocation() {
        // Check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        }

        // Check if location services are enabled
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        }

        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGPSEnabled && !isNetworkEnabled) {
            // Location services are disabled
            showLocationSettingsDialog();
            return;
        }

        try {
            Toast.makeText(this, "Fetching location...", Toast.LENGTH_SHORT).show();

            // Try to get last known location from GPS provider
            Location location = null;
            if (isGPSEnabled) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            // If GPS not available or no location, try network provider
            if (location == null && isNetworkEnabled) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            // If still null, try passive provider
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }

            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                etLatitude.setText(String.format("%.6f", latitude));
                etLongitude.setText(String.format("%.6f", longitude));

                Toast.makeText(this, "Location fetched successfully!", Toast.LENGTH_SHORT).show();
            } else {
                // No last known location, try to request updates
                requestLocationUpdates();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error fetching location", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestLocationUpdates() {
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        }

        // Create location listener
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                etLatitude.setText(String.format("%.6f", latitude));
                etLongitude.setText(String.format("%.6f", longitude));

                Toast.makeText(PlaceEditor.this, "Location fetched successfully!", Toast.LENGTH_SHORT).show();

                // Stop listening after getting location
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(PlaceEditor.this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationManager.removeUpdates(locationListener);
                    }
                } else {
                    locationManager.removeUpdates(locationListener);
                }
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
        };

        try {
            // Request updates from GPS or Network
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (isNetworkEnabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                                1000, 0, locationListener);
                    }
                } else {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            1000, 0, locationListener);
                }
            }

            if (isGPSEnabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                1000, 0, locationListener);
                    }
                } else {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            1000, 0, locationListener);
                }
            }

            // Set a timeout to stop listening if no location is found
            btnThisPlace.postDelayed(this::stopLocationUpdates, LOCATION_UPDATE_TIMEOUT);
        } catch (SecurityException e) {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationUpdates() {
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
                Toast.makeText(this, "Location update timeout. Please try again.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void openMapSelector() {
        Intent intent = new Intent(this, MapSelector.class);

        // Pass current latitude and longitude if they exist
        String latStr = etLatitude.getText().toString().trim();
        String lonStr = etLongitude.getText().toString().trim();

        if (!latStr.isEmpty() && !lonStr.isEmpty()) {
            try {
                double latitude = Double.parseDouble(latStr);
                double longitude = Double.parseDouble(lonStr);
                intent.putExtra("latitude", latitude);
                intent.putExtra("longitude", longitude);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        startActivityForResult(intent, MAP_SELECTOR_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MAP_SELECTOR_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            double latitude = data.getDoubleExtra("latitude", Double.NaN);
            double longitude = data.getDoubleExtra("longitude", Double.NaN);

            if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
                etLatitude.setText(String.format("%.6f", latitude));
                etLongitude.setText(String.format("%.6f", longitude));
                Toast.makeText(this, "Location selected from map", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showLocationSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Location Services Disabled")
                .setMessage("Location services are disabled. Please enable them in Settings to get your current location.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    // Open location settings
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    private void savePlace() {
        String placeName = etPlaceName.getText().toString().trim();
        String latStr = etLatitude.getText().toString().trim();
        String lonStr = etLongitude.getText().toString().trim();

        if (placeName.isEmpty()) {
            Toast.makeText(this, "Place name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (latStr.isEmpty() || lonStr.isEmpty()) {
            Toast.makeText(this, "Latitude and longitude cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        double latitude, longitude;
        try {
            latitude = Double.parseDouble(latStr);
            longitude = Double.parseDouble(lonStr);

            // Validate latitude and longitude ranges
            if (latitude < -90 || latitude > 90) {
                Toast.makeText(this, "Latitude must be between -90 and 90", Toast.LENGTH_SHORT).show();
                return;
            }
            if (longitude < -180 || longitude > 180) {
                Toast.makeText(this, "Longitude must be between -180 and 180", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid latitude or longitude format", Toast.LENGTH_SHORT).show();
            return;
        }

        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("place_name", placeName);
            values.put("lat", latitude);
            values.put("lon", longitude);

            if (placeId == -1) {
                // Insert new place
                long newRowId = db.insert("places", null, values);
                if (newRowId != -1) {
                    Toast.makeText(this, "Place saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Error saving place.", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Update existing place
                int rows = db.update("places", values, "id = ?", new String[]{String.valueOf(placeId)});
                if (rows > 0) {
                    Toast.makeText(this, "Place updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "No place updated.", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void deletePlace() {
        if (placeId == -1) {
            Toast.makeText(this, "Cannot delete: place not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            int rows = db.delete("places", "id = ?", new String[]{String.valueOf(placeId)});
            if (rows > 0) {
                Toast.makeText(this, "Place deleted successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "No place deleted.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}


