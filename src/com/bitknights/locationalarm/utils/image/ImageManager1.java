package com.bitknights.locationalarm.utils.image;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.bitknights.locationalarm.StaticContextApplication;

/**
 * Asynchronously loads contact photos and maintains a cache of photos.
 */

class ImageManager1 extends ImageManager implements Callback, ComponentCallbacks {
    private static final String LOADER_THREAD_NAME = "ContactPhotoLoader";

    /**
     * Type of message sent by the UI thread to itself to indicate that some
     * photos need to be loaded.
     */
    private static final int MESSAGE_REQUEST_LOADING = 1;

    /**
     * Type of message sent by the loader thread to indicate that some photos
     * have been loaded.
     */
    private static final int MESSAGE_PHOTOS_LOADED = 2;

    /**
     * Maintains the state of a particular photo.
     */
    private static class BitmapHolder {
	final byte[] bytes;
	final int originalSmallerExtent;

	Bitmap bitmap;
	int decodedSampleSize;

	public BitmapHolder(byte[] bytes, int originalSmallerExtent) {
	    this.bytes = bytes;
	    this.originalSmallerExtent = originalSmallerExtent;
	}
    }

    /**
     * An LRU cache for bitmap holders. The cache contains bytes for photos just
     * as they come from the database. Each holder has a soft reference to the
     * actual bitmap.
     */
    private final LruCache<Object, BitmapHolder> mBitmapHolderCache;

    /**
     * Cache size threshold at which bitmaps will not be preloaded.
     */
    private final int mBitmapHolderCacheRedZoneBytes;

    /**
     * Level 2 LRU cache for bitmaps. This is a smaller cache that holds the
     * most recently used bitmaps to save time on decoding them from bytes (the
     * bytes are stored in {@link #mBitmapHolderCache}.
     */
    private final LruCache<Object, Bitmap> mBitmapCache;

    /**
     * A map from ImageView to the corresponding photo ID or uri, encapsulated
     * in a request. The request may swapped out before the photo loading
     * request is started.
     */
    private final ConcurrentHashMap<ImageView, Request> mPendingRequests = new ConcurrentHashMap<ImageView, Request>();

    /**
     * Handler for messages sent to the UI thread.
     */
    private final Handler mMainThreadHandler = new Handler(this);

    /**
     * Thread responsible for loading photos from the database. Created upon the
     * first request.
     */
    private LoaderThread mLoaderThread;

    /**
     * A gate to make sure we only send one instance of MESSAGE_PHOTOS_NEEDED at
     * a time.
     */
    private boolean mLoadingRequested;

    /**
     * Flag indicating if the image loading is paused.
     */
    private boolean mPaused;

    /** Cache size for {@link #mBitmapHolderCache} for devices with "large" RAM. */
    private static final int HOLDER_CACHE_SIZE = 4000000;

    /** Cache size for {@link #mBitmapCache} for devices with "large" RAM. */
    private static final int BITMAP_CACHE_SIZE = 73728 * 48; // 3456K

    private static final int SMALL_RAM_THRESHOLD = 256 * 1024 * 1024;

    private static final int LARGE_RAM_THRESHOLD = 640 * 1024 * 1024;

    public ImageManager1() {
	final long totalMemorySize = MemoryUtils.getTotalMemorySize();
	final float cacheSizeAdjustment = (totalMemorySize >= LARGE_RAM_THRESHOLD) ? 1.0f
		: (totalMemorySize > SMALL_RAM_THRESHOLD) ? 0.5f : 0.35f;
	final int bitmapCacheSize = (int) (cacheSizeAdjustment * BITMAP_CACHE_SIZE);
	mBitmapCache = new LruCache<Object, Bitmap>(bitmapCacheSize) {
	    @Override
	    protected int sizeOf(Object key, Bitmap value) {
		return BitmapUtil.getByteCount(value);
	    }

	    @Override
	    protected void entryRemoved(boolean evicted, Object key, Bitmap oldValue, Bitmap newValue) {
		if (DEBUG)
		    dumpStats();
	    }
	};
	final int holderCacheSize = (int) (cacheSizeAdjustment * HOLDER_CACHE_SIZE);
	mBitmapHolderCache = new LruCache<Object, BitmapHolder>(holderCacheSize) {
	    @Override
	    protected int sizeOf(Object key, BitmapHolder value) {
		return value.bytes != null ? value.bytes.length : 0;
	    }

	    @Override
	    protected void entryRemoved(boolean evicted, Object key, BitmapHolder oldValue, BitmapHolder newValue) {
		if (DEBUG)
		    dumpStats();
	    }
	};
	mBitmapHolderCacheRedZoneBytes = (int) (holderCacheSize * 0.75);
	if (DEBUG_ALL) {
	    Log.i(TAG, "Cache adj: " + cacheSizeAdjustment);
	}

	if (DEBUG) {
	    Log.d(TAG, "Cache size: " + btk(mBitmapHolderCache.maxSize()) + " + " + btk(mBitmapCache.maxSize()));
	}
    }

    /** Converts bytes to K bytes, rounding up. Used only for debug log. */
    private static String btk(int bytes) {
	return ((bytes + 1023) / 1024) + "K";
    }

    private static final int safeDiv(int dividend, int divisor) {
	return (divisor == 0) ? 0 : (dividend / divisor);
    }

    /**
     * Dump cache stats on logcat.
     */
    private void dumpStats() {
	if (!DEBUG)
	    return;
	{
	    int numHolders = 0;
	    int rawBytes = 0;
	    int bitmapBytes = 0;
	    int numBitmaps = 0;
	    for (BitmapHolder h : mBitmapHolderCache.snapshot().values()) {
		numHolders++;
		if (h.bytes != null) {
		    rawBytes += h.bytes.length;
		}
		Bitmap b = h.bitmap;
		if (b != null) {
		    numBitmaps++;
		    bitmapBytes += BitmapUtil.getByteCount(b);
		}
	    }
	    Log.d(TAG, "L1: " + btk(rawBytes) + " + " + btk(bitmapBytes) + " = " + btk(rawBytes + bitmapBytes) + ", "
		    + numHolders + " holders, " + numBitmaps + " bitmaps, avg: " + btk(safeDiv(rawBytes, numHolders))
		    + "," + btk(safeDiv(bitmapBytes, numBitmaps)));
	    Log.d(TAG, "L1 Stats: " + mBitmapHolderCache.toString());
	}

	{
	    int numBitmaps = 0;
	    int bitmapBytes = 0;
	    for (Bitmap b : mBitmapCache.snapshot().values()) {
		numBitmaps++;
		bitmapBytes += BitmapUtil.getByteCount(b);
	    }
	    Log.d(TAG,
		    "L2: " + btk(bitmapBytes) + ", " + numBitmaps + " bitmaps" + ", avg: "
			    + btk(safeDiv(bitmapBytes, numBitmaps)));
	    // We don't get from L2 cache, so L2 stats is meaningless.
	}
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
	// Not used yet
    }

    @Override
    public void onLowMemory() {
	Log.e(TAG, "onLowMemory");
	clear();
    }

    @Override
    public void preloadPhotosInBackground() {
	ensureLoaderThread();
	mLoaderThread.requestPreloading();
    }

    @Override
    public void loadImageByUrl(ImageView view, String photoUrl, int requestedExtent,
	    DefaultImageProvider defaultProvider, NewUrlRequest newUrlRequest) {
	if (photoUrl == null) {
	    // No photo is needed
	    defaultProvider.applyDefaultImage(view, requestedExtent);
	    mPendingRequests.remove(view);
	} else {
	    if (DEBUG)
		Log.d(TAG, "loadPhoto request: " + photoUrl);
	    loadPhotoByIdOrUri(view, Request.createCopy(photoUrl, requestedExtent, defaultProvider, newUrlRequest));
	}
    }

    private void loadPhotoByIdOrUri(ImageView view, Request request) {
	boolean loaded = loadCachedPhoto(view, request);
	if (loaded) {
	    mPendingRequests.remove(view);
	} else {
	    mPendingRequests.put(view, request);
	    if (!mPaused) {
		// Send a request to start loading photos
		requestLoading();
	    }
	}
    }

    @Override
    public void removePhoto(ImageView view) {
	view.setImageDrawable(null);
	mPendingRequests.remove(view);
    }

    /**
     * Checks if the photo is present in cache. If so, sets the photo on the
     * view.
     * 
     * @return false if the photo needs to be (re)loaded from the provider.
     */
    private boolean loadCachedPhoto(ImageView view, Request request) {
	BitmapHolder holder = mBitmapHolderCache.get(request.getKey());
	if (holder == null) {
	    // The bitmap has not been loaded ==> show default avatar
	    request.applyDefaultImage(view);
	    return false;
	}

	if (holder.bytes == null) {
	    request.applyDefaultImage(view);
	    return false;
	}

	Bitmap cachedBitmap = holder.bitmap;
	if (cachedBitmap == null) {
	    if (holder.bytes.length < 8 * 1024) {
		// Small thumbnails are usually quick to inflate. Let's do that
		// on the UI thread
		inflateBitmap(holder, request.getRequestedExtent());
		cachedBitmap = holder.bitmap;
		if (cachedBitmap == null)
		    return false;
	    } else {
		// This is bigger data. Let's send that back to the Loader so
		// that we can
		// inflate this in the background
		request.applyDefaultImage(view);
		return false;
	    }
	}

	view.setImageBitmap(cachedBitmap);

	// Put the bitmap in the LRU cache. But only do this for images that are
	// small enough
	// (we require that at least six of those can be cached at the same
	// time)
	if (BitmapUtil.getByteCount(cachedBitmap) < mBitmapCache.maxSize() / 6) {
	    mBitmapCache.put(request.getKey(), cachedBitmap);
	}

	return true;
    }

    /**
     * If necessary, decodes bytes stored in the holder to Bitmap. As long as
     * the bitmap is held either by {@link #mBitmapCache} or by a soft reference
     * in the holder, it will not be necessary to decode the bitmap.
     */
    private static void inflateBitmap(BitmapHolder holder, int requestedExtent) {
	final int sampleSize = BitmapUtil.findOptimalSampleSize(holder.originalSmallerExtent, requestedExtent);
	byte[] bytes = holder.bytes;
	if (bytes == null || bytes.length == 0) {
	    return;
	}

	if (sampleSize == holder.decodedSampleSize) {
	    // Check the soft reference. If will be retained if the bitmap is
	    // also
	    // in the LRU cache, so we don't need to check the LRU cache
	    // explicitly.
	    if (holder.bitmap != null) {
		return;
	    }
	}

	try {
	    Bitmap bitmap = BitmapUtil.decodeBitmapFromBytes(bytes, sampleSize);

	    // make bitmap mutable and draw size onto it
	    if (DEBUG_SIZES) {
		Bitmap original = bitmap;
		bitmap = bitmap.copy(bitmap.getConfig(), true);
		original.recycle();
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setTextSize(16);
		paint.setColor(Color.BLUE);
		paint.setStyle(Style.FILL);
		canvas.drawRect(0.0f, 0.0f, 160.0f, 20.0f, paint);
		paint.setColor(Color.WHITE);
		paint.setAntiAlias(true);
		canvas.drawText(bitmap.getWidth() + "/" + bitmap.getHeight() + " in " + sampleSize + "x sample", 0, 15,
			paint);
	    }

	    holder.decodedSampleSize = sampleSize;
	    holder.bitmap = bitmap;
	    if (DEBUG) {
		Log.d(TAG, "inflateBitmap " + btk(bytes.length) + " -> " + bitmap.getWidth() + "x" + bitmap.getHeight()
			+ ", " + btk(BitmapUtil.getByteCount(bitmap)));
	    }
	} catch (OutOfMemoryError e) {
	    // Do nothing - the photo will appear to be missing
	}
    }

    public void clear() {
	if (DEBUG)
	    Log.d(TAG, "clear");

	mPendingRequests.clear();
	mBitmapHolderCache.evictAll();
	mBitmapCache.evictAll();
    }

    @Override
    public void pause() {
	mPaused = true;
    }

    @Override
    public void resume() {
	mPaused = false;
	if (DEBUG)
	    dumpStats();
	if (!mPendingRequests.isEmpty()) {
	    requestLoading();
	}
    }

    /**
     * Sends a message to this thread itself to start loading images. If the
     * current view contains multiple image views, all of those image views will
     * get a chance to request their respective photos before any of those
     * requests are executed. This allows us to load images in bulk.
     */
    private void requestLoading() {
	if (!mLoadingRequested) {
	    mLoadingRequested = true;
	    mMainThreadHandler.sendEmptyMessage(MESSAGE_REQUEST_LOADING);
	}
    }

    /**
     * Processes requests on the main thread.
     */
    @Override
    public boolean handleMessage(Message msg) {
	switch (msg.what) {
	case MESSAGE_REQUEST_LOADING: {
	    mLoadingRequested = false;
	    if (!mPaused) {
		ensureLoaderThread();
		mLoaderThread.requestLoading();
	    }
	    return true;
	}

	case MESSAGE_PHOTOS_LOADED: {
	    if (!mPaused) {
		processLoadedImages();
	    }
	    if (DEBUG)
		dumpStats();
	    return true;
	}
	}
	return false;
    }

    public void ensureLoaderThread() {
	if (mLoaderThread == null) {
	    mLoaderThread = new LoaderThread();
	    mLoaderThread.start();
	}
    }

    /**
     * Goes over pending loading requests and displays loaded photos. If some of
     * the photos still haven't been loaded, sends another request for image
     * loading.
     */
    private void processLoadedImages() {
	Iterator<ImageView> iterator = mPendingRequests.keySet().iterator();
	while (iterator.hasNext()) {
	    ImageView view = iterator.next();
	    Request key = mPendingRequests.get(view);
	    boolean loaded = loadCachedPhoto(view, key);
	    if (loaded) {
		iterator.remove();
	    }
	}

	if (!mPendingRequests.isEmpty()) {
	    requestLoading();
	}
    }

    /**
     * Stores the supplied bitmap in cache.
     */
    private void cacheBitmap(Object key, byte[] bytes, boolean preloading, int requestedExtent) {
	if (DEBUG) {
	    BitmapHolder prev = mBitmapHolderCache.get(key);
	    Log.d(TAG, "Caching data: key=" + key + ", " + (bytes == null ? "<null>" : btk(bytes.length)));
	}
	BitmapHolder holder = new BitmapHolder(bytes, bytes == null ? -1 : BitmapUtil.getSmallerExtentFromBytes(bytes));

	// Unless this image is being preloaded, decode it right away while
	// we are still on the background thread.
	if (!preloading) {
	    inflateBitmap(holder, requestedExtent);
	}

	mBitmapHolderCache.put(key, holder);
    }

    @Override
    public void cacheBitmap(Request originalRequest, Bitmap bitmap, byte[] photoBytes) {
	final int smallerExtent = Math.min(bitmap.getWidth(), bitmap.getHeight());
	// We can pretend here that the extent of the photo was the size that we
	// originally
	// requested
	Request request = Request.createCopy(originalRequest.getUrl(), smallerExtent,
		originalRequest.getDefaultProvider(), originalRequest.getNewUrlRequest());
	BitmapHolder holder = new BitmapHolder(photoBytes, smallerExtent);
	holder.bitmap = bitmap;
	mBitmapHolderCache.put(request.getKey(), holder);
	mBitmapCache.put(request.getKey(), bitmap);
    }

    /**
     * Populates an array of photo IDs that need to be loaded. Also decodes
     * bitmaps that we have already loaded
     */
    private void obtainPhotoIdsAndUrisToLoad(Set<String> photoIds, Set<Request> uris) {
	photoIds.clear();
	uris.clear();

	boolean jpegsDecoded = false;

	/*
	 * Since the call is made from the loader thread, the map could be
	 * changing during the iteration. That's not really a problem:
	 * ConcurrentHashMap will allow those changes to happen without throwing
	 * exceptions. Since we may miss some requests in the situation of
	 * concurrent change, we will need to check the map again once loading
	 * is complete.
	 */
	Iterator<Request> iterator = mPendingRequests.values().iterator();
	while (iterator.hasNext()) {
	    Request request = iterator.next();
	    final BitmapHolder holder = mBitmapHolderCache.get(request.getKey());
	    if (holder != null && holder.bytes != null && holder.bitmap == null) {
		// This was previously loaded but we don't currently have the
		// inflated Bitmap
		inflateBitmap(holder, request.getRequestedExtent());
		jpegsDecoded = true;
	    } else {
		if (holder == null) {
		    if (request.isUrlRequest()) {
			uris.add(request);
		    } else {
			photoIds.add(request.getId());
		    }
		}
	    }
	}

	if (jpegsDecoded)
	    mMainThreadHandler.sendEmptyMessage(MESSAGE_PHOTOS_LOADED);
    }

    /**
     * The thread that performs loading of photos from the database.
     */
    private class LoaderThread extends HandlerThread implements Callback {
	private static final int BUFFER_SIZE = 32768;
	private static final int MESSAGE_PRELOAD_PHOTOS = 0;
	private static final int MESSAGE_LOAD_PHOTOS = 1;

	/**
	 * A pause between preload batches that yields to the UI thread.
	 */
	private static final int PHOTO_PRELOAD_DELAY = 1000;

	/**
	 * Number of photos to preload per batch.
	 */
	private static final int PRELOAD_BATCH = 25;

	/**
	 * Maximum number of photos to preload. If the cache size is 2Mb and the
	 * expected average size of a photo is 4kb, then this number should be
	 * 2Mb/4kb = 500.
	 */
	private static final int MAX_PHOTOS_TO_PRELOAD = 100;

	private final Set<String> mImageIds = new HashSet<String>();
	private final Set<Request> mImageUrls = new HashSet<Request>();
	private final List<String> mPreloadImageIds = new ArrayList<String>();

	private Handler mLoaderThreadHandler;
	private byte mBuffer[];

	private static final int PRELOAD_STATUS_NOT_STARTED = 0;
	private static final int PRELOAD_STATUS_IN_PROGRESS = 1;
	private static final int PRELOAD_STATUS_DONE = 2;

	private int mPreloadStatus = PRELOAD_STATUS_NOT_STARTED;

	public LoaderThread() {
	    super(LOADER_THREAD_NAME);
	}

	public void ensureHandler() {
	    if (mLoaderThreadHandler == null) {
		mLoaderThreadHandler = new Handler(getLooper(), this);
	    }
	}

	/**
	 * Kicks off preloading of the next batch of photos on the background
	 * thread. Preloading will happen after a delay: we want to yield to the
	 * UI thread as much as possible.
	 * <p>
	 * If preloading is already complete, does nothing.
	 */
	public void requestPreloading() {
	    if (mPreloadStatus == PRELOAD_STATUS_DONE) {
		return;
	    }

	    ensureHandler();
	    if (mLoaderThreadHandler.hasMessages(MESSAGE_LOAD_PHOTOS)) {
		return;
	    }

	    mLoaderThreadHandler.sendEmptyMessageDelayed(MESSAGE_PRELOAD_PHOTOS, PHOTO_PRELOAD_DELAY);
	}

	/**
	 * Sends a message to this thread to load requested photos. Cancels a
	 * preloading request, if any: we don't want preloading to impede
	 * loading of the photos we need to display now.
	 */
	public void requestLoading() {
	    ensureHandler();
	    mLoaderThreadHandler.removeMessages(MESSAGE_PRELOAD_PHOTOS);
	    mLoaderThreadHandler.sendEmptyMessage(MESSAGE_LOAD_PHOTOS);
	}

	/**
	 * Receives the above message, loads photos and then sends a message to
	 * the main thread to process them.
	 */
	@Override
	public boolean handleMessage(Message msg) {
	    switch (msg.what) {
	    case MESSAGE_PRELOAD_PHOTOS:
		preloadPhotosInBackground();
		break;
	    case MESSAGE_LOAD_PHOTOS:
		loadPhotosInBackground();
		break;
	    }
	    return true;
	}

	/**
	 * The first time it is called, figures out which photos need to be
	 * preloaded. Each subsequent call preloads the next batch of photos and
	 * requests another cycle of preloading after a delay. The whole process
	 * ends when we either run out of photos to preload or fill up cache.
	 */
	private void preloadPhotosInBackground() {
	    if (mPreloadStatus == PRELOAD_STATUS_DONE) {
		return;
	    }

	    if (mPreloadStatus == PRELOAD_STATUS_NOT_STARTED) {
		queryPhotosForPreload();
		if (mPreloadImageIds.isEmpty()) {
		    mPreloadStatus = PRELOAD_STATUS_DONE;
		} else {
		    mPreloadStatus = PRELOAD_STATUS_IN_PROGRESS;
		}
		requestPreloading();
		return;
	    }

	    if (mBitmapHolderCache.size() > mBitmapHolderCacheRedZoneBytes) {
		mPreloadStatus = PRELOAD_STATUS_DONE;
		return;
	    }

	    mImageIds.clear();

	    int preloadSize = mPreloadImageIds.size();
	    while (preloadSize > 0 && mImageIds.size() < PRELOAD_BATCH) {
		preloadSize--;
		String photoId = mPreloadImageIds.get(preloadSize);
		mImageIds.add(photoId);
		mPreloadImageIds.remove(preloadSize);
	    }

	    loadFromDisk(true);

	    if (preloadSize == 0) {
		mPreloadStatus = PRELOAD_STATUS_DONE;
	    }

	    requestPreloading();
	}

	private void queryPhotosForPreload() {
	    final Context context = StaticContextApplication.getAppContext();
	    String[] fileList = context.fileList();
	    if (fileList != null) {
		for (String filename : fileList) {
		    if (filename == null || !filename.endsWith("_img")) {
			continue;
		    }

		    mPreloadImageIds.add(0, filename.substring(0, filename.length() - 4));

		    if (mPreloadImageIds.size() == MAX_PHOTOS_TO_PRELOAD) {
			break;
		    }
		}
	    }
	}

	private void loadPhotosInBackground() {
	    obtainPhotoIdsAndUrisToLoad(mImageIds, mImageUrls);
	    loadFromDisk(false);
	    loadFromNetwork();
	    requestPreloading();
	}

	/** Loads thumbnail photos with ids */
	private void loadFromDisk(boolean preloading) {
	    if (mImageIds.isEmpty()) {
		return;
	    }

	    // Remove loaded photos from the preload queue: we don't want
	    // the preloading process to load them again.
	    if (!preloading && mPreloadStatus == PRELOAD_STATUS_IN_PROGRESS) {
		for (String id : mImageIds) {
		    mPreloadImageIds.remove(id);
		}
		if (mPreloadImageIds.isEmpty()) {
		    mPreloadStatus = PRELOAD_STATUS_DONE;
		}
	    }

	    if (mBuffer == null) {
		mBuffer = new byte[BUFFER_SIZE];
	    }

	    final Context context = StaticContextApplication.getAppContext();
	    String[] fileList = context.fileList();
	    if (fileList != null) {
		for (String filename : fileList) {
		    if (filename == null || !filename.endsWith("_img")) {
			continue;
		    }

		    String id = filename.substring(0, filename.length() - 4);

		    try {
			InputStream is = context.openFileInput(filename);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			if (is != null) {
			    try {
				int size;
				while ((size = is.read(mBuffer)) != -1) {
				    baos.write(mBuffer, 0, size);
				}

				cacheBitmap(id, baos.toByteArray(), preloading, -1);
				mImageIds.remove(id);
			    } catch (IOException e) {
				e.printStackTrace();
				cacheBitmap(id, null, preloading, -1);
			    } finally {
				if (is != null) {
				    try {
					is.close();
				    } catch (IOException e) {
					e.printStackTrace();
				    }
				}

				if (baos != null) {
				    try {
					baos.close();
				    } catch (IOException e) {
					e.printStackTrace();
				    }
				}
			    }
			}
		    } catch (FileNotFoundException e) {
			e.printStackTrace();
			cacheBitmap(id, null, preloading, -1);
		    } catch (OutOfMemoryError e) {
			e.printStackTrace();
			cacheBitmap(id, null, preloading, -1);
		    }
		}
	    }

	    mMainThreadHandler.sendEmptyMessage(MESSAGE_PHOTOS_LOADED);
	}

	/**
	 * Loads photos referenced with Uris. Those can be remote thumbnails
	 * (from directory searches), display photos etc
	 */
	private void loadFromNetwork() {
	    if (mBuffer == null) {
		mBuffer = new byte[BUFFER_SIZE];
	    }

	    for (Request uriRequest : mImageUrls) {
		String url = uriRequest.getUrl();

		try {
		    HttpResponse response = null;
		    while (true) {
			final HttpClient client = new DefaultHttpClient();
			response = client.execute(new HttpGet(url));
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == HttpStatus.SC_OK) {
			    uriRequest.changeUrlTo(url);
			    break;
			} else {
			    url = uriRequest.requestNewUrl(url);
			    if (TextUtils.isEmpty(url)) {
				url = uriRequest.getUrl();
				throw new java.lang.UnsatisfiedLinkError("Error " + statusCode
					+ " while retrieving bitmap from " + url);
			    }
			}
		    }

		    final HttpEntity entity = response.getEntity();
		    if (entity != null) {
			FlushedInputStream inputStream = null;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			try {
			    InputStream contentStream = entity.getContent();
			    if (contentStream != null) {
				inputStream = new FlushedInputStream(contentStream);

				int size;
				while ((size = inputStream.read(mBuffer)) != -1) {
				    baos.write(mBuffer, 0, size);
				}
			    } else {
				Log.e(TAG, "Cannot load photo " + url);
				cacheBitmap(url, null, false, uriRequest.getRequestedExtent());
			    }
			} finally {
			    if (inputStream != null) {
				inputStream.close();
			    }
			    entity.consumeContent();
			}

			cacheBitmap(url, baos.toByteArray(), false, uriRequest.getRequestedExtent());

			mMainThreadHandler.sendEmptyMessage(MESSAGE_PHOTOS_LOADED);
		    }
		} catch (Exception ex) {
		    Log.e(TAG, "Cannot load photo " + url, ex);
		    cacheBitmap(url, null, false, uriRequest.getRequestedExtent());
		} catch (Error e) {
		    Log.e(TAG, "Cannot load photo " + url, e);
		    cacheBitmap(url, null, false, uriRequest.getRequestedExtent());
		}
	    }
	}
    }
}
