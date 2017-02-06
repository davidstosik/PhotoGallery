package me.davidstosik.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import me.davidstosik.photogallery.flickr.GalleryItem;

/**
 * Created by sto on 2017/01/20.
 */

public class PhotoGalleryFragment extends Fragment {
    public static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private int mLastPage;
    private int mSpanCount;
    private int mCellSize;
    private int mDisplayableItems;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mLastPage = 0;
        new FetchItemsTask().execute(mLastPage);

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        photoHolder.bindDrawable(drawable);
                    }
                }
        );
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "onCreate: Background thread started");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_photo_gallery_recycler_view);

        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                setSpanCount();
                GridLayoutManager layoutManager = (GridLayoutManager) mPhotoRecyclerView.getLayoutManager();
                layoutManager.setSpanCount(mSpanCount);
            }
        });

        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), getMinSpanCount());
        mPhotoRecyclerView.setLayoutManager(layoutManager);

        setupAdapter();

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "onDestroy: Background thread destroyed");
    }

    private void setupAdapter() {
        if (isAdded()) {
            PhotoAdapter adapter = (PhotoAdapter) mPhotoRecyclerView.getAdapter();

            if (adapter == null) {
                adapter = new PhotoAdapter(mItems);
                mPhotoRecyclerView.setAdapter(adapter);
                mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                        Log.d(TAG, "onScrollStateChanged: " + newState);
                        super.onScrollStateChanged(recyclerView, newState);

                        if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                            return;
                        }

                        GridLayoutManager layoutManager = (GridLayoutManager) mPhotoRecyclerView.getLayoutManager();
                        int totalItemCount = layoutManager.getItemCount();
                        int lastVisibleItem = layoutManager.findLastVisibleItemPosition();

                        if (lastVisibleItem == totalItemCount - 1) {
                            Log.d(TAG, "onScrolled: End of list");
                            mLastPage++;
                            new FetchItemsTask().execute(mLastPage);
                        }
                    }
                });
            } else {
                adapter.notifyItemInserted(mItems.size() - 1);
            }
        }
    }

    private int getMinSpanCount() {
        return getResources().getInteger(R.integer.min_span_count);
    }

    private void setSpanCount() {
        float width = mPhotoRecyclerView.getWidth();
        float minWidth = getResources().getDimension(R.dimen.min_cell_width);

        Log.d(TAG, "setSpanCount: width = " + width);
        Log.d(TAG, "setSpanCount: minWidth = " + minWidth);

        int computedSpanCount = (int) Math.floor(width / minWidth);
        Log.d(TAG, "setSpanCount: computed span count = " + computedSpanCount);
        mSpanCount = Math.max(computedSpanCount, getMinSpanCount());
        Log.d(TAG, "setSpanCount: final span count = " + mSpanCount);

        mCellSize = (int) width / mSpanCount;
        Log.d(TAG, "setSpanCount: cellSize = " + mCellSize);

        float height = mPhotoRecyclerView.getHeight();
        mDisplayableItems = (int) (mSpanCount * Math.ceil(height / mCellSize));
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mItemImageView;
        private ProgressBar mItemProgressBar;

        public PhotoHolder(View itemView) {
            super(itemView);
            RelativeLayout layout = (RelativeLayout) itemView.findViewById(R.id.fragment_photo_gallery_layout);
            mItemImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
            mItemProgressBar = (ProgressBar) itemView.findViewById(R.id.fragment_photo_gallery_progress_bar);

            layout.getLayoutParams().height = mCellSize;
        }

        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
            mItemImageView.setVisibility(View.VISIBLE);
            mItemProgressBar.setVisibility(View.GONE);
        }

        public void unbindDrawable() {
            mItemImageView.setImageDrawable(null);
            mItemImageView.setVisibility(View.GONE);
            mItemProgressBar.setVisibility(View.VISIBLE);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, viewGroup, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            photoHolder.unbindDrawable();
            mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private class FetchItemsTask extends AsyncTask<Integer,Void,List<GalleryItem>> {
        @Override
        protected List<GalleryItem> doInBackground(Integer... params) {
            int page = 0;
            if (params.length > 0) {
                page = params[0].intValue();
            }
            return new FlickrFetchr().fetchItems(page);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems.addAll(items);
            setupAdapter();
        }
    }
}
