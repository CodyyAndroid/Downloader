package com.codyy.download.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.codyy.download.db.DownloadDao;
import com.codyy.download.db.DownloadDaoImpl;
import com.codyy.download.entity.DownloadEntity;
import com.codyy.download.threadpool.ThreadPoolType;
import com.codyy.download.threadpool.ThreadPoolUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by lijian on 2017/6/7.
 */

public class DownloadService extends Service implements Handler.Callback {
    private Handler mHandler;
    private Map<String, DownThread> mDownThreadMap = new HashMap<>();
    private Map<String, DownLoadListener> mDownLoadListeners = new HashMap<>();
    private ThreadPoolUtils mThreadPoolUtils = new ThreadPoolUtils(ThreadPoolType.SINGLE_THREAD, 1);
    private DownloadDao mDownloadDao;
    private volatile static long sRates = 0;
    private Timer mTimer;
    private DownloadRateListener mRateListener;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(this);
        mDownloadDao = DownloadDaoImpl.getInstance(this);
//        startAll();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        pauseAll();
        if (mTimer != null) {
            mTimer.cancel();
        }
        if (mDownloadDao != null) {
            mDownloadDao.closeDB();
        }
        mHandler = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new DownloadBinder();
    }

    public class DownloadBinder extends Binder {
        public DownloadService getDownloadService() {
            return DownloadService.this;
        }
    }

    /**
     * 开始所有下载任务(已完成和已删除的除外)
     */
    public void startAll() {
        for (DownloadEntity entity : mDownloadDao.queryDoingOn()) {
            download(entity.getUrl(), entity.getSavePath(), entity.getName(), entity.getThumbnails());
        }
    }

    /**
     * 执行下载
     *
     * @param downloadUrl 下载地址
     * @param path        保存路径(默认地址为Download)
     * @param fileName    文件名称(默认为下载连接截取名称)
     * @param thumbnails  缩略图地址
     */
    public void download(@NonNull String downloadUrl, String path, String fileName, String thumbnails) {
        String target = getSavePath(downloadUrl, path, fileName);
        if (!mDownloadDao.isExist(downloadUrl)) {
            mDownloadDao.save(new DownloadEntity(0, 0, downloadUrl, target, TextUtils.isEmpty(fileName) ? downloadUrl.substring(downloadUrl.lastIndexOf(File.separator) + 1) : fileName, DownloadFlag.WAITING, TextUtils.isEmpty(thumbnails) ? "" : thumbnails));
        } else {
            DownloadEntity entity = mDownloadDao.query(downloadUrl);
            if (entity.getStatus() == DownloadFlag.COMPLETED) {
                return;
            } else {
                mDownloadDao.updateProgress(downloadUrl, entity.getCurrent(), entity.getTotal(), DownloadFlag.WAITING);
            }
        }
        sendPauseOrWaitingMessage(DownloadFlag.WAITING, downloadUrl);
        DownThread thread = new DownThread(downloadUrl, target);
        mDownThreadMap.put(downloadUrl, thread);
        mThreadPoolUtils.execute(thread);
    }

    private String getSavePath(@NonNull String downloadUrl, String path, String fileName) {
        String name;
        if (TextUtils.isEmpty(fileName)) {
            name = downloadUrl.substring(downloadUrl.lastIndexOf(File.separator) + 1);
        } else {
            name = fileName + downloadUrl.substring(downloadUrl.lastIndexOf("."));
        }
        if (TextUtils.isEmpty(path)) {
            return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name).getAbsolutePath();
        } else {
            return path.endsWith(File.separator) ? path + name : path + File.separator + name;
        }
    }

    /**
     * 接收下载状态
     *
     * @param downloadUrl  下载地址
     * @param loadListener 监听
     */
    public void receiveDownloadStatus(@NonNull String downloadUrl, @NonNull DownLoadListener loadListener) {
        mDownLoadListeners.put(downloadUrl, loadListener);
    }

    public void addRateListener(DownloadRateListener rateListener) {
        mRateListener = rateListener;
    }

    public void removeRateListener() {
        mRateListener = null;
        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    public void receiveDownloadRate(DownloadRateListener rateListener) {
        addRateListener(rateListener);
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mRateListener != null) {
                    List<DownloadEntity> downloadEntities = new ArrayList<>();
                    for (DownloadEntity entity : mDownloadDao.queryDoingOn()) {
                        if (DownloadFlag.COMPLETED != entity.getStatus() && DownloadFlag.DELETED != entity.getStatus()) {
                            downloadEntities.add(entity);
                        }
                    }
                    sendRatingMessage(downloadEntities);
                }
                sRates = 0;
            }
        }, 0L, 1000L);
    }

    private void sendRatingMessage(List<DownloadEntity> downloadEntities) {
        Message message = new Message();
        message.what = DownloadFlag.RATING;
        Bundle bundle = new Bundle();
        bundle.putString("rate", DownloadStatus.formatRate(sRates));
        bundle.putInt("count", downloadEntities.size());
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    /**
     * 删除所有下载记录
     */
    public void deleteAll() {
        for (DownThread thread : mDownThreadMap.values()) {
            thread.pause();
        }
        mDownloadDao.deleteAll();
        for (String key : mDownThreadMap.keySet()) {
            if (mDownLoadListeners.containsKey(key)) {
                mDownLoadListeners.remove(key);
            }
//            sendDeleteMessage(DownloadFlag.DELETED, key);
        }
        mDownThreadMap.clear();
    }

    /**
     * 删除下载记录
     *
     * @param isRetained 是否保留已下载的文件 true:保留;false:删除
     * @param urls
     */
    public void delete(boolean isRetained, @NonNull String... urls) {
        for (String url : urls) {
            if (mDownThreadMap.containsKey(url)) {
                mDownThreadMap.get(url).pause();
                mDownThreadMap.remove(url);
            }
            mDownloadDao.delete(url, isRetained);
            if (mDownLoadListeners.containsKey(url)) {
                mDownLoadListeners.remove(url);
            }
//            sendDeleteMessage(DownloadFlag.DELETED, url);
        }
    }

    /**
     * 删除下载记录(默认删除本地文件)
     *
     * @param urls
     */
    public void delete(@NonNull String... urls) {
        this.delete(false, urls);
    }

    /**
     * 暂停所有下载任务
     */
    public void pauseAll() {
        for (DownThread thread : mDownThreadMap.values()) {
            thread.pause();
        }
        for (String key : mDownThreadMap.keySet()) {
            sendPauseOrWaitingMessage(DownloadFlag.PAUSED, key);
        }
    }

    /**
     * 暂停下载
     *
     * @param urls 地址
     */
    public void pause(@NonNull String... urls) {
        for (String url : urls) {
            if (mDownThreadMap.containsKey(url)) {
                mDownThreadMap.get(url).pause();
            }
        }
    }

    /**
     * 获取所有下载记录
     *
     * @return 下载记录
     */
    public List<DownloadEntity> getTotalDownloadRecords() {
        return mDownloadDao.queryAll();
    }

    /**
     * 获取未完成的下载记录
     *
     * @return 未完成的下载记录
     */
    public List<DownloadEntity> getDownloadingRecords() {
        return mDownloadDao.queryDoingOn();
    }

    /**
     * 根据url获取下载记录
     */
    public DownloadEntity getDownloadRecord(String url) {
        return mDownloadDao.query(url);
    }

    /**
     * 文件下载线程
     */
    private class DownThread implements Runnable {
        private RandomAccessFile currentPart;
        int length;
        private HttpURLConnection conn;
        private InputStream inStream;
        private String downloadUrl;
        private String savePath;
        private DownloadEntity mDownloadEntity;
        private long totalSize;
        private volatile boolean isPaused;

        DownThread(@NonNull String url, @NonNull String target) {
            this.downloadUrl = url;
            this.savePath = target;
        }

        /**
         * 暂停下载
         */
        void pause() {
            isPaused = true;
            if (mDownloadStatus != null)
                mDownloadDao.updateProgress(downloadUrl, mDownloadStatus.getDownloadSize(), mDownloadStatus.getTotalSize(), DownloadFlag.PAUSED);
        }


        @Override
        public void run() {
            if (isPaused) {
                mDownloadDao.updateStatus(downloadUrl, DownloadFlag.PAUSED);
                if (mDownloadDao.isPaused(downloadUrl))
                    sendPauseOrWaitingMessage(DownloadFlag.PAUSED, downloadUrl);
                Log.d("Thread ", downloadUrl + " was paused");
                return;
            }
            if (!mDownloadDao.isExist(downloadUrl)) {//如果下载记录不存在或本地下载文件被删除,将重新开始下载
                start(0);
            } else {
                mDownloadEntity = mDownloadDao.query(downloadUrl);//获取下载记录
                if (mDownloadEntity != null) {
                    sendProgressMessage(new DownloadStatus(mDownloadEntity.getCurrent(), mDownloadEntity.getTotal()), downloadUrl);//记录不为空,发送当前下载进度消息
                    if (mDownloadEntity.getCurrent() == mDownloadEntity.getTotal() && mDownloadEntity.getStatus() == DownloadFlag.COMPLETED) {
                        sendStartOrCompleteMessage(DownloadFlag.COMPLETED, downloadUrl);//如果status=DownloadFlag.COMPLETED,发送文件下载完成消息
                    } else {
                        start(mDownloadEntity.getCurrent());//存在下载记录,并且未完成,则从当前位置开始下载
                    }
                }
            }
        }

        /**
         * 下载状态
         */
        private DownloadStatus mDownloadStatus;

        /**
         * 开始下载
         *
         * @param range 下载起始位置
         */
        private void start(long range) {
            try {
                URL url = new URL(downloadUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5 * 1000);
                conn.setReadTimeout(5 * 1000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "*/*");
                conn.setRequestProperty("Accept-Language", "zh-CN");
                conn.setRequestProperty("Charset", "UTF-8");
                conn.setRequestProperty("Range", "bytes=" + range + "-");
                totalSize = (range == 0 ? conn.getContentLength() : mDownloadEntity.getTotal());
                currentPart = new RandomAccessFile(savePath, "rwd");
                currentPart.setLength(totalSize);
                currentPart.close();
                if (conn.getResponseCode() == 206) {
                    currentPart = new RandomAccessFile(savePath, "rwd");
                    currentPart.seek(range);
                    sendStartOrCompleteMessage(DownloadFlag.NORMAL, downloadUrl);
                    inStream = conn.getInputStream();
                    byte[] buffer = new byte[4096];
                    int hasRead;
                    while (!mDownloadDao.isPaused(downloadUrl) && length < totalSize && (hasRead = inStream.read(buffer)) != -1) {
                        currentPart.write(buffer, 0, hasRead);
                        length += hasRead;
                        if (mRateListener != null) {
                            sRates += hasRead;
                        } else {
                            sRates = 0;
                        }
                        mDownloadStatus = range == 0 ? new DownloadStatus(length, totalSize) : new DownloadStatus((length + mDownloadEntity.getCurrent()), totalSize);
                        sendProgressMessage(mDownloadStatus, downloadUrl);
                        if (mDownloadStatus.getPercentNumber() >= 100 && mHandler != null) {
                            mDownloadDao.updateProgress(downloadUrl, mDownloadStatus.getDownloadSize(), mDownloadStatus.getTotalSize(), DownloadFlag.COMPLETED);
                            sendStartOrCompleteMessage(DownloadFlag.COMPLETED, downloadUrl);
                        }
                    }
                    if (mDownloadDao.isPaused(downloadUrl))
                        sendPauseOrWaitingMessage(DownloadFlag.PAUSED, downloadUrl);
                } else {
                    mDownloadDao.updateStatus(downloadUrl, DownloadFlag.FAILED);
                    sendFailureMessage(conn.getResponseCode(), downloadUrl);
                }
            } catch (Exception e) {
                e.printStackTrace();
                mDownloadDao.updateStatus(downloadUrl, DownloadFlag.ERROR);
                sendErrorMessage(e, downloadUrl);
            } finally {
                try {
                    if (currentPart != null)
                        currentPart.close();
                    if (inStream != null)
                        inStream.close();
                    if (conn != null)
                        conn.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                    mDownloadDao.updateStatus(downloadUrl, DownloadFlag.ERROR);
                    sendErrorMessage(e, downloadUrl);
                }

            }
        }
    }

    private void sendStartOrCompleteMessage(@DownloadFlag int status, String downloadUrl) {
        sendNormalMessage(status, downloadUrl);
    }

    private void sendPauseOrWaitingMessage(@DownloadFlag int status, String downloadUrl) {
        sendNormalMessage(status, downloadUrl);
    }

    private void sendDeleteMessage(@DownloadFlag int status, String downloadUrl) {
        sendNormalMessage(status, downloadUrl);
    }

    private void sendNormalMessage(@DownloadFlag int status, String downloadUrl) {
        Message message = new Message();
        message.setData(getBundle(downloadUrl));
        message.what = status;
        sendMessage(message);
    }

    private void sendProgressMessage(DownloadStatus status, String downloadUrl) {
        Message message = new Message();
        message.what = DownloadFlag.PROGRESS;
        message.obj = status;
        message.setData(getBundle(downloadUrl));
        sendMessage(message);
    }

    private void sendFailureMessage(int code, String downloadUrl) {
        Message message = new Message();
        message.what = DownloadFlag.FAILED;
        message.obj = code;
        message.setData(getBundle(downloadUrl));
        sendMessage(message);
    }

    private void sendErrorMessage(Exception e, String downloadUrl) {
        Message message = new Message();
        message.what = DownloadFlag.ERROR;
        message.obj = e;
        message.setData(getBundle(downloadUrl));
        sendMessage(message);
    }

    private void sendMessage(Message message) {
        if (mHandler != null) {
            mHandler.sendMessage(message);
        }
    }

    private Bundle getBundle(String downloadUrl) {
        Bundle bundle = new Bundle();
        bundle.putString("url", downloadUrl);
        return bundle;
    }

    @Override
    public boolean handleMessage(Message msg) {
        DownLoadListener downLoadListener = mDownLoadListeners.get(msg.getData().getString("url"));
        switch (msg.what) {
            case DownloadFlag.NORMAL:
                if (downLoadListener != null) {
                    downLoadListener.onStart();
                }
                break;
            case DownloadFlag.WAITING:
                if (downLoadListener != null) {
                    downLoadListener.onWaiting();
                }
                break;
            case DownloadFlag.PROGRESS:
                if (downLoadListener != null) {
                    downLoadListener.onProgress((DownloadStatus) msg.obj);
                }
                break;
            case DownloadFlag.PAUSED:
                if (downLoadListener != null) {
                    downLoadListener.onPause();
                }
                break;
            case DownloadFlag.COMPLETED:
                if (downLoadListener != null) {
                    downLoadListener.onComplete();
                }
                if (mRateListener != null) {
                    DownloadEntity downloadEntity = mDownloadDao.query(msg.getData().getString("url"));
                    if (downloadEntity != null)
                        mRateListener.onComplete(downloadEntity);
                }
                if (mDownLoadListeners.containsKey(msg.getData().getString("url"))) {
                    mDownLoadListeners.remove(msg.getData().getString("url"));
                }
                if (mDownThreadMap.containsKey(msg.getData().getString("url"))) {
                    mDownThreadMap.remove(msg.getData().getString("url"));
                }
                break;
            case DownloadFlag.FAILED:
                if (downLoadListener != null) {
                    downLoadListener.onFailure((Integer) msg.obj);
                }
                break;
            case DownloadFlag.ERROR:
                if (downLoadListener != null) {
                    downLoadListener.onError((Exception) msg.obj);
                }
                break;
            case DownloadFlag.DELETED:
                if (downLoadListener != null) {
                    downLoadListener.onDelete();
                }
                break;
            case DownloadFlag.RATING:
                if (mRateListener != null) {
                    mRateListener.onRate(msg.getData().getString("rate"), msg.getData().getInt("count", 0));
                }
                break;
        }
        return true;
    }

}
