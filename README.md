# 🧾 SmartDoc - 智能票据归档助手

## 前端仓库
https://github.com/IIICJXXIII/smart_doc_frontend

---

SmartDoc 是一个基于 **AI 视觉技术** 的智能票据管理系统。旨在解决传统财务报销中手工录入繁琐、发票整理困难的问题。系统支持 **JPG/PNG 图片及 PDF 电子发票** 的原件上传,利用 **OCR 技术** 自动提取商户、金额、日期、发票号码及具体购买项目,并实现 **自动分类与云端归档**。

> 💡 **适用场景**: 软件工程课程设计 / 毕业设计 参考项目

---

## ✨ 核心功能 (Features)

- 🤖 **多模态 AI 识别**: 集成百度智能云 OCR 高精度接口,支持对印刷体、手写体的精准识别
- 📄 **PDF 电子发票支持**: 后端内置 PDF 渲染引擎 (Apache PDFBox),可直接解析并预览 PDF 格式的电子发票,无需手动截图
- 🧠 **智能语义提取**: 基于正则算法的后处理引擎,自动从杂乱的 OCR 文字中提取"发票号码"、"项目名称"、"价税合计"等关键字段
- 🏷️ **自动智能分类**: 根据识别到的商品关键词(如"餐饮"、"交通"、"办公"),自动归类票据类型
- 🔐 **多用户数据隔离**: 完善的用户鉴权系统(基于 Session/Interceptor 机制),确保每个用户只能管理自己的财务数据
- 💾 **数据持久化存储**: 基于 MySQL + JPA 实现票据数据的增删改查,支持历史记录查询
- 🔄 **完整的 CRUD 操作**: 支持票据的新增、查询、修改和删除,提供完善的数据管理能力
- 📊 **数据可视化**: 支持 ECharts 数据统计看板,展示支出趋势和分类占比

---

## 🛠️ 技术栈 (Tech Stack)

### 前端 (Frontend)
https://github.com/IIICJXXIII/smart_doc_frontend

| 技术 | 版本 | 说明 |
|-----|------|-----|
| **核心框架** | Vue 3.5.25 | Composition API |
| **开发语言** | TypeScript 5.9 | 类型安全 |
| **构建工具** | Vite 7.2 | 下一代前端构建工具 |
| **UI 组件库** | Element Plus 2.12 | Vue 3 组件库 |
| **状态管理** | Pinia 3.0 | Vue 3 官方状态管理 |
| **图表库** | ECharts 6.0 | 数据可视化 |

### 后端 (Backend)

| 技术 | 版本 | 说明 |
|-----|------|-----|
| **开发语言** | Java 17+ | 主开发语言 |
| **核心框架** | Spring Boot 3.5.8 | Web 应用框架 |
| **持久层** | Spring Data JPA (Hibernate) | ORM 框架 |
| **数据库** | MySQL 8.0 | 关系型数据库 |
| **AI SDK** | Baidu AIP SDK 4.16.19 | 百度 AI 开放平台 SDK |
| **PDF 处理** | Apache PDFBox 2.0.27 | PDF 转图片处理 |
| **Excel 导出** | Apache POI 5.2.3 | Excel 文件处理 |
| **工具库** | Hutool 5.8.16 | 工具类库 |
| **简化代码** | Lombok | 注解简化 Java Bean |

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
# 1. 创建表结构
mysql -u root -p smartdoc < DDL.sql

# 2. 插入测试数据
mysql -u root -p smartdoc < DML.sql

# 3. (可选) 如果需要支持多用户隔离,执行以下脚本添加 user_id 字段
mysql -u root -p smartdoc < modify.sql
```

**说明**: 
- `DDL.sql`: 创建 `invoice_record` (票据表) 和 `sys_user` (用户表)
- `DML.sql`: 插入测试数据 (包含 10 条发票记录和 2 个测试用户)
- `modify.sql`: 为 `invoice_record` 表添加 `user_id` 字段以支持多租户隔离

### 3. 配置百度 OCR

#### 3.1 获取 API Key

1. 访问 [百度智能云控制台](https://console.bce.baidu.com/)
2. 进入 **产品服务 > 文字识别 OCR**
3. 创建应用并获取 `APP_ID`、`API_Key`、`Secret_Key`

#### 3.2 修改配置文件

编辑 `src/main/resources/application.properties`:

```properties
# ==============================
# 百度 AI 配置 (Baidu OCR Config)
# ==============================
baidu.ocr.app-id=你的AppID
baidu.ocr.api-key=你的API_Key
baidu.ocr.secret-key=你的Secret_Key

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

启动成功后,后端服务运行在: **http://localhost:8080**

### 5. 测试 API

使用 **Postman** 或 **curl** 测试接口:

```bash
# 1. 用户登录
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 2. 上传发票图片
curl -X POST http://localhost:8080/api/doc/upload \
  -H "token: 你的登录token" \
  -F "file=@/path/to/invoice.jpg"

# 3. 查询我的票据
curl http://localhost:8080/api/doc/list \
  -H "token: 你的登录token"
```

---

## 📂 项目结构 (Project Structure)

```
SmartDoc/
├── src/
│   ├── main/
│   │   ├── java/com/example/smartdoc/
│   │   │   ├── SmartDocApplication.java       # 启动类
│   │   │   ├── controller/                    # 控制层 (API 接口)
│   │   │   │   ├── DocController.java         # 票据管理接口
│   │   │   │   └── UserController.java        # 用户登录接口
│   │   │   ├── service/                       # 业务逻辑层
│   │   │   │   └── OcrService.java            # OCR 识别核心服务
│   │   │   ├── model/                         # 实体类
│   │   │   │   ├── InvoiceData.java           # 票据实体
│   │   │   │   └── User.java                  # 用户实体
│   │   │   ├── repository/                    # 数据访问层 (JPA)
│   │   │   │   ├── InvoiceRepository.java     # 票据数据仓库
│   │   │   │   └── UserRepository.java        # 用户数据仓库
│   │   │   ├── config/                        # 配置类
│   │   │   │   ├── WebConfig.java             # Web 配置(拦截器注册)
│   │   │   │   └── LoginInterceptor.java      # 登录拦截器
│   │   │   └── utils/                         # 工具类
│   │   └── resources/
│   │       ├── application.properties          # 配置文件
│   │       ├── static/                         # 静态资源
│   │       └── templates/                      # 模板文件
│   └── test/                                   # 单元测试
├── target/                                     # 编译输出目录
├── uploads/                                    # 文件上传目录
├── DDL.sql                                     # 数据库表结构
├── DML.sql                                     # 测试数据脚本
├── modify.sql                                  # 数据库升级脚本
├── pom.xml                                     # Maven 依赖配置
└── README.md                                   # 项目说明文档
```

---

## 📡 核心 API 接口

### 用户模块

| 接口 | 方法 | 路径 | 说明 |
|-----|------|------|-----|
| 用户登录 | POST | `/api/user/login` | 返回 token 用于后续鉴权 |
| 用户注册 | POST | `/api/user/register` | 创建新用户账号 |

### 票据模块

| 接口 | 方法 | 路径 | 说明 | 鉴权 |
|-----|------|------|-----|-----|
| 上传识别 | POST | `/api/doc/upload` | 上传发票图片/PDF,返回识别结果 | ✅ |
| 保存归档 | POST | `/api/doc/save` | 保存识别结果到数据库 | ✅ |
| 查询列表 | GET | `/api/doc/list` | 查询当前用户的所有票据 | ✅ |
| 删除票据 | DELETE | `/api/doc/delete/{id}` | 删除指定票据 | ✅ |
| 更新票据 | PUT | `/api/doc/update` | 修改票据信息 | ✅ |

> 💡 **鉴权说明**: 需要在请求头中携带 `token` 参数

---

## 🔑 核心功能实现

### 1. OCR 智能识别流程

```
用户上传文件 → 判断文件类型
                 ├─ PDF → 使用 PDFBox 转换为图片
                 └─ JPG/PNG → 直接读取字节流
                              ↓
                      调用百度 OCR 通用文字识别 API
                              ↓
                      正则提取关键字段:
                      - 发票号码 (No:xxxxxxxx)
                      - 商户名称 (第一行文字)
                      - 金额 (¥xx.xx)
                      - 日期 (yyyy-mm-dd)
                              ↓
                      智能分类 (餐饮/交通/办公等)
                              ↓
                      返回结构化数据给前端
```

### 2. 多用户数据隔离

```java
// 登录拦截器验证 token
@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        String token = request.getHeader("token");
        // 验证 token 并存储用户信息到 request.session
    }
}

// 票据保存时绑定用户ID
@PostMapping("/save")
public String saveDoc(@RequestBody InvoiceData data) {
    User currentUser = getCurrentUser();
    data.setUserId(currentUser.getId());
    invoiceRepository.save(data);
}

// 查询时只返回当前用户的数据
@GetMapping("/list")
public List<InvoiceData> getList() {
    return invoiceRepository.findByUserId(currentUser.getId());
}
```

---

## 🧪 测试账号

数据库初始化后会自动创建以下测试账号:

| 用户名 | 密码 | 角色 | 说明 |
|-------|------|------|-----|
| admin | 123456 | 管理员 | 超级管理员账号 |
| test | 123456 | 普通用户 | 测试用户账号 |

---

## 📝 待办事项 (TODO)

- [x] 基础上传与 OCR 识别
- [x] PDF 格式支持
- [x] 用户注册与登录系统
- [x] 数据持久化与多租户隔离
- [x] 票据的增删改查功能
- [x] 前端界面开发 (Vue 3 + Element Plus)
- [x] ECharts 数据可视化看板
- [ ] 导出 Excel 报表功能
- [ ] 多文件批量上传支持
- [ ] 票据图片云存储(OSS)

---

## 🔧 常见问题 (FAQ)

### Q1: 启动时报错 `Access denied for user 'root'@'localhost'`
**A**: 检查 `application.properties` 中的数据库用户名和密码是否正确。

### Q2: OCR 识别失败或返回空结果
**A**: 
1. 确认百度 OCR 的 API Key 配置正确
2. 检查百度云账号是否有剩余调用次数(免费版每天 50000 次)
3. 上传的图片格式是否支持(JPG/PNG/PDF)

### Q3: PDF 文件无法识别
**A**: 
1. 确保 `pom.xml` 中已引入 `pdfbox` 依赖
2. 检查 PDF 文件是否损坏
3. 查看控制台是否有异常日志

### Q4: 如何支持更多票据类型?
**A**: 编辑 `OcrService.java` 中的 `classifyCategory()` 方法,添加更多关键词匹配规则。

---

## 🤝 贡献与许可

本项目仅供 **学习与交流** 使用。

**License**: MIT

---

## 📧 联系方式

如有问题或建议,欢迎提交 Issue 或 Pull Request。

**项目作者**: [IIICJXXIII](https://github.com/IIICJXXIII)  
**项目地址**: https://github.com/IIICJXXIII/smart_doc_backend

---

## 🎯 项目亮点

1. **完整的前后端分离架构** - 适合作为 Spring Boot + Vue 3 入门项目
2. **AI 技术实战应用** - 集成百度 OCR,体验人工智能在实际业务中的应用
3. **规范的代码结构** - 遵循 MVC 三层架构,代码清晰易读
4. **多用户系统设计** - 实现了基础的用户鉴权和数据隔离
5. **详细的注释文档** - 核心代码均有详细注释,便于理解学习
6. **数据可视化** - ECharts 图表展示财务统计数据

---

**⭐ 如果这个项目对你有帮助,欢迎 Star ⭐**
