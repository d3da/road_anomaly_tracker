package com.example.map_test;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener {

    // Maps
    private GoogleMap mMap;

    // Location services
    private boolean requestingLocationUpdates = false;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;

    // Sensor services
    private SensorManager sensMgr;
    private Sensor linearAccelSensor;

    // Preprocessing
    private DataProcessor dataProcessor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //// From template
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        // start data preprocessor
        dataProcessor = new DataProcessor(this);


        // activate switch logic
        Switch toggle = findViewById(R.id.switch_id);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                dataProcessor.sendData = b;
            }
        });
        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dataProcessor.getServerLocationData(mMap);
            }
        });

        /// Start location services

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (!requestingLocationUpdates) {
            startLocationUpdates();
        }

        startSensorUpdates();

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!requestingLocationUpdates) {
            startLocationUpdates();
        }


    }

    private void startSensorUpdates() {
        sensMgr = (SensorManager) getSystemService(SENSOR_SERVICE);

        for (Sensor s : sensMgr.getSensorList(Sensor.TYPE_ALL)) {
            Log.e("LOG", String.format("Found sensor %s", s.getName()));
        }
        linearAccelSensor = sensMgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        sensMgr.registerListener(this, linearAccelSensor, SensorManager.SENSOR_DELAY_FASTEST);

    }

    private void startLocationUpdates() {

        LocationRequest locationRequest = LocationRequest.create();
        long locationUpdateInterval = 1000;
        locationRequest.setInterval(locationUpdateInterval);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        double wayLatitude = location.getLatitude();
                        double wayLongitude = location.getLongitude();
                        float speedms = location.getSpeed();
//                        Log.e("LOG", String.format("%f, %f", wayLatitude, wayLongitude));

//                        LatLng loc = new LatLng(wayLatitude, wayLongitude);
//                        mMap.addMarker(new MarkerOptions().position(loc).title("My Location"));
//                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 20f));

                        dataProcessor.addLocationData(wayLatitude, wayLongitude, speedms);
                    }
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 420);
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int accuracy = sensorEvent.accuracy;
        long timestamp = sensorEvent.timestamp;

        dataProcessor.addAccelData(timestamp, accuracy, sensorEvent.values.clone());

//        float xVal = sensorEvent.values[0];
//        float yVal = sensorEvent.values[1];
//        float zVal = sensorEvent.values[2];
//        Log.e("LOG", String.format("time: %d\tsensor: %s\tacc: %d\tvalues: %f, %f, %f", timestamp, sensorName, accuracy, xVal, yVal, zVal));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.e("LOG", String.format("Accuracy of %s changed to %d", sensor.getName(), accuracy));

    }

    public void setMarker(double lat, double lng) {
        LatLng loc = new LatLng(lat, lng);
        mMap.addMarker(new MarkerOptions().position(loc).title("My Location"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 20f));
    }

}
