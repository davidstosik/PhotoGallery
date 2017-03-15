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
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
    private View mLoadingView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private ThumbnailDownloader<String> mThumbnailPreloader;
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
        setHasOptionsMenu(true);
        mLastPage = 0;
        updateItems();

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

        mThumbnailPreloader = ThumbnailDownloader.getThumbnailPreloader();
        mThumbnailPreloader.start();
        mThumbnailPreloader.getLooper();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mLoadingView = v.findViewById(R.id.fragment_photo_gallery_progress_bar);

        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_photo_gallery_recycler_view);

        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                setSpanCount();
            }
        });

        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                Log.d(TAG, "onScrollStateChanged: " + newState);
                super.onScrollStateChanged(recyclerView, newState);

                if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                    return;
                }

                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                int totalItemCount = layoutManager.getItemCount();
                int lastVisibleItem = layoutManager.findLastVisibleItemPosition();

                if (lastVisibleItem == totalItemCount - 1) {
                    Log.d(TAG, "onScrollStateChanged: End of list");
                    mLastPage++;
                    updateItems();
                }

                mThumbnailPreloader.clearQueue();
                ((PhotoAdapter) recyclerView.getAdapter()).preloadAround();
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
        mThumbnailPreloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        mThumbnailPreloader.quit();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        final MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "onQueryTextSubmit: " + query);
                QueryPreferences.setStoredQuery(getContext(), query);

                searchView.setQuery("", false);
                searchView.setIconified(true);

                clearItems();
                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "onQueryTextChange: " + newText);
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick");
                String query = QueryPreferences.getStoredQuery(getContext());
                searchView.setQuery(query, false);
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                Log.d(TAG, "onClose: " + searchView.getQuery());
                return false;
            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollServiceHelper.isScheduled(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                clearItems();
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollServiceHelper.isScheduled(getActivity());
                PollServiceHelper.schedule(getActivity(), shouldStartAlarm);
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void clearItems() {
        Log.d(TAG, "clearItems: ");
        mLastPage = 0;
        int oldSize = mItems.size();
        mItems.clear();
        PhotoAdapter adapter = (PhotoAdapter) mPhotoRecyclerView.getAdapter();
        adapter.notifyItemRangeRemoved(0, oldSize);

        mThumbnailDownloader.clearQueue();
        mThumbnailPreloader.clearQueue();
    }

    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getActivity());
        if (mPhotoRecyclerView != null && mLastPage == 0) {
            mLoadingView.setVisibility(View.VISIBLE);
        }
        new FetchItemsTask(query, mLastPage).execute();
    }

    private void setupAdapter() {
        if (isAdded()) {
            PhotoAdapter adapter = new PhotoAdapter(mItems);
            mPhotoRecyclerView.setAdapter(adapter);
        }
    }

    private void updateAdapter(int positionStart, int size) {
        Log.d(TAG, "updateAdapter: ");
        PhotoAdapter adapter = (PhotoAdapter) mPhotoRecyclerView.getAdapter();
        adapter.notifyItemRangeInserted(positionStart, size);
        if (mLastPage > 0) {
            mPhotoRecyclerView.scrollBy(0, mCellSize / 2);
        }

        mLoadingView.setVisibility(View.GONE);
    }


    private int getMinSpanCount() {
        return getResources().getInteger(R.integer.min_span_count);
    }

    private void setSpanCount() {
        float width = mPhotoRecyclerView.getWidth();
        float minWidth = getResources().getDimension(R.dimen.min_cell_width);

        int computedSpanCount = (int) Math.floor(width / minWidth);
        mSpanCount = Math.max(computedSpanCount, getMinSpanCount());

        GridLayoutManager layoutManager = (GridLayoutManager) mPhotoRecyclerView.getLayoutManager();
        layoutManager.setSpanCount(mSpanCount);

        mCellSize = (int) width / mSpanCount;

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
            mItemProgressBar = (ProgressBar) itemView.findViewById(R.id.fragment_photo_item_progress_bar);

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

            if (position == 0) {
                preloadAround(-1, mDisplayableItems - 1);
            }
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }

        public void preloadAround() {
            GridLayoutManager layoutManager = (GridLayoutManager) mPhotoRecyclerView.getLayoutManager();
            int first = layoutManager.findFirstVisibleItemPosition();
            int last = layoutManager.findLastVisibleItemPosition();

            preloadAround(first, last);
        }

        public void preloadAround(int first, int last) {
            Log.d(TAG, "preloadAround: first=" + String.valueOf(first) + ", last=" + String.valueOf(last));

            if (last >= 0) {
                preloadInterval(last + 1, last + mDisplayableItems);
            }

            if (first > 0) {
                preloadInterval(first - mDisplayableItems, first - 1);
            }
        }

        private void preloadInterval(int first, int last) {
            Log.d(TAG, "preloadInterval: first=" + String.valueOf(first) + ", last=" + String.valueOf(last));
            for (int i = first; i <= last; i++) {
                try {
                    String url = mGalleryItems.get(i).getUrl();
                    mThumbnailPreloader.queueThumbnail(url);
                } catch (IndexOutOfBoundsException ioobe) {
                    // Nothing.
                }
            }
        }
    }

    private class FetchItemsTask extends AsyncTask<Void,Void,List<GalleryItem>> {
        private String mQuery;
        private int mPage;

        public FetchItemsTask(String query, int page) {
            Log.d(TAG, "FetchItemsTask: new query=" + query + " | page=" + String.valueOf(page));
            mQuery = query;
            mPage = page;
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            List<GalleryItem> result;
            if (mQuery == null || mQuery.equals("")) {
                result = new FlickrFetchr().fetchRecentPhotos(mPage);
            } else {
                result = new FlickrFetchr().searchPhotos(mQuery, mPage);
            }
            return result;
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            int positionStart = mItems.size();
            mItems.addAll(items);

            updateAdapter(positionStart, items.size());
        }
    }
}
