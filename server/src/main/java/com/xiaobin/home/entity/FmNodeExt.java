package com.xiaobin.home.entity;

import com.xiaobin.home.orm.annotation.Entity;
import com.xiaobin.home.orm.annotation.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class FmNodeExt {

    @Id
    private Long id;

    private Long foldId;

    private String childrenOrder;

    private Boolean deleted;

}
