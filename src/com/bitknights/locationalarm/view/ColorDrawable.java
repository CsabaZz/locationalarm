package com.bitknights.locationalarm.view;

import android.graphics.Canvas;

public class ColorDrawable extends android.graphics.drawable.ColorDrawable {
    private int color;

    public ColorDrawable(int color) {
	this.color = color;
    }

    public int getColor() {
	return color;
    }

    public void setColor(int color) {
	this.color = color;
    }

    @Override
    public void draw(Canvas canvas) {
	canvas.drawColor(color);
    }
}
