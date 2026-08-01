package com.xiaobin.home.entity;

import com.xiaobin.home.orm.annotation.Entity;
import com.xiaobin.home.orm.annotation.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class FmDownload {

    @Id
    private Integer id;

    private String hash;

    private LocalDateTime createTime;

    private LocalDateTime finishTime;

    private Boolean deleted;

}
