package com.example.mis.sensor;

import android.content.Context;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.mis.sensor.FFT;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener  {


    //sensor variables
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
//    private Sensor mGravity;

    private CustomGraphView sensor_data_view;
    private CustomGraphView fft_view;


    private final int DARK_BG = 0xff222222;
    private final int LIGHT_BG = 0xffaaaaaa;
    private final int GRAPH_X_RESOLUTION = 50;
    private final float GRAPH_SCALE_Y = 0.005f;
    private final int WINDOW_SIZE = 64;

    private Handler handler;

    private ArrayList<AccelOutput> accelData = new ArrayList<>();

    private static final String TAG = MainActivity.class.getSimpleName();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!sensorInit()){
            Toast.makeText(this, "Could not connect to sensors", Toast.LENGTH_LONG).show();
        }

        sensor_data_view = findViewById(R.id.sensor_input_view);
        fft_view = findViewById(R.id.fft_view);

        visualiseSensorData();

        //start FFT timer
        handler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);

        new Thread(new Task()).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        
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
    class Task implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            handler.post(new Runnable() {
                @Override
                public void run() {

                    //create list of double values if there is enough accelerometer data
                    if (accelData.size() > WINDOW_SIZE){
                        double[] magnitude_data = new double[WINDOW_SIZE];
                        for (int j = 0; j < WINDOW_SIZE; j++){
                            float FFT_SCALING = 0.01f;
                            magnitude_data[j] = FFT_SCALING * accelData.get(accelData.size() - (j + 1)).magnitude;
                        }

                        //start fft process
                        new FFTAsynctask(WINDOW_SIZE).execute(magnitude_data);
                    }


                    handler.post(this);

                }
            });
        }
    }


    //sends sensor data to line graph display
    private void visualiseSensorData(){

        ArrayList<DrawableDataSet> dataSets = new ArrayList<>();

        //for 4 lines: x,y,z,magnitude
        for (int lineNum = 0; lineNum < 4; lineNum++){

            float[] dataToDraw = new float[GRAPH_X_RESOLUTION];
            int dataReadPos = accelData.size() - 1;

            //copy data from end of array
            for (int i = GRAPH_X_RESOLUTION - 1; i >= 0; i--){

                if (dataReadPos >= 0){
                    dataToDraw[i] = (accelData.get(dataReadPos).get(lineNum) * GRAPH_SCALE_Y) + 0.5f;//scale and shift
                    dataReadPos--;
                }
            }

            DrawableDataSet ds = new DrawableDataSet(dataToDraw, AccelOutput.getColour(lineNum), "line" + (1));
            dataSets.add(ds);
        }

        sensor_data_view.updateData(dataSets, DARK_BG);

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
            visualiseSensorData();
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

    private class FFTAsynctask extends AsyncTask<double[], Void, double[]> {

        private int wsize; //window size must be power of 2

        // constructor to set window size
        FFTAsynctask(int wsize) {
            this.wsize = wsize;
        }

        @Override
        protected double[] doInBackground(double[]... values) {


            double[] realPart = values[0].clone(); // actual acceleration values
            double[] imagPart = new double[wsize]; // init empty

            /**
             * Init the FFT class with given window size and run it with your input.
             * The fft() function overrides the realPart and imagPart arrays!
             */
            FFT fft = new FFT(wsize);
            fft.fft(realPart, imagPart);
            //init new double array for magnitude (e.g. frequency count)
            double[] magnitude = new double[wsize];


            //fill array with magnitude values of the distribution
            for (int i = 0; wsize > i ; i++) {
                magnitude[i] = Math.sqrt(Math.pow(realPart[i], 2) + Math.pow(imagPart[i], 2));
            }

            return magnitude;

        }

        @Override
        protected void onPostExecute(double[] values) {
            //hand over values to global variable after background task is finished
            visualiseFFTData(values);

//            Toast.makeText(MainActivity.this, "I did some FFT!", Toast.LENGTH_SHORT).show();

        }
    }




}
