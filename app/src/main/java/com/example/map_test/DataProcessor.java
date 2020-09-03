package com.example.map_test;

import android.os.CountDownTimer;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataProcessor extends CountDownTimer {

    double[] lastKnownLocation = {0f, 0f};
    boolean locationKnown = false;

    int accelMeasureCount = 0;
    int locationMeasureCount = 0;

    int tickIteration = 0;
    int markerEvery = 100;

    private DataPointBuilder dataPointBuilder = null; // null is 'reset' value

    MapsActivity activity;
    RequestQueue queue;
    String url = "http://82.197.215.243:5000/post";

    boolean sendData = true;


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
         * which is currently the average magnitude of acceleration vectors in the last N millis
         *
         * todo look at the bigger picture (in post)
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




    public DataProcessor(MapsActivity activity) {
        super(Long.MAX_VALUE, 500);
        this.activity = activity;
        this.queue = Volley.newRequestQueue(activity);
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

        locationKnown = true;
    }


    @Override
    public void onTick(long l) {

        if (dataPointBuilder == null) {
            Log.e("LOG", "no accel data? datapointbuilder is null");
            return;
        }

        float f = dataPointBuilder.preprocess();

        /* Do something with data here */
        Log.e("LOG", String.format("\n\nlocation: %f/%f\ncount: accel %d, location %d", lastKnownLocation[0], lastKnownLocation[1],
                accelMeasureCount, locationMeasureCount));
        Log.e("LOG", String.format("\n\n\n\n\nBumpiness: %f\n", f));

        // 'reset' the builder
        dataPointBuilder = null;


        // send request to server
        if (locationKnown) {
            sendToServer(f, lastKnownLocation);

            if (tickIteration % markerEvery == 0) {
                activity.setMarker(lastKnownLocation[0], lastKnownLocation[1]);
            }
        }
        tickIteration++;
    }

    @Override
    public void onFinish() {
        Log.e("LOG", "TIMER FINISHED (shouldnt happen really)");
    }


    public void sendToServer(final float magnitude, final double[] location) {

        if (!sendData) return;

        StringRequest req = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.e("LOG", String.format("server responded with \n%s", response));

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("magnitude", Float.toString(magnitude));
                params.put("longitude", Double.toString(location[0]));
                params.put("latitude", Double.toString(location[1]));

                return params;

            }
        };

        queue.add(req);
    }

}
