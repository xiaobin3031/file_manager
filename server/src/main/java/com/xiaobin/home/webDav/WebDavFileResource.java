package com.xiaobin.home.webDav;

import com.xiaobin.home.dto.WebDavPath;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.CollectionResource;
import io.milton.resource.FileResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;

public class WebDavFileResource implements FileResource {

    private final WebDavService webDavService;
    private final WebDavPath p;

    WebDavFileResource(WebDavService webDavService, WebDavPath p) {
        this.webDavService = webDavService;
        this.p = p;
    }

    @Override
    public void copyTo(CollectionResource collectionResource, String s) throws NotAuthorizedException, BadRequestException, ConflictException {

    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {

    }

    @Override
    public String processForm(Map<String, String> map, Map<String, FileItem> map1) throws BadRequestException, NotAuthorizedException, ConflictException {
        return "";
    }

    @Override
    public void sendContent(OutputStream outputStream, Range range, Map<String, String> map, String s) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        try (InputStream in = this.webDavService.open(this.p.getPath())) {
            if (in != null) {
                in.transferTo(outputStream);
            }
        }
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return 0L;
    }

    @Override
    public String getContentType(String s) {
        return "";
    }

    @Override
    public Long getContentLength() {
        return this.p.getSize();
    }

    @Override
    public void moveTo(CollectionResource collectionResource, String s) throws ConflictException, NotAuthorizedException, BadRequestException {

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
        return this.p.getModifyTime();
    }

    @Override
    public String checkRedirect(Request request) throws NotAuthorizedException, BadRequestException {
        return "";
    }
}
