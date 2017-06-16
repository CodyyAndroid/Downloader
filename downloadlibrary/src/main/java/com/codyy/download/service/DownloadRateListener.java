package com.codyy.download.service;

import com.codyy.download.entity.DownloadEntity;

/**
 * Created by lijian on 2017/6/13.
 */

public interface DownloadRateListener {
    /**
     * 下载速率
     *
     * @param rate  XXMB/S
     * @param count 任务数量
     */
    void onRate(String rate, int count);

    /**
     * 下载完成返回结果
     */
    void onComplete(DownloadEntity entity);
}
