package com.xiaobin.home.constant;

public class Constant {

    public static final short FILE_PROGRESS_INIT = 0;
    public static final short FILE_PROGRESS_FINISH = 50;
    public static final short FILE_PROGRESS_ERROR = 99;

    public static final String WEBDAV_PATH = "/webdav/";

    public static final short NODE_TYPE_FILE = 1;
    public static final short NODE_TYPE_FOLDER = 2;

    public static boolean isFold(Short nodeType) {
        return nodeType != null && nodeType == NODE_TYPE_FOLDER;
    }

    public static boolean isFile(Short nodeType) {
        return nodeType != null && nodeType == NODE_TYPE_FILE;
    }
}
