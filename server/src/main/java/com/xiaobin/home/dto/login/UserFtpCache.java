package com.xiaobin.home.dto.login;

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.*;

@Getter
@Setter
public class UserFtpCache {

    private static final Map<String, Long> fileTokenMap = new HashMap<>();

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

    private Integer loginId;

    public long currentFoldId() {
        return foldIds.isEmpty() ? 0L : foldIds.getFirst();
    }

    public String addFileToken(Long nodeId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        fileTokenMap.put(token, nodeId);
        return token;
    }

}
