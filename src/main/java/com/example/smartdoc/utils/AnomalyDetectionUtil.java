package com.example.smartdoc.utils;

import com.example.smartdoc.model.InvoiceData;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AnomalyDetectionUtil {

    private static MultilayerPerceptron mlp;
    private static ArrayList<Attribute> attributes;

    // 静态代码块，用于定义Weka实例的“表头”结构
    static {
        // 1. 定义特征
        // 特征1: 金额 (Numeric)
        Attribute amount = new Attribute("amount");

        // 特征2: 类别 (Nominal)
        List<String> categories = Arrays.asList("餐饮美食", "交通出行", "办公耗材", "通讯网络", "电子设备", "其他");
        Attribute category = new Attribute("category", categories);

        // 2. 定义要预测的目标（类标签）
        List<String> classValues = Arrays.asList("false", "true"); // 必须是字符串
        Attribute isAnomaly = new Attribute("is_anomaly", classValues);

        // 3. 组装成一个完整的“表头”
        attributes = new ArrayList<>();
        attributes.add(amount);
        attributes.add(category);
        attributes.add(isAnomaly);
    }

    /**
     * 训练神经网络模型
     * @param allInvoices 所有的历史票据数据
     */
    private static void trainModel(List<InvoiceData> allInvoices) throws Exception {
        // 1. 创建一个空的Weka数据集
        Instances trainingData = new Instances("InvoiceAnomalies", attributes, allInvoices.size());
        trainingData.setClassIndex(attributes.size() - 1); // 设置最后一列为预测目标

        // 2. 将我们的Java对象列表 (allInvoices) 转换成Weka的数据格式
        for (InvoiceData invoice : allInvoices) {
            DenseInstance instance = new DenseInstance(3);
            instance.setValue(attributes.get(0), invoice.getAmount());
            instance.setValue(attributes.get(1), invoice.getCategory());
            // [修复] 将 0/1 转换为模型定义的 "false"/"true"
            String anomalyLabel = invoice.getIsAnomaly() == 1 ? "true" : "false";
            instance.setValue(attributes.get(2), anomalyLabel); // 目标值
            trainingData.add(instance);
        }

        // 3. 初始化并训练神经网络
        mlp = new MultilayerPerceptron();
        mlp.setHiddenLayers("a"); // 'a' = (attributes + classes) / 2
        mlp.setTrainingTime(500); // 训练500轮
        mlp.buildClassifier(trainingData);
    }

    /**
     * [核心方法] 使用训练好的模型预测新票据是否异常
     * @param newInvoice  需要检查的新票据
     * @param allInvoices 用于训练模型的历史数据
     * @return 是否为异常
     */
    public static boolean isAnomaly(InvoiceData newInvoice, List<InvoiceData> allInvoices) {
        // 安全校验：如果历史数据太少，无法训练，则退回旧的简单逻辑
        if (allInvoices == null || allInvoices.size() < 10) {
            return newInvoice.getAmount() > 10000 || (newInvoice.getItemName() != null && newInvoice.getItemName().contains("测试"));
        }

        try {
            // 1. 训练模型 (每次都用最新数据重新训练)
            trainModel(allInvoices);

            // 2. 创建一个Weka实例来代表这张新票据
            Instances testSet = new Instances("TestInstance", attributes, 1);
            testSet.setClassIndex(attributes.size() - 1);

            DenseInstance instanceToTest = new DenseInstance(3);
            instanceToTest.setValue(attributes.get(0), newInvoice.getAmount());
            instanceToTest.setValue(attributes.get(1), newInvoice.getCategory());
            // 目标值设为缺失，因为这是我们要预测的
            instanceToTest.setMissing(attributes.get(2));
            testSet.add(instanceToTest);

            // 3. 使用模型进行预测
            double predictionIndex = mlp.classifyInstance(testSet.firstInstance()); // 返回预测值的索引 (0.0 for 'false', 1.0 for 'true')

            System.out.println("🤖 [Weka NN] Prediction for new invoice: " + testSet.classAttribute().value((int) predictionIndex));

            // 4. 返回预测结果
            return predictionIndex == 1.0;

        } catch (Exception e) {
            System.err.println("❌ Weka model prediction failed: " + e.getMessage());
            e.printStackTrace();
            // 如果机器学习模型失败，同样回退到简单规则，保证系统可用性
            return newInvoice.getAmount() > 10000 || (newInvoice.getItemName() != null && newInvoice.getItemName().contains("测试"));
        }
    }
}
