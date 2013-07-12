package com.bitknights.locationalarm.utils;

import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

public class ShadingTouchListener implements View.OnTouchListener {

    private static final int pressed = Color.argb(255, 128, 128, 128);
    private static final int released = Color.argb(255, 255, 255, 255);

    private void setViewFilter(View v, MotionEvent event, int color) {
	Drawable background = v.getBackground();
	if (background != null) {
	    background.mutate().setColorFilter(color, Mode.MULTIPLY);
	}
    }

    private void setImageButtonFilter(ImageButton v, MotionEvent event, int color) {
	Drawable background = v.getBackground();
	if (background != null) {
	    background.mutate().setColorFilter(color, Mode.MULTIPLY);
	}

	v.setColorFilter(color, Mode.MULTIPLY);
    }

    private void setImageViewFilter(ImageView v, MotionEvent event, int color) {
	Drawable background = v.getBackground();
	if (background != null) {
	    background.mutate().setColorFilter(color, Mode.MULTIPLY);
	}

	v.setColorFilter(color, Mode.MULTIPLY);
    }

    private void setButtonFilter(Button v, MotionEvent event, int color) {
	Drawable background = v.getBackground();
	Drawable[] drawables = v.getCompoundDrawables();

	if (background != null) {
	    background.mutate().setColorFilter(color, Mode.MULTIPLY);
	}

	if (drawables[0] != null) {
	    drawables[0].mutate().setColorFilter(color, Mode.MULTIPLY);
	}

	if (drawables[1] != null) {
	    drawables[1].mutate().setColorFilter(color, Mode.MULTIPLY);
	}

	if (drawables[2] != null) {
	    drawables[2].mutate().setColorFilter(color, Mode.MULTIPLY);
	}

	if (drawables[3] != null) {
	    drawables[3].mutate().setColorFilter(color, Mode.MULTIPLY);
	}
    }

    private void setViewGroupFilter(ViewGroup vg, MotionEvent event, int color) {
	Drawable background = vg.getBackground();
	if (background != null) {
	    background.mutate().setColorFilter(color, Mode.MULTIPLY);
	}

	int childCount = vg.getChildCount();

	for (int i = 0; i < childCount; ++i) {
	    View child = vg.getChildAt(i);
	    switch (event.getAction()) {
	    case MotionEvent.ACTION_DOWN:
		setColor(child, event, true, pressed);
		break;
	    case MotionEvent.ACTION_UP:
	    case MotionEvent.ACTION_CANCEL:
	    case MotionEvent.ACTION_OUTSIDE:
		setColor(child, event, true, released);
		break;
	    }

	    child.invalidate();
	    child.onTouchEvent(event);
	}
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
	switch (event.getAction()) {
	case MotionEvent.ACTION_DOWN:
	    setColor(v, event, v.isClickable(), pressed);
	    break;
	case MotionEvent.ACTION_UP:
	case MotionEvent.ACTION_CANCEL:
	case MotionEvent.ACTION_OUTSIDE:
	    setColor(v, event, v.isClickable(), released);
	    break;
	}

	return v.onTouchEvent(event);
    }

    private void setColor(View v, MotionEvent event, boolean clickable, int color) {
	if (v instanceof ImageButton && clickable) {
	    setImageButtonFilter(((ImageButton) v), event, color);
	} else if (v instanceof ImageView && clickable) {
	    setImageViewFilter(((ImageView) v), event, color);
	} else if (v instanceof Button && clickable) {
	    setButtonFilter(((Button) v), event, color);
	} else if (v instanceof ViewGroup && clickable) {
	    setViewGroupFilter(((ViewGroup) v), event, color);
	} else {
	    setViewFilter(v, event, color);
	}

	v.invalidate();
    }

}
