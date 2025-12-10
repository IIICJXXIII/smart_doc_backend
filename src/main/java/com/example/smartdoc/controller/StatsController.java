package com.example.smartdoc.controller;

import com.example.smartdoc.model.InvoiceData;
import com.example.smartdoc.model.User;
import com.example.smartdoc.repository.InvoiceRepository;
import com.example.smartdoc.utils.LinearRegressionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "*")
public class StatsController {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @GetMapping("/trend")
    public Map<String, Object> getTrendPrediction(@RequestHeader("Authorization") String token) {
        User user = UserController.tokenMap.get(token);
        if (user == null) return Map.of("code", 401);

        // 1. 获取数据库真实数据
        List<Object[]> rawData = invoiceRepository.findMonthlyStatsByUserId(user.getId());

        List<String> months = new ArrayList<>();
        List<Double> amounts = new ArrayList<>();

        for (Object[] row : rawData) {
            months.add(row[0].toString()); // 月份
            amounts.add(Double.parseDouble(row[1].toString())); // 金额
        }

        // 2. 调用算法进行预测
        Double nextMonthPrediction = 0.0;
        String nextMonthLabel = "下月预测";

        if (!amounts.isEmpty()) {
            nextMonthPrediction = LinearRegressionUtil.predictNext(amounts);
            // 简单计算下个月份的字符串 (这里简化处理，直接叫"预测值")
        }

        // 3. 封装返回结果
        Map<String, Object> data = new HashMap<>();
        data.put("months", months);         // x轴: ["2025-01", "2025-02"...]
        data.put("amounts", amounts);       // y轴: [1000, 1200...]
        data.put("prediction", nextMonthPrediction); // 预测下个月的值

        return Map.of("code", 200, "data", data);
    }

    // 新增：获取知识图谱数据
    @GetMapping("/graph")
    public Map<String, Object> getKnowledgeGraph(@RequestHeader("Authorization") String token) {
        User user = UserController.tokenMap.get(token);
        if (user == null) return Map.of("code", 401);

        // 1. 查出该用户所有数据
        List<InvoiceData> list = invoiceRepository.findByUserIdOrderByIdDesc(user.getId());

        // 2. 构建图谱结构
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();

        // 辅助集合去重
        List<String> addedCategories = new ArrayList<>();
        List<String> addedMerchants = new ArrayList<>();

        // A. 添加根节点 (用户自己)
        Map<String, Object> rootNode = new HashMap<>();
        rootNode.put("id", "ROOT");
        rootNode.put("name", user.getNickname());
        rootNode.put("symbolSize", 60); // 根节点最大
        rootNode.put("category", 0);    // 颜色分组索引
        nodes.add(rootNode);

        // B. 遍历数据构建 分类节点 和 商户节点
        int categoryIndex = 1; // 颜色分组

        // 统计每个分类的总金额，用于决定节点大小
        Map<String, Double> categoryAmountMap = new HashMap<>();
        // 统计每个商户的总金额
        Map<String, Double> merchantAmountMap = new HashMap<>();

        for (InvoiceData item : list) {
            categoryAmountMap.merge(item.getCategory(), item.getAmount(), Double::sum);
            merchantAmountMap.merge(item.getMerchantName(), item.getAmount(), Double::sum);
        }

        // C. 生成节点和连线
        for (InvoiceData item : list) {
            String cat = item.getCategory();
            String merch = item.getMerchantName();

            // 1. 处理分类节点 (Level 1)
            if (!addedCategories.contains(cat)) {
                Map<String, Object> catNode = new HashMap<>();
                catNode.put("id", "CAT_" + cat);
                catNode.put("name", cat);
                // 节点大小跟金额挂钩 (基础大小20 + 金额缩放)
                double size = 20 + Math.log(categoryAmountMap.get(cat) + 1) * 5;
                catNode.put("symbolSize", Math.min(size, 50));
                catNode.put("category", 1);
                nodes.add(catNode);
                addedCategories.add(cat);

                // 连线：用户 -> 分类
                Map<String, Object> link = new HashMap<>();
                link.put("source", "ROOT");
                link.put("target", "CAT_" + cat);
                links.add(link);
            }

            // 2. 处理商户节点 (Level 2)
            if (!addedMerchants.contains(merch)) {
                Map<String, Object> merchNode = new HashMap<>();
                merchNode.put("id", "MER_" + merch);
                merchNode.put("name", merch);
                double size = 10 + Math.log(merchantAmountMap.get(merch) + 1) * 3;
                merchNode.put("symbolSize", Math.min(size, 30));
                merchNode.put("category", 2);
                nodes.add(merchNode);
                addedMerchants.add(merch);

                // 连线：分类 -> 商户
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
}