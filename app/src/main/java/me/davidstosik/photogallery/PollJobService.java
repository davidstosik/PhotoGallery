package me.davidstosik.photogallery;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Created by sto on 2017/02/23.
 */

@TargetApi(android.os.Build.VERSION_CODES.LOLLIPOP)
public class PollJobService extends JobService {
    private static final String TAG = "PollJobService";
    private static final int JOB_ID = 1;

    private PollTask mCurrentTask;

    @Override
    public boolean onStartJob(JobParameters params) {
        mCurrentTask = new PollTask();
        mCurrentTask.execute(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParams) {
        if (mCurrentTask != null) {
            mCurrentTask.cancel(true);
        }
        return true;
    }

    public static class Interface implements PollServiceHelper.Interface {
        private Context mContext;

        public Interface(Context context) {
            mContext = context;
        }

        @Override
        public void schedule(boolean isOn) {
            Log.d(TAG, "schedule: " + String.valueOf(isOn));
            JobScheduler scheduler = (JobScheduler) mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            JobInfo jobInfo = new JobInfo.Builder(JOB_ID, new ComponentName(mContext, PollJobService.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(PollServiceHelper.POLL_INTERVAL)
                    .setPersisted(true)
                    .build();
            if (isOn) {
                scheduler.schedule(jobInfo);
            } else {
                scheduler.cancel(JOB_ID);
            }

        }

        @Override
        public boolean isScheduled() {
            JobScheduler scheduler = (JobScheduler) mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            for (JobInfo jobInfo : scheduler.getAllPendingJobs()) {
                if (jobInfo.getId() == JOB_ID) {
                    Log.d(TAG, "isScheduled: true");
                    return true;
                }
            }
            Log.d(TAG, "isScheduled: false");
            return false;
        }
    }

    private class PollTask extends AsyncTask<JobParameters,Void,Void> {
        @Override
        protected Void doInBackground(JobParameters... params) {
            Log.d(TAG, "doInBackground");
            JobParameters jobParams = params[0];

            PollServiceHelper.poll(PollJobService.this);

            jobFinished(jobParams, false);
            return null;
        }
    }
}
