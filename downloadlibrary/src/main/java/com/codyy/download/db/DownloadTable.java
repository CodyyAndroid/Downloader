package com.codyy.download.db;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by lijian on 2017/6/12.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
public @interface DownloadTable {
    /*表名称*/
    String TABLE_NAME = "download";
    /*断点续传当前下载位置*/
    String COLUMN_NAME_CURRENT_POSITION = "current";
    /*文件总共大小*/
    String COLUMN_NAME_TOTAL_SIZE = "total";
    /*下载地址*/
    String COLUMN_NAME_DOWNLOAD_URL = "url";
    /*本地下载路径*/
    String COLUMN_NAME_SAVE_PATH = "path";
    /*下载标题名称*/
    String COLUMN_NAME_TITLE = "title";
    /*下载状态*/
    String COLUMN_NAME_STATUS = "status";
    /*缩略图地址*/
    String COLUMN_NAME_THUMBNAILS = "thumbnails";
    /*下载时间*/
    String COLUMN_NAME_DOWNLOAD_TIME = "time";
    /*其他信息1*/
    String COLUMN_NAME_EXTRA1 = "extra1";
    /*其他信息2*/
    String COLUMN_NAME_EXTRA2 = "extra2";
}
