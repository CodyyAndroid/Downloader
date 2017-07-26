package com.codyy.download.service;

/**
 * Created by lijian on 2017/7/26.
 */

public abstract class SimpleDownloadListener implements DownLoadListener {
    @Override
    public void onStart() {

    }

    @Override
    public void onWaiting() {

    }

    @Override
    public void onProgress(DownloadStatus status) {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onComplete() {

    }

    @Override
    public void onDelete() {

    }

    @Override
    public void onFailure(int code) {

    }

    @Override
    public void onError(Exception e) {

    }
}
