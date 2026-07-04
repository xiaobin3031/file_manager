package com.xiaobin.home.repository;

import com.xiaobin.home.entity.Folds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface FoldsDao extends JpaRepository<Folds, Long> {

    Folds findFoldsByNameAndUserIdAndDeletedFalse(String name, Integer userId);

    Folds findFoldsByParentIdAndId(Long parentId, Long id);

    Folds findByIdAndUserIdAndDeletedFalse(Long id, Integer userId);

    @Query("select f from Folds f where f.parentId = :parentId and f.userId = :userId and f.deleted = false")
    List<Folds> loadFolds(Long parentId, Integer userId);

    @Query("select f from Folds f where f.name = :name and f.parentId = :parentId and f.userId = :userId and f.deleted = false")
    Folds loadFoldsByNameInFold(String name, Long parentId, Integer userId);

    @Query("select f from Folds f where f.id = :id and f.userId = :userId and f.deleted = false")
    Folds loadSpecialFoldsInFold(Long id, Integer userId);

    @Query("select f from Folds f where f.parentId = :parentId and f.id = :id and f.userId = :userId and f.deleted = false")
    Folds loadFoldInFold(Long parentId, Long id, Integer userId);

    @Modifying
    @Transactional
    @Query("update Folds set sort = :sort where parentId = :foldId and id = :id and userId = :userId and deleted = false")
    void updateSortById(@Param("foldId") Long foldId, @Param("id") Long id, @Param("userId") Integer userId, @Param("sort") Integer sort);

    @Query("select f from Folds f where f.deleted = true and f.deleteAt >= :time")
    List<Folds> loadDeletedWithTime(LocalDateTime time);

    @Query("select f from Folds f where f.parentId = :foldId")
    List<Folds> findByParentIdAndDeletedFalse(Long foldId);

    @Modifying
    @Transactional
    @Query("update Folds set parentId = :curFoldId where parentId != 0 and parentId = :id and userId = :userId and deleted = false")
    void changeFold(Long curFoldId, Long id, Integer userId);

    @Modifying
    @Transactional
    @Query("update Folds set foldCount = foldCount + :count where id = :id and userId = :userId")
    void updateFoldCount(Long id, int count, Integer userId);

    @Modifying
    @Transactional
    @Query("update Folds set fileCount = foldCount + :count where id = :id and userId = :userId")
    void updateFileCount(Long id, int count, Integer userId);

    @Modifying
    @Transactional
    @Query("update Folds set fileCount = 0, foldCount = 0 where id in :ids and userId = :userId")
    void clearFoldAndFileCount(List<Long> ids, Integer userId);
}
