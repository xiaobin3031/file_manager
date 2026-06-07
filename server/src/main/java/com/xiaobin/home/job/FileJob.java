package com.xiaobin.home.job;

import com.xiaobin.home.constant.FileStatusConstant;
import com.xiaobin.home.entity.Files;
import com.xiaobin.home.entity.Folds;
import com.xiaobin.home.repository.FilesDao;
import com.xiaobin.home.repository.FoldsDao;
import com.xiaobin.home.service.FileDownloadPlanService;
import com.xiaobin.home.service.FileDownloadService;
import com.xiaobin.home.service.FileService;
import com.xiaobin.home.service.PageQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class FileJob {

    @Autowired
    private FilesDao filesDao;
    @Autowired
    private FileService fileService;
    @Autowired
    private FileDownloadService fileDownloadService;
    @Autowired
    private FileDownloadPlanService fileDownloadPlanService;
    @Autowired
    private FoldsDao foldsDao;

    /**
     * 转换指定格式到mp4
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void convertSpecialVideoToMp4() {
        PageQueryService<Files> pageQueryService = new PageQueryService<>(100);
        pageQueryService.setMapper((minId, size) -> this.filesDao.loadFilesByFileType(minId, List.of("application/vnd.rn-realmedia-vbr", "video/mp2t"), size));
        pageQueryService.setIdMapper((files, minId) -> {
            this.fileService.convertRmvbToMp4(files);
            return Math.max(minId, files.getId());
        });
        pageQueryService.query();
    }

    @Scheduled(cron = "0 0 0/1 * * ?")
    public void signDeleteInDeletedFold() {
        this.fileService.signDeleteInDeletedFold();
        this.fileService.deletePhysicalFile();
        this.fileService.deleteFileRecord();
    }

    @Scheduled(cron = "0 0 0/1 * * ?")
    public void checkDownloadFileIsFinish() {
        PageQueryService<Files> pageQueryService = new PageQueryService<>(100);
        pageQueryService.setMapper((minId, size) -> this.filesDao.loadSpecialStatusFiles(minId, size, FileStatusConstant.DOWNLOAD));
        pageQueryService.setIdMapper((files, minId) -> {
            try {
                this.fileDownloadService.checkDownloadFileIsFinish(files);
            } catch (Exception e) {
                log.error("检查文件[{}]下载结果失败: {}", files.getId(), e.getMessage(), e);
            }
            return Math.max(minId, files.getId());
        });
        pageQueryService.query();
    }

    @Scheduled(cron = "0 30 0 * * ?")
    public void doDownloadPlan() {
        this.fileDownloadPlanService.planDownloadList();
    }

    //    @Scheduled(cron = "0 0 0 1 * ?")
    public void removePreviewImage() {
//        this.filesDao.loadFilesByFileType();
    }

//    @Scheduled(cron = "0 0 0/1 * * ?")
    @Scheduled(fixedDelay = 1L)
    public void downloadFold() {
        List<Folds> toDownloadFolds = this.foldsDao.findByStatusAndDeletedFalseAndHostUrlIsNotNull(FileStatusConstant.DOWNLOAD);
        this.fileDownloadPlanService.downloadFolds(toDownloadFolds);
    }
}
