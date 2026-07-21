package com.xiaobin.home.storage;

import com.xiaobin.home.dto.StorageLocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface StorageService {

    InputStream open(StorageLocation storageLocation) throws IOException;

    OutputStream create(StorageLocation storageLocation) throws IOException;

    void delete(StorageLocation storageLocation) throws IOException;

    long size(StorageLocation storageLocation) throws IOException;

}
