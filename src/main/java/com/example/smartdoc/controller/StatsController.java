package com.example.smartdoc.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartdoc.model.InvoiceData;
import com.example.smartdoc.model.User;
import com.example.smartdoc.repository.InvoiceRepository;
import com.example.smartdoc.service.DeepSeekService;
import com.example.smartdoc.utils.KMeansUtil;
import com.example.smartdoc.utils.LinearRegressionUtil;

/**
 * 统计分析控制器 - 提供消费数据的智能分析和可视化
 * 
 * <p>该控制器集成了多种数据分析算法，为用户提供消费趋势预测、
 * 知识图谱可视化、K-Means 聚类分析等高级功能。</p>
 * 
 * <h3>核心功能:</h3>
 * <ul>
 *   <li>消费趋势预测: 使用线性回归算法预测下月消费</li>
 *   <li>知识图谱: 构建用户-分类-商户的关联图谱</li>
 *   <li>K-Means 聚类: 无监督学习分析消费模式</li>
 *   <li>AI 聚类解读: 调用大模型生成分析报告</li>
 * </ul>
 * 
 * <h3>算法说明:</h3>
 * <pre>
 * 1. 线性回归 (Trend Prediction)
 *    - 输入: 近12个月的消费金额
 *    - 输出: 下个月的预测金额
 *    - 原理: y = ax + b (最小二乘法)
 * 
 * 2. K-Means 聚类 (Clustering)
 *    - 输入: (消费日期, 消费金额) 二维数据点
 *    - 输出: K=3 个聚类 (日常消费/固定支出/突发消费)
 *    - 原理: 迭代优化聚类中心
 * 
 * 3. 知识图谱 (Knowledge Graph)
 *    - 节点: 用户、分类、商户
 *    - 边: 用户→分类→商户
 *    - 可视化: 供 ECharts 关系图使用
 * </pre>
 * 
 * <h3>API 接口:</h3>
 * <ul>
 *   <li>GET /api/stats/trend - 消费趋势及预测</li>
 *   <li>GET /api/stats/graph - 知识图谱数据</li>
 *   <li>GET /api/stats/clustering - K-Means 聚类结果</li>
 *   <li>GET /api/stats/analyze-clustering - AI 聚类分析报告</li>
 * </ul>
 * 
 * @author SmartDoc Team
 * @see LinearRegressionUtil
 * @see KMeansUtil
 */
@RestController  // RESTful 控制器
@RequestMapping("/api/stats")  // URL 前缀
@CrossOrigin(origins = "*")  // 允许跨域
public class StatsController {

    /** 票据数据仓库 */
    @Autowired
    private InvoiceRepository invoiceRepository;

    /** DeepSeek AI 服务 - 用于生成聚类分析报告 */
    @Autowired
    private DeepSeekService deepSeekService;

    /**
     * 获取消费趋势预测数据
     * 
     * <p>该接口返回用户近12个月的消费统计数据，并使用线性回归算法
     * 预测下个月的消费金额。结果可用于前端绑定折线图展示。</p>
     * 
     * <h4>算法原理:</h4>
     * <pre>
     * 1. 获取近12个月的月度消费总额
     * 2. 将月份映射为 x 轴 (1, 2, 3, ..., 12)
     * 3. 使用最小二乘法计算回归方程 y = ax + b
     * 4. 预测 x = 13 时的 y 值
     * </pre>
     * 
     * <h4>返回数据结构:</h4>
     * <pre>
     * {
     *   "code": 200,
     *   "data": {
     *     "months": ["2025-01", "2025-02", ...],  // 月份列表
     *     "amounts": [1500.0, 2300.0, ...],       // 消费金额列表
     *     "prediction": 2150.50,                  // 预测值
     *     "nextMonthLabel": "2026-01 (预测)"     // 预测月份标签
     *   }
     * }
     * </pre>
     * 
     * @param token 用户登录凭证
     * @return 趋势数据及预测结果
     */
    @GetMapping("/trend")
    public Map<String, Object> getTrendPrediction(@RequestHeader("Authorization") String token) {
        // 1. 身份验证
        User user = UserController.tokenMap.get(token);
        if (user == null) {
            return Map.of("code", 401);
        }

        // 2. 获取月度统计数据
        // 注意: SQL 返回的是按月倒序的数据 (最新的在前)
        List<Object[]> rawData = invoiceRepository.findMonthlyStatsByUserId(user.getId());

        // 3. 关键步骤: 反转数据使其变为正序 (2025-01 → 2025-12)
        // 线性回归需要时间正序的数据
        Collections.reverse(rawData);

        // 4. 提取月份和金额到独立列表
        List<String> months = new ArrayList<>();
        List<Double> amounts = new ArrayList<>();

        for (Object[] row : rawData) {
            months.add(row[0].toString());  // 月份字符串
            amounts.add(Double.parseDouble(row[1].toString()));  // 金额
        }

        // 5. 使用线性回归预测下月消费
        Double nextMonthPrediction = 0.0;
        String nextMonthLabel = "下月预测";

        if (!amounts.isEmpty()) {
            // 调用线性回归工具类
            nextMonthPrediction = LinearRegressionUtil.predictNext(amounts);

            // 自动计算下个月的具体日期字符串
            try {
                String lastMonthStr = months.get(months.size() - 1);
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
                YearMonth lastMonth = YearMonth.parse(lastMonthStr, fmt);
                YearMonth nextMonth = lastMonth.plusMonths(1);
                nextMonthLabel = nextMonth.format(fmt) + " (预测)";
            } catch (Exception e) {
                nextMonthLabel = "下月预测";
            }
        }

        // 6. 封装返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("months", months);           // 月份列表 (X轴)
        data.put("amounts", amounts);         // 消费金额 (Y轴)
        data.put("prediction", nextMonthPrediction);  // 预测值
        data.put("nextMonthLabel", nextMonthLabel);   // 预测月份标签

        return Map.of("code", 200, "data", data);
    }

    /**
     * 获取知识图谱数据
     * 
     * <p>该接口构建用户消费的知识图谱，展示用户、消费分类、商户之间的关联关系。
     * 返回的数据结构适配 ECharts 的关系图组件。</p>
     * 
     * <h4>图谱结构:</h4>
     * <pre>
     *          [用户]
     *         /  |  \
     *    [餐饮] [交通] [办公]
     *      |      |      |
     *   [商户A] [商户B] [商户C]
     * </pre>
     * 
     * <h4>节点分类:</h4>
     * <ul>
     *   <li>category=0: 根节点 (用户)</li>
     *   <li>category=1: 消费分类节点</li>
     *   <li>category=2: 商户节点</li>
     * </ul>
     * 
     * <h4>节点大小:</h4>
     * <p>节点大小与该分类/商户的消费金额成对数关系</p>
     * 
     * @param token 用户登录凭证
     * @return 知识图谱数据 (nodes + links)
     */
    @GetMapping("/graph")
    public Map<String, Object> getKnowledgeGraph(@RequestHeader("Authorization") String token) {
        // 1. 身份验证
        User user = UserController.tokenMap.get(token);
        if (user == null) {
            return Map.of("code", 401);
        }

        // 2. 获取该用户的所有票据
        List<InvoiceData> list = invoiceRepository.findByUserIdOrderByIdDesc(user.getId());

        // 3. 初始化图谱数据结构
        List<Map<String, Object>> nodes = new ArrayList<>();  // 节点列表
        List<Map<String, Object>> links = new ArrayList<>();  // 边列表
        List<String> addedCategories = new ArrayList<>();     // 已添加的分类 (去重)
        List<String> addedMerchants = new ArrayList<>();      // 已添加的商户 (去重)

        // 4. 创建根节点 (用户)
        Map<String, Object> rootNode = new HashMap<>();
        rootNode.put("id", "ROOT");
        rootNode.put("name", user.getNickname());
        rootNode.put("symbolSize", 60);  // 根节点最大
        rootNode.put("category", 0);     // 类别 0 = 用户
        nodes.add(rootNode);

        // 5. 统计各分类和商户的总消费金额 (用于计算节点大小)
        Map<String, Double> categoryAmountMap = new HashMap<>();
        Map<String, Double> merchantAmountMap = new HashMap<>();

        for (InvoiceData item : list) {
            // 累加分类金额
            categoryAmountMap.merge(item.getCategory(), item.getAmount(), Double::sum);
            // 累加商户金额
            merchantAmountMap.merge(item.getMerchantName(), item.getAmount(), Double::sum);
        }

        // 6. 遍历票据构建图谱
        for (InvoiceData item : list) {
            String cat = item.getCategory();
            String merch = item.getMerchantName();

            // 6.1 添加分类节点 (去重)
            if (!addedCategories.contains(cat)) {
                Map<String, Object> catNode = new HashMap<>();
                catNode.put("id", "CAT_" + cat);
                catNode.put("name", cat);
                // 节点大小: 基础值 + 对数缩放 (避免金额差异过大导致显示问题)
                double size = 20 + Math.log(categoryAmountMap.get(cat) + 1) * 5;
                catNode.put("symbolSize", Math.min(size, 50));  // 最大50
                catNode.put("category", 1);  // 类别 1 = 分类
                nodes.add(catNode);
                addedCategories.add(cat);

                // 创建边: 用户 → 分类
                Map<String, Object> link = new HashMap<>();
                link.put("source", "ROOT");
                link.put("target", "CAT_" + cat);
                links.add(link);
            }

            // 6.2 添加商户节点 (去重)
            if (!addedMerchants.contains(merch)) {
                Map<String, Object> merchNode = new HashMap<>();
                merchNode.put("id", "MER_" + merch);
                merchNode.put("name", merch);
                double size = 10 + Math.log(merchantAmountMap.get(merch) + 1) * 3;
                merchNode.put("symbolSize", Math.min(size, 30));  // 最大30
                merchNode.put("category", 2);  // 类别 2 = 商户
                nodes.add(merchNode);
                addedMerchants.add(merch);

                // 创建边: 分类 → 商户
                Map<String, Object> link = new HashMap<>();
                link.put("source", "CAT_" + cat);
                link.put("target", "MER_" + merch);
                links.add(link);
            }
        }

        // 7. 封装返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("links", links);

        return Map.of("code", 200, "data", result);
    }

    /**
     * 获取 K-Means 聚类分析结果
     * 
     * <p>该接口使用 K-Means 算法对用户的消费数据进行聚类分析，
     * 将消费记录分为 K=3 个群体。返回的数据适合绑定 ECharts 散点图。</p>
     * 
     * <h4>特征维度:</h4>
     * <ul>
     *   <li>X 轴: 消费日期 (1-31号)</li>
     *   <li>Y 轴: 消费金额</li>
     * </ul>
     * 
     * <h4>聚类解读示例:</h4>
     * <ul>
     *   <li>群体1 (月初高额): 可能是房租/固定支出</li>
     *   <li>群体2 (全月低额): 日常餐饮/交通</li>
     *   <li>群体3 (月末中额): 突发性采购</li>
     * </ul>
     * 
     * @param token 用户登录凭证
     * @return 聚类结果 (数据点 + 聚类中心)
     */
    @GetMapping("/clustering")
    public Map<String, Object> getClustering(@RequestHeader("Authorization") String token) {
        // 1. 身份验证
        User user = UserController.tokenMap.get(token);
        if (user == null) {
            return Map.of("code", 401);
        }

        // 2. 获取该用户的所有票据
        List<InvoiceData> list = invoiceRepository.findByUserIdOrderByIdDesc(user.getId());
        
        // 3. 构建二维数据点 (日期, 金额)
        List<KMeansUtil.Point> points = new ArrayList<>();

        for (InvoiceData item : list) {
            try {
                // 提取消费日期中的"几号"作为 X 坐标
                int day = LocalDate.parse(item.getDate()).getDayOfMonth();
                // 金额作为 Y 坐标，初始聚类标记为 -1
                points.add(new KMeansUtil.Point(day, item.getAmount(), -1));
            } catch (Exception e) {
                // 日期解析失败，跳过该数据点
            }
        }

        // 4. 执行 K-Means 聚类 (K=3, 最大迭代100次)
        KMeansUtil.ClusterResult result = KMeansUtil.fit(points, 3, 100);
        
        return Map.of("code", 200, "data", result);
    }

    /**
     * AI 聚类分析报告
     * 
     * <p>该接口首先执行 K-Means 聚类，然后将聚类中心数据发送给 DeepSeek AI，
     * 由 AI 生成通俗易懂的消费习惯分析报告和理财建议。</p>
     * 
     * <h4>AI 分析内容:</h4>
     * <ul>
     *   <li>识别各群体可能代表的消费类型</li>
     *   <li>分析用户的消费习惯特征</li>
     *   <li>提供针对性的理财建议</li>
     * </ul>
     * 
     * @param token 用户登录凭证
     * @return AI 生成的分析报告 (Markdown 格式)
     */
    @GetMapping("/analyze-clustering")
    public Map<String, Object> analyzeClustering(@RequestHeader("Authorization") String token) {
        // 1. 身份验证
        User user = UserController.tokenMap.get(token);
        if (user == null) {
            return Map.of("code", 401);
        }

        // 2. 重新计算聚类以获取中心点
        List<InvoiceData> list = invoiceRepository.findByUserIdOrderByIdDesc(user.getId());
        List<KMeansUtil.Point> points = new ArrayList<>();
        
        for (InvoiceData item : list) {
            try {
                int day = LocalDate.parse(item.getDate()).getDayOfMonth();
                points.add(new KMeansUtil.Point(day, item.getAmount(), -1));
            } catch (Exception e) {
                // 跳过解析失败的数据
            }
        }

        // 3. 数据量检查
        if (points.size() < 3) {
            return Map.of("code", 200, "data", "数据量不足，暂无法生成分析报告。");
        }

        // 4. 执行聚类
        KMeansUtil.ClusterResult result = KMeansUtil.fit(points, 3, 50);

        // 5. 构建 AI Prompt
        StringBuilder dataDesc = new StringBuilder();
        List<KMeansUtil.Point> centers = result.getCentroids();

        // 将聚类中心转换为自然语言描述
        for (int i = 0; i < centers.size(); i++) {
            KMeansUtil.Point p = centers.get(i);
            dataDesc.append(String.format(
                "- 群体%d特征: 平均发生在每月 %d 号左右，平均金额约 %.2f 元。\n",
                i + 1, (int)p.getX(), p.getY()
            ));
        }

        // 6. 调用 DeepSeek AI 生成分析报告
        String systemPrompt = "你是一个专业的财务数据分析师。请根据用户的消费聚类中心数据，用通俗易懂的语言分析用户的消费习惯。";
        String userPrompt = String.format("""
            我的消费数据被 K-Means 算法聚类为以下 3 类：
            %s
            
            请帮我分析：
            1. 哪一类可能是日常餐饮/交通？
            2. 哪一类可能是房租/房贷或固定大额支出？
            3. 哪一类可能是突发性消费？
            4. 给出一句简短的理财建议。
            
            请直接给出分析结果，不要啰嗦，使用 Markdown 格式。
            """, dataDesc.toString());

        // 7. 获取 AI 回复
        String analysis = deepSeekService.callAi(systemPrompt, userPrompt);

        return Map.of("code", 200, "data", analysis);
    }
}