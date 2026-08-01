package com.xiaobin.home.archive;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.jupiter.api.Test;

import java.util.Enumeration;

public class TestArchiveService {

    @Test
    public void testZip() throws Exception {
        String path = "/Users/lixiaolin/Downloads/realesrgan-ncnn-vulkan-20220424-ubuntu.zip";
        try (ZipFile zip = ZipFile.builder().setFile(path).get()) {
            Enumeration<ZipArchiveEntry> entries = zip.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                System.out.println("entry.name: "+ entry.getName());
//                Path target = output.resolve(entry.getName());
//                FmNode child = new FmNode();
//                child.setNodeName(entry.getName());
//                child.setOwnerId(loginId);
//                if(entry.isDirectory()) {
//                    child.setNodeType(Constant.NODE_TYPE_FOLDER);
//                    this.nodeService.addChild()
//                }
            }
        }
    }
}
