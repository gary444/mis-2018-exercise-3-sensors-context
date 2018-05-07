//package com.example.mis.sensor;
//
//import android.content.Context;
//import android.media.MediaPlayer;
//import android.net.Uri;
//import android.provider.MediaStore;
//
//import java.io.File;
//import java.util.Timer;
//import java.util.TimerTask;
//
////from https://stackoverflow.com/questions/6884590/
//// android-how-to-create-fade-in-fade-out-sound-effects-for-any-music-file-that-my/15395265#15395265
////adapted and extended
//public class MusicHandler {
//    private MediaPlayer runningPlayer;
//    private MediaPlayer cyclingPlayer;
//    private Context context;
//    private int iVolume;
//
//    private boolean playing;
//    private int DEFAULT_FADE = 2000;
//
//    private final static int INT_VOLUME_MAX = 100;
//    private final static int INT_VOLUME_MIN = 0;
//    private final static float FLOAT_VOLUME_MAX = 1;
//    private final static float FLOAT_VOLUME_MIN = 0;
//
//    public MusicHandler(Context context) {
//        this.context = context;
//        playing = false;
//    }
//
//    public void startRunningMusic(){
//
//        pause();
//        runningPlayer = MediaPlayer.create(context, R.raw.running_music);
//        runningPlayer.setLooping(true);
//        play(DEFAULT_FADE);
//
//    }
//    public void startCyclingMusic(){
//        pause();
//        runningPlayer = MediaPlayer.create(context, R.raw.cycling_music);
//        runningPlayer.setLooping(true);
//        play(DEFAULT_FADE);
//    }
//
//
////    public void load(int address, boolean looping) {
////        mediaPlayer = MediaPlayer.create(context, address);
////        mediaPlayer.setLooping(looping);
////        playing = false;
////    }
//
//    public void play(int fadeDuration) {
//        // Set current volume, depending on fade or not
//        if (fadeDuration > 0)
//            iVolume = INT_VOLUME_MIN;
//        else
//            iVolume = INT_VOLUME_MAX;
//
//        updateVolume(0);
//
//        // Play music
//        if (!mediaPlayer.isPlaying()){
//            mediaPlayer.start();
//            playing = true;
//        }
//
//
//
//
//        // Start increasing volume in increments
//        if (fadeDuration > 0) {
//            final Timer timer = new Timer(true);
//            TimerTask timerTask = new TimerTask() {
//                @Override
//                public void run() {
//                    updateVolume(1);
//                    if (iVolume == INT_VOLUME_MAX) {
//                        timer.cancel();
//                        timer.purge();
//                    }
//                }
//            };
//
//            // calculate delay, cannot be zero, set to 1 if zero
//            int delay = fadeDuration / INT_VOLUME_MAX;
//            if (delay == 0)
//                delay = 1;
//
//            timer.schedule(timerTask, delay, delay);
//        }
//
//    }
//
//    public void pause(String player) {
//
//        if (player == "cycling"){
//
//            if (!cyclingPlayer.isPlaying())
//                return;
//
//            iVolume = INT_VOLUME_MAX;
//            updateVolume(0, "cycling");
//
//            // Start increasing volume in increments
//            if (DEFAULT_FADE > 0) {
//                final Timer timer = new Timer(true);
//                TimerTask timerTask = new TimerTask() {
//                    @Override
//                    public void run() {
//                        updateVolume(-1, "cycling");
//                        if (iVolume == INT_VOLUME_MIN) {
//                            // Pause music
//                            if (cyclingPlayer.isPlaying())
//                                cyclingPlayer.pause();
//                            timer.cancel();
//                            timer.purge();
//                            playing = false;
//                        }
//                    }
//                };
//
//                // calculate delay, cannot be zero, set to 1 if zero
//                int delay = DEFAULT_FADE / INT_VOLUME_MAX;
//                if (delay == 0)
//                    delay = 1;
//
//                timer.schedule(timerTask, delay, delay);
//            }
//        }
//        else if (player == "running"){
//
//            if (!runningPlayer.isPlaying())
//                return;
//
//            iVolume = INT_VOLUME_MAX;
//            updateVolume(0, "running");
//
//            // Start increasing volume in increments
//            if (DEFAULT_FADE > 0) {
//                final Timer timer = new Timer(true);
//                TimerTask timerTask = new TimerTask() {
//                    @Override
//                    public void run() {
//                        updateVolume(-1, "running");
//                        if (iVolume == INT_VOLUME_MIN) {
//                            // Pause music
//                            if (runningPlayer.isPlaying())
//                                runningPlayer.pause();
//                            timer.cancel();
//                            timer.purge();
//                            playing = false;
//                        }
//                    }
//                };
//
//                // calculate delay, cannot be zero, set to 1 if zero
//                int delay = DEFAULT_FADE / INT_VOLUME_MAX;
//                if (delay == 0)
//                    delay = 1;
//
//                timer.schedule(timerTask, delay, delay);
//            }
//        }
//
//
//
//
//
//    }
//
//    private void updateVolume(int change, String player) {
//        // increment or decrement depending on type of fade
//        iVolume = iVolume + change;
//
//        // ensure iVolume within boundaries
//        if (iVolume < INT_VOLUME_MIN)
//            iVolume = INT_VOLUME_MIN;
//        else if (iVolume > INT_VOLUME_MAX)
//            iVolume = INT_VOLUME_MAX;
//
//        // convert to float value
//        float fVolume = 1 - ((float) Math.log(INT_VOLUME_MAX - iVolume) / (float) Math.log(INT_VOLUME_MAX));
//
//        // ensure fVolume within boundaries
//        if (fVolume < FLOAT_VOLUME_MIN)
//            fVolume = FLOAT_VOLUME_MIN;
//        else if (fVolume > FLOAT_VOLUME_MAX)
//            fVolume = FLOAT_VOLUME_MAX;
//
//        if(player == "running"){
//
//            runningPlayer.setVolume(fVolume, fVolume);
//        }
//        else if (player == "cycling"){
//
//            cyclingPlayer.setVolume(fVolume, fVolume);
//        }
//
//    }
//
//    public boolean isPlaying() {
//        return playing;
//    }
//}
