package com.example.smartdoc.service;

import cn.hutool.http.HttpRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * DeepSeek AI 服务 - 提供大语言模型调用能力
 * 
 * <p>封装 DeepSeek API 的调用逻辑，为系统提供 AI 对话能力。
 * 主要用于智能问答和 Text2SQL 功能，将自然语言转换为 SQL 查询。</p>
 * 
 * <h3>调用参数说明:</h3>
 * <ul>
 *   <li>model: deepseek-chat（对话模型）</li>
 *   <li>temperature: 0.3（较低值保证分析任务的准确性）</li>
 *   <li>timeout: 60秒（防止长时间等待）</li>
 * </ul>
 * 
 * <h3>配置项:</h3>
 * <pre>
 * deepseek.api.key=sk-xxx
 * deepseek.api.url=https://api.deepseek.com/v1/chat/completions
 * </pre>
 * 
 * @author SmartDoc Team
 * @see com.example.smartdoc.controller.ChatServer
 */
@Service
public class DeepSeekService {

    /** DeepSeek API 密钥 (从配置文件注入) */
    @Value("${deepseek.api.key}")
    private String apiKey;

    /** DeepSeek API 地址 */
    @Value("${deepseek.api.url}")
    private String apiUrl;

    /**
     * 调用 DeepSeek AI 接口
     * 
     * <p>发送系统提示词和用户消息，获取 AI 回复。
     * 使用 Hutool 的 HttpRequest 简化 HTTP 调用。</p>
     * 
     * @param systemPrompt 系统提示词，定义 AI 的角色和行为约束
     * @param userMessage  用户输入的消息内容
     * @return AI 生成的回复内容，异常时返回错误提示
     */
    public String callAi(String systemPrompt, String userMessage) {
        try {
            // 构建请求体
            JSONObject body = new JSONObject();
            body.put("model", "deepseek-chat");
            body.put("temperature", 0.3); // 分析类任务温度低一点，更理性

            // 构建消息数组 (system + user)
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
            messages.put(new JSONObject().put("role", "user").put("content", userMessage));
            body.put("messages", messages);

            // 发送 POST 请求
            String response = HttpRequest.post(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(60000) // 60秒超时
                    .execute()
                    .body();

            // 解析响应
            JSONObject jsonResponse = new JSONObject(response);
            if (jsonResponse.has("error")) {
                return "AI 服务响应错误: " + jsonResponse.getJSONObject("error").getString("message");
            }

            // 提取 AI 回复内容
            return jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

        } catch (Exception e) {
            e.printStackTrace();
            return "分析服务暂时不可用";
        }
    }
}
