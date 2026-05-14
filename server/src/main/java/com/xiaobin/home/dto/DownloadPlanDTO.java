package com.xiaobin.home.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DownloadPlanDTO {

    private String url;

    private String xpathExpression;

    private Long foldId;
}
