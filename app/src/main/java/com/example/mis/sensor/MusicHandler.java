package com.example.mis.sensor;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import static android.content.ContentValues.TAG;

//from https://stackoverflow.com/questions/6884590/
// android-how-to-create-fade-in-fade-out-sound-effects-for-any-music-file-that-my/15395265#15395265
//adapted and extended
public class MusicHandler {
    private MediaPlayer mediaPlayer;
//    private Context context;
    private int iVolume;
    private String name;
    private boolean playing;
    private int DEFAULT_FADE = 2000;

    private final static int INT_VOLUME_MAX = 100;
    private final static int INT_VOLUME_MIN = 0;
    private final static float FLOAT_VOLUME_MAX = 1;
    private final static float FLOAT_VOLUME_MIN = 0;

    public MusicHandler(Context context, int resource_id, boolean setLooping, String name) {
        playing = false;
        this.name = name;
        mediaPlayer = MediaPlayer.create(context, resource_id);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setLooping(setLooping);
    }


    public void play() {

        Log.d(TAG, "play: media player " + name);

        if (playing)
            return;

        // Set current volume, depending on fade or not
        if (DEFAULT_FADE > 0)
            iVolume = INT_VOLUME_MIN;
        else
            iVolume = INT_VOLUME_MAX;

        updateVolume(0);

        // Play music
        if (!mediaPlayer.isPlaying()) {

            mediaPlayer.start();
            playing = true;
        }

        // Start increasing volume in increments
        if (DEFAULT_FADE > 0) {
            final Timer timer = new Timer(true);
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    updateVolume(1);
                    if (iVolume == INT_VOLUME_MAX) {
                        timer.cancel();
                        timer.purge();
                    }
                }
            };

            // calculate delay, cannot be zero, set to 1 if zero
            int delay = DEFAULT_FADE / INT_VOLUME_MAX;
            if (delay == 0)
                delay = 1;

            timer.schedule(timerTask, delay, delay);
        }
    }

    public void pause() {


        Log.d(TAG, "pause: media player " + name);

        if (!playing)
            return;

        // Set current volume, depending on fade or not
        if (DEFAULT_FADE > 0)
            iVolume = INT_VOLUME_MAX;
        else
            iVolume = INT_VOLUME_MIN;

        updateVolume(0);

        // Start increasing volume in increments
        if (DEFAULT_FADE > 0) {
            final Timer timer = new Timer(true);
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    updateVolume(-1);
                    if (iVolume == INT_VOLUME_MIN) {
                        // Pause music
                        if (mediaPlayer.isPlaying()) {

                            mediaPlayer.pause();
                            playing = false;
                        }
                        timer.cancel();
                        timer.purge();
                    }
                }
            };
            // calculate delay, cannot be zero, set to 1 if zero
            int delay = DEFAULT_FADE / INT_VOLUME_MAX;
            if (delay == 0)
                delay = 1;

            timer.schedule(timerTask, delay, delay);
        }
    }

    private void updateVolume(int change) {
        // increment or decrement depending on type of fade
        iVolume = iVolume + change;

        // ensure iVolume within boundaries
        if (iVolume < INT_VOLUME_MIN)
            iVolume = INT_VOLUME_MIN;
        else if (iVolume > INT_VOLUME_MAX)
            iVolume = INT_VOLUME_MAX;

        // convert to float value
        float fVolume = 1 - ((float) Math.log(INT_VOLUME_MAX - iVolume) / (float) Math.log(INT_VOLUME_MAX));

        // ensure fVolume within boundaries
        if (fVolume < FLOAT_VOLUME_MIN)
            fVolume = FLOAT_VOLUME_MIN;
        else if (fVolume > FLOAT_VOLUME_MAX)
            fVolume = FLOAT_VOLUME_MAX;

        mediaPlayer.setVolume(fVolume, fVolume);
    }


    public boolean isPlaying() {
        return playing;
    }

    public void stopAndRelease(){
        if (playing){
            mediaPlayer.stop();
        }
        mediaPlayer.release();
    }
}
