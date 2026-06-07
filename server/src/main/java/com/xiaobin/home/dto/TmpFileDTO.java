package com.xiaobin.home.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TmpFileDTO {

    private String referer;

    private List<Data> items;

    @Getter
    @Setter
    public static class Data {
        private String fileName;

        private String url;
    }
}
