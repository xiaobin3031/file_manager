package com.xiaobin.home.repository;

import com.xiaobin.home.entity.Files;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface FilesDao extends JpaRepository<Files, Long> {

    List<Files> findFilesByFoldIdAndUserIdAndDeletedFalse(Long foldId, Integer userId);

    @Query("select f from Files f where f.foldId = :foldId and f.name = :name and f.userId = :userId and f.deleted = false")
    Files loadFilesByNameInFold(Long foldId, String name, Integer userId);

    @Query("select f from Files f where f.dhtHash = :dhtHash and f.deleted = false")
    Files loadDhtFile(String dhtHash);

    @Query("select f from Files f where f.foldId = :foldId and f.id = :id and f.userId = :userId and f.deleted = false")
    Files loadFileInFold(Long foldId, Long id, Integer userId);

    @Query("select f from Files f where f.id > :minId and f.fileType is null and f.deleted = false order by f.id asc")
    List<Files> pageFetchFileTypeNull(long minId, int size);

    @Query("select f from Files f where f.id = :id and f.userId = :userId and f.deleted = false")
    Files loadById(Long id, Integer userId);

    /**
     * 查询视频文件
     */
    @Query("select f from Files f where f.id > :minId and f.fileType is not null and f.deleted = false and f.sampleStatus = 0 and f.status = 0 order by f.id asc limit :size")
    List<Files> pageNoSample(long minId, int size);

    @Query("select f from Files f where f.id > :minId and f.storagePath not like '/home/xiaobin/mnt/wd4t/%' and f.deleted = false order by f.id asc limit :size")
    List<Files> loadToMoveFiles(long minId, int size);

    /**
     * 查找下一张图片
     */
    @Query("select f from Files f where f.foldId = :foldId and f.id > :curFileId and f.fileType like 'image/%' and f.deleted = false order by f.id asc limit 1")
    Files loadNextImageInFold(Long foldId, Long curFileId, Integer userId);

    @Query("select f from Files f where f.foldId = :foldId and f.sort > :sort and f.fileType like 'image/%' and f.deleted = false order by f.sort asc limit 1")
    Files loadNextImageInFold(Long foldId, Integer sort, Integer userId);

    @Query("select f from Files f where f.foldId = :foldId and f.sort > :sort and f.fileType = :fileType and f.deleted = false order by f.sort asc limit 1")
    Files loadNextFileInFold(Long foldId, Integer sort, Integer userId, String fileType);

    @Query("select f from Files f where f.foldId = :foldId and f.sort < :sort and f.fileType = :fileType and f.deleted = false order by f.sort desc limit 1")
    Files loadPrevFileInFold(Long foldId, Integer sort, Integer userId, String fileType);

    @Query("select f from Files f where f.foldId = :foldId and f.id > :curFileId and f.fileType = :fileType and f.deleted = false order by f.id asc limit 1")
    Files loadNextFileInFold(Long foldId, Long curFileId, Integer userId, String fileType);

    @Query("select f from Files f where f.foldId = :foldId and f.id < :curFileId and f.fileType = :fileType and f.deleted = false order by f.id desc limit 1")
    Files loadPrevFileInFold(Long foldId, Long curFileId, Integer userId, String fileType);

    /**
     * 查找上一张图片
     */
    @Query("select f from Files f where f.foldId = :foldId and f.id < :curFileId and f.fileType like 'image/%' and f.deleted = false order by f.id desc limit 1")
    Files loadPrevImageInFold(Long foldId, Long curFileId, Integer userId);

    @Query("select f from Files f where f.foldId = :foldId and f.sort < :sort and f.fileType like 'image/%' and f.deleted = false order by f.sort desc limit 1")
    Files loadPrevImageInFold(Long foldId, Integer sort, Integer userId);

    @Modifying
    @Transactional
    @Query("update Files set sort = :sort where foldId = :foldId and id = :id and userId = :userId and deleted = false")
    void updateSortById(@Param("foldId") Long foldId, @Param("id") Long id, @Param("userId") Integer userId, @Param("sort") Integer sort);

    @Query("select f from Files f where f.fileType in :fileTypes and f.id > :minId and f.deleted = false order by f.id limit :size")
    List<Files> loadFilesByFileType(long minId, List<String> fileTypes, int size);

    List<Files> findByFileTypeAndFoldIdAndDeletedFalse(String fileType, Long foldId);

    @Query("select f from Files f where f.foldId = :foldId and f.deleted = false")
    List<Files> findByFoldIdAndDeletedFalse(Long foldId);

    @Query("select f from Files f where f.id > :minId and f.deleted = false and f.status = :status order by f.id asc limit :size")
    List<Files> loadSpecialStatusFiles(Long minId, int size, short status);

    @Query("select f from Files f where f.deleted = true and f.deleteAt < :endTime and f.id > :minId and f.removed is null and f.storagePath is not null order by f.id asc limit :size")
    List<Files> loadToDeletePhysicalFiles(Long minId, int size, LocalDateTime endTime);

    @Query("select f from Files f where f.name = :name and f.deleted = false and f.id != :id and f.storagePath = :storagePath")
    List<Files> findSameNames(String name, Long id, String storagePath);

    @Query("select f from Files f where f.id > :minId and f.deleted = false and f.status = 5 order by f.id asc limit :size")
    List<Files> loadToUnzipFiles(Long minId, Integer size);

    @Modifying
    @Transactional
    @Query("update Files set foldId = :curFoldId where foldId = :id and userId = :userId and deleted = false")
    void changeFold(Long curFoldId, Long id, Integer userId);

    @Query("select f from Files f where f.id > :minId and f.deleted = true and f.removed = true order by f.id asc limit :size")
    List<Files> loadToDeleteFilesRecord(Long minId, Integer size);
}
