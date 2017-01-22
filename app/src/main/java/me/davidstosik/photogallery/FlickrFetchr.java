package me.davidstosik.photogallery;

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
    public static final String TAG = "FlickrFetchr";

    public static final String API_KEY = "f6272651d8e3d238580152cb6bf8e5f0";

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

    public List<GalleryItem> fetchItems() {
        List<GalleryItem> items = new ArrayList<>();

        try {
            String url = Uri.parse("https://api.flickr.com/services/rest/")
                    .buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s")
                    .build().toString();
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);

            items = new GsonBuilder().create()
                    .fromJson(jsonString, PhotosResponse.class)
                    .getPhotosPage()
                    .getGalleryItems();

            for (int i = 0; i < items.size(); i++) {
                Log.i(TAG, "fetchItems: " + items.get(i).getCaption());
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items: ", ioe);
        }

        return items;
    }
}
