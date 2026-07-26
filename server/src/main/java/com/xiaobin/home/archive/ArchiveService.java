package com.xiaobin.home.archive;

import com.xiaobin.home.constant.Constant;
import com.xiaobin.home.dto.StorageLocation;
import com.xiaobin.home.node.NodePermissionService;
import com.xiaobin.home.node.NodeService;
import com.xiaobin.home.node.entity.FmNode;
import com.xiaobin.home.orm.Orm;
import com.xiaobin.home.service.FileService;
import com.xiaobin.home.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Enumeration;

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

    private void decompressZip(FmNode fmNode, Integer loginId) throws IOException {
        StorageLocation location = this.nodeService.location(fmNode);
        try (ZipFile zip = ZipFile.builder().setFile(location.path()).get()) {
            Enumeration<ZipArchiveEntry> entries = zip.getEntries();
            Path output = new File(this.fileService.getRootPath()).toPath();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                Path target = output.resolve(entry.getName());
                FmNode child = new FmNode();
                child.setNodeName(entry.getName());
                child.setOwnerId(loginId);
                if(entry.isDirectory()) {
                    child.setNodeType(Constant.NODE_TYPE_FOLDER);
                    this.nodeService.addChild()
                }
            }
        }
    }

    public void decompress(FmNode fmNode, Integer loginId) throws IOException {
        fmNode = this.nodePermissionService.checkPermission(fmNode, loginId);
        if ("application/zip".equals(fmNode.getMimeType())) {
            this.decompressZip(fmNode);
        }
    }
}
