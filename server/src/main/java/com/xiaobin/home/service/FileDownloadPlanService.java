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
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
            URL url = new URL(urlString);
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

    public void planDownload(FileDownloadPlan plan) throws ParserConfigurationException, XPathExpressionException {

        String html = this.loadHtmlFromUrl(plan.getUrl());
        if (html == null) return;

        // 3. 使用 HtmlCleaner 解析 HTML
        HtmlCleaner cleaner = new HtmlCleaner();
        TagNode rootNode = cleaner.clean(html);

        // 4. 转换为 DOM，用于 XPath 查询
        Document dom = new DomSerializer(cleaner.getProperties(), true).createDOM(rootNode);

        // 5. XPath 查询示例
        XPath xpath = XPathFactory.newInstance().newXPath();

        NodeList nodes = (NodeList) xpath.evaluate(plan.getXpathExpression(), dom, XPathConstants.NODESET);

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
}
