package com.codyy.download.service;


import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Bundle 常量
 * Created by lijian on 2017/6/7.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
public @interface DownloadExtra {
    String EXTRA_RATE = "rate";
    String EXTRA_COUNT = "count";
    String EXTRA_ID = "id";
    String REQUEST_METHOD = "GET";
    String REQUEST_PROPERTY_KEY1 = "Accept";
    String REQUEST_PROPERTY_VALUE1 = "*/*";
    String REQUEST_PROPERTY_KEY2 = "Accept-Language";
    String REQUEST_PROPERTY_VALUE2 = "zh-CN";
    String REQUEST_PROPERTY_KEY3 = "Charset";
    String REQUEST_PROPERTY_VALUE3 = "UTF-8";
    String REQUEST_PROPERTY_KEY4 = "Range";
    String RANDOM_ACCESS_FILE_MODE = "rwd";
    int TIME_OUT = 10000;
}
