package com.xiaobin.home.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class WebDavPath {

    private String path;

    private String name;

    private boolean directory;

    private long size;

    private Date modifyTime;
}
