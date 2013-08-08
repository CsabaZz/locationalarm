
package com.bitknights.locationalarm.utils.image;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bitknights.locationalarm.StaticContextApplication;
import com.bitknights.locationalarm.utils.Utils;

public class ImageUtils {
    private static final String TAG = Utils.TAG + ImageUtils.class.getSimpleName();

    private static final class CacheRunnable implements Runnable {
        private Bitmap mBitmap;
        private String mFileName;

        // private Exception mOuterEx;

        public void setBitmap(Bitmap bitmap) {
            this.mBitmap = bitmap;
        }

        public void setFileName(String fileName) {
            this.mFileName = fileName;
        }

        /*
         * public void setOuterex(Exception outerex) { this.mOuterEx = outerex;
         * }
         */

        @Override
        public void run() {
            if (mBitmap == null || TextUtils.isEmpty(mFileName)) {
                return;
            }

            String filename = mFileName + "_cache";

            final Context context = StaticContextApplication.getAppContext();
            OutputStream os = null;

            try {
                os = context.openFileOutput(filename, Context.MODE_PRIVATE);
            } catch (IOException ex) {
                // Log.e(TAG,
                // "This will help me to find out what is wrong here",
                // mOuterEx);
                Log.e(TAG, "Can not open the output file", ex);
                return;
            }

            mBitmap.compress(CompressFormat.JPEG, 100, os);

            try {
                os.close();
            } catch (IOException ex) {
                // Log.e(TAG,
                // "This will help me to find out what is wrong here",
                // mOuterEx);
                Log.e(TAG, "Can not close the output file", ex);
            }
        }
    }

    @SuppressLint("UseSparseArrays")
    private static final HashMap<Integer, SoftReference<Drawable>> cache = new HashMap<Integer, SoftReference<Drawable>>();
    private static final ArrayList<Integer> keyCache = new ArrayList<Integer>();

    public static Drawable getDrawable(Integer resId) {
        final Context context = StaticContextApplication.getAppContext();

        Drawable drawable = null;

        synchronized (cache) {
            if (cache.containsKey(resId)) {
                SoftReference<Drawable> reference = cache.get(resId);
                if (reference != null) {
                    drawable = reference.get();
                    if (drawable != null) {
                        // Log.e(TAG, resId.intValue() +
                        // " has been read from the cache!");
                        return drawable;
                    }
                }
            }
        }

        try {
            int resourceId = resId.intValue();
            if (resourceId == 0) {
                return null;
            }

            drawable = context.getResources().getDrawable(resourceId);

            synchronized (cache) {
                cache.put(resId, new SoftReference<Drawable>(drawable));
                keyCache.add(resId);
            }

            // Log.e(TAG, resId.intValue() + " has been created and cached!");
        } catch (OutOfMemoryError ex) {
            Log.e(TAG, "Out of bitmap memory", ex);
        }

        return drawable;
    }

    public static void destroy(Integer id) {
        SoftReference<Drawable> reference = cache.remove(id);

        if (reference != null) {
            reference.clear();
        }

        reference = null;
    }

    public static void destroyAll() {
        if (null == cache || null == keyCache) {
            return;
        }

        synchronized (cache) {
            while (keyCache.size() > 0) {
                destroy(keyCache.remove(keyCache.size() - 1));
            }
        }
    }

    public static void unbindDrawables(View view) {
        clearReferences(view);
        System.gc();
    }

    @SuppressWarnings("deprecation")
    private static void clearReferences(View view) {
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
            view.setBackgroundDrawable(null);
        }

        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                clearReferences(((ViewGroup) view).getChildAt(i));
            }

        } else if (view instanceof ImageView) {
            ((ImageView) view).setImageBitmap(null);
        }
    }

    public static Bitmap getResizedBitmap(Bitmap bitmap, int size) {
        Matrix resizerMatrix = getResizerMatrix(bitmap.getWidth(), bitmap.getHeight(), size, size,
                true);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                resizerMatrix, true);
    }

    public static Matrix getResizerMatrix(float originalWidth, float originalHeight,
            float desiredWidth,
            float desiredHeight, boolean keepAspectRatio) {
        float scaleWidth = 0.0f;
        float scaleHeight = 0.0f;
        if ((desiredWidth == desiredHeight) || keepAspectRatio) {
            float scaleMinValue = Math.min(desiredWidth / originalWidth, desiredHeight
                    / originalHeight);
            scaleWidth = scaleMinValue;
            scaleHeight = scaleMinValue;
        } else {
            scaleWidth = desiredWidth / originalWidth;
            scaleHeight = desiredHeight / originalHeight;
        }

        Matrix result = new Matrix();
        result.postScale(scaleWidth, scaleHeight);
        return result;
    }

    public static Bitmap getWidthResizedBitmap(Bitmap bitmap, int width) {
        Matrix resizerMatrix = getSizeResizerMatrix(bitmap.getWidth(), width);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                resizerMatrix, true);
    }

    public static Bitmap getHeightResizedBitmap(Bitmap bitmap, int height) {
        Matrix resizerMatrix = getSizeResizerMatrix(bitmap.getHeight(), height);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                resizerMatrix, true);
    }

    public static Matrix getSizeResizerMatrix(float originalSize, float desiredSize) {
        float scale = desiredSize / originalSize;

        Matrix result = new Matrix();
        result.postScale(scale, scale);
        return result;
    }

    public static void cacheImage(Bitmap bitmap, String fileName) {
        /*
         * Exception runex = null; try { throw new
         * RuntimeException("Throw an exception directly..."); } catch
         * (RuntimeException ex) { runex = ex; }
         */

        CacheRunnable cacheRunnable = new CacheRunnable();
        cacheRunnable.setBitmap(bitmap);
        cacheRunnable.setFileName(fileName);
        // cacheRunnable.setOuterex(runex);

        Thread t = new Thread(cacheRunnable);
        t.setDaemon(false);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        t.start();
    }
}
