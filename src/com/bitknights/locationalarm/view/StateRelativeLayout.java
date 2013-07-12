package com.bitknights.locationalarm.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

public class StateRelativeLayout extends RelativeLayout {
    public interface OnVisibilityChangedListener {
	void onVisibilityChanged(int visibility);
    }

    private OnVisibilityChangedListener mOnVisibilityChanged;

    public StateRelativeLayout(Context context) {
	super(context);
    }

    public StateRelativeLayout(Context context, AttributeSet attrs) {
	super(context, attrs);
    }

    public StateRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
	super(context, attrs, defStyle);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
	super.onVisibilityChanged(changedView, visibility);
	if (this.mOnVisibilityChanged != null) {
	    this.mOnVisibilityChanged.onVisibilityChanged(visibility);
	}
    }

    public void setOnVisibilityChangedListener(OnVisibilityChangedListener listener) {
	this.mOnVisibilityChanged = listener;
    }

}
