package com.codyy.download.threadpool;

import android.support.annotation.IntDef;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by lijian on 2017/6/9.
 */
@IntDef({
        ThreadPoolType.CACHED_THREAD,
        ThreadPoolType.FIXED_THREAD,
        ThreadPoolType.DEFAULT_THREAD,
        ThreadPoolType.SINGLE_THREAD
})
@Documented
@Retention(RetentionPolicy.SOURCE)
public @interface ThreadPoolType {
    int FIXED_THREAD = 2;
    int CACHED_THREAD = 1;
    int DEFAULT_THREAD = 0;
    int SINGLE_THREAD = -1;
}
