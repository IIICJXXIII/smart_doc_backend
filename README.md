# 🧾 SmartDoc - 智能票据归档助手

## 📦 相关仓库

| 仓库 | 地址 |
|-----|------|
| 🖥️ **前端仓库** | https://github.com/IIICJXXIII/smart_doc_frontend |
| ⚙️ **后端仓库** | https://github.com/IIICJXXIII/smart_doc_backend |

---

## 📖 项目简介

SmartDoc 是一个基于 **AI 视觉技术** 的智能票据管理系统。旨在解决传统财务报销中手工录入繁琐、发票整理困难的问题。系统支持 **JPG/PNG 图片及 PDF 电子发票** 的原件上传，利用 **百度 OCR + DeepSeek AI** 双引擎技术自动提取商户、金额、日期、发票号码及具体购买项目，并实现 **自动分类、智能分析与云端归档**。

> 💡 **适用场景**: 软件工程课程设计 / 毕业设计 / AI 应用实战参考项目

---

## ✨ 核心功能 (Features)

### 📄 票据管理
- 🤖 **多模态 AI 识别**: 集成百度智能云 OCR 高精度接口，支持增值税发票、出租车票、火车票、飞机票、定额发票等多种票据类型
- 📄 **PDF 电子发票支持**: 后端内置 PDF 渲染引擎 (Apache PDFBox)，可直接解析 PDF 格式的电子发票
- 🧠 **智能语义提取**: 自动从 OCR 结果中提取"发票号码"、"项目名称"、"价税合计"等关键字段
- 🏷️ **自动智能分类**: 根据识别到的商品关键词自动归类（餐饮美食、交通出行、办公耗材、通讯网络、电子设备等）
- 📊 **Excel 导出**: 一键导出票据数据为 Excel 表格

### 🤖 AI 智能分析
- 💬 **AI 财务助手 (WebSocket)**: 集成 DeepSeek 大模型，支持自然语言查询票据数据（如"这个月花了多少钱"）
- 🧮 **Text2SQL 能力**: AI 自动将用户问题转换为 SQL 查询，实现智能数据检索
- 📈 **消费趋势预测**: 基于线性回归算法，预测下月消费金额
- 🔬 **K-Means 聚类分析**: 无监督学习分析消费习惯，识别日常消费、固定支出、突发消费等模式
- 🎯 **知识图谱可视化**: 构建用户-分类-商户的关联图谱，直观展示消费结构
- ⚠️ **异常消费检测**: 基于 Z-Score 算法，自动标记异常大额消费

### 💰 预算管理
- 📊 **分类预算设置**: 为不同消费类别设置预算上限
- 📉 **预算进度监控**: 实时计算各分类已使用金额与进度百分比
- 🔔 **预算预警**: 支出接近或超过预算时提醒

### 🔐 用户与权限
- 👤 **用户注册/登录**: 基于 Token 的身份认证机制
- 🔒 **多用户数据隔离**: 每个用户只能管理自己的财务数据
- 👨‍💼 **管理员审批流程**: 支持票据的提交审核、管理员审批通过/驳回
- 📝 **操作审计日志**: 记录所有敏感操作，支持追溯

### 🗑️ 数据安全
- ♻️ **回收站机制**: 软删除票据，支持误删恢复
- 💾 **数据备份/恢复**: 一键导出全量数据为 JSON，支持数据迁移与恢复
- 🧹 **彻底删除**: 支持清空回收站，物理删除数据

---

## 🛠️ 技术栈 (Tech Stack)

### 后端 (Backend)

| 技术 | 版本 | 说明 |
|-----|------|-----|
| **开发语言** | Java 17+ | 主开发语言 |
| **核心框架** | Spring Boot 3.5.8 | Web 应用框架 |
| **持久层** | Spring Data JPA (Hibernate) | ORM 框架 |
| **数据库** | MySQL 8.0 | 关系型数据库 |
| **AI - OCR** | Baidu AIP SDK 4.16.19 | 百度智能云 OCR |
| **AI - 大模型** | DeepSeek API | 自然语言对话 & 智能分析 |
| **PDF 处理** | Apache PDFBox 2.0.27 | PDF 转图片处理 |
| **工具库** | Hutool 5.8.16 | 工具类库 (Excel/JSON/HTTP) |
| **实时通信** | WebSocket (Jakarta) | AI 对话实时推送 |
| **简化代码** | Lombok | 注解简化 Java Bean |

### 核心算法实现

| 算法 | 文件 | 说明 |
|-----|------|-----|
| **线性回归** | `LinearRegressionUtil.java` | 最小二乘法预测消费趋势 |
| **K-Means 聚类** | `KMeansUtil.java` | 无监督学习分析消费模式 |
| **Z-Score 异常检测** | `AnomalyDetectionUtil.java` | 统计学方法识别异常消费 |

---

## 🚀 快速开始 (Getting Started)

### 1. 环境准备

确保本地已安装以下环境:

- ✅ **JDK 17** 或更高版本
- ✅ **Maven 3.6+** (项目管理工具)
- ✅ **MySQL 8.0+** (数据库)

### 2. 数据库配置

#### 2.1 创建数据库

在 MySQL 中执行:

```sql
CREATE DATABASE IF NOT EXISTS smartdoc CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

#### 2.2 初始化表结构

项目根目录提供了数据库初始化脚本:

```bash
# 1. 创建表结构 (包含所有表)
mysql -u root -p smartdoc < DDL.sql

# 2. 插入测试数据 (可选，包含全年12个月的模拟数据)
mysql -u root -p smartdoc < DML.sql
```

#### 2.3 数据库表说明

| 表名 | 说明 |
|-----|------|
| `sys_user` | 系统用户表 (用户名、密码、角色) |
| `invoice_record` | 票据归档表 (核心业务表，含审批状态、异常标记、软删除等) |
| `sys_chat_log` | AI 对话记录表 (支持多会话) |
| `sys_budget` | 预算管理表 (分类预算) |
| `sys_operation_log` | 操作审计日志表 |

### 3. 配置 API Key

编辑 `src/main/resources/application.properties`:

```properties
# ==============================
# 百度 AI 配置 (Baidu OCR)
# ==============================
baidu.ocr.app-id=你的AppID
baidu.ocr.api-key=你的API_Key
baidu.ocr.secret-key=你的Secret_Key

# ==============================
# DeepSeek AI 配置 (大模型对话)
# ==============================
deepseek.api.key=你的DeepSeek_API_Key
deepseek.api.url=https://api.deepseek.com/chat/completions

# ==============================
# MySQL 数据库配置
# ==============================
spring.datasource.url=jdbc:mysql://localhost:3306/smartdoc?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=你的MySQL密码
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# ==============================
# JPA 配置 (自动建表)
# ==============================
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

#### 3.1 获取百度 OCR API Key

1. 访问 [百度智能云控制台](https://console.bce.baidu.com/)
2. 进入 **产品服务 > 文字识别 OCR**
3. 创建应用并获取 `APP_ID`、`API_Key`、`Secret_Key`

#### 3.2 获取 DeepSeek API Key

1. 访问 [DeepSeek 开放平台](https://platform.deepseek.com/)
2. 注册账号并创建 API Key

### 4. 启动后端服务

在项目根目录执行:

```bash
# 方式一: 使用 Maven Wrapper (推荐)
.\mvnw.cmd spring-boot:run

# 方式二: 使用本地 Maven
mvn spring-boot:run

# 方式三: 打包后运行
mvn clean package
java -jar target/SmartDoc-0.0.1-SNAPSHOT.jar
```

启动成功后，后端服务运行在: **http://localhost:8080**

---

## 📂 项目结构 (Project Structure)

```
SmartDoc/
├── src/
│   ├── main/
│   │   ├── java/com/example/smartdoc/
│   │   │   ├── SmartDocApplication.java        # 🚀 启动类
│   │   │   │
│   │   │   ├── controller/                     # 📡 控制层 (API 接口)
│   │   │   │   ├── UserController.java         # 用户登录/注册/会话管理
│   │   │   │   ├── DocController.java          # 票据上传/识别/CRUD/导出
│   │   │   │   ├── StatsController.java        # 统计分析/趋势预测/知识图谱/聚类
│   │   │   │   ├── BudgetController.java       # 预算管理
│   │   │   │   ├── AuditController.java        # 审批流程 (管理员)
│   │   │   │   ├── RecycleBinController.java   # 回收站管理
│   │   │   │   ├── SystemController.java       # 系统功能 (备份/恢复/日志)
│   │   │   │   └── ChatServer.java             # WebSocket AI 对话服务
│   │   │   │
│   │   │   ├── service/                        # 🧠 业务逻辑层
│   │   │   │   ├── OcrService.java             # OCR 识别核心 (多票据类型解析)
│   │   │   │   └── DeepSeekService.java        # DeepSeek AI 调用封装
│   │   │   │
│   │   │   ├── model/                          # 📦 实体类
│   │   │   │   ├── User.java                   # 用户实体
│   │   │   │   ├── InvoiceData.java            # 票据实体 (含审批/异常/软删除字段)
│   │   │   │   ├── Budget.java                 # 预算实体
│   │   │   │   ├── ChatLog.java                # 对话记录实体
│   │   │   │   └── OperationLog.java           # 操作日志实体
│   │   │   │
│   │   │   ├── repository/                     # 💾 数据访问层 (JPA)
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── InvoiceRepository.java
│   │   │   │   ├── BudgetRepository.java
│   │   │   │   ├── ChatLogRepository.java
│   │   │   │   └── OperationLogRepository.java
│   │   │   │
│   │   │   ├── config/                         # ⚙️ 配置类
│   │   │   │   ├── WebConfig.java              # Web 配置 (拦截器注册)
│   │   │   │   ├── LoginInterceptor.java       # 登录拦截器
│   │   │   │   └── WebSocketConfig.java        # WebSocket 配置
│   │   │   │
│   │   │   └── utils/                          # 🔧 工具类 (算法实现)
│   │   │       ├── LinearRegressionUtil.java   # 线性回归 (趋势预测)
│   │   │       ├── KMeansUtil.java             # K-Means 聚类算法
│   │   │       └── AnomalyDetectionUtil.java   # Z-Score 异常检测
│   │   │
│   │   └── resources/
│   │       ├── application.properties           # 配置文件
│   │       ├── static/                          # 静态资源
│   │       └── templates/                       # 模板文件
│   │
│   └── test/                                    # 单元测试
│
├── uploads/                                     # 文件上传目录
├── DDL.sql                                      # 数据库表结构 (完整版)
├── DML.sql                                      # 测试数据脚本
├── modify.sql                                   # 数据库升级脚本 (历史记录)
├── pom.xml                                      # Maven 依赖配置
└── README.md                                    # 项目说明文档
```

---

## 📡 API 接口文档

### 🔐 用户模块 (`/api/user`)

| 接口 | 方法 | 路径 | 说明 | 鉴权 |
|-----|------|------|-----|-----|
| 用户登录 | POST | `/api/user/login` | 返回 token 用于后续鉴权 | ❌ |
| 用户注册 | POST | `/api/user/register` | 创建新用户账号 | ❌ |
| 用户登出 | POST | `/api/user/logout` | 清除登录状态 | ✅ |
| 更新信息 | POST | `/api/user/update` | 修改昵称/密码 | ✅ |
| 获取会话列表 | GET | `/api/user/chat/sessions` | AI 对话会话列表 | ✅ |
| 获取对话历史 | GET | `/api/user/chat/history?sessionId=xxx` | 指定会话的对话记录 | ✅ |

### 📄 票据模块 (`/api/doc`)

| 接口 | 方法 | 路径 | 说明 | 鉴权 |
|-----|------|------|-----|-----|
| 上传识别 | POST | `/api/doc/upload` | 上传发票图片/PDF，返回 OCR 识别结果 | ✅ |
| 保存归档 | POST | `/api/doc/save` | 保存识别结果到数据库 (自动异常检测) | ✅ |
| 查询列表 | GET | `/api/doc/list` | 查询当前用户的所有票据 | ✅ |
| 删除票据 | DELETE | `/api/doc/delete/{id}` | 软删除票据 (进入回收站) | ✅ |
| 导出 Excel | GET | `/api/doc/export` | 导出所有票据为 Excel 文件 | ✅ |

### 📊 统计分析模块 (`/api/stats`)

| 接口 | 方法 | 路径 | 说明 | 鉴权 |
|-----|------|------|-----|-----|
| 消费趋势 | GET | `/api/stats/trend` | 月度消费趋势 + 下月预测值 | ✅ |
| 知识图谱 | GET | `/api/stats/graph` | 用户-分类-商户关联图谱数据 | ✅ |
| 聚类分析 | GET | `/api/stats/clustering` | K-Means 聚类结果 (散点图数据) | ✅ |
| AI 聚类解读 | GET | `/api/stats/analyze-clustering` | AI 分析聚类结果，生成理财建议 | ✅ |

### 💰 预算模块 (`/api/budget`)

| 接口 | 方法 | 路径 | 说明 | 鉴权 |
|-----|------|------|-----|-----|
| 预算列表 | GET | `/api/budget/list` | 获取所有预算及使用进度 | ✅ |
| 设置预算 | POST | `/api/budget/save` | 新增/更新分类预算 | ✅ |
| 删除预算 | DELETE | `/api/budget/delete/{id}` | 删除预算 | ✅ |

### ✅ 审批模块 (`/api/audit`)

| 接口 | 方法 | 路径 | 说明 | 鉴权 |
|-----|------|------|-----|-----|
| 提交审核 | POST | `/api/audit/submit/{id}` | 用户提交票据审核申请 | ✅ 用户 |
| 待审列表 | GET | `/api/audit/pending-list` | 获取所有待审核票据 | ✅ 管理员 |
| 审核通过 | POST | `/api/audit/pass/{id}` | 批准票据 | ✅ 管理员 |
| 审核驳回 | POST | `/api/audit/reject/{id}` | 驳回票据 (附原因) | ✅ 管理员 |

### ♻️ 回收站模块 (`/api/recycle`)

| 接口 | 方法 | 路径 | 说明 | 鉴权 |
|-----|------|------|-----|-----|
| 回收站列表 | GET | `/api/recycle/list` | 查看已删除的票据 | ✅ |
| 还原票据 | POST | `/api/recycle/restore/{id}` | 恢复已删除票据 | ✅ |
| 彻底删除 | DELETE | `/api/recycle/destroy/{id}` | 物理删除，不可恢复 | ✅ |
| 清空回收站 | DELETE | `/api/recycle/clear-all` | 清空所有已删除票据 | ✅ |

### 🔧 系统模块 (`/api/system`)

| 接口 | 方法 | 路径 | 说明 | 鉴权 |
|-----|------|------|-----|-----|
| 操作日志 | GET | `/api/system/logs` | 查看操作审计日志 | ✅ |
| 数据备份 | GET | `/api/system/backup` | 下载全量数据 (JSON) | ✅ |
| 数据恢复 | POST | `/api/system/restore` | 上传 JSON 恢复数据 | ✅ |

### 💬 AI 对话 (WebSocket)

| 类型 | 地址 | 说明 |
|-----|------|-----|
| WebSocket | `ws://localhost:8080/ws/chat/{token}` | AI 财务助手实时对话 |

**消息格式** (发送):
```json
{
  "sessionId": "会话ID",
  "content": "这个月我花了多少钱？"
}
```

**功能特性**:
- 自动识别用户意图，决定是否需要查询数据库
- Text2SQL: 自然语言转 SQL 查询
- 流式返回 AI 回答
- 对话历史持久化存储

> 💡 **鉴权说明**: 所有需要鉴权的接口，在请求头中携带 `Authorization: <token>`

---

## 🔑 核心功能实现详解

### 1. OCR 智能识别流程

```
用户上传文件 → 判断文件类型
                 ├─ PDF → 使用 PDFBox 转换为 JPG
                 └─ JPG/PNG → 直接读取字节流
                              ↓
              调用百度 OCR [智能财务票据识别] API
              (multiple_invoice 接口，支持多种票据)
                              ↓
              根据票据类型分发解析:
              ├─ vat_invoice   → 增值税发票解析
              ├─ taxi_receipt  → 出租车票解析
              ├─ train_ticket  → 火车票解析
              ├─ air_ticket    → 飞机票解析
              └─ quota_invoice → 定额发票解析
                              ↓
              后处理引擎:
              - 日期格式标准化 (YYYY-MM-DD)
              - 智能分类推断 (餐饮/交通/办公...)
                              ↓
              返回结构化 InvoiceData 对象给前端
```

### 2. 异常消费检测 (Z-Score 算法)

```java
// 核心逻辑 (保存票据时自动触发)
// 1. 获取该用户同类别的历史消费金额
List<Double> historyAmounts = ...;

// 2. 计算均值和标准差
double mean = calculateMean(historyAmounts);
double stdDev = calculateStdDev(historyAmounts, mean);

// 3. 计算 Z-Score = |当前值 - 均值| / 标准差
double zScore = Math.abs((newAmount - mean) / stdDev);

// 4. 判定: Z-Score > 2.0 视为异常 (前5%的极端值)
boolean isAnomaly = zScore > 2.0;
```

### 3. 消费趋势预测 (线性回归)

```java
// 最小二乘法计算斜率和截距
// y = ax + b

double a = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);  // 斜率
double b = (sumY - a * sumX) / n;  // 截距

// 预测下个月 (x = n + 1)
double nextMonthPrediction = a * (n + 1) + b;
```

### 4. K-Means 聚类分析

```
输入数据点: (日期, 金额) → 如 (15, 32.00) 表示15号消费32元

算法流程:
1. 随机初始化 K=3 个聚类中心
2. E步: 将每个点分配到最近的中心
3. M步: 重新计算各聚类的中心点
4. 重复 2-3 直到收敛

输出:
- 群体1: 月初高额消费 → 可能是房租/固定支出
- 群体2: 日常小额消费 → 餐饮/交通
- 群体3: 月末突发消费 → 临时采购
```

### 5. AI 对话 Agent (Text2SQL)

```
用户提问: "12月份交通花了多少钱？"
                    ↓
Step 1: AI 生成 SQL
"SELECT SUM(amount) FROM invoice_record 
 WHERE user_id=1 AND category='交通出行' 
 AND date LIKE '2025-12%'"
                    ↓
Step 2: 执行 SQL，获取结果
"523.50"
                    ↓
Step 3: AI 生成自然语言回答
"12月份您的交通出行类消费共计 523.50 元。"
```

---

## 🧪 测试账号

数据库初始化后会自动创建以下测试账号:

| 用户名 | 密码 | 角色 | 权限说明 |
|-------|------|------|---------|
| admin | 123456 | 管理员 | 可审批所有用户的票据 |
| test | 123456 | 普通用户 | 仅管理自己的数据 |

---

## 📝 待办事项 (TODO)

- [x] 基础上传与 OCR 识别 (多票据类型)
- [x] PDF 格式支持
- [x] 用户注册与登录系统
- [x] 数据持久化与多租户隔离
- [x] 票据的增删改查功能
- [x] Excel 导出功能
- [x] AI 财务助手 (WebSocket + DeepSeek)
- [x] Text2SQL 智能查询
- [x] 消费趋势预测 (线性回归)
- [x] K-Means 聚类分析
- [x] 异常消费检测 (Z-Score)
- [x] 知识图谱可视化
- [x] 预算管理功能
- [x] 审批工作流
- [x] 回收站 (软删除)
- [x] 数据备份与恢复
- [x] 操作审计日志
- [ ] 多文件批量上传支持
- [ ] 票据图片云存储 (OSS)
- [ ] 邮件/短信预算预警通知

---

## 🔧 常见问题 (FAQ)

### Q1: 启动时报错 `Access denied for user 'root'@'localhost'`
**A**: 检查 `application.properties` 中的数据库用户名和密码是否正确。

### Q2: OCR 识别失败或返回空结果
**A**: 
1. 确认百度 OCR 的 API Key 配置正确
2. 检查百度云账号是否有剩余调用次数 (免费版每天 500 次)
3. 上传的图片格式是否支持 (JPG/PNG/PDF)
4. 图片是否清晰，票据是否完整

### Q3: PDF 文件无法识别
**A**: 
1. 确保 `pom.xml` 中已引入 `pdfbox` 依赖
2. 检查 PDF 文件是否损坏
3. 某些扫描版 PDF 可能识别效果不佳

### Q4: AI 对话无响应
**A**: 
1. 检查 DeepSeek API Key 是否正确配置
2. 确认网络可以访问 `api.deepseek.com`
3. 查看后端控制台是否有错误日志

### Q5: WebSocket 连接失败
**A**: 
1. 确认 token 有效 (先登录获取 token)
2. 检查 `WebSocketConfig.java` 是否正确配置
3. 浏览器控制台查看具体错误信息

### Q6: 如何支持更多票据类型?
**A**: 编辑 `OcrService.java`，在 `callSmartFinanceOcr()` 方法的 switch 语句中添加新的票据类型解析逻辑。

---

## 🎯 项目亮点

1. **🤖 双 AI 引擎**: 百度 OCR (视觉识别) + DeepSeek (自然语言理解)，实现从图片到智能分析的完整闭环
2. **📊 三大算法实现**: 手写线性回归、K-Means 聚类、Z-Score 异常检测，展示机器学习在财务场景的应用
3. **💬 Text2SQL 能力**: AI 自动将用户问题转换为 SQL，真正的智能助手
4. **🔐 完整权限体系**: 多用户隔离 + 管理员审批 + 操作审计，企业级安全设计
5. **♻️ 数据安全设计**: 软删除 + 回收站 + 备份恢复，数据不会误删丢失
6. **📱 前后端分离**: RESTful API + WebSocket，标准的现代化架构

---

## 🤝 贡献与许可

本项目仅供 **学习与交流** 使用。

**License**: MIT

---

## 📧 联系方式

如有问题或建议，欢迎提交 Issue 或 Pull Request。

**项目作者**: [IIICJXXIII](https://github.com/IIICJXXIII)  
**后端仓库**: https://github.com/IIICJXXIII/smart_doc_backend  
**前端仓库**: https://github.com/IIICJXXIII/smart_doc_frontend

---

**⭐ 如果这个项目对你有帮助，欢迎 Star ⭐**
