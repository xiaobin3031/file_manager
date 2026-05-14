package com.xiaobin.home.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FtpDirsDTO {

    private final Long id;

    private final boolean fileFlag;

    private final String name;

    private Integer sort;

    public FtpDirsDTO(Long id, boolean fileFlag, String name) {
        this.id = id;
        this.fileFlag = fileFlag;
        this.name = name;
    }

    /**
     * 预览图
     */
    private String sample;
    private String fileType;

    /**
     * 视频历史播放使用
     */
    private Long start;
    private Long end;
}
