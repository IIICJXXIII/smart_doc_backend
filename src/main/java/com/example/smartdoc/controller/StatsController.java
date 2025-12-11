package com.example.smartdoc.controller;

import com.example.smartdoc.model.InvoiceData;
import com.example.smartdoc.model.User;
import com.example.smartdoc.repository.InvoiceRepository;
import com.example.smartdoc.utils.LinearRegressionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.example.smartdoc.service.DeepSeekService;
import com.example.smartdoc.utils.KMeansUtil;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计分析控制器
 * 提供数据可视化、趋势预测、知识图谱和聚类分析等高级功能
 */
@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "*")
public class StatsController {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private DeepSeekService deepSeekService;

    /**
     * 获取消费趋势预测
     * 基于最近12个月的数据，使用线性回归算法预测下个月的支出
     *
     * @param token 用户认证令牌
     * @return 包含月度数据和预测结果的 Map
     */
    // 1. 获取趋势预测 (修复了年份排序问题)
    @GetMapping("/trend")
    public Map<String, Object> getTrendPrediction(@RequestHeader("Authorization") String token) {
        User user = UserController.tokenMap.get(token);
        if (user == null)
            return Map.of("code", 401);

        // 1. 获取数据 (现在取到的是最新的12条，但是是倒序的: 2025-12, 2025-11...)
        // 注意：这里需要在 InvoiceRepository 中把 SQL 改为 ORDER BY month DESC
        List<Object[]> rawData = invoiceRepository.findMonthlyStatsByUserId(user.getId());

        // 2. 🔥 关键步骤：把数据反转回正序 (变成 2025-01 ... 2025-12)
        Collections.reverse(rawData);

        List<String> months = new ArrayList<>();
        List<Double> amounts = new ArrayList<>();

        for (Object[] row : rawData) {
            months.add(row[0].toString());
            amounts.add(Double.parseDouble(row[1].toString()));
        }

        // 3. 预测下个月
        Double nextMonthPrediction = 0.0;
        String nextMonthLabel = "下月预测";

        if (!amounts.isEmpty()) {
            nextMonthPrediction = LinearRegressionUtil.predictNext(amounts);

            // 自动计算下个月的具体日期字符串 (例如 "2026-01")
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

        // 4. 封装返回结果
        Map<String, Object> data = new HashMap<>();
        data.put("months", months);
        data.put("amounts", amounts);
        data.put("prediction", nextMonthPrediction);
        data.put("nextMonthLabel", nextMonthLabel);

        return Map.of("code", 200, "data", data);
    }

    /**
     * 获取知识图谱数据
     * 构建 用户 -> 分类 -> 商户 的关系图谱，用于 ECharts 关系图展示
     *
     * @param token 用户认证令牌
     * @return 节点(nodes)和边(links)的数据
     */
    // 2. 获取知识图谱数据
    @GetMapping("/graph")
    public Map<String, Object> getKnowledgeGraph(@RequestHeader("Authorization") String token) {
        User user = UserController.tokenMap.get(token);
        if (user == null)
            return Map.of("code", 401);

        List<InvoiceData> list = invoiceRepository.findByUserIdOrderByIdDesc(user.getId());

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();
        List<String> addedCategories = new ArrayList<>();
        List<String> addedMerchants = new ArrayList<>();

        // A. 根节点
        Map<String, Object> rootNode = new HashMap<>();
        rootNode.put("id", "ROOT");
        rootNode.put("name", user.getNickname());
        rootNode.put("symbolSize", 60);
        rootNode.put("category", 0);
        nodes.add(rootNode);

        Map<String, Double> categoryAmountMap = new HashMap<>();
        Map<String, Double> merchantAmountMap = new HashMap<>();

        for (InvoiceData item : list) {
            categoryAmountMap.merge(item.getCategory(), item.getAmount(), Double::sum);
            merchantAmountMap.merge(item.getMerchantName(), item.getAmount(), Double::sum);
        }

        for (InvoiceData item : list) {
            String cat = item.getCategory();
            String merch = item.getMerchantName();

            // 分类节点
            if (!addedCategories.contains(cat)) {
                Map<String, Object> catNode = new HashMap<>();
                catNode.put("id", "CAT_" + cat);
                catNode.put("name", cat);
                double size = 20 + Math.log(categoryAmountMap.get(cat) + 1) * 5;
                catNode.put("symbolSize", Math.min(size, 50));
                catNode.put("category", 1);
                nodes.add(catNode);
                addedCategories.add(cat);

                Map<String, Object> link = new HashMap<>();
                link.put("source", "ROOT");
                link.put("target", "CAT_" + cat);
                links.add(link);
            }

            // 商户节点
            if (!addedMerchants.contains(merch)) {
                Map<String, Object> merchNode = new HashMap<>();
                merchNode.put("id", "MER_" + merch);
                merchNode.put("name", merch);
                double size = 10 + Math.log(merchantAmountMap.get(merch) + 1) * 3;
                merchNode.put("symbolSize", Math.min(size, 30));
                merchNode.put("category", 2);
                nodes.add(merchNode);
                addedMerchants.add(merch);

                Map<String, Object> link = new HashMap<>();
                link.put("source", "CAT_" + cat);
                link.put("target", "MER_" + merch);
                links.add(link);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("links", links);

        return Map.of("code", 200, "data", result);
    }

    /**
     * 获取消费聚类分析
     * 使用 K-Means 算法对消费时间(日)和金额进行聚类，识别消费习惯
     *
     * @param token 用户认证令牌
     * @return 聚类结果（包含中心点和各点所属类别）
     */
    // 3. K-Means 聚类分析
    @GetMapping("/clustering")
    public Map<String, Object> getClustering(@RequestHeader("Authorization") String token) {
        User user = UserController.tokenMap.get(token);
        if (user == null)
            return Map.of("code", 401);

        List<InvoiceData> list = invoiceRepository.findByUserIdOrderByIdDesc(user.getId());
        List<KMeansUtil.Point> points = new ArrayList<>();

        for (InvoiceData item : list) {
            try {
                int day = LocalDate.parse(item.getDate()).getDayOfMonth();
                points.add(new KMeansUtil.Point(day, item.getAmount(), -1));
            } catch (Exception e) {
            }
        }

        KMeansUtil.ClusterResult result = KMeansUtil.fit(points, 3, 100);
        return Map.of("code", 200, "data", result);
    }

    /**
     * AI 智能分析聚类结果
     * 将聚类中心数据发送给 AI，让其生成通俗易懂的理财建议报告
     *
     * @param token 用户认证令牌
     * @return AI 生成的分析文本
     */
    // 4. AI 对聚类结果的分析报告
    @GetMapping("/analyze-clustering")
    public Map<String, Object> analyzeClustering(@RequestHeader("Authorization") String token) {
        User user = UserController.tokenMap.get(token);
        if (user == null)
            return Map.of("code", 401);

        // A. 重新计算聚类以获取中心点
        List<InvoiceData> list = invoiceRepository.findByUserIdOrderByIdDesc(user.getId());
        List<KMeansUtil.Point> points = new ArrayList<>();
        for (InvoiceData item : list) {
            try {
                int day = LocalDate.parse(item.getDate()).getDayOfMonth();
                points.add(new KMeansUtil.Point(day, item.getAmount(), -1));
            } catch (Exception e) {
            }
        }

        if (points.size() < 3) {
            return Map.of("code", 200, "data", "数据量不足，暂无法生成分析报告。");
        }

        KMeansUtil.ClusterResult result = KMeansUtil.fit(points, 3, 50);

        // B. 构建 Prompt
        StringBuilder dataDesc = new StringBuilder();
        List<KMeansUtil.Point> centers = result.getCentroids();

        for (int i = 0; i < centers.size(); i++) {
            KMeansUtil.Point p = centers.get(i);
            dataDesc.append(String.format("- 群体%d特征: 平均发生在每月 %d 号左右，平均金额约 %.2f 元。\n",
                    i + 1, (int) p.getX(), p.getY()));
        }

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

        // C. 调用 AI
        String analysis = deepSeekService.callAi(systemPrompt, userPrompt);

        return Map.of("code", 200, "data", analysis);
    }
}