package com.xiaobin.home.repository;

import com.xiaobin.home.entity.Logs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface LogsDao extends JpaRepository<Logs,Long> {

    @Modifying
    @Transactional
    @Query(value = "insert into logs(title,content, user_id) values(?1,?2, ?3)", nativeQuery = true)
    void addLog(String title, String content, Integer userId);

    Logs findOneByIdAndUserId(Long id, Integer userId);

    long countByUserIdAndRead(Integer userId, Boolean read);

    Page<Logs> findAllByUserId(Integer userId, Pageable pageable);
}
