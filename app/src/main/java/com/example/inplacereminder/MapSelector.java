package com.example.inplacereminder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapSelector extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {
    private MapView mapView;
    private GoogleMap googleMap;
    private Marker currentMarker;
    private LatLng selectedLocation;
    private Button btnConfirm;
    private ImageButton ib_back;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "MapSelector";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_selector);

        // Check Google Play Services availability
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, 2404).show();
            } else {
                Toast.makeText(this, "This device is not supported for Google Maps", Toast.LENGTH_LONG).show();
                finish();
            }
            return;
        }

        mapView = findViewById(R.id.mapView);
        btnConfirm = findViewById(R.id.btnConfirm);
        ib_back = findViewById(R.id.ib_back);

        ib_back.setOnClickListener(v -> finish());

        // Get initial location from intent if available
        if (getIntent() != null) {
            double lat = getIntent().getDoubleExtra("latitude", Double.NaN);
            double lon = getIntent().getDoubleExtra("longitude", Double.NaN);
            if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
                selectedLocation = new LatLng(lat, lon);
            }
        }

        btnConfirm.setOnClickListener(v -> confirmLocation());

        // Request location permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST_CODE);
            }
        }

        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        } else {
            Log.e(TAG, "MapView is null - layout might not have mapView element");
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        try {
            googleMap = map;
            Log.d(TAG, "Map ready callback received");

            googleMap.setOnMapClickListener(this);

            // Enable MyLocation layer if the permission is granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                googleMap.setMyLocationEnabled(true);
                Log.d(TAG, "Location enabled on map");
            } else {
                Log.d(TAG, "Location permission not granted");
            }

            // If a location was passed, show it
            if (selectedLocation != null) {
                Log.d(TAG, "Moving camera to: " + selectedLocation.latitude + ", " + selectedLocation.longitude);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15));
                currentMarker = googleMap.addMarker(new MarkerOptions()
                        .position(selectedLocation)
                        .title("Selected Location"));
            } else {
                // Default to world view
                Log.d(TAG, "No location provided, showing world view");
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 2));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onMapReady: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading map: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        selectedLocation = latLng;

        // Remove old marker if exists
        if (currentMarker != null) {
            currentMarker.remove();
        }

        // Add new marker
        currentMarker = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Selected Location"));

        Toast.makeText(this, "Location selected: " + latLng.latitude + ", " + latLng.longitude, Toast.LENGTH_SHORT).show();
    }

    private void confirmLocation() {
        if (selectedLocation == null) {
            Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent result = new Intent();
        result.putExtra("latitude", selectedLocation.latitude);
        result.putExtra("longitude", selectedLocation.longitude);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted");
                if (googleMap != null) {
                    try {
                        googleMap.setMyLocationEnabled(true);
                    } catch (SecurityException e) {
                        Log.e(TAG, "Error enabling location: " + e.getMessage());
                    }
                }
            } else {
                Log.d(TAG, "Location permission denied");
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}

