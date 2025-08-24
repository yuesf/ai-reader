# AI-Reader 技术文档

## 1. 系统架构

### 1.1 技术架构概述
AI-Reader 采用前后端分离的架构设计，后端基于 Spring Boot 框架构建，使用 MyBatis 作为 ORM 框架，SQLite 作为嵌入式数据库，阿里云 OSS 作为文件存储服务。

### 1.2 架构图
```
┌─────────────────┐    REST API    ┌─────────────────┐
│   Vue Frontend  │◄──────────────►│  Spring Boot    │
└─────────────────┘                │    Backend      │
                                   └─────────┬───────┘
                                             │
                                    ┌────────▼────────┐
                                    │  SQLite Database│
                                    └─────────────────┘
                                             │
                                    ┌────────▼────────┐
                                    │  Alibaba Cloud  │
                                    │      OSS        │
                                    └─────────────────┘
```

### 1.3 技术栈
- 后端框架：Spring Boot 3.5.4
- ORM框架：MyBatis 3.0.3
- 数据库：SQLite 3.44.1.0
- 辅助工具：Lombok 1.18.30, Apache Commons Lang3 3.14.0
- 文件存储：阿里云OSS SDK
- 前端框架：Vue 3 + Vite
- 小程序：微信小程序原生开发

## 2. 项目结构

```
reader/
├── src/
│   ├── main/
│   │   ├── java/com/yuesf/aireader/
│   │   │   ├── annotation/     # 自定义注解
│   │   │   ├── config/         # 配置类
│   │   │   ├── controller/     # 控制器层
│   │   │   ├── dto/            # 数据传输对象
│   │   │   ├── entity/         # 实体类
│   │   │   ├── exception/      # 异常处理
│   │   │   ├── interceptor/    # 拦截器
│   │   │   ├── mapper/         # 数据访问层
│   │   │   ├── service/        # 业务逻辑层
│   │   │   ├── util/           # 工具类
│   │   │   └── vo/             # 视图对象
│   │   └── resources/
│   │       ├── mappers/        # MyBatis XML映射文件
│   │       ├── application.yml # 配置文件
│   │       ├── schema.sql      # 数据库表结构
│   │       └── data.sql        # 初始数据
│   └── test/                   # 测试代码
└── pom.xml                     # Maven配置文件
```

## 3. 核心模块设计

### 3.1 认证模块

#### 3.1.1 技术实现
- 使用JWT（JSON Web Token）进行用户身份验证
- 通过拦截器实现接口访问控制
- 用户信息存储在SQLite数据库中

#### 3.1.2 核心类
- [AuthController](file:///d:/projects/ai-reader/reader/src/main/java/com/yuesf/aireader/controller/AuthController.java#L23-L77)：处理认证相关接口
- [AuthInterceptor](file:///d:/projects/ai-reader/reader/src/main/java/com/yuesf/aireader/interceptor/AuthInterceptor.java#L19-L57)：认证拦截器
- [AuthService](file:///d:/projects/ai-reader/reader/src/main/java/com/yuesf/aireader/service/AuthService.java#L21-L66)：认证服务实现

### 3.2 报告管理模块

#### 3.2.1 技术实现
- 支持复杂条件查询和分页
- 支持关键词搜索（标题、摘要、标签）
- 支持多字段排序
- 通过外键关联保证数据一致性

#### 3.2.2 核心类
- [ReportController](file:///d:/projects/ai-reader/reader/src/main/java/com/yuesf/aireader/controller/ReportController.java#L24-L115)：处理报告相关接口
- [ReportService](file:///d:/projects/ai-reader/reader/src/main/java/com/yuesf/aireader/service/ReportService.java#L25-L144)：报告业务逻辑实现
- [ReportMapper](file:///d:/projects/ai-reader/reader/src/main/java/com/yuesf/aireader/mapper/ReportMapper.java#L15-L35)：报告数据访问接口

### 3.3 文件上传模块

#### 3.3.1 技术实现
- 集成阿里云OSS SDK实现文件存储
- 支持多种文件类型上传（报告文件、图片、通用文件）
- 自动生成唯一文件名，避免冲突
- 文件按类型分类存储在不同文件夹中（reports/images/files）
- 支持文件大小和类型验证
- 支持私有文件访问控制

#### 3.3.2 核心类
- [FileUploadController](file:///d:/projects/ai-reader/reader/src/main/java/com/yuesf/aireader/controller/FileUploadController.java#L28-L126)：处理文件上传相关接口
- [FileUploadService](file:///d:/projects/ai-reader/reader/src/main/java/com/yuesf/aireader/service/FileUploadService.java#L31-L151)：文件上传业务逻辑实现
- [OssConfig](file:///d:/projects/ai-reader/reader/src/main/java/com/yuesf/aireader/config/OssConfig.java#L15-L43)：OSS配置类

## 4. 数据库设计

### 4.1 数据库表结构

#### 4.1.1 reports表（报告信息表）
存储报告基本信息

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | VARCHAR(50) | 报告ID |
| title | VARCHAR(500) NOT NULL | 报告标题 |
| summary | TEXT | 报告摘要 |
| source | VARCHAR(100) | 来源 |
| category | VARCHAR(50) | 分类 |
| pages | INTEGER | 页数 |
| file_size | BIGINT | 文件大小 |
| publish_date | DATE | 发布日期 |
| update_date | DATE | 更新日期 |
| thumbnail | VARCHAR(500) | 缩略图URL |
| download_count | INTEGER | 下载次数 |
| view_count | INTEGER | 浏览次数 |
| is_free | BOOLEAN | 是否免费 |
| price | INTEGER | 价格（分） |
| report_file_id | VARCHAR(50) | 报告文件ID |
| report_file_url | VARCHAR(500) | 报告文件URL |
| report_file_name | VARCHAR(255) | 报告文件名 |
| report_file_size | VARCHAR(50) | 报告文件大小 |

#### 4.1.2 report_tags表（报告标签表）
存储报告标签信息

| 字段名 | 类型 | 说明 |
|--------|------|------|
| report_id | VARCHAR(50) | 报告ID |
| tag | VARCHAR(100) | 标签 |

#### 4.1.3 file_info表（文件信息表）
存储文件信息

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | VARCHAR(50) | 文件ID |
| file_name | VARCHAR(500) NOT NULL | 存储文件名 |
| original_name | VARCHAR(255) NOT NULL | 原始文件名 |
| file_size | BIGINT NOT NULL | 文件大小（字节） |
| file_type | VARCHAR(50) | 文件类型 |
| folder | VARCHAR(100) | 存储文件夹 |
| upload_time | TIMESTAMP | 上传时间 |
| upload_user_id | VARCHAR(50) | 上传用户ID |
| status | VARCHAR(20) | 文件状态 |
| request_id | VARCHAR(100) | OSS请求ID |

### 4.2 索引设计
- reports表：在分类、来源、发布日期等常用查询字段上建立索引
- report_tags表：在报告ID上建立索引
- file_info表：在文件夹、上传时间、状态、上传用户ID上建立索引

## 5. 接口设计

### 5.1 RESTful API设计原则
- 使用HTTP动词表示操作类型（GET、POST、PUT、DELETE）
- 使用名词表示资源
- 使用HTTP状态码表示操作结果
- 所有接口返回统一的[ApiResponse](file:///d:/projects/ai-reader/reader/src/main/java/com/yuesf/aireader/dto/ApiResponse.java#L11-L65)包装格式

### 5.2 接口统一返回格式
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 5.3 核心接口

#### 5.3.1 认证相关接口
- POST /v1/auth/login - 用户登录
- GET /v1/auth/verify - Token校验
- POST /v1/auth/logout - 用户退出

#### 5.3.2 报告管理接口
- POST /v1/reports - 获取报告列表
- GET /v1/reports/{id} - 获取报告详情
- POST /v1/reports/create - 创建报告
- DELETE /v1/reports/{id} - 删除报告
- POST /v1/reports/delete - 批量删除报告

#### 5.3.3 文件上传接口
- POST /v1/upload/report - 上传报告文件
- POST /v1/upload/image - 上传图片文件
- POST /v1/upload/file - 通用文件上传
- POST /v1/upload/report/info - 上传报告文件并返回文件信息
- DELETE /v1/upload/file - 删除文件

## 6. 阿里云OSS集成

### 6.1 OSS配置
通过[application.yml](file:///d:/projects/ai-reader/reader/src/main/resources/application.yml#L1-L41)配置文件进行OSS相关参数配置：

```yaml
app:
  oss:
    endpoint: https://oss-cn-hangzhou.aliyuncs.com
    access-key-id: your-access-key-id
    access-key-secret: your-access-key-secret
    bucket-name: your-bucket-name
    upload:
      max-file-size: 100MB
      allowed-extensions: jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,ppt,pptx
      base-url: https://your-bucket-name.oss-cn-hangzhou.aliyuncs.com
```

### 6.2 OSS功能实现
- 文件上传：支持多种文件类型上传，自动生成唯一文件名
- 文件访问：私有文件通过临时访问链接访问，链接有过期时间
- 文件删除：根据文件URL删除OSS中的文件
- ACL控制：上传时设置私有ACL权限
- 请求追踪：保存OSS返回的requestId用于追踪

### 6.3 OSS配置设计
1. **配置参数**
   - `endpoint`: 阿里云OSS地域节点
   - `access-key-id`: 阿里云访问密钥ID
   - `access-key-secret`: 阿里云访问密钥Secret
   - `bucket-name`: OSS存储桶名称
   - `upload.max-file-size`: 文件最大大小限制
   - `upload.allowed-extensions`: 允许上传的文件扩展名列表
   - `upload.base-url`: OSS访问基础URL

2. **配置方式**
   - 通过`application.yml`进行配置
   - 使用`@ConfigurationProperties`注解自动绑定配置属性
   - 通过`OssConfig`类创建OSS客户端Bean

### 6.4 OSS服务设计
1. **文件上传**
   - 支持多种文件类型上传（报告文件、图片、通用文件）
   - 自动生成唯一文件名，避免冲突
   - 文件按类型分类存储在不同文件夹中（reports/images/files）
   - 支持文件大小和类型验证
   - 上传时设置私有ACL权限
   - 保存OSS返回的requestId用于追踪

2. **文件访问**
   - 私有文件通过临时访问链接访问
   - 临时链接有过期时间（默认1小时）
   - 通过OSS SDK生成临时访问链接

3. **文件删除**
   - 根据文件URL删除OSS中的文件
   - URL安全检查，防止误删其他文件

4. **文件命名策略**
   - 按文件类型分类存储：reports、images、files
   - 时间戳+UUID生成唯一文件名
   - 避免文件名冲突和安全问题

### 6.5 OSS接口设计
1. **API接口**
   - `/v1/upload/report`: 上传报告文件
   - `/v1/upload/image`: 上传图片文件
   - `/v1/upload/file`: 通用文件上传
   - `/v1/upload/file`: 删除文件（DELETE方法）

2. **接口特点**
   - 统一使用`multipart/form-data`格式上传文件
   - 返回文件URL等信息供后续使用
   - 完善的异常处理和错误信息返回

### 6.6 安全设计
1. **访问控制**
   - 通过配置文件管理AccessKey，避免硬编码
   - 文件类型和大小限制，防止恶意上传
   - URL验证，确保只删除本系统上传的文件
   - 私有文件通过临时链接访问

2. **配置安全**
   - 建议使用环境变量或配置中心管理敏感信息
   - 定期轮换AccessKey，提高安全性

## 7. 配置信息

### 7.1 依赖版本信息
| 组件 | 版本 | 配置文件 |
|------|------|----------|
| Spring Boot | 3.5.4 | 根 `pom.xml` Parent |
| MyBatis Spring Boot | 3.0.3 | `reader/pom.xml` |
| SQLite JDBC | 3.44.1.0 | `reader/pom.xml` |
| Lombok | 1.18.30 | `reader/pom.xml` |
| Apache Commons Lang3 | 3.14.0 | `reader/pom.xml` |
| 端口 | 8080 | `application.yml` |
| 数据源 | `jdbc:sqlite:reader.db` | `application.yml` |
| MyBatis 映射 | `classpath*:mappers/*.xml` | `application.yml` |
| 类型别名 | `com.yuesf.aireader.entity` | `application.yml` |
| SQL 初始化 | `schema.sql` / `data.sql` | `application.yml` |

## 8. 技术实现特点

### 8.1 框架技术
- Spring Boot 3.5.4作为主框架
- MyBatis作为ORM框架
- SQLite作为嵌入式数据库
- 阿里云OSS SDK进行文件存储操作
- Lombok简化Java代码
- Apache Commons Lang3提供常用工具方法

### 8.2 安全控制
- JWT Token认证机制
- 文件类型限制
- 文件大小限制
- 参数验证和异常处理

### 8.3 性能优化
- 数据库索引优化
- 分页查询避免大数据量传输
- 合理的缓存策略

## 9. 部署与配置

### 9.1 环境要求
- JDK 17
- Maven 3.6+
- 可访问的阿里云OSS服务

### 9.2 部署步骤
1. 配置阿里云OSS参数
2. 执行`mvn clean package`构建项目
3. 运行`java -jar reader/target/ai-reader-0.0.1-SNAPSHOT.jar`启动服务

### 9.3 配置项
- 数据库连接配置
- OSS服务配置
- 文件上传限制配置
- JWT密钥配置

## 10. 注意事项

1. 查询接口必须具备条件：`keyword` 或显式 `startDate/endDate`；若都未提供，则默认按近 30 天范围查询。
2. `page` 从 1 开始；`pageSize` 最大 50，避免大结果集。
3. 日期格式统一 `YYYY-MM-DD`；价格单位为"分"。
4. 如需在列表返回 `tags`，可在 MyBatis XML 中追加关联查询与映射。
5. 文件上传相关字段：`reportFileId`（必填）、`reportFileUrl`、`reportFileName`、`reportFileSize` 用于存储文件相关信息。
6. 数据库使用外键约束，删除报告时会自动删除关联的标签数据。
7. 需要配置正确的阿里云OSS参数才能使用文件上传功能。
8. **新建报告功能改造要点**：
   - 必须上传报告文件，上传到OSS后文件信息自动存储到`file_info`表
   - 新建报告时必须提供`reportFileId`，关联文件信息表
   - 文件信息包含：文件名称、大小、URL、类型、上传时间等
   - 支持文件状态管理（ACTIVE/DELETED）