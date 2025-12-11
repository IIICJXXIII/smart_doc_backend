package com.example.smartdoc.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * K-Means 聚类算法工具类 - 无监督学习消费模式分析
 * 
 * <p>使用 K-Means 算法将发票数据聚类为 K 个群组，
 * 帮助用户发现消费模式（如日常小额、中等消费、大额支出）。</p>
 * 
 * <h3>算法流程:</h3>
 * <pre>
 * 1. 随机初始化 K 个聚类中心
 * 2. E步 (Expectation): 将每个数据点分配到最近的中心
 * 3. M步 (Maximization): 重新计算每个聚类的中心点
 * 4. 重复步骤 2-3 直到收敛或达到最大迭代次数
 * </pre>
 * 
 * <h3>数据维度:</h3>
 * <ul>
 *   <li>X轴: 消费日期 (月中的第几天, 1-31)</li>
 *   <li>Y轴: 消费金额</li>
 * </ul>
 * 
 * <h3>聚类含义 (K=3 时的典型解释):</h3>
 * <ul>
 *   <li>聚类1: 日常小额消费 (餐饮、交通)</li>
 *   <li>聚类2: 中等金额消费 (购物、娱乐)</li>
 *   <li>聚类3: 大额支出 (设备采购、差旅)</li>
 * </ul>
 * 
 * @author SmartDoc Team
 * @see com.example.smartdoc.controller.StatsController#getClusters
 */
public class KMeansUtil {

    /**
     * 数据点类 - 表示二维平面上的一个点
     */
    @Data
    @AllArgsConstructor
    public static class Point {
        /** X坐标: 日期 (几号) */
        double x;
        /** Y坐标: 金额 */
        double y;
        /** 所属聚类索引 (-1 表示未分配) */
        int clusterIndex = -1;
    }

    /**
     * 聚类结果类 - 包含分类后的数据点和聚类中心
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ClusterResult {
        /** 原始数据点 (带分类标记) */
        List<Point> points;
        /** 各聚类的中心点 */
        List<Point> centroids;
    }

    /**
     * 执行 K-Means 聚类
     * 
     * @param rawData       原始数据点列表
     * @param k             聚类数量 (推荐 3)
     * @param maxIterations 最大迭代次数 (推荐 50-100)
     * @return 聚类结果，包含分类后的点和中心点
     */
    public static ClusterResult fit(List<Point> rawData, int k, int maxIterations) {
        // 数据点数量不足时直接返回
        if (rawData.size() < k) return new ClusterResult(rawData, new ArrayList<>());

        // 1. 随机初始化 K 个中心点 (从数据中随机选取)
        List<Point> centroids = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < k; i++) {
            Point randomPoint = rawData.get(random.nextInt(rawData.size()));
            centroids.add(new Point(randomPoint.x, randomPoint.y, i));
        }

        boolean changed = true;  // 是否有点改变了所属聚类
        int iter = 0;            // 当前迭代次数

        // 迭代直到收敛或达到最大次数
        while (changed && iter < maxIterations) {
            changed = false;
            iter++;

            // 2. E步 (Expectation): 分配每个点到最近的中心
            for (Point p : rawData) {
                int nearestIndex = -1;
                double minDist = Double.MAX_VALUE;

                // 计算到每个中心的距离，找最近的
                for (int i = 0; i < centroids.size(); i++) {
                    double dist = calculateDistance(p, centroids.get(i));
                    if (dist < minDist) {
                        minDist = dist;
                        nearestIndex = i;
                    }
                }

                // 如果分配发生变化，标记为需要继续迭代
                if (p.clusterIndex != nearestIndex) {
                    p.clusterIndex = nearestIndex;
                    changed = true;
                }
            }

            // 3. M步 (Maximization): 重新计算每个聚类的中心点
            for (int i = 0; i < k; i++) {
                double sumX = 0, sumY = 0;
                int count = 0;
                
                // 累加该聚类内所有点的坐标
                for (Point p : rawData) {
                    if (p.clusterIndex == i) {
                        sumX += p.x;
                        sumY += p.y;
                        count++;
                    }
                }
                
                // 更新中心点为该聚类所有点的平均位置
                if (count > 0) {
                    centroids.get(i).x = sumX / count;
                    centroids.get(i).y = sumY / count;
                }
            }
        }

        // 构建并返回结果
        ClusterResult result = new ClusterResult();
        result.points = rawData;
        result.centroids = centroids;
        return result;
    }

    /**
     * 计算两点间的欧几里得距离
     * <p>公式: d = √[(x1-x2)² + (y1-y2)²]</p>
     */
    private static double calculateDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }
}