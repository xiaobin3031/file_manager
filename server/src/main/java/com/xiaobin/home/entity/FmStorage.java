package com.xiaobin.home.entity;

import com.xiaobin.home.orm.annotation.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class FmStorage {

    @Id
    private Integer id;

    private String hash;

    private Long size;

    private String mimeType;

    private String path;

    private Integer refCount;

    private LocalDateTime createTime;

    private Boolean sampleFlag;
}
