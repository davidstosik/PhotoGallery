package me.davidstosik.photogallery;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;

/**
 * Created by sto on 2017/02/06.
 */

public class ImageCache {
    private static final String TAG = "ImageCache";
    private LruCache<String, Bitmap> mCache;
    private static final int CACHE_SIZE = 32 * 1024 * 1024;

    private static class Holder {
        static final ImageCache INSTANCE = new ImageCache();
    }

    private ImageCache() {
        mCache = new LruCache<String, Bitmap>(CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
    }

    public static ImageCache getInstance() {
        return Holder.INSTANCE;
    }

    public LruCache<String, Bitmap> getLru() {
        return mCache;
    }

    public static void logCacheStats() {
        LruCache cache = getInstance().getLru();

        synchronized (cache) {
            Log.d(TAG, "logCacheStats: " + cache
                    + ",puts=" + cache.putCount()
                    + ",evictions=" + cache.evictionCount());
        }
    }
}
