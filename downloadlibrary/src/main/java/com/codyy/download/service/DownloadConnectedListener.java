package com.codyy.download.service;

/**
 * Created by lijian on 2017/6/20.
 */

public interface DownloadConnectedListener {
    /**
     * 服务启动成功后,可执行增删改查操作
     */
    void onConnected();
}
