package com.codyy.download.entity;

import com.codyy.download.service.DownloadFlag;

import java.io.Serializable;

/**
 * Created by lijian on 2017/6/7.
 */

public class DownloadEntity implements Serializable {
    private long current;
    private long total;
    private String url;
    private String savePath;
    private String name;
    private int status;
    private String thumbnails;

    public DownloadEntity(long current, long total, String url, String savePath, String name, int status) {
        this.current = current;
        this.total = total;
        this.url = url;
        this.savePath = savePath;
        this.name = name;
        this.status = status;
    }

    public DownloadEntity(long current, long total, String url, String savePath, String name, int status, String thumbnails) {
        this.current = current;
        this.total = total;
        this.url = url;
        this.savePath = savePath;
        this.name = name;
        this.status = status;
        this.thumbnails = thumbnails;
    }

    public String getThumbnails() {
        return thumbnails;
    }

    public void setThumbnails(String thumbnails) {
        this.thumbnails = thumbnails;
    }

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public
    @DownloadFlag
    int getStatus() {
        return status;
    }

    public void setStatus(@DownloadFlag int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "DownloadEntity{" +
                "current=" + current +
                ", total=" + total +
                ", url='" + url + '\'' +
                ", savePath='" + savePath + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                '}';
    }
}
