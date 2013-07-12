package com.bitknights.locationalarm.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ViewFlipper;

public class StateViewFlipper extends ViewFlipper {
    public interface OnChildAddedListener {
	void onChildAdded(View child);
    }

    private OnChildAddedListener mOnChildAdded;

    public StateViewFlipper(Context context) {
	super(context);
    }

    public StateViewFlipper(Context context, AttributeSet attrs) {
	super(context, attrs);
    }

    @Override
    public void addView(View child) {
	boolean contains = indexOfChild(child) != -1;
	if (contains) {
	    removeView(child);
	}

	super.addView(child);
	if (this.mOnChildAdded != null) {
	    this.mOnChildAdded.onChildAdded(child);
	}
    }

    public void setOnChildAddedListener(OnChildAddedListener listener) {
	this.mOnChildAdded = listener;
    }

}
