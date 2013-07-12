package com.bitknights.locationalarm;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.bitknights.locationalarm.LaunchActivity.Menu;
import com.bitknights.locationalarm.utils.image.ImageManager;
import com.bitknights.locationalarm.utils.image.ImageUtils;
import com.bitknights.locationalarm.view.StateRelativeLayout;
import com.bitknights.locationalarm.view.StateRelativeLayout.OnVisibilityChangedListener;


public abstract class BaseFragment extends Fragment implements OnVisibilityChangedListener {

    protected RelativeLayout mRootLayout;
    protected ImageManager mImageManager;

    protected int mMenuIndex;
    protected boolean mLockUIActions;

    protected abstract View getContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);

	if (null != savedInstanceState) {
	    doRestoreInstanceState(savedInstanceState);
	}
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	View view = inflater.inflate(R.layout.root, null, false);
	if (view instanceof StateRelativeLayout) {
	    ((StateRelativeLayout) view).setOnVisibilityChangedListener(this);
	}
	
	mRootLayout = (RelativeLayout) view.findViewById(R.root.rootLayout);

	View contentView = getContentView(inflater, this.mRootLayout, savedInstanceState);
	this.mRootLayout.addView(contentView);

	view.setTag(this);

	return view;
    }

    @Override
    public void onDestroyView() {
	if (this.mRootLayout != null) {
	    this.mRootLayout.removeAllViews();

	    this.mRootLayout = null;
	}

	final View view = getView();
	if (view instanceof StateRelativeLayout) {
	    ((StateRelativeLayout) view).setOnVisibilityChangedListener(null);
	}

	ImageUtils.unbindDrawables(view);

	super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
	super.onSaveInstanceState(outState);

	if (outState != null) {
	    doSaveInstanceState(outState);
	}
    }

    public void setTitleText(int titleResId) {
	getActivity().setTitle(titleResId);
    }

    public void setTitleText(Spanned ss) {
	getActivity().setTitle(ss);
    }

    public String getStackName() {
	return getClass().getName();
    }

    public void doHeavyLoad() {
	// Used in descendants to do heavy UI works after the main animation
    }

    protected void doRestoreInstanceState(Bundle savedInstanceState) {
	this.mMenuIndex = savedInstanceState.getInt("menuIndex");
	this.mLockUIActions = savedInstanceState.getBoolean("lockUIActions");
    }

    protected void doSaveInstanceState(Bundle outState) {
	outState.putInt("menuIndex", this.mMenuIndex);
	outState.putBoolean("lockUIActions", this.mLockUIActions);
    }

    protected void showHomePage() {
	final LaunchActivity activity = (LaunchActivity) getActivity();
	activity.selectHomeInMenu();
	activity.showLocations();
    }

    public void lockUIActions() {
	this.mLockUIActions = true;
    }

    public void unlockUIActions() {
	this.mLockUIActions = false;
    }

    public Menu getMenuStyle() {
	return Menu.PRIMARY;
    }

    public int getMenuIndex() {
	return this.mMenuIndex;
    }

    public void setMenuIndex(int index) {
	this.mMenuIndex = index;
    }

    @Override
    public void onVisibilityChanged(int visibility) {
    }
}
