package com.xiaobin.home.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
public class FileDownloadPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    private Integer type;

    private String url;

    private String xpathExpression;

    private String currentName;

    private Boolean finish;

    private Long foldId;

    private Boolean deleted;

    private Integer userId;

    private LocalDateTime lastFileTime;

    private Integer separatorDays;
}
