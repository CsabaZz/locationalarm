package com.bitknights.locationalarm.menu;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import com.bitknights.locationalarm.R;
import com.bitknights.locationalarm.BaseListAdapter;
import com.bitknights.locationalarm.StaticContextApplication;
import com.bitknights.locationalarm.data.local.Menu;

public class MenuListAdapter extends BaseListAdapter<Menu> {
    private static final String STATE_SELECTION = "MenuListAdapter::SelectionState";

    private static class ViewHolder {
	public CheckedTextView titleText;
    }

    private ArrayList<Menu> mItems;
    private LayoutInflater mInflater;

    private int mSelectedPosition;

    public MenuListAdapter() {
	super();

	this.mItems = new ArrayList<Menu>();
	this.mSelectedPosition = 0;

	final Context context = StaticContextApplication.getAppContext();
	this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public void cleanUp() {
	this.mInflater = null;

	this.mItems.clear();
	this.mItems = null;

	notifyDataSetChanged();
    }

    @Override
    public int getCount() {
	return this.mItems == null ? 0 : this.mItems.size();
    }

    @Override
    public Object getItem(int position) {
	return this.mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
	return position;
    }

    private View newView(int position, View convertView) {
	ViewHolder holder = new ViewHolder();

	View view = this.mInflater.inflate(R.layout.menu_list_item, null, false);
	// view.setOnTouchListener(Utils.touchListener);
	view.setTag(holder);

	holder.titleText = (CheckedTextView) view.findViewById(R.menu_list_item.titleText);

	return view;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
	if (convertView == null) {
	    convertView = newView(position, convertView);
	}

	bindView(position, convertView);

	return convertView;
    }

    private void bindView(int position, View convertView) {
	ViewHolder holder = (ViewHolder) convertView.getTag();
	Menu item = this.mItems.get(position);

	//final Context context = StaticContextApplication.getAppContext();
	//final Resources resources = context.getResources();

	holder.titleText.setChecked(position == this.mSelectedPosition);
	holder.titleText.setText(Html.fromHtml(item.getTitle()));

	//holder.titleText.setBackgroundResource(backgroundResource);
	//holder.titleText.setTextColor(resources.getColorStateList(textColorResource));
    }

    public synchronized void add(Menu section) {
	this.mItems.add(section);
    }

    public synchronized void addAll(ArrayList<Menu> list) {
	final ArrayList<Menu> items = this.mItems;
	items.clear();
	items.addAll(list);

	notifyDataSetChanged();
    }

    public void select(int position) {
	this.mSelectedPosition = position;
    }

    public int getSelected() {
	return this.mSelectedPosition;
    }

    @Override
    public boolean areAllItemsEnabled() {
	return true;
    }

    @SuppressWarnings("unchecked")
    public void restoreDataStateInstance(Bundle savedInstanceState, String key) {
	if(savedInstanceState.containsKey(key)) {
	    this.mItems = (ArrayList<Menu>) savedInstanceState.getSerializable(key);
	}
	
	this.mSelectedPosition = savedInstanceState.getInt(STATE_SELECTION);
    }

    public void saveDataStateInstance(Bundle outState, String key) {
	outState.putSerializable(key, this.mItems);
	outState.putInt(STATE_SELECTION, this.mSelectedPosition);
    }

}
