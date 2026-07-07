package com.xiaobin.home.controller;

import com.xiaobin.home.dto.*;
import com.xiaobin.home.dto.login.UserFtpCache;
import com.xiaobin.home.entity.FileDownloadPlan;
import com.xiaobin.home.entity.Files;
import com.xiaobin.home.entity.Folds;
import com.xiaobin.home.entity.PlayHistory;
import com.xiaobin.home.repository.FileDownloadPlanDao;
import com.xiaobin.home.repository.FilesDao;
import com.xiaobin.home.repository.FoldsDao;
import com.xiaobin.home.repository.PlayHistoryDao;
import com.xiaobin.home.service.FileDownloadPlanService;
import com.xiaobin.home.service.FileDownloadService;
import com.xiaobin.home.service.FileService;
import com.xiaobin.home.service.LoginService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RequestMapping("/ftp")
@RestController
@Slf4j
public class FtpController {

    @Autowired
    private FileService fileService;
    @Autowired
    private FoldsDao foldsDao;
    @Autowired
    private FilesDao filesDao;
    @Autowired
    private LoginService loginService;
    @Autowired
    private FileDownloadService fileDownloadService;
    @Autowired
    private FileDownloadPlanDao fileDownloadPlanDao;
    @Autowired
    private FileDownloadPlanService fileDownloadPlanService;
    @Autowired
    private PlayHistoryDao playHistoryDao;

    @GetMapping("/goBack")
    public ApiResponse goBack() {
        UserFtpCache ftpCache = this.loginService.getFtpCache();
        if (ftpCache.isPhysicsFlag()) {
            if (ftpCache.getPhysicsFile().getAbsolutePath().startsWith(ftpCache.getPhysicsRootPath())) {
                if (ftpCache.getPhysicsFile().getAbsolutePath().equals(ftpCache.getPhysicsRootPath())) {
                    ftpCache.setPhysicsFlag(false);
                    ftpCache.setPhysicsRootPath(null);
                    ftpCache.setPhysicsFile(null);
                    ftpCache.setPath(new ArrayList<>());
                    ftpCache.setFoldIds(new ArrayList<>());
                } else {
                    ftpCache.setPhysicsFile(ftpCache.getPhysicsFile().getParentFile());
                }
            }
        } else if (!ftpCache.getFoldIds().isEmpty()) {
            ftpCache.getFoldIds().removeFirst();
            ftpCache.getPath().removeLast();
        }
        return this.listDirs();
    }

    /**
     * 获取当前目录下的所有文件夹
     */
    @GetMapping("/listDirs")
    public ApiResponse listDirs() {
        UserFtpCache ftpCache = this.loginService.getFtpCache();
        List<FtpDirsDTO> list = new ArrayList<>();
        FtpDirsDTO lastFiles = null;
        if (ftpCache.isPhysicsFlag()) {
            File[] files = ftpCache.getPhysicsFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    list.add(new FtpDirsDTO(-1L, !file.isDirectory(), file.getName()));
                }
            }
        } else {
            long foldId = ftpCache.getFoldIds().isEmpty() ? 0L : ftpCache.getFoldIds().getFirst();
            List<Folds> folds = this.foldsDao.loadFolds(foldId, this.loginService.getLoginId());
            folds.sort(Comparator.comparing(a -> Objects.requireNonNullElse(a.getSort(), 0)));
            for (Folds fold : folds) {
                FtpDirsDTO ftpDirsDTO = new FtpDirsDTO(fold.getId(), false, fold.getName());
                ftpDirsDTO.setSort(fold.getSort());
                ftpDirsDTO.setFoldCount(fold.getFoldCount());
                ftpDirsDTO.setFileCount(fold.getFileCount());
                list.add(ftpDirsDTO);
            }
            if (foldId > 0L) {
                List<Files> files = filesDao.findFilesByFoldIdAndUserIdAndDeletedFalse(foldId, this.loginService.getLoginId());
                this.fileService.formatFiles(files, list);
            }
            if (!ftpCache.getFoldIds().isEmpty()) {
                PlayHistory playHistory = this.playHistoryDao.findOneByFoldIdAndUserIdAndStatus(ftpCache.getFoldIds().getFirst(), this.loginService.getLoginId(), (short) 0);
                if (log.isDebugEnabled()) {
                    log.debug("playHistory: {}, foldId: {}", playHistory == null ? 0L : playHistory.getId(), ftpCache.getFoldIds().getFirst());
                }
                if (playHistory != null && playHistory.getFileId() != null) {
                    Files files = this.filesDao.loadById(playHistory.getFileId(), this.loginService.getLoginId());
                    if (log.isDebugEnabled()) {
                        log.debug("files is null: {}, files.id: {}", files == null, playHistory.getStart());
                    }
                    if (files != null) {
                        lastFiles = new FtpDirsDTO(files.getId(), true, files.getName());
                        lastFiles.setFileType(files.getFileType());
                        lastFiles.setSort(files.getSort());
                        lastFiles.setStart(playHistory.getStart());
                        lastFiles.setEnd(playHistory.getEnd());
                    }
                }
            }
        }
        ListDirDTO result = new ListDirDTO();
        result.setPath(ftpCache.getPath());
        result.setFiles(list);
        // 查一下有没有进度
        result.setLastFile(lastFiles);
        result.setCurrentFoldId(ftpCache.currentFoldId());
        return ApiResponse.ok(result);
    }

    /**
     * 调整目录
     *
     * @return 当前目录
     */
    @PostMapping("changeDir")
    public ApiResponse changeDir(@RequestBody FtpRequestDTO dto) {
        UserFtpCache ftpCache = this.loginService.getFtpCache();
        if (ftpCache.isPhysicsFlag()) {
            assert (dto.getFilename() != null && !dto.getFilename().isEmpty());
            File[] files = ftpCache.getPhysicsFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().equals(dto.getFilename())) {
                        ftpCache.setPhysicsFile(file);
                        ftpCache.setCurFileId(null);
                        return listDirs();
                    }
                }
            }
        } else {
            if (dto.getFoldId() == null && dto.getDirName() != null) {
                int idx = -1;
                List<String> pathList = ftpCache.getPath().reversed();
                log.info("当前路径: {}", pathList);
                for (int i = 0; i < pathList.size(); i++) {
                    String path = pathList.get(i);
                    if (path.equals(dto.getDirName())) {
                        idx = i;
                        break;
                    }
                }
                if (idx == -1) {
                    return ApiResponse.error("指定的目录不存在");
                }
                ftpCache.setPath(pathList.subList(idx, ftpCache.getPath().size()).reversed());
                ftpCache.setFoldIds(ftpCache.getFoldIds().subList(idx, ftpCache.getFoldIds().size()));
                return listDirs();
            }
            Folds fold = this.foldsDao.loadSpecialFoldsInFold(dto.getId(), this.loginService.getLoginId());
            if (fold != null) {
                ftpCache.getFoldIds().addFirst(fold.getId());
                ftpCache.getPath().add(fold.getName());
                ftpCache.setPhysicsFlag(Boolean.TRUE.equals(fold.getPhysics()));
                if (ftpCache.isPhysicsFlag()) {
                    ftpCache.setPhysicsRootPath(fold.getPath());
                    ftpCache.setPhysicsFile(new File(fold.getPath()));
                }
                ftpCache.setCurFileId(null);
                return listDirs();
            }
        }
        return ApiResponse.error("路径不存在");
    }

    @PostMapping("/prepareFile")
    public ApiResponse prepareFile(@RequestBody FtpRequestDTO dto) {
        UserFtpCache ftpCache = this.loginService.getFtpCache();
        if (!ftpCache.isPhysicsFlag()) {
            Files files = this.filesDao.loadFileInFold(ftpCache.currentFoldId(), dto.getId(), this.loginService.getLoginId());
            File file = new File(files.getStoragePath());
            if (file.exists()) {
                return ApiResponse.ok(this.fileService.generateFileDownloadToken(dto.isPrepareForPlay(), files, this.loginService.getLoginId()));
            }
        }

        return null;
    }

    /**
     * 下载文件
     */
    @GetMapping("/downloadFile")
    public void downloadFile(@RequestParam("fileToken") String fileToken, HttpServletResponse response) throws IOException {
        Files files = this.fileService.readFileFromCache(fileToken);
        File file = new File(files.getStoragePath());
        String mimeType = java.nio.file.Files.probeContentType(file.toPath());
        if (mimeType == null) mimeType = "application/octet-stream";

        // 设置响应头
        response.setContentType(mimeType);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(files.getName(), StandardCharsets.UTF_8) + "\"");
        response.setHeader("Content-Length", String.valueOf(java.nio.file.Files.size(file.toPath())));

        // 以流的方式写出文件内容
        try(InputStream inputStream = new FileInputStream(file);OutputStream out = response.getOutputStream()) {
            int read;
            byte[] bytes = new byte[10240];
            while( (read = inputStream.read(bytes)) != -1 ) {
                out.write(bytes, 0, read);
            }
            out.flush();
        }
    }

    @PostMapping("/addFold")
    public ApiResponse addFold(@RequestBody FtpRequestDTO dto) {
        if (StringUtils.hasText(dto.getDirName())) {
            UserFtpCache ftpCache = this.loginService.getFtpCache();
            if (!ftpCache.isPhysicsFlag()) {
                Long foldId = this.fileService.addFold(dto.getDirName(), ftpCache.currentFoldId(), this.loginService.getLoginId()).getId();
                return ApiResponse.ok(foldId);
            }
        }
        return ApiResponse.error("目录名称为空");
    }

    @PostMapping("/uploadFile")
    public ApiResponse uploadFile(@RequestParam("file") MultipartFile file, @RequestParam(value = "foldId", required = false) Long foldId) {
        UserFtpCache ftpCache = this.loginService.getFtpCache();
        if (!ftpCache.isPhysicsFlag()) {
            if (foldId == null) {
                foldId = ftpCache.currentFoldId();
            }
            File foldDir = new File(this.fileService.getRootPath(), String.valueOf(foldId));
            if (!foldDir.exists() && !foldDir.mkdir()) {
                return ApiResponse.error("文件夹创建失败");
            }
            File targetFile = new File(foldDir, Objects.requireNonNullElse(file.getOriginalFilename(), UUID.randomUUID().toString()));
            if (!targetFile.exists()) {
                try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                    fos.write(file.getBytes());
                    fos.flush();
                } catch (IOException e) {
                    log.error("upload file error", e);
                    return null;
                }
            }
            this.fileService.addFile(foldId, targetFile, this.loginService.getLoginId());
        }
        return this.listDirs();
    }

    @PostMapping("/removeFile")
    public ApiResponse removeFile(@RequestBody FtpRequestDTO dto) {
        UserFtpCache ftpCache = this.loginService.getFtpCache();
        if (!ftpCache.isPhysicsFlag()) {
            if (dto.getId() != null && dto.getId() > 0) {
                dto.setFiles(new ArrayList<>());
                dto.getFiles().add(new FtpDirsDTO(dto.getId(), dto.isFileFlag(), ""));
            }
            this.fileService.batchRemoveFile(dto.getFiles(), ftpCache.currentFoldId(), this.loginService.getLoginId());
        }
        return ApiResponse.ok();
    }

    @PostMapping("/rename")
    public ApiResponse rename(@RequestBody List<FtpRequestDTO> dto) {
        UserFtpCache ftpCache = this.loginService.getFtpCache();
        if (!ftpCache.isPhysicsFlag()) {
            this.fileService.batchRename(dto, ftpCache.currentFoldId(), this.loginService.getLoginId());
        }
        return listDirs();
    }

    @GetMapping("/foldByParentId")
    public ApiResponse foldByParentId(Long parentId) {
        List<Folds> folds = this.foldsDao.loadFolds(parentId, this.loginService.getLoginId());
        return ApiResponse.ok(folds.stream().map(a -> new FtpDirsDTO(a.getId(), false, a.getName())).toList());
    }

    @PostMapping("/moveFile")
    public ApiResponse moveFile(@RequestBody FtpRequestDTO dto) {
        UserFtpCache ftpCache = this.loginService.getFtpCache();
        Integer userId = this.loginService.getLoginId();
        if (ftpCache.isPhysicsFlag()) {
            log.info("move physics file");
            this.fileService.batchMovePhysicsFile(dto.getFiles(), dto.getFoldId(), ftpCache, userId);
        } else {
            log.info("move normal file");
            this.fileService.batchMoveFile(dto.getFiles(), dto.getFoldId(), ftpCache, userId);
        }
        return this.listDirs();
    }

    @PostMapping("/sortFiles")
    public ApiResponse sortFiles(@RequestBody List<FtpDirsDTO> dtos) {
        this.fileService.sortFiles(dtos, this.loginService.getFtpCache());
        return ApiResponse.ok();
    }

    @PostMapping("/unzipFile")
    public ApiResponse unzipFile(@RequestBody List<Long> fileIds) {
        UserFtpCache ftpCache = this.loginService.getFtpCache();
        this.fileService.unzip(fileIds, ftpCache.currentFoldId(), this.loginService.getLoginId());
        this.fileService.doUnZip();
        return ApiResponse.ok();
    }

    @GetMapping("/previewPdf/{fileToken}")
    public void previewPdf(@PathVariable String fileToken, HttpServletResponse response) {
        Files files = this.fileService.readFileFromCache(fileToken);
        if (files == null || !this.fileService.isPdf(files.getFileType())) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        File pdfFile = new File(files.getStoragePath());
        if (!pdfFile.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try (FileInputStream in = new FileInputStream(pdfFile)) {
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=" + pdfFile.getName());

            OutputStream out = response.getOutputStream();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @PostMapping("/addDownload")
    public ApiResponse addDownload(@RequestBody DownloadFileDTO dto) {
        if (!StringUtils.hasText(dto.getMagnet())) {
            return ApiResponse.error("请提供下载地址");
        }
        Files file = this.fileDownloadService.downloadFromMagnet(dto.getMagnet(), this.loginService.getLoginId(), this.loginService.getFtpCache().currentFoldId());
        FtpDirsDTO ftpDirsDTO = new FtpDirsDTO(file.getId(), true, file.getName());
        ftpDirsDTO.setFileType(file.getFileType());
        ftpDirsDTO.setSort(file.getSort());
        return ApiResponse.ok(ftpDirsDTO);
    }

    @PostMapping("/addDownloadTorrent")
    public ApiResponse addDownloadTorrent(@RequestParam("file") MultipartFile file) {
        if (file == null) {
            return ApiResponse.error("请提供torrent文件");
        }
        Files files = this.fileDownloadService.downloadFromTorrent(file, this.loginService.getLoginId(), this.loginService.getFtpCache().currentFoldId());
        FtpDirsDTO ftpDirsDTO = new FtpDirsDTO(files.getId(), true, file.getName());
        ftpDirsDTO.setFileType(files.getFileType());
        ftpDirsDTO.setSort(files.getSort());
        return ApiResponse.ok(ftpDirsDTO);
    }

    @PostMapping("/addDownloadPlan")
    public ApiResponse addDownloadPlan(@RequestBody DownloadPlanDTO dto) {
        Integer loginId = this.loginService.getLoginId();
        Long foldId = this.loginService.getFtpCache().currentFoldId();
        FileDownloadPlan plan = this.fileDownloadPlanDao.findOneByUrlAndDeletedFalseAndUserIdAndFoldId(dto.getUrl(), loginId, foldId);
        if (plan == null) {
            plan = new FileDownloadPlan();
            plan.setType(0);
            plan.setUrl(dto.getUrl());
            plan.setXpathExpression(dto.getXpathExpression());
            plan.setFinish(false);
            plan.setFoldId(foldId);
            plan.setDeleted(false);
            plan.setUserId(loginId);
            this.fileDownloadPlanDao.save(plan);
            FileDownloadPlan finalPlan = plan;
            CompletableFuture.runAsync(() -> {
                try {
                    this.fileDownloadPlanService.planDownload(finalPlan);
                } catch (ParserConfigurationException | XPathExpressionException e) {
                    log.error("下载计划执行失败", e);
                }
            });
            return ApiResponse.ok();
        }

        return ApiResponse.error("该下载计划已存在");
    }

    @GetMapping("/loadHtmlText")
    public ApiResponse loadHtmlText(@RequestParam("url") String url) {
        this.loginService.getLoginId();
        String html = this.fileDownloadPlanService.loadHtmlFromUrl(url);
        return ApiResponse.ok(html);
    }

    @PostMapping("/sortFilesByNameAsc")
    public ApiResponse sortFilesByNameAsc() {
        this.fileService.sortFilesByNameAsc();
        return this.listDirs();
    }

    @PostMapping("/migrateFiles")
    public ApiResponse migrateFiles() {
        return ApiResponse.ok();
    }

    /**
     * 这是给外部接口用的，暂时获取不到登录信息
     */
    @PostMapping("/addTmpFile")
    public ApiResponse addTmpFile(@RequestBody TmpFileDTO dto) {
        if (!CollectionUtils.isEmpty(dto.getItems()) && !CollectionUtils.isEmpty(dto.getFoldNames())) {
            this.fileService.addTmpFile(dto, 1);
            CompletableFuture.runAsync(() -> this.fileDownloadService.downloadDirectBatch());
        }
        return ApiResponse.ok();
    }

    @PostMapping("/createCbz")
    public ApiResponse createCbz(@RequestBody List<Long> fileIds) {
        this.fileService.createCbz(fileIds);
        return ApiResponse.ok();
    }
}
