package com.codyy.download.db;

import com.codyy.download.entity.DownloadEntity;
import com.codyy.download.service.DownloadFlag;

import java.util.List;

/**
 * Created by lijian on 2017/6/7.
 */

public interface DownloadDao {
    /**
     * 检查本地下载记录,是否下载过
     */
    boolean isExist(String url);

    /**
     * 检查本地数据是否暂停下载状态
     */
    boolean isPaused(String url);

    /**
     * 保存下载的具体信息
     *
     * @param entry 具体信息
     */
    boolean save(DownloadEntity entry);

    /**
     * 根据url查询具体下载信息
     */
    DownloadEntity query(String url);

    /**
     * 查询所有下载集合
     */
    List<DownloadEntity> queryAll();

    /**
     * 查询未完成的下载任务
     */
    List<DownloadEntity> queryDoingOn();

    /**
     * 更新下载状态
     */
    boolean updateProgress(String url, long downloadSize, long totalSize, @DownloadFlag int status);

    /**
     * 更新下载状态
     */
    boolean updateStatus(String url, @DownloadFlag int status);

    /**
     * 删除下载记录
     */
    boolean delete(String url, boolean isRetained);

    /**
     * 删除所有下载记录
     */
    void deleteAll();

    /**
     * 关闭数据库
     */
    void closeDB();
}
