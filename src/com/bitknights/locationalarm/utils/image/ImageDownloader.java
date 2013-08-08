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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.bitknights.locationalarm.StaticContextApplication;
import com.bitknights.locationalarm.utils.Utils;

/**
 * This helper class download images from the Internet and binds those with the
 * provided ImageView.
 * <p>
 * It requires the INTERNET permission, which should be added to your
 * application's manifest file.
 * </p>
 * A local cache of downloaded images is maintained internally to improve
 * performance.
 */
public class ImageDownloader extends ImageManager {

    public static final String IMAGE_SERVICE = "ImageDownloader";

    /**
     * The actual AsyncTask that will asynchronously download the image.
     */
    class BitmapDownloaderTask extends AsyncTask<String[], Void, BitmapDrawable> {
        private String url;
        private String filename;
        private final WeakReference<ImageView> imageViewReference;

        public BitmapDownloaderTask(ImageView imageView) {
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        /**
         * Actual download method.
         */
        @Override
        protected BitmapDrawable doInBackground(String[]... params) {
            final Context context = StaticContextApplication.getAppContext();

            String[] data = params[0];
            Bitmap bmp = null;

            try {
                if (data.length > 0) {
                    url = data[0];
                }

                if (data.length > 1) {
                    filename = data[1];
                }

                if (isCancelled()) {
                    return null;
                }

                bmp = loadFromDisk(url);

                if (bmp != null) {
                    bmp.setDensity(0);
                    return new BitmapDrawable(context.getResources(), bmp);
                }

                if (isCancelled()) {
                    return new BitmapDrawable(context.getResources(), bmp);
                }

                bmp = downloadBitmap(url);

                if (bmp != null) {
                    bmp.setDensity(0);

                    if (filename != null) {
                        ImageUtils.cacheImage(bmp, filename);
                    }

                    return new BitmapDrawable(context.getResources(), bmp);
                } else {
                    if (isCancelled()) {
                        return new BitmapDrawable(context.getResources(), bmp);
                    }

                    if (Utils.isOnline()) {
                        return new BitmapDrawable(context.getResources(),
                                BitmapFactory.decodeResource(
                                        context.getResources(), stubId, opt));
                    }

                    if (filename == null) {
                        filename = String.valueOf(url.hashCode());
                    }

                    bmp = loadFromCache(filename);

                    if (bmp != null) {
                        bmp.setDensity(0);
                    }

                    return new BitmapDrawable(context.getResources(), bmp);
                }
            } catch (Error e) {
                synchronized (sHardBitmapCache) {
                    Set<String> keys = sHardBitmapCache.keySet();
                    for (String key : keys) {
                        CacheItem item = sHardBitmapCache.get(key);
                        Log.e("", item != null ? item.toString() : "NULL ITEM?!");
                    }
                    Log.e(Utils.TAG + "ImageDownloader", "Can not fetch the image", e);
                }

                return null;
            }
        }

        /**
         * Once the image is downloaded, associates it to the imageView
         */
        @Override
        protected void onPostExecute(BitmapDrawable bitmap) {
            if (isCancelled() || bitmap == null) {
                if (bitmap != null && bitmap.getBitmap() != null) {
                    bitmap.getBitmap().recycle();
                }

                bitmap = null;
                return;
            }

            addBitmapToCache(url, bitmap);

            if (imageViewReference != null) {
                ImageView imageView = imageViewReference.get();
                BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
                // Change bitmap only if this process is still associated with
                // it
                // Or if we don't use any bitmap to task association
                // (NO_DOWNLOADED_DRAWABLE mode)
                if ((this == bitmapDownloaderTask) /* || (mode != Mode.CORRECT) */) {
                    setImageIntoView(imageView, bitmap);
                }
            }
        }
    }

    /**
     * A fake Drawable that will be attached to the imageView while the download
     * is in progress.
     * <p>
     * Contains a reference to the actual download task, so that a download task
     * can be stopped if a new binding is required, and makes sure that only the
     * last started download process can bind its result, independently of the
     * download finish order.
     * </p>
     */
    static class DownloadedDrawable extends LayerDrawable {
        private final WeakReference<BitmapDownloaderTask> bitmapDownloaderTaskReference;

        public DownloadedDrawable(Context context, BitmapDownloaderTask bitmapDownloaderTask,
                int stubId) {
            super(new Drawable[] {
                stubId == 0 ? new ColorDrawable(Color.TRANSPARENT) : context.getResources()
                        .getDrawable(stubId)
            });
            bitmapDownloaderTaskReference = new WeakReference<BitmapDownloaderTask>(
                    bitmapDownloaderTask);
        }

        public BitmapDownloaderTask getBitmapDownloaderTask() {
            return bitmapDownloaderTaskReference.get();
        }
    }

    private static class MutableInt {
        private int value;

        public MutableInt(int defValue) {
            value = defValue;
        }

        public int getValue() {
            return value;
        }

        public void increment() {
            this.value += 1;
        }

        public void decrement() {
            this.value -= 1;
        }
    }

    private static class CacheItem {
        private MutableInt refCount;
        private Drawable drawable;

        public CacheItem(Drawable drawable) {
            this.drawable = drawable;
            this.refCount = new MutableInt(1);
        }

        @Override
        public String toString() {
            if (drawable != null && ((BitmapDrawable) drawable).getBitmap() != null) {
                return "Size: " + ((BitmapDrawable) drawable).getBitmap().getRowBytes()
                        * ((BitmapDrawable) drawable).getBitmap().getHeight()
                        + " bytes; RefCount: "
                        + refCount.getValue() + " [" + super.toString() + "]";
            } else {
                return "Size: N/A (deleted?) bytes; RefCount: " + refCount.getValue() + " ["
                        + super.toString() + "]";
            }
        }
    }

    /*
     * Cache-related fields and methods. We use a hard and a soft cache. A soft
     * reference cache is too aggressively cleared by the Garbage Collector.
     */

    private static final int HARD_CACHE_CAPACITY = 10;
    private static final int DELAY_BEFORE_PURGE = 10 * 1000; // in milliseconds

    // Hard cache, with a fixed maximum capacity and a life duration
    private final static HashMap<String, CacheItem> sHardBitmapCache = new LinkedHashMap<String, CacheItem>(
            HARD_CACHE_CAPACITY / 2, 0.75f, true) {
        /**
			 * 
			 */
        private static final long serialVersionUID = 2648058302564915861L;

        @Override
        protected boolean removeEldestEntry(LinkedHashMap.Entry<String, CacheItem> eldest) {
            if (size() > HARD_CACHE_CAPACITY) {
                // Entries push-out of hard reference cache are transferred to
                // soft reference cache
                sSoftBitmapCache.put(eldest.getKey(),
                        new SoftReference<CacheItem>(eldest.getValue()));
                return true;
            } else
                return false;
        }
    };

    // Soft cache for bitmaps kicked out of hard cache
    private final static ConcurrentHashMap<String, SoftReference<CacheItem>> sSoftBitmapCache = new ConcurrentHashMap<String, SoftReference<CacheItem>>(
            HARD_CACHE_CAPACITY / 2);

    private final Handler purgeHandler = new Handler();

    private final Runnable purger = new Runnable() {
        public void run() {
            clearCache();
        }
    };

    private ArrayList<String> keys;

    private BitmapFactory.Options opt;

    private int stubId;

    private static ImageDownloader mImageDownloader;

    public static ImageDownloader getInstance() {
        if (mImageDownloader == null) {
            mImageDownloader = new ImageDownloader();
        }

        return mImageDownloader;
    }

    public ImageDownloader() {
        keys = new ArrayList<String>();

        opt = new BitmapFactory.Options();
        opt.inDither = false;
        // opt.inPurgeable = true;
        opt.inPreferredConfig = Config.ARGB_8888;

        this.stubId = 0;
    }

    private void matrixAction(ImageView imageView, Drawable drawable) {
        final Context context = StaticContextApplication.getAppContext();
        if (imageView.getScaleType() == ScaleType.MATRIX) {
            float scale = context.getResources().getDisplayMetrics().widthPixels
                    / (float) drawable.getMinimumWidth();

            Matrix m = new Matrix();
            m.setScale(scale, scale);

            LayoutParams lp = imageView.getLayoutParams();

            lp.width = (int) (drawable.getMinimumWidth() * scale);
            lp.height = (int) (drawable.getMinimumHeight() * scale);

            imageView.setImageMatrix(m);
            imageView.setLayoutParams(lp);
        }
    }

    public void setImageIntoView(ImageView imageView, Drawable drawable) {
        matrixAction(imageView, drawable);
        imageView.setImageDrawable(drawable);
    }

    /**
     * Download the specified image from the Internet and binds it to the
     * provided ImageView. The binding is immediate if the image is found in the
     * cache and will be done asynchronously otherwise. A null bitmap will be
     * associated to the ImageView if an error occurs.
     * 
     * @param url The URL of the image to download.
     * @param imageView The ImageView to bind the downloaded image to.
     */
    public void download(String url, ImageView imageView) {
        imageView.setImageResource(stubId);

        if (TextUtils.isEmpty(url)) {
            return;
        }

        String fixedUrl = url.replace("\\/", "/");

        resetPurgeTimer();
        BitmapDrawable drawable = getBitmapFromCache(fixedUrl);

        if (drawable == null || drawable.getBitmap() == null) {
            forceDownload(fixedUrl, imageView);
        } else {
            cancelPotentialDownload(fixedUrl, imageView);
            setImageIntoView(imageView, drawable);
        }
    }

    /**
     * Download the specified image from the Internet and binds it to the
     * provided ImageView. The binding is immediate if the image is found in the
     * cache and will be done asynchronously otherwise. A null bitmap will be
     * associated to the ImageView if an error occurs.
     * 
     * @param url The URL of the image to download.
     * @param filename The file name of the stored image on the disk.
     * @param imageView The ImageView to bind the downloaded image to.
     */
    public void download(String url, String filename, ImageView imageView) {
        imageView.setImageResource(stubId);

        String fixedUrl = url.replace("\\/", "/");

        resetPurgeTimer();
        BitmapDrawable drawable = getBitmapFromCache(fixedUrl);

        if (drawable == null || drawable.getBitmap() == null) {
            forceDownload(fixedUrl, filename, imageView);
        } else {
            cancelPotentialDownload(fixedUrl, imageView);
            setImageIntoView(imageView, drawable);
        }
    }

    /*
     * Same as download but the image is always downloaded and the cache is not
     * used. Kept private at the moment as its interest is not clear. private
     * void forceDownload(String url, ImageView view) { forceDownload(url, view,
     * null); }
     */

    /**
     * Same as download but the image is always downloaded and the cache is not
     * used. Kept private at the moment as its interest is not clear.
     */
    private void forceDownload(String url, ImageView imageView) {
        final Context context = StaticContextApplication.getAppContext();

        // State sanity: url is guaranteed to never be null in
        // DownloadedDrawable and cache keys.
        if (url == null) {
            imageView.setImageDrawable(null);
            return;
        }

        // if (cancelPotentialDownload(url, imageView)) {
        cancelPotentialDownload(url, imageView);

        BitmapDownloaderTask task = new BitmapDownloaderTask(imageView);
        DownloadedDrawable downloadedDrawable = new DownloadedDrawable(context, task, stubId);

        setImageIntoView(imageView, downloadedDrawable);
        imageView.setMinimumHeight(156);
        task.execute(new String[] {
            url
        });
        // }
    }

    /**
     * Same as download but the image is always downloaded and the cache is not
     * used. Kept private at the moment as its interest is not clear.
     */
    private void forceDownload(String url, String filename, ImageView imageView) {
        final Context context = StaticContextApplication.getAppContext();

        // State sanity: url is guaranteed to never be null in
        // DownloadedDrawable and cache keys.
        if (url == null) {
            imageView.setImageDrawable(null);
            return;
        }

        // if (cancelPotentialDownload(url, imageView)) {
        cancelPotentialDownload(url, imageView);

        BitmapDownloaderTask task = new BitmapDownloaderTask(imageView);
        DownloadedDrawable downloadedDrawable = new DownloadedDrawable(context, task, stubId);
        setImageIntoView(imageView, downloadedDrawable);
        imageView.setMinimumHeight(156);
        task.execute(new String[] {
                url, filename
        });
        // }
    }

    /**
     * Returns true if the current download has been canceled or if there was no
     * download in progress on this image view. Returns false if the download in
     * progress deals with the same url. The download is not stopped in that
     * case.
     */
    public static boolean cancelPotentialDownload(String url, ImageView imageView) {
        BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);

        if (bitmapDownloaderTask != null) {
            String bitmapUrl = bitmapDownloaderTask.url;
            if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
                bitmapDownloaderTask.cancel(true);
            } else {
                // Whether the same URL is already being downloaded to the same
                // imageview
                return bitmapDownloaderTask.imageViewReference != null
                        && bitmapDownloaderTask.imageViewReference.get() != imageView;
            }
        }

        return true;
    }

    /**
     * @param imageView Any imageView
     * @return Retrieve the currently active download task (if any) associated
     *         with this imageView. null if there is no such task.
     */
    private static BitmapDownloaderTask getBitmapDownloaderTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedDrawable) {
                DownloadedDrawable downloadedDrawable = (DownloadedDrawable) drawable;
                return downloadedDrawable.getBitmapDownloaderTask();
            }
        }
        return null;
    }

    private Bitmap downloadBitmap(String url) {
        final HttpClient client = new DefaultHttpClient();
        final HttpGet getRequest = new HttpGet(url);

        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new java.lang.UnsatisfiedLinkError("Error " + statusCode
                        + " while retrieving bitmap from " + url);
            }

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                Bitmap b = null;
                try {
                    inputStream = entity.getContent();
                    // return BitmapFactory.decodeStream(inputStream);
                    // Bug on slow connections, fixed in future release.
                    b = BitmapFactory.decodeStream(new FlushedInputStream(inputStream), null, opt);
                } catch (OutOfMemoryError e) {
                    synchronized (sHardBitmapCache) {
                        Set<String> keys = sHardBitmapCache.keySet();
                        for (String key : keys) {
                            CacheItem item = sHardBitmapCache.get(key);
                            Log.e("", item != null ? item.toString() : "NULL ITEM?!");
                        }
                        Log.e(Utils.TAG + getClass().getSimpleName(),
                                "Can not create a new bitmap", e);
                    }
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    entity.consumeContent();
                }

                if (b == null) {
                    return null;
                }
                return saveToDisc(b, url);
            }
        } catch (IOException e) {
            getRequest.abort();
            Log.w(Utils.TAG + getClass().getSimpleName(), "I/O error while retrieving bitmap from "
                    + url, e);
        } catch (IllegalStateException e) {
            getRequest.abort();
            Log.w(Utils.TAG + getClass().getSimpleName(), "Incorrect URL: " + url);
        } catch (Exception e) {
            getRequest.abort();
            Log.w(Utils.TAG + getClass().getSimpleName(), "Error while retrieving bitmap from "
                    + url, e);
        } catch (Error e) {
            getRequest.abort();
            Log.w(Utils.TAG + getClass().getSimpleName(), "Error while retrieving bitmap from "
                    + url, e);
        }

        return null;
    }

    public Bitmap saveToDisc(Bitmap b, String url) throws Exception {
        final Context context = StaticContextApplication.getAppContext();

        String filename = getFileNameFromUrl(url);

        if (opt.outHeight < context.getResources().getDisplayMetrics().heightPixels
                && opt.outWidth < context.getResources().getDisplayMetrics().widthPixels) {
            b.compress(CompressFormat.PNG, 100,
                    context.openFileOutput(filename, Context.MODE_PRIVATE));
            return b;
        } else if (opt.outWidth < opt.outHeight) {
            Bitmap bmp = ImageUtils
                    .getResizedBitmap(
                            b,
                            opt.outHeight > context.getResources().getDisplayMetrics().heightPixels ? context
                                    .getResources()
                                    .getDisplayMetrics().heightPixels : opt.outHeight);

            if (b != null && bmp != b) {
                b.recycle();
                b = null;
            }

            if (bmp != null) {
                bmp.compress(CompressFormat.PNG, 100,
                        context.openFileOutput(filename, Context.MODE_PRIVATE));
                return bmp;
            }
        } else {
            Bitmap bmp = ImageUtils.getResizedBitmap(b,
                    opt.outWidth > context.getResources().getDisplayMetrics().widthPixels ? context
                            .getResources()
                            .getDisplayMetrics().widthPixels : opt.outWidth);

            if (b != null && bmp != b) {
                b.recycle();
                b = null;
            }

            if (bmp != null) {
                bmp.compress(CompressFormat.PNG, 100,
                        context.openFileOutput(filename, Context.MODE_PRIVATE));
                return bmp;
            }
        }
        return null;
    }

    /**
     * Adds this bitmap to the cache.
     * 
     * @param bitmap The newly downloaded bitmap.
     */
    private void addBitmapToCache(String url, BitmapDrawable bitmap) {
        if (bitmap != null) {
            synchronized (sHardBitmapCache) {
                if (sHardBitmapCache.containsKey(url)) {
                    final CacheItem item = sHardBitmapCache.get(url);

                    if (item != null && item.drawable != null) {
                        BitmapDrawable drawable = (BitmapDrawable) item.drawable;
                        if (drawable != null && drawable.getBitmap() != null) {
                            // Bitmap found in hard cache
                            // Move element to first position, so that it is
                            // removed last
                            sHardBitmapCache.remove(url);

                            item.refCount.increment();

                            sHardBitmapCache.put(url, item);
                        }
                    }
                } else {
                    keys.add(url);
                    sHardBitmapCache.put(url, new CacheItem(bitmap));
                }
            }
        }
    }

    /**
     * @param url The URL of the image that will be retrieved from the cache.
     * @return The cached bitmap or null if it was not found.
     */
    public BitmapDrawable getBitmapFromCache(String url) {
        if (url == null) {
            return null;
        }

        // First try the hard reference cache
        synchronized (sHardBitmapCache) {
            final CacheItem item = sHardBitmapCache.get(url);

            if (item != null && item.drawable != null) {
                BitmapDrawable drawable = (BitmapDrawable) item.drawable;
                if (drawable != null && drawable.getBitmap() != null) {
                    // Bitmap found in hard cache
                    // Move element to first position, so that it is removed
                    // last
                    sHardBitmapCache.remove(url);

                    item.refCount.increment();

                    sHardBitmapCache.put(url, item);

                    return drawable;
                }
            }
        }

        // Then try the soft reference cache
        SoftReference<CacheItem> bitmapReference = sSoftBitmapCache.get(url);
        if (bitmapReference != null) {
            CacheItem item = bitmapReference.get();
            if (item != null && item.drawable != null) {
                BitmapDrawable drawable = (BitmapDrawable) item.drawable;
                if (drawable != null && drawable.getBitmap() != null) {
                    final Bitmap bitmap = drawable.getBitmap();
                    if (bitmap != null) {
                        // Bitmap found in soft cache, put it back to the hard
                        // cache
                        sSoftBitmapCache.remove(url);

                        item.refCount.increment();

                        synchronized (sHardBitmapCache) {
                            sHardBitmapCache.put(url, item);
                        }

                        return drawable;
                    } else {
                        // Soft reference has been Garbage Collected
                        sSoftBitmapCache.remove(url);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Clears the image cache used internally to improve performance. Note that
     * for memory efficiency reasons, the cache will automatically be cleared
     * after a certain inactivity delay.
     */
    public void clearCache() {
        // sHardBitmapCache.clear();
        sSoftBitmapCache.clear();
    }

    public void clearVirtualCache() {
        while (keys.size() > 0) {
            removeFromCache(keys.remove(0));
        }

        System.gc();
    }

    public void removeFromCache(String key) {
        /*
         * try { // check why should we unload all drawable from the memory
         * throw new RuntimeException(); } catch (RuntimeException ex) {
         * Log.e("", "", ex); ex.printStackTrace(); }
         */

        if (key == null) {
            return;
        }

        CacheItem item = null;

        synchronized (sHardBitmapCache) {
            item = sHardBitmapCache.get(key);

            if (item != null) {
                // Log.e("", key + " getted (hard refCount: " +
                // item.refCount.getValue() + ")");
                item.refCount.decrement();
                if (item.refCount.getValue() < 1) {
                    if (item.drawable instanceof BitmapDrawable) {
                        ((BitmapDrawable) item.drawable).setCallback(null);

                        if (((BitmapDrawable) item.drawable).getBitmap() != null) {
                            ((BitmapDrawable) item.drawable).getBitmap().recycle();
                        }
                    }

                    sHardBitmapCache.remove(key);
                    // Log.e("", key + " removed (hard refCount: " +
                    // item.refCount.getValue() + ")");

                    item.drawable = null;
                } else {
                    // Log.e("", key + " decreased (hard refCount: " +
                    // item.refCount.getValue() + ")");
                }
            }
        }

        if (item == null) {
            synchronized (sSoftBitmapCache) {
                SoftReference<CacheItem> ref = sSoftBitmapCache.contains(key) ? sSoftBitmapCache
                        .get(key) : null;

                if (ref == null) {
                    return;
                }

                item = ref.get();

                if (item != null) {
                    // Log.e("", key + " getted (soft refCount: " +
                    // item.refCount.getValue() + ")");
                    item.refCount.decrement();
                    if (item.refCount.getValue() < 1) {
                        if (item.drawable instanceof BitmapDrawable) {
                            ((BitmapDrawable) item.drawable).setCallback(null);

                            if (((BitmapDrawable) item.drawable).getBitmap() != null) {
                                ((BitmapDrawable) item.drawable).getBitmap().recycle();
                            }
                        }

                        sSoftBitmapCache.remove(key);
                        // Log.e("", key + " removed (soft refCount: " +
                        // item.refCount.getValue() + ")");

                        item.drawable = null;
                    } else {
                        // Log.e("", key + " decreased (soft refCount: " +
                        // item.refCount.getValue() + ")");
                    }
                }
            }
        }

        System.gc();
    }

    public static void clearCache(Context context) {
        File dir = context.getFilesDir();

        if (dir != null && dir.exists()) {
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
     * Allow a new delay before the automatic cache clear is done.
     */
    private void resetPurgeTimer() {
        purgeHandler.removeCallbacks(purger);
        purgeHandler.postDelayed(purger, DELAY_BEFORE_PURGE);
    }

    public String getFileNameFromUrl(String url) {
        return String.valueOf(url.hashCode()) + "_img";
    }

    public Bitmap loadFromDisk(String url) {
        final Context context = StaticContextApplication.getAppContext();

        String filename = getFileNameFromUrl(url);

        Bitmap b = null;

        try {
            InputStream is = context.openFileInput(filename);
            b = BitmapFactory.decodeStream(is, null, opt);

            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (b == null) {
                return null;
            }

            if (opt.outHeight < context.getResources().getDisplayMetrics().heightPixels
                    && opt.outWidth < context.getResources().getDisplayMetrics().widthPixels) {
                return b;
            } else if (opt.outWidth < opt.outHeight) {
                Bitmap bmp = ImageUtils
                        .getResizedBitmap(
                                b,
                                opt.outHeight > context.getResources().getDisplayMetrics().heightPixels ? context
                                        .getResources().getDisplayMetrics().heightPixels
                                        : opt.outHeight);

                if (b != null && bmp != b) {
                    b.recycle();
                    b = null;
                }

                if (bmp != null) {
                    return bmp;
                }
            } else {
                Bitmap bmp = ImageUtils
                        .getResizedBitmap(
                                b,
                                opt.outWidth > context.getResources().getDisplayMetrics().widthPixels ? context
                                        .getResources()
                                        .getDisplayMetrics().widthPixels : opt.outWidth);

                if (b != null && bmp != b) {
                    b.recycle();
                    b = null;
                }

                if (bmp != null) {
                    return bmp;
                }
            }
        } catch (FileNotFoundException e) {
            b = null;
        } catch (OutOfMemoryError e) {
            b = null;
        }

        return b;
    }

    public Bitmap loadFromCache(String fileName) {
        final Context context = StaticContextApplication.getAppContext();

        String filename = fileName + "_cache";

        Bitmap b = null;

        try {
            InputStream is = context.openFileInput(filename);
            b = BitmapFactory.decodeStream(is, null, opt);

            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (b == null) {
                return null;
            }

            if (opt.outHeight < context.getResources().getDisplayMetrics().heightPixels
                    && opt.outWidth < context.getResources().getDisplayMetrics().widthPixels) {
                return b;
            } else if (opt.outWidth < opt.outHeight) {
                Bitmap bmp = ImageUtils
                        .getResizedBitmap(
                                b,
                                opt.outHeight > context.getResources().getDisplayMetrics().heightPixels ? context
                                        .getResources().getDisplayMetrics().heightPixels
                                        : opt.outHeight);

                if (b != null && bmp != b) {
                    b.recycle();
                    b = null;
                }

                if (bmp != null) {
                    return bmp;
                }
            } else {
                Bitmap bmp = ImageUtils
                        .getResizedBitmap(
                                b,
                                opt.outWidth > context.getResources().getDisplayMetrics().widthPixels ? context
                                        .getResources()
                                        .getDisplayMetrics().widthPixels : opt.outWidth);

                if (b != null && bmp != b) {
                    b.recycle();
                    b = null;
                }

                if (bmp != null) {
                    return bmp;
                }
            }
        } catch (FileNotFoundException e) {
            b = null;
        } catch (OutOfMemoryError e) {
            b = null;
        }

        return b;
    }

    public void putOntoMemory(String url, Bitmap bmp) {
        synchronized (sHardBitmapCache) {
            if (!sHardBitmapCache.containsKey(url) && bmp != null) {
                bmp.setDensity(0);

                final Context context = StaticContextApplication.getAppContext();
                CacheItem item = new CacheItem(new BitmapDrawable(context.getResources(), bmp));

                sHardBitmapCache.put(url, item);
            } else {
                sHardBitmapCache.get(url).refCount.increment();
            }
        }
    }

    @Override
    public void loadImageByUrl(ImageView view, String photoUrl, int requestedExtent,
            DefaultImageProvider defaultProvider, NewUrlRequest newUrlRequest) {
        download(photoUrl, view);
    }

    @Override
    public void removePhoto(ImageView view) {
        // TODO Auto-generated method stub
    }

    @Override
    public void pause() {
        // TODO Auto-generated method stub
    }

    @Override
    public void resume() {
        // TODO Auto-generated method stub
    }

    @Override
    public void cacheBitmap(Request originalRequest, Bitmap bitmap, byte[] photoBytes) {
        // TODO Auto-generated method stub
    }

    @Override
    public void preloadPhotosInBackground() {
        // TODO Auto-generated method stub
    }
}
