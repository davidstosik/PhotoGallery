package me.davidstosik.photogallery;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.util.List;

import me.davidstosik.photogallery.flickr.GalleryItem;

import static android.content.Context.CONNECTIVITY_SERVICE;

/**
 * Created by sto on 2017/02/23.
 */

public abstract class PollServiceHelper {
    private static final String TAG = "PollServiceHelper";

    public interface Interface {
        void schedule(boolean isOn);
        boolean isScheduled();
    }

    public static final int POLL_INTERVAL = 60 * 1000 * 15;

    public static void schedule(Context context, boolean isOn) {
        Log.d(TAG, "schedule: " + String.valueOf(isOn));
        getPollServiceInterface(context).schedule(isOn);
    }

    public static boolean isScheduled(Context context) {
        boolean isScheduled = getPollServiceInterface(context).isScheduled();
        Log.d(TAG, "isScheduled: " + String.valueOf(isScheduled));
        return isScheduled;
    }

    public static void poll(Context context) {
        if (!isNetworkAvailableAndConnected(context)) {
            return;
        }

        String query = QueryPreferences.getStoredQuery(context);
        String lastResultId = QueryPreferences.getLastResultId(context);
        List<GalleryItem> items;

        if (query == null) {
            items = new FlickrFetchr().fetchRecentPhotos(0);
        } else {
            items = new FlickrFetchr().searchPhotos(query, 0);
        }

        if (items.size() == 0) {
            return;
        }

        String resultId = items.get(0).getId();
        if (resultId.equals(lastResultId)) {
            Log.i(TAG, "Got an old result: " + resultId);
        } else {
            Log.i(TAG, "Got an new result: " + resultId);

            Resources resources = context.getResources();
            Intent i = PhotoGalleryActivity.newIntent(context);
            PendingIntent pi = PendingIntent.getActivity(context, 0, i, 0);

            Notification notification = new NotificationCompat.Builder(context)
                    .setTicker(resources.getString(R.string.new_pictures_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(resources.getString(R.string.new_pictures_title))
                    .setContentText(resources.getString(R.string.new_pictures_text))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(0, notification);
        }

        QueryPreferences.setPrefLastResultId(context, resultId);
    }

    private static boolean isNetworkAvailableAndConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);

        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();

        return isNetworkConnected;
    }

    private static boolean useJobService() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    }

    private static Interface getPollServiceInterface(Context context) {
        if (useJobService()) {
            return new PollJobService.Interface(context);
        } else {
            return new PollIntentService.Interface(context);
        }
    }
}
