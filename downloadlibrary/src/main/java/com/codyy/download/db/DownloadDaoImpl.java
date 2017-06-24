package com.codyy.download.db;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.codyy.download.entity.DownloadEntity;
import com.codyy.download.service.DBSelection;
import com.codyy.download.service.DownloadFlag;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库操作实现类
 * Created by lijian on 2017/6/7.
 */

public class DownloadDaoImpl implements DownloadDao {
    private volatile static DownloadDaoImpl mDownloadDaoImpl;

    private volatile static DownloadDbHelper mDbHelper;

    public static DownloadDaoImpl getInstance(Context context) {
        if (mDownloadDaoImpl == null) {
            synchronized (DownloadDaoImpl.class) {
                mDownloadDaoImpl = new DownloadDaoImpl();
                mDbHelper = new DownloadDbHelper(context);
            }
        }
        return mDownloadDaoImpl;
    }

    @Override
    public synchronized boolean isExist(String url) {
        String[] projection = {
                DownloadTable.COLUMN_NAME_DOWNLOAD_URL,
                DownloadTable.COLUMN_NAME_CURRENT_POSITION,
                DownloadTable.COLUMN_NAME_TOTAL_SIZE,
                DownloadTable.COLUMN_NAME_SAVE_PATH,
                DownloadTable.COLUMN_NAME_FILE_NAME,
                DownloadTable.COLUMN_NAME_STATUS,
                DownloadTable.COLUMN_NAME_THUMBNAILS
        };
        String selection = DownloadTable.COLUMN_NAME_DOWNLOAD_URL + DBSelection.SELECTION_EQUAL;
        String[] selectionArgs = {url};
        Cursor cursor = mDbHelper.getReadableDatabase().query(
                DownloadTable.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        int count = cursor.getCount();
        /*if (count > 0) {//如果数据库存在记录,但是文件已被删除,则从数据库删除记录
            cursor.moveToFirst();
            if (!new File(cursor.getString(cursor.getColumnIndexOrThrow(DownloadTable.COLUMN_NAME_SAVE_PATH))).exists()) {
                delete(url, false);
                count = 0;
            }
        }*/
        cursor.close();
        return count > 0;
    }

    @Override
    public synchronized boolean isPaused(String url) {
        boolean isPaused = false;
        String[] projection = {
                DownloadTable.COLUMN_NAME_DOWNLOAD_URL,
                DownloadTable.COLUMN_NAME_CURRENT_POSITION,
                DownloadTable.COLUMN_NAME_TOTAL_SIZE,
                DownloadTable.COLUMN_NAME_SAVE_PATH,
                DownloadTable.COLUMN_NAME_FILE_NAME,
                DownloadTable.COLUMN_NAME_STATUS,
                DownloadTable.COLUMN_NAME_THUMBNAILS
        };
        String selection = DownloadTable.COLUMN_NAME_DOWNLOAD_URL + DBSelection.SELECTION_EQUAL;
        String[] selectionArgs = {url};
        Cursor cursor = mDbHelper.getReadableDatabase().query(
                DownloadTable.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        while (cursor.moveToNext()) {
            if (Integer.parseInt(cursor.getString(cursor.getColumnIndexOrThrow(DownloadTable.COLUMN_NAME_STATUS))) == DownloadFlag.PAUSED) {
                isPaused = true;
            }
        }
        cursor.close();
        return isPaused;
    }

    @Override
    public synchronized boolean save(DownloadEntity entry) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            String sql = "insert into " + DownloadTable.TABLE_NAME + " (" + DownloadTable.COLUMN_NAME_DOWNLOAD_URL
                    + "," + DownloadTable.COLUMN_NAME_CURRENT_POSITION
                    + "," + DownloadTable.COLUMN_NAME_TOTAL_SIZE
                    + "," + DownloadTable.COLUMN_NAME_SAVE_PATH
                    + "," + DownloadTable.COLUMN_NAME_FILE_NAME
                    + "," + DownloadTable.COLUMN_NAME_STATUS
                    + "," + DownloadTable.COLUMN_NAME_THUMBNAILS
                    + ") values(?,?,?,?,?,?,?)";
            Object[] bindArgs = {entry.getUrl(), entry.getCurrent(), entry.getTotal(), entry.getSavePath(), entry.getName(), entry.getStatus(), entry.getThumbnails()};
            database.execSQL(sql, bindArgs);
            database.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            database.endTransaction();
        }
        return true;
        //add
       /* ContentValues values = new ContentValues();
        values.put(DownloadTable.COLUMN_NAME_DOWNLOAD_URL, entry.getUrl());
        values.put(DownloadTable.COLUMN_NAME_CURRENT_POSITION, entry.getCurrent() + "");
        values.put(DownloadTable.COLUMN_NAME_TOTAL_SIZE, entry.getTotal() + "");
        values.put(DownloadTable.COLUMN_NAME_SAVE_PATH, entry.getSavePath());
        values.put(DownloadTable.COLUMN_NAME_FILE_NAME, entry.getName());
        values.put(DownloadTable.COLUMN_NAME_STATUS, entry.getStatus() + "");
        long newRowId = mDbHelper.getWritableDatabase().insert(DownloadTable.TABLE_NAME, null, values);
        return newRowId != -1;*/
    }

    @Override
    public synchronized boolean updateProgress(String url, long downloadSize, long totalSize, @DownloadFlag int status) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            String sql = "update " + DownloadTable.TABLE_NAME + " set " + DownloadTable.COLUMN_NAME_CURRENT_POSITION
                    + "=?," + DownloadTable.COLUMN_NAME_TOTAL_SIZE
                    + "=?," + DownloadTable.COLUMN_NAME_STATUS + "=? where " + DownloadTable.COLUMN_NAME_DOWNLOAD_URL + DBSelection.SELECTION_EQUAL;
            Object[] bindArgs = {downloadSize, totalSize, status, url};
            database.execSQL(sql, bindArgs);
            database.setTransactionSuccessful();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            database.endTransaction();
        }
        return true;
       /* ContentValues values = new ContentValues(3);
        values.put(DownloadTable.COLUMN_NAME_CURRENT_POSITION, downloadSize + "");
        values.put(DownloadTable.COLUMN_NAME_TOTAL_SIZE, totalSize + "");
        values.put(DownloadTable.COLUMN_NAME_STATUS, status + "");
        String selection = DownloadTable.COLUMN_NAME_DOWNLOAD_URL + DBSelection.SELECTION_EQUAL;
        String[] selectionArgs = {url};
        int count = mDbHelper.getWritableDatabase().update(
                DownloadTable.TABLE_NAME,
                values,
                selection,
                selectionArgs
        );
        return count > 0;*/
    }

    @Override
    public synchronized boolean updateStatus(String url, @DownloadFlag int status) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            String sql = "update " + DownloadTable.TABLE_NAME + " set " + DownloadTable.COLUMN_NAME_STATUS + "=? where " + DownloadTable.COLUMN_NAME_DOWNLOAD_URL + DBSelection.SELECTION_EQUAL;
            Object[] bindArgs = {status, url};
            database.execSQL(sql, bindArgs);
            database.setTransactionSuccessful();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            database.endTransaction();
        }
        return true;
       /* ContentValues values = new ContentValues(1);
        values.put(DownloadTable.COLUMN_NAME_STATUS, status + "");
        String selection = DownloadTable.COLUMN_NAME_DOWNLOAD_URL + DBSelection.SELECTION_EQUAL;
        String[] selectionArgs = {url};
        int count = mDbHelper.getWritableDatabase().update(
                DownloadTable.TABLE_NAME,
                values,
                selection,
                selectionArgs
        );
        return count > 0;*/
    }

    @Override
    public synchronized boolean delete(String url, boolean isRetained) {
        if (!isRetained) {
            DownloadEntity entity = query(url);
            if (entity != null && entity.getSavePath() != null) {
                File file = new File(entity.getSavePath());
                if (file.exists()) {
                    file.delete();
                }
            }
        }
        // Define 'where' part of queryAll.
        String selection = DownloadTable.COLUMN_NAME_DOWNLOAD_URL + DBSelection.SELECTION_EQUAL;
        // Specify arguments in placeholder order.
        String[] selectionArgs = {url};
        // Issue SQL statement.
        return mDbHelper.getWritableDatabase().delete(DownloadTable.TABLE_NAME, selection, selectionArgs) > 0;

    }

    @Override
    public synchronized void deleteAll() {
        List<DownloadEntity> list = queryAll();
        for (DownloadEntity entity : list) {
            if (entity != null && entity.getSavePath() != null) {
                File file = new File(entity.getSavePath());
                if (file.exists()) {
                    file.delete();
                }
            }
        }
        mDbHelper.getWritableDatabase().delete(DownloadTable.TABLE_NAME, null, null);
    }

    @Override
    public DownloadEntity query(String url) {
        String[] projection = {
                DownloadTable.COLUMN_NAME_DOWNLOAD_URL,
                DownloadTable.COLUMN_NAME_CURRENT_POSITION,
                DownloadTable.COLUMN_NAME_TOTAL_SIZE,
                DownloadTable.COLUMN_NAME_SAVE_PATH,
                DownloadTable.COLUMN_NAME_FILE_NAME,
                DownloadTable.COLUMN_NAME_STATUS,
                DownloadTable.COLUMN_NAME_THUMBNAILS
        };
        String selection = DownloadTable.COLUMN_NAME_DOWNLOAD_URL + DBSelection.SELECTION_EQUAL;
        String[] selectionArgs = {url};
        Cursor cursor = mDbHelper.getReadableDatabase().query(
                DownloadTable.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            return new DownloadEntity(
                    Long.parseLong(cursor.getString(cursor.getColumnIndex(DownloadTable.COLUMN_NAME_CURRENT_POSITION))),
                    Long.parseLong(cursor.getString(cursor.getColumnIndex(DownloadTable.COLUMN_NAME_TOTAL_SIZE))),
                    cursor.getString(cursor.getColumnIndex(DownloadTable.COLUMN_NAME_DOWNLOAD_URL)),
                    cursor.getString(cursor.getColumnIndex(DownloadTable.COLUMN_NAME_SAVE_PATH)),
                    cursor.getString(cursor.getColumnIndex(DownloadTable.COLUMN_NAME_FILE_NAME)),
                    Integer.parseInt(cursor.getString(cursor.getColumnIndex(DownloadTable.COLUMN_NAME_STATUS))),
                    cursor.getString(cursor.getColumnIndexOrThrow(DownloadTable.COLUMN_NAME_THUMBNAILS))
            );
        }
        cursor.close();
        return null;
    }

    @Override
    public List<DownloadEntity> queryAll() {
        List<DownloadEntity> list = new ArrayList<>();
        String[] projection = {
                DownloadTable.COLUMN_NAME_DOWNLOAD_URL,
                DownloadTable.COLUMN_NAME_CURRENT_POSITION,
                DownloadTable.COLUMN_NAME_TOTAL_SIZE,
                DownloadTable.COLUMN_NAME_SAVE_PATH,
                DownloadTable.COLUMN_NAME_FILE_NAME,
                DownloadTable.COLUMN_NAME_STATUS,
                DownloadTable.COLUMN_NAME_THUMBNAILS
        };
        String selection = DownloadTable.COLUMN_NAME_STATUS + DBSelection.SELECTION_EQUAL;
        String[] selectionArgs = {DownloadFlag.COMPLETED + ""};
        Cursor cursor = mDbHelper.getReadableDatabase().query(
                DownloadTable.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        while (cursor.moveToNext()) {
            list.add(new DownloadEntity(
                    Long.parseLong(cursor.getString(cursor.getColumnIndexOrThrow(DownloadTable.COLUMN_NAME_CURRENT_POSITION))),
                    Long.parseLong(cursor.getString(cursor.getColumnIndexOrThrow(DownloadTable.COLUMN_NAME_TOTAL_SIZE))),
                    cursor.getString(cursor.getColumnIndexOrThrow(DownloadTable.COLUMN_NAME_DOWNLOAD_URL)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DownloadTable.COLUMN_NAME_SAVE_PATH)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DownloadTable.COLUMN_NAME_FILE_NAME)),
                    Integer.parseInt(cursor.getString(cursor.getColumnIndexOrThrow(DownloadTable.COLUMN_NAME_STATUS))),
                    cursor.getString(cursor.getColumnIndexOrThrow(DownloadTable.COLUMN_NAME_THUMBNAILS))
            ));
        }
        cursor.close();
        return list;
    }

    @Override
    public List<DownloadEntity> queryDoingOn() {
        List<DownloadEntity> list = new ArrayList<>();
        String[] projection = {
                DownloadTable.COLUMN_NAME_DOWNLOAD_URL,
                DownloadTable.COLUMN_NAME_CURRENT_POSITION,
                DownloadTable.COLUMN_NAME_TOTAL_SIZE,
                DownloadTable.COLUMN_NAME_SAVE_PATH,
                DownloadTable.COLUMN_NAME_FILE_NAME,
                DownloadTable.COLUMN_NAME_STATUS,
                DownloadTable.COLUMN_NAME_THUMBNAILS
        };
        String selection = DownloadTable.COLUMN_NAME_STATUS + DBSelection.SELECTION_UNEQUAL;
        String[] selectionArgs = {DownloadFlag.COMPLETED + ""};
        Cursor cursor = mDbHelper.getReadableDatabase().query(
                DownloadTable.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        while (cursor.moveToNext()) {
            list.add(new DownloadEntity(
                    Long.parseLong(cursor.getString(cursor.getColumnIndexOrThrow(DownloadTable.COLUMN_NAME_CURRENT_POSITION))),
                    Long.parseLong(cursor.getString(cursor.getColumnIndexOrThrow(DownloadTable.COLUMN_NAME_TOTAL_SIZE))),
                    cursor.getString(cursor.getColumnIndexOrThrow(DownloadTable.COLUMN_NAME_DOWNLOAD_URL)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DownloadTable.COLUMN_NAME_SAVE_PATH)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DownloadTable.COLUMN_NAME_FILE_NAME)),
                    Integer.parseInt(cursor.getString(cursor.getColumnIndexOrThrow(DownloadTable.COLUMN_NAME_STATUS))),
                    cursor.getString(cursor.getColumnIndexOrThrow(DownloadTable.COLUMN_NAME_THUMBNAILS))
            ));
        }
        cursor.close();
        return list;
    }

    @Override
    public void closeDB() {
        if (mDbHelper != null) {
            mDbHelper.close();
        }
    }
}
