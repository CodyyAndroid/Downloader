package com.codyy.download.service;

import android.util.Log;

import com.codyy.download.Downloader;


/**
 * 日志工具类
 */
class Cog {

    public static void d(String tag, String msg) {
        if (Downloader.DEBUG) {
            Log.d(tag, msg);
        }
    }

    public static void d(String tag, String... messages) {
        if (Downloader.DEBUG) {
            Log.d(tag, spliceMessages(messages));
        }
    }

    public static void d(String tag, Object... messages) {
        if (Downloader.DEBUG) {
            Log.d(tag, spliceMessages(messages));
        }
    }

    public static void i(String tag, String msg) {
        if (Downloader.DEBUG) {
            Log.i(tag, msg);
        }
    }

    public static void i(String tag, String... messages) {
        if (Downloader.DEBUG) {
            Log.i(tag, spliceMessages(messages));
        }
    }

    public static void i(String tag, Object... messages) {
        if (Downloader.DEBUG) {
            Log.i(tag, spliceMessages(messages));
        }
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void e(String tag, String... messages) {
        Log.e(tag, spliceMessages(messages));
    }

    public static void e(String tag, Object... messages) {
        Log.e(tag, spliceMessages(messages));
    }

    public static void e(String tag, String msg, Throwable ex) {
        Log.e(tag, msg, ex);
    }

    public static void e(String tag, Throwable ex, String... messages) {
        Log.e(tag, spliceMessages(messages), ex);
    }

    /**
     * 拼接日志消息
     *
     * @param messages
     * @return
     */
    private static String spliceMessages(String... messages) {
        String msg = "";
        for (String str : messages) {
            msg += str;
        }
        return msg;
    }

    /**
     * 对象转字串了后拼接日志消息
     *
     * @param messages
     * @return
     */
    private static String spliceMessages(Object... messages) {
        String msg = "";
        for (Object obj : messages) {
            msg += obj;
        }
        return msg;
    }

}
