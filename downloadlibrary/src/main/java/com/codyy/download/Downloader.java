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
import com.codyy.download.service.DownloadRateListener;
import com.codyy.download.service.DownloadService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件下载器
 * Created by lijian on 2017/6/7.
 */

public class Downloader {
    @SuppressLint("StaticFieldLeak")
    private static volatile Downloader INSTANCE;
    /**
     * DownloadService是否绑定
     */
    private volatile static boolean bound = false;
    private Context context;

    private Downloader(Context context) {
        this.context = context;
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

    /**
     * 开始下载服务
     */
    private void startDownloadService() {
        Intent intent = new Intent(context, DownloadService.class);
        context.startService(intent);
        context.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                bound = true;
                mDownloadService = ((DownloadService.DownloadBinder) service).getDownloadService();
                for (String key : mDownTasks.keySet()) {
                    mDownloadService.download(key, mDownTasks.get(key).path, mDownTasks.get(key).fileName, mDownTasks.get(key).thumbnails);
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
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                bound = false;
            }
        }, Context.BIND_AUTO_CREATE);
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
     * @param fileName    自定义文件名称
     */
    public void download(@NonNull String downloadUrl, String fileName) {
        this.download(downloadUrl, null, fileName, null);
    }

    /**
     * 开始下载
     *
     * @param downloadUrl 下载地址
     * @param fileName    自定义文件名称
     */
    public void download(@NonNull String downloadUrl, String fileName, String thumbnails) {
        this.download(downloadUrl, null, fileName, thumbnails);
    }

    /**
     * 开始下载
     *
     * @param downloadUrl 下载地址
     * @param path        自定义文件保存路径
     * @param fileName    自定义文件保存名称
     */
    public void download(@NonNull String downloadUrl, String path, String fileName, String thumbnails) {
        if (!bound) startDownloadService();
        if (mDownloadService != null) {
            mDownloadService.download(downloadUrl, path, fileName, thumbnails);
        } else {
            mDownTasks.put(downloadUrl, new FileEntity(path, fileName, thumbnails));
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
}
