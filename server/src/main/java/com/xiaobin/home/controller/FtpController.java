package com.xiaobin.home.controller;

import com.xiaobin.home.constant.Constant;
import com.xiaobin.home.dto.*;
import com.xiaobin.home.dto.login.UserFtpCache;
import com.xiaobin.home.entity.FileDownloadPlan;
import com.xiaobin.home.exception.SimpleBizException;
import com.xiaobin.home.node.NodePermissionService;
import com.xiaobin.home.node.NodeService;
import com.xiaobin.home.node.entity.FmNode;
import com.xiaobin.home.orm.Orm;
import com.xiaobin.home.repository.FileDownloadPlanDao;
import com.xiaobin.home.repository.FilesDao;
import com.xiaobin.home.repository.FoldsDao;
import com.xiaobin.home.repository.PlayHistoryDao;
import com.xiaobin.home.service.FileDownloadPlanService;
import com.xiaobin.home.service.FileDownloadService;
import com.xiaobin.home.service.FileService;
import com.xiaobin.home.service.LoginService;
import com.xiaobin.home.storage.StorageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
    @Autowired
    private NodeService nodeService;
    @Autowired
    private NodePermissionService nodePermissionService;
    @Autowired
    private Orm orm;
    @Autowired
    private StorageService storageService;

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
        if (ftpCache.isPhysicsFlag()) {
            File[] files = ftpCache.getPhysicsFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    list.add(new FtpDirsDTO(-1L, !file.isDirectory(), file.getName()));
                }
            }
        } else {
            long parentId = ftpCache.currentFoldId();
            FmNode parent = new FmNode();
            parent.setId(parentId);
            parent.setOwnerId(ftpCache.getLoginId());
            List<FmNode> children = this.nodeService.children(parent);
            children = this.nodePermissionService.filterPermissions(children, ftpCache.getLoginId());
            children.sort((o1, o2) -> {
                int i = o2.getNodeType().compareTo(o1.getNodeType());
                if (i == 0) {
                    if (o1.getSort() != null && o2.getSort() != null) {
                        i = o1.getSort().compareTo(o2.getSort());
                    } else {
                        i = o1.getId().compareTo(o2.getId());
                    }
                }
                return i;
            });
            for (FmNode child : children) {
                FtpDirsDTO ftpDirsDTO = new FtpDirsDTO(child.getId(), Constant.isFile(child.getNodeType()), child.getNodeName());
                ftpDirsDTO.setSort(child.getSort());
                list.add(ftpDirsDTO);
            }
        }
        ListDirDTO result = new ListDirDTO();
        result.setPath(ftpCache.getPath());
        result.setFiles(list);
        // 查一下有没有进度
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
            FmNode fmNode = new FmNode();
            fmNode.setId(dto.getId());
            fmNode.setOwnerId(ftpCache.getLoginId());
            fmNode = this.orm.of(FmNode.class).buildQueryByEntity(fmNode).one();
            if (fmNode != null) {
                ftpCache.getFoldIds().addFirst(fmNode.getId());
                ftpCache.getPath().add(fmNode.getNodeName());
                ftpCache.setPhysicsFlag(false);
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
            FmNode fmNode = new FmNode();
            fmNode.setParentId(ftpCache.currentFoldId());
            fmNode.setId(dto.getId());
            fmNode.setOwnerId(ftpCache.getLoginId());
            fmNode = this.orm.of(FmNode.class).buildQueryByEntity(fmNode).one();
            if (fmNode != null && Constant.isFile(fmNode.getNodeType())) {
                return ApiResponse.ok(this.nodeService.generateToken(fmNode));
            }
        }

        return ApiResponse.error("file not found");
    }

    /**
     * 下载文件
     */
    @GetMapping("/downloadFile")
    public void downloadFile(@RequestParam("fileToken") String fileToken, HttpServletResponse response) throws IOException {
        FmNode fmNode = this.nodeService.getByToken(fileToken);
        if (fmNode == null) {
            return;
        }
        fmNode = this.nodePermissionService.checkPermission(fmNode, fmNode.getOwnerId());
        StorageLocation location = this.nodeService.location(fmNode);
        if (location == null) return;
        String mimeType = "application/octet-stream";

        // 设置响应头
        response.setContentType(mimeType);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(fmNode.getNodeName(), StandardCharsets.UTF_8) + "\"");
        response.setHeader("Content-Length", String.valueOf(this.storageService.size(location)));

        // 以流的方式写出文件内容
        try (InputStream inputStream = this.storageService.open(location); OutputStream out = response.getOutputStream()) {
            int read;
            byte[] bytes = new byte[10240];
            while ((read = inputStream.read(bytes)) != -1) {
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
                FmNode fmNode = new FmNode();
                fmNode.setNodeType(Constant.NODE_TYPE_FOLDER);
                fmNode.setNodeName(dto.getDirName());
                fmNode.setParentId(ftpCache.currentFoldId());
                fmNode.setOwnerId(ftpCache.getLoginId());
                fmNode = this.nodeService.create(fmNode);
                return ApiResponse.ok(fmNode.getId());
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
            this.fileService.uploadFile(foldId, file, ftpCache.getLoginId());
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
            int update = this.orm.ofUpdate(FmNode.class)
                    .set(FmNode::getDeleted, 1)
                    .in(FmNode::getId, dto.getFiles().stream().map(FtpDirsDTO::getId).toList())
                    .eq(FmNode::getParentId, ftpCache.currentFoldId())
                    .eq(FmNode::getOwnerId, ftpCache.getLoginId())
                    .update();
            if (update == 0) {
                return ApiResponse.error("batch error");
            }
            CompletableFuture.runAsync(this.fileService::signDeleteInDeletedFold);
        }
        return ApiResponse.ok();
    }

    @PostMapping("/rename")
    public ApiResponse rename(@RequestBody List<FtpRequestDTO> dto) {
        UserFtpCache ftpCache = this.loginService.getFtpCache();
        if (!ftpCache.isPhysicsFlag()) {
            if (dto.stream().anyMatch(a -> a.getId() == null || org.apache.commons.lang3.StringUtils.isEmpty(a.getNewName()))) {
                throw new SimpleBizException("文件id或者新名称为空");
            }
            this.fileService.batchRename(dto, ftpCache);
        }
        return listDirs();
    }

    @GetMapping("/foldByParentId")
    public ApiResponse foldByParentId(Long parentId) {
        FmNode parent = new FmNode();
        parent.setParentId(parentId);
        parent.setOwnerId(this.loginService.getLoginId());
        parent.setNodeType(Constant.NODE_TYPE_FOLDER);
        List<FmNode> folds = this.orm.of(FmNode.class).buildQueryByEntity(parent).list();
        return ApiResponse.ok(folds.stream().map(a -> new FtpDirsDTO(a.getId(), false, a.getNodeName())).toList());
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
            this.fileService.batchMoveFile(dto.getFiles(), dto.getFoldId(), ftpCache);
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
        FmNode fmNode = this.nodeService.getByToken(fileToken);
        if (fmNode == null || !this.fileService.isPdf(fmNode.getMimeType())) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        fmNode = this.nodePermissionService.checkPermission(fmNode, fmNode.getOwnerId());
        StorageLocation location = this.nodeService.location(fmNode);
        try (InputStream in = this.storageService.open(location)) {
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=" + fmNode.getNodeName());

            OutputStream out = response.getOutputStream();
            in.transferTo(out);
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
        UserFtpCache ftpCache = this.loginService.getFtpCache();
        FmNode fmNode = this.fileDownloadService.downloadFromMagnet(dto.getMagnet(), ftpCache.currentFoldId(), ftpCache.getLoginId());
        FtpDirsDTO ftpDirsDTO = new FtpDirsDTO(fmNode.getId(), true, fmNode.getNodeName());
        ftpDirsDTO.setFileType(fmNode.getMimeType());
        ftpDirsDTO.setSort(fmNode.getSort());
        return ApiResponse.ok(ftpDirsDTO);
    }

    @PostMapping("/addDownloadTorrent")
    public ApiResponse addDownloadTorrent(@RequestParam("file") MultipartFile file) {
        if (file == null) {
            return ApiResponse.error("请提供torrent文件");
        }
        FmNode fmNode = this.fileDownloadService.downloadFromTorrent(file, this.loginService.getLoginId(), this.loginService.getFtpCache().currentFoldId());
        FtpDirsDTO ftpDirsDTO = new FtpDirsDTO(fmNode.getId(), true, file.getName());
        ftpDirsDTO.setSort(fmNode.getSort());
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
