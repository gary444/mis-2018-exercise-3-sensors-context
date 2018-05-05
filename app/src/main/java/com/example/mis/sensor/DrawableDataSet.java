package com.example.mis.sensor;


//container for storing a data set, it's name, and a colour for drawing with
public class DrawableDataSet {

    public float[] data_points;
    public int draw_colour;
    public String name;


    public DrawableDataSet() {
        //defaults
        data_points = new float[1];
        draw_colour = 0xff000000;
        name = "no_name";
    }

    public DrawableDataSet(float[] data_points, int draw_colour, String name) {
        this.data_points = data_points;
        this.draw_colour = draw_colour;
        this.name = name;
    }
}
