package com.codyy.downloader;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.StatFs;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.codyy.download.Downloader;
import com.codyy.download.entity.DownloadEntity;
import com.codyy.download.service.DownloadRateListener;
import com.codyy.download.service.DownloadStatus;
import com.codyy.download.service.SimpleDownloadListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lijian on 2017/6/6.
 */

public class MultiDownloadActivity extends AppCompatActivity {
    private List<FileEntity> mFileEntities = new ArrayList<>();
    private TextView mTextView;
    private EditText mEditText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Downloader.init(this, BuildConfig.DEBUG);
        setContentView(R.layout.activity_multi_download);
        mTextView = (TextView) findViewById(R.id.tv_rate);
        mEditText = (EditText) findViewById(R.id.et);
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (keyEvent.getAction() == KeyEvent.KEYCODE_DEL) {
                    Log.d("keyCode", "del");
                }
                return false;
            }
        });
        mEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                    Log.d("keyCode", "del");
                }
                return false;
            }
        });
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mFileEntities.add(new FileEntity("百度手机助手", getString(R.string.url_apk_file)));
//        mFileEntities.add(new FileEntity("resource", "http://reserver.9itest.com:8081/res/view/mobile/download/video/d908232052b84ef0bfa10152067339b8/396face834ca48dab37ad22c6323573b.do"));
//        mFileEntities.add(new FileEntity("resource", "http://reserver.jxd.9itest.com:8091/res/view/mobile/download/video/e74f91af5dd74767b8ad96753b5fb3ee/19632d7b5cfe458b9fcfaef7811f88bb.do"));
        mFileEntities.add(new FileEntity("resource", "http://reserver.jxd.9itest.com:8091/res/view/mobile/download/video/08ca41aeeb7b4e4c9801d0e8b1408890/27254357585042d9b4950332aa7a36a9.do"));
        mFileEntities.add(new FileEntity("TeamViewer_Setup", "http://mock.5idoo.com/file/TeamViewer_Setup.exe"));
        mFileEntities.add(new FileEntity("OSP", "http://srv.codyy.cn/images/9059e96d-98e5-44dc-b509-a46d11716960.apk/app.apk"));
        recyclerView.setAdapter(new FileAdapter(mFileEntities));
        checkPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 2018: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    mEditText.setText(Formatter.formatFileSize(getBaseContext(), getAvailableStore(getExternalStoragePath())) + "/" + Formatter.formatFileSize(getBaseContext(), getTotalStore(getExternalStoragePath())));
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private boolean getPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void checkPermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this).setCancelable(false).setMessage("您未授予下载权限,将无法下载文件,是否授予权限?").setTitle("提示").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MultiDownloadActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                2018);
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).create().show();
            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        2018);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    // 获取SD卡路径
    public static String getExternalStoragePath() {
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

    private String getAvailMemory() {// 获取android当前可用内存大小

        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        //mi.availMem; 当前系统的可用内存

        return Formatter.formatFileSize(getBaseContext(), mi.availMem);// 将获取的内存大小规格化
    }

    public static long getAvailableStore(String filePath) {

        // 取得sdcard文件路径

        StatFs statFs = new StatFs(filePath);

        // 获取block的SIZE

        long blocSize = statFs.getBlockSize();

        // 获取BLOCK数量

        // long totalBlocks = statFs.getBlockCount();

        // 可使用的Block的数量

        long availaBlock = statFs.getAvailableBlocks();

        // long total = totalBlocks * blocSize;

        long availableSpare = availaBlock * blocSize;

        return availableSpare;

    }

    public static long getTotalStore(String filePath) {

        // 取得sdcard文件路径

        StatFs statFs = new StatFs(filePath);

        // 获取block的SIZE

        long blocSize = statFs.getBlockSize();

        // 获取BLOCK数量

        long totalBlocks = statFs.getBlockCount();

        // 可使用的Block的数量

        long total = totalBlocks * blocSize;


        return total;

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /*if (keyCode == KeyEvent.KEYCODE_DEL) {
         Log.d("keyCode","del");
        }*/
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Downloader.getInstance(this).addRateListener(new DownloadRateListener() {
            @Override
            public void onRate(String rate, int count) {
                mTextView.setText(rate + " 任务:" + count);
            }

            @Override
            public void onComplete(DownloadEntity entity) {
                Log.d("entity", entity.toString());
            }
        });
        Downloader.getInstance(getApplicationContext()).setHoneyCombDownload(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Downloader.getInstance(this).removeRateListener();
    }

    public void pauseAll(View view) {
       /* for (DownloadEntity entity : Downloader.getInstance(this).getDownloadingRecords()) {
            Log.e("entity", entity.toString());
        }*/
        Downloader.getInstance(this).pauseAll();

    }

    public void delete(View view) {
        Downloader.getInstance(this).delete(getString(R.string.url_small_file));
    }

    public void deleteAll(View view) {
        Downloader.getInstance(this).deleteAll();
    }

    public void startAll(View view) {
        Downloader.getInstance(this).startAll();
    }

    public void getLoading(View view) {
        for (DownloadEntity entity : Downloader.getInstance(getApplicationContext()).getDownloadingRecords()) {
            Log.d("download", entity.toString());
        }
    }

    public void queryAll(View view) {
        for (DownloadEntity entity : Downloader.getInstance(getApplicationContext()).getTotalDownloadRecords()) {
            Log.d("queryAll", entity.getTime() + ":" + entity.getName());
        }
    }

    public void update(View view) {
        DownloadEntity entity = Downloader.getInstance(this).getDownloadRecord(getString(R.string.url_small_file));
        if (entity != null && entity.getExtra2() != null) {
            entity.setExtra2(new StringBuilder(entity.getExtra2()).append("\u0003").append("sultan").toString());
            Downloader.getInstance(this).update(entity);
        }
    }

    class FileViewHolder extends RecyclerViewHolder<FileEntity> {
        TextView tvName;
        TextView tvUrl;
        ProgressBar pb;
        Button btn;

        FileViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void mapFromView(final View view) {
            tvName = view.findViewById(R.id.tv_title);
            tvUrl = view.findViewById(R.id.tv_url);
            pb = view.findViewById(R.id.pb_progress);
            btn = view.findViewById(R.id.btn_download);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!getPermission()) {
                        checkPermission();
                        return;
                    }
                    if ("下载".equals(btn.getText().toString())) {
                        DownloadEntity entity = new DownloadEntity();
                        entity.setId(tvUrl.getText().toString());
                        entity.setUrl(tvUrl.getText().toString());
                        entity.setExtra2("lijian");
                        Downloader.getInstance(itemView.getContext()).download(entity);
                    } else if ("暂停".equals(btn.getText().toString())) {
                        btn.setText("下载");
                        Downloader.getInstance(itemView.getContext()).pause(tvUrl.getText().toString());
                    }
                }
            });
        }

        @Override
        public void setDataToView(FileEntity data) {
            tvName.setText(data.getName());
            tvUrl.setText(data.getUrl());
            Downloader.getInstance(itemView.getContext()).receiveDownloadStatus(tvUrl.getText().toString(), new SimpleDownloadListener() {
                @Override
                public void onWaiting() {
                    btn.setText("等待");
                }

                @Override
                public void onProgress(DownloadStatus status) {
                    pb.setProgress((int) status.getPercentNumber());
                    tvName.setText(status.getFormatStatusString());
                }

                @Override
                public void onPause() {
                    btn.setText("下载");
                }

                @Override
                public void onComplete() {
                    btn.setText("完成");
                }

            });
           /* Downloader.getInstance(itemView.getContext()).receiveDownloadStatus(tvUrl.getText().toString(), new DownLoadListener() {
                @Override
                public void onStart() {
                    btn.setText("暂停");
                }

                @Override
                public void onWaiting() {
                    btn.setText("等待");
                }

                @Override
                public void onProgress(DownloadStatus status) {
                    pb.setProgress((int) status.getPercentNumber());
                    tvName.setText(status.getFormatStatusString());
//                    Log.e("Download", "" + progress);
                }

                @Override
                public void onPause() {
                    btn.setText("下载");
                }

                @Override
                public void onComplete() {
                    btn.setText("完成");
                }

                @Override
                public void onDelete() {
                    btn.setText("下载");
                    pb.setProgress(0);
                    Toast.makeText(getApplicationContext(), "已删除记录", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int code) {
                    Log.e("Download", " onFailure Code:" + code);
                }

                @Override
                public void onError(Exception e) {
                    Log.e("Download", " onError :" + e.getMessage());
                }
            });*/
        }
    }

    private class FileAdapter extends RecyclerView.Adapter<FileViewHolder> {
        List<FileEntity> list;

        FileAdapter(List<FileEntity> list) {
            this.list = list;
        }

        @Override
        public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new FileViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_multi_download, parent, false));
        }

        @Override
        public void onBindViewHolder(FileViewHolder holder, int position) {
            holder.setDataToView(list.get(position));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
