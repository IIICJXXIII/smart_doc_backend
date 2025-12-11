package com.example.smartdoc.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.smartdoc.model.InvoiceData;
import com.example.smartdoc.model.User;
import com.example.smartdoc.repository.InvoiceRepository;
import com.example.smartdoc.service.OcrService;
import com.example.smartdoc.utils.AnomalyDetectionUtil;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 票据文档控制器 - 处理票据的上传识别、存储管理和导出功能
 * 
 * <p>该控制器是整个票据管理系统的核心，负责处理票据的完整生命周期：
 * 从上传识别、智能分类、异常检测到归档存储和导出。</p>
 * 
 * <h3>核心功能:</h3>
 * <ul>
 *   <li>票据上传识别: 支持 JPG/PNG 图片和 PDF 电子发票</li>
 *   <li>智能 OCR: 调用百度 AI 识别商户、金额、日期等信息</li>
 *   <li>自动分类: 根据识别内容智能归类 (餐饮、交通、办公等)</li>
 *   <li>异常检测: 使用 Z-Score 算法标记异常大额消费</li>
 *   <li>数据管理: 增删改查、软删除、Excel 导出</li>
 * </ul>
 * 
 * <h3>处理流程:</h3>
 * <pre>
 * 用户上传文件 → OcrService 识别 → 返回识别结果给前端
 *                                        ↓ (用户确认后)
 *                              调用 save 接口保存
 *                                        ↓
 *                              触发异常检测算法
 *                                        ↓
 *                              持久化到数据库
 * </pre>
 * 
 * <h3>API 接口:</h3>
 * <ul>
 *   <li>POST /api/doc/upload - 上传并识别票据</li>
 *   <li>POST /api/doc/save - 保存票据到数据库</li>
 *   <li>GET /api/doc/list - 获取票据列表</li>
 *   <li>DELETE /api/doc/delete/{id} - 删除票据 (软删除)</li>
 *   <li>GET /api/doc/export - 导出 Excel 报表</li>
 * </ul>
 * 
 * @author SmartDoc Team
 * @see OcrService
 * @see AnomalyDetectionUtil
 */
@RestController  // RESTful 控制器，返回 JSON 数据
@RequestMapping("/api/doc")  // URL 前缀
@CrossOrigin(origins = "*")  // 允许跨域
public class DocController {

    /** OCR 识别服务 - 调用百度 AI 进行票据识别 */
    @Autowired
    private OcrService ocrService;

    /** 票据数据仓库 - 用于操作 invoice_record 表 */
    @Autowired
    private InvoiceRepository invoiceRepository;

    /** HTTP 请求对象 - 用于获取请求头中的 Token */
    @Autowired
    private HttpServletRequest request;

    /**
     * 上传并识别票据
     * 
     * <p>该接口接收用户上传的票据文件（图片或 PDF），调用 OCR 服务进行识别，
     * 返回识别结果供前端展示和确认。</p>
     * 
     * <h4>支持的文件格式:</h4>
     * <ul>
     *   <li>图片: JPG, PNG</li>
     *   <li>电子发票: PDF (会自动转换为图片后识别)</li>
     * </ul>
     * 
     * <h4>识别内容:</h4>
     * <ul>
     *   <li>商户名称 (merchantName)</li>
     *   <li>项目名称 (itemName)</li>
     *   <li>金额 (amount)</li>
     *   <li>开票日期 (date)</li>
     *   <li>发票号码 (invoiceCode)</li>
     *   <li>自动分类 (category)</li>
     * </ul>
     * 
     * @param file 用户上传的票据文件
     * @return 识别后的票据数据对象，识别失败返回 null
     */
    @PostMapping("/upload")
    public InvoiceData uploadAndAnalyze(@RequestParam("file") MultipartFile file) {
        try {
            // 调用 OCR 服务处理文档
            // OcrService 会自动判断文件类型并调用相应的识别接口
            return ocrService.processDocument(file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;  // 识别失败返回 null
        }
    }

    /**
     * 保存票据到数据库 (带异常检测)
     * 
     * <p>该接口将用户确认后的票据数据持久化到数据库。
     * 保存前会触发 Z-Score 异常检测算法，自动标记可疑的大额消费。</p>
     * 
     * <h4>异常检测逻辑:</h4>
     * <ol>
     *   <li>获取该用户同分类下的所有历史消费金额</li>
     *   <li>计算历史数据的均值 (μ) 和标准差 (σ)</li>
     *   <li>计算当前金额的 Z-Score = |当前值 - μ| / σ</li>
     *   <li>如果 Z-Score > 2.0，标记为异常 (约前 5% 极端值)</li>
     * </ol>
     * 
     * @param data 待保存的票据数据 (从请求体 JSON 解析)
     * @return "success" 表示保存成功，"error: not login" 表示未登录
     */
    @PostMapping("/save")
    public String saveDoc(@RequestBody InvoiceData data) {
        // 1. 获取当前登录用户
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "error: not login";
        }

        // 2. 绑定用户 ID (数据隔离的关键)
        data.setUserId(currentUser.getId());

        // ========== 3. 异常检测算法核心逻辑 ==========
        try {
            // 3.1 获取该用户、该分类下的所有历史消费记录
            // 只与同类别的数据比较，如餐饮只跟餐饮比
            List<InvoiceData> historyList = invoiceRepository.findByUserIdAndCategoryOrderByIdDesc(
                    currentUser.getId(),
                    data.getCategory()
            );

            // 3.2 提取历史金额列表作为训练数据
            List<Double> historyAmounts = historyList.stream()
                    .map(InvoiceData::getAmount)
                    .toList();  // JDK 16+ 写法

            // 3.3 样本量检查: 至少需要 5 条历史数据才能进行统计分析
            if (historyAmounts.size() >= 5) {
                // 计算均值 (Mean)
                double mean = AnomalyDetectionUtil.calculateMean(historyAmounts);
                
                // 计算标准差 (Standard Deviation)
                double stdDev = AnomalyDetectionUtil.calculateStdDev(historyAmounts, mean);

                // 3.4 使用 Z-Score 算法判定是否异常
                boolean isWeird = AnomalyDetectionUtil.isAnomaly(data.getAmount(), mean, stdDev);

                // 3.5 打标: 0=正常, 1=异常
                data.setIsAnomaly(isWeird ? 1 : 0);

                // 输出日志方便调试
                if (isWeird) {
                    System.out.println("⚠️ 发现异常消费！金额: " + data.getAmount() + ", 均值: " + mean);
                }
            } else {
                // 样本不足，默认标记为正常
                data.setIsAnomaly(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            data.setIsAnomaly(0);  // 算法出错时兜底为正常
        }
        // ============================================

        // 4. 保存到数据库
        invoiceRepository.save(data);
        return "success";
    }

    /**
     * 获取当前用户的票据列表
     * 
     * <p>该接口只返回当前登录用户的票据数据，实现多租户数据隔离。
     * 结果按 ID 倒序排列，最新的记录在前面。</p>
     * 
     * <h4>数据过滤:</h4>
     * <p>由于 InvoiceData 实体上有 @Where(clause = "is_deleted = 0") 注解，
     * 已删除的记录（在回收站中）会被自动过滤掉。</p>
     * 
     * @return 票据列表，未登录时返回空列表
     */
    @GetMapping("/list")
    public List<InvoiceData> getList() {
        // 1. 获取当前登录用户
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return List.of();  // 未登录返回空列表
        }

        // 2. 查询该用户的所有票据 (自动排除软删除的记录)
        return invoiceRepository.findByUserIdOrderByIdDesc(currentUser.getId());
    }

    /**
     * 删除票据 (软删除)
     * 
     * <p>该接口将票据标记为已删除状态，实际数据会保留在回收站中。
     * 用户可以通过回收站接口恢复误删的数据。</p>
     * 
     * <h4>软删除实现:</h4>
     * <p>InvoiceData 实体上的 @SQLDelete 注解会将 DELETE 操作
     * 转换为 UPDATE is_deleted = 1</p>
     * 
     * <h4>权限校验:</h4>
     * <p>只允许删除属于自己的票据，防止越权操作</p>
     * 
     * @param id 票据主键 ID
     * @return "success" 或 "fail: permission denied"
     */
    @DeleteMapping("/delete/{id}")
    public String deleteDoc(@PathVariable Long id) {
        // 1. 获取当前用户
        User currentUser = getCurrentUser();

        // 2. 查询目标票据
        InvoiceData data = invoiceRepository.findById(id).orElse(null);

        // 3. 权限校验: 票据必须存在且属于当前用户
        if (data != null && data.getUserId().equals(currentUser.getId())) {
            // 执行删除 (由于 @SQLDelete 注解，实际是软删除)
            invoiceRepository.deleteById(id);
            return "success";
        } else {
            return "fail: permission denied";  // 没权限删别人的
        }
    }

    /**
     * 辅助方法: 从请求头的 Token 中获取当前用户对象
     * 
     * <p>从 HTTP 请求的 Authorization 头中提取 Token，
     * 然后从 tokenMap 中查找对应的用户信息。</p>
     * 
     * @return 当前登录的用户对象，未登录或 Token 无效返回 null
     */
    private User getCurrentUser() {
        String token = request.getHeader("Authorization");
        if (token != null && UserController.tokenMap.containsKey(token)) {
            return UserController.tokenMap.get(token);
        }
        return null;
    }

    /**
     * 导出票据为 Excel 文件
     * 
     * <p>该接口将当前用户的所有票据数据导出为 .xlsx 格式的 Excel 文件，
     * 方便用户离线查看和存档。使用 Hutool 的 ExcelUtil 实现导出功能。</p>
     * 
     * <h4>导出列:</h4>
     * <ul>
     *   <li>编号 (id)</li>
     *   <li>商户名称 (merchantName)</li>
     *   <li>项目名称 (itemName)</li>
     *   <li>金额 (amount)</li>
     *   <li>开票日期 (date)</li>
     *   <li>分类 (category)</li>
     *   <li>发票号码 (invoiceCode)</li>
     *   <li>创建时间 (createTime)</li>
     * </ul>
     * 
     * @param response HTTP 响应对象，用于输出文件流
     * @param token    用户登录凭证
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response, @RequestHeader("Authorization") String token) {
        try {
            // 1. 身份验证
            User user = UserController.tokenMap.get(token);
            if (user == null) return;

            // 2. 查询该用户的所有票据数据
            List<InvoiceData> list = invoiceRepository.findByUserIdOrderByIdDesc(user.getId());

            // 3. 使用 Hutool 创建 Excel Writer
            // 参数 true 表示创建 xlsx 格式 (Excel 2007+)
            ExcelWriter writer = ExcelUtil.getWriter(true);

            // 4. 配置表头别名 (将英文字段名映射为中文)
            writer.addHeaderAlias("id", "编号");
            writer.addHeaderAlias("merchantName", "商户名称");
            writer.addHeaderAlias("itemName", "项目名称");
            writer.addHeaderAlias("amount", "金额");
            writer.addHeaderAlias("date", "开票日期");
            writer.addHeaderAlias("category", "分类");
            writer.addHeaderAlias("invoiceCode", "发票号码");
            writer.addHeaderAlias("createTime", "创建时间");

            // 5. 只导出配置了别名的列，忽略 userId 等内部字段
            writer.setOnlyAlias(true);

            // 6. 写入数据 (第二个参数 true 表示写入表头)
            writer.write(list, true);

            // 7. 设置响应头，告诉浏览器这是一个下载文件
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            String fileName = URLEncoder.encode("发票归档报表", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

            // 8. 将 Excel 写入响应输出流
            ServletOutputStream out = response.getOutputStream();
            writer.flush(out, true);  // flush 并关闭 writer
            writer.close();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}