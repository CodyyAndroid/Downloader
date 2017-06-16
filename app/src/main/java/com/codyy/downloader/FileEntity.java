package com.codyy.downloader;

/**
 * Created by lijian on 2017/6/8.
 */

public class FileEntity {
    private String name;
    private String url;

    public FileEntity(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
