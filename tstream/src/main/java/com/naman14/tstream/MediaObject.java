package com.naman14.tstream;

/**
 * Created by naman on 17/02/16.
 */
public class MediaObject {

    public String title;
    public String author;

    public MediaObject() {
        this.title = null;
        this.author = null;
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
}
