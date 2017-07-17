package com.codyy.download.entity;

import com.codyy.download.service.DownloadFlag;

import java.io.Serializable;

/**
 * Created by lijian on 2017/6/7.
 */

public class DownloadEntity implements Serializable {
    private String id;
    private long current;
    private long total;
    private String url;
    private String savePath;
    private String name;
    private int status;
    private String thumbnails;
    private long time;
    private String extra1;
    private String extra2;

    public DownloadEntity() {
    }

    public DownloadEntity(long current, long total, String url, String savePath, String name, int status) {
        this.current = current;
        this.total = total;
        this.url = url;
        this.savePath = savePath;
        this.name = name;
        this.status = status;
    }

    public DownloadEntity(long current, long total, String url, String savePath, String name, int status, long time) {
        this.current = current;
        this.total = total;
        this.url = url;
        this.savePath = savePath;
        this.name = name;
        this.status = status;
        this.time = time;
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

    public DownloadEntity(long current, long total, String url, String savePath, String name, int status, String thumbnails, long time) {
        this.current = current;
        this.total = total;
        this.url = url;
        this.savePath = savePath;
        this.name = name;
        this.status = status;
        this.thumbnails = thumbnails;
        this.time = time;
    }

    public DownloadEntity(long current, long total, String url, String savePath, String name, int status, String thumbnails, long time, String extra1) {
        this.current = current;
        this.total = total;
        this.url = url;
        this.savePath = savePath;
        this.name = name;
        this.status = status;
        this.thumbnails = thumbnails;
        this.time = time;
        this.extra1 = extra1;
    }

    public DownloadEntity(long current, long total, String url, String savePath, String name, int status, String thumbnails, long time, String extra1, String extra2) {
        this.current = current;
        this.total = total;
        this.url = url;
        this.savePath = savePath;
        this.name = name;
        this.status = status;
        this.thumbnails = thumbnails;
        this.time = time;
        this.extra1 = extra1;
        this.extra2 = extra2;
    }

    public DownloadEntity(String id, long current, long total, String url, String savePath, String name, int status, String thumbnails, long time, String extra1, String extra2) {
        this.id = id;
        this.current = current;
        this.total = total;
        this.url = url;
        this.savePath = savePath;
        this.name = name;
        this.status = status;
        this.thumbnails = thumbnails;
        this.time = time;
        this.extra1 = extra1;
        this.extra2 = extra2;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getExtra1() {
        return extra1;
    }

    public void setExtra1(String extra1) {
        this.extra1 = extra1;
    }

    public String getExtra2() {
        return extra2;
    }

    public void setExtra2(String extra2) {
        this.extra2 = extra2;
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
                ", thumbnails='" + thumbnails + '\'' +
                ", time=" + time +
                ", extra1='" + extra1 + '\'' +
                ", extra2='" + extra2 + '\'' +
                '}';
    }
}
