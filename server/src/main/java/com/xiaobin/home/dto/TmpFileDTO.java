package com.xiaobin.home.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TmpFileDTO {

    private String referer;

    /**
     * 文件夹名称
     */
    private List<String> foldNames;

    private List<Data> items;

    @Getter
    @Setter
    public static class Data {
        private String filename;

        private String url;
    }
}
