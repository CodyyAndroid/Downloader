package com.codyy.downloader;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.codyy.download.Downloader;
import com.codyy.download.entity.DownloadEntity;
import com.codyy.download.service.DownLoadListener;
import com.codyy.download.service.DownloadConnectedListener;
import com.codyy.download.service.DownloadRateListener;
import com.codyy.download.service.DownloadStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lijian on 2017/6/6.
 */

public class MultiDownloadActivity extends AppCompatActivity {
    private List<FileEntity> mFileEntities = new ArrayList<>();
    private TextView mTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_download);
        mTextView = (TextView) findViewById(R.id.tv_rate);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mFileEntities.add(new FileEntity("百度手机助手", getString(R.string.url_apk_file)));
        mFileEntities.add(new FileEntity("AndroidPDF", getString(R.string.url_small_file)));
        recyclerView.setAdapter(new FileAdapter(mFileEntities));
//        Downloader.getInstance(getApplicationContext()).download(getString(R.string.url_apk_file));
//        Downloader.getInstance(getApplicationContext()).download(getString(R.string.url_small_file));
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
                    if ("下载".equals(btn.getText().toString())) {
                        Downloader.getInstance(itemView.getContext()).download(tvUrl.getText().toString());
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
            Downloader.getInstance(itemView.getContext()).receiveDownloadStatus(tvUrl.getText().toString(), new DownLoadListener() {
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
            });
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
