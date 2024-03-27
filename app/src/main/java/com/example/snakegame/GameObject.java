package com.example.snakegame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;

public abstract class GameObject {

    protected Context context;
    protected Point location;
    protected int size;

    public GameObject(Context context, Point location, int size) {
        this.context = context;
        this.location = location;
        this.size = size;
    }

    public abstract void draw(Canvas canvas, Paint paint);

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
