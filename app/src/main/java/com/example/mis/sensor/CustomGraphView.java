package com.example.mis.sensor;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.jar.Attributes;

import static android.content.ContentValues.TAG;

//help from https://medium.com/@rey5137/custom-drawable-part-1-6fb26bb25690
public class CustomGraphView extends AppCompatImageView //implements View.OnTouchListener
{

    private MotionDisplay graphDrawable;

    public CustomGraphView(Context context){
        super(context);
    }
    public CustomGraphView(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public void updateData(ArrayList<DrawableDataSet> dataSets, int bg_color){
        graphDrawable = new MotionDisplay(dataSets, bg_color);
        setImageDrawable(graphDrawable);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

    }


}
