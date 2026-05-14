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
@Entity(name = "Folds")
public class Folds {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Long parentId;

    private LocalDateTime createAt;

    private Integer userId;

    private Boolean deleted;

    private LocalDateTime deleteAt;

    private String path;

    /**
     * 是否物理路径
     */
    private Boolean physics;

    private Integer sort;
}
