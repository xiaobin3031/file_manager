package com.xiaobin.home.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class PlayHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long foldId;

    private Long fileId;

    private String fileType;

    private Integer userId;

    private String foldPath;

    private Long start;

    private Long end;

    private LocalDateTime addTime;

    private LocalDateTime updateTime;

    private Short status;
}
