package com.codyy.download.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


/**
 * Created by lijian on 2017/6/7.
 */

class DownloadDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "download.db";
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + DownloadTable.TABLE_NAME + " (" +
                    DownloadTable.COLUMN_NAME_DOWNLOAD_URL + " TEXT PRIMARY KEY ," +
                    DownloadTable.COLUMN_NAME_CURRENT_POSITION + TEXT_TYPE + COMMA_SEP +
                    DownloadTable.COLUMN_NAME_TOTAL_SIZE + TEXT_TYPE + COMMA_SEP +
                    DownloadTable.COLUMN_NAME_SAVE_PATH + TEXT_TYPE + COMMA_SEP +
                    DownloadTable.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP +
                    DownloadTable.COLUMN_NAME_THUMBNAILS + TEXT_TYPE + COMMA_SEP +
                    DownloadTable.COLUMN_NAME_DOWNLOAD_TIME + TEXT_TYPE + COMMA_SEP +
                    DownloadTable.COLUMN_NAME_EXTRA1 + TEXT_TYPE + COMMA_SEP +
                    DownloadTable.COLUMN_NAME_EXTRA2 + TEXT_TYPE + COMMA_SEP +
                    DownloadTable.COLUMN_NAME_STATUS + TEXT_TYPE + " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + DownloadTable.TABLE_NAME;

    DownloadDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
}
