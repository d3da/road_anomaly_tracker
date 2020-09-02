package com.example.map_test;

import android.os.CountDownTimer;
import android.util.Log;

import java.util.ArrayList;

public class DataProcessor extends CountDownTimer {

    double[] lastKnownLocation = {0f, 0f};

    int accelMeasureCount = 0;
    int locationMeasureCount = 0;

    ArrayList<AccelEvent> accelBuffer = new ArrayList<>();

    private static class AccelEvent {
        float magnitude;
        long timestamp;
        int accuracy;

        double[] latLng;

        public AccelEvent(long _timestamp, int _accuracy, float[] xyz, double[] locLatLng) {
            timestamp = _timestamp;
            accuracy = _accuracy;

            magnitude = (float) Math.sqrt(Math.pow(xyz[0], 2) + Math.pow(xyz[1], 2) + Math.pow(xyz[2], 2));

            latLng = locLatLng;
        }
    }

//    private static class AccelBatch {
//        List<Float> magnitudes = new ArrayList<>();
//        long startTimestamp = 0;
//
//        public void addEvent()
//
//    }



    public DataProcessor() {
        super(Long.MAX_VALUE, 1000);
        start(); //start timer
    }

    void addAccelData(long timestamp, int accuracy, float[] xyz) {
        accelBuffer.add(new AccelEvent(timestamp, accuracy, xyz, lastKnownLocation));
    }


    void addLocationData(long timestamp, double lat, double lng) {
        lastKnownLocation[0] = lat;
        lastKnownLocation[1] = lng;
        locationMeasureCount++;

    }


    @Override
    public void onTick(long l) {

        if (accelBuffer.isEmpty()) {
            Log.e("LOG", "No acceleration data received :(");
            return;
        }

        float maxMag = 0;
        float minMag = Float.MAX_VALUE;
        double sumMag = 0;

        for (AccelEvent e : accelBuffer) {
            if (e.magnitude > maxMag) {
                maxMag = e.magnitude;
            }
            if (e.magnitude < minMag) {
                minMag = e.magnitude;
            }
            sumMag += e.magnitude;
        }
        float avgMag = (float) (sumMag / accelBuffer.size());


        Log.e("LOG", String.format("\n\nlocation: %f/%f\ncount: accel %d, location %d", lastKnownLocation[0], lastKnownLocation[1],
                accelMeasureCount, locationMeasureCount));
        Log.e("LOG", String.format("Got batch measures of size %d\n max: %f, min: %f, avg: %f", accelBuffer.size(), maxMag, minMag, avgMag));

        accelBuffer.clear();
    }

    @Override
    public void onFinish() {
        Log.e("LOG", "TIMER FINISHED (for some odd reason)");
    }
}
