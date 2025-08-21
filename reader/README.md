# AI Reader 后端服务

## 项目概述

AI Reader 是一个基于Spring Boot的报告查询和搜索系统，提供RESTful API接口来管理报告数据。

## 技术架构

### 架构模式
- **Controller层**: 处理HTTP请求，参数验证和响应封装
- **Service层**: 业务逻辑处理，数据过滤和排序
- **Mapper层**: 数据访问层，使用MyBatis操作数据库
- **Entity层**: 实体类，映射数据库表结构

### 技术栈
- **框架**: Spring Boot 3.x
- **数据库**: SQLite
- **ORM**: MyBatis
- **构建工具**: Maven
- **Java版本**: 17

## 项目结构

```
src/main/java/com/yuesf/aireader/
├── controller/          # 控制器层
│   ├── AuthController.java
│   ├── FileUploadController.java
│   ├── ReportController.java
│   └── UserController.java
├── service/             # 服务层
│   ├── AuthService.java
│   ├── FileInfoService.java
│   ├── FileUploadService.java
│   ├── ReportService.java
│   └── UserService.java
├── mapper/              # 数据访问层
│   ├── AdminUserMapper.java
│   ├── FileInfoMapper.java
│   └── ReportMapper.java
├── entity/              # 实体类
│   ├── AdminUser.java
│   ├── FileInfo.java
│   └── Report.java
├── dto/                 # 数据传输对象
│   ├── ApiResponse.java
│   ├── ReportBatchDeleteRequest.java
│   ├── ReportCreateRequest.java
│   ├── ReportListRequest.java
│   ├── ReportListResponse.java
│   ├── UserListRequest.java
│   └── UserListResponse.java
├── config/              # 配置类
│   ├── AuthInterceptor.java
│   ├── OssConfig.java
│   └── WebMvcConfig.java
├── util/                # 工具类
│   └── FileUtils.java
└── exception/           # 异常处理
    └── GlobalExceptionHandler.java

src/main/resources/
├── mappers/             # MyBatis XML映射文件
│   ├── FileInfoMapper.xml
│   └── ReportMapper.xml
├── application.yml      # 应用配置
├── schema.sql           # 数据库表结构
└── data.sql             # 初始数据
```

## 数据库设计

### 主要表结构

#### reports 表
- `id`: 报告唯一标识
- `title`: 报告标题
- `summary`: 报告摘要
- `source`: 报告来源
- `category`: 报告分类
- `pages`: 页数
- `file_size`: 文件大小
- `publish_date`: 发布日期
- `update_date`: 更新日期
- `thumbnail`: 缩略图URL
- `download_count`: 下载次数
- `view_count`: 浏览次数
- `is_free`: 是否免费
- `price`: 价格
- `report_file_id`: 报告文件ID（关联[file_info](file:///d:/projects/ai-reader/reader/src/main/resources/schema.sql#L15-L15)表）
- `report_file_url`: 报告文件URL
- `report_file_name`: 报告文件名
- `report_file_size`: 报告文件大小

#### file_info 表（文件信息表）
- `id`: 文件唯一标识
- `file_name`: 存储文件名（OSS中的路径）
- `original_name`: 原始文件名
- `file_size`: 文件大小（字节）
- `file_type`: 文件类型
- `folder`: 存储文件夹
- `upload_time`: 上传时间
- `upload_user_id`: 上传用户ID
- `status`: 文件状态（ACTIVE/DELETED）
- `request_id`: OSS请求ID

#### report_tags 表
- `report_id`: 报告ID（外键）
- `tag`: 标签名称

#### admin_users 表
- `id`: 用户ID
- `username`: 用户名
- `password`: 密码（明文存储，生产环境应使用加盐哈希）
- `display_name`: 显示名称
- `status`: 用户状态
- `create_time`: 创建时间

## API接口

### 1. 认证与鉴权
- **登录**：`POST /v1/auth/login`
- **Token 校验**：`GET /v1/auth/verify`
- **退出**：`POST /v1/auth/logout`

### 2. 报告管理
- **获取/搜索报告列表**：`POST /v1/reports`
- **获取报告详情**：`GET /v1/reports/{id}`
- **创建报告**：`POST /v1/reports/create`
- **删除报告**：`DELETE /v1/reports/{id}`
- **批量删除报告**：`POST /v1/reports/delete`

### 3. 文件上传
- **上传报告文件**：`POST /v1/upload/report`
- **上传报告文件并返回文件信息**：`POST /v1/upload/report/info`
- **上传图片文件**：`POST /v1/upload/image`
- **通用上传**：`POST /v1/upload/file`
- **删除文件**：`DELETE /v1/upload/file`

### 4. 用户管理
- **获取用户列表**：`POST /v1/users`
- **删除用户**：`DELETE /v1/users/{id}`

### 5. 健康检查
- **接口**：`GET /v1/health`

## 文件上传与报告关联功能说明

### 新增文件信息管理模块

**主要功能：**
- 文件信息的增删改查
- 文件状态管理（ACTIVE/DELETED）
- 文件验证（检查文件是否存在且有效）

### 改造文件上传服务

**新增方法：**
```java
// 上传文件并保存文件信息
public FileInfo uploadFile(MultipartFile file, String folder, String uploadUserId)

// 上传报告文件并保存文件信息
public FileInfo uploadReportFile(MultipartFile file, String uploadUserId)
```

**改造要点：**
- 文件上传到OSS后，自动创建文件信息记录
- 返回完整的文件信息对象，包含文件ID
- 保持向后兼容，原有方法仍然可用

### 改造报告创建流程

**验证逻辑：**
- 必须提供 `report_file_id`
- 验证文件信息是否存在且有效
- 自动填充文件相关信息（URL、名称、大小）

**数据流程：**
1. 用户上传报告文件 → 获得文件信息（包含文件ID）
2. 创建报告时提供文件ID → 系统验证文件有效性
3. 自动关联文件信息 → 完成报告创建

## 运行说明

### 环境要求
- JDK 17+
- Maven 3.6+

### 启动步骤
1. 克隆项目到本地
2. 进入项目目录: `cd reader`
3. 编译项目: `mvn compile`
4. 运行项目: `mvn spring-boot:run`

### 数据库初始化
- 项目启动时会自动创建SQLite数据库文件 `reader.db`