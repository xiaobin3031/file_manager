package com.xiaobin.home.service;

import com.xiaobin.home.config.FtpConfig;
import com.xiaobin.home.constant.FileStatusConstant;
import com.xiaobin.home.dto.FtpDirsDTO;
import com.xiaobin.home.dto.FtpRequestDTO;
import com.xiaobin.home.dto.TmpFileDTO;
import com.xiaobin.home.dto.login.UserFtpCache;
import com.xiaobin.home.entity.DictManager;
import com.xiaobin.home.entity.Files;
import com.xiaobin.home.entity.Folds;
import com.xiaobin.home.exception.BizException;
import com.xiaobin.home.exception.SimpleBizException;
import com.xiaobin.home.repository.DictManagerDao;
import com.xiaobin.home.repository.FilesDao;
import com.xiaobin.home.repository.FoldsDao;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;

@Service
@Slf4j
public class FileService {
    private static final Map<String, byte[]> fileCache = new HashMap<>();
    private static final Map<String, Files> fileTokenCache = new HashMap<>();
    private static final Map<Integer, String> userFileCache = new HashMap<>();
    private static final List<Long> toUnzipFiles = new LinkedList<>();
    private static final Lock lock = new ReentrantLock();
    private static DictManager activeDictManager;

    private static boolean unzipFlag = false;

    @Autowired
    private FilesDao filesDao;
    @Autowired
    private FoldsDao foldsDao;
    @Autowired
    private FtpConfig ftpConfig;
    @Autowired
    private LoginService loginService;
    @Autowired
    private DictManagerDao dictManagerDao;
    @Autowired
    private Environment environment;

    @PostConstruct
    public void fillFileType() {
        String[] profiles = environment.getActiveProfiles();
        for (String profile : profiles) {
            if ("dev".equals(profile)) {
                return;
            }
        }
        List<DictManager> all = this.dictManagerDao.findAll();
        for (DictManager dictManager : all) {
            if (Boolean.TRUE.equals(dictManager.getActive())) {
                activeDictManager = dictManager;
                break;
            }
        }
        if (activeDictManager == null) {
            throw new SimpleBizException("当前没有生效的磁盘，请先维护");
        }
        Thread.ofVirtual().start(() -> {

            signDeleteInDeletedFold();

            // 设置fileType
            log.info("设置文件的fileType");
            PageQueryService<Files> pageQueryService = new PageQueryService<>();
            pageQueryService.setMapper(this.filesDao::pageFetchFileTypeNull);
            pageQueryService.setIdMapper((file, minId) -> {
                String fileType = this.getFileType(file.getName());
                if (fileType != null) {
                    file.setFileType(fileType);
                    this.filesDao.save(file);
                }
                return Math.max(minId, file.getId());
            });
            pageQueryService.query();
            log.info("设置文件的fileType 结束");

            createFileSample();
        });

        Thread.ofVirtual().start(() -> {
            log.info("将文件迁移到指定目录: {}", this.getRootPath());
            PageQueryService<Files> pageQueryService = new PageQueryService<>();
            pageQueryService.setMapper(this.filesDao::loadToMoveFiles);
            pageQueryService.setIdMapper((file, minId) -> {
                String targetPath = file.getStoragePath().replaceAll("^/home/xiaobin/Downloads", this.getRootPath());
                File targetFile = new File(targetPath);
                if (!targetFile.exists()) {
                    File parentFile = targetFile.getParentFile();
                    if (!parentFile.exists()) {
                        if (!parentFile.mkdirs()) {
                            log.error("创建目录失败: {}", parentFile.getAbsolutePath());
                        }
                    }
                    log.info("准备复制: [{}] -> [{}]", file.getStoragePath(), targetFile.getAbsolutePath());
                    try {
                        java.nio.file.Files.copy(new File(file.getStoragePath()).toPath(), targetFile.toPath());
                    } catch (IOException e) {
                        log.error("复制文件失败, file.id: {}", file.getId(), e);
                    }
                    log.info("复制结束");
                }
                file.setStoragePath(targetFile.getAbsolutePath());
                this.filesDao.save(file);
                return Math.max(minId, file.getId());
            });
            pageQueryService.query();
            log.info("文件迁移结束");

        });

        Thread.ofVirtual().start(() -> {
            log.info("开始处理压缩包");
            PageQueryService<Files> pageQueryService = new PageQueryService<>();
            pageQueryService.setMapper(this.filesDao::loadToUnzipFiles);
            pageQueryService.setIdMapper((file, minId) -> {
                toUnzipFiles.add(file.getId());
                return Math.max(minId, file.getId());
            });
            pageQueryService.setQueryEnd(a -> doUnZip());
            pageQueryService.query();
            log.info("处理压缩包结束");

        });
    }

    public void createFileSample() {
        log.info("准备生成视频的缩率图");
        PageQueryService<Files> pageQueryService = new PageQueryService<>();
        pageQueryService.setMapper(this.filesDao::pageNoSample);
        pageQueryService.setIdMapper((file, minId) -> {
            this.saveSample(file);
            return Math.max(minId, file.getId());
        });
        pageQueryService.query();
        log.info("生成视频缩略图结束");
    }

    public void saveSample(Files file) {
        File sampleFile = this.previewImage(file);
        if (sampleFile.exists()) {
            file.setSampleStatus(1);
            this.filesDao.save(file);
        } else {
            if (isVideo(file.getFileType())) {
                saveVideoSample(sampleFile, file);
            } else if (isImage(file.getFileType())) {
                resizeToPng(sampleFile, file.getStoragePath(), file);
            } else if (isPdf(file.getFileType())) {
                savePdfSample(sampleFile, file);
            }
        }
    }

    private void savePdfSample(File output, Files files) {
        File file = new File(files.getStoragePath());
        try (PDDocument document = PDDocument.load(file)) {
            // 加载 PDF 文件

            // 渲染 PDF 页面为图像
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 100); // 0 表示第一页，300 DPI 高质量

            File tmpFile = java.nio.file.Files.createTempFile(files.getName(), "png").toFile();
            // 保存为 PNG 文件
            ImageIO.write(bim, "PNG", tmpFile);
            this.resizeToPng(output, tmpFile.getAbsolutePath(), files);
        } catch (Exception e) {
            log.error("从pdf[{}]中获取图片失败: {}", files.getStoragePath(), e.getMessage());
        }
    }

    /**
     * 将图片缩放并保存为 PNG 格式
     *
     * @param sampleFile 输出文件
     */
    public void resizeToPng(File sampleFile, String inputImage, Files file) {
        String ffmpegPath = "ffmpeg"; // 确保 ffmpeg 已添加到系统 PATH
        String outputImage = sampleFile.getAbsolutePath();

        try {
            log.info("准备生成缩略图...{}", inputImage);
            // 构造命令
            ProcessBuilder processBuilder = new ProcessBuilder(
                    ffmpegPath,
                    "-y",                       // 覆盖输出文件
                    "-i", inputImage,
                    "-vcodec", "libwebp",
                    "-lossless", "0",
                    "-vf", "scale=320:-1",
                    "-q:v", "60",
                    "-compression_level", "6",
                    outputImage
            );
            Process process = processBuilder.inheritIO().start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("缩略图生成成功: {}", outputImage);
                file.setSampleStatus(1);
            } else {
                file.setSampleStatus(2);
                log.error("ffmpeg 执行失败，退出码: {}", exitCode);
            }
            this.filesDao.save(file);
        } catch (IOException | InterruptedException e) {
            log.error("保存缩略图失败: {}", e.getMessage());
        }

    }

    private void saveVideoSample(File sampleFile, Files file) {
        String ffmpegPath = "ffmpeg"; // 确保 ffmpeg 已添加到系统 PATH
        String inputVideo = file.getStoragePath();
        String outputImage = sampleFile.getAbsolutePath();
        int timestampSeconds = 10; // 从第几秒提取帧

        try {
            log.info("准备生成缩略图...{}", inputVideo);
            // 构造命令
            ProcessBuilder processBuilder = new ProcessBuilder(
                    ffmpegPath,
                    "-y",                       // 覆盖输出文件
                    "-ss", String.valueOf(timestampSeconds),
                    "-i", inputVideo,
                    "-frames:v", "1",
                    "-vcodec", "libwebp",
                    "-lossless", "0",
                    "-vf", "scale=320:-1",
                    "-q:v", "60",
                    "-compression_level", "6",
                    outputImage
            );
            Process process = processBuilder.inheritIO().start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("缩略图生成成功: {}", outputImage);
                file.setSampleStatus(1);
            } else {
                file.setSampleStatus(2);
                log.error("ffmpeg 执行失败，退出码: {}", exitCode);
            }
            this.filesDao.save(file);
        } catch (IOException | InterruptedException e) {
            log.error("保存缩略图失败: {}", e.getMessage());
        }
    }

    public byte[] readFile(File file) {
        if (file == null || !file.isFile() || !file.exists()) return null;

        byte[] bytes = fileCache.get(file.getAbsolutePath());
        if (bytes == null) {
            bytes = readFileInner(file);
        }
        return bytes;
    }

    private synchronized byte[] readFileInner(File file) {
        byte[] bytes = fileCache.get(file.getAbsolutePath());
        if (bytes == null) {
            try (FileInputStream fis = new FileInputStream(file)) {
                bytes = new byte[(int) file.length()];
                if (fis.read(bytes) == -1) {
                    bytes = null;
                } else {
                    fileCache.put(file.getAbsolutePath(), bytes);
                }
            } catch (IOException e) {
                log.error("读取文件[{}]失败: {}", file.getAbsolutePath(), e.getMessage(), e);
                bytes = null;
            }
        }
        return bytes;
    }

    public String getFileType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "application/octet-stream"; // 默认二进制流
        }

        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        return switch (ext) {
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "ogg" -> "video/ogg";
            case "mov" -> "video/quicktime";
            case "avi" -> "video/x-msvideo";
            case "mkv" -> "video/x-matroska";
            case "flv" -> "video/x-flv";
            case "wmv" -> "video/x-ms-wmv";
            case "3gp" -> "video/3gpp";
            case "ts" -> "video/mp2t";
            case "m4v" -> "video/x-m4v";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "flac" -> "audio/flac";
            case "aac" -> "audio/aac";
            case "m4a" -> "audio/mp4";
            case "amr" -> "audio/amr";
            case "opus" -> "audio/opus";
            case "rmvb" -> "application/vnd.rn-realmedia-vbr";
            default -> URLConnection.guessContentTypeFromName(filename); // 未知类型
        };
    }

    public String getFileHash(File file) {
        return getFileHash(file, "SHA-256");
    }

    public String getFileHash(File file, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            InputStream fis = new FileInputStream(file);

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            fis.close();

            byte[] hashBytes = digest.digest();

            // 转为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public String generateFileDownloadToken(boolean prepareForPlay, Files files, Integer userId) {
        String token = userFileCache.get(userId);
        String originToken = token;
        if (token != null) {
            if (!prepareForPlay) {
                throw new SimpleBizException("当前文件下载中");
            }
            Files files1 = fileTokenCache.get(token);
            if (files1 != null && !Objects.equals(files1.getId(), files.getId())) {
                token = null;
            }
        }
        if (token == null) {
            token = UUID.randomUUID().toString().replaceAll("-", "");
            userFileCache.put(userId, token);
            fileTokenCache.put(token, files);
            if (originToken != null) {
                fileTokenCache.remove(originToken);
            }
        }
        return token;
    }

    public Files readFileFromCache(String token) {
        return fileTokenCache.get(token);
    }

    /**
     * 不能转移到物理目录
     */
    @Transactional
    public void batchMoveFile(List<FtpDirsDTO> ftpFiles, Long toFoldId, UserFtpCache ftpCache, Integer userId) {
        if (ftpFiles != null && !ftpFiles.isEmpty()) {
            Long curFoldId = ftpCache.currentFoldId();
            for (FtpDirsDTO file : ftpFiles) {
                Folds targetFolds = this.foldsDao.findByIdAndUserIdAndDeletedFalse(toFoldId, userId);
                if (targetFolds == null) continue;

                if (file.isFileFlag()) {
                    Files files = this.filesDao.loadFileInFold(curFoldId, file.getId(), userId);
                    if (files != null && !Objects.equals(files.getFoldId(), toFoldId)) {
                        files.setFoldId(targetFolds.getId());
                        files.setUpdateAt(LocalDateTime.now());
                        files.setSampleStatus(0);
                        this.filesDao.save(files);
                        CompletableFuture.runAsync(() -> saveSample(files));
                    }
                } else {
                    Folds folds = this.foldsDao.loadFoldInFold(curFoldId, file.getId(), userId);
                    if (folds != null && !Objects.equals(folds.getParentId(), toFoldId)) {
                        folds.setParentId(targetFolds.getId());
                        this.foldsDao.save(folds);
                    }
                }
            }
        }
    }

    @Transactional
    public void batchMovePhysicsFile(List<FtpDirsDTO> ftpFiles, Long toFoldId, UserFtpCache ftpCache, Integer userId) {
        if (ftpFiles != null && !ftpFiles.isEmpty()) {
            Folds targetFolds = this.foldsDao.findByIdAndUserIdAndDeletedFalse(toFoldId, userId);
            log.info("target is null ? {}", targetFolds == null);
            if (targetFolds == null) return;

            File physicsFile = ftpCache.getPhysicsFile();
            File[] files = physicsFile.listFiles();
            log.info("physics files is empty? {}", files == null);
            if (files == null) return;

            for (FtpDirsDTO ftpFile : ftpFiles) {
                File srcFile = null;
                for (File file : files) {
                    if (file.getName().equals(ftpFile.getName())) {
                        srcFile = file;
                        break;
                    }
                }
                log.info("src file is null? {}", srcFile == null);
                if (srcFile == null) {
                    throw new BizException("file: " + ftpFile.getName() + " not found");
                }
                if (srcFile.isFile()) {
                    log.info("src file is file: {}", srcFile.getAbsolutePath());
                    movePhysicsFile(srcFile, toFoldId, userId);
                } else if (srcFile.isDirectory()) {
                    log.info("src file is fold: {}", srcFile.getAbsolutePath());
                    Long parentId = addFold(srcFile.getName(), toFoldId, userId).getId();
                    movePhysicsDir(parentId, srcFile, userId);
                }
            }
        }
    }

    private void movePhysicsFile(File srcFile, Long toFoldId, Integer userId) {
        File foldDir = new File(this.getRootPath(), String.valueOf(toFoldId));
        if (!foldDir.exists() && !foldDir.mkdir()) {
            return;
        }
        File targetFile = new File(foldDir, srcFile.getName());
        if (!targetFile.exists()) {
            try (FileInputStream fis = new FileInputStream(srcFile)) {
                try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                    byte[] bytes = new byte[4096];
                    int read;
                    while ((read = fis.read(bytes)) != -1) {
                        fos.write(bytes, 0, read);
                    }
                }
                addFile(toFoldId, targetFile, userId);
                log.info("move physics file success: {}", targetFile.getAbsolutePath());
            } catch (Exception e) {
                log.error("move file[{}] to [{}] error: {}", srcFile.getAbsolutePath(), targetFile.getAbsolutePath(), e.getMessage());
            }

        } else {
            addFile(toFoldId, targetFile, userId);
        }
    }

    public void movePhysicsDir(Long toFoldId, File srcFold, Integer userId) {
        if (toFoldId != null && toFoldId > 0) {
            File[] files = srcFold.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        movePhysicsFile(file, toFoldId, userId);
                    } else if (file.isDirectory()) {
                        Long parentId = addFold(file.getName(), toFoldId, userId).getId();
                        movePhysicsDir(parentId, file, userId);
                    }
                }
            }
        }
    }

    public void addFile(Long foldId, File targetFile, Integer userId) {
        Files files = this.filesDao.loadFilesByNameInFold(foldId, targetFile.getName(), userId);
        if (files == null) {
            files = this.buildFiles(foldId, targetFile.getName(), targetFile.length(), targetFile.getAbsolutePath(), userId);
            final Files savedFiles = this.filesDao.save(files);
            CompletableFuture.runAsync(() -> saveSample(savedFiles));
        }
    }

    public void addFileFromFile(Files srcFiles, File targetFile) {
        Files files = this.filesDao.loadFilesByNameInFold(srcFiles.getFoldId(), targetFile.getName(), srcFiles.getUserId());
        if (files == null) {
            files = this.buildFiles(srcFiles.getFoldId(), targetFile.getName(), targetFile.length(), targetFile.getAbsolutePath(), srcFiles.getUserId());
            files.setSort(srcFiles.getSort());
            final Files savedFiles = this.filesDao.save(files);
            CompletableFuture.runAsync(() -> saveSample(savedFiles));
        }
    }

    public Files buildFiles(Long foldId, String fileName, Long fileSize, String storagePath, Integer userId) {
        Files files = new Files();
        files.setName(fileName);
        files.setUserId(userId);
        files.setSize(fileSize);
        files.setFileType(getFileType(fileName));
        files.setStoragePath(storagePath);
        files.setFoldId(foldId);
        files.setCreateAt(LocalDateTime.now());
        files.setDeleted(false);
        files.setUpdateAt(LocalDateTime.now());
        files.setSampleStatus(0);
        files.setStatus(FileStatusConstant.INIT);
        return files;
    }

    private boolean isVideo(String fileType) {
        return fileType != null && fileType.startsWith("video/");
    }

    public boolean isImage(String filetype) {
        return filetype != null && filetype.startsWith("image/");
    }

    public boolean isPdf(String filetype) {
        return "application/pdf".equals(filetype);
    }

    public boolean isRmvb(String fileType) {
        return "application/vnd.rn-realmedia-vbr".equals(fileType);
    }

    @Transactional
    public void batchRemoveFile(List<FtpDirsDTO> ftpFiles, Long curFoldId, Integer userId) {
        if (ftpFiles != null && !ftpFiles.isEmpty()) {
            boolean deleteFolds = false;
            for (FtpDirsDTO file : ftpFiles) {
                if (file.isFileFlag()) {
                    Files files = this.filesDao.loadFileInFold(curFoldId, file.getId(), userId);
                    if (files != null) {
                        files.setDeleted(true);
                        files.setDeleteAt(LocalDateTime.now());
                        this.filesDao.save(files);
                    }
                } else {
                    Folds fold = this.foldsDao.findFoldsByParentIdAndId(curFoldId, file.getId());
                    if (fold != null) {
                        fold.setDeleted(true);
                        fold.setDeleteAt(LocalDateTime.now());
                        this.foldsDao.save(fold);
                        deleteFolds = true;
                    }
                }
            }
            if (deleteFolds) {
                CompletableFuture.runAsync(this::signDeleteInDeletedFold);
            }
        }
    }

    public Folds addFold(String name, Long parentId, Integer userId) {
        Folds fold = this.foldsDao.loadFoldsByNameInFold(name, parentId, userId);
        if (fold == null) {
            fold = new Folds();
            fold.setName(name);
            fold.setParentId(parentId);
            fold.setCreateAt(LocalDateTime.now());
            fold.setUserId(userId);
            fold.setDeleted(false);
            fold.setCreateAt(LocalDateTime.now());
            this.foldsDao.save(fold);
            if (fold.getId() != null && fold.getId() > 0) {
                File foldDir = new File(this.getRootPath(), String.valueOf(fold.getId()));
                if (!foldDir.exists() && !foldDir.mkdir()) {
                    log.error("创建文件夹失败: {}", foldDir.getName());
                }
            }
        }
        return fold;
    }

    @Transactional
    public void sortFiles(List<FtpDirsDTO> ftpFiles, UserFtpCache ftpCache) {
        for (FtpDirsDTO ftpFile : ftpFiles) {
            if (ftpFile.getSort() == null) continue;

            if (ftpFile.isFileFlag()) {
                this.filesDao.updateSortById(ftpCache.currentFoldId(), ftpFile.getId(), this.loginService.getLoginId(), ftpFile.getSort());
            } else {
                this.foldsDao.updateSortById(ftpCache.currentFoldId(), ftpFile.getId(), this.loginService.getLoginId(), ftpFile.getSort());
            }
        }
    }

    public void doUnZip() {
        if (toUnzipFiles.isEmpty()) {
            log.info("解压的fileId为空");
            return;
        }

        if (unzipFlag) {
            log.info("正在解压...");
            return;
        }
        synchronized (this) {
            if (unzipFlag) {
                log.info("正在解压 2 ...");
                return;
            }
            unzipFlag = true;
        }
        log.info("开始异步解压：{}", toUnzipFiles);
        Thread.ofVirtual().start(() -> {
            log.info("进入解压线程");
            try {
                while (!toUnzipFiles.isEmpty()) {
                    Long fileId = toUnzipFiles.removeFirst();
                    lock.lock();
                    log.info("开始解压：{}", fileId);
                    try {
                        this.filesDao.findById(fileId)
                                .filter(a -> FileStatusConstant.isUnzip(a.getStatus()))
                                .ifPresent(files -> {
                                    if ("application/zip".equals(files.getFileType()) || "application/x-7z-compressed".equals(files.getFileType())) {
                                        this.unzipZipShell(files, files.getFoldId(), files.getUserId());
                                    }
                                });
                    } catch (Exception e) {
                        log.error("解压失败: {}", e.getMessage(), e);
                    } finally {
                        lock.unlock();
                    }
                }
                log.info("解压结束");
            } finally {
                unzipFlag = false;
            }
        });
    }

    @Transactional
    public void unzip(List<Long> fileIds, Long curFoldId, Integer userId) {
        List<Long> toUnzips = new ArrayList<>();
        for (Long fileId : fileIds) {
            Files files = this.filesDao.loadFileInFold(curFoldId, fileId, userId);
            if (files == null) {
                Folds folds = this.foldsDao.loadFoldInFold(curFoldId, fileId, userId);
                if (folds != null) {
                    this.filesDao.changeFold(curFoldId, folds.getId(), userId);
                    this.foldsDao.changeFold(curFoldId, folds.getId(), userId);
                    continue;
                }
                throw new SimpleBizException("解压失败");
            }
            if (!FileStatusConstant.isFree(files.getStatus())) {
                log.error("文件[{}]正被锁定: {}", files.getName(), FileStatusConstant.getStatusName(files.getStatus()));
                continue;
            }
            files.setStatus(FileStatusConstant.UNZIP);
            this.filesDao.save(files);
            toUnzips.add(fileId);
        }
        if (!toUnzips.isEmpty()) {
            toUnzipFiles.addAll(toUnzips);
        }
    }

    private void unzipZipShell(Files files, Long curFoldId, Integer userId) {
        try {
            File zipFilePath = new File(files.getStoragePath());
            if (!zipFilePath.exists()) {
                log.error("文件不存在: {}", zipFilePath.getAbsolutePath());
                return;
            }

            String name = zipFilePath.getName();
            name = name.substring(0, name.lastIndexOf("."));
            File dir = new File(zipFilePath.getParentFile(), name);
            if (!dir.exists() && !dir.mkdirs()) {
                log.error("文件夹[{}] 创建失败", dir.getAbsolutePath());
                return;
            }
            Long foldId = addFold(dir.getName(), curFoldId, userId).getId();

            // 构建 unzip 命令（注意：-O 可能不是所有 unzip 都支持）
            String[] command = {
                    "7z", "x", files.getStoragePath(), "-o" + dir.getAbsolutePath(), "-y"
            };

            log.info("执行命令: {}", String.join(" ", command));

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);  // 合并 stderr 到 stdout

            Process process = builder.start();
            // 🔥 关闭子进程的 stdin，防止 unzip 阻塞等待输入
            process.getOutputStream().close();

            // 读取命令输出
            Thread infoThread = Thread.ofVirtual().start(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("[unzip] {}", line);
                    }
                } catch (Exception e) {
                    log.error("[unzip] 读取命令输出失败: {}", e.getMessage(), e);
                }
            });

            Thread stderrThread = Thread.ofVirtual().start(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.warn("[unzip stderr] {}", line);
                    }
                } catch (Exception e) {
                    log.error("读取 unzip stderr 出错", e);
                }
            });

            process.waitFor();
            infoThread.join();
            stderrThread.join();
//            if (exitCode != 0) {
//                throw new SimpleBizException("unzip 解压失败，退出码: " + exitCode);
//            }
            log.info("✅ 解压完成");

            this.storePhysicsFiles(dir, foldId, userId);
        } catch (Exception e) {
            log.info("解码zip文件失败: {}", files.getName(), e);
        } finally {
            files.setStatus(FileStatusConstant.INIT);
            this.filesDao.save(files);
        }
    }

    // 防止 Zip Slip 攻击
    private File newFile(File destDir, ZipEntry entry) throws IOException {
        File destFile = new File(destDir, entry.getName());
        String destDirPath = destDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside the target dir: " + entry.getName());
        }
        return destFile;
    }

    private void storePhysicsFiles(File dir, Long foldId, Integer userId) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    Long subFoldId = addFold(file.getName(), foldId, userId).getId();
                    storePhysicsFiles(file, subFoldId, userId);
                } else if (file.isFile()) {
                    addFile(foldId, file, userId);
                }
            }
        }
    }

    public void convertRmvbToMp4(Files files) {
        if (isRmvb(files.getFileType())) {
            File oriFile = new File(files.getStoragePath());
            File targetFold = new File(this.getRootPath(), String.valueOf(files.getFoldId()));
            if (!targetFold.exists() && !targetFold.mkdirs()) {
                log.error("文件夹创建失败: {}", targetFold.getAbsolutePath());
                return;
            }
            File targetFile = new File(targetFold, oriFile.getName().substring(0, oriFile.getName().lastIndexOf(".")) + ".mp4");
            if (!targetFile.exists()) {
                String[] command = null;
                if ("application/vnd.rn-realmedia-vbr".equals(files.getFileType())) {
                    command = new String[]{
                            "ffmpeg",
                            "-i", files.getStoragePath(),
                            "-c:v", "libx264",
                            "-preset", "fast",
                            "-crf", "23",
                            "-c:a", "aac",
                            "-b:a", "128k",
                            targetFile.getAbsolutePath()
                    };
                } else if ("video/mp2t".equals(files.getFileType())) {
                    command = new String[]{
                            "ffmpeg", "-i", files.getStoragePath(),
                            "-c:v", "libx264", "-c:a", "aac",
                            "-strict", "experimental",
                            targetFile.getAbsolutePath()
                    };
                } else {
                    log.info("不支持的文件类型: {}", files.getFileType());
                    return;
                }

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true); // 将 stderr 合并到 stdout
                try {
                    Process process = pb.start();

                    // 打印输出信息
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        log.info("转换结果");
                        while ((line = reader.readLine()) != null) {
                            log.info(line);
                        }
                    }

                    int exitCode = process.waitFor();
                    log.info("转换完成，退出码：{}", exitCode);
                } catch (IOException | InterruptedException e) {
                    log.error("转换文件[{}]失败：{}", files.getName(), e.getMessage());
                }
            }
            this.addFileFromFile(files, targetFile);
            files.setDeleted(true);
            files.setDeleteAt(LocalDateTime.now());
            this.filesDao.save(files);
        }
    }

    public void signDeleteInDeletedFold() {
        LocalDateTime time = LocalDateTime.now().minusHours(1);
        List<Folds> folds = this.foldsDao.loadDeletedWithTime(time);
        if (!folds.isEmpty()) {
            for (Folds fold : folds) {
                this.deleteFold(fold);
            }
        }
    }

    private void deleteFold(Folds fold) {
        List<Folds> folds = this.foldsDao.findByParentIdAndDeletedFalse(fold.getId());
        if (!folds.isEmpty()) {
            for (Folds subFold : folds) {
                if (Boolean.FALSE.equals(subFold.getDeleted())) {
                    subFold.setDeleted(true);
                    subFold.setDeleteAt(LocalDateTime.now());
                    this.foldsDao.save(subFold);
                }
                this.deleteFold(subFold);
            }
        }
        List<Files> files = this.filesDao.findByFoldIdAndDeletedFalse(fold.getId());
        for (Files file : files) {
            file.setDeleted(true);
            file.setDeleteAt(LocalDateTime.now());
            this.filesDao.save(file);
        }
    }

    @Transactional
    public void batchRename(List<FtpRequestDTO> list, Long foldId, Integer userId) {
        for (FtpRequestDTO dto : list) {
            this.rename(dto, foldId, userId);
        }
    }

    private void rename(FtpRequestDTO dto, Long foldId, Integer userId) {
        if (dto.isFileFlag()) {
            Files files = this.filesDao.loadFilesByNameInFold(foldId, dto.getNewName(), userId);
            if (files == null) {
                files = this.filesDao.loadFileInFold(foldId, dto.getId(), userId);
                if (files != null && !files.getName().equals(dto.getNewName())) {
                    files.setName(dto.getNewName());
                    files.setUpdateAt(LocalDateTime.now());
                    this.filesDao.save(files);
                }
            }
        } else {
            Folds folds = this.foldsDao.loadFoldsByNameInFold(dto.getNewName(), foldId, userId);
            if (folds == null) {
                folds = this.foldsDao.loadFoldInFold(foldId, dto.getId(), userId);
                if (folds != null && !folds.getName().equals(dto.getNewName())) {
                    folds.setName(dto.getNewName());
                    this.foldsDao.save(folds);
                }
            }
        }
    }

    public void deletePhysicalFile() {
        PageQueryService<Files> pageQueryService = new PageQueryService<>(500);
        LocalDateTime endTime = LocalDateTime.now().minusDays(3);
        pageQueryService.setMapper((minId, size) -> this.filesDao.loadToDeletePhysicalFiles(minId, size, endTime));
        pageQueryService.setIdMapper((files, minId) -> {
            try {
                File pythsicalFile = new File(files.getStoragePath());
                List<Files> sameNameFiles = this.filesDao.findSameNames(files.getName(), files.getId(), files.getStoragePath());
                boolean flag = true;
                if (sameNameFiles.stream().noneMatch(a -> a.getStoragePath().equals(files.getStoragePath()))) {
                    if (pythsicalFile.exists()) {
                        flag = pythsicalFile.delete();
                    }
                }
                if (flag) {
                    files.setRemoved(true);
                    this.filesDao.save(files);
                    File sampleFile = this.previewImage(files);
                    if (sampleFile.exists()) {
                        sampleFile.delete();
                    }
                }
            } catch (Exception e) {
                log.error("删除物理文件[{}]失败：{}", files.getName(), e.getMessage(), e);
            }
            return Math.max(minId, files.getId());
        });
        pageQueryService.query();
    }

    public void deleteFileRecord() {
        PageQueryService<Files> pageQueryService = new PageQueryService<>(500);
        pageQueryService.setMapper((minId, size) -> this.filesDao.loadToDeleteFilesRecord(minId, size));
        pageQueryService.setIdMapper((files, minId) -> {
            try {
                File pythsicalFile = new File(files.getStoragePath());
                if (!pythsicalFile.exists()) {
                    this.filesDao.deleteById(files.getId());
                }
            } catch (Exception e) {
                log.error("删除数据[{}]失败: {}", files.getId(), e.getMessage());
            }
            return Math.max(minId, files.getId());
        });
        pageQueryService.query();
    }

    @Transactional
    public void sortFilesByNameAsc() {
        UserFtpCache ftpCache = this.loginService.getFtpCache();
        Integer loginId = this.loginService.getLoginId();
        List<Files> filesList = this.filesDao.findFilesByFoldIdAndUserIdAndDeletedFalse(ftpCache.currentFoldId(), loginId);
        if (!filesList.isEmpty()) {
            filesList.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            for (int i = 0; i < filesList.size(); i++) {
                filesList.get(i).setSort(i);
            }
            this.filesDao.saveAll(filesList);
        }

        List<Folds> folds = this.foldsDao.loadFolds(ftpCache.currentFoldId(), loginId);
        if (!folds.isEmpty()) {
            folds.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            for (int i = 0; i < folds.size(); i++) {
                folds.get(i).setSort(i);
            }
            this.foldsDao.saveAll(folds);
        }
    }

    private File previewImage(Files files) {
        return new File(this.ftpConfig.getPreviewPath(), files.getId() + ".webp");
    }

    public String getRootPath() {
        return this.ftpConfig.getRootPath();
    }

    public void formatFiles(List<Files> files, List<FtpDirsDTO> list) {
        // 按照fileType排序，如果一样的话，按照sort排序
        Comparator<Files> comparator = Comparator.comparing(a -> Objects.requireNonNullElse(a.getFileType(), ""));
        comparator = comparator.thenComparing(a -> Objects.requireNonNullElse(a.getSort(), 0));
        files.sort(comparator);
        files.sort(Comparator.comparing(a -> Objects.requireNonNullElse(a.getSort(), 0)));
        for (Files file : files) {
            FtpDirsDTO ftpDirsDTO = new FtpDirsDTO(file.getId(), true, file.getName());
            ftpDirsDTO.setFileType(file.getFileType());
            ftpDirsDTO.setSort(file.getSort());
            list.add(ftpDirsDTO);
        }
    }

    private Folds loadTmpFold(Integer userId) {
        Folds tmp = this.foldsDao.loadFoldsByNameInFold("tmp", 0L, userId);
        if (tmp == null) {
            throw new SimpleBizException("no tmp directory");
        }
        return tmp;
    }

    @Transactional
    public void addTmpFile(TmpFileDTO dto, Integer userId) {
        Folds tmp = this.loadTmpFold(userId);
        for (String foldName : dto.getFoldNames()) {
            tmp = this.addFold(foldName, tmp.getId(), userId);
        }
        File targetFold = new File(this.getRootPath(), String.valueOf(tmp.getId()));
        if (!targetFold.exists()) {
            if (!targetFold.mkdirs()) {
                log.error("创建目录: {} 失败", targetFold.getAbsoluteFile());
                return;
            }
        }

        URI uri = URI.create(dto.getReferer());
        String referer = "%s://%s/".formatted(uri.getScheme(), uri.getHost());
        for (TmpFileDTO.Data tmpFile : dto.getItems()) {
            if (StringUtils.isEmpty(tmpFile.getUrl())) continue;

            String filename = tmpFile.getFileName();
            if (StringUtils.isEmpty(filename)) {
                filename = tmpFile.getUrl().substring(tmpFile.getUrl().lastIndexOf("/") + 1);
            }
            File targetFile = new File(targetFold, filename);
            if (targetFile.exists()) {
                this.addFile(tmp.getId(), targetFile, userId);
            } else {
                Files newFiles = this.buildFiles(tmp.getId(), filename, 0L, targetFile.getAbsolutePath(), userId);
                newFiles.setHostUrl(tmpFile.getUrl());
                newFiles.setStatus(FileStatusConstant.DOWNLOAD_DIRECT);
                newFiles.setReferer(referer);
                this.filesDao.save(newFiles);
            }
        }
    }

}
