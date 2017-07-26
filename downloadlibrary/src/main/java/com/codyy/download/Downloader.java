package com.codyy.download;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.codyy.download.entity.DownloadEntity;
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
 * @version 1.1.7
 */

public class Downloader {
    /**
     * 存储空间不足action
     */
    public static final String ACTION_DOWNLOAD_OUT_OF_MEMORY = "action.download.out.of.memory";
    @SuppressLint("StaticFieldLeak")
    private static volatile Downloader INSTANCE;
    /**
     * DownloadService是否绑定
     */
    private volatile static boolean bound = false;
    public static boolean DEBUG = false;
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
    private Map<String, DownloadEntity> mDownTasks = new HashMap<>();

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

    private static void initDebug(boolean debug) {
        DEBUG = debug;
    }

    /**
     * 增加初始化方法
     */
    public static void init(Context context) {
        getInstance(context).startDownloadService();
    }

    public static void init(Context context, boolean debug) {
        initDebug(debug);
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
                    mDownloadService.download(mDownTasks.get(key));
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
     * 同步已下载的记录
     *
     * @param entity 下载记录
     * @return true:同步成功;false:服务未启动或同步失败
     */
    public boolean syncDownloadRecord(@NonNull DownloadEntity entity) {
        return mDownloadService != null && mDownloadService.syncDownloadRecord(entity);
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
     */
    public void download(@NonNull DownloadEntity entity) {
        if (!bound) startDownloadService();
        if (mDownloadService != null) {
            mDownloadService.download(entity);
        } else {
            mDownTasks.put(TextUtils.isEmpty(entity.getId()) ? entity.getUrl() : entity.getId(), entity);
        }
    }

    /**
     * 暂停下载
     *
     * @param ids 下载地址
     */
    public void pause(@NonNull String... ids) {
        if (mDownloadService != null) {
            mDownloadService.pause(ids);
        }
    }

    /**
     * 暂停下载
     *
     * @param ids 下载地址
     */
    public void pause(@NonNull List<String> ids) {
        if (mDownloadService != null) {
            mDownloadService.pause((String[]) ids.toArray());
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
     * @param ids 下载地址
     */
    public void delete(@NonNull String... ids) {
        this.delete(false, ids);
    }

    /**
     * 删除下载任务
     *
     * @param ids 下载地址
     */
    public void delete(@NonNull List<String> ids) {
        this.delete(false, (String[]) ids.toArray());
    }

    /**
     * 删除下载任务
     *
     * @param isRetained true:保留下载文件,只删除记录;false:文件及记录均删除
     * @param ids
     */
    public void delete(boolean isRetained, @NonNull String... ids) {
        if (mDownloadService != null) {
            mDownloadService.delete(isRetained, ids);
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
     * @param id           下载资源ID
     * @param loadListener 状态监听
     */
    public void receiveDownloadStatus(@NonNull String id, @NonNull DownLoadListener loadListener) {
        if (!bound) startDownloadService();
        if (mDownloadService != null) {
            mDownloadService.receiveDownloadStatus(id, loadListener);
        } else {
            mReceiveDownloadStatus.put(id, loadListener);
        }
    }

    /**
     * 移除下载状态监听
     *
     * @param id 下载资源ID
     */
    public void removeDownloadStatusListener(@NonNull String id) {
        if (mDownloadService != null) {
            mDownloadService.removeDownloadStatusListener(id);
        }
    }

    /**
     * 移除所有下载状态监听
     */
    public void removeAllDownloadStatusListener() {
        if (mDownloadService != null) {
            mDownloadService.removeAllDownloadStatusListener();
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
     * @param id 下载地址
     * @return entity
     */
    public DownloadEntity getDownloadRecord(String id) {
        if (mDownloadService != null) {
            return mDownloadService.getDownloadRecord(id);
        }
        return null;
    }

    private DownloadConnectedListener mConnectedListener;

    public void setOnConnectedListener(DownloadConnectedListener listener) {
        this.mConnectedListener = listener;
    }
}
