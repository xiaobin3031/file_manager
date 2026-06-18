package com.xiaobin.home.service;

import com.xiaobin.home.config.FtpConfig;
import com.xiaobin.home.constant.FileStatusConstant;
import com.xiaobin.home.entity.FileUploadProgress;
import com.xiaobin.home.exception.SimpleBizException;
import com.xiaobin.home.repository.FileUploadProgressDao;
import com.xiaobin.home.repository.FilesDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

@Service
@Slf4j
public class FileUploadService {

    @Autowired
    private FtpConfig ftpConfig;
    @Autowired
    private FileUploadProgressDao fileUploadProgressDao;
    @Autowired
    private FilesDao filesDao;
    @Autowired
    private FileService fileService;

    public void finishUpload(FileUploadProgress progress, Integer userId, Long foldId) {
        if (progress.getTotalChunk() - 1 == progress.getCurrentChunk()) {
            String uploadId = progress.getUploadId();
            Long fileId = Long.parseLong(uploadId.substring(0, uploadId.indexOf("_")));
            com.xiaobin.home.entity.Files files = this.filesDao.loadFileInFold(foldId, fileId, userId);
            if(files != null && FileStatusConstant.isUpload(files.getStatus())) {
                File targetFile = new File(files.getStoragePath());
                File dir = targetFile.getParentFile();
                if(!dir.exists()) {
                    // 切换了盘符，可能会不存在目录
                    if (!dir.mkdirs()) {
                        throw new SimpleBizException("目录创建失败: " + dir.getAbsolutePath());
                    }
                }
                if (!targetFile.exists() || targetFile.length() == 0L) {
                    try (OutputStream fos = new FileOutputStream(targetFile)) {
                        byte[] bytes = new byte[5 * 1024];
                        int read;
                        File tmpDir = new File(this.ftpConfig.getTmpPath(), progress.getUploadId());
                        for (int i = 0; i < progress.getTotalChunk(); i++) {
                            File tmpFile = new File(tmpDir, i + ".part");
                            if (!tmpFile.exists()) {
                                throw new SimpleBizException("分片 " + i + " 不存在");
                            }
                            try (InputStream fis = new FileInputStream(tmpFile)) {
                                while ((read = fis.read(bytes)) != -1) {
                                    fos.write(bytes, 0, read);
                                }
                            }
                        }
                        fos.flush();
                    } catch (IOException e) {
                        log.error("合并文件[{}]失败: {}", targetFile.getAbsolutePath(), e.getMessage());
                        throw new SimpleBizException("合并文件失败");
                    }
                    files.setStatus(FileStatusConstant.INIT);
                    this.filesDao.save( files);
                    this.fileService.saveSample(files);
                }
            }
            progress.setStoreFlag(true);
            this.fileUploadProgressDao.save(progress);
            File tmpFold = new File(this.ftpConfig.getTmpPath(), progress.getUploadId());
            if (tmpFold.exists()) {
                try {
                    Files.walkFileTree(tmpFold.toPath(), new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    log.error("文件夹删除[{}]失败: {}", tmpFold.getAbsolutePath(), e.getMessage());
                }
            }
        }
    }
}
