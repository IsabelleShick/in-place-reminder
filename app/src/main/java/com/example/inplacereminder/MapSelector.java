package com.example.inplacereminder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_selector);

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

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.setOnMapClickListener(this);

        // Enable MyLocation layer if the permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }

        // If a location was passed, show it
        if (selectedLocation != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15));
            currentMarker = googleMap.addMarker(new MarkerOptions()
                    .position(selectedLocation)
                    .title("Selected Location"));
        } else {
            // Default to world view
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 2));
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}

