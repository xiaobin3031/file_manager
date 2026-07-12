package com.xiaobin.home.entity;

import com.xiaobin.home.orm.annotation.Entity;
import com.xiaobin.home.orm.annotation.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class FmNode {

    @Id
    private Long id;
    private Short nodeType;
    private String nodeName;
    private Long parentId;
    private Integer ownerId;
    private Integer storageId;
    private Long size;
    private Integer sort;
    private Boolean deleted;
    private Boolean hidden;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
