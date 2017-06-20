package com.codyy.download.service;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Created by lijian on 2017/6/9.
 */

public class DownloadStatus implements Parcelable {
    private long totalSize;
    private long downloadSize;

    public DownloadStatus(long downloadSize, long totalSize) {
        this.totalSize = totalSize;
        this.downloadSize = downloadSize;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getDownloadSize() {
        return downloadSize;
    }

    public void setDownloadSize(long downloadSize) {
        this.downloadSize = downloadSize;
    }

    /**
     * 获得格式化的总Size
     *
     * @return example: 2K , 10M
     */
    public String getFormatTotalSize() {
        return formatSize(totalSize);
    }

    public String getFormatDownloadSize() {
        return formatSize(downloadSize);
    }

    /**
     * 获得格式化的状态字符串
     *
     * @return example: 2M/36M
     */
    public String getFormatStatusString() {
        return getFormatDownloadSize() + "/" + getFormatTotalSize();
    }

    /**
     * 获得下载的百分比,默认保留两位小数
     *
     * @return example: 5.25%
     */
    public String getPercent() {

        return getPercent(2);//2：表示保留2位小数点
    }

    public String getPercent(int minimumFractuibDigits) {
        String percent;
        Double result;
        if (totalSize == 0L) {
            result = 0.0;
        } else {
            result = downloadSize * 1.0 / totalSize;
        }
        NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMinimumFractionDigits(minimumFractuibDigits);//控制保留小数点后几位
        percent = nf.format(result);
        return percent;
    }

    /**
     * 获得下载的百分比数值
     *
     * @return example: 5%  will return 5, 10% will return 10.
     */
    public long getPercentNumber() {
        double result;
        if (totalSize == 0L) {
            result = 0.0;
        } else {
            result = downloadSize * 1.0 / totalSize;
        }
        return (long) (result * 100);
    }

    public static String formatSize(long size) {
        String hrSize;
        double b = size;
        double k = size / 1024.0;
        double m = ((size / 1024.0) / 1024.0);
        double g = (((size / 1024.0) / 1024.0) / 1024.0);
        double t = ((((size / 1024.0) / 1024.0) / 1024.0) / 1024.0);
        DecimalFormat dec = new DecimalFormat("0.00");
        if (t > 1) {
            hrSize = dec.format(t).concat(" T");
        } else if (g > 1) {
            hrSize = dec.format(g).concat(" G");
        } else if (m > 1) {
            hrSize = dec.format(m).concat(" M");
        } else if (k > 1) {
            hrSize = dec.format(k).concat(" K");
        } else {
            hrSize = dec.format(b).concat(" B");
        }
        return hrSize;
    }

    public static String formatRate(long size) {
        String hrSize;
        double b = size;
        double k = size / 1024.0;
        double m = ((size / 1024.0) / 1024.0);
        double g = (((size / 1024.0) / 1024.0) / 1024.0);
        double t = ((((size / 1024.0) / 1024.0) / 1024.0) / 1024.0);
        DecimalFormat dec = new DecimalFormat("0.00");
        if (t > 1) {
            hrSize = dec.format(t).concat(" T/S");
        } else if (g > 1) {
            hrSize = dec.format(g).concat(" G/S");
        } else if (m > 1) {
            hrSize = dec.format(m).concat(" M/S");
        } else if (k > 1) {
            hrSize = dec.format(k).concat(" K/S");
        } else {
            hrSize = dec.format(b).concat(" B/S");
        }
        return hrSize;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.totalSize);
        dest.writeLong(this.downloadSize);
    }

    protected DownloadStatus(Parcel in) {
        this.totalSize = in.readLong();
        this.downloadSize = in.readLong();
    }

    public static final Parcelable.Creator<DownloadStatus> CREATOR = new Parcelable.Creator<DownloadStatus>() {
        @Override
        public DownloadStatus createFromParcel(Parcel source) {
            return new DownloadStatus(source);
        }

        @Override
        public DownloadStatus[] newArray(int size) {
            return new DownloadStatus[size];
        }
    };
}
