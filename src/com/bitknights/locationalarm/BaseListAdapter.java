package com.bitknights.locationalarm;

import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bitknights.locationalarm.utils.image.ImageManager;
import com.bitknights.locationalarm.utils.image.ImageManager.NewUrlRequest;

public abstract class BaseListAdapter<E> extends BaseAdapter {

    private ImageManager mImageManager;

    public abstract void cleanUp();

    protected void loadImageToView(ImageView imageview, String link) {
	imageview.setTag(link);
	loadImage(imageview, link);
    }

    private void loadImage(ImageView imageview, String link) {
	if (mImageManager == null) {
	    Context context = StaticContextApplication.getAppContext();
	    this.mImageManager = (ImageManager) context.getSystemService(ImageManager.IMAGE_SERVICE);
	}

	this.mImageManager.loadImageByUrl(imageview, link);
    }

    protected void loadImageToView(ImageView imageview, String link, NewUrlRequest newUrlRequest) {
	imageview.setTag(link);
	loadImage(imageview, link, newUrlRequest);
    }

    private void loadImage(ImageView imageview, String link, NewUrlRequest newUrlRequest) {
	if (mImageManager == null) {
	    Context context = StaticContextApplication.getAppContext();
	    this.mImageManager = (ImageManager) context.getSystemService(ImageManager.IMAGE_SERVICE);
	}

	this.mImageManager.loadImageByUrl(imageview, link, newUrlRequest);
    }

    public void setImageManager(ImageManager imageManager) {
	this.mImageManager = imageManager;
    }

}
