package com.xiaobin.home.repository;

import com.xiaobin.home.entity.Shares;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharesDao extends JpaRepository<Shares, Long> {
}
