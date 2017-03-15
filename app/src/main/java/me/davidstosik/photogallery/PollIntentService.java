package me.davidstosik.photogallery;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

/**
 * Created by sto on 2017/02/14.
 */

public class PollIntentService extends IntentService {
    private static final String TAG = "PollIntentService";

    public PollIntentService() {
        super(TAG);
    }

    private static Intent newIntent(Context context) {
        return new Intent(context, PollIntentService.class);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        PollServiceHelper.poll(this);
    }

    public static class Interface implements PollServiceHelper.Interface {
        private Context mContext;

        public Interface(Context context) {
            mContext = context;
        }

        @Override
        public void schedule(boolean isOn) {
            Log.d(TAG, "schedule: " + String.valueOf(isOn));
            Intent i = PollIntentService.newIntent(mContext);
            PendingIntent pi = PendingIntent.getService(mContext, 0, i, 0);

            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            if (isOn) {
                alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                        SystemClock.elapsedRealtime(), PollServiceHelper.POLL_INTERVAL, pi);
            } else {
                alarmManager.cancel(pi);
                pi.cancel();
            }
        }

        @Override
        public boolean isScheduled() {
            Intent i = PollIntentService.newIntent(mContext);
            PendingIntent pi = PendingIntent.getService(mContext, 0, i, PendingIntent.FLAG_NO_CREATE);
            boolean isScheduled = pi != null;
            Log.d(TAG, "isScheduled: " + String.valueOf(isScheduled));
            return isScheduled;
        }
    }
}
