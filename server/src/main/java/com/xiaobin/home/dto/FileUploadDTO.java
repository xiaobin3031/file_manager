package com.xiaobin.home.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileUploadDTO {

    private String fileId;

    private Long totalChunks;

    private Long currentChunk;

    private Long totalSize;

    private Long foldId;

    private String fileName;
}
