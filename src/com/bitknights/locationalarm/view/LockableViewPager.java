package com.bitknights.locationalarm.view;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class LockableViewPager extends ViewPager {
    private static final String STATE_SUPER = "LockableViewPager::SuperState";
    private static final String STATE_LOCKED = "LockableViewPager::LockedState";
    
    private boolean mLocked;

    public LockableViewPager(Context context) {
	super(context);
    }

    public LockableViewPager(Context context, AttributeSet attrs) {
	super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
	if(this.mLocked) {
	    return false;
	} else {
	    return super.onInterceptTouchEvent(event);
	}
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
	if(this.mLocked) {
	    return false;
	} else {
	    return super.onTouchEvent(event);
	}
    }

    @Override
    public final void onRestoreInstanceState(Parcelable state) {
	if (state instanceof Bundle) {
	    Bundle bundle = (Bundle) state;

	    mLocked = bundle.getBoolean(STATE_LOCKED, false);
	    
	    final Parcelable superState = bundle.getParcelable(STATE_SUPER);
	    super.onRestoreInstanceState(superState);
	} else {
	    super.onRestoreInstanceState(null);
	}
    }
    
    @Override
    public Parcelable onSaveInstanceState() {
	Bundle bundle = new Bundle();

	bundle.putBoolean(STATE_LOCKED, mLocked);
	bundle.putParcelable(STATE_SUPER, super.onSaveInstanceState());

	return bundle;
    }

    public boolean isLocked() {
	return this.mLocked;
    }

    public void setLocked(boolean locked) {
	this.mLocked = locked;
    }

}
