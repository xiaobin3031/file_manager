package com.xiaobin.home.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class ExpandFoldsAuto {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String storagePath;

    private Long expandToFold;

    private Integer expandUserId;

    private Boolean expanded;

    /**
     * 是否移动文件
     */
    private Boolean moveFile;

}
