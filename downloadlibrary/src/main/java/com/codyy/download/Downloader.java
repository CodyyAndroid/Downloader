package com.codyy.download;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;

import com.codyy.download.entity.DownloadEntity;
import com.codyy.download.entity.FileEntity;
import com.codyy.download.service.DownLoadListener;
import com.codyy.download.service.DownloadConnectedListener;
import com.codyy.download.service.DownloadIsPauseAllListener;
import com.codyy.download.service.DownloadRateListener;
import com.codyy.download.service.DownloadService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件下载器
 * Created by lijian on 2017/6/7.
 *
 * @version 0.2.9
 */

public class Downloader {
    @SuppressLint("StaticFieldLeak")
    private static volatile Downloader INSTANCE;
    /**
     * DownloadService是否绑定
     */
    private volatile static boolean bound = false;
    private Context context;
    /**
     * 下载服务
     */
    private DownloadService mDownloadService;
    /**
     * 下载服务连接成功前,缓存接收下载状态任务
     */
    private Map<String, DownLoadListener> mReceiveDownloadStatus = new HashMap<>();
    private Map<String, DownloadRateListener> mReceiveDownloadRate = new HashMap<>();
    /**
     * 下载服务连接成功前,缓存下载任务
     */
    private Map<String, FileEntity> mDownTasks = new HashMap<>();

    private Downloader(Context context) {
        this.context = context;
    }

    public static boolean isBound() {
        return bound;
    }

    /**
     * 获取下载器单例
     *
     * @param context context
     * @return Downloader
     */
    public static Downloader getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (Downloader.class) {
                INSTANCE = new Downloader(context.getApplicationContext());
            }
        }
        return INSTANCE;
    }

    /**
     * 增加初始化方法
     */
    public static void init(Context context) {
        getInstance(context).startDownloadService();
    }

    /**
     * 开始下载服务
     */
    private void startDownloadService() {
        Intent intent = new Intent(context, DownloadService.class);
        context.startService(intent);
        context.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mDownloadService = ((DownloadService.DownloadBinder) service).getDownloadService();
                if (!bound && mConnectedListener != null) {
                    mConnectedListener.onConnected();
                }
                for (String key : mDownTasks.keySet()) {
                    mDownloadService.download(key, mDownTasks.get(key).path, mDownTasks.get(key).fileName, mDownTasks.get(key).thumbnails, mDownTasks.get(key).extra);
                }
                for (String key : mReceiveDownloadStatus.keySet()) {
                    mDownloadService.receiveDownloadStatus(key, mReceiveDownloadStatus.get(key));
                }
                for (String key : mReceiveDownloadRate.keySet()) {
                    mDownloadService.receiveDownloadRate(mReceiveDownloadRate.get(key));
                }
                mDownTasks.clear();
                mReceiveDownloadStatus.clear();
                mReceiveDownloadRate.clear();
                bound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                bound = false;
            }
        }, Context.BIND_AUTO_CREATE);
    }

    /**
     * 设置wifi状态是否自动下载
     *
     * @param wifiDownload 默认为true
     */
    public void setWifiDownload(boolean wifiDownload) {
        if (mDownloadService != null) {
            mDownloadService.setWifiDownload(wifiDownload);
        }
    }

    /**
     * 设置蜂窝数据是否自动下载
     *
     * @param honeyCombDownload 默认为false
     */
    public void setHoneyCombDownload(boolean honeyCombDownload) {
        if (mDownloadService != null) {
            mDownloadService.setHoneyCombDownload(honeyCombDownload);
        }
    }

    /**
     * 判断wifi状态是否可自动下载
     *
     * @return true:可自动下载;false:禁止自动下载
     */
    public boolean isWifiDownload() {
        return mDownloadService != null && mDownloadService.isWifiDownload();
    }

    /**
     * 判断蜂窝数据状态是否可自动下载
     *
     * @return true:可自动下载;false:禁止自动下载
     */
    public boolean isHoneyCombDownload() {
        return mDownloadService != null && mDownloadService.isHoneyCombDownload();
    }

    /**
     * 开始下载
     *
     * @param downloadUrl 下载地址
     */
    public void download(@NonNull String downloadUrl) {
        this.download(downloadUrl, null);
    }

    /**
     * 开始下载
     *
     * @param downloadUrl 下载地址
     * @param title       标题
     */
    public void download(@NonNull String downloadUrl, String title) {
        this.download(downloadUrl, null, title, null);
    }

    /**
     * 开始下载
     *
     * @param downloadUrl 下载地址
     * @param title       标题
     */
    public void download(@NonNull String downloadUrl, String title, String thumbnails) {
        this.download(downloadUrl, null, title, thumbnails, null);
    }

    /**
     * 开始下载
     *
     * @param downloadUrl 下载地址
     * @param title       标题
     * @param thumbnails  缩略图地址
     * @param extra       其他信息
     */
    public void download(@NonNull String downloadUrl, String title, String thumbnails, String extra) {
        this.download(downloadUrl, null, title, thumbnails, extra);
    }

    /**
     * 开始下载
     *
     * @param downloadUrl 下载地址
     * @param path        自定义文件保存路径
     * @param title       自定义文件保存名称
     * @param thumbnails  缩略图地址
     * @param extra       其他信息
     */
    public void download(@NonNull String downloadUrl, String path, String title, String thumbnails, String extra) {
        if (!bound) startDownloadService();
        if (mDownloadService != null) {
            mDownloadService.download(downloadUrl, path, title, thumbnails, extra);
        } else {
            mDownTasks.put(downloadUrl, new FileEntity(path, title, thumbnails));
        }
    }

    /**
     * 暂停下载
     *
     * @param urls 下载地址
     */
    public void pause(@NonNull String... urls) {
        if (mDownloadService != null) {
            mDownloadService.pause(urls);
        }
    }

    /**
     * 暂停下载
     *
     * @param urls 下载地址
     */
    public void pause(@NonNull List<String> urls) {
        if (mDownloadService != null) {
            mDownloadService.pause((String[]) urls.toArray());
        }
    }

    /**
     * 暂停所有下载任务
     */
    public void pauseAll() {
        if (mDownloadService != null) {
            mDownloadService.pauseAll();
        }
    }

    /**
     * 手动启动全部下载任务
     */
    public void startAll() {
        if (!bound) startDownloadService();
        if (mDownloadService != null) {
            mDownloadService.startAll();
        }
    }

    /**
     * 删除下载任务
     *
     * @param urls 下载地址
     */
    public void delete(@NonNull String... urls) {
        this.delete(false, urls);
    }

    /**
     * 删除下载任务
     *
     * @param urls 下载地址
     */
    public void delete(@NonNull List<String> urls) {
        this.delete(false, (String[]) urls.toArray());
    }

    /**
     * 删除下载任务
     *
     * @param isRetained true:保留下载文件,只删除记录;false:文件及记录均删除
     * @param urls
     */
    public void delete(boolean isRetained, @NonNull String... urls) {
        if (mDownloadService != null) {
            mDownloadService.delete(isRetained, urls);
        }
    }

    /**
     * 删除所有下载任务包括已下载的文件
     */
    public void deleteAll() {
        if (mDownloadService != null) {
            mDownloadService.deleteAll();
        }
    }

    /**
     * 接收下载状态
     *
     * @param downloadUrl  下载地址
     * @param loadListener 状态监听
     */
    public void receiveDownloadStatus(@NonNull String downloadUrl, @NonNull DownLoadListener loadListener) {
        if (!bound) startDownloadService();
        if (mDownloadService != null) {
            mDownloadService.receiveDownloadStatus(downloadUrl, loadListener);
        } else {
            mReceiveDownloadStatus.put(downloadUrl, loadListener);
        }
    }

    /**
     * 监听下载速率和正在下载的任务数{@link DownloadRateListener#onRate(String, int)}
     * 监听下载成功后返回下载结果{@link DownloadRateListener#onComplete(DownloadEntity)}
     *
     * @param downloadRateListener downloadRateListener
     */
    public void addRateListener(@NonNull DownloadRateListener downloadRateListener) {
        if (!bound) startDownloadService();
        if (mDownloadService != null) {
            mDownloadService.receiveDownloadRate(downloadRateListener);
        } else {
            mReceiveDownloadRate.put("rate", downloadRateListener);
        }
    }

    /**
     * 移除正在下载的监听{@link DownloadRateListener}
     */
    public void removeRateListener() {
        if (mDownloadService != null) {
            mDownloadService.removeRateListener();
        }
    }

    /**
     * 监听当前下载状态是否全部暂停,false:未全部暂停;true:全部暂停
     */
    public void addIsPauseAllListener(DownloadIsPauseAllListener listener) {
        if (mDownloadService != null) {
            mDownloadService.addIsPauseAllListener(listener);
        }
    }

    /**
     * 移除是否全部暂停监听
     */
    public void removeIsPauseAllListener() {
        if (mDownloadService != null) {
            mDownloadService.removeIsPauseAllListener();
        }
    }

    /**
     * 获取所有的下载记录
     *
     * @return list
     */
    public List<DownloadEntity> getTotalDownloadRecords() {
        if (mDownloadService != null) {
            return mDownloadService.getTotalDownloadRecords();
        }
        return new ArrayList<>();
    }

    /**
     * 获取未完成的下载记录
     */
    public List<DownloadEntity> getDownloadingRecords() {
        if (mDownloadService != null) {
            return mDownloadService.getDownloadingRecords();
        }
        return new ArrayList<>();
    }

    /**
     * 获取指定下载记录
     *
     * @param url 下载地址
     * @return entity
     */
    public DownloadEntity getDownloadRecord(String url) {
        if (mDownloadService != null) {
            return mDownloadService.getDownloadRecord(url);
        }
        return null;
    }

    private DownloadConnectedListener mConnectedListener;

    public void setOnConnectedListener(DownloadConnectedListener listener) {
        this.mConnectedListener = listener;
    }
}
