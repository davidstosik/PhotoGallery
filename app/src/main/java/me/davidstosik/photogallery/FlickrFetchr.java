package me.davidstosik.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.google.gson.GsonBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import me.davidstosik.photogallery.flickr.GalleryItem;
import me.davidstosik.photogallery.flickr.PhotosResponse;

/**
 * Created by sto on 2017/01/20.
 */

public class FlickrFetchr {
    private static final String TAG = "FlickrFetchr";

    private static final String API_KEY = "f6272651d8e3d238580152cb6bf8e5f0";
    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri ENDPOINT = Uri
            .parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() +
                        ": with " +
                        urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    private List<GalleryItem> downloadGalleryItems(String url) {
        Log.d(TAG, "downloadGalleryItems: START");
        List<GalleryItem> items = new ArrayList<>();

        try {
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);

            items = new GsonBuilder().create()
                    .fromJson(jsonString, PhotosResponse.class)
                    .getPhotosPage()
                    .getGalleryItems();

            for (int i = 0; i < items.size(); i++) {
                Log.i(TAG, "fetchItems: " + items.get(i).getCaption());
                if (items.get(i).getUrl() == null || items.get(i).getUrl().equals("")) {
                    Log.w(TAG, "fetchItems: ^ URL is EMPTY!");
                }
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items: ", ioe);
        }

        return items;
    }

    private String buildUrl(String method, String query, int page) {
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("method", method);

        if (page >= 0) {
            uriBuilder.appendQueryParameter("page", String.valueOf(page));
        }

        if (method == SEARCH_METHOD) {
            uriBuilder.appendQueryParameter("text", String.valueOf(query));
        }

        return uriBuilder.build().toString();
    }

    public List<GalleryItem> fetchRecentPhotos(int page) {
        String url = buildUrl(FETCH_RECENTS_METHOD, null, page);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query, int page) {
        String url = buildUrl(SEARCH_METHOD, query, page);
        return downloadGalleryItems(url);
    }

    public Bitmap fetchImage(String urlSpec) {
        try {
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(urlSpec);
            return BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch image: ", ioe);
            return null;
        }
    }
}
