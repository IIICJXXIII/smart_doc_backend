# 🧾 SmartDoc - 智能票据归档助手

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.8-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> 基于 Spring Boot 3.5.8 + 百度 OCR 的智能票据管理系统

---

## 📖 项目简介

SmartDoc 是一个基于 **AI 视觉技术** 的智能票据管理系统，旨在解决传统财务报销中手工录入繁琐、发票整理困难的问题。系统支持 **JPG/PNG 图片及 PDF 电子发票** 的原件上传，利用 **百度 OCR 技术** 自动提取商户、金额、日期、发票号码及具体购买项目，并实现 **自动分类与云端归档**。

**适用场景**: 软件工程课程设计 / 毕业设计参考项目 / 企业财务报销系统

---

## ✨ 核心功能

- 🤖 **多模态 AI 识别**: 集成百度智能云 OCR 高精度接口，支持印刷体、手写体的精准识别
- 📄 **PDF 电子发票支持**: 后端内置 PDF 渲染引擎（Apache PDFBox），可直接解析 PDF 格式的电子发票
- 🧠 **智能语义提取**: 基于正则算法的后处理引擎，自动从 OCR 文字中提取"发票号码"、"项目名称"、"价税合计"等关键字段
- 🏷️ **自动智能分类**: 根据识别到的商品关键词（如"餐饮"、"交通"、"办公"），自动归类票据类型
- 🔐 **多用户数据隔离**: 完善的用户鉴权系统（基于 Session/Interceptor 机制），确保每个用户只能管理自己的财务数据
- 💾 **数据持久化存储**: 基于 MySQL + JPA 实现票据数据的增删改查，支持历史记录查询
- 📊 **Excel 报表导出**: 使用 Apache POI 实现财务数据导出功能
- 🔌 **WebSocket 实时通信**: 支持实时消息推送和通信

---

## 🛠️ 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| **Java** | 17 | 主开发语言 |
| **Spring Boot** | 3.5.8 | Web 应用框架 |
| **Spring Data JPA** | 3.5.8 | ORM 框架（Hibernate） |
| **Spring WebSocket** | 3.5.8 | 实时通信 |
| **MySQL Connector** | 8.0+ | 数据库驱动 |
| **Baidu AIP SDK** | 4.16.19 | 百度 AI 开放平台 OCR SDK |
| **Apache PDFBox** | 2.0.27 | PDF 文件处理 |
| **Apache POI** | 5.2.3 | Excel 导出 |
| **Hutool** | 5.8.16 | Java 工具类库 |
| **Lombok** | - | 简化 Java Bean 代码 |

### 前端技术

前端项目独立维护，详见：[https://github.com/IIICJXXIII/smart_doc_frontend](https://github.com/IIICJXXIII/smart_doc_frontend)

---

## 🚀 快速开始

### 1. 环境要求

确保本地已安装以下环境：

- ✅ **JDK 17** 或更高版本
- ✅ **Maven 3.6+**（项目管理工具）
- ✅ **MySQL 8.0+**（数据库）

### 2. 数据库配置

#### 2.1 创建数据库

在 MySQL 中执行：

```sql
CREATE DATABASE IF NOT EXISTS smartdoc CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

#### 2.2 初始化表结构

项目根目录提供了数据库初始化脚本：

```bash
# 1. 创建表结构
mysql -u root -p smartdoc < DDL.sql

# 2. 插入测试数据（可选）
mysql -u root -p smartdoc < DML.sql

# 3. 如果需要支持多用户隔离，执行以下脚本添加 user_id 字段
mysql -u root -p smartdoc < modify.sql
```

**脚本说明**：
- `DDL.sql`: 创建 `invoice_record`（票据表）和 `sys_user`（用户表）
- `DML.sql`: 插入测试数据（包含测试发票记录和测试用户）
- `modify.sql`: 为 `invoice_record` 表添加 `user_id` 字段，支持多租户隔离

### 3. 百度 OCR 配置

#### 3.1 获取 API Key

1. 访问 [百度智能云控制台](https://console.bce.baidu.com/)
2. 进入 **产品服务 > 文字识别 OCR**
3. 创建应用并获取 `APP_ID`、`API_Key`、`Secret_Key`

#### 3.2 修改配置文件

创建或编辑 `src/main/resources/application.properties`：

```properties
# ==============================
# 数据库配置
# ==============================
spring.datasource.url=jdbc:mysql://localhost:3306/smartdoc?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=你的MySQL密码
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# ==============================
# JPA 配置
# ==============================
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect

# ==============================
# 百度 OCR 配置
# ==============================
baidu.ocr.app-id=你的AppID
baidu.ocr.api-key=你的API_Key
baidu.ocr.secret-key=你的Secret_Key

# ==============================
# 服务器配置
# ==============================
server.port=8080

# ==============================
# 文件上传配置
# ==============================
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

### 4. 启动应用

在项目根目录执行：

```bash
# 方式一：使用 Maven Wrapper（推荐，Windows）
.\mvnw.cmd spring-boot:run

# 方式一：使用 Maven Wrapper（Linux/Mac）
./mvnw spring-boot:run

# 方式二：使用本地 Maven
mvn spring-boot:run

# 方式三：打包后运行
mvn clean package
java -jar target/SmartDoc-0.0.1-SNAPSHOT.jar
```

启动成功后，后端服务运行在：**http://localhost:8080**

### 5. 测试 API

使用 **Postman** 或 **curl** 测试接口：

```bash
# 1. 用户登录
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 2. 上传发票图片（需要替换 YOUR_TOKEN）
curl -X POST http://localhost:8080/api/doc/upload \
  -H "token: YOUR_TOKEN" \
  -F "file=@/path/to/invoice.jpg"

# 3. 查询我的票据列表
curl http://localhost:8080/api/doc/list \
  -H "token: YOUR_TOKEN"
```

---

## 📂 项目结构

```
SmartDoc/
├── src/main/java/com/example/smartdoc/
│   ├── SmartDocApplication.java       # Spring Boot 启动类
│   ├── controller/                    # 控制层（API 接口）
│   │   ├── DocController.java         # 票据管理接口
│   │   └── UserController.java        # 用户接口
│   ├── service/                       # 业务逻辑层
│   │   └── OcrService.java            # OCR 识别服务
│   ├── model/                         # 实体类
│   │   ├── InvoiceData.java           # 票据实体
│   │   └── User.java                  # 用户实体
│   ├── repository/                    # 数据访问层（JPA Repository）
│   │   ├── InvoiceRepository.java     # 票据数据仓库
│   │   └── UserRepository.java        # 用户数据仓库
│   ├── config/                        # 配置类
│   │   ├── WebConfig.java             # Web 配置（拦截器注册）
│   │   ├── LoginInterceptor.java      # 登录拦截器
│   │   └── WebSocketConfig.java       # WebSocket 配置
│   └── utils/                         # 工具类
├── resources/
│   └── application.properties         # 应用配置文件
├── DDL.sql                            # 数据库表结构
├── DML.sql                            # 测试数据
├── modify.sql                         # 数据库升级脚本
├── pom.xml                            # Maven 依赖配置
└── README.md                          # 项目说明文档
```

---

## 📡 API 接口文档

### 用户模块

| 方法 | 路径 | 说明 | 请求参数 |
|------|------|------|----------|
| POST | `/api/user/login` | 用户登录 | `{"username":"admin","password":"123456"}` |
| POST | `/api/user/register` | 用户注册 | `{"username":"user","password":"123456"}` |

**登录响应示例**：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "user": {
      "id": 1,
      "username": "admin",
      "nickname": "管理员"
    }
  }
}
```

### 票据模块

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/api/doc/upload` | 上传识别票据 | ✅ |
| POST | `/api/doc/save` | 保存归档票据 | ✅ |
| GET | `/api/doc/list` | 查询票据列表 | ✅ |
| DELETE | `/api/doc/delete/{id}` | 删除指定票据 | ✅ |
| PUT | `/api/doc/update` | 更新票据信息 | ✅ |

> 💡 **鉴权说明**：标记为 ✅ 的接口需要在请求头中携带 `token` 参数

**上传识别响应示例**：
```json
{
  "code": 200,
  "message": "识别成功",
  "data": {
    "merchantName": "XX餐饮有限公司",
    "itemName": "餐费",
    "invoiceCode": "No:12345678",
    "amount": 58.00,
    "date": "2024-01-15",
    "category": "餐饮"
  }
}
```

---

## ⚙️ 配置说明

### application.properties 配置项

| 配置项 | 说明 | 示例值 |
|--------|------|--------|
| `spring.datasource.url` | 数据库连接地址 | `jdbc:mysql://localhost:3306/smartdoc` |
| `spring.datasource.username` | 数据库用户名 | `root` |
| `spring.datasource.password` | 数据库密码 | `123456` |
| `baidu.ocr.app-id` | 百度 OCR APP ID | `你的AppID` |
| `baidu.ocr.api-key` | 百度 OCR API Key | `你的API_Key` |
| `baidu.ocr.secret-key` | 百度 OCR Secret Key | `你的Secret_Key` |
| `server.port` | 服务端口 | `8080` |
| `spring.servlet.multipart.max-file-size` | 最大文件上传大小 | `10MB` |

### 数据库表结构

#### sys_user（用户表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 ID |
| username | varchar(50) | 用户名（唯一） |
| password | varchar(100) | 密码 |
| nickname | varchar(50) | 用户昵称 |
| role | varchar(20) | 角色权限（admin/user） |

#### invoice_record（票据表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 ID |
| merchant_name | varchar(255) | 商户名称 |
| item_name | varchar(255) | 项目名称/商品明细 |
| invoice_code | varchar(50) | 发票号码 |
| amount | double(10,2) | 金额 |
| date | varchar(20) | 开票日期 |
| category | varchar(50) | 智能分类 |
| user_id | bigint | 所属用户 ID |
| create_time | datetime | 创建时间 |

---

## 🧪 测试账号

数据库初始化后会自动创建以下测试账号：

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | 123456 | 管理员 |
| test | 123456 | 普通用户 |

---

## ❓ 常见问题（FAQ）

### Q1: 启动时报错 `Access denied for user 'root'@'localhost'`

**A**: 检查 `application.properties` 中的数据库用户名和密码是否正确。

```properties
spring.datasource.username=root
spring.datasource.password=你的实际密码
```

### Q2: OCR 识别失败或返回空结果

**A**: 
1. 确认百度 OCR 的 API Key 配置正确
2. 检查百度云账号是否有剩余调用次数（免费版每天 50000 次）
3. 确认上传的图片格式支持（JPG/PNG/PDF）
4. 检查图片质量，建议分辨率不低于 800x600

### Q3: PDF 文件无法识别

**A**: 
1. 确保 `pom.xml` 中已引入 `pdfbox` 依赖
2. 检查 PDF 文件是否损坏或加密
3. 查看控制台日志是否有异常
4. 尝试使用图片格式进行测试

### Q4: 如何支持更多票据类型？

**A**: 编辑 `OcrService.java` 中的 `classifyCategory()` 方法，添加更多关键词匹配规则：

```java
private String classifyCategory(String text) {
    if (text.contains("餐饮") || text.contains("饭店")) return "餐饮";
    if (text.contains("交通") || text.contains("出租车")) return "交通";
    // 添加您的分类规则
    return "其他";
}
```

### Q5: 如何修改服务端口？

**A**: 在 `application.properties` 中修改：

```properties
server.port=8080  # 修改为您需要的端口
```

### Q6: 数据库连接超时怎么办？

**A**: 检查 MySQL 服务是否启动，并确认防火墙设置允许连接。可以在数据库 URL 中添加连接超时配置：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/smartdoc?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&connectTimeout=60000
```

---

## 🔗 相关链接

- **前端仓库**: [https://github.com/IIICJXXIII/smart_doc_frontend](https://github.com/IIICJXXIII/smart_doc_frontend)
- **百度 OCR 文档**: [https://ai.baidu.com/ai-doc/OCR/](https://ai.baidu.com/ai-doc/OCR/)
- **Spring Boot 官方文档**: [https://spring.io/projects/spring-boot](https://spring.io/projects/spring-boot)

---

## 📄 开源许可证

本项目采用 **MIT License** 开源许可证，详见 [LICENSE](LICENSE) 文件。

```
MIT License

Copyright (c) 2024 IIICJXXIII

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request 来帮助改进项目！

1. Fork 本仓库
2. 创建您的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交您的修改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启一个 Pull Request

---

## 📧 联系方式

- **项目作者**: [IIICJXXIII](https://github.com/IIICJXXIII)
- **项目地址**: [https://github.com/IIICJXXIII/smart_doc_backend](https://github.com/IIICJXXIII/smart_doc_backend)
- **问题反馈**: [提交 Issue](https://github.com/IIICJXXIII/smart_doc_backend/issues)

---

## ⭐ Star History

如果这个项目对你有帮助，欢迎给个 Star ⭐！

---

**© 2024 SmartDoc. All Rights Reserved.**
