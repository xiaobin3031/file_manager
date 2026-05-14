package com.xiaobin.home.repository;

import com.xiaobin.home.entity.PlayHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayHistoryDao extends JpaRepository<PlayHistory, Long> {

    PlayHistory findOneByFoldIdAndUserIdAndStatus(Long foldId, Integer userId, Short status);
}
