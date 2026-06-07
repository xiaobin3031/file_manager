package com.xiaobin.home.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaobin.home.config.FtpConfig;
import com.xiaobin.home.constant.FileStatusConstant;
import com.xiaobin.home.entity.Files;
import com.xiaobin.home.exception.SimpleBizException;
import com.xiaobin.home.repository.FilesDao;
import com.xiaobin.home.repository.LogsDao;
import com.xiaobin.home.util.TorrentHashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.crypto.dsig.SignatureProperties;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件下载服务
 */
@Service
@Slf4j
public class FileDownloadService {

    private static final String LOGIN_PATH = "/api/v2/auth/login";
    private static final String ADD_TORRENT_PATH = "/api/v2/torrents/add";
    private static final String INFO_PATH = "/api/v2/torrents/info";
    private static final String DELETE_PATH = "/api/v2/torrents/delete";
    private static final HttpHeaders httpHeaders = new HttpHeaders();

    static {
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    }

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private FtpConfig ftpConfig;
    @Autowired
    private FileService fileService;
    @Autowired
    private FilesDao filesDao;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private LogsDao logsDao;

    private void login() {
        log.info("开始登录");
        FtpConfig.Qbittorrent qbittorrent = this.ftpConfig.getQbittorrent();
        HttpEntity<MultiValueMap<String, String>> req = this.buildReqForm(Map.of("username", qbittorrent.getUsername(), "password", qbittorrent.getPassword()));
        ResponseEntity<String> response = this.restTemplate.postForEntity(qbittorrent.getUrlPrefix() + LOGIN_PATH, req, String.class);
        if (response.getBody() == null || !isOk(response.getBody())) {
            throw new SimpleBizException("登录失败");
        }
        log.info("登录结果: {}", response.getBody());
        HttpHeaders headers = response.getHeaders();
        List<String> cookies = headers.get("Set-Cookie");
        if (!CollectionUtils.isEmpty(cookies)) {
            httpHeaders.addAll("Cookie", cookies);
        }
    }

    private boolean isOk(String body) {
        return "Ok.".equals(body);
    }

    private HttpEntity<MultiValueMap<String, String>> buildReqForm(Map<String, String> params) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        params.forEach(form::add);
        return new HttpEntity<>(form, httpHeaders);
    }

    private String send(String path, Map<String, String> params) {
        this.login();
        HttpEntity<MultiValueMap<String, String>> req = this.buildReqForm(params);
        ResponseEntity<String> response = this.restTemplate.postForEntity(ftpConfig.getQbittorrent().getUrlPrefix() + path, req, String.class);
        if (response.getBody() == null) {
            throw new SimpleBizException("请求失败: " + path);
        }
        if (log.isDebugEnabled()) {
            log.debug("请求: [{}],返回: [{}]", path, response.getBody());
        }
        return response.getBody();
    }

    private void uploadTorrent(String path, byte[] torrentBytes, String filename) {
        this.login();

        try (HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()) {
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(("--" + boundary + "\r\n").getBytes());
            bos.write(("Content-Disposition: form-data; name=\"torrents\"; filename=\"" + filename + "\"\r\n").getBytes());
            bos.write("Content-Type: application/x-bittorrent\r\n\r\n".getBytes());
            bos.write(torrentBytes);
            bos.write("\r\n".getBytes());
            bos.write(("--" + boundary + "--\r\n").getBytes());
            HttpRequest uploadReq = HttpRequest.newBuilder()
                    .uri(URI.create(ftpConfig.getQbittorrent().getUrlPrefix() + path))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("Cookie", httpHeaders.getFirst("Cookie"))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bos.toByteArray()))
                    .build();
            HttpResponse<String> response = client.send(uploadReq, HttpResponse.BodyHandlers.ofString());
            if (log.isDebugEnabled()) {
                log.debug("请求: [{}],返回: [{}]", path, response.body());
            }
            if (!isOk(response.body())) {
                throw new SimpleBizException("文件上传失败: " + filename);
            }
        } catch (Exception e) {
            throw new SimpleBizException("文件上传失败: " + filename);
        }
    }

    public Map<String, Object> loadInfo(String hash) {
        String response = this.send(INFO_PATH, Map.of("hashes", hash));
        List<Map<String, Object>> list;
        try {
            list = this.mapper.readValue(response, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("转换下载hash[{}]信息成json失败: {}", hash, e.getMessage());
            throw new SimpleBizException("查询下载信息失败");
        }
        return CollectionUtils.isEmpty(list) ? null : list.getFirst();
    }

    @Transactional
    public Files downloadFromTorrent(MultipartFile file, Integer userId, Long foldId) {
        Files files;
        byte[] bytes;
        String hash;
        try {
            bytes = file.getBytes();
            hash = TorrentHashUtil.infoHash(bytes);
        } catch (Exception e) {
            log.error("读取torrent的hash失败: {}", e.getMessage());
            throw new SimpleBizException("torrent文件读取失败: " + file.getOriginalFilename());
        }
        files = this.buildDhtFile(hash, foldId, userId);
        this.uploadTorrent(ADD_TORRENT_PATH, bytes, file.getOriginalFilename());
        return files;
    }

    private Files buildDhtFile(String hash, Long foldId, Integer userId) {
        Files files = this.filesDao.loadDhtFile(hash);
        if (files == null) {
            files = new Files();
            files.setName(hash);
            files.setUserId(userId);
            files.setSize(0L);
            files.setDeleted(false);
            files.setSampleStatus(0);
            files.setCreateAt(LocalDateTime.now());
            files.setUpdateAt(LocalDateTime.now());
            files.setFoldId(foldId);
            files.setStatus(FileStatusConstant.DOWNLOAD);
            files.setDhtHash(hash);
            this.filesDao.save(files);
        } else if (!Objects.equals(files.getFoldId(), foldId) || !Objects.equals(files.getUserId(), userId)) {
            // 不是自己的，或者不在这个目录，那就改成自己的
            Files updateFiles = new Files();
            updateFiles.setName(files.getName());
            updateFiles.setUserId(userId);
            updateFiles.setSize(files.getSize());
            updateFiles.setDeleted(false);
            updateFiles.setSampleStatus(0);
            updateFiles.setCreateAt(LocalDateTime.now());
            updateFiles.setUpdateAt(LocalDateTime.now());
            updateFiles.setFoldId(foldId);
            updateFiles.setStatus(files.getStatus());
            updateFiles.setDhtHash(hash);
            this.filesDao.save(updateFiles);
            files = updateFiles;
        }
        return files;
    }

    @Transactional
    public Files downloadFromMagnet(String magnet, Integer userId, Long foldId) {
        // 解析成hash
        Pattern pattern = Pattern.compile("magnet:\\?xt=urn:btih:([a-zA-Z0-9]+)");
        Matcher matcher = pattern.matcher(magnet);
        String hash = null;
        if (matcher.find()) {
            hash = matcher.group(1);
        }
        if (hash == null) {
            throw new SimpleBizException("磁力链接不合法");
        }
        log.info("开始下载：{}", magnet);
        Files files = this.buildDhtFile(hash, foldId, userId);
        // 先查询一下，再来做决定
        Map<String, Object> info = this.loadInfo(hash);
        if (info == null) {
            Map<String, String> params = Map.of("urls", magnet);
            String response = this.send(ADD_TORRENT_PATH, params);
            if (!response.equals("Ok.")) {
                throw new SimpleBizException("添加下载任务失败");
            }
        }

        return files;
    }

    public void checkDownloadFileIsFinish(Files files) throws IOException {
        Map<String, Object> info = this.loadInfo(files.getDhtHash());
        if (info == null) {
            files.setDeleted(true);
            files.setDeleteAt(LocalDateTime.now());
            this.filesDao.save(files);
            return;
        }
        Integer completionOn = (Integer) info.get("completion_on");
        String fileName = (String) info.get("name");
        if (completionOn > 0) {
            // 2. 检查是否超过 2 小时 (2 * 60 * 60 = 7200 秒)
            long nowSecond = System.currentTimeMillis() / 1000;
            long diffSeconds = nowSecond - completionOn;

            if (diffSeconds < 7200) {
                log.info("种子 [{}] 下载完成不足 2 小时，跳过处理。剩余等待时间: {} 分钟", files.getName(), (7200 - diffSeconds) / 60);
                return; // 时间未到，直接返回，不执行后续的 Copy 和 Delete 逻辑
            }
            // 下载完成，更新信息
            files.setName(fileName);
            File fold = new File(this.fileService.getRootPath(), String.valueOf(files.getFoldId()));
            if (!fold.exists() && !fold.mkdir()) {
                log.info("文件夹[{}]创建失败", fold.getAbsolutePath());
                return;
            }
            File srcFile = new File((String) info.get("content_path"));
            if (srcFile.exists()) {
                File targetFile = new File(fold, srcFile.getName());
                if (!targetFile.exists()) {
                    if (srcFile.isFile()) {
                        if (srcFile.exists()) {
                            java.nio.file.Files.copy(srcFile.toPath(), targetFile.toPath());
                        }
                        files.setName(targetFile.getName());
                        files.setStoragePath(targetFile.getAbsolutePath());
                        files.setUpdateAt(LocalDateTime.now());
                        files.setStatus(FileStatusConstant.INIT);
                        files.setSize(targetFile.length());
                        String fileType = this.fileService.getFileType(files.getName());
                        if (fileType != null) {
                            files.setFileType(fileType);
                        }
                        this.filesDao.save(files);
                        CompletableFuture.runAsync(() -> this.fileService.saveSample(files));
                    } else if (srcFile.isDirectory()) {
                        Long newFoldId = this.fileService.addFold(srcFile.getName(), files.getFoldId(), files.getUserId());
                        this.fileService.movePhysicsDir(newFoldId, srcFile, files.getUserId());
                        files.setDeleted(true);
                        files.setDeleteAt(LocalDateTime.now());
                        this.filesDao.save(files);
                    }
                }
                this.logsDao.addLog("文件下载完成", files.getName(), files.getUserId());

            }
            // 删除下载记录
            this.send(DELETE_PATH, Map.of("hashes", files.getDhtHash(), "deleteFiles", "true"));
        } else if (!files.getName().equals(fileName)) {
            // 更新文件名称
            files.setName(fileName);
            this.filesDao.save(files);
        }
    }
}
