# AI Reader 后端服务

## 项目概述

AI Reader 是一个基于Spring Boot的报告查询和搜索系统，提供RESTful API接口来管理报告数据。

## 技术架构

### 架构模式
- **Controller层**: 处理HTTP请求，参数验证和响应封装
- **Service层**: 业务逻辑处理，数据过滤和排序
- **DAO层**: 数据访问层，使用Spring Data JPA操作数据库
- **Entity层**: 实体类，映射数据库表结构

### 技术栈
- **框架**: Spring Boot 3.x
- **数据库**: SQLite
- **ORM**: Spring Data JPA + Hibernate
- **构建工具**: Maven
- **Java版本**: 17

## 项目结构

```
src/main/java/com/yuesf/aireader/
├── controller/          # 控制器层
│   └── ReportController.java
├── service/            # 服务层
│   └── impl/
│       └── ReportServiceImpl.java
├── dao/               # 数据访问层
│   └── ReportDao.java
├── entity/            # 实体类
│   └── Report.java
├── dto/               # 数据传输对象
│   ├── ReportListRequest.java
│   └── ReportListResponse.java
├── config/            # 配置类
│   └── DatabaseConfig.java
└── exception/         # 异常处理
    └── GlobalExceptionHandler.java

src/main/resources/
├── application.yml    # 应用配置
├── schema.sql         # 数据库表结构
└── data.sql          # 初始数据
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

#### report_tags 表
- `report_id`: 报告ID（外键）
- `tag`: 标签名称

## API接口

### 1. 获取/搜索报告列表
- **接口**: `POST /v1/reports`
- **功能**: 支持基础查询和高级搜索
- **参数**: 分页、关键词、分类、来源、日期范围、排序等

### 2. 获取报告详情
- **接口**: `GET /v1/reports/{id}`
- **功能**: 根据ID获取报告详细信息

### 3. 健康检查
- **接口**: `GET /v1/health`
- **功能**: 服务健康状态检查

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
- 自动执行 `schema.sql` 创建表结构
- 自动执行 `data.sql` 插入初始测试数据

## 配置说明

### 数据库配置
- 数据库类型: SQLite
- 数据库文件: `reader.db` (项目根目录)
- 连接配置: `application.yml`

### JPA配置
- 方言: SQLiteDialect
- DDL策略: create-drop (开发环境)
- SQL日志: 启用

## 开发说明

### 添加新的查询方法
1. 在 `ReportDao` 中添加新的查询方法
2. 在 `ReportServiceImpl` 中实现业务逻辑
3. 在 `ReportController` 中添加新的接口

### 数据库迁移
- 修改 `schema.sql` 文件
- 重启应用或手动执行SQL脚本

## 注意事项

1. SQLite数据库文件会在项目根目录自动创建
2. 开发环境使用 `create-drop` 模式，每次启动会重建表结构
3. 生产环境建议修改为 `update` 模式
4. 标签使用独立的表存储，支持一对多关系
5. 所有日期使用 `LocalDate` 类型，格式为 `YYYY-MM-DD`

## 性能优化建议

1. 为常用查询字段添加数据库索引
2. 实现查询结果缓存机制
3. 使用分页查询避免大量数据返回
4. 优化JPA查询，避免N+1问题
