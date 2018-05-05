package com.example.mis.sensor;

import android.util.Log;

import static android.content.ContentValues.TAG;

public class AccelOutput {
    public float x;
    public float y;
    public float z;
    public float magnitude;

    public AccelOutput(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.magnitude = (float)Math.sqrt(x*x + y*y + z*z);
    }

    public float get(int dataIndex){
        switch (dataIndex){
            case 0: return x;
            case 1: return y;
            case 2: return z;
            case 3: return magnitude;
            default:
                Log.d(TAG, "accelOutput: out of range data access");
                return 0.f;
        }
    }
    public static int getColour(int colourIndex){
        switch (colourIndex){
            case 0: return 0xffff0000;
            case 1: return 0xff00ff00;
            case 2: return 0xff6bd7ff;
            case 3: return 0xffffffff;
            default:
                Log.d(TAG, "accelOutput: out of range colour access");
                return 0;
        }
    }

}
