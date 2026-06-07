package com.xiaobin.home.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class Files {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Integer userId;

    private Long size;

    private String fileType;

    private String hash;

    private String storagePath;

    private Boolean deleted;

    private LocalDateTime deleteAt;

    private LocalDateTime createAt;

    private LocalDateTime updateAt;

    private Long foldId;

    private Integer sampleStatus;

    private Integer sort;

    private Short status;

    private Boolean removed;

    private String dhtHash;

    private String hostUrl;

    private String referer;
}
