package com.xiaobin.home.webDav;

import com.xiaobin.home.dto.WebDavPath;
import com.xiaobin.home.entity.Files;
import com.xiaobin.home.entity.Folds;
import com.xiaobin.home.repository.FilesDao;
import com.xiaobin.home.repository.FoldsDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class WebDavService {

    private final Integer userId = 1;
    @Autowired
    private FoldsDao foldsDao;
    @Autowired
    private FilesDao filesDao;

    /**
     * 获取一个路径
     */
    public WebDavPath stat(String path) {
        log.info("stat path:{}", path);
        String[] paths = path.split("/");
        if (paths.length < 2) {
            WebDavPath webDavPath = new WebDavPath();
            webDavPath.setPath("");
            webDavPath.setName("root");
            webDavPath.setModifyTime(Calendar.getInstance().getTime());
            webDavPath.setDirectory(true);
            return webDavPath;
        }
        String lastName = paths[paths.length - 1];
        Folds folds = this.foldsDao.findFoldsByNameAndUserIdAndDeletedFalse(lastName, userId);
        if (folds != null) {
            return this.foldsConvertPath(folds, path);
        }
        if (paths.length > 2) {
            String foldName = paths[paths.length - 2];
            folds = this.foldsDao.findFoldsByNameAndUserIdAndDeletedFalse(foldName, userId);
            if (folds != null) {
                Files files = this.filesDao.loadFilesByNameInFold(folds.getId(), lastName, userId);
                if (files != null) {
                    return this.filesConvertPath(files, foldName);
                }
            }
        }
        return null;
    }

    /**
     * 获取目录内容
     */
    public List<WebDavPath> list(String path) {
        log.info("list path:{}", path);
        String[] paths = path.split("/");
        List<WebDavPath> list = new ArrayList<>();
        if (path.length() < 2) {
            List<Folds> folds = this.foldsDao.loadFolds(0L, userId);
            for (Folds fold : folds) {
                list.add(this.foldsConvertPath(fold, path));
            }
        } else {
            String lastName = paths[paths.length - 1];
            Folds folds = this.foldsDao.findFoldsByNameAndUserIdAndDeletedFalse(lastName, userId);
            if (folds != null) {
                List<Folds> childFolds = this.foldsDao.loadFolds(folds.getId(), userId);
                for (Folds child : childFolds) {
                    list.add(this.foldsConvertPath(child, path));
                }
                List<Files> files = this.filesDao.findFilesByFoldIdAndUserIdAndDeletedFalse(folds.getId(), userId);
                for (Files file : files) {
                    list.add(this.filesConvertPath(file, path));
                }
            }
        }
        return list;
    }

    private WebDavPath filesConvertPath(Files files, String path) {
        WebDavPath webDavPath = new WebDavPath();
        webDavPath.setPath(path + "/" + files.getName());
        webDavPath.setName(files.getName());
        webDavPath.setModifyTime(Date.from(files.getUpdateAt().toInstant(ZoneOffset.of("+8"))));
        webDavPath.setSize(files.getSize());
        return webDavPath;
    }

    private WebDavPath foldsConvertPath(Folds folds, String path) {
        WebDavPath webDavPath = new WebDavPath();
        webDavPath.setPath(path + "/" + folds.getName());
        webDavPath.setName(folds.getName());
        webDavPath.setSize(folds.getFileCount() + folds.getFoldCount());
        webDavPath.setModifyTime(Calendar.getInstance().getTime());
        webDavPath.setDirectory(true);
        return webDavPath;
    }

    /**
     * 打开文件
     */
    public InputStream open(String path) {
        log.info("open path:{}", path);
        String[] paths = path.split("/");
        Folds folds = this.foldsDao.findFoldsByNameAndUserIdAndDeletedFalse(paths[paths.length - 2], userId);
        if (folds != null) {
            Files files = this.filesDao.loadFilesByNameInFold(folds.getId(), paths[paths.length - 1], userId);
            if (files != null) {
                try {
                    return new FileInputStream(files.getStoragePath());
                } catch (FileNotFoundException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
