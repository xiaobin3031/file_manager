package com.xiaobin.home.controller;

import com.xiaobin.home.config.FtpConfig;
import com.xiaobin.home.constant.FileStatusConstant;
import com.xiaobin.home.dto.ApiResponse;
import com.xiaobin.home.dto.FileUploadDTO;
import com.xiaobin.home.dto.login.UserFtpCache;
import com.xiaobin.home.entity.FileUploadProgress;
import com.xiaobin.home.entity.Files;
import com.xiaobin.home.entity.Folds;
import com.xiaobin.home.repository.FileUploadProgressDao;
import com.xiaobin.home.repository.FilesDao;
import com.xiaobin.home.repository.FoldsDao;
import com.xiaobin.home.service.FileService;
import com.xiaobin.home.service.FileUploadService;
import com.xiaobin.home.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@RequestMapping("/file-upload")
@RestController
@Slf4j
public class FileUploadController {

    private static final Map<String, FileUploadProgress> progressMap = new ConcurrentHashMap<>();

    @Autowired
    private FileUploadProgressDao fileUploadProgressDao;
    @Autowired
    private FtpConfig ftpConfig;
    @Autowired
    private LoginService loginService;
    @Autowired
    private FoldsDao foldsDao;
    @Autowired
    private FileService fileService;
    @Autowired
    private FileUploadService fileUploadService;
    @Autowired
    private FilesDao filesDao;

    @PostMapping("/init")
    public ApiResponse initUpload(@RequestBody FileUploadDTO dto) {
        UserFtpCache ftpCache = this.loginService.getFtpCache();
        if (ftpCache.currentFoldId() == 0L || ftpCache.isPhysicsFlag()) {
            return ApiResponse.error("暂无权限");
        }
        Integer userId = this.loginService.getLoginId();
        Files files = this.filesDao.loadFilesByNameInFold(ftpCache.currentFoldId(), dto.getFileName(), userId);
        if(files == null) {
            files = this.fileService.buildFiles(ftpCache.currentFoldId(),
                    dto.getFileName(),
                    dto.getTotalSize(),
                    this.ftpConfig.getRootPath() + File.separator + dto.getFileName(),
                    this.loginService.getLoginId());
            files.setStatus(FileStatusConstant.UPLOAD);
            this.filesDao.save(files);
        }else if(FileStatusConstant.isLocked(files.getStatus())){
            return ApiResponse.error("文件正在被其他操作锁定: " + FileStatusConstant.getStatusName(files.getStatus()));
        }
        String uploadId = files.getId() + "_fold";
        FileUploadProgress progress = this.fileUploadProgressDao.findOneByUploadId(uploadId);
        if (progress == null) {
            progress = new FileUploadProgress();
            progress.setStoreFlag(false);
            progress.setUploadId(uploadId);
            progress.setAddTime(LocalDateTime.now());
            progress.setTotalChunk(dto.getTotalChunks());
            progress.setCurrentChunk(0L);
            this.fileUploadProgressDao.save(progress);
        }
        progressMap.put(uploadId, progress);
        return ApiResponse.ok(progress);
    }

    @PostMapping("/upload")
    public ApiResponse uploadChunk(
            @RequestParam("fileId") String fileId,
            @RequestParam("currentChunk") Long currentChunk,
            @RequestParam("file") MultipartFile file) {

        this.loginService.getLoginId();
        FileUploadProgress progress = progressMap.get(fileId);
        if (progress == null || Boolean.TRUE.equals(progress.getStoreFlag())) {
            return ApiResponse.error("文件不存在");
        }
        if (progress.getCurrentChunk() > currentChunk) {
            return ApiResponse.ok("分片已上传");
        }

        // 创建临时目录
        File tmpDir = new File(this.ftpConfig.getTmpPath());
        if (!tmpDir.exists()) {
            return ApiResponse.error("目录不存在");
        }
        File dir = new File(tmpDir, fileId);
        if (!dir.exists() && !dir.mkdirs()) {
            return ApiResponse.error("创建目录失败");
        }

        // 保存当前分片
        File chunkFile = new File(dir, currentChunk + ".part");
        if (!chunkFile.exists()) {
            try {
                file.transferTo(chunkFile);
            } catch (IOException e) {
                return ApiResponse.error("保存分片失败");
            }
        }
        if (currentChunk % 50 == 0 || currentChunk == progress.getTotalChunk() - 1) {
            progress.setCurrentChunk(currentChunk);
            this.fileUploadProgressDao.save(progress);
        }

        return ApiResponse.ok("分片上传成功: " + currentChunk);
    }

    @PostMapping("/finish")
    public ApiResponse finish(@RequestBody FileUploadDTO dto) {
        UserFtpCache ftpCache = this.loginService.getFtpCache();
        Integer loginId = this.loginService.getLoginId();
        FileUploadProgress progress = progressMap.get(dto.getFileId());
        if (progress == null) {
            return ApiResponse.error("文件不存在");
        }
        Folds folds = this.foldsDao.loadSpecialFoldsInFold(ftpCache.currentFoldId(), loginId);
        if (folds == null) {
            return ApiResponse.error("文件夹不存在");
        }

        CompletableFuture.runAsync(() -> this.fileUploadService.finishUpload(progress, loginId, folds.getId()));

        return ApiResponse.ok("正在处理，请稍后再查询结果");
    }
}
