package com.example.mis.sensor;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class MotionDisplay extends Drawable {

    private ArrayList<DrawableDataSet> dataSets = new ArrayList<>();
    private ArrayList<Paint> paints = new ArrayList<>();
    private int bg_color;

    public MotionDisplay(ArrayList<DrawableDataSet> _dataSets, int bg_color) {


        this.dataSets = _dataSets;

        for (DrawableDataSet ds : dataSets){

            Paint p = new Paint();
            p.setStrokeWidth(3);
            p.setColor(ds.draw_colour);
            paints.add(p);
        }

        this.bg_color = bg_color;
    }


    @Override
    public void draw(@NonNull Canvas canvas) {

        if (dataSets == null){
            //no data to show
            return;
        }

        //get height and width here
        int width = getBounds().width();
        int height = getBounds().height();
        int graph_height = (int)(height * 0.95);
        int graph_width = (int)(width * 1.0);

        //fill bg - dark
        canvas.drawColor(bg_color);

        //draw graph
        //assume normalised times series graph that should fill width of screen
        //therefore y axis is 0 to 1, x axis is split into n values where n is length of data array

        for (int d = 0; d < dataSets.size(); d++) {

            DrawableDataSet dataSet = dataSets.get(d);
            int j;
            for (int i = 1; i < dataSet.data_points.length; i++){
                j = i - 1;
                float x1 = graph_width * (j / (float)dataSet.data_points.length);
                float y1 = graph_height - (dataSet.data_points[j] * graph_height);
                float x2 = graph_width * (i / (float)dataSet.data_points.length);
                float y2 = graph_height - (dataSet.data_points[i] * graph_height);

                canvas.drawLine(x1,y1,x2,y2,paints.get(d));

            }
        }

    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
