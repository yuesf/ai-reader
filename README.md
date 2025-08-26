# AI Reader 项目

AI Reader 是一个用于文件上传、报告生成与管理的AI阅读辅助系统。

## 项目结构

- [reader](./reader) - 后端Spring Boot服务  
- [ui-vue3](./ui-vue3) - 前端Vue 3管理界面  
- [wxchat](./wxchat) - 微信小程序客户端

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

### 后端服务 (reader)

1. 确保已安装 JDK 17 和 Maven
2. 生成SSL证书:
   - Windows: 运行 `generate-ssl-cert.bat`
   - Linux/macOS: 运行 `chmod +x generate-ssl-cert.sh && ./generate-ssl-cert.sh`
3. 构建项目: `mvn clean package`
4. 运行项目: `java -jar reader/target/ai-reader-0.0.1-SNAPSHOT.jar`

### 前端界面

1. 确保已安装 Node.js 16+ 和 npm
2. 进入前端目录：`cd ui-vue3`
3. 安装依赖：`npm install`
4. 启动开发服务器：`npm run dev`

### 微信小程序

1. 使用微信开发者工具打开 `wxchat` 目录
2. 配置小程序 AppID
3. 编译运行


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

```bash
# windows 启动命令
cmd /c "npm run dev"
```

## 贡献指南

欢迎提交 Issue 和 Pull Request 来帮助改进项目。

## 许可证

[待补充]