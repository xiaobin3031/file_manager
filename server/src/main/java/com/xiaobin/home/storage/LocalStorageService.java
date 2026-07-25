package com.xiaobin.home.storage;

import com.xiaobin.home.dto.StorageLocation;
import org.apache.commons.lang3.StringUtils;

import java.io.*;

public class LocalStorageService implements StorageService {

    @Override
    public InputStream open(StorageLocation storageLocation) throws IOException {
        if (!this.exist(storageLocation)) {
            throw new RuntimeException(storageLocation.path() + " doesn't exist");
        }
        File file = new File(storageLocation.path());
        if (!file.exists()) {
            throw new FileNotFoundException(storageLocation.path());
        }
        return new FileInputStream(file);
    }

    @Override
    public OutputStream create(StorageLocation storageLocation) throws IOException {
        if (!this.exist(storageLocation)) {
            throw new RuntimeException(storageLocation.path() + " doesn't exist");
        }
        File file = new File(storageLocation.path());
        return new FileOutputStream(file);
    }

    @Override
    public void delete(StorageLocation storageLocation) throws IOException {
        if (!this.exist(storageLocation)) {
            throw new RuntimeException(storageLocation.path() + " doesn't exist");
        }
        File file = new File(storageLocation.path());
        if (file.exists() && !file.delete()) {
            throw new IOException(storageLocation.path() + " delete failed");
        }
    }

    @Override
    public long size(StorageLocation storageLocation) throws IOException {
        if (!exist(storageLocation)) {
            return 0L;
        }
        File file = new File(storageLocation.path());
        return file.exists() ? file.length() : 0L;
    }

    @Override
    public boolean exist(StorageLocation storageLocation) throws IOException {
        if (StringUtils.isNotEmpty(storageLocation.path())) {
            return new File(storageLocation.path()).exists();
        }
        return false;
    }

    @Override
    public void unzip(StorageLocation storageLocation) throws IOException {

    }
}
