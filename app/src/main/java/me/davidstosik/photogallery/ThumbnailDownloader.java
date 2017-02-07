package me.davidstosik.photogallery;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.util.LruCache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by sto on 2017/01/26.
 */

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private boolean mHasQuit = false;
    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap();
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;
    private LruCache<String, Bitmap> mImageLruCache;

    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloadListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    private ThumbnailDownloader(Handler responseHandler, int priority) {
        super(TAG, priority);
        mResponseHandler = responseHandler;
    }

    public static ThumbnailDownloader<String> getThumbnailPreloader() {
        return new ThumbnailDownloader(null, Process.THREAD_PRIORITY_LOWEST);
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    handleRequest(target);
                }
            }
        };
        mImageLruCache = ImageCache.getInstance().getLru();
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    public void queueThumbnail(T target, String url) {
        if (target == null) {
            return;
        } else if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
                    .sendToTarget();
        }
    }

    public void queueThumbnail(String url) {
        queueThumbnail((T)url, url);
    }

    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }

    private void handleRequest(final T target) {
        final String url = mRequestMap.get(target);
        if (url == null) {
            return;
        }

        final Bitmap bitmap = downloadImage(url);

        if (mResponseHandler == null) {
            return;
        }

        mResponseHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mRequestMap.get(target) != url || mHasQuit) {
                    return;
                }
                mRequestMap.remove(target);
                mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
            }
        });
    }

    private Bitmap downloadImage(String url) {
        Bitmap bitmap;
        synchronized (mImageLruCache) {
            bitmap = mImageLruCache.get(url);
        }
        ImageCache.logCacheStats();
        if (bitmap == null) {
            Log.i(TAG, "downloadImage: Bitmap not found in cache: " + url);
            bitmap = new FlickrFetchr().fetchImage(url);
            synchronized (mImageLruCache) {
                mImageLruCache.put(url, bitmap);
            }
            ImageCache.logCacheStats();
            Log.d(TAG, "downloadImage: size = " + bitmap.getByteCount());
            return bitmap;
        }
        return bitmap;
    }
}
