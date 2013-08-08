
package com.bitknights.locationalarm.locations;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bitknights.locationalarm.BaseFragment;
import com.bitknights.locationalarm.LaunchActivity;
import com.bitknights.locationalarm.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;

public class LocationsFragment extends BaseFragment {

    public static LocationsFragment instantiate(LaunchActivity launchActivity, int homepagePosition) {
        LocationsFragment fragment = new LocationsFragment();
        return fragment;
    }

    public static String getFragmentTag() {
        return LocationsFragment.class.getName();
    }

    private static final String FRAGMENT_TAG_MAP = "Fragments::Map";

    private GoogleMapOptions mMapOptions;

    private SupportMapFragment mMapFragment;
    private GoogleMap mMap;

    //private Point mTouchedPoint;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //this.mTouchedPoint = new Point(0, 0);

        this.mMapOptions = new GoogleMapOptions();
        this.mMapOptions.mapType(GoogleMap.MAP_TYPE_NORMAL);
        this.mMapOptions.compassEnabled(true);
        this.mMapOptions.tiltGesturesEnabled(true);
        this.mMapOptions.rotateGesturesEnabled(true);
        this.mMapOptions.scrollGesturesEnabled(true);
        this.mMapOptions.zoomControlsEnabled(true);
        this.mMapOptions.zoomGesturesEnabled(true);
    }

    @Override
    protected View getContentView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.map, null);

        final FragmentManager manager = getChildFragmentManager();
        if (null == savedInstanceState) {
            this.mMapFragment = SupportMapFragment.newInstance(this.mMapOptions);

            final FragmentTransaction transaction = manager.beginTransaction();
            transaction.add(R.map.mapLayout, this.mMapFragment, FRAGMENT_TAG_MAP);
            transaction.commit();
        } else {
            this.mMapFragment = (SupportMapFragment) manager.findFragmentByTag(FRAGMENT_TAG_MAP);
        }

        tryToSetupMap();

        return contentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        setTitleText(R.string.mapTitle);

        tryToSetupMap();
    }

    @Override
    public void onDestroyView() {
        if (null != this.mMapFragment && this.mMapFragment.isInLayout()) {
            final FragmentManager manager = getChildFragmentManager();
            final FragmentTransaction transaction = manager.beginTransaction();
            transaction.remove(this.mMapFragment);
            transaction.commit();
        }

        this.mMap = null;
        this.mMapFragment = null;

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        this.mMapOptions = null;

        super.onDestroy();
    }

    private void tryToSetupMap() {
        if (null != this.mMap) {
            return;
        }

        this.mMap = this.mMapFragment.getMap();
        if (null != this.mMap) {
            this.mMap.setMyLocationEnabled(true);
        }
    }

}
