package com.example.smartdoc.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 手写 K-Means 聚类算法 (无监督学习)
 * 用于将消费数据按【日期-金额】特征聚类，发现消费习惯
 */
public class KMeansUtil {

    /** 数据点内部类 */
    @Data
    @AllArgsConstructor
    public static class Point {
        double x; // 维度1: 日期 (几号)
        double y; // 维度2: 金额
        int clusterIndex = -1; // 所属聚类索引
    }

    /** 聚类结果内部类 */
    @Data
    @AllArgsConstructor // 🟢 修复点 1：生成带参构造函数
    @NoArgsConstructor // 🟢 修复点 2：生成无参构造函数
    public static class ClusterResult {
        List<Point> points; // 原数据点(带分类标记)
        List<Point> centroids; // 聚类中心点
    }

    /**
     * 执行 K-Means 聚类
     *
     * @param rawData       原始数据点列表
     * @param k             聚类数量 (例如 3 类)
     * @param maxIterations 最大迭代次数
     * @return 聚类结果对象
     */
    public static ClusterResult fit(List<Point> rawData, int k, int maxIterations) {
        // 这里调用带参构造函数，需要 @AllArgsConstructor
        if (rawData.size() < k)
            return new ClusterResult(rawData, new ArrayList<>());

        // 1. 随机初始化 K 个中心点
        List<Point> centroids = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < k; i++) {
            Point randomPoint = rawData.get(random.nextInt(rawData.size()));
            centroids.add(new Point(randomPoint.x, randomPoint.y, i));
        }

        boolean changed = true;
        int iter = 0;

        while (changed && iter < maxIterations) {
            changed = false;
            iter++;

            // 2. E步：分配每个点到最近的中心
            for (Point p : rawData) {
                int nearestIndex = -1;
                double minDist = Double.MAX_VALUE;

                for (int i = 0; i < centroids.size(); i++) {
                    double dist = calculateDistance(p, centroids.get(i));
                    if (dist < minDist) {
                        minDist = dist;
                        nearestIndex = i;
                    }
                }

                if (p.clusterIndex != nearestIndex) {
                    p.clusterIndex = nearestIndex;
                    changed = true;
                }
            }

            // 3. M步：重新计算中心点
            for (int i = 0; i < k; i++) {
                double sumX = 0, sumY = 0;
                int count = 0;
                for (Point p : rawData) {
                    if (p.clusterIndex == i) {
                        sumX += p.x;
                        sumY += p.y;
                        count++;
                    }
                }
                if (count > 0) {
                    centroids.get(i).x = sumX / count;
                    centroids.get(i).y = sumY / count;
                }
            }
        }

        // 这里调用无参构造函数，需要 @NoArgsConstructor
        ClusterResult result = new ClusterResult();
        result.points = rawData;
        result.centroids = centroids;
        return result;
    }

    /** 计算两个点之间的欧几里得距离 */
    private static double calculateDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }
}