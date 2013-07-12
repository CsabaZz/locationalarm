package com.bitknights.locationalarm;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.Log;

import com.bitknights.locationalarm.utils.Utils;
import com.bitknights.locationalarm.utils.image.ImageManager;

public class StaticContextApplication extends Application {
    private static final String TAG = Utils.TAG + StaticContextApplication.class.getSimpleName();

    private static Application INSTANCE;
    private static List<Activity> ACTIVITY_CONTEXT_LIST = new ArrayList<Activity>();

    private static int mMemoryClass = -1;

    public static Context getAppContext() {
	return INSTANCE;
    }

    public static Resources getStaticResources() {
	return null == INSTANCE ? null : INSTANCE.getResources();
    }

    public static float getStaticDimension(int resId) {
	return null == INSTANCE ? 0f : INSTANCE.getResources().getDimension(resId);
    }

    public static Activity getCurrentActivityContext() {
	if (ACTIVITY_CONTEXT_LIST.isEmpty()) {
	    return null;
	}

	return ACTIVITY_CONTEXT_LIST.get(ACTIVITY_CONTEXT_LIST.size() - 1);
    }

    public static void addActivityContext(Activity activity) {
	ACTIVITY_CONTEXT_LIST.add(activity);
    }

    public static void removeActivityContext(Activity activity) {
	ACTIVITY_CONTEXT_LIST.remove(activity);
    }

    private ImageManager mImageManager;

    public StaticContextApplication() {
	INSTANCE = this;
    }

    @Override
    public void onCreate() {
	super.onCreate();

	Locale def = Locale.getDefault();

	if (!def.getLanguage().toLowerCase().equals("hu")) {
	    Locale locale = new Locale("hu", "hu_HU");
	    Locale.setDefault(locale);

	    Configuration config = new Configuration();
	    config.locale = locale;

	    getResources().updateConfiguration(config, null);
	}

	final Context context = StaticContextApplication.INSTANCE;
	final Resources resources = context.getResources();
	final AssetManager manager = resources.getAssets();

	try {
	    Utils.NormalTypeface = Typeface.createFromAsset(manager, "fonts/Roboto-Regular.ttf");
	} catch (Exception e) {
	    Log.e(TAG, "Could not get typeface 'fonts/Roboto-Regular.ttf' because " + e.getMessage());
	}

	try {
	    Utils.BoldTypeface = Typeface.createFromAsset(manager, "fonts/Roboto-Bold.ttf");
	} catch (Exception e) {
	    Log.e(TAG, "Could not get typeface 'fonts/Roboto-Regular.ttf' because " + e.getMessage());
	}

	try {
	    Utils.ItalicTypeface = Typeface.createFromAsset(manager, "fonts/Roboto-Italic.ttf");
	} catch (Exception e) {
	    Log.e(TAG, "Could not get typeface 'fonts/Roboto-Regular.ttf' because " + e.getMessage());
	}
    }

    @Override
    public Object getSystemService(String name) {
	if (ImageManager.IMAGE_SERVICE.equals(name)) {
	    if (mImageManager == null) {
		mImageManager = ImageManager.createImageManager();

		if (android.os.Build.VERSION.SDK_INT > 13) {
		    SDK14.registerComponentCallbacks(mImageManager);
		}

		mImageManager.preloadPhotosInBackground();
	    }

	    return mImageManager;
	} else {
	    return super.getSystemService(name);
	}
    }

    @TargetApi(14)
    static class SDK14 {
	static void registerComponentCallbacks(Object obj) {
	    INSTANCE.registerComponentCallbacks((android.content.ComponentCallbacks2) obj);
	}
    }

    public static int getMemoryClass() {
	if (mMemoryClass < 0) {
	    ActivityManager manager = (ActivityManager) INSTANCE.getSystemService(Context.ACTIVITY_SERVICE);
	    mMemoryClass = manager.getMemoryClass();

	    manager = null;
	}

	return mMemoryClass;
    }

}
