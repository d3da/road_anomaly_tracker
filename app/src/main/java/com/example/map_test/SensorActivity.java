package com.example.map_test;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class SensorActivity extends Activity implements SensorEventListener {
    private SensorManager mgr;
    private Sensor accelSensor;

    public SensorActivity() {
        mgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelSensor = mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
