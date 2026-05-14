package com.xiaobin.home.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class Shares {
    @Id
    @GeneratedValue
    private Long id;

    private Long fileId;

    private Integer userId;

    private String shareCode;

    private LocalDateTime expireAt;

    private LocalDateTime createAt;

    private Integer toUserId;
}
