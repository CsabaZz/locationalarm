package com.bitknights.locationalarm.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class AnimationRelativeLayout extends RelativeLayout {

    public AnimationRelativeLayout(Context context) {
	super(context);
    }

    public AnimationRelativeLayout(Context context, AttributeSet attrs) {
	super(context, attrs);
    }

    public AnimationRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
	super(context, attrs, defStyle);
    }
    
    public float getXFraction() {
        final int width = getWidth();
	if(width == 0) {
	    return 0;
	} else {
	    return getX() / getWidth();
	}
    }

    public void setXFraction(float xFraction) {
        final int width = getWidth();
        setX((width > 0) ? (xFraction * width) : -9999);
    }

}
