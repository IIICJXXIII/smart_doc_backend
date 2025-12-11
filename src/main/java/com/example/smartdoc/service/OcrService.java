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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

        return callSmartFinanceOcr(fileBytes);
    }

    /**
     * 策略 A: 智能财务票据识别
     * 注意：此接口参数必须为 HashMap<String, Object>
     */
    private InvoiceData callSmartFinanceOcr(byte[] imageBytes) {
        try {
            // 🟢 这里的泛型是 <String, Object>
            HashMap<String, Object> options = new HashMap<>();
            options.put("probability", "true");

            JSONObject res = client.multipleInvoice(imageBytes, options);

            if (res.has("words_result")) {
                JSONArray results = res.getJSONArray("words_result");
                if (results.length() == 0) return callGeneralOcr(imageBytes);

                JSONObject bestTicket = results.getJSONObject(0);
                String type = bestTicket.optString("type", "unknown");

                if (!bestTicket.has("result")) {
                    System.out.println("⚠️ 票据类型 [" + type + "] 不含详细结构，切换通用识别...");
                    return callGeneralOcr(imageBytes);
                }

                JSONObject content = bestTicket.getJSONObject("result");
                InvoiceData data = new InvoiceData();
                data.setRawImageUrl("memory_image");

                switch (type) {
                    case "vat_invoice":
                        parseVatInvoice(content, data);
                        break;
                    case "train_ticket":
                        parseTrainTicket(content, data);
                        break;
                    case "air_ticket":
                        parseAirTicket(content, data);
                        break;
                    case "taxi_receipt":
                        parseTaxiReceipt(content, data);
                        break;
                    case "quota_invoice":
                        parseQuotaInvoice(content, data);
                        break;
                    case "taxi_online_ticket":
                        parseTaxiOnline(content, data);
                        break;
                    default:
                        data.setMerchantName("票据类型: " + type);
                        data.setCategory("其他");
                        data.setAmount(getDouble(content, "Amount", "TotalAmount", "total_fare", "fare", "money"));
                        data.setDate(getValue(content, "Date", "date", "Time"));
                }

                postProcess(data);
                return data;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return callGeneralOcr(imageBytes);
    }

    /**
     * 策略 B: 通用文字识别 (正则提取)
     * 注意：此接口参数必须为 HashMap<String, String>，否则会报编译错误
     */
    private InvoiceData callGeneralOcr(byte[] imageBytes) {
        InvoiceData data = new InvoiceData();
        data.setMerchantName("未知商户(通用识别)");
        data.setCategory("其他");
        data.setItemName("扫描件");

        try {
            // 🔴 关键修复：这里的泛型必须改回 <String, String>
            HashMap<String, String> options = new HashMap<>();
            options.put("detect_direction", "true");

            JSONObject res = client.basicAccurateGeneral(imageBytes, options);

            if (res.has("words_result")) {
                parseWordsToInvoice(res.getJSONArray("words_result"), data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    // ================= 专用解析方法区 =================

    private void parseTrainTicket(JSONObject r, InvoiceData data) {
        data.setCategory("交通出行");
        String trainNum = getValue(r, "train_num");
        String start = getValue(r, "starting_station");
        String end = getValue(r, "destination_station");

        String itemName = "火车票";
        if (trainNum != null) itemName += " " + trainNum;
        if (start != null && end != null) itemName += " (" + start + "-" + end + ")";
        data.setItemName(itemName);

        data.setMerchantName("铁路客运");
        data.setAmount(getDouble(r, "ticket_rates"));
        data.setDate(getValue(r, "date"));
        data.setInvoiceCode(getValue(r, "ticket_num"));
    }

    private void parseAirTicket(JSONObject r, InvoiceData data) {
        data.setCategory("交通出行");
        String carrier = getValue(r, "carrier");
        String flight = getValue(r, "flight");
        String start = getValue(r, "starting_station");
        String end = getValue(r, "destination_station");

        data.setMerchantName(carrier != null ? carrier : "航空公司");

        String itemName = "机票";
        if (flight != null) itemName += " " + flight;
        if (start != null && end != null) itemName += " (" + start + "-" + end + ")";
        data.setItemName(itemName);

        data.setAmount(getDouble(r, "ticket_rates", "fare", "TotalAmount"));
        data.setDate(getValue(r, "date"));
        data.setInvoiceCode(getValue(r, "ticket_number"));
    }

    private void parseTaxiReceipt(JSONObject r, InvoiceData data) {
        data.setCategory("交通出行");
        data.setItemName("出租车费");
        data.setMerchantName("出租车 " + getValue(r, "TaxiNum"));
        data.setAmount(getDouble(r, "TotalFare", "Fare"));
        data.setDate(getValue(r, "Date"));
        data.setInvoiceCode(getValue(r, "InvoiceCode"));
    }

    private void parseTaxiOnline(JSONObject r, InvoiceData data) {
        data.setCategory("交通出行");
        String provider = getValue(r, "service_provider");
        data.setMerchantName(provider != null ? provider : "网约车");
        data.setItemName("网约车行程");
        data.setAmount(getDouble(r, "total_fare"));
        data.setDate(getValue(r, "application_date"));
    }

    private void parseVatInvoice(JSONObject r, InvoiceData data) {
        data.setMerchantName(getValue(r, "SellerName"));
        data.setAmount(getDouble(r, "TotalAmount", "AmountInFiguers"));
        data.setDate(getValue(r, "InvoiceDate"));
        data.setInvoiceCode(getValue(r, "InvoiceNum"));
        if (data.getInvoiceCode() == null) data.setInvoiceCode(getValue(r, "InvoiceCode"));
        String item = getValue(r, "CommodityName");
        data.setItemName(item != null ? item : "办公用品/服务费");
    }

    private void parseQuotaInvoice(JSONObject r, InvoiceData data) {
        data.setCategory("餐饮美食");
        data.setAmount(getDouble(r, "invoice_rate", "invoice_rate_in_figure"));
        data.setInvoiceCode(getValue(r, "invoice_number"));
        data.setMerchantName("定额发票");
        data.setItemName("定额消费");
    }

    // --- 通用正则解析 ---
    private void parseWordsToInvoice(JSONArray words, InvoiceData data) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < words.length(); i++) {
            lines.add(words.getJSONObject(i).getString("words"));
        }
        double maxAmount = 0.0;
        for (String line : lines) {
            Matcher m = Pattern.compile("(\\d{1,3}(,\\d{3})*\\.\\d{2})").matcher(line);
            while (m.find()) {
                try {
                    double v = Double.parseDouble(m.group(1).replace(",", ""));
                    if (v > maxAmount && v < 1000000) maxAmount = v;
                } catch (Exception e) {}
            }
        }
        if (maxAmount > 0) data.setAmount(maxAmount);
        for (String line : lines) {
            Matcher m = Pattern.compile("202\\d[-年/.]\\d{1,2}[-月/.]\\d{1,2}").matcher(line);
            if (m.find()) {
                data.setDate(m.group().replaceAll("[年月/.]", "-"));
                break;
            }
        }
        String fullText = String.join(" ", lines);
        if (fullText.contains("餐饮") || fullText.contains("饭")) data.setCategory("餐饮美食");
        else if (fullText.contains("车") || fullText.contains("交通")) data.setCategory("交通出行");
    }

    // ================= 工具方法 =================

    private String getValue(JSONObject obj, String... possibleKeys) {
        for (String key : possibleKeys) {
            if (obj.has(key)) {
                JSONArray arr = obj.getJSONArray(key);
                if (arr.length() > 0) {
                    return arr.getJSONObject(0).optString("word", null);
                }
            }
        }
        return null;
    }

    private Double getDouble(JSONObject obj, String... keys) {
        String val = getValue(obj, keys);
        if (val != null) {
            try {
                String numStr = val.replaceAll("[^0-9.]", "");
                return Double.parseDouble(numStr);
            } catch (Exception e) {}
        }
        return 0.0;
    }

    private void postProcess(InvoiceData data) {
        if (data.getDate() != null) {
            String d = data.getDate().replaceAll("[年月/.]", "-").replace("日", "");
            Matcher m = Pattern.compile("\\d{4}-\\d{1,2}-\\d{1,2}").matcher(d);
            if (m.find()) data.setDate(m.group());
        }
        if (data.getCategory() == null) {
            data.setCategory("其他");
        }
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
}