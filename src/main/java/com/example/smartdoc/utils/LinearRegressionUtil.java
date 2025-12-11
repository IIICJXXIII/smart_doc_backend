package com.example.smartdoc.utils;

import java.util.List;

/**
 * 线性回归工具类 - 基于最小二乘法的趋势预测
 * 
 * <p>使用简单线性回归模型 y = ax + b 进行趋势预测，
 * 通过历史消费数据预测下个月的消费金额。</p>
 * 
 * <h3>最小二乘法原理:</h3>
 * <pre>
 * 目标: 找到一条直线 y = ax + b，使得所有点到直线的垂直距离之和最小
 * 
 * 斜率 a = (nΣxy - ΣxΣy) / (nΣx² - (Σx)²)
 * 截距 b = (Σy - aΣx) / n
 * 
 * 预测: y_next = a * (n+1) + b
 * </pre>
 * 
 * <h3>应用场景:</h3>
 * <p>根据用户过去几个月的消费数据，预测下个月的消费趋势，
 * 帮助用户进行预算规划。</p>
 * 
 * @author SmartDoc Team
 * @see com.example.smartdoc.controller.StatsController#getTrend
 */
public class LinearRegressionUtil {

    /**
     * 预测下一个月的消费金额
     * 
     * <p>将月份作为 X 轴 (1, 2, 3...)，金额作为 Y 轴，
     * 拟合直线后预测 X = n+1 时的 Y 值。</p>
     * 
     * @param data 历史月度消费数据列表 (按时间顺序)
     *             例如: [100.0, 120.0, 110.0, 130.0]
     * @return 预测的下个月消费金额，数据不足时返回 0.0
     */
    public static Double predictNext(List<Double> data) {
        int n = data.size();
        if (n < 2) return 0.0; // 数据太少无法预测

        // 初始化累加变量
        double sumX = 0;   // Σx
        double sumY = 0;   // Σy
        double sumXY = 0;  // Σxy
        double sumXX = 0;  // Σx²

        // 把月份看作 x 轴 (1, 2, 3...), 金额看作 y 轴
        for (int i = 0; i < n; i++) {
            double x = i + 1;      // 月份序号从 1 开始
            double y = data.get(i); // 该月消费金额

            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }

        // 计算斜率 (Slope) a
        // a = (nΣxy - ΣxΣy) / (nΣx² - (Σx)²)
        double a = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);

        // 计算截距 (Intercept) b
        // b = (Σy - aΣx) / n
        double b = (sumY - a * sumX) / n;

        // 预测下一个月 (x = n + 1) 的消费金额
        double nextX = n + 1;
        double nextY = a * nextX + b;

        // 金额不能为负
        return nextY > 0 ? nextY : 0;
    }
}