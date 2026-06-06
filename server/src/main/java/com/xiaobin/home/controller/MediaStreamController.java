package com.xiaobin.home.controller;

import com.xiaobin.home.dto.ApiResponse;
import com.xiaobin.home.dto.FtpDirsDTO;
import com.xiaobin.home.dto.login.UserFtpCache;
import com.xiaobin.home.entity.Files;
import com.xiaobin.home.entity.PlayHistory;
import com.xiaobin.home.repository.FilesDao;
import com.xiaobin.home.service.FileService;
import com.xiaobin.home.service.LoginService;
import com.xiaobin.home.service.PlayHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/media")
@Slf4j
public class MediaStreamController {

    @Autowired
    private LoginService loginService;
    @Autowired
    private FilesDao filesDao;
    @Autowired
    private PlayHistoryService playHistoryService;
    @Autowired
    private FileService fileService;

    @GetMapping("/play")
    public void streamMedia(
            @RequestParam("fileId") Long fileId,
            @RequestParam("token") String token,
            HttpServletRequest request, HttpServletResponse response) {
        UserFtpCache ftpCache = this.loginService.getFtpCache(token);
        int loginId = this.loginService.getLoginUser(token).getId();
        com.xiaobin.home.entity.Files files = this.filesDao.loadFileInFold(ftpCache.currentFoldId(), fileId, loginId);
        if (files == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        File mediaFile = new File(files.getStoragePath());
        if (!mediaFile.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        long fileLength = mediaFile.length();
//        String mimeType = Files.probeContentType(mediaFile.toPath());
//        if (mimeType == null) {
//            mimeType = "application/octet-stream";
//        }
//
//        response.setContentType(mimeType);
        response.setHeader("Accept-Ranges", "bytes");

        String range = request.getHeader("Range");
        long start = 0, end = fileLength - 1;

        if (range != null && range.startsWith("bytes=")) {
            // 示例：Range: bytes=500-999
            String[] ranges = range.replace("bytes=", "").split("-");
            try {
                start = Long.parseLong(ranges[0]);
                if (ranges.length > 1) {
                    end = Long.parseLong(ranges[1]);
                }
            } catch (NumberFormatException ignored) {
            }
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        }

        long contentLength = end - start + 1;
        response.setHeader("Content-Length", String.valueOf(contentLength));
        response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);

        try (RandomAccessFile raf = new RandomAccessFile(mediaFile, "r");
             OutputStream os = response.getOutputStream()) {

            raf.seek(start);
            byte[] buffer = new byte[8192];
            long remaining = contentLength;
            int len;

            while ((len = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining))) > 0) {
                os.write(buffer, 0, len);
                remaining -= len;
                if (remaining <= 0) break;
            }
            os.flush();
        } catch (IOException e) {
            log.info("视频播放异常: {}", e.getMessage());
        }
        PlayHistory history = new PlayHistory();
        history.setFoldId(files.getFoldId());
        history.setFileType(files.getFileType());
        history.setUserId(files.getUserId());
        history.setFoldPath(ftpCache.getFoldIds().stream().map(String::valueOf).collect(Collectors.joining(",")));
        history.setUpdateTime(LocalDateTime.now());
        history.setFileId(files.getId());
        history.setStart(start);
        history.setEnd(end);
        this.playHistoryService.replaceHistory(history);
    }

    @PostMapping("/next")
    public ApiResponse nextVideoId(@RequestBody Files reqFiles) {
        Files files = null;
        if (reqFiles.getId() != null) {
            UserFtpCache ftpCache = loginService.getFtpCache();
            Integer userId = this.loginService.getLoginId();
            Files curFiles = this.filesDao.loadById(reqFiles.getId(), userId);
            if (curFiles == null) {
                return ApiResponse.ok();
            }
            if (curFiles.getSort() != null) {
                files = this.filesDao.loadNextFileInFold(curFiles.getFoldId(), curFiles.getSort(), userId, curFiles.getFileType());
            } else {
                files = this.filesDao.loadNextFileInFold(curFiles.getFoldId(), curFiles.getId(), userId, curFiles.getFileType());
            }
        }
        return ApiResponse.ok(files);
    }

    @PostMapping("/previous")
    public ApiResponse previousVideoId(@RequestBody Files reqFiles) {
        Files files = null;
        if (reqFiles.getId() != null) {
            Integer userId = this.loginService.getLoginId();
            Files curFiles = this.filesDao.loadById(reqFiles.getId(), userId);
            if (curFiles == null) {
                return ApiResponse.ok();
            }
            if (curFiles.getSort() != null) {
                files = this.filesDao.loadPrevFileInFold(curFiles.getFoldId(), curFiles.getSort(), userId, curFiles.getFileType());
            } else {
                files = this.filesDao.loadPrevFileInFold(curFiles.getFoldId(), curFiles.getId(), userId, curFiles.getFileType());
            }
            log.info("files is null: {}, next files: {}", files == null, files == null ? 0L : files.getId());
        }
        return ApiResponse.ok(files);
    }

    @PostMapping("video-list")
    public ApiResponse videoList(@RequestBody Files reqFiles) {
        List<FtpDirsDTO> list = new ArrayList<>();
        if (reqFiles.getId() != null) {
            Integer userId = this.loginService.getLoginId();
            Files curFiles = this.filesDao.loadById(reqFiles.getId(), userId);
            if (curFiles == null) {
                return ApiResponse.ok();
            }
            List<Files> files = this.filesDao.findByFileTypeAndFoldIdAndDeletedFalse(curFiles.getFileType(), curFiles.getFoldId());
            this.fileService.formatFiles(files, list);
        }
        return ApiResponse.ok(list);
    }
}
