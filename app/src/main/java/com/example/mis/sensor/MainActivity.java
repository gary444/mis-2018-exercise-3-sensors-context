package com.example.mis.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener, SeekBar.OnSeekBarChangeListener, View.OnClickListener {


    //sensor variables
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    //views
    private CustomGraphView sensor_data_view;
    private CustomGraphView fft_view;

    private TextView window_size_label;
    private TextView sample_rate_label;

    private SeekBar window_size_seek_bar;
    private SeekBar sample_rate_seek_bar;

    private Button test_btn;

    //visual parameters
    private final int DARK_BG = 0xff222222;
    private final int LIGHT_BG = 0xffaaaaaa;
    private final int GRAPH_X_RESOLUTION = 50;
    private final float SENSOR_GRAPH_SCALE_Y = 0.005f;
    private final float FFT_GRAPH_SCALE_Y = 0.3f;

    //sensor parameters
    private int SENSOR_SAMPLE_DELAY = 20000; //  sensor delay: game
    private int SENSOR_SAMPLE_RATE = 1000000/SENSOR_SAMPLE_DELAY;
    private int FFT_WINDOW_SIZE = 64;

    //data log
    private ArrayList<AccelOutput> accelData = new ArrayList<>();

    private MediaPlayer mediaPlayer;

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

        //seek bar setup
        sample_rate_seek_bar = findViewById(R.id.sample_rate_seek_bar);
        window_size_seek_bar = findViewById(R.id.window_size_seek_bar);

        sample_rate_seek_bar.setMax(99); //1 -> 100 : range of values
        window_size_seek_bar.setMax(6); // 10 - 4 range of values

        window_size_seek_bar.setProgress(2);//hard code - could implement log2 function to derive from window size
        sample_rate_seek_bar.setProgress(SENSOR_SAMPLE_RATE - 1);//adjust for range of seekbar

        window_size_label = findViewById(R.id.window_size_label);
        sample_rate_label = findViewById(R.id.sample_rate_label);

        window_size_label.setText(String.format(Locale.getDefault(),
                "%s %d",getResources().getString(R.string.window_size_label), FFT_WINDOW_SIZE));
        sample_rate_label.setText(String.format(Locale.getDefault(),
                "%s %dHz",getResources().getString(R.string.sample_rate_label), SENSOR_SAMPLE_RATE));

        visualiseSensorData();
        
        test_btn = findViewById(R.id.test_btn);
        test_btn.setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SENSOR_SAMPLE_DELAY);

        sample_rate_seek_bar.setOnSeekBarChangeListener(this);
        window_size_seek_bar.setOnSeekBarChangeListener(this);


//        MediaPlayer mediaPlayer = new MediaPlayer();
//        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);

        if (mediaPlayer != null){
            mediaPlayer.release();
            mediaPlayer = null;
        }

    }



    private boolean sensorInit(){

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            Log.d(TAG, "sensorInit: accelerometer found, min delay: " + mAccelerometer.getMinDelay());
        }
        else {
            Log.d(TAG, "sensorInit: accelerometer not found");
            return false;
        }
        return true;
    }

    private void resetSensorDelay(){
        mSensorManager.unregisterListener(this);
        mSensorManager.registerListener(this, mAccelerometer,SENSOR_SAMPLE_DELAY);
    }


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

            SENSOR_SAMPLE_RATE = (sample_rate_seek_bar.getProgress() + 1);
            SENSOR_SAMPLE_DELAY = 1000000/SENSOR_SAMPLE_RATE;

            resetSensorDelay();

            sample_rate_label.setText(String.format(Locale.getDefault(),
                    "%s %dHz",getResources().getString(R.string.sample_rate_label), SENSOR_SAMPLE_RATE));



        }
        else if (seekBar.getId() == R.id.window_size_seek_bar){


            FFT_WINDOW_SIZE = (int)Math.pow(2.0, seekBar.getProgress() + 4);

            //update label
            window_size_label.setText(String.format(Locale.getDefault(),
                    "%s %d",getResources().getString(R.string.window_size_label), FFT_WINDOW_SIZE));
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


    //button for testing
    @Override
    public void onClick(View v) {
        
        if (v == test_btn){

            mediaPlayer = MediaPlayer.create(this, R.raw.running_music);
            mediaPlayer.start();
        }
    }

}
