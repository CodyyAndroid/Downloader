package com.codyy.download.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.codyy.download.Downloader;
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
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.codyy.download.service.NetworkType.NETWORK_2G;
import static com.codyy.download.service.NetworkType.NETWORK_3G;
import static com.codyy.download.service.NetworkType.NETWORK_4G;
import static com.codyy.download.service.NetworkType.NETWORK_WIFI;

/**
 * Download Service
 * Created by lijian on 2017/6/7.
 */

public class DownloadService extends Service implements Handler.Callback {
    private static final String TAG = "DownloadService";
    private Handler mHandler;
    private Map<String, DownThread> mDownThreadMap = new HashMap<>();
    private Map<String, DownLoadListener> mDownLoadListeners = new HashMap<>();
    private ThreadPoolUtils mThreadPoolUtils = new ThreadPoolUtils(ThreadPoolType.SINGLE_THREAD, 1);
    private DownloadDao mDownloadDao;
    private volatile static long sRates = 0;
    private Timer mTimer;
    private DownloadRateListener mRateListener;
    private DownloadIsPauseAllListener mIsPauseAllListener;

    /**
     * wifi状态是否自动下载,默认为true
     */
    private boolean isWifiDownload = true;
    /**
     * 蜂窝数据状态是否自动下载,默认为false
     */
    private boolean isHoneyCombDownload = false;
    private NetReceiver mNetReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Cog.d(TAG, "DownloadService onCreate");
        mNetReceiver = new NetReceiver();
        registerReceiver(mNetReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));//开启网络状态变化监测
        Cog.d(TAG, "Network State Receiver register");
        mHandler = new Handler(this);
        mDownloadDao = DownloadDaoImpl.getInstance(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mNetReceiver);
        Cog.d(TAG, "Network State Receiver register");
        Cog.d(TAG, "Stop All Download Tasks");
        pauseAll();
        if (mTimer != null) {
            mTimer.cancel();
        }
        if (mDownloadDao != null) {
            mDownloadDao.closeDB();
        }
        mHandler = null;
        Cog.d(TAG, "DownloadService onDestroy");
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
            if (!mDownThreadMap.containsKey(entity.getId()))
                download(entity);
        }
    }

    /**
     * 同步下载记录
     */
    public boolean syncDownloadRecord(@NonNull DownloadEntity entity) {
        return !mDownloadDao.isExist(entity.getId()) && mDownloadDao.save(new DownloadEntity(TextUtils.isEmpty(entity.getId()) ? entity.getUrl() : entity.getId(), entity.getCurrent(), entity.getTotal(), entity.getUrl(), entity.getSavePath(), TextUtils.isEmpty(entity.getName()) ? entity.getUrl().substring(entity.getUrl().lastIndexOf(File.separator) + 1) : entity.getName(), DownloadFlag.COMPLETED, TextUtils.isEmpty(entity.getThumbnails()) ? "" : entity.getThumbnails(), entity.getTime() == 0 ? System.currentTimeMillis() : entity.getTime(), entity.getExtra1(), entity.getExtra2()));
    }

    public void download(@NonNull DownloadEntity entity) {
        String target = getSavePath(entity.getUrl(), entity.getSavePath());
        if (!mDownloadDao.isExist(entity.getId())) {
            mDownloadDao.save(new DownloadEntity(TextUtils.isEmpty(entity.getId()) ? entity.getUrl() : entity.getId(), 0, 0, entity.getUrl(), target, TextUtils.isEmpty(entity.getName()) ? entity.getUrl().substring(entity.getUrl().lastIndexOf(File.separator) + 1) : entity.getName(), DownloadFlag.WAITING, TextUtils.isEmpty(entity.getThumbnails()) ? "" : entity.getThumbnails(), System.currentTimeMillis(), entity.getExtra1(), entity.getExtra2()));
        } else {
            DownloadEntity downloadEntity = mDownloadDao.query(entity.getId());
            if (downloadEntity.getStatus() == DownloadFlag.COMPLETED) {
                return;
            } else {
                mDownloadDao.updateProgress(entity.getId(), downloadEntity.getCurrent(), downloadEntity.getTotal(), DownloadFlag.WAITING);
            }
        }
        sendPauseOrWaitingMessage(DownloadFlag.WAITING, entity.getId());
        networkType(entity.getId(), target, entity.getUrl());
    }

    private void networkType(@NonNull String id, String target, String url) {
        switch (getNetworkType(getApplicationContext())) {
            case NETWORK_2G:
                if (isHoneyCombDownload) {
                    startDownloadTask(id, target, url);
                }
                break;
            case NETWORK_3G:
                if (isHoneyCombDownload) {
                    startDownloadTask(id, target, url);
                }
                break;
            case NETWORK_4G:
                if (isHoneyCombDownload) {
                    startDownloadTask(id, target, url);
                }
                break;
            case NETWORK_WIFI:
                startDownloadTask(id, target, url);
                break;
            default:
                startDownloadTask(id, target, url);
                break;
        }
    }

    private void startDownloadTask(@NonNull String id, String target, String url) {
        DownThread thread = new DownThread(id, target, url);
        mDownThreadMap.put(id, thread);
        mThreadPoolUtils.execute(thread);
    }

    private String getSavePath(@NonNull String downloadUrl, String path) {
        String name = downloadUrl.substring(downloadUrl.lastIndexOf(File.separator) + 1);
        if (TextUtils.isEmpty(path)) {
            return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name).getAbsolutePath();
        } else {
            return path;
        }
    }

    /**
     * 接收下载状态
     *
     * @param id           下载地址
     * @param loadListener 监听
     */
    public void receiveDownloadStatus(@NonNull String id, @NonNull DownLoadListener loadListener) {
        mDownLoadListeners.put(id, loadListener);
    }

    /**
     * 增加下载速率监听
     *
     * @param rateListener {@link DownloadRateListener}
     */
    public void addRateListener(DownloadRateListener rateListener) {
        mRateListener = rateListener;
    }

    public void addIsPauseAllListener(DownloadIsPauseAllListener listener) {
        mIsPauseAllListener = listener;
    }

    public void removeIsPauseAllListener() {
        mIsPauseAllListener = null;
    }

    /**
     * 移除速率监听
     */
    public void removeRateListener() {
        mRateListener = null;
        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    /**
     * 接收下载速率及任务完成监听
     *
     * @param rateListener {@link DownloadRateListener}
     */
    public void receiveDownloadRate(DownloadRateListener rateListener) {
        addRateListener(rateListener);
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mRateListener != null) {
                    List<DownloadEntity> downloadEntities = new ArrayList<>();
                    for (DownloadEntity entity : mDownloadDao.queryDoingOn()) {
                        downloadEntities.add(entity);
                    }
                    sendRatingMessage(downloadEntities);
                }
                sRates = 0;
            }
        }, 0L, 1000L);
    }

    /**
     * 发送下载速率Messages
     *
     * @param downloadEntities 下载完成的数据
     */
    private void sendRatingMessage(List<DownloadEntity> downloadEntities) {
        Message message = new Message();
        message.what = DownloadFlag.RATING;
        Bundle bundle = new Bundle();
        bundle.putString(DownloadExtra.EXTRA_RATE, DownloadStatus.formatRate(sRates));
        bundle.putInt(DownloadExtra.EXTRA_COUNT, downloadEntities.size());
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
     * @param ids
     */
    public void delete(boolean isRetained, @NonNull String... ids) {
        for (String id : ids) {
            if (mDownThreadMap.containsKey(id)) {
                mDownThreadMap.get(id).pause();
                mDownThreadMap.remove(id);
            }
            mDownloadDao.delete(id, isRetained);
            if (mDownLoadListeners.containsKey(id)) {
                mDownLoadListeners.remove(id);
            }
//            sendDeleteMessage(DownloadFlag.DELETED, url);
        }
    }

    /**
     * 删除下载记录(默认删除本地文件)
     *
     * @param ids
     */
    public void delete(@NonNull String... ids) {
        this.delete(false, ids);
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
        mDownThreadMap.clear();
        for (DownloadEntity entity : mDownloadDao.queryDoingOn()) {
            mDownloadDao.updateStatus(entity.getId(), DownloadFlag.PAUSED);
        }
    }

    /**
     * 暂停下载
     *
     * @param ids 地址
     */
    public void pause(@NonNull String... ids) {
        for (String id : ids) {
            if (mDownThreadMap.containsKey(id)) {
                mDownThreadMap.get(id).pause();
                mDownThreadMap.remove(id);
            }
            mDownloadDao.updateStatus(id, DownloadFlag.PAUSED);
            sendPauseOrWaitingMessage(DownloadFlag.PAUSED, id);
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
    public DownloadEntity getDownloadRecord(String id) {
        return mDownloadDao.query(id);
    }

    /**
     * 文件下载线程
     */
    private class DownThread implements Runnable {
        private RandomAccessFile currentPart;
        int length;
        private HttpURLConnection conn;
        private InputStream inStream;
        private String id;
        private String downloadUrl;
        private String savePath;
        private DownloadEntity mDownloadEntity;
        private long totalSize;
        private volatile boolean isPaused;

        DownThread(@NonNull String id, @NonNull String target, String url) {
            this.id = id;
            this.savePath = target;
            this.downloadUrl = url;
        }

        /**
         * 暂停下载
         */
        void pause() {
            isPaused = true;
            if (mDownloadStatus != null)
                mDownloadDao.updateProgress(id, mDownloadStatus.getDownloadSize(), mDownloadStatus.getTotalSize(), DownloadFlag.PAUSED);
        }


        @Override
        public void run() {
            if (isPaused) {
                mDownloadDao.updateStatus(id, DownloadFlag.PAUSED);
                if (mDownloadDao.isPaused(id))
                    sendPauseOrWaitingMessage(DownloadFlag.PAUSED, id);
                Cog.d(TAG, "Thread" + id + " was paused");
                return;
            }
            if (!mDownloadDao.isExist(id)) {//如果下载记录不存在或本地下载文件被删除,将重新开始下载
                start(0);
            } else {
                mDownloadEntity = mDownloadDao.query(id);//获取下载记录
                if (mDownloadEntity != null) {
                    sendProgressMessage(new DownloadStatus(mDownloadEntity.getCurrent(), mDownloadEntity.getTotal()), id);//记录不为空,发送当前下载进度消息
                    if (mDownloadEntity.getCurrent() == mDownloadEntity.getTotal() && mDownloadEntity.getStatus() == DownloadFlag.COMPLETED) {
                        sendStartOrCompleteMessage(DownloadFlag.COMPLETED, id);//如果status=DownloadFlag.COMPLETED,发送文件下载完成消息
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
                conn.setConnectTimeout(DownloadExtra.TIME_OUT);
                conn.setReadTimeout(DownloadExtra.TIME_OUT);
                conn.setRequestMethod(DownloadExtra.REQUEST_METHOD);
                conn.setRequestProperty(DownloadExtra.REQUEST_PROPERTY_KEY1, DownloadExtra.REQUEST_PROPERTY_VALUE1);
                conn.setRequestProperty(DownloadExtra.REQUEST_PROPERTY_KEY2, DownloadExtra.REQUEST_PROPERTY_VALUE2);
                conn.setRequestProperty(DownloadExtra.REQUEST_PROPERTY_KEY3, DownloadExtra.REQUEST_PROPERTY_VALUE3);
                conn.setRequestProperty(DownloadExtra.REQUEST_PROPERTY_KEY4, String.format(Locale.getDefault(), "bytes=%d-", range));
                totalSize = (range == 0 ? conn.getContentLength() : mDownloadEntity.getTotal());
                if (savePath.endsWith(".do")) {
                    new File(savePath).delete();
                    String contentDisposition = new String(conn.getHeaderField("Content-Disposition").getBytes("UTF-8"), "UTF-8");
                    String filename = contentDisposition.substring(contentDisposition.indexOf("=") + 1);
//                    Cog.e("filename", filename.trim());
                    savePath = savePath.replace(".do", filename);
                    mDownloadDao.updatePath(id, savePath);
                }
                if (totalSize > getAvailableStore()) {
                    Cog.e(TAG, "存储空间不足,total=" + totalSize + " availableStore=" + getAvailableStore());
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(Downloader.ACTION_DOWNLOAD_OUT_OF_MEMORY));
                    throw new IOException("存储空间不足");
                } else {
                    currentPart = new RandomAccessFile(savePath, DownloadExtra.RANDOM_ACCESS_FILE_MODE);
                    currentPart.setLength(totalSize);
                    currentPart.close();
                    if (conn.getResponseCode() == 206) {
                        currentPart = new RandomAccessFile(savePath, DownloadExtra.RANDOM_ACCESS_FILE_MODE);
                        currentPart.seek(range);
                        sendStartOrCompleteMessage(DownloadFlag.NORMAL, id);
                        inStream = conn.getInputStream();
                        byte[] buffer = new byte[4096];
                        int hasRead;
                        while (!mDownloadDao.isPaused(id) && length < totalSize && (hasRead = inStream.read(buffer)) != -1) {
                            currentPart.write(buffer, 0, hasRead);
                            length += hasRead;
                            if (mRateListener != null) {
                                sRates += hasRead;
                            } else {
                                sRates = 0;
                            }
                            mDownloadStatus = range == 0 ? new DownloadStatus(length, totalSize) : new DownloadStatus((length + mDownloadEntity.getCurrent()), totalSize);
                            sendProgressMessage(mDownloadStatus, id);
                            if (mDownloadStatus.getPercentNumber() >= 100 && mHandler != null) {
                                mDownloadDao.updateProgress(id, mDownloadStatus.getDownloadSize(), mDownloadStatus.getTotalSize(), DownloadFlag.COMPLETED);
                                sendStartOrCompleteMessage(DownloadFlag.COMPLETED, id);
                            }
                        }
                        if (mDownloadDao.isPaused(id))
                            sendPauseOrWaitingMessage(DownloadFlag.PAUSED, id);
                    } else {
                        mDownloadDao.updateStatus(id, DownloadFlag.FAILED);
                        sendFailureMessage(conn.getResponseCode(), id);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                mDownloadDao.updateStatus(id, DownloadFlag.ERROR);
                sendErrorMessage(e, id);
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
                    mDownloadDao.updateStatus(id, DownloadFlag.ERROR);
                    sendErrorMessage(e, id);
                }

            }
        }
    }

    // 获取SD卡路径
    private static String getExternalStoragePath() {
        // 获取SdCard状态
        String state = android.os.Environment.getExternalStorageState();
        // 判断SdCard是否存在并且是可用的
        if (android.os.Environment.MEDIA_MOUNTED.equals(state)) {
            if (android.os.Environment.getExternalStorageDirectory().canWrite()) {
                return android.os.Environment.getExternalStorageDirectory()
                        .getPath();
            }
        }
        return null;
    }

    private static long getAvailableStore() {
        String path = getExternalStoragePath();
        if (path == null) return -1;
        // 取得sdcard文件路径
        StatFs statFs = new StatFs(path);
        // 获取block的SIZE
        long blocSize = statFs.getBlockSize();
        // 获取BLOCK数量
        // long totalBlocks = statFs.getBlockCount();
        // 可使用的Block的数量
        long availaBlock = statFs.getAvailableBlocks();
        // long total = totalBlocks * blocSize;
        return availaBlock * blocSize;
    }

    private void sendStartOrCompleteMessage(@DownloadFlag int status, String id) {
        sendNormalMessage(status, id);
    }

    private void sendPauseOrWaitingMessage(@DownloadFlag int status, String id) {
        sendNormalMessage(status, id);
    }

    private void sendDeleteMessage(@DownloadFlag int status, String id) {
        sendNormalMessage(status, id);
    }

    private void sendNormalMessage(@DownloadFlag int status, String id) {
        Message message = new Message();
        message.setData(getBundle(id));
        message.what = status;
        sendMessage(message);
    }

    private void sendProgressMessage(DownloadStatus status, String id) {
        Message message = new Message();
        message.what = DownloadFlag.PROGRESS;
        message.obj = status;
        message.setData(getBundle(id));
        sendMessage(message);
    }

    private void sendFailureMessage(int code, String id) {
        Message message = new Message();
        message.what = DownloadFlag.FAILED;
        message.obj = code;
        message.setData(getBundle(id));
        sendMessage(message);
    }

    private void sendErrorMessage(Exception e, String id) {
        Message message = new Message();
        message.what = DownloadFlag.ERROR;
        message.obj = e;
        message.setData(getBundle(id));
        sendMessage(message);
    }

    private void sendMessage(Message message) {
        if (mHandler != null) {
            mHandler.sendMessage(message);
        }
    }

    private Bundle getBundle(String id) {
        Bundle bundle = new Bundle();
        bundle.putString(DownloadExtra.EXTRA_ID, id);
        return bundle;
    }

    @Override
    public boolean handleMessage(Message msg) {
        boolean isPauseAll = true;
        for (DownloadEntity entity : mDownloadDao.queryDoingOn()) {
            if (DownloadFlag.PAUSED != entity.getStatus()) {
                isPauseAll = false;
                break;
            }
        }
        if (mIsPauseAllListener != null) mIsPauseAllListener.isPauseAll(isPauseAll);
        DownLoadListener downLoadListener = mDownLoadListeners.get(msg.getData().getString(DownloadExtra.EXTRA_ID));
        switch (msg.what) {
            case DownloadFlag.NORMAL:
                Cog.d(TAG, "Download Start " + msg.getData().getString(DownloadExtra.EXTRA_ID));
                if (downLoadListener != null) {
                    downLoadListener.onStart();
                }
                break;
            case DownloadFlag.WAITING:
                Cog.d(TAG, "Download Waiting" + msg.getData().getString(DownloadExtra.EXTRA_ID));
                if (downLoadListener != null) {
                    downLoadListener.onWaiting();
                }
                break;
            case DownloadFlag.PROGRESS:
                Cog.d(TAG, "Download Progress " + ((DownloadStatus) msg.obj).getPercent() + " id:" + msg.getData().getString(DownloadExtra.EXTRA_ID));
                if (downLoadListener != null) {
                    downLoadListener.onProgress((DownloadStatus) msg.obj);
                }
                break;
            case DownloadFlag.PAUSED:
                Cog.d(TAG, "Download Pause" + msg.getData().getString(DownloadExtra.EXTRA_ID));
                if (downLoadListener != null) {
                    downLoadListener.onPause();
                }
                break;
            case DownloadFlag.COMPLETED:
                Cog.d(TAG, "Download Complete" + msg.getData().getString(DownloadExtra.EXTRA_ID));
                if (downLoadListener != null) {
                    downLoadListener.onComplete();
                }
                if (mRateListener != null) {
                    DownloadEntity downloadEntity = mDownloadDao.query(msg.getData().getString(DownloadExtra.EXTRA_ID));
                    if (downloadEntity != null)
                        mRateListener.onComplete(downloadEntity);
                }
                if (mDownLoadListeners.containsKey(msg.getData().getString(DownloadExtra.EXTRA_ID))) {
                    mDownLoadListeners.remove(msg.getData().getString(DownloadExtra.EXTRA_ID));
                }
                if (mDownThreadMap.containsKey(msg.getData().getString(DownloadExtra.EXTRA_ID))) {
                    mDownThreadMap.remove(msg.getData().getString(DownloadExtra.EXTRA_ID));
                }
                break;
            case DownloadFlag.FAILED:
                Cog.e(TAG, "Download Failure" + msg.getData().getString(DownloadExtra.EXTRA_ID));
                if (downLoadListener != null) {
                    downLoadListener.onFailure((Integer) msg.obj);
                }
                break;
            case DownloadFlag.ERROR:
                Cog.e(TAG, "Download Error" + msg.getData().getString(DownloadExtra.EXTRA_ID));
                if (downLoadListener != null) {
                    downLoadListener.onError((Exception) msg.obj);
                }
                break;
            case DownloadFlag.DELETED:
                Cog.d(TAG, "Download Deleted" + msg.getData().getString(DownloadExtra.EXTRA_ID));
                if (downLoadListener != null) {
                    downLoadListener.onDelete();
                }
                break;
            case DownloadFlag.RATING:
                if (mRateListener != null) {
//                    Cog.d(TAG, "Download Rating" + msg.getData().getString(DownloadExtra.EXTRA_ID) + msg.getData().getString(DownloadExtra.EXTRA_RATE) + msg.getData().getInt(DownloadExtra.EXTRA_COUNT, 0));
                    mRateListener.onRate(msg.getData().getString(DownloadExtra.EXTRA_RATE), msg.getData().getInt(DownloadExtra.EXTRA_COUNT, 0));
                }
                break;
        }
        return true;
    }

    private class NetReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction()) && Downloader.isBound()) {
                switch (DownloadService.getNetworkType(context)) {
                    case NETWORK_2G:
                        startToDownload(context, Downloader.getInstance(context).isHoneyCombDownload());
                        break;
                    case NETWORK_3G:
                        startToDownload(context, Downloader.getInstance(context).isHoneyCombDownload());
                        break;
                    case NETWORK_4G:
                        startToDownload(context, Downloader.getInstance(context).isHoneyCombDownload());
                        break;
                    case NETWORK_WIFI:
                        startToDownload(context, true);
                        break;
                    default:
                        startToDownload(context, false);
                        break;
                }
            }
        }

    }

    /**
     * 是否开始下载或暂停任务
     *
     * @param isStart true:开始下载未缓存完成任务;false:停止下载未缓存完成任务
     */
    private void startToDownload(Context context, boolean isStart) {
        if (isStart) {
            Downloader.getInstance(context).startAll();
        } else {
            Downloader.getInstance(context).pauseAll();
        }
    }

    private static final int NETWORK_TYPE_GSM = 16;
    private static final int NETWORK_TYPE_TD_SCDMA = 17;
    private static final int NETWORK_TYPE_IWLAN = 18;

    /**
     * 获取当前网络类型
     * <p>需添加权限 {@code <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>}</p>
     *
     * @return 网络类型
     * <ul>
     * <li>{@link NetworkType#NETWORK_WIFI   } </li>
     * <li>{@link NetworkType#NETWORK_4G     } </li>
     * <li>{@link NetworkType#NETWORK_3G     } </li>
     * <li>{@link NetworkType#NETWORK_2G     } </li>
     * <li>{@link NetworkType#NETWORK_UNKNOWN} </li>
     * <li>{@link NetworkType#NETWORK_NO     } </li>
     * </ul>
     */
    public static
    @NetworkType
    int getNetworkType(Context context) {
        int netType = NetworkType.NETWORK_NO;
        NetworkInfo info = getActiveNetworkInfo(context);
        if (info != null && info.isAvailable()) {

            if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                netType = NETWORK_WIFI;
            } else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                switch (info.getSubtype()) {

                    case NETWORK_TYPE_GSM:
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                    case TelephonyManager.NETWORK_TYPE_1xRTT:
                    case TelephonyManager.NETWORK_TYPE_IDEN:
                        netType = NETWORK_2G;
                        break;

                    case NETWORK_TYPE_TD_SCDMA:
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                    case TelephonyManager.NETWORK_TYPE_HSUPA:
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                    case TelephonyManager.NETWORK_TYPE_EVDO_B:
                    case TelephonyManager.NETWORK_TYPE_EHRPD:
                    case TelephonyManager.NETWORK_TYPE_HSPAP:
                        netType = NETWORK_3G;
                        break;

                    case NETWORK_TYPE_IWLAN:
                    case TelephonyManager.NETWORK_TYPE_LTE:
                        netType = NETWORK_4G;
                        break;
                    default:

                        String subtypeName = info.getSubtypeName();
                        if (subtypeName.equalsIgnoreCase("TD-SCDMA")
                                || subtypeName.equalsIgnoreCase("WCDMA")
                                || subtypeName.equalsIgnoreCase("CDMA2000")) {
                            netType = NETWORK_3G;
                        } else {
                            netType = NetworkType.NETWORK_UNKNOWN;
                        }
                        break;
                }
            } else {
                netType = NetworkType.NETWORK_UNKNOWN;
            }
        }
        return netType;
    }

    /**
     * 获取活动网络信息
     * <p>需添加权限 {@code <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>}</p>
     *
     * @return NetworkInfo
     */
    private static NetworkInfo getActiveNetworkInfo(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo();
    }

    public void setWifiDownload(boolean wifiDownload) {
        isWifiDownload = wifiDownload;
    }

    public void setHoneyCombDownload(boolean honeyCombDownload) {
        isHoneyCombDownload = honeyCombDownload;
    }

    public boolean isWifiDownload() {
        return isWifiDownload;
    }

    public boolean isHoneyCombDownload() {
        return isHoneyCombDownload;
    }

}
