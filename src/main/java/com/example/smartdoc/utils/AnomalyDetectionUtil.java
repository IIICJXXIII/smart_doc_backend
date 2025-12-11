package com.example.smartdoc.utils;

import java.util.List;

/**
 * 异常检测工具类
 * 基于统计学方法 (均值和标准差) 检测数据异常
 */
public class AnomalyDetectionUtil {

    /**
     * 计算均值 (Mean)
     *
     * @param data 数据列表
     * @return 平均值
     */
    public static double calculateMean(List<Double> data) {
        if (data == null || data.isEmpty())
            return 0.0;
        double sum = 0.0;
        for (Double num : data) {
            sum += num;
        }
        return sum / data.size();
    }

    /**
     * 计算标准差 (Standard Deviation)
     * 衡量数据的波动程度，标准差越大表示数据越分散
     *
     * @param data 数据列表
     * @param mean 平均值
     * @return 标准差
     */
    public static double calculateStdDev(List<Double> data, double mean) {
        if (data == null || data.size() < 2)
            return 0.0;
        double temp = 0;
        for (Double a : data) {
            temp += (a - mean) * (a - mean);
        }
        return Math.sqrt(temp / (data.size() - 1));
    }

    /**
     * 判断是否异常 (Z-Score 算法)
     * 规则：如果一个数值距离平均值超过 2 倍标准差 (2-sigma)，则视为异常
     *
     * @param value  待检测的值
     * @param mean   平均值
     * @param stdDev 标准差
     * @return true=异常, false=正常
     */
    public static boolean isAnomaly(double value, double mean, double stdDev) {
        if (stdDev == 0)
            return false; // 无法判断波动

        // Z-Score = (X - μ) / σ
        double zScore = Math.abs((value - mean) / stdDev);

        // 阈值设为 2.0 (约前5%的极端数据)，也可设为 3.0 (极度稀有)
        return zScore > 2.0;
    }
}
