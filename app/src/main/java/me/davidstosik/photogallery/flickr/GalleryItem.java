package me.davidstosik.photogallery.flickr;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class GalleryItem {

    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("owner")
    @Expose
    private String owner;
    @SerializedName("secret")
    @Expose
    private String secret;
    @SerializedName("server")
    @Expose
    private String server;
    @SerializedName("farm")
    @Expose
    private Integer farm;
    @SerializedName("title")
    @Expose
    private String caption;
    @SerializedName("ispublic")
    @Expose
    private Integer isPublic;
    @SerializedName("isfriend")
    @Expose
    private Integer isFriend;
    @SerializedName("isfamily")
    @Expose
    private Integer isFamily;
    @SerializedName("url_s")
    @Expose
    private String url;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public Integer getFarm() {
        return farm;
    }

    public void setFarm(Integer farm) {
        this.farm = farm;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public Integer isPublic() {
        return isPublic;
    }

    public void setPublic(Integer isPublic) {
        this.isPublic = isPublic;
    }

    public Integer isFriend() {
        return isFriend;
    }

    public void setFriend(Integer isFriend) {
        this.isFriend = isFriend;
    }

    public Integer isFamily() {
        return isFamily;
    }

    public void setFamily(Integer isFamily) {
        this.isFamily = isFamily;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String toString() {
        return getCaption();
    }
}
