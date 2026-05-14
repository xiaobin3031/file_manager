package com.xiaobin.home.controller;

import com.xiaobin.home.dto.ApiResponse;
import com.xiaobin.home.dto.ImageShowDTO;
import com.xiaobin.home.dto.login.UserFtpCache;
import com.xiaobin.home.entity.Files;
import com.xiaobin.home.entity.PlayHistory;
import com.xiaobin.home.repository.FilesDao;
import com.xiaobin.home.service.FileService;
import com.xiaobin.home.service.LoginService;
import com.xiaobin.home.service.PlayHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

@RequestMapping("/image-preview")
@RestController
@Slf4j
public class ImagePreviewController {

    @Autowired
    private FilesDao filesDao;
    @Autowired
    private LoginService loginService;
    @Autowired
    private FileService fileService;
    @Autowired
    private PlayHistoryService playHistoryService;

    @PostMapping("/show")
    public ApiResponse showImage(@RequestBody ImageShowDTO dto) throws IOException {
        UserFtpCache ftpCache = loginService.getFtpCache();
        Files files = this.filesDao.loadFileInFold(ftpCache.currentFoldId(), dto.getFileId(), this.loginService.getLoginId());
        if (files == null || !this.fileService.isImage(files.getFileType())) {
            log.info("文件未找到：文件不存在或者，文件不是图片类型");
            return ApiResponse.ok();
        }

        return sendImage(files, ftpCache);
    }

    private ApiResponse sendImage(Files files, UserFtpCache ftpCache) throws IOException {
        File file = new File(files.getStoragePath());
        String img = "";
        if(file.exists() && file.isFile()) {
            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            img = Base64.getEncoder().encodeToString(bytes);
            ftpCache.setCurFileId(files.getId());
            PlayHistory history = new PlayHistory();
            history.setFoldId(files.getFoldId());
            history.setFileType(files.getFileType());
            history.setUserId(files.getUserId());
            history.setFoldPath(ftpCache.getFoldIds().stream().map(String::valueOf).collect(Collectors.joining(",")));
            history.setStatus((short)0);
            history.setUpdateTime(LocalDateTime.now());
            history.setFileId(files.getId());
            this.playHistoryService.replaceHistory(history);
        }
        return ApiResponse.ok(Map.of("sort", files.getSort(), "img", img, "fileId", files.getId(), "fileType", files.getFileType()));
    }

    @PostMapping("/next")
    public ApiResponse nextImage() throws IOException {
        UserFtpCache ftpCache = loginService.getFtpCache();
        Integer userId = this.loginService.getLoginId();
        Files curFiles = this.filesDao.loadById(ftpCache.getCurFileId(), userId);
        Files files;
        if (curFiles.getSort() != null) {
            files = this.filesDao.loadNextImageInFold(ftpCache.currentFoldId(), curFiles.getSort(), userId);
        } else {
            files = this.filesDao.loadNextImageInFold(ftpCache.currentFoldId(), ftpCache.getCurFileId(), userId);
        }
        if (files == null) {
            return ApiResponse.ok();
        }
        return sendImage(files, ftpCache);
    }

    @PostMapping("/prev")
    public ApiResponse prevImage() throws IOException {
        UserFtpCache ftpCache = loginService.getFtpCache();
        Integer userId = this.loginService.getLoginId();
        Files curFiles = this.filesDao.loadById(ftpCache.getCurFileId(), userId);
        Files files;
        if (curFiles.getSort() != null) {
            files = this.filesDao.loadPrevImageInFold(ftpCache.currentFoldId(), curFiles.getSort(), this.loginService.getLoginId());
        } else {
            files = this.filesDao.loadPrevImageInFold(ftpCache.currentFoldId(), ftpCache.getCurFileId(), this.loginService.getLoginId());
        }
        if (files == null) {
            return ApiResponse.ok();
        }
        return sendImage(files, ftpCache);
    }
}
