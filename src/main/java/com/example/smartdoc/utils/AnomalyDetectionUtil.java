package com.example.smartdoc.utils;

import java.util.List;

/**
 * 异常检测工具类 - 基于 Z-Score 算法的异常值检测
 * 
 * <p>使用统计学方法识别异常高或异常低的发票金额。
 * 核心思想：如果一个值偏离平均值超过一定程度，则认为是异常。</p>
 * 
 * <h3>Z-Score 算法原理:</h3>
 * <pre>
 * Z = (X - μ) / σ
 * 其中:
 *   X = 待检测的值
 *   μ = 样本均值 (Mean)
 *   σ = 样本标准差 (Standard Deviation)
 * 
 * |Z| > 2 → 异常 (约 5% 的极端值)
 * |Z| > 3 → 极端异常 (约 0.3% 的极端值)
 * </pre>
 * 
 * <h3>应用场景:</h3>
 * <p>在发票保存时，将新发票金额与历史同类别金额对比，
 * 自动标记异常高或低的发票，提醒用户关注。</p>
 * 
 * @author SmartDoc Team
 * @see com.example.smartdoc.controller.DocController#saveInvoice
 */
public class AnomalyDetectionUtil {

    /**
     * 计算均值 (Mean)
     * <p>均值是所有数据的算术平均，代表数据的"中心位置"。</p>
     * 
     * @param data 数值列表
     * @return 均值，空列表返回 0.0
     */
    public static double calculateMean(List<Double> data) {
        if (data == null || data.isEmpty()) return 0.0;
        double sum = 0.0;
        for (Double num : data) {
            sum += num;
        }
        return sum / data.size();
    }

    /**
     * 计算标准差 (Standard Deviation)
     * <p>标准差衡量数据的离散程度，值越大说明数据越分散。</p>
     * 
     * <h4>计算公式:</h4>
     * <pre>
     * σ = √[Σ(Xi - μ)² / (n-1)]
     * </pre>
     * <p>注意：这里使用样本标准差 (n-1)，而非总体标准差 (n)。</p>
     * 
     * @param data 数值列表
     * @param mean 预先计算的均值
     * @return 标准差，数据不足返回 0.0
     */
    public static double calculateStdDev(List<Double> data, double mean) {
        if (data == null || data.size() < 2) return 0.0;
        double temp = 0;
        for (Double a : data) {
            temp += (a - mean) * (a - mean);
        }
        return Math.sqrt(temp / (data.size() - 1));
    }

    /**
     * 判断是否为异常值 (Z-Score 算法)
     * 
     * <p>规则：如果一个数值距离平均值超过 2 倍标准差 (2-sigma)，
     * 则视为异常。阈值 2.0 约对应正态分布中最极端的 5% 数据。</p>
     * 
     * @param value  待检测的数值
     * @param mean   样本均值
     * @param stdDev 样本标准差
     * @return true=异常, false=正常
     */
    public static boolean isAnomaly(double value, double mean, double stdDev) {
        // 标准差为 0 表示所有值相同，无法判断波动
        if (stdDev == 0) return false;

        // Z-Score = |X - μ| / σ
        double zScore = Math.abs((value - mean) / stdDev);

        // 阈值设为 2.0 (约前5%的极端数据)
        // 也可设为 3.0 (约0.3%的极端数据)
        return zScore > 2.0;
    }
}
