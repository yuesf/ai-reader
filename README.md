# AI Reader - AI文档阅读辅助系统

AI Reader 是一个用于文件上传、报告生成与管理的AI阅读辅助系统，旨在帮助用户处理和分析文档内容。

## 项目概述

AI Reader 提供完整的文档上传、解析、报告生成与管理功能。系统支持多种文档格式，利用AI技术对文档内容进行分析和处理，生成结构化报告，并提供便捷的管理界面。

## 核心功能

- **文件上传与存储**：支持多种文档格式上传，使用阿里云OSS进行文件存储
- **报告生成与管理**：基于文档内容生成报告，并提供完整的报告管理功能
- **报告列表展示**：支持分页、筛选、排序等功能的报告列表展示
- **多端适配**：同时支持Web端和微信小程序端访问

## 技术架构

AI Reader 采用前后端分离架构：

- **后端**：Spring Boot + MyBatis + SQLite
- **前端**：Vue 3 + Element Plus
- **移动端**：微信小程序
- **文件存储**：阿里云OSS

### 后端技术栈

- Spring Boot 3.5.4
- MyBatis
- Java 17
- Maven

### 前端技术栈

- Vue 3
- Element Plus
- Vite

## 项目结构

```
ai-reader/
├── reader/              # 后端服务（Spring Boot）
│   ├── src/main/java    # Java源代码
│   ├── src/main/resources # 配置文件和资源
│   ├── sql/             # 数据库脚本
│   └── prompts/         # 提示词和文档
├── ui-vue3/             # 前端界面（Vue 3）
│   ├── src/             # Vue源代码
│   └── public/          # 静态资源
└── wxchat/              # 微信小程序
    ├── pages/           # 小程序页面
    └── utils/           # 工具函数
```

## 快速开始

### 后端服务

1. 确保已安装 JDK 17 和 Maven 3.x
2. 进入后端目录：`cd reader`
3. 编译项目：`mvn compile`
4. 运行项目：`mvn spring-boot:run`

### 前端界面

1. 确保已安装 Node.js 16+ 和 npm
2. 进入前端目录：`cd ui-vue3`
3. 安装依赖：`npm install`
4. 启动开发服务器：`npm run dev`

### 微信小程序

1. 使用微信开发者工具打开 `wxchat` 目录
2. 配置小程序 AppID
3. 编译运行

## 配置说明

### 数据库配置

系统默认使用 SQLite 数据库，无需额外配置。

### OSS配置

编辑 `reader/src/main/resources/application.yml` 文件，配置阿里云OSS信息：

```yaml
app:
  oss:
    endpoint: https://oss-cn-hangzhou.aliyuncs.com
    access-key-id: your-access-key-id
    access-key-secret: your-access-key-secret
    bucket-name: your-bucket-name
```

## API文档

后端提供RESTful API接口：

- 认证接口：`/v1/auth/**`
- 报告管理：`/v1/reports/**`
- 文件上传：`/v1/upload/**`
- 用户管理：`/v1/users/**`

## 部署说明

### 后端部署

```bash
# 构建项目
mvn clean package

# 运行应用
java -jar reader/target/ai-reader-0.0.1-SNAPSHOT.jar
```

### 前端部署

```bash
# 构建项目
npm run build

# 部署 dist 目录到Web服务器
```

## 贡献指南

欢迎提交 Issue 和 Pull Request 来帮助改进项目。

## 许可证

[待补充]