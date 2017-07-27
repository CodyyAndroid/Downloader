package com.codyy.download.service;


import android.support.annotation.IntDef;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 文件下载状态
 * Created by lijian on 2017/6/7.
 * @version 1.1.8
 */
@IntDef({DownloadFlag.NORMAL,
        DownloadFlag.WAITING,
        DownloadFlag.PROGRESS,
        DownloadFlag.PAUSED,
        DownloadFlag.COMPLETED,
        DownloadFlag.FAILED,
        DownloadFlag.ERROR,
        DownloadFlag.DELETED,
        DownloadFlag.RATE})
@Documented
@Retention(RetentionPolicy.SOURCE)
public @interface DownloadFlag {
    int NORMAL = 9990;
    int WAITING = 9991;
    int PROGRESS = 9992;
    int PAUSED = 9993;
    int COMPLETED = 9994;
    int FAILED = 9995;
    int ERROR = 9996;
    int DELETED = 9999;
    int RATE = -1;
}
