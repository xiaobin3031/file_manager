package com.xiaobin.home.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ListDirDTO {

    private List<String> path;

    private List<FtpDirsDTO> files;

    private FtpDirsDTO lastFile;
}
