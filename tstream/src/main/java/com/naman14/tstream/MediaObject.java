package com.naman14.tstream;

/**
 * Created by naman on 17/02/16.
 */
public class MediaObject {

    private String title;
    private String author;
    private String url;

    public MediaObject() {
        this.title = "";
        this.author = "";
        this.url = null;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
