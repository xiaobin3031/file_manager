package com.xiaobin.home.repository;

import com.xiaobin.home.entity.ExpandFoldsAuto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpandFoldsAutoDao  extends JpaRepository<ExpandFoldsAuto, Integer> {

    List<ExpandFoldsAuto> findByExpandedFalse();
}
