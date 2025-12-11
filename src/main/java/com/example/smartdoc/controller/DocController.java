package com.example.smartdoc.controller;

import com.example.smartdoc.model.InvoiceData;
import com.example.smartdoc.model.User;
import com.example.smartdoc.repository.InvoiceRepository;
import com.example.smartdoc.service.OcrService;
import com.example.smartdoc.utils.AnomalyDetectionUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.List;

/**
 * 此时票据管理核心控制器
 * 负责发票的上传识别、保存归档、查询列表、导出 Excel 以及删除功能
 * 集成了 OCR 服务和异常检测算法
 */
@RestController
@RequestMapping("/api/doc")
@CrossOrigin(origins = "*")
public class DocController {

    @Autowired
    private OcrService ocrService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private HttpServletRequest request; // 注入 request 以获取 Header

    /**
     * 上传发票图片或PDF进行 OCR 识别
     * 不会将数据存入数据库，仅返回识别结果供前端确认
     *
     * @param file 上传的文件
     * @return 识别出的发票结构化数据
     */
    // 1. 上传识别 (不需要改，识别不涉及存库)
    @PostMapping("/upload")
    public InvoiceData uploadAndAnalyze(@RequestParam("file") MultipartFile file) {
        try {
            return ocrService.processDocument(file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 保存发票数据到数据库
     * 会自动触发金额异常检测算法，标记疑似异常消费
     *
     * @param data 从前端确认并提交的发票数据
     * @return 操作结果
     */
    // 2. 保存归档 (Create) - 绑定当前用户
    @PostMapping("/save")
    public String saveDoc(@RequestBody InvoiceData data) {
        User currentUser = getCurrentUser();
        if (currentUser == null)
            return "error: not login";

        data.setUserId(currentUser.getId());

        // --- 🔥 核心升级：触发异常检测算法 ---
        try {
            // 1. 取出该用户、该分类下的所有历史金额 (作为训练数据)
            List<InvoiceData> historyList = invoiceRepository.findByUserIdAndCategoryOrderByIdDesc(
                    currentUser.getId(),
                    data.getCategory() // 只跟同类别的比，比如餐饮只跟餐饮比
            );

            // 提取金额列表
            List<Double> historyAmounts = historyList.stream()
                    .map(InvoiceData::getAmount)
                    .toList(); // JDK 16+ 写法，如果是旧版用 .collect(Collectors.toList())

            // 只有历史数据足够多(比如大于5条)才开始检测，否则样本太少不准
            if (historyAmounts.size() >= 5) {
                double mean = AnomalyDetectionUtil.calculateMean(historyAmounts);
                double stdDev = AnomalyDetectionUtil.calculateStdDev(historyAmounts, mean);

                // 2. 算法判定
                boolean isWeird = AnomalyDetectionUtil.isAnomaly(data.getAmount(), mean, stdDev);

                // 3. 打标
                data.setIsAnomaly(isWeird ? 1 : 0);

                if (isWeird) {
                    System.out.println("⚠️ 发现异常消费！金额: " + data.getAmount() + ", 均值: " + mean);
                }
            } else {
                data.setIsAnomaly(0); // 样本不足默认正常
            }
        } catch (Exception e) {
            e.printStackTrace();
            data.setIsAnomaly(0); // 算法出错兜底为正常
        }
        // ---------------------------------------

        invoiceRepository.save(data);
        return "success";
    }

    /**
     * 获取当前用户的发票列表
     * 按ID倒序排列
     *
     * @return 发票列表
     */
    // 3. 获取列表 (Read) - 只查自己的数据
    @GetMapping("/list")
    public List<InvoiceData> getList() {
        // A. 获取当前登录用户
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return List.of(); // 未登录返回空列表
        }

        // B. 调用 Repository 新写的方法，只查这个人的
        return invoiceRepository.findByUserIdOrderByIdDesc(currentUser.getId());
    }

    /**
     * 删除指定发票记录
     * 只有记录属于当前用户时才允许删除
     *
     * @param id 发票ID
     * @return 操作结果
     */
    // 4. 删除 (Delete) - 安全校验
    @DeleteMapping("/delete/{id}")
    public String deleteDoc(@PathVariable Long id) {
        User currentUser = getCurrentUser();

        // 查一下这条数据是不是存在的
        InvoiceData data = invoiceRepository.findById(id).orElse(null);

        // 只有数据存在，且属于当前用户，才允许删除
        if (data != null && data.getUserId().equals(currentUser.getId())) {
            invoiceRepository.deleteById(id);
            return "success";
        } else {
            return "fail: permission denied"; // 没权限删别人的
        }
    }

    /**
     * 辅助方法：从 Header 的 Token 中获取当前用户对象
     */
    private User getCurrentUser() {
        String token = request.getHeader("Authorization");
        if (token != null && UserController.tokenMap.containsKey(token)) {
            return UserController.tokenMap.get(token);
        }
        return null; // Token 无效或未登录
    }

    /**
     * 导出用户发票数据为 Excel 文件
     *
     * @param response HTTP 响应对象，用于写入文件流
     * @param token    用户认证令牌
     */
    // 新增：导出 Excel 接口
    @GetMapping("/export")
    public void export(HttpServletResponse response, @RequestHeader("Authorization") String token) {
        try {
            User user = UserController.tokenMap.get(token);
            if (user == null)
                return;

            // 1. 查询该用户所有数据
            List<InvoiceData> list = invoiceRepository.findByUserIdOrderByIdDesc(user.getId());

            // 2. 使用 Hutool 创建 Excel Writer
            ExcelWriter writer = ExcelUtil.getWriter(true);

            // 3. 自定义标题别名 (否则导出的表头是英文列名)
            writer.addHeaderAlias("id", "编号");
            writer.addHeaderAlias("merchantName", "商户名称");
            writer.addHeaderAlias("itemName", "项目名称");
            writer.addHeaderAlias("amount", "金额");
            writer.addHeaderAlias("date", "开票日期");
            writer.addHeaderAlias("category", "分类");
            writer.addHeaderAlias("invoiceCode", "发票号码");
            writer.addHeaderAlias("createTime", "创建时间");

            // 默认只导出这些列，忽略 userId 等内部字段
            writer.setOnlyAlias(true);

            // 4. 写出数据
            writer.write(list, true);

            // 5. 设置浏览器响应格式 (弹出下载框)
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            String fileName = URLEncoder.encode("发票归档报表", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

            // 6. 写出流
            ServletOutputStream out = response.getOutputStream();
            writer.flush(out, true);
            writer.close();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}