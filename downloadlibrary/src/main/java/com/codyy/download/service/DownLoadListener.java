package com.codyy.download.service;

/**
 * Created by lijian on 2017/6/7.
 */

public interface DownLoadListener {
    /*开始下载*/
    void onStart();

    /*等待中*/
    void onWaiting();

    //返回当前下载进度的百分比
    void onProgress(DownloadStatus status);

    /*暂停*/
    void onPause();

    /*下载完成*/
    void onComplete();

    /*删除下载*/
    void onDelete();

    /*下载失败*/
    void onFailure(int code);

    /*下载出错*/
    void onError(Exception e);
}
