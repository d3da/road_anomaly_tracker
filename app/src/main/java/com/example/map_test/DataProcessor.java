package com.example.map_test;

import android.os.CountDownTimer;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataProcessor extends CountDownTimer {

    double[] lastKnownLocation = {0f, 0f};
    float lastKnownLocationSpeed = 0;
    boolean locationKnown = false;

    int accelMeasureCount = 0;
    int locationMeasureCount = 0;

    int tickIteration = 0;
    int markerEvery = 10;

    private DataPointBuilder dataPointBuilder = null; // null is 'reset' value

    MapsActivity activity;
    RequestQueue queue;
    String url = "http://82.197.215.243:5000";

    boolean sendData = false;


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


    void addLocationData(double lat, double lng, float speed) {
        lastKnownLocation[0] = lat;
        lastKnownLocation[1] = lng;
        lastKnownLocationSpeed = speed;
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
            tickIteration++;
        }
    }

    @Override
    public void onFinish() {
        Log.e("LOG", "TIMER FINISHED (shouldnt happen really)");
    }


    public void sendToServer(final float magnitude, final double[] location) {

        if (!sendData) return;

        if (magnitude < 1f) return;
        if (lastKnownLocationSpeed < 1f) return;


        StringRequest req = new StringRequest(Request.Method.POST, url+"/post",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
//                        Log.e("LOG", String.format("server responded with \n%s", response));
                        ;

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


    /**
     * get a csv file from the server
     *, remove all the markers from the map
     * and add new markers
     */
    public void getServerLocationData(final GoogleMap gMap) {
       StringRequest req = new StringRequest(Request.Method.GET, url+"/csv2",
                new Response.Listener<String>() {
                    /**
                     * Attempt to parse csv data by hand lol
                     * #rapidprototyping
                     */
                    @Override
                    public void onResponse(String response) {
                        try {
//                            Log.e("LOG", response);
                            BufferedReader read = new BufferedReader(new StringReader(response));
                            String line;
                            int i = 0;
                            while ((line = read.readLine()) != null) {
                                i++;
                                Log.e("LOG", line);
                                String[] vals = line.split(",");
                                if (vals.length != 4) {
                                    Log.e("LOG", String.format("malformed csv line\n%s", line));
                                    return;
                                }
                                String ip = vals[0];
                                String lngStr = vals[1];
                                String latStr = vals[2];
                                String magStr = vals[3];

                                Double lng = Double.valueOf(lngStr);
                                Double lat = Double.valueOf(latStr);
                                Float mag = Float.valueOf(magStr);

                                LatLng pos = new LatLng(lng, lat);

                                float hue = Float.max(1, Float.min(120f, 120 - (mag / 20f * 120f)));

                                BitmapDescriptor bmp = BitmapDescriptorFactory.defaultMarker(hue);

                                gMap.addMarker(new MarkerOptions().position(pos).icon(bmp));
                                gMap.moveCamera(CameraUpdateFactory.newLatLng(pos));
//                                if (i > 50) {
//                                    break;
//                                }
                            }

                        } catch (IOException e) {
                            Log.e("LOG", "IOEXCEPTION BRO");
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }
        );

        queue.add(req);

    }
}
