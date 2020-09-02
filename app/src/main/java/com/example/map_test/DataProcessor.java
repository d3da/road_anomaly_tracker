package com.example.map_test;

import android.os.CountDownTimer;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DataProcessor extends CountDownTimer {

    double[] lastKnownLocation = {0f, 0f};

    int accelMeasureCount = 0;
    int locationMeasureCount = 0;

    private DataPointBuilder dataPointBuilder = null; // null is 'reset' value

    private static class DataPointBuilder {
        List<Float> magnitudes = new ArrayList<>();
        long timestamp;
        double[] location;

        public DataPointBuilder(long timestamp, double[] location) {
            this.timestamp = timestamp;
            this.location = location;
        }

        void addValue(float[] xyz) {
            magnitudes.add((float) Math.sqrt(Math.pow(xyz[0], 2) + Math.pow(xyz[1], 2) + Math.pow(xyz[2], 2)));
        }

        /**
         * returns a single 'bumpiness'-ish value
         *
         * which is currently the average magnitude of acceleration vectors in the last 100 millis
         *
         * todo look at the bigger picture
         */
        float preprocess() {
            float avg = 0;
            for (float f : magnitudes) {
                avg += f;
            }
            avg = (float) avg / magnitudes.size();

            return avg;
        }
    }




    public DataProcessor() {
        super(Long.MAX_VALUE, 1000);
        start(); //start timer
    }

    void addAccelData(long timestamp, int accuracy, float[] xyz) {
        if (dataPointBuilder == null) {
            dataPointBuilder = new DataPointBuilder(timestamp, lastKnownLocation);
        }

        dataPointBuilder.addValue(xyz);
    }


    void addLocationData(double lat, double lng) {
        lastKnownLocation[0] = lat;
        lastKnownLocation[1] = lng;
        locationMeasureCount++;

    }


    @Override
    public void onTick(long l) {

        if (dataPointBuilder == null) {
            Log.e("LOG", "no accel data? datapointbuilder is null");
            return;
        }

        float f = dataPointBuilder.preprocess();

        Log.e("LOG", String.format("\n\nlocation: %f/%f\ncount: accel %d, location %d", lastKnownLocation[0], lastKnownLocation[1],
                accelMeasureCount, locationMeasureCount));
        Log.e("LOG", String.format("\n\n\n\n\nBumpiness: %f\n", f));

        dataPointBuilder = null;

    }

    @Override
    public void onFinish() {
        Log.e("LOG", "TIMER FINISHED (shouldnt happen really)");
    }
}
