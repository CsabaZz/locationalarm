/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bitknights.locationalarm.utils.image;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.widget.ImageView;

import com.bitknights.locationalarm.R;
import com.bitknights.locationalarm.StaticContextApplication;
import com.bitknights.locationalarm.utils.Utils;

/**
 * Asynchronously loads contact photos and maintains a cache of photos.
 */
public abstract class ImageManager {
    static final String TAG = Utils.TAG + ImageManager.class.getSimpleName();
    static final boolean DEBUG = false; // Don't submit with true
    static final boolean DEBUG_SIZES = false; // Don't submit with true
    static final boolean DEBUG_ALL = false; // Don't submit any debug line

    /**
     * Caches 180dip in pixel. This is used to detect whether to show the hires
     * or lores version of the default avatar
     */
    private static int s180DipInPixel = -1;

    public static final String IMAGE_SERVICE = "contactPhotos";

    public static interface NewUrlRequest {
	String requestNewUrl(String oldUrl);
    }

    /**
     * A holder for either a Uri or an id and a flag whether this was requested
     * for the dark or light theme
     */
    protected static final class Request {
	private String mId;
	private String mUrl;
	private final int mRequestedExtent;
	private final DefaultImageProvider mDefaultProvider;
	private final NewUrlRequest mNewUrlRequest;

	private Request(String id, String url, int requestedExtent, DefaultImageProvider defaultProvider,
		NewUrlRequest newUrlRequest) {
	    mId = id;
	    mUrl = url;
	    mRequestedExtent = requestedExtent;
	    mDefaultProvider = defaultProvider;
	    mNewUrlRequest = newUrlRequest;
	}

	public String requestNewUrl(String oldUrl) {
	    return null == mNewUrlRequest ? null : mNewUrlRequest.requestNewUrl(oldUrl);
	}

	public static Request createCopy(String url, int requestedExtent, DefaultImageProvider defaultProvider,
		NewUrlRequest newUrlRequest) {
	    return new Request(String.valueOf(url.hashCode()), url, requestedExtent, defaultProvider, newUrlRequest);
	}

	public boolean isUrlRequest() {
	    return mUrl != null;
	}

	public String getUrl() {
	    return mUrl;
	}

	public String getId() {
	    return mId;
	}

	public int getRequestedExtent() {
	    return mRequestedExtent;
	}

	public DefaultImageProvider getDefaultProvider() {
	    return mDefaultProvider;
	}

	public NewUrlRequest getNewUrlRequest() {
	    return mNewUrlRequest;
	}

	@Override
	public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + ((mId == null) ? 0 : mId.hashCode());
	    result = prime * result + mRequestedExtent;
	    result = prime * result + ((mUrl == null) ? 0 : mUrl.hashCode());
	    return result;
	}

	@Override
	public boolean equals(Object obj) {
	    if (this == obj)
		return true;
	    if (obj == null)
		return false;
	    if (getClass() != obj.getClass())
		return false;
	    final Request that = (Request) obj;
	    if (mId != that.mId)
		return false;
	    if (mRequestedExtent != that.mRequestedExtent)
		return false;
	    if (!mUrl.equals(that.mUrl))
		return false;

	    return true;
	}

	public Object getKey() {
	    return mUrl == null ? mId : mUrl;
	}

	public void applyDefaultImage(ImageView view) {
	    mDefaultProvider.applyDefaultImage(view, mRequestedExtent);
	}

	public boolean isDefaultDrawable(Drawable drawable) {
	    return mDefaultProvider.isDefaultDrawable(drawable);
	}

	public void changeUrlTo(String newUrl) {
	    mUrl = newUrl;
	}
    }

    /**
     * Returns the resource id of the default avatar. Tries to find a resource
     * that is bigger than the given extent (width or height). If extent=-1, a
     * thumbnail avatar is returned
     */
    public static int getDefaultAvatarResId(Context context, int extent) {
	// TODO: Is it worth finding a nicer way to do hires/lores here?
	if (s180DipInPixel == -1) {
	    Resources r = context.getResources();
	    s180DipInPixel = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 180, r.getDisplayMetrics());
	}

	final boolean hires = (extent != -1) && (extent > s180DipInPixel);
	return getDefaultAvatarResId(hires);
    }

    public static int getDefaultAvatarResId(boolean hires) {
	return 0;
    }

    public static abstract class DefaultImageProvider {
	/**
	 * Applies the default avatar to the ImageView. Extent is an indicator
	 * for the size (width or height). If darkTheme is set, the avatar is
	 * one that looks better on dark background
	 */
	public abstract void applyDefaultImage(ImageView view, int extent);

	public abstract boolean isDefaultDrawable(Drawable drawable);
    }

    private static class BlankDefaultImageProvider extends DefaultImageProvider {
	private static Drawable sDrawable;

	@Override
	public void applyDefaultImage(ImageView view, int extent) {
	    if (sDrawable == null) {
		final int transparentColor = Color.TRANSPARENT;
		sDrawable = new ColorDrawable(transparentColor);
	    }
	    view.setImageDrawable(sDrawable);
	}

	@Override
	public boolean isDefaultDrawable(Drawable drawable) {
	    return sDrawable == drawable;
	}
    }

    private static class BlackDefaultImageProvider extends DefaultImageProvider {
	private static Drawable sDrawable;

	@Override
	public void applyDefaultImage(ImageView view, int extent) {
	    if (sDrawable == null) {
		final Context context = view.getContext();
		final Resources resources = context.getResources();
		final int blackColor = resources.getColor(R.color.blackColor);
		sDrawable = new ColorDrawable(blackColor);
	    }
	    view.setImageDrawable(sDrawable);
	}

	@Override
	public boolean isDefaultDrawable(Drawable drawable) {
	    return sDrawable == drawable;
	}
    }

    /*
     * An InputStream that skips the exact number of bytes provided, unless it
     * reaches EOF.
     */
    static class FlushedInputStream extends FilterInputStream {
	public FlushedInputStream(InputStream inputStream) {
	    super(inputStream);
	}

	@Override
	public long skip(long n) throws IOException {
	    long totalBytesSkipped = 0L;
	    while (totalBytesSkipped < n) {
		long bytesSkipped = in.skip(n - totalBytesSkipped);
		if (bytesSkipped == 0L) {
		    int b = read();
		    if (b < 0) {
			break; // we reached EOF
		    } else {
			bytesSkipped = 1; // we read one byte
		    }
		}
		totalBytesSkipped += bytesSkipped;
	    }
	    return totalBytesSkipped;
	}
    }

    public static final DefaultImageProvider DEFAULT_BLANK = new BlankDefaultImageProvider();

    public static final DefaultImageProvider DEFAULT_BLACK = new BlackDefaultImageProvider();

    public static synchronized ImageManager createImageManager() {
	if (android.os.Build.VERSION.SDK_INT < 14) {
	    return SDK1.createImageManager();
	} else {
	    return SDK14.createImageManager();
	}
    }

    @TargetApi(1)
    static class SDK1 {
	public static ImageManager createImageManager() {
	    return new ImageDownloader();
	}
    }

    @TargetApi(14)
    static class SDK14 {
	public static ImageManager createImageManager() {
	    return new ImageManager14();
	}
    }

    public static void clearDiscCache() {
	final Context context = StaticContextApplication.getAppContext();
	File dir = null == context ? null : context.getFilesDir();

	if (null != dir && dir.exists()) {
	    File[] files = dir.listFiles();

	    if (files != null) {
		for (File f : files) {
		    if (f != null && f.getName().endsWith("_img")) {
			f.delete();
		    }
		}
	    }
	}
    }

    /**
     * Load photo into the supplied image view. If the photo is already cached,
     * it is displayed immediately. Otherwise a request is sent to load the
     * photo from the location specified by the URI.
     * 
     * @param view
     *            The target view
     * @param photoUri
     *            The uri of the photo to load
     * @param requestedExtent
     *            Specifies an approximate Max(width, height) of the targetView.
     *            This is useful if the source image can be a lot bigger that
     *            the target, so that the decoding is done using efficient
     *            sampling. If requestedExtent is specified, no sampling of the
     *            image is performed
     * @param darkTheme
     *            Whether the background is dark. This is used for default
     *            avatars
     * @param defaultProvider
     *            The provider of default avatars (this is used if photoUri
     *            doesn't refer to an existing image)
     */
    public abstract void loadImageByUrl(ImageView view, String photoUrl, int requestedExtent,
	    DefaultImageProvider defaultProvider, NewUrlRequest newUrlRequest);

    /**
     * Calls {@link #loadImageByUrl(ImageView, String, boolean, NewUrlRequest)}.
     */
    public final void loadImageByUrl(ImageView view, String photoUrl, NewUrlRequest newUrlRequest) {
	loadImageByUrl(view, photoUrl, -1, DEFAULT_BLACK, newUrlRequest);
    }

    /**
     * Calls
     * {@link #loadImageByUrl(ImageView, String, boolean, DefaultImageProvider)}
     * with {@link #DEFAULT_AVATAR}.
     */
    public final void loadImageByUrl(ImageView view, String photoUrl, DefaultImageProvider defaultProvider) {
	loadImageByUrl(view, photoUrl, -1, defaultProvider, null);
    }

    /**
     * Calls
     * {@link #loadImageByUrl(ImageView, String, boolean, DefaultImageProvider)}
     * with {@link #DEFAULT_AVATAR} and with the assumption, that the image is a
     * thumbnail
     */
    public final void loadImageByUrl(ImageView view, String photoUrl) {
	loadImageByUrl(view, photoUrl, -1, DEFAULT_BLACK, null);
    }

    /**
     * Remove photo from the supplied image view. This also cancels current
     * pending load request inside this photo manager.
     */
    public abstract void removePhoto(ImageView view);

    /**
     * Temporarily stops loading photos from the database.
     */
    public abstract void pause();

    /**
     * Resumes loading photos from the database.
     */
    public abstract void resume();

    /**
     * Stores the given bitmap directly in the LRU bitmap cache.
     * 
     * @param originalRequest
     *            The URI of the photo (for future requests).
     * @param bitmap
     *            The bitmap.
     * @param photoBytes
     *            The bytes that were parsed to create the bitmap.
     */
    public abstract void cacheBitmap(Request originalRequest, Bitmap bitmap, byte[] photoBytes);

    /**
     * Initiates a background process that over time will fill up cache with
     * preload photos.
     */
    public abstract void preloadPhotosInBackground();
}