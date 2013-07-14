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
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ShareActionProvider;

import com.bitknights.locationalarm.info.AboutFragment;
import com.bitknights.locationalarm.locations.LocationsFragment;
import com.bitknights.locationalarm.menu.MenuListFragment;
import com.bitknights.locationalarm.settings.SettingsFragment;
import com.bitknights.locationalarm.utils.Utils;
import com.bitknights.locationalarm.utils.image.ImageManager;
import com.bitknights.locationalarm.utils.image.ImageUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class LaunchActivity extends Activity implements Runnable, OnClickListener {
    
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
    
    private static final int REQUEST_GOOGLEPLAY_SERVICES = 1122;
    
    private MenuListFragment mMenuListFragment;
    
    private ArrayList<OnBackPressedListener> mBackPressedObservers;
    
    private DrawerLayout mDrawerLayout;
    private FrameLayout mMenuFrame;
    private FrameLayout mContentFrame;
    
    private DrawerToggle mDrawerToggle;
    
    private LocationsFragment mLocationFragment;
    
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

	this.mContentFrame = (FrameLayout) findViewById(R.id.content_frame);

	if (null == savedInstanceState) {
	    // Lazy initialize the activity to get start as fast as we can
	    this.mContentFrame.postDelayed(this, 1000);
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
	if(this.isFinishing() || this.mContentFrame == null) {
	    return;
	}

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
	    showInstallDialog();
	} else {
	    showNoConnectionDialog();
	}
    }
    
    private void showInstallDialog() {
	final Context context = StaticContextApplication.getAppContext();
	int checkGooglePlayServices = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
	if (checkGooglePlayServices != ConnectionResult.SUCCESS) {
	    GooglePlayServicesUtil.getErrorDialog(checkGooglePlayServices, this, REQUEST_GOOGLEPLAY_SERVICES).show();
	}
    }

    private void showNoConnectionDialog() {
	AlertDialog.Builder dialog = new AlertDialog.Builder(this);
	dialog.setCancelable(false);
	dialog.setTitle(R.string.netTitle);
	dialog.setMessage(R.string.netText);
	dialog.setPositiveButton(android.R.string.ok, this);
	dialog.show();
    }

    @Override
    protected void onActivityResult(int responseCode, int requestCode, Intent data) {
        super.onActivityResult(responseCode, requestCode, data);
        
        if(requestCode == REQUEST_GOOGLEPLAY_SERVICES) {
	    showInstallDialog();
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
	if(null != this.mDrawerToggle){
	    this.mDrawerToggle.cleanUp();
	}
	
	this.mDrawerToggle = null;

	if(null != this.mDrawerLayout){
	    this.mDrawerLayout.setDrawerListener(null);
	}
	
	this.mDrawerLayout = null;

	this.mMenuListFragment = null;
	this.mLocationFragment = null;
	
	if(null != this.mContentFrame){
	    this.mContentFrame.removeAllViews();
	}
	
	this.mContentFrame = null;

	ImageManager.clearDiscCache();
	ImageUtils.destroyAll();

	super.onDestroy();

	StaticContextApplication.removeActivityContext(this);
    }
    
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        //MenuItem searchItem = menu.findItem(R.id.action_search);
        //SearchView searchView = (SearchView) searchItem.getActionView();

        MenuItem shareItem = menu.findItem(R.id.action_share);
        ShareActionProvider shareActionProvider = (ShareActionProvider) shareItem.getActionProvider();
        shareActionProvider.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME); 
        shareActionProvider.setShareIntent(createShareIntent());
        
        return super.onCreateOptionsMenu(menu);
    }
    
    private Intent createShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/*");
        shareIntent.putExtra(Intent.EXTRA_TITLE, ""); //TODO put valid title into share Intent
        shareIntent.putExtra(Intent.EXTRA_TEXT, ""); //TODO put valid text into share Intent
        return shareIntent;
    }

    @Override
    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
	boolean menuOpened = isMenuOpened();
        menu.findItem(R.id.action_share).setVisible(!menuOpened); 
        menu.findItem(R.id.action_search).setVisible(!menuOpened); 
        return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	if (this.mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else {
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
	if (keyCode == KeyEvent.KEYCODE_BACK && this.mDrawerLayout.isDrawerOpen(this.mMenuFrame)) {
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
	    transaction.add(this.mContentFrame.getId(), this.mLocationFragment,
		    this.mLocationFragment.getStackName());
	    transaction.commit();
	}
    }

    public void changeFragment(BaseFragment fragment) {
	changeFragment(fragment, true);
    }

    public void changeFragment(BaseFragment fragment, boolean animate) {
	final FrameLayout contentFrame = this.mContentFrame;

	if (fragment.isAdded()) {
	    contentFrame.addView(fragment.getView());
	} else {
	    final FragmentManager manager = getFragmentManager();
	    final FragmentTransaction transaction = manager.beginTransaction();
	    transaction.setCustomAnimations(R.animator.push_right_in, R.animator.push_left_out, R.animator.push_left_in, R.animator.push_right_out);
	    transaction.addToBackStack(fragment.getStackName());
	    
	    transaction.replace(contentFrame.getId(), fragment, fragment.getStackName());
	    transaction.commit();
	}
	
	closeMenu();
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

}
