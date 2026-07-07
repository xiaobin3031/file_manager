package com.xiaobin.home.service;

import com.xiaobin.home.constant.FileStatusConstant;
import com.xiaobin.home.entity.ExpandFoldsAuto;
import com.xiaobin.home.entity.Files;
import com.xiaobin.home.entity.Folds;
import com.xiaobin.home.repository.ExpandFoldsAutoDao;
import com.xiaobin.home.repository.FilesDao;
import com.xiaobin.home.repository.FoldsDao;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ExpandFoldsAutoService {

    @Autowired
    private ExpandFoldsAutoDao expandFoldsAutoDao;
    @Autowired
    private FoldsDao foldsDao;
    @Autowired
    private FilesDao filesDao;
    @Autowired
    private FileService fileService;

    @PostConstruct
    public void autoExpand() {
        new Thread(() -> {
            log.info("auto expand start");
            List<ExpandFoldsAuto> list = this.expandFoldsAutoDao.findByExpandedFalse();
            if (!list.isEmpty()) {
                for (ExpandFoldsAuto expandFoldsAuto : list) {
                    if (log.isDebugEnabled()) {
                        log.debug("expand to fold: {}, expand to user id: {}", expandFoldsAuto.getExpandToFold(), expandFoldsAuto.getExpandUserId());
                    }
                    Folds folds = this.foldsDao.loadSpecialFoldsInFold(expandFoldsAuto.getExpandToFold(), expandFoldsAuto.getExpandUserId());
                    if (folds != null) {
                        try {
                            File file = new File(expandFoldsAuto.getStoragePath());
                            if (file.exists() && !file.isFile()) {
                                expandFolds(file, folds, expandFoldsAuto.getExpandUserId());
                            }
                        } catch (Exception e) {
                            log.error("扩展失败: {}", e.getMessage(), e);
                            continue;
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("folds is null");
                        }
                    }
                    expandFoldsAuto.setExpanded(true);
                    this.expandFoldsAutoDao.save(expandFoldsAuto);
                }
                this.fileService.createFileSample();
            }
        }).start();
    }

    private void expandFolds(File fold, Folds folds, Integer userId) {
        File[] files = fold.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isFile()) {
                Files storageFiles = this.filesDao.loadFilesByNameInFold(folds.getId(), file.getName(), userId);
                if (storageFiles == null) {
                    storageFiles = new Files();
                    storageFiles.setName(file.getName());
                    storageFiles.setUserId(userId);
                    storageFiles.setSize(file.length());
                    storageFiles.setFileType(this.fileService.getFileType(file.getName()));
                    // 太慢了，暂时不计算
//                    storageFiles.setHash(this.fileReadService.getFileHash(file, "SHA-256"));
                    storageFiles.setDeleted(false);
                    storageFiles.setCreateAt(LocalDateTime.now());
                    storageFiles.setUpdateAt(LocalDateTime.now());
                    storageFiles.setFoldId(folds.getId());
                    storageFiles.setStoragePath(file.getAbsolutePath());
                    storageFiles.setSampleStatus(0);
                    storageFiles.setStatus(FileStatusConstant.INIT);
                    this.filesDao.save(storageFiles);
                }
            } else if (!file.getName().startsWith(".")) {
                // 隐藏文件夹，跳过
                Folds newFolds = this.foldsDao.loadFoldsByNameInFold(file.getName(), folds.getId(), userId);
                if (newFolds == null) {
                    newFolds = new Folds();
                    newFolds.setName(file.getName());
                    newFolds.setParentId(folds.getId());
                    newFolds.setCreateAt(LocalDateTime.now());
                    newFolds.setUserId(userId);
                    newFolds.setDeleted(false);
                    newFolds.setFoldCount(0);
                    newFolds.setFileCount(0);
                    newFolds = this.foldsDao.save(newFolds);
                    if (newFolds.getId() == null) return;
                }
                expandFolds(file, newFolds, userId);
            }
        }
    }

}
