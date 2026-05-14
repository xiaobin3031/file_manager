package com.xiaobin.home.repository;

import com.xiaobin.home.entity.FileUploadProgress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileUploadProgressDao extends JpaRepository<FileUploadProgress, Integer> {

    FileUploadProgress findOneByUploadId(String uploadId);
}
