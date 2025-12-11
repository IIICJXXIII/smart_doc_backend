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

/**
 * OCR 识别服务 - 基于百度 AI 的发票识别核心服务
 * 
 * <p>使用百度 OCR API 实现多种类型票据的自动识别，
 * 采用"智能财务票据识别优先 + 通用文字识别兜底"的双层策略。</p>
 * 
 * <h3>支持的票据类型:</h3>
 * <ul>
 *   <li>增值税发票 (vat_invoice)</li>
 *   <li>火车票 (train_ticket)</li>
 *   <li>机票行程单 (air_ticket)</li>
 *   <li>出租车票 (taxi_receipt)</li>
 *   <li>网约车发票 (taxi_online_ticket)</li>
 *   <li>定额发票 (quota_invoice)</li>
 *   <li>其他票据 → 通用识别 + 正则提取</li>
 * </ul>
 * 
 * <h3>识别策略:</h3>
 * <pre>
 * 1. 优先调用 multipleInvoice (智能财务票据识别)
 * 2. 若无法识别或结构不完整，降级到 basicAccurateGeneral (通用文字识别)
 * 3. 通用识别时使用正则表达式提取金额、日期等关键信息
 * </pre>
 * 
 * <h3>配置项:</h3>
 * <pre>
 * baidu.ocr.app-id=xxx
 * baidu.ocr.api-key=xxx
 * baidu.ocr.secret-key=xxx
 * </pre>
 * 
 * @author SmartDoc Team
 * @see com.example.smartdoc.controller.DocController
 */
@Service
public class OcrService {

    /** 百度 OCR 应用 ID */
    @Value("${baidu.ocr.app-id}")
    private String appId;
    
    /** 百度 OCR API Key */
    @Value("${baidu.ocr.api-key}")
    private String apiKey;
    
    /** 百度 OCR Secret Key */
    @Value("${baidu.ocr.secret-key}")
    private String secretKey;

    /** 百度 OCR 客户端实例 */
    private AipOcr client;

    /**
     * 初始化百度 OCR 客户端
     * <p>在 Spring Bean 创建后自动执行，配置连接超时参数。</p>
     */
    @PostConstruct
    public void init() {
        client = new AipOcr(appId, apiKey, secretKey);
        client.setConnectionTimeoutInMillis(2000);   // 连接超时 2秒
        client.setSocketTimeoutInMillis(60000);      // 读取超时 60秒
    }

    /**
     * 处理上传的发票文档
     * <p>支持图片和 PDF 格式，PDF 会先转换为图片再识别。</p>
     * 
     * @param file 上传的发票文件
     * @return 识别后的发票数据对象
     * @throws IOException 文件读取异常
     */
    public InvoiceData processDocument(MultipartFile file) throws IOException {
        byte[] fileBytes;
        String fileName = file.getOriginalFilename();
        
        // PDF 文件需要先转换为图片
        if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
            fileBytes = convertPdfToJpg(file.getBytes());
        } else {
            fileBytes = file.getBytes();
        }

        return callSmartFinanceOcr(fileBytes);
    }

    /**
     * 策略 A: 智能财务票据识别 (优先策略)
     * <p>调用百度 multipleInvoice 接口，可自动识别多种票据类型并返回结构化数据。</p>
     * 
     * @param imageBytes 图片字节数组
     * @return 识别后的发票数据
     */
    private InvoiceData callSmartFinanceOcr(byte[] imageBytes) {
        try {
            // 设置识别参数（注意: multipleInvoice 接口要求 HashMap<String, Object>）
            HashMap<String, Object> options = new HashMap<>();
            options.put("probability", "true");  // 返回置信度

            JSONObject res = client.multipleInvoice(imageBytes, options);

            if (res.has("words_result")) {
                JSONArray results = res.getJSONArray("words_result");
                if (results.length() == 0) return callGeneralOcr(imageBytes);

                // 取第一个识别结果（通常是主票据）
                JSONObject bestTicket = results.getJSONObject(0);
                String type = bestTicket.optString("type", "unknown");

                // 检查是否有详细结构化数据
                if (!bestTicket.has("result")) {
                    System.out.println("⚠️ 票据类型 [" + type + "] 不含详细结构，切换通用识别...");
                    return callGeneralOcr(imageBytes);
                }

                JSONObject content = bestTicket.getJSONObject("result");
                InvoiceData data = new InvoiceData();
                data.setRawImageUrl("memory_image");

                // 根据票据类型调用对应的解析方法
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
                        // 未知类型使用通用提取
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
        // 降级到通用识别
        return callGeneralOcr(imageBytes);
    }

    /**
     * 策略 B: 通用文字识别 (兜底策略)
     * <p>当智能财务票据识别失败时，使用通用 OCR + 正则表达式提取关键信息。</p>
     * 
     * @param imageBytes 图片字节数组
     * @return 识别后的发票数据
     */
    private InvoiceData callGeneralOcr(byte[] imageBytes) {
        InvoiceData data = new InvoiceData();
        data.setMerchantName("未知商户(通用识别)");
        data.setCategory("其他");
        data.setItemName("扫描件");

        try {
            // 设置识别参数（注意: basicAccurateGeneral 接口要求 HashMap<String, String>）
            HashMap<String, String> options = new HashMap<>();
            options.put("detect_direction", "true");  // 自动检测图片方向

            JSONObject res = client.basicAccurateGeneral(imageBytes, options);

            if (res.has("words_result")) {
                parseWordsToInvoice(res.getJSONArray("words_result"), data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    // ==================== 专用票据解析方法 ====================

    /**
     * 解析火车票
     * <p>提取车次、出发站、到达站、票价、日期等信息。</p>
     */
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

    /**
     * 解析机票行程单
     * <p>提取航空公司、航班号、起降站、票价等信息。</p>
     */
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

    /**
     * 解析出租车票
     * <p>提取车牌号、车费、日期等信息。</p>
     */
    private void parseTaxiReceipt(JSONObject r, InvoiceData data) {
        data.setCategory("交通出行");
        data.setItemName("出租车费");
        data.setMerchantName("出租车 " + getValue(r, "TaxiNum"));
        data.setAmount(getDouble(r, "TotalFare", "Fare"));
        data.setDate(getValue(r, "Date"));
        data.setInvoiceCode(getValue(r, "InvoiceCode"));
    }

    /**
     * 解析网约车发票
     * <p>提取服务商、行程费用、日期等信息。</p>
     */
    private void parseTaxiOnline(JSONObject r, InvoiceData data) {
        data.setCategory("交通出行");
        String provider = getValue(r, "service_provider");
        data.setMerchantName(provider != null ? provider : "网约车");
        data.setItemName("网约车行程");
        data.setAmount(getDouble(r, "total_fare"));
        data.setDate(getValue(r, "application_date"));
    }

    /**
     * 解析增值税发票
     * <p>提取销售方名称、金额、日期、发票号码等信息。</p>
     */
    private void parseVatInvoice(JSONObject r, InvoiceData data) {
        data.setMerchantName(getValue(r, "SellerName"));
        data.setAmount(getDouble(r, "TotalAmount", "AmountInFiguers"));
        data.setDate(getValue(r, "InvoiceDate"));
        data.setInvoiceCode(getValue(r, "InvoiceNum"));
        if (data.getInvoiceCode() == null) data.setInvoiceCode(getValue(r, "InvoiceCode"));
        String item = getValue(r, "CommodityName");
        data.setItemName(item != null ? item : "办公用品/服务费");
    }

    /**
     * 解析定额发票
     * <p>定额发票通常用于餐饮消费，提取金额和发票号码。</p>
     */
    private void parseQuotaInvoice(JSONObject r, InvoiceData data) {
        data.setCategory("餐饮美食");
        data.setAmount(getDouble(r, "invoice_rate", "invoice_rate_in_figure"));
        data.setInvoiceCode(getValue(r, "invoice_number"));
        data.setMerchantName("定额发票");
        data.setItemName("定额消费");
    }

    /**
     * 通用文字识别结果解析
     * <p>使用正则表达式从 OCR 文字结果中提取金额、日期、类别等信息。
     * 金额提取策略：找出所有金额格式的数字，取最大值（通常是总金额）。
     * 日期提取策略：匹配 2020-2029 年的日期格式。
     * 类别识别：根据关键词判断消费类别。</p>
     */
    private void parseWordsToInvoice(JSONArray words, InvoiceData data) {
        // 将所有识别文字合并为行列表
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < words.length(); i++) {
            lines.add(words.getJSONObject(i).getString("words"));
        }
        
        // 提取金额（找最大值，假设为总金额）
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
        
        // 提取日期（匹配 202X年XX月XX日 或 202X-XX-XX 格式）
        for (String line : lines) {
            Matcher m = Pattern.compile("202\\d[-年/.]\\d{1,2}[-月/.]\\d{1,2}").matcher(line);
            if (m.find()) {
                data.setDate(m.group().replaceAll("[年月/.]", "-"));
                break;
            }
        }
        
        // 根据关键词推断消费类别
        String fullText = String.join(" ", lines);
        if (fullText.contains("餐饮") || fullText.contains("饭")) data.setCategory("餐饮美食");
        else if (fullText.contains("车") || fullText.contains("交通")) data.setCategory("交通出行");
    }

    // ==================== 工具方法 ====================

    /**
     * 从百度 OCR 返回的 JSON 结构中提取字段值
     * <p>百度 OCR 的字段值是数组格式 [{word: "xxx"}]，此方法处理该结构。</p>
     * 
     * @param obj          OCR 结果 JSON 对象
     * @param possibleKeys 可能的字段名（支持多个备选）
     * @return 提取的字符串值，未找到返回 null
     */
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

    /**
     * 从百度 OCR 返回的 JSON 结构中提取数值
     * <p>自动清理非数字字符后解析为 Double。</p>
     * 
     * @param obj  OCR 结果 JSON 对象
     * @param keys 可能的字段名
     * @return 提取的数值，解析失败返回 0.0
     */
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

    /**
     * 数据后处理 - 标准化日期格式和补全默认值
     * <p>将各种日期格式统一转换为 yyyy-MM-dd 格式，
     * 并为缺失的类别设置默认值。</p>
     * 
     * @param data 待处理的发票数据
     */
    private void postProcess(InvoiceData data) {
        // 标准化日期格式
        if (data.getDate() != null) {
            String d = data.getDate().replaceAll("[年月/.]", "-").replace("日", "");
            Matcher m = Pattern.compile("\\d{4}-\\d{1,2}-\\d{1,2}").matcher(d);
            if (m.find()) data.setDate(m.group());
        }
        // 默认类别为"其他"
        if (data.getCategory() == null) {
            data.setCategory("其他");
        }
    }

    /**
     * PDF 转 JPG 图片
     * <p>使用 Apache PDFBox 将 PDF 首页渲染为 JPG 图片，
     * 以便发送给百度 OCR 进行识别。</p>
     * 
     * @param pdfBytes PDF 文件字节数组
     * @return JPG 图片字节数组
     * @throws IOException 转换异常
     */
    private byte[] convertPdfToJpg(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(pdfBytes);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDFRenderer renderer = new PDFRenderer(document);
            // 以 2.0 倍缩放渲染首页，RGB 格式
            BufferedImage image = renderer.renderImage(0, 2.0f, ImageType.RGB);
            ImageIO.write(image, "jpg", baos);
            return baos.toByteArray();
        }
    }
}