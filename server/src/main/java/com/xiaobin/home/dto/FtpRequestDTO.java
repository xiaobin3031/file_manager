package com.xiaobin.home.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FtpRequestDTO {

    private String dirName;

    private String filename;

    private Long id;

    private boolean fileFlag;

    private String newName;

    private Long foldId;

    // 用于播放
    private boolean prepareForPlay;

    private List<FtpDirsDTO> files;
}
