package me.davidstosik.photogallery.flickr;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PhotosResponse {

    @SerializedName("photos")
    @Expose
    private PhotosPage photosPage;
    @SerializedName("stat")
    @Expose
    private String stat;

    public PhotosPage getPhotosPage() {
        return photosPage;
    }

    public void setPhotosPage(PhotosPage photosPage) {
        this.photosPage = photosPage;
    }

    public String getStat() {
        return stat;
    }

    public void setStat(String stat) {
        this.stat = stat;
    }

    public PhotosResponse() {
        photosPage = new PhotosPage();
    }

    public static PhotosResponse parseJSON(String response) {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(response, PhotosResponse.class);
    }
}