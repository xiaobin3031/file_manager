package com.xiaobin.home.repository;

import com.xiaobin.home.entity.FileDownloadConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileDownloadConfigDao extends JpaRepository<FileDownloadConfig, Long> {
    Optional<FileDownloadConfig> findFirstByHost(String host);
}
