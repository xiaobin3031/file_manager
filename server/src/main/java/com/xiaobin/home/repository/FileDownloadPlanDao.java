package com.xiaobin.home.repository;

import com.xiaobin.home.entity.FileDownloadPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileDownloadPlanDao extends JpaRepository<FileDownloadPlan, Integer> {

    FileDownloadPlan findOneByUrlAndDeletedFalseAndUserIdAndFoldId(String url, Integer userId, Long foldId);

    List<FileDownloadPlan> findByFinishAndDeletedFalse(Boolean finish);
}
