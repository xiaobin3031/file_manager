package com.xiaobin.home.dto.login;

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class UserFtpCache {

    private List<String> path = new ArrayList<>();

    private List<Long> foldIds = new ArrayList<>();

    private boolean physicsFlag;

    private File physicsFile;

    /**
     * 物理根路径
     */
    private String physicsRootPath;

    /**
     * 当前的文件id
     */
    private Long curFileId;

    public long currentFoldId() {
        return foldIds.isEmpty() ? 0L : foldIds.getFirst();
    }

    public String getCurrentPath() {
        ArrayList<String> tmpPath = new ArrayList<>(path);
        Collections.reverse(tmpPath);
        return String.join(File.separator, tmpPath);
    }
}
