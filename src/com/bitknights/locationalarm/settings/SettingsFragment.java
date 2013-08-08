
package com.bitknights.locationalarm.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bitknights.locationalarm.R;
import com.bitknights.locationalarm.BaseFragment;
import com.bitknights.locationalarm.LaunchActivity;

public class SettingsFragment extends BaseFragment {

    public static SettingsFragment instantiate(LaunchActivity activity, int position) {
        SettingsFragment settingsFragment = new SettingsFragment();
        return settingsFragment;
    }

    public static String getFragmentTag() {
        return SettingsFragment.class.getName();
    }

    @Override
    protected View getContentView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.settings, null);
        return contentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        setTitleText(R.string.settingsTitle);
    }

}
