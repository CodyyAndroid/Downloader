package com.codyy.download.service;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 数据库查询条件常量
 * Created by lijian on 2017/6/12.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
public @interface DBSelection {
    String SELECTION_EQUAL = "=?";
    String SELECTION_UNEQUAL = " NOT IN (?)";
    String SELECTION_DESC = " desc";
}
