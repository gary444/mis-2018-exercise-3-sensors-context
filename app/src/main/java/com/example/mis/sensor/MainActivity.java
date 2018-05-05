package com.example.mis.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener, SeekBar.OnSeekBarChangeListener {


    //sensor variables
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private CustomGraphView sensor_data_view;
    private CustomGraphView fft_view;

    private SeekBar window_size_seek_bar;
    private SeekBar sample_rate_seek_bar;




    private final int DARK_BG = 0xff222222;
    private final int LIGHT_BG = 0xffaaaaaa;
    private final int GRAPH_X_RESOLUTION = 50;
    private final float SENSOR_GRAPH_SCALE_Y = 0.005f;
    private final float FFT_GRAPH_SCALE_Y = 0.3f;
    private final int FFT_WINDOW_SIZE = 64;

    private Handler handler;

    private ArrayList<AccelOutput> accelData = new ArrayList<>();

    private static final String TAG = MainActivity.class.getSimpleName();

    //testing
//    private int refresh_count = 0;
//    private long lastReport = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!sensorInit()){
            Toast.makeText(this, "Could not connect to sensors", Toast.LENGTH_LONG).show();
        }

        sensor_data_view = findViewById(R.id.sensor_input_view);
        fft_view = findViewById(R.id.fft_view);

        sample_rate_seek_bar = findViewById(R.id.sample_rate_seek_bar);
        window_size_seek_bar = findViewById(R.id.window_size_seek_bar);

        visualiseSensorData();

        //start FFT timer
        handler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);

        sample_rate_seek_bar.setOnSeekBarChangeListener(this);
        window_size_seek_bar.setOnSeekBarChangeListener(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);

        //deregister as seek bar listener?

        //stop handler task 
    }



    private boolean sensorInit(){

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Log.d(TAG, "sensorInit: accelerometer found");
        }
        else {
            Log.d(TAG, "sensorInit: accelerometer not found");
            return false;
        }
        return true;
    }

    //help from https://examples.javacodegeeks.com/android/core/os/handler/android-handler-example/
//    class Task implements Runnable {
//        @Override
//        public void run() {
//
//            handler.post(new Runnable() {
//                @Override
//                public void run() {
//
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                        Log.d(TAG, "run: thread sleep interrupted");
//                    }
//
//                    //create list of double values if there is enough accelerometer data
//                    if (accelData.size() > FFT_WINDOW_SIZE){
//                        double[] magnitude_data = new double[FFT_WINDOW_SIZE];
//                        for (int j = 0; j < FFT_WINDOW_SIZE; j++){
//                            float FFT_SCALING = 0.01f;
//                            magnitude_data[j] = FFT_SCALING * accelData.get(accelData.size() - (j + 1)).magnitude;
//                        }
//
//                        //start fft process
////                        new FFTAsynctask(FFT_WINDOW_SIZE).execute(magnitude_data);
//                    }
//
//                    //call this task again
//                    handler.post(this);
//
//                }
//            });
//        }
//    }


    //sends sensor data to line graph display
    //TODO consider calling this on an async task, as FFT display?
    private void visualiseSensorData(){

        ArrayList<DrawableDataSet> dataSets = new ArrayList<>();

        //for 4 lines: x,y,z,magnitude
        for (int lineNum = 0; lineNum < 4; lineNum++){

            float[] dataToDraw = new float[GRAPH_X_RESOLUTION];
            int dataReadPos = accelData.size() - 1;

            //copy data from end of array
            for (int i = GRAPH_X_RESOLUTION - 1; i >= 0; i--){

                if (dataReadPos >= 0){
                    dataToDraw[i] = (accelData.get(dataReadPos).get(lineNum) * SENSOR_GRAPH_SCALE_Y) + 0.5f;//scale and shift
                    dataReadPos--;
                }
            }

            DrawableDataSet ds = new DrawableDataSet(dataToDraw, AccelOutput.getColour(lineNum), "line" + (1));
            dataSets.add(ds);
        }

        sensor_data_view.updateData(dataSets, DARK_BG);


//        final ArrayList<DrawableDataSet> final_sets = dataSets;
//        runOnUiThread(new Runnable() {
//            public void run() {
//                sensor_data_view.updateData(final_sets, DARK_BG);
//            }
//        });


    }

    private void visualiseFFTData(double[] values){



        ArrayList<DrawableDataSet> dataSets = new ArrayList<>();
        float[] dataToDraw = new float[values.length];
        //cast
        for (int i = 0; i < values.length; i++){
            dataToDraw[i] = (float)values[i];
            //scale?
        }

        DrawableDataSet ds = new DrawableDataSet(dataToDraw, 0xffff0000, "fft data");
        dataSets.add(ds);

        fft_view.updateData(dataSets, LIGHT_BG);

    }

    //sensor methods ---------------------------------------------------
    @Override
    public void onSensorChanged(SensorEvent event) {

        //if accel
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            float[] values = event.values;
            accelData.add(new AccelOutput(values[0], values[1], values[2]));

//            refresh_count++;
//            if ((int)(uptimeMillis() / 1000) != lastReport){
//                Log.d(TAG, "onSensorChanged: refresh rate = " + refresh_count + "Hz");
//                refresh_count = 0;
//                lastReport = uptimeMillis()/1000;
//            }

            visualiseSensorData();
            new FFTAsynctask(FFT_WINDOW_SIZE).execute(accelData);


            //create list of double values if there is enough accelerometer data
//            if (accelData.size() > FFT_WINDOW_SIZE){
//                double[] magnitude_data = new double[FFT_WINDOW_SIZE];
//                for (int j = 0; j < FFT_WINDOW_SIZE; j++){
//                    float FFT_SCALING = 0.01f;
//                    magnitude_data[j] = FFT_SCALING * accelData.get(accelData.size() - (j + 1)).magnitude;
//                }
//
//                //start fft process
//                new FFTAsynctask(FFT_WINDOW_SIZE).execute(magnitude_data);
//            }

//            new SensorGraphTask().execute(accelData);


        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }




    /**
     * Implements the fft functionality as an async task
     * FFT(int n): constructor with fft length
     * fft(double[] x, double[] y)
     */

//    private class FFTAsynctask extends AsyncTask<double[], Void, double[]> {

    private class FFTAsynctask extends AsyncTask<ArrayList<AccelOutput>, Void, double[]> {

        private int wsize; //window size must be power of 2

        // constructor to set window size
        FFTAsynctask(int wsize) {
            this.wsize = wsize;
        }

        @Override
        protected double[] doInBackground(ArrayList<AccelOutput>... data) {

            double[] values = new double[wsize];

            //create list of double values if there is enough accelerometer data
            if (data[0].size() > wsize){

                for (int j = 0; j < wsize; j++){
                    float FFT_SCALING = 0.01f;
                    values[j] = FFT_SCALING * data[0].get(data[0].size() - (j + 1)).magnitude;
                }
            }

            double[] realPart = values; // actual acceleration values
            double[] imagPart = new double[wsize]; // init empty

            /**
             * Init the FFT class with given window size and run it with your input.
             * The fft() function overrides the realPart and imagPart arrays!
             */
            FFT fft = new FFT(wsize);
            fft.fft(realPart, imagPart);
            //init new double array for magnitude (e.g. frequency count)
            double[] magnitude = new double[wsize/2 + 1];


            //fill array with magnitude values of the distribution
            for (int i = 0; (wsize/2 + 1) > i ; i++) {
                magnitude[i] = FFT_GRAPH_SCALE_Y * Math.sqrt(Math.pow(realPart[i], 2) + Math.pow(imagPart[i], 2));

//                magnitude[i] = 20 * Math.log10(Math.sqrt(Math.pow(realPart[i], 2) + Math.pow(imagPart[i], 2)));
            }

            return magnitude;

        }

        @Override
        protected void onPostExecute(double[] values) {
            //hand over values to global variable after background task is finished
            visualiseFFTData(values);


        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        if (seekBar.getId() == R.id.sample_rate_seek_bar){


            //TODO do stuff
        }
        else if (seekBar.getId() == R.id.window_size_seek_bar){

            //TODO do stuff
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }


//    private class SensorGraphTask extends AsyncTask<ArrayList<AccelOutput>, Void, Void>{
//
//        @Override
//        protected Void doInBackground(ArrayList<AccelOutput>... arrayLists) {
//
//            visualiseSensorData();
//            return null;
//        }
//    }



}
