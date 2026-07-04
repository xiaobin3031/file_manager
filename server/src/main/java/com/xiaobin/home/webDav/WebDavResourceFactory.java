package com.xiaobin.home.webDav;

import com.xiaobin.home.constant.Constant;
import com.xiaobin.home.dto.WebDavPath;
import io.milton.http.ResourceFactory;
import io.milton.resource.Resource;

public class WebDavResourceFactory implements ResourceFactory {

    private final WebDavService webDavService;

    public WebDavResourceFactory(WebDavService webDavService) {
        this.webDavService = webDavService;
    }

    @Override
    public Resource getResource(String host, String path) {
        path = path.replace(Constant.WEBDAV_PATH, "");
        WebDavPath p = this.webDavService.stat(path);
        if (p == null) return null;

        if (p.isDirectory()) {
            return new WebDavFolderResource(this.webDavService, p);
        }
        return new WebDavFileResource(this.webDavService, p);
    }
}
