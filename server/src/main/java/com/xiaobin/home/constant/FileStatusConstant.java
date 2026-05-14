package com.xiaobin.home.constant;

public class FileStatusConstant {

    public static final short INIT = 0;

    public static final short UNZIP = 5;

    public static final short UPLOAD = 10;

    public static final short DOWNLOAD = 20;

    public static boolean isFree(Short status) {
        return status == null || status == INIT;
    }

    public static boolean isLocked(Short status) {
        return status != null && status != INIT;
    }

    public static boolean isUnzip(Short status) {
        return status != null && status == UNZIP;
    }

    public static String getStatusName(Short status) {
        return switch (status) {
            case INIT -> "初始化";
            case UNZIP -> "解压中";
            case UPLOAD -> "上传中";
            case DOWNLOAD -> "下载中";
            default -> "未知";
        };
    }

    public static boolean isUpload(Short status) {
        return status != null && status == UPLOAD;
    }

    public static boolean isDownload(Short status) {
        return status != null && status == DOWNLOAD;
    }
}
