package com.xiaobin.home.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;

public class TorrentHashUtil {

    public static String infoHash(Path torrentFile) throws Exception {
        byte[] bytes = Files.readAllBytes(torrentFile);
        return infoHash(bytes);
    }

    public static String infoHash(byte[] bytes) throws Exception {
        int infoStart = findInfoDictStart(bytes);
        int infoEnd = findBencodeEnd(bytes, infoStart);
        byte[] infoBytes = Arrays.copyOfRange(bytes, infoStart, infoEnd);
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] hash = sha1.digest(infoBytes);
        return toHex(hash);
    }

    /**
     * 找到 info 字典开始位置
     */
    private static int findInfoDictStart(byte[] bytes) throws IOException {

        byte[] pattern = "4:info".getBytes();
        for (int i = 0; i < bytes.length - pattern.length; i++) {
            boolean match = true;
            for (int j = 0; j < pattern.length; j++) {
                if (bytes[i + j] != pattern[j]) {
                    match = false;
                    break;
                }
            }

            if (match) return i + pattern.length;
        }
        throw new IOException("info dictionary not found");
    }

    /**
     * 找到一个 bencode 对象结束位置
     */
    private static int findBencodeEnd(byte[] data, int start) {
        int[] pos = {start};
        parse(data, pos);
        return pos[0];
    }

    private static void parse(byte[] data, int[] pos) {
        byte b = data[pos[0]];
        switch (b) {
            case 'd':
                pos[0]++;
                while (data[pos[0]] != 'e') {
                    parse(data, pos); // key
                    parse(data, pos); // value
                }
                pos[0]++;
                break;
            case 'l':
                pos[0]++;
                while (data[pos[0]] != 'e') {
                    parse(data, pos);
                }
                pos[0]++;
                break;
            case 'i':
                do {
                    pos[0]++;
                } while (data[pos[0]] != 'e');
                pos[0]++;
                break;
            default:
                if (Character.isDigit((char) b)) {
                    int len = 0;
                    while (data[pos[0]] != ':') {
                        len = len * 10 + (data[pos[0]] - '0');
                        pos[0]++;
                    }
                    pos[0]++;
                    pos[0] += len;
                    break;
                }
                throw new IllegalArgumentException("invalid bencode at " + pos[0]);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        File file = new File("/Users/lixiaolin/Downloads/【悠哈璃羽字幕组 &amp;amp; jsum】[Sakura Taisen Katsudou Shashin][剧场版 樱花大战 活动写真][GB][BDRIP][1920x1040][Movie].torrent");
        String hash = infoHash(file.toPath());
        System.out.println(hash);
    }
}
