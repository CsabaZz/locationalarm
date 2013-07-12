package com.bitknights.locationalarm;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class CompoundDrawableOnTouchListener implements View.OnTouchListener {
    public enum POSITION {
	LEFT, TOP, RIGHT, BOTTOM
    };

    private Drawable[] drawables;

    /**
     * @param keyword
     */
    public CompoundDrawableOnTouchListener(TextView view) {
	super();
	drawables = view.getCompoundDrawables();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.View.OnTouchListener#onTouch(android.view.View,
     * android.view.MotionEvent)
     */
    @Override
    public boolean onTouch(final View v, final MotionEvent event) {
	if (null == drawables || drawables.length < 4) {
	    return false;
	}

	if (event.getAction() == MotionEvent.ACTION_DOWN) {
	    final int x = (int) event.getX();
	    final int y = (int) event.getY();
	    if (isLeftDrawableTouched(v, x, y)) {
		return onDrawableTouch(event, POSITION.LEFT);
	    } else if (isTopDrawableTouched(v, x, y)) {
		return onDrawableTouch(event, POSITION.TOP);
	    } else if (isRightDrawableTouched(v, x, y)) {
		return onDrawableTouch(event, POSITION.RIGHT);
	    } else if (isBottomDrawableTouched(v, x, y)) {
		return onDrawableTouch(event, POSITION.BOTTOM);
	    }
	}

	return false;
    }

    private boolean isLeftDrawableTouched(View v, int x, int y) {
	if (null == drawables[0]) {
	    return false;
	}

	final Rect bounds = drawables[0].getBounds();
	return x >= (v.getLeft() + v.getPaddingLeft())
		&& x <= (v.getLeft() + v.getPaddingLeft() + bounds.width() + v.getPaddingRight())
		&& y >= (v.getTop() + v.getPaddingTop()) && y <= (v.getBottom() - v.getPaddingBottom());
    }

    private boolean isRightDrawableTouched(View v, int x, int y) {
	if (null == drawables[1]) {
	    return false;
	}

	final Rect bounds = drawables[1].getBounds();
	return x >= (v.getLeft() + v.getPaddingLeft())
		&& x <= (v.getLeft() + v.getPaddingLeft() + bounds.width() + v.getPaddingRight())
		&& y >= (v.getTop() + v.getPaddingTop()) && y <= (v.getBottom() - v.getPaddingBottom());
    }

    private boolean isTopDrawableTouched(View v, int x, int y) {
	if (null == drawables[2]) {
	    return false;
	}

	final Rect bounds = drawables[2].getBounds();
	return x >= (v.getLeft() + v.getPaddingLeft())
		&& x <= (v.getLeft() + v.getPaddingLeft() + bounds.width() + v.getPaddingRight())
		&& y >= (v.getTop() + v.getPaddingTop()) && y <= (v.getBottom() - v.getPaddingBottom());
    }

    private boolean isBottomDrawableTouched(View v, int x, int y) {
	if (null == drawables[3]) {
	    return false;
	}

	final Rect bounds = drawables[3].getBounds();
	return x >= (v.getLeft() + v.getPaddingLeft())
		&& x <= (v.getLeft() + v.getPaddingLeft() + bounds.width() + v.getPaddingRight())
		&& y >= (v.getTop() + v.getPaddingTop()) && y <= (v.getBottom() - v.getPaddingBottom());
    }

    public boolean onDrawableTouch(final MotionEvent event, POSITION position) {
	return false;
    }
}
