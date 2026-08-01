package com.xiaobin.home.archive;

import com.xiaobin.home.constant.Constant;
import com.xiaobin.home.dto.StorageLocation;
import com.xiaobin.home.node.NodePermissionService;
import com.xiaobin.home.node.NodeService;
import com.xiaobin.home.node.entity.FmNode;
import com.xiaobin.home.node.entity.FmStorage;
import com.xiaobin.home.orm.Orm;
import com.xiaobin.home.service.FileService;
import com.xiaobin.home.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ArchiveService {

    @Autowired
    private Orm orm;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private NodePermissionService nodePermissionService;
    @Autowired
    private StorageService storageService;
    @Autowired
    private FileService fileService;

    private FmNode ensureFolds(String entryName, FmNode root, Map<String, FmNode> foldsMap) {
        String[] paths = entryName.split("/");
        FmNode node = root;
        if (paths.length > 1) {
            // folds
            StringBuilder pathBuilder = new StringBuilder();
            for (int i = 1; i < paths.length - 1; i++) {
                pathBuilder.append(paths[i]);
                FmNode fold = foldsMap.get(pathBuilder.toString());
                if (fold == null) {
                    fold = new FmNode();
                    fold.setNodeName(paths[i]);
                    fold.setNodeType(Constant.NODE_TYPE_FOLDER);
                    fold.setOwnerId(node.getOwnerId());
                    this.nodeService.addChild(node, fold);
                    foldsMap.put(pathBuilder.toString(), fold);
                }
                node = fold;
                pathBuilder.append("/");
            }
        }
        return node;
    }

    private void decompressZip(FmNode fmNode) throws IOException {
        StorageLocation location = this.nodeService.location(fmNode);
        try (ZipFile zip = ZipFile.builder().setFile(location.path()).get()) {
            Enumeration<ZipArchiveEntry> entries = zip.getEntries();
            Path output = new File(this.fileService.getRootPath()).toPath();
            Map<String, FmNode> foldsMap = new HashMap<>();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                log.info("entry.name: {}", entry.getName());
                FmNode node = this.ensureFolds(entry.getName(), fmNode, foldsMap);
                int index = entry.getName().lastIndexOf("/");
                String filename;
                if (index == -1) {
                    filename = entry.getName();
                } else {
                    filename = entry.getName().substring(index + 1);
                }
                Path targetFold = output.resolve(String.valueOf(node.getId()));
                Path target = targetFold.resolve(filename);
                StorageLocation targetLocation = new StorageLocation("", target.toString());
                zip.getInputStream(entry).transferTo(this.storageService.create(targetLocation));
                FmStorage storage = new FmStorage();
                storage.setPath(target.toString());
                storage.setSize(entry.getSize());
                if (this.orm.save(storage) == 0) {
                    log.info("save storage error: {}", storage.getPath());
                    return;
                }
                Files.createDirectories(targetFold);
                FmNode fileNode = new FmNode();
                fileNode.setStorageId(storage.getId());
                fileNode.setNodeName(filename);
                fileNode.setNodeType(Constant.NODE_TYPE_FILE);
                fileNode.setOwnerId(node.getOwnerId());
                fileNode.setMimeType(this.fileService.getFileType(fileNode.getNodeName()));
                this.nodeService.addChild(node, fileNode);
            }
        }
    }

    private void decompress7z(FmNode fmNode) throws IOException {
        StorageLocation location = this.nodeService.location(fmNode);
        try (SevenZFile zip = SevenZFile.builder().setFile(location.path()).get()) {
            SevenZArchiveEntry entry;
            Path output = new File(this.fileService.getRootPath()).toPath();
            Map<String, FmNode> foldsMap = new HashMap<>();
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                log.info("entry.name: {}", entry.getName());
                FmNode node = this.ensureFolds(entry.getName(), fmNode, foldsMap);
                int index = entry.getName().lastIndexOf("/");
                String filename;
                if (index == -1) {
                    filename = entry.getName();
                } else {
                    filename = entry.getName().substring(index + 1);
                }
                Path targetFold = output.resolve(String.valueOf(node.getId()));
                Path target = targetFold.resolve(filename);
                StorageLocation targetLocation = new StorageLocation("", target.toString());
                try(OutputStream out = this.storageService.create(targetLocation)) {
                    byte[] buffer = new byte[1024 * 4];
                    int len;
                    while((len = zip.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    out.flush();
                }
                FmStorage storage = new FmStorage();
                storage.setPath(target.toString());
                storage.setSize(entry.getSize());
                if (this.orm.save(storage) == 0) {
                    log.info("save storage error: {}", storage.getPath());
                    return;
                }
                Files.createDirectories(targetFold);
                FmNode fileNode = new FmNode();
                fileNode.setStorageId(storage.getId());
                fileNode.setNodeName(filename);
                fileNode.setNodeType(Constant.NODE_TYPE_FILE);
                fileNode.setOwnerId(node.getOwnerId());
                fileNode.setMimeType(this.fileService.getFileType(fileNode.getNodeName()));
                this.nodeService.addChild(node, fileNode);
            }
        }
    }

    public void decompress(FmNode fmNode) throws IOException {
        fmNode = this.nodePermissionService.checkPermission(fmNode, fmNode.getOwnerId());
        if ("application/zip".equals(fmNode.getMimeType())) {
            this.decompressZip(fmNode);
        } else if ("application/x-7z-compressed".equals(fmNode.getMimeType())) {
            this.decompress7z(fmNode);
        }
    }
}
