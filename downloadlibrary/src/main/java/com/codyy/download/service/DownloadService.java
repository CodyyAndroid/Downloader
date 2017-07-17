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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
            if (!mDownThreadMap.containsKey(entity.getUrl()))
                download(entity.getUrl(), entity.getSavePath(), entity.getName(), entity.getThumbnails());
        }
    }

    /**
     * 执行下载
     *
     * @param downloadUrl 下载地址
     * @param path        保存路径(默认地址为Download)
     * @param title       文件标题
     * @param thumbnails  缩略图地址
     */
    public void download(@NonNull String downloadUrl, String path, String title, String thumbnails) {
        this.download(downloadUrl, path, title, thumbnails, null);
    }

    public void download(@NonNull String downloadUrl, String path, String title, String thumbnails, String extra1) {
        this.download(downloadUrl, path, title, thumbnails, extra1, null);
    }

    public void download(@NonNull String downloadUrl, String path, String title, String thumbnails, String extra1, String extra2) {
        String target = getSavePath(downloadUrl, path);
        if (!mDownloadDao.isExist(downloadUrl)) {
            mDownloadDao.save(new DownloadEntity(0, 0, downloadUrl, target, TextUtils.isEmpty(title) ? downloadUrl.substring(downloadUrl.lastIndexOf(File.separator) + 1) : title, DownloadFlag.WAITING, TextUtils.isEmpty(thumbnails) ? "" : thumbnails, System.currentTimeMillis(), extra1, extra2));
        } else {
            DownloadEntity entity = mDownloadDao.query(downloadUrl);
            if (entity.getStatus() == DownloadFlag.COMPLETED) {
                return;
            } else {
                mDownloadDao.updateProgress(downloadUrl, entity.getCurrent(), entity.getTotal(), DownloadFlag.WAITING);
            }
        }
        sendPauseOrWaitingMessage(DownloadFlag.WAITING, downloadUrl);
        networkType(downloadUrl, target);

    }

    private void networkType(@NonNull String downloadUrl, String target) {
        switch (getNetworkType(getApplicationContext())) {
            case NETWORK_2G:
                if (isHoneyCombDownload) {
                    startDownloadTask(downloadUrl, target);
                }
                break;
            case NETWORK_3G:
                if (isHoneyCombDownload) {
                    startDownloadTask(downloadUrl, target);
                }
                break;
            case NETWORK_4G:
                if (isHoneyCombDownload) {
                    startDownloadTask(downloadUrl, target);
                }
                break;
            case NETWORK_WIFI:
                startDownloadTask(downloadUrl, target);
                break;
            default:
                startDownloadTask(downloadUrl, target);
                break;
        }
    }

    private void startDownloadTask(@NonNull String downloadUrl, String target) {
        DownThread thread = new DownThread(downloadUrl, target);
        mDownThreadMap.put(downloadUrl, thread);
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
     * @param downloadUrl  下载地址
     * @param loadListener 监听
     */
    public void receiveDownloadStatus(@NonNull String downloadUrl, @NonNull DownLoadListener loadListener) {
        mDownLoadListeners.put(downloadUrl, loadListener);
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
        mDownThreadMap.clear();
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
                mDownThreadMap.remove(url);
                sendPauseOrWaitingMessage(DownloadFlag.PAUSED, url);
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
                Cog.d(TAG, "Thread" + downloadUrl + " was paused");
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
                    mDownloadDao.updatePath(downloadUrl, savePath);
                }
                currentPart = new RandomAccessFile(savePath, DownloadExtra.RANDOM_ACCESS_FILE_MODE);
                currentPart.setLength(totalSize);
                currentPart.close();
                if (conn.getResponseCode() == 206) {
                    currentPart = new RandomAccessFile(savePath, DownloadExtra.RANDOM_ACCESS_FILE_MODE);
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
        bundle.putString(DownloadExtra.EXTRA_URL, downloadUrl);
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
        DownLoadListener downLoadListener = mDownLoadListeners.get(msg.getData().getString(DownloadExtra.EXTRA_URL));
        switch (msg.what) {
            case DownloadFlag.NORMAL:
                if (downLoadListener != null) {
                    Cog.d(TAG, "Download Start " + msg.getData().getString(DownloadExtra.EXTRA_URL));
                    downLoadListener.onStart();
                }
                break;
            case DownloadFlag.WAITING:
                if (downLoadListener != null) {
                    Cog.d(TAG, "Download Waiting" + msg.getData().getString(DownloadExtra.EXTRA_URL));
                    downLoadListener.onWaiting();
                }
                break;
            case DownloadFlag.PROGRESS:
                if (downLoadListener != null) {
                    Cog.d(TAG, "Download Progress " + ((DownloadStatus) msg.obj).getPercent() + " url:" + msg.getData().getString(DownloadExtra.EXTRA_URL));
                    downLoadListener.onProgress((DownloadStatus) msg.obj);
                }
                break;
            case DownloadFlag.PAUSED:
                if (downLoadListener != null) {
                    Cog.d(TAG, "Download Pause" + msg.getData().getString(DownloadExtra.EXTRA_URL));
                    downLoadListener.onPause();
                }
                break;
            case DownloadFlag.COMPLETED:
                if (downLoadListener != null) {
                    Cog.d(TAG, "Download Complete" + msg.getData().getString(DownloadExtra.EXTRA_URL));
                    downLoadListener.onComplete();
                }
                if (mRateListener != null) {
                    DownloadEntity downloadEntity = mDownloadDao.query(msg.getData().getString(DownloadExtra.EXTRA_URL));
                    if (downloadEntity != null)
                        mRateListener.onComplete(downloadEntity);
                }
                if (mDownLoadListeners.containsKey(msg.getData().getString(DownloadExtra.EXTRA_URL))) {
                    mDownLoadListeners.remove(msg.getData().getString(DownloadExtra.EXTRA_URL));
                }
                if (mDownThreadMap.containsKey(msg.getData().getString(DownloadExtra.EXTRA_URL))) {
                    mDownThreadMap.remove(msg.getData().getString(DownloadExtra.EXTRA_URL));
                }
                break;
            case DownloadFlag.FAILED:
                if (downLoadListener != null) {
                    Cog.e(TAG, "Download Failure" + msg.getData().getString(DownloadExtra.EXTRA_URL));
                    downLoadListener.onFailure((Integer) msg.obj);
                }
                break;
            case DownloadFlag.ERROR:
                if (downLoadListener != null) {
                    Cog.e(TAG, "Download Error" + msg.getData().getString(DownloadExtra.EXTRA_URL));
                    downLoadListener.onError((Exception) msg.obj);
                }
                break;
            case DownloadFlag.DELETED:
                if (downLoadListener != null) {
                    Cog.d(TAG, "Download Deleted" + msg.getData().getString(DownloadExtra.EXTRA_URL));
                    downLoadListener.onDelete();
                }
                break;
            case DownloadFlag.RATING:
                if (mRateListener != null) {
//                    Cog.d(TAG, "Download Rating" + msg.getData().getString(DownloadExtra.EXTRA_URL) + msg.getData().getString(DownloadExtra.EXTRA_RATE) + msg.getData().getInt(DownloadExtra.EXTRA_COUNT, 0));
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
