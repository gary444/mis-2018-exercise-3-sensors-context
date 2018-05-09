package com.example.mis.sensor;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.mis.sensor.MainActivity.ExerciseMode.CYCLING;
import static com.example.mis.sensor.MainActivity.ExerciseMode.NONE;
import static com.example.mis.sensor.MainActivity.ExerciseMode.RUNNING;

public class MainActivity extends AppCompatActivity implements SensorEventListener, SeekBar.OnSeekBarChangeListener, View.OnClickListener, LocationListener {



    public enum ExerciseMode {
        NONE,
        RUNNING,
        CYCLING
    }



    //views
    private CustomGraphView sensor_data_view;
    private CustomGraphView fft_view;

    private TextView window_size_label;
    private TextView sample_rate_label;
    private TextView fft_dom_freq_label;
    private TextView fft_energy_label;
    private TextView mode_label;
    private TextView speed_label;

    private SeekBar window_size_seek_bar;
    private SeekBar sample_rate_seek_bar;

//    private Button test_btn1;
//    private Button test_btn2;

    //visual parameters
    private final int DARK_BG = 0xff222222;
    private final int LIGHT_BG = 0xffaaaaaa;
    private final int GRAPH_X_RESOLUTION = 50;
    private final float SENSOR_GRAPH_SCALE_Y = 0.01f;
    private final float FFT_GRAPH_SCALE_Y = 0.3f;

    //sensor variables
    private SensorManager mSensorManager;
    private Sensor mLinearAccelerometer;

    //sensor parameters
    private int SENSOR_SAMPLE_DELAY = 20000; //  as sensor delay: game
    private int SENSOR_SAMPLE_RATE = 1000000/SENSOR_SAMPLE_DELAY;
    private int FFT_WINDOW_SIZE = 64;
    private double dominantFrequency = 0;
    private double fft_energy = 0.0;
    private final double  MOVEMENT_THRESHOLD = 0.05;

    //data log
    private ArrayList<AccelOutput> accelData = new ArrayList<>();

    private boolean allowModeChange = true;
    private final long MODE_CHANGE_DELAY = 3000;//ms
    private long time_of_last_mode_change = 0;
    ExerciseMode exercisemode = NONE;
    private double last_known_speed = 0.0;
    private int time_since_speed_update = 0;

    private MusicHandler cycling_player;
    private MusicHandler running_player;

    private static final String TAG = MainActivity.class.getSimpleName();

    //location stuff - help from http://findnerd.com/list/view/How-to-Track-Users-Speed-Using-location-getSpeed-GPS-Method-in-Android/34577/
    private LocationManager locationManager;
    private static final float MIN_UPDATE_DIST = 1;
    private static final long MIN_UPDATE_TIME = 3000;
    private boolean mLocationPermissionGranted;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;




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
        fft_dom_freq_label = findViewById(R.id.fft_dom_freq_label);
        mode_label = findViewById(R.id.mode_output);
        speed_label = findViewById(R.id.speed_output);
        fft_energy_label = findViewById(R.id.fft_energy_output);

        window_size_label.setText(String.format(Locale.getDefault(),
                "%s %d",getResources().getString(R.string.window_size_label), FFT_WINDOW_SIZE));
        sample_rate_label.setText(String.format(Locale.getDefault(),
                "%s %dHz",getResources().getString(R.string.sample_rate_label), SENSOR_SAMPLE_RATE));
        mode_label.setText(String.format(Locale.getDefault(), "Mode: %s", modeToString(exercisemode)));
        speed_label.setText(String.format(Locale.getDefault(), "Speed: %6.2f km/h  (%d sec ago)",
                last_known_speed, time_since_speed_update));

        visualiseSensorData();
        
//        test_btn1 = findViewById(R.id.test_btn1);
//        test_btn1.setOnClickListener(this);
//        test_btn2 = findViewById(R.id.test_btn2);
//        test_btn2.setOnClickListener(this);



        //location setup
        getLocationPermission();
        registerForLocationUpdates();

        //set callback for updating last speed update
        final Timer timer = new Timer(true);
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                time_since_speed_update++;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        speed_label.setText(String.format(Locale.getDefault(), "Speed: %6.2f km/h  (%d sec ago)",
                                last_known_speed, time_since_speed_update));
                        fft_energy_label.setText(String.format(Locale.getDefault(), "FFT energy = %10.4f", fft_energy));
                    }
                });
            }
        };
        timer.schedule(timerTask, 1000, 1000);


    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mLinearAccelerometer, SENSOR_SAMPLE_DELAY);

        sample_rate_seek_bar.setOnSeekBarChangeListener(this);
        window_size_seek_bar.setOnSeekBarChangeListener(this);

        cycling_player = new MusicHandler(this, R.raw.cycling_music, true, "cycling");
        running_player = new MusicHandler(this, R.raw.running_music, true, "running");

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);

        if (cycling_player != null)
            cycling_player.stopAndRelease();
        if (running_player != null)
            running_player.stopAndRelease();

    }


    private boolean sensorInit(){

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
            mLinearAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

            Log.d(TAG, "sensorInit: linear accelerometer found, min delay: " + mLinearAccelerometer.getMinDelay());
        }
        else {
            Log.d(TAG, "sensorInit: linear accelerometer not found");
            return false;
        }
        return true;
    }

    private void resetSensorDelay(){
        mSensorManager.unregisterListener(this);
        mSensorManager.registerListener(this, mLinearAccelerometer,SENSOR_SAMPLE_DELAY);
    }

    //sensor methods ---------------------------------------------------
    @Override
    public void onSensorChanged(SensorEvent event) {

        //if accel
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
            float[] values = event.values;

            accelData.add(new AccelOutput(values[0], values[1], values[2]));

            visualiseSensorData();
            new FFTAsynctask(FFT_WINDOW_SIZE).execute(accelData);

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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




    /**
     * Implements the fft functionality as an async task
     * FFT(int n): constructor with fft length
     * fft(double[] x, double[] y)
     */
    private class FFTAsynctask extends AsyncTask<ArrayList<AccelOutput>, Void, double[]> {

        private int wsize; //window size must be power of 2

        // constructor to set window size
        FFTAsynctask(int wsize) {
            this.wsize = wsize;
        }

        @Override
        protected double[] doInBackground(ArrayList<AccelOutput>... data) {

            double[] realPart = new double[wsize];

            //create list of double values if there is enough accelerometer data
            if (data[0].size() > wsize){

                for (int j = 0; j < wsize; j++){
                    float FFT_SCALING = 0.01f;
                    realPart[j] = FFT_SCALING * data[0].get(data[0].size() - (j + 1)).magnitude;
                }
            }
            double[] imagPart = new double[wsize]; // init empty

            /*
             * Init the FFT class with given window size and run it with your input.
             * The fft() function overrides the realPart and imagPart arrays!
             */
            FFT fft = new FFT(wsize);
            fft.fft(realPart, imagPart);
            //init new double array for magnitude (e.g. frequency count)
            double[] magnitude = new double[wsize/2 + 1];


            //fill array with magnitude values of the distribution
            //dont use first bin in graph - it's the DC term
            //find dominant freq at same time
            double maxAmp = 0.0;
            int maxBin = -1;
            fft_energy = Math.sqrt(Math.pow(realPart[0], 2)) / wsize;

            for (int i = 0; (wsize/2) > i ; i++) {
                magnitude[i] = FFT_GRAPH_SCALE_Y * Math.sqrt(Math.pow(realPart[i + 1], 2) + Math.pow(imagPart[i + 1], 2));
                if (magnitude[i] > maxAmp){
                    maxAmp = magnitude[i];
                    maxBin = i + 1;//take index of fft output with biggest component
                }

            }
            //help from https://stackoverflow.com/questions/7674877/how-to-get-frequency-from-fft-result
            dominantFrequency = (float)maxBin * SENSOR_SAMPLE_RATE / wsize;

            checkMovementType();

            return magnitude;
        }

        @Override
        protected void onPostExecute(double[] values) {
            //hand over values to global variable after background task is finished
            visualiseFFTData(values);
            if (fft_energy > MOVEMENT_THRESHOLD)
                fft_dom_freq_label.setText(String.format(Locale.getDefault(), "Dom. Freq. = %8.2f Hz", dominantFrequency));
            else
                fft_dom_freq_label.setText(String.format(Locale.getDefault(), "Dom. Freq. = n/a (lo input)", dominantFrequency));


            //update mode for testing
            mode_label.setText(String.format(Locale.getDefault(), "Mode: %s", modeToString(exercisemode)));
        }
    }

    //check various metrics (fft energy and frequency, speed)
    //updates movement type if necessary
    private void checkMovementType(){

        //check mode was not changed too recently
        if (SystemClock.uptimeMillis() - MODE_CHANGE_DELAY < time_of_last_mode_change)
            return;

        //use points system to score likelihood of various modes
        Map<ExerciseMode, Integer> mode_map = new HashMap<>();
        mode_map.put(NONE, 0);
        mode_map.put(CYCLING, 0);
        mode_map.put(RUNNING, 0);


        if (fft_energy < MOVEMENT_THRESHOLD){ // no energy -> no movement
            setExerciseMode(NONE);
        }
        else {
            //check speed
            if (last_known_speed > 50) { // in a vehicle
                setExerciseMode(NONE);
                return;
            }
            else if (last_known_speed > 35){//probably in a vehicle
                mode_map.put(NONE, mode_map.get(NONE) + 3);
                mode_map.put(CYCLING, mode_map.get(CYCLING) + 1);
            }
            else if (last_known_speed > 15){//prob cycling
                mode_map.put(CYCLING, mode_map.get(CYCLING) + 3);
            }
            else if (last_known_speed > 7){//prob running
                mode_map.put(RUNNING, mode_map.get(RUNNING) + 3);
                mode_map.put(CYCLING, mode_map.get(CYCLING) + 1);
            }
            else {//prob walking/stationary
                mode_map.put(NONE, mode_map.get(NONE) + 3);
            }

            //check dominant frequency
            if (dominantFrequency < 1.0){
                mode_map.put(CYCLING, mode_map.get(CYCLING) + 2);
                mode_map.put(RUNNING, mode_map.get(RUNNING) + 1);
            }
            else if (dominantFrequency < 2.0){
                mode_map.put(RUNNING, mode_map.get(RUNNING) + 2);
            }
            else {
                mode_map.put(NONE, mode_map.get(NONE) + 1);
            }

        }

        //find biggest score
        ExerciseMode highest_mode = NONE;
        int highest_score = 0;
        for (Map.Entry<ExerciseMode, Integer> entry : mode_map.entrySet()){
            if (entry.getValue() > highest_score){//only update for positive 'win' on points
                highest_score = entry.getValue();
                highest_mode = entry.getKey();
            }
        }

        //update mode
        setExerciseMode(highest_mode);
        time_of_last_mode_change = SystemClock.uptimeMillis();
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
    public void onStartTrackingTouch(SeekBar seekBar) { }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}


    //button for testing
    @Override
    public void onClick(View v) {


//        if (v == test_btn1){
//
//            if (exercisemode == RUNNING)
//                setExerciseMode(NONE);
//            else
//                setExerciseMode(RUNNING);
//
//
//        }
//        else if (v == test_btn2){
//
//            if (exercisemode == CYCLING)
//                setExerciseMode(NONE);
//            else
//                setExerciseMode(CYCLING);
//        }
    }

    //controls music players when changing modes
    private void setExerciseMode (ExerciseMode targetMode){

        if (targetMode == exercisemode)
            return;//dont need to do anything


        if (targetMode == RUNNING){
            cycling_player.pause();
            running_player.play();
        }
        else if (targetMode == CYCLING){
           running_player.pause();
           cycling_player.play();
        }
        else if(targetMode == NONE){
            running_player.pause();
            cycling_player.pause();
        }
        exercisemode = targetMode;
    }


    public void registerForLocationUpdates() {
        try {
            locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_UPDATE_TIME, MIN_UPDATE_DIST, this);
            }
            catch(SecurityException e){
                Log.e("Exception: %s", e.getMessage());
            }
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

    }
    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        time_since_speed_update = 0;
        last_known_speed = toKmh(location.getSpeed());
        speed_label.setText(String.format(Locale.getDefault(), "Speed: %6.2f km/h  (%d sec ago)",
                last_known_speed, time_since_speed_update));
    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override
    public void onProviderEnabled(String provider) {}
    @Override
    public void onProviderDisabled(String provider) {}

    //helper - conversion function, m/s to km/h
    private float toKmh(float in_metres_per_sec){
        return in_metres_per_sec * 60 * 60 / 1000;
    }
    //helper - mode to string
    private String modeToString(ExerciseMode mode){
        switch (mode){
            case NONE: return "NONE";
            case RUNNING: return "RUNNING";
            case CYCLING: return "CYCLING";
            default:break;
        }
        return "";
    }

}
