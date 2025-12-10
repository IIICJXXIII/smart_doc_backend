package com.example.smartdoc.service;

import com.baidu.aip.ocr.AipOcr;
import com.example.smartdoc.model.InvoiceData;
import jakarta.annotation.PostConstruct;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrService {

    @Value("${baidu.ocr.app-id}")
    private String appId;
    @Value("${baidu.ocr.api-key}")
    private String apiKey;
    @Value("${baidu.ocr.secret-key}")
    private String secretKey;

    private AipOcr client;

    @PostConstruct
    public void init() {
        client = new AipOcr(appId, apiKey, secretKey);
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);
    }

    public InvoiceData processDocument(MultipartFile file) throws IOException {
        byte[] fileBytes;
        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
            fileBytes = convertPdfToJpg(file.getBytes());
        } else {
            fileBytes = file.getBytes();
        }
        return callBaiduOcr(fileBytes);
    }

    private byte[] convertPdfToJpg(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(pdfBytes);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImage(0, 2.0f, ImageType.RGB);
            ImageIO.write(image, "jpg", baos);
            return baos.toByteArray();
        }
    }

    private InvoiceData callBaiduOcr(byte[] imageBytes) {
        InvoiceData data = new InvoiceData();
        data.setRawImageUrl("memory_image");
        HashMap<String, String> options = new HashMap<>();
        options.put("detect_direction", "true");
        options.put("probability", "true");

        JSONObject res = client.basicAccurateGeneral(imageBytes, options);

        if (res.has("words_result")) {
            parseWordsToInvoice(res.getJSONArray("words_result"), data);
        } else {
            System.err.println("❌ 百度OCR报错: " + res.toString());
            data.setMerchantName("识别异常: " + res.optString("error_msg"));
        }
        return data;
    }

    /**
     * 核心解析算法 v3.0：支持提取项目名称、发票号码及智能分类
     */
    private void parseWordsToInvoice(JSONArray words, InvoiceData data) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < words.length(); i++) {
            lines.add(words.getJSONObject(i).getString("words"));
        }

        // --- 1. 提取发票号码 (Invoice Number) ---
        // 策略：找 "发票号码" 关键词，或者直接找 20 位连续数字
        for (String line : lines) {
            // 你的样例中：发票号码:25332000000561515815
            if (line.contains("发票号码")) {
                String num = line.replaceAll(".*发票号码[:：]*", "").trim();
                // 提取其中的数字部分
                Matcher m = Pattern.compile("\\d+").matcher(num);
                if (m.find()) {
                    data.setInvoiceCode(m.group());
                }
                break;
            }
        }
        // 兜底：如果没找到关键词，找单独的一行长数字 (通常是20位或10/12位)
        if (data.getInvoiceCode() == null) {
            for (String line : lines) {
                if (line.matches("^\\d{10,20}$")) {
                    data.setInvoiceCode(line);
                    break;
                }
            }
        }

        // --- 2. 提取项目名称 (Item Name) & 强分类 ---
        // 策略：电子发票通常格式为 "*分类简称*具体名称" (例如 *软饮料*农夫山泉)
        for (String line : lines) {
            // 正则逻辑：匹配两个星号中间有文字，后面还有文字的格式
            // 例子： *软饮料*农夫山泉 -> group(1)=软饮料, group(2)=农夫山泉
            Matcher itemMatcher = Pattern.compile("\\*([^*]+)\\*(.+)").matcher(line);

            if (itemMatcher.find()) {
                String categoryRaw = itemMatcher.group(1); // 分类简称 (如: 软饮料)
                String itemName = itemMatcher.group(2);    // 具体物品 (如: 农夫山泉...)

                data.setItemName(itemName.trim());

                // --- 基于项目名称的“绝对准确”分类 ---
                if (categoryRaw.contains("餐饮") || categoryRaw.contains("饮食")) {
                    data.setCategory("餐饮美食");
                } else if (categoryRaw.contains("饮料") || categoryRaw.contains("食品")) {
                    data.setCategory("餐饮美食"); // 饮料也归为餐饮
                } else if (categoryRaw.contains("交通") || categoryRaw.contains("运输")) {
                    data.setCategory("交通出行");
                } else if (categoryRaw.contains("信息") || categoryRaw.contains("通信")) {
                    data.setCategory("通讯网络");
                } else if (categoryRaw.contains("办公") || categoryRaw.contains("纸")) {
                    data.setCategory("办公耗材");
                }
                break; // 找到第一个主要项目就停止
            }
        }

        // --- 3. 提取日期 (常规逻辑) ---
        if (data.getDate() == null) {
            for (String line : lines) {
                Matcher m = Pattern.compile("202\\d[-年/.]\\d{1,2}[-月/.]\\d{1,2}").matcher(line);
                if (m.find()) {
                    data.setDate(m.group().replaceAll("[年月/.]", "-").replace("日", ""));
                    break;
                }
            }
        }

        // --- 4. 提取金额 (价税合计优先) ---
        boolean amountFound = false;
        for (String line : lines) {
            if (line.contains("价税合计") || line.contains("小写")) {
                Matcher m = Pattern.compile("(\\d+\\.\\d{2})").matcher(line);
                String temp = null;
                while (m.find()) temp = m.group(1);
                if (temp != null) {
                    try { data.setAmount(Double.parseDouble(temp)); amountFound = true; break; } catch (Exception e) {}
                }
            }
        }
        if (!amountFound) {
            double max = 0.0;
            for (String line : lines) {
                if (line.contains("￥") || line.contains("¥")) {
                    Matcher m = Pattern.compile("(\\d+\\.\\d{2})").matcher(line);
                    while (m.find()) {
                        try { double v = Double.parseDouble(m.group(1)); if (v > max) max = v; } catch (Exception e) {}
                    }
                }
            }
            if (max > 0) data.setAmount(max);
        }

        // --- 5. 提取商户 (排除买方逻辑) ---
        String potentialSeller = null;
        boolean nextIsSeller = false;
        for (String line : lines) {
            if (line.contains("销售方") && line.contains("名称")) {
                String s = line.substring(line.indexOf("名称")).replaceAll("名称[:：\\s]*", "");
                if (s.length() > 2) { data.setMerchantName(s); return; }
            }
            if (line.contains("销售方")) { nextIsSeller = true; continue; }
            if (nextIsSeller) {
                if (line.contains("名称")) { data.setMerchantName(line.replaceAll("名称[:：\\s]*", "")); return; }
                if (line.contains("公司") || line.contains("店")) { data.setMerchantName(line); return; }
            }
            if (line.contains("名称") && !line.contains("购买方") && !line.contains("大学") && !line.contains("师范")) {
                potentialSeller = line.replaceAll("名称[:：\\s]*", "");
            }
        }
        if (data.getMerchantName() == null && potentialSeller != null) data.setMerchantName(potentialSeller);

        // --- 6. 兜底分类 (如果上面没根据项目名分出来) ---
        if (data.getCategory() == null) {
            String fullText = String.join(" ", lines);
            if (fullText.contains("餐饮") || fullText.contains("美食")) data.setCategory("餐饮美食");
            else if (fullText.contains("交通") || fullText.contains("车")) data.setCategory("交通出行");
            else if (fullText.contains("办公")) data.setCategory("办公耗材");
            else data.setCategory("其他");
        }
    }
}