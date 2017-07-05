package com.codyy.download.service;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 网络类型
 * Created by lijian on 2017/6/12.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
public @interface NetworkType {
    int NETWORK_WIFI = 0;
    int NETWORK_4G = 1;
    int NETWORK_3G = 2;
    int NETWORK_2G = 3;
    int NETWORK_UNKNOWN = 4;
    int NETWORK_NO = 5;
}
