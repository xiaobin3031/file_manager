package com.xiaobin.home.service;

import com.xiaobin.home.entity.FileDownloadPlan;
import com.xiaobin.home.repository.FileDownloadPlanDao;
import lombok.extern.slf4j.Slf4j;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FileDownloadPlanService {

    @Autowired
    private FileDownloadPlanDao fileDownloadPlanDao;
    @Autowired
    private FileDownloadService fileDownloadService;

    public void planDownloadList() {
        List<FileDownloadPlan> planList = this.fileDownloadPlanDao.findByFinishAndDeletedFalse(false);
        if (!planList.isEmpty()) {
            for (FileDownloadPlan plan : planList) {
                try {
                    this.planDownload(plan);
                } catch (Exception e) {
                    log.error("计划下载失败：{}", e.getMessage());
                }
            }
        }
    }

    public String loadHtmlFromUrl(String urlString) {
        // 2. 使用 Java URL 读取网页内容
        String html = null;
        try {
            URL url = URI.create(urlString).toURL();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                StringBuilder htmlBuilder = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    htmlBuilder.append(inputLine);
                }
                html = htmlBuilder.toString();
            }
        } catch (Exception e) {
            log.error("加载网页失败：{}", e.getMessage());
        }

        return html;
    }

    private NodeList loadHtml(String url, String xpathExp) {
        String html = this.loadHtmlFromUrl(url);
        if (html == null) return null;

        // 3. 使用 HtmlCleaner 解析 HTML
        HtmlCleaner cleaner = new HtmlCleaner();
        TagNode rootNode = cleaner.clean(html);

        try {
            // 4. 转换为 DOM，用于 XPath 查询
            Document dom = new DomSerializer(cleaner.getProperties(), true).createDOM(rootNode);

            // 5. XPath 查询示例
            XPath xpath = XPathFactory.newInstance().newXPath();

            return (NodeList) xpath.evaluate(xpathExp, dom, XPathConstants.NODESET);
        } catch (Exception e) {
            log.info("xpath解析失败: {}", e.getMessage());
        }
        return null;
    }

    public void planDownload(FileDownloadPlan plan) throws ParserConfigurationException, XPathExpressionException {
        NodeList nodes = this.loadHtml(plan.getUrl(), plan.getXpathExpression());
        if (nodes == null) return;

        List<String> magnetList = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            String magnet = nodes.item(i).getTextContent();
            if (magnet.equals(plan.getCurrentName())) {
                break;
            }
            magnetList.add(magnet);
        }
        if (!magnetList.isEmpty()) {
            for (String s : magnetList) {
                this.fileDownloadService.downloadFromMagnet(s, plan.getUserId(), plan.getFoldId());
            }
            plan.setCurrentName(magnetList.getFirst());
            if (plan.getLastFileTime() == null) {
                plan.setSeparatorDays(7);
            } else {
                Duration duration = Duration.between(plan.getLastFileTime(), LocalDateTime.now());
                long days = duration.toDays();
                if (days <= 0) days = 1;
                plan.setSeparatorDays((int) days);
            }
            plan.setLastFileTime(LocalDateTime.now());
            this.fileDownloadPlanDao.save(plan);
        } else {
            if (plan.getLastFileTime() == null) {
                plan.setLastFileTime(LocalDateTime.now());
                plan.setSeparatorDays(7);
                this.fileDownloadPlanDao.save(plan);
            } else {
                Duration duration = Duration.between(plan.getLastFileTime(), LocalDateTime.now());
                long days = duration.toDays();
                if (days <= 0) days = 1;
                if (days > plan.getSeparatorDays() * 2) {
                    plan.setFinish(true);
                    this.fileDownloadPlanDao.save(plan);
                }
            }
        }
    }

    public static void main(String[] args) throws ParserConfigurationException, XPathExpressionException, IOException, InterruptedException {
        HttpClient CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://biccam.com/chapters/106111/1-51122f.jpg"))
                .header("Referer", "https://mycomic.com/")
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();

        File file = new File("a.jpg");
        HttpResponse<Path> response = CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofFile(file.toPath())
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException("下载失败: " + response.statusCode());
        }
    }

}
