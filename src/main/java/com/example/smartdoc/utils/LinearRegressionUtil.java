package com.example.smartdoc.utils;

import java.util.List;

/**
 * 最小二乘法 - 线性回归工具类
 * 用于基于历史数据预测下一个时间点的值 (例如下月支出)
 */
public class LinearRegressionUtil {

    /**
     * 预测下一个点的值 (Next Value Prediction)
     * 假设数据是按时间均匀分布的 (x=1,2,3...)
     *
     * @param data 历史数据列表 (例如: [100.0, 120.0, 110.0, ...])
     * @return 预测的下一个值 y (x=n+1)
     */
    public static Double predictNext(List<Double> data) {
        int n = data.size();
        if (n < 2)
            return 0.0; // 数据太少无法预测

        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumXX = 0;

        // 我们把月份看作 x 轴 (1, 2, 3...), 金额看作 y 轴
        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = data.get(i);

            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }

        // 计算斜率 (Slope) a
        double a = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);

        // 计算截距 (Intercept) b
        double b = (sumY - a * sumX) / n;

        // 预测下一个点 (x = n + 1)
        double nextX = n + 1;
        double nextY = a * nextX + b;

        return nextY > 0 ? nextY : 0; // 金额不能为负
    }
}