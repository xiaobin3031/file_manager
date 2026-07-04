package com.xiaobin.home.webDav;

import com.xiaobin.home.dto.WebDavPath;
import io.milton.http.Auth;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.CollectionResource;
import io.milton.resource.PropFindableResource;
import io.milton.resource.Resource;

import java.util.Date;
import java.util.List;

public class WebDavFolderResource implements CollectionResource, PropFindableResource {

    private final WebDavService webDavService;
    private final WebDavPath p;

    WebDavFolderResource(WebDavService webDavService, WebDavPath p) {
        this.webDavService = webDavService;
        this.p = p;
    }

    @Override
    public Resource child(String s) throws NotAuthorizedException, BadRequestException {
        return null;
    }

    @Override
    public List<? extends Resource> getChildren() {
        return this.webDavService.list(this.p.getPath())
                .stream()
                .map(item -> {
                    if (item.isDirectory()) {
                        return new WebDavFolderResource(this.webDavService, item);
                    }
                    return new WebDavFileResource(this.webDavService, item);
                }).toList();
    }

    @Override
    public Date getCreateDate() {
        return null;
    }

    @Override
    public String getUniqueId() {
        return "";
    }

    @Override
    public String getName() {
        return this.p.getName();
    }

    @Override
    public Object authenticate(String s, String s1) {
        return "anonymous";
    }

    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth) {
        return true;
    }

    @Override
    public String getRealm() {
        return "webdav";
    }

    @Override
    public Date getModifiedDate() {
        return null;
    }

    @Override
    public String checkRedirect(Request request) throws NotAuthorizedException, BadRequestException {
        return "";
    }
}
