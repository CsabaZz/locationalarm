package com.bitknights.locationalarm;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ViewFlipper;

import com.bitknights.locationalarm.info.AboutFragment;
import com.bitknights.locationalarm.locations.LocationsFragment;
import com.bitknights.locationalarm.menu.MenuListFragment;
import com.bitknights.locationalarm.settings.SettingsFragment;
import com.bitknights.locationalarm.utils.Utils;
import com.bitknights.locationalarm.utils.image.ImageManager;
import com.bitknights.locationalarm.utils.image.ImageUtils;
import com.bitknights.locationalarm.view.StateViewFlipper;
import com.bitknights.locationalarm.view.StateViewFlipper.OnChildAddedListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class LaunchActivity extends Activity implements Runnable, Callback, AnimationListener,
	OnChildAddedListener, OnClickListener {
    
    public interface OnBackPressedListener {
	public boolean onBackPressed();
    }
    
    private static class DrawerToggle extends ActionBarDrawerToggle {
	private LaunchActivity mActivity;

	public DrawerToggle(LaunchActivity activity, DrawerLayout drawerLayout, int drawerImageRes,
		int openDrawerContentDescRes, int closeDrawerContentDescRes) {
	    super(activity, drawerLayout, drawerImageRes, openDrawerContentDescRes, closeDrawerContentDescRes);
	    this.mActivity = activity;
	}
	
        public void onDrawerClosed(View view) {
            if(null == this.mActivity) {
        	return;
            }
            
            this.mActivity.getActionBar().setTitle(this.mActivity.mTitle);
            this.mActivity.invalidateOptionsMenu();
        }

        public void onDrawerOpened(View drawerView) {
            if(null == this.mActivity) {
        	return;
            }
            
            this.mActivity.getActionBar().setTitle(this.mActivity.mMenuTitle);
            this.mActivity.invalidateOptionsMenu();
        }

	public void cleanUp() {
            this.mActivity = null;
	}
	
    }

    public enum Menu {
	PRIMARY, SECONDARY
    }
    
    private static final int MESSAGE_ANIMATION = 0;

    private static final int DELAY_ANIMATION = 200;
    
    private static final int REQUEST_GOOGLEPLAY_SERVICES = 1122;
    
    private MenuListFragment mMenuListFragment;
    
    private ArrayList<OnBackPressedListener> mBackPressedObservers;
    
    private DrawerLayout mDrawerLayout;
    private FrameLayout mMenuFrame;
    private StateViewFlipper mContentFlipper;
    
    private DrawerToggle mDrawerToggle;
    
    private Animation mLeftInAnimation;
    private Animation mLeftOutAnimation;

    private Animation mRightInAnimation;
    private Animation mRightOutAnimation;
    
    private LocationsFragment mLocationFragment;
    private BaseFragment mNewFragment;

    private Handler mAnimHandler;
    
    private boolean mIsAnimationRunning;
    
    private CharSequence mTitle;
    private CharSequence mMenuTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
	StaticContextApplication.addActivityContext(this);
	
	this.mBackPressedObservers = new ArrayList<LaunchActivity.OnBackPressedListener>();

	super.onCreate(savedInstanceState);

	setContentView(R.layout.main);
	
	this.mDrawerLayout = (DrawerLayout) findViewById(R.id.main_drawer);
	this.mMenuFrame = (FrameLayout) findViewById(R.id.menu_frame);
	
	getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
	
	this.mTitle = getTitle();
	this.mMenuTitle = getString(R.string.menuTitle);
	
	this.mDrawerToggle = new DrawerToggle(this, this.mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close);
	
	this.mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START); 
	this.mDrawerLayout.setDrawerListener(this.mDrawerToggle);

	this.mContentFlipper = (StateViewFlipper) findViewById(R.id.contentFlipper);
	this.mContentFlipper.setOnChildAddedListener(this);

	if (null == savedInstanceState) {
	    // Lazy initialize the activity to get start as fast as we can
	    this.mContentFlipper.postDelayed(this, 1000);
	} else {
	    this.mMenuListFragment = (MenuListFragment) findFragmentById(R.id.menu_frame);
	    runOnUiThread(this);
	}
    }
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        this.mDrawerToggle.syncState();
    }

    @Override
    public void run() {
	if(this.isFinishing() || this.mContentFlipper == null) {
	    return;
	}
	
	this.mAnimHandler = new Handler(this);

	this.mLeftInAnimation = AnimationUtils.loadAnimation(this, R.anim.push_left_in);
	this.mLeftOutAnimation = AnimationUtils.loadAnimation(this, R.anim.push_left_out);

	this.mRightInAnimation = AnimationUtils.loadAnimation(this, R.anim.push_right_in);
	this.mRightOutAnimation = AnimationUtils.loadAnimation(this, R.anim.push_right_out);

	this.mLeftInAnimation.setAnimationListener(this);
	this.mLeftOutAnimation.setAnimationListener(this);

	this.mRightInAnimation.setAnimationListener(this);
	this.mRightOutAnimation.setAnimationListener(this);

	if (null == this.mMenuListFragment) {
	    this.mMenuListFragment = new MenuListFragment();

	    final FragmentManager manager = getFragmentManager();
	    final FragmentTransaction transaction = manager.beginTransaction();
	    transaction.replace(R.id.menu_frame, this.mMenuListFragment);
	    transaction.commit();
	}

	addHomePage();
    }
    

    @Override
    protected void onResume() {
	super.onResume();

	if (Utils.isOnline()) {
	    final Context context = StaticContextApplication.getAppContext();
	    int checkGooglePlayServices = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
	    if (checkGooglePlayServices != ConnectionResult.SUCCESS) {
		GooglePlayServicesUtil.getErrorDialog(checkGooglePlayServices, this, REQUEST_GOOGLEPLAY_SERVICES).show();
	    }
	} else {
	    AlertDialog.Builder dialog = new AlertDialog.Builder(this);
	    dialog.setCancelable(false);
	    dialog.setTitle(R.string.netTitle);
	    dialog.setMessage(R.string.netText);
	    dialog.setPositiveButton(android.R.string.ok, this);
	    dialog.show();
	}
    }
    
    @Override
    protected void onActivityResult(int responseCode, int requestCode, Intent data) {
        super.onActivityResult(responseCode, requestCode, data);
        
        if(requestCode == REQUEST_GOOGLEPLAY_SERVICES) {
            // TODO implement me!!!
        }
    }

    @Override
    protected void onStop() {
	if (isFinishing() && null != this.mMenuListFragment) {
	    final FragmentManager manager = getFragmentManager();
	    final FragmentTransaction transaction = manager.beginTransaction();
	    transaction.remove(this.mMenuListFragment);
	    transaction.commit();
	}

	super.onStop();
    }

    @Override
    protected void onDestroy() {
	this.mDrawerToggle.cleanUp();
	this.mDrawerToggle = null;
	
	this.mDrawerLayout.setDrawerListener(null);
	this.mDrawerLayout = null;

	this.mMenuListFragment = null;
	this.mLocationFragment = null;
	this.mNewFragment = null;
	
	this.mContentFlipper = null;

	this.mAnimHandler = null;

	this.mLeftInAnimation = destroyAnimation(this.mLeftInAnimation);
	this.mLeftOutAnimation = destroyAnimation(this.mLeftOutAnimation);

	this.mRightInAnimation = destroyAnimation(this.mRightInAnimation);
	this.mRightOutAnimation = destroyAnimation(this.mRightOutAnimation);

	ImageManager.clearDiscCache();
	ImageUtils.destroyAll();

	super.onDestroy();

	StaticContextApplication.removeActivityContext(this);
    }
    
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu); 
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
	boolean menuOpened = isMenuOpened();
        menu.findItem(R.id.action_search).setVisible(!menuOpened); 
        return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	if (this.mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
	
	switch(item.getItemId()) {
        case R.id.action_search:
            return true;
        default:
            return super.onOptionsItemSelected(item);
        } 
    }
    
    @Override
    public void setTitle(int titleId) {
        super.setTitle(titleId);
        updateActionBarTitle();
    }
    
    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        updateActionBarTitle();
    }
    
    private void updateActionBarTitle() {
	this.mTitle = getTitle();
        getActionBar().setTitle(this.mTitle);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mDrawerToggle.onConfigurationChanged(newConfig);
    }

    
    public void addOnBackPressedListener(OnBackPressedListener listener) {
	this.mBackPressedObservers.add(listener);
    }

    public void removeOnBackPressedListener(OnBackPressedListener listener) {
	this.mBackPressedObservers.remove(listener);
    }

    private Animation destroyAnimation(Animation animation) {
	if (null != animation) {
	    animation.setAnimationListener(null);
	}

	return null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
	if (this.mIsAnimationRunning) {
	    return true;
	} else if (keyCode == KeyEvent.KEYCODE_BACK && this.mDrawerLayout.isDrawerOpen(this.mMenuFrame)) {
	    closeMenu();
	    return true;
	} else if (keyCode == KeyEvent.KEYCODE_MENU) {
	    toggle();
	    return true;
	} else if (keyCode == KeyEvent.KEYCODE_BACK) {
	    int size = null == this.mBackPressedObservers ? 0 : this.mBackPressedObservers.size();
	    for (int i = 0; i < size; ++i) {
		final OnBackPressedListener listener = this.mBackPressedObservers.get(i);
		if (listener.onBackPressed()) {
		    return true;
		}
	    }

	    if (this.mContentFlipper.getChildCount() > 1) {
		final FragmentManager manager = getFragmentManager();
		BaseFragment fragment = (BaseFragment) manager.findFragmentByTag(String.valueOf(this.mContentFlipper
			.getChildCount()));
		if (fragment != null) {
		    fragment.lockUIActions();
		}

		showPreviousContent();

		return true;
	    }
	}

	return super.onKeyDown(keyCode, event);
    }

    private void addHomePage() {
	Fragment fragment = findFragmentByTag(LocationsFragment.getFragmentTag());
	if (fragment != null) {
	    this.mLocationFragment = (LocationsFragment) fragment;
	} else {
	    final FragmentManager manager = getFragmentManager();
	    final FragmentTransaction transaction = manager.beginTransaction();

	    this.mLocationFragment = LocationsFragment.instantiate(this, MenuListFragment.HOMEPAGE_POSITION);
	    transaction.add(this.mContentFlipper.getId(), this.mLocationFragment,
		    this.mLocationFragment.getStackName());
	    transaction.commit();
	}
    }

    public void changeFragment(BaseFragment fragment) {
	changeFragment(fragment, true);
    }

    public void changeFragment(BaseFragment fragment, boolean animate) {
	final ViewFlipper contentFlipper = this.mContentFlipper;

	if (fragment.isAdded()) {
	    this.mContentFlipper.addView(fragment.getView());
	} else {
	    this.mNewFragment = fragment;

	    final FragmentManager manager = getFragmentManager();
	    final FragmentTransaction transaction = manager.beginTransaction();
	    transaction.add(contentFlipper.getId(), fragment, fragment.getStackName());

	    transaction.setTransition(FragmentTransaction.TRANSIT_NONE);
	    transaction.commit();
	}
    }

    private void showNextContent() {
	closeMenu();
	
	final ViewFlipper contentFlipper = this.mContentFlipper;
	contentFlipper.setInAnimation(this.mRightInAnimation);
	contentFlipper.setOutAnimation(this.mLeftOutAnimation);

	contentFlipper.setDisplayedChild(contentFlipper.getChildCount() - 1);
    }

    private void showPreviousContent() {
	this.mIsAnimationRunning = true;

	final ViewFlipper contentFlipper = this.mContentFlipper;

	contentFlipper.setInAnimation(this.mLeftInAnimation);
	contentFlipper.setOutAnimation(this.mRightOutAnimation);

	contentFlipper.showPrevious();
    }

    private Fragment findFragmentById(int parentId) {
	return getFragmentManager().findFragmentById(parentId);
    }

    private Fragment findFragmentByTag(String fragmentTag) {
	return getFragmentManager().findFragmentByTag(fragmentTag);
    }

    public boolean isMenuOpened() {
	return this.mDrawerLayout.isDrawerOpen(this.mMenuFrame);
    }

    public void openMenu() {
	this.mDrawerLayout.openDrawer(this.mMenuFrame);
    }

    public void closeMenu() {
	this.mDrawerLayout.closeDrawer(this.mMenuFrame);
    }
    
    public void toggle() {
	if(isMenuOpened()) {
	    closeMenu();
	} else {
	    openMenu();
	}
    }

    public void selectHomeInMenu() {
	this.mMenuListFragment.selectHome();
    }

    public void showLocations() {
	Fragment frmgt = findFragmentByTag(LocationsFragment.getFragmentTag());
	if (frmgt == null) {
	    LocationsFragment articleListFragment = LocationsFragment.instantiate(this,
		    MenuListFragment.HOMEPAGE_POSITION);
	    changeFragment(articleListFragment);
	} else {
	    changeFragment((LocationsFragment) frmgt);
	}
    }

    public void showSettings(int position) {
	Fragment frmgt = findFragmentByTag(SettingsFragment.getFragmentTag());
	if (frmgt == null) {
	    SettingsFragment settingsFragment = SettingsFragment.instantiate(this, position);
	    changeFragment(settingsFragment);
	} else {
	    changeFragment((SettingsFragment) frmgt);
	}
    }

    public void showAbout(int position) {
	Fragment frmgt = findFragmentByTag(AboutFragment.getFragmentTag());
	if (frmgt == null) {
	    AboutFragment aboutFragment = AboutFragment.instantiate(this, position);
	    changeFragment(aboutFragment);
	} else {
	    changeFragment((AboutFragment) frmgt);
	}
    }

    @Override
    public void onClick(DialogInterface arg0, int arg1) {
	finish();
    }
    
    @Override
    public boolean handleMessage(Message msg) {
	if (isFinishing() || this.mContentFlipper == null) {
	    return true;
	}

	if (msg.what == -1) {
	    this.mLeftInAnimation.setAnimationListener(LaunchActivity.this);
	    this.mLeftOutAnimation.setAnimationListener(LaunchActivity.this);

	    this.mRightInAnimation.setAnimationListener(LaunchActivity.this);
	    this.mRightOutAnimation.setAnimationListener(LaunchActivity.this);
	    
	    return true;
	} else if (msg.what == MESSAGE_ANIMATION) {
	    final Animation animation = (Animation) msg.obj;

	    if (animation == this.mRightInAnimation) {
		if (this.mNewFragment != null) {
		    this.mNewFragment.doHeavyLoad();
		    this.mNewFragment = null;
		}
	    } else if (animation == this.mLeftInAnimation) {
		int displayedChildPosition = this.mContentFlipper.getDisplayedChild();
		if (displayedChildPosition < this.mContentFlipper.getChildCount()) {
		    int previousPosition = displayedChildPosition + 1;
		    View lastChild = this.mContentFlipper.getChildAt(previousPosition);
		    if (lastChild != null) {
			this.mContentFlipper.removeView(lastChild);

			final FragmentManager manager = getFragmentManager();
			final FragmentTransaction transaction = manager.beginTransaction();
			transaction.remove((Fragment) lastChild.findViewById(R.root.rootLayout).getTag());
			transaction.setTransition(FragmentTransaction.TRANSIT_NONE);
			transaction.commit();
		    }
		}

		final View child = this.mContentFlipper.getChildAt(displayedChildPosition);
		final View childRoot = child.findViewById(R.root.rootLayout);
		final BaseFragment fragment = (BaseFragment) childRoot.getTag();

		final int menuIndex = fragment.getMenuIndex();
		this.mMenuListFragment.select(menuIndex);

		this.mIsAnimationRunning = false;
	    }

	    return true;
	} else {
	    return false;
	}
    }

    @Override
    public void onChildAdded(View child) {
	if (this.mContentFlipper.getChildCount() > 1) {
	    showNextContent();
	}
    }

    @Override
    public void onAnimationEnd(Animation animation) {
	if (animation == this.mRightInAnimation) {
	    closeMenu();
	}

	Message msg = Message.obtain(this.mAnimHandler, MESSAGE_ANIMATION, animation);
	this.mAnimHandler.sendMessageDelayed(msg, DELAY_ANIMATION);
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
	// Not used by now
    }

    @Override
    public void onAnimationStart(Animation animation) {
	// Not used by now
    }

}
