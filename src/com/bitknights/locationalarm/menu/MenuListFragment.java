package com.bitknights.locationalarm.menu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.bitknights.locationalarm.BaseFragment;
import com.bitknights.locationalarm.LaunchActivity;
import com.bitknights.locationalarm.R;
import com.bitknights.locationalarm.data.local.Menu;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

public class MenuListFragment extends BaseFragment implements OnItemClickListener, OnRefreshListener<ListView> {
    private static final String STATE_DATA = "MenuListFragment::DataState";
    
    private static final int MENU_ID_LOCATIONS = 0;
    private static final int MENU_ID_SETTINGS = 1;
    private static final int MENU_ID_ABOUT = 2;

    public static final int HOMEPAGE_POSITION = 0;

    private PullToRefreshListView mMenuListView;
    private MenuListAdapter mListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
	this.mListAdapter = new MenuListAdapter();
	super.onCreate(savedInstanceState);
    }

    @Override
    protected View getContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	// Not used
	return null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	View view = inflater.inflate(R.layout.menu, null);

	this.mMenuListView = (PullToRefreshListView) view.findViewById(android.R.id.list);
	this.mMenuListView.setAdapter(this.mListAdapter);
	this.mMenuListView.setShowIndicator(false);

	final PullToRefreshListView menuListView = this.mMenuListView;
	menuListView.setOnItemClickListener(this);
	menuListView.setOnRefreshListener(this);

	return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
	if(this.mListAdapter.isEmpty()) {
	    startReadingSections();
	}
    }

    @Override
    public void onDestroyView() {
	this.mListAdapter.cleanUp();
	
	this.mMenuListView.setOnRefreshListener((OnRefreshListener<ListView>) null);
	this.mMenuListView.setOnItemClickListener(null);
	this.mMenuListView.setAdapter(null);
	
	this.mMenuListView = null;

	super.onDestroyView();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRefresh(PullToRefreshBase<ListView> refreshView) {
	startReadingSections();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	final LaunchActivity activity = (LaunchActivity) getActivity();
	final MenuListAdapter listAdapter = this.mListAdapter;

	if (position == listAdapter.getSelected()) {
	    activity.closeMenu();
	    return;
	}

	listAdapter.select(position);
	listAdapter.notifyDataSetChanged();

	final Menu menu = (Menu) listAdapter.getItem(position);
	final int menuPosition = menu.getPosition();

	if (MENU_ID_LOCATIONS == menuPosition) {
	    activity.showLocations();
	} else if (MENU_ID_SETTINGS == menuPosition) {
	    activity.showSettings(position);
	} else if (MENU_ID_ABOUT == menuPosition) {
	    activity.showAbout(position);
	}
    }

    private void startReadingSections() {
	final MenuListAdapter listAdapter = this.mListAdapter;
	listAdapter.add(new Menu(MENU_ID_LOCATIONS, getString(R.string.menuLocations)));
	listAdapter.add(new Menu(MENU_ID_SETTINGS, getString(R.string.menuSettings)));
	listAdapter.add(new Menu(MENU_ID_ABOUT, getString(R.string.menuAbout)));
	
	listAdapter.select(HOMEPAGE_POSITION); // select home page
	listAdapter.notifyDataSetChanged();
	
	this.mMenuListView.onRefreshComplete();
    }

    public void select(int index) {
	final MenuListAdapter listAdapter = this.mListAdapter;
	listAdapter.select(index);
	listAdapter.notifyDataSetChanged();
    }

    public void selectHome() {
	final MenuListAdapter listAdapter = this.mListAdapter;
	listAdapter.select(HOMEPAGE_POSITION);
	listAdapter.notifyDataSetChanged();
    }

    public int getSelectedMenuIndex() {
	return this.mListAdapter.getSelected();
    }
    
    @Override
    protected void doRestoreInstanceState(Bundle savedInstanceState) {
        super.doRestoreInstanceState(savedInstanceState);
        this.mListAdapter.restoreDataStateInstance(savedInstanceState, STATE_DATA);
    }
    
    @Override
    protected void doSaveInstanceState(Bundle outState) {
        super.doSaveInstanceState(outState);
        this.mListAdapter.saveDataStateInstance(outState, STATE_DATA);
    }

}
