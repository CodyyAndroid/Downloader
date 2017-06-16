package com.codyy.download.entity;

/**
 * Created by lijian on 2017/6/9.
 */

public class FileEntity {
    public String path;
    public String fileName;
    public String thumbnails;

    public FileEntity(String path, String fileName, String thumbnails) {
        this.path = path;
        this.fileName = fileName;
        this.thumbnails = thumbnails;
    }

    public FileEntity(String path, String fileName) {
        this.path = path;
        this.fileName = fileName;
    }

}
