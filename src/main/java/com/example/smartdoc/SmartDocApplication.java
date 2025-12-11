package com.example.smartdoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SmartDoc 智能票据归档助手 - Spring Boot 应用程序入口类
 * 
 * <p>该类是整个应用程序的启动入口点，使用 Spring Boot 框架进行自动配置和启动。</p>
 * 
 * <h3>核心功能概述:</h3>
 * <ul>
 *   <li>票据识别: 集成百度 OCR 实现多种票据类型的智能识别</li>
 *   <li>AI 助手: 集成 DeepSeek 大模型实现自然语言财务查询</li>
 *   <li>数据分析: 提供消费趋势预测、K-Means 聚类分析、异常检测等功能</li>
 *   <li>用户管理: 支持多用户注册登录、权限隔离、管理员审批</li>
 * </ul>
 * 
 * @author SmartDoc Team
 * @version 1.0.0
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 */
@SpringBootApplication  // 组合注解，包含 @Configuration, @EnableAutoConfiguration, @ComponentScan
public class SmartDocApplication {

	/**
	 * 应用程序主入口方法
	 * 
	 * <p>该方法负责启动 Spring Boot 应用程序，主要完成以下工作:</p>
	 * <ol>
	 *   <li>创建 Spring 应用上下文 (ApplicationContext)</li>
	 *   <li>自动扫描并加载所有组件 (Controller, Service, Repository 等)</li>
	 *   <li>根据 application.properties 完成自动配置</li>
	 *   <li>启动内嵌的 Tomcat 服务器 (默认端口 8080)</li>
	 * </ol>
	 * 
	 * @param args 命令行参数，可用于覆盖默认配置
	 *             例如: --server.port=9090 可修改启动端口
	 */
	public static void main(String[] args) {
		// 使用 SpringApplication.run() 启动应用
		// 第一个参数: 主配置类 (标注了 @SpringBootApplication 的类)
		// 第二个参数: 命令行参数数组
		SpringApplication.run(SmartDocApplication.class, args);
	}

}
