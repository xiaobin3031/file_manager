package com.xiaobin.home.repository;

import com.xiaobin.home.entity.FileProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FileProgressDao extends JpaRepository<FileProgress, Long> {

    @Query("select f from FileProgress f where f.fileId = ?1 and f.status != 99")
    FileProgress findByFileId(Long fileId);
}
