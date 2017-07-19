package com.codyy.download.entity;

import com.codyy.download.service.DownloadFlag;

import java.io.Serializable;

/**
 * Created by lijian on 2017/6/7.
 */

public class DownloadEntity implements Serializable {
    /**
     * 文件资源id
     */
    private String id;
    /**
     * 文件已下载大小
     */
    private long current;
    /**
     * 文件总大小
     */
    private long total;
    /**
     * 文件下载地址
     */
    private String url;
    /**
     * 文件下载后保存路径,默认为Download
     */
    private String savePath;
    /**
     * 文件名或标题名
     */
    private String name;
    /**
     * 文件当前下载状态
     */
    private int status;
    /**
     * 文件缩略图地址
     */
    private String thumbnails;
    /**
     * 文件下载日期(时间戳)
     */
    private long time;
    /**
     * 额外信息
     */
    private String extra1;
    /**
     * 额外信息
     */
    private String extra2;

    public DownloadEntity() {
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
                "id='" + id + '\'' +
                ", current=" + current +
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
