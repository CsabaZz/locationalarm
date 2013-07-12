package com.bitknights.locationalarm.view;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;

public class TextStyleSpan extends android.text.style.TypefaceSpan {

    private final float mTextSize;
    private final int mTextColor;
    private final Typeface mTypeface;

    public TextStyleSpan(Typeface typeface, int textColor, float textSize) {
	super("monospace");

	this.mTypeface = typeface;
	this.mTextColor = textColor;
	this.mTextSize = textSize;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
	applyTypeFace(ds, mTypeface);
    }

    @Override
    public void updateMeasureState(TextPaint paint) {
	applyTypeFace(paint, mTypeface);
    }

    private void applyTypeFace(Paint paint, Typeface tf) {
	int oldStyle = 0;

	Typeface old = paint.getTypeface();
	if (old != null) {
	    oldStyle = old.getStyle();
	}

	int newStyle = oldStyle & ~tf.getStyle();
	if ((newStyle & Typeface.BOLD) != 0) {
	    paint.setFakeBoldText(true);
	}

	if ((newStyle & Typeface.ITALIC) != 0) {
	    paint.setTextSkewX(-0.25f);
	}

	paint.setTextSize(mTextSize);
	paint.setColor(mTextColor);
	paint.setTypeface(tf);
    }
}
