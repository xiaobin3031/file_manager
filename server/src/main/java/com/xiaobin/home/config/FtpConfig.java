package com.xiaobin.home.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties("ftp")
@Configuration
public class FtpConfig {


    private String rootPath;

    private String previewPath;

    private String tmpPath;

    private Qbittorrent qbittorrent;

    @Getter
    @Setter
    public static class Qbittorrent {
        private String urlPrefix;

        private String username;

        private String password;
    }
}
