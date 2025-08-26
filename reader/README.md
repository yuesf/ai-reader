# AI Reader 后端服务

## 接口说明

### 接口分离设计

为了满足不同客户端的需求，报告相关接口分为两个版本：

#### 1. 后台管理接口 (`/v1/reports/*`)
- 适用于后台管理系统
- 提供完整的报告管理功能
- 支持高级搜索和批量操作
- 返回完整的报告数据结构
- **需要JWT鉴权和管理员权限**

#### 2. 小程序接口 (`/v1/mini/reports/*`)
- 适用于微信小程序等移动端
- 提供简化的报告查询功能
- 返回适合移动端显示的数据结构
- 限制分页大小（最大20条）
- **无需鉴权，公开访问**

### API 接口列表

#### 认证接口
- `POST /v1/auth/login` - 用户登录，获取JWT令牌
- `POST /v1/auth/verify` - 验证JWT令牌

#### 后台管理接口（需要鉴权）
- `POST /v1/reports` - 获取报告列表（支持高级搜索）
- `GET /v1/reports/{id}` - 获取报告详情
- `POST /v1/reports/create` - 创建报告
- `DELETE /v1/reports/{id}/thumbnail` - 删除报告缩略图
- `POST /v1/reports/{id}/thumbnail/regenerate` - 重新生成缩略图
- `POST /v1/reports/batch-delete` - 批量删除报告
- `GET /v1/reports/file/{id}` - 获取报告文件临时访问URL

#### 文件上传接口（需要鉴权）
- `POST /v1/upload/report` - 上传报告文件
- `POST /v1/upload/report/info` - 上传报告文件并返回信息
- `POST /v1/upload/image` - 上传图片文件
- `GET /v1/images/{id}` - 获取图片文件（需要登录）

#### 小程序接口（无需鉴权）
- `POST /v1/mini/reports` - 获取小程序报告列表
- `GET /v1/mini/reports/{id}` - 获取小程序报告详情

## 配置说明

### SSL/HTTPS 配置

本服务支持 HTTPS 协议，以确保数据传输的安全性。默认配置如下：

- HTTPS 端口: 443
- HTTP 端口: 80 (自动重定向到 HTTPS)
- SSL 证书: `yuesf.cn.jks` (位于 `src/main/resources` 目录)

#### SSL 配置文件

1. `application.yml` 包含 SSL 证书配置:
   ```yaml
   server:
     port: 443
     ssl:
       key-store: classpath:yuesf.cn.jks
       key-store-password: changeit
       key-store-type: JKS
       key-alias: yuesf.cn
       enabled: true
   ```

2. `application.properties` 包含 HTTP 端口配置:
   ```properties
   server.http.port=80
   ```

3. `SslConfig.java` 配置了 HTTP 到 HTTPS 的重定向:
   - HTTP 请求会自动重定向到 HTTPS
   - 同时监听 80 和 443 端口

#### 生成本地开发SSL证书

有关如何生成本地开发用SSL证书的详细说明，请参考 [SSL_CERTIFICATE_GENERATION.md](file:///d:/projects/ai-reader/reader/SSL_CERTIFICATE_GENERATION.md) 文件。

#### 自定义 SSL 证书

如需使用自定义 SSL 证书，请替换以下文件:
1. 将新的 JKS 证书文件放置在 `src/main/resources` 目录下
2. 更新 `application.yml` 中的证书配置:
   ```yaml
   server:
     ssl:
       key-store: classpath:your-certificate.jks
       key-store-password: your-password
       key-alias: your-alias
   ```

### 阿里云文档智能服务配置

本服务使用阿里云 DashScope 的兼容 OpenAI 接口来生成报告摘要。配置通过 `application.properties` 文件进行管理。

#### 配置项

在 `src/main/resources/application.properties` 中配置以下项：

```properties
# 阿里云文档智能服务配置
app.ai.apiKey=你的DashScope_API_Key
app.ai.summarize.model=qwen-long
app.ai.endpoint=https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions

# JWT配置
app.jwt.secret=your-secret-key-here-change-in-production
app.jwt.expiration=86400

# 异步任务配置
spring.task.execution.pool.core-size=2
spring.task.execution.pool.max-size=5
spring.task.execution.pool.queue-capacity=100
```

#### 配置说明

- `app.ai.apiKey`: 阿里云 DashScope 的 API 密钥（必需）
- `app.ai.summarize.model`: 使用的 AI 模型，默认为 `qwen-long`
- `app.ai.endpoint`: 服务端点，默认为 DashScope 兼容接口
- `app.jwt.secret`: JWT签名密钥（生产环境必须修改）
- `app.jwt.expiration`: JWT过期时间（秒），默认24小时
- `spring.task.execution.*`: 异步任务线程池配置

### JWT认证配置

#### 配置类结构

配置通过 `AIConfig` 类进行管理，使用 `@ConfigurationProperties` 注解自动绑定配置文件中的属性：

```java
@ConfigurationProperties(prefix = "app.ai")
public class AIConfig {
    private String apiKey;
    private SummarizeConfig summarize;
    private String endpoint;
    
    public static class SummarizeConfig {
        private String model;
    }
}
```

#### 使用方式

在服务类中注入配置：

```java
@Service
public class AITextSummaryService {
    @Autowired
    private AIConfig aiConfig;
    
    public String summarize(String text) {
        // 使用 aiConfig.getApiKey(), aiConfig.getEndpoint() 等
    }
}
```

## 权限控制

### 权限注解

使用 `@RequireAuth` 注解控制接口访问权限：

```java
@RestController
@RequestMapping("/v1")
@RequireAuth(requireAdmin = true) // 整个控制器需要管理员权限
public class ReportController {
    
    @GetMapping("/reports/{id}")
    public ApiResponse<Report> getReportById(@PathVariable String id) {
        // 需要管理员权限
    }
    
    @GetMapping("/images/{id}")
    @RequireAuth(requireAdmin = false) // 只需要登录，不需要管理员权限
    public void getImage(@PathVariable String id, HttpServletResponse response) {
        // 只需要登录验证
    }
}
```

### 权限级别

1. **无需鉴权**: 小程序接口、健康检查等
2. **需要登录**: 图片访问等基础功能
3. **需要管理员**: 报告管理、文件上传等核心功能

### 认证流程

1. **登录**: 调用 `POST /v1/auth/login` 获取JWT令牌
2. **请求**: 在请求头中添加 `Authorization: Bearer <token>`
3. **验证**: 拦截器自动验证令牌和权限
4. **响应**: 根据权限返回相应结果

## 日志系统

### 日志框架

项目使用 Lombok 的 `@Slf4j` 注解和 SLF4J 日志框架，提供统一的日志输出。

### 日志级别

- `log.info()`: 记录重要的业务操作信息
- `log.warn()`: 记录警告信息（如参数错误）
- `log.error()`: 记录错误信息和异常堆栈
- `log.debug()`: 记录调试信息（如文件访问）

### 日志示例

``java
@Slf4j
@Service
public class ReportService {
    
    public Report createReport(ReportCreateRequest request) {
        log.info("创建报告，标题: {}", request.getTitle());
        try {
            // 业务逻辑
            log.info("报告创建成功，ID: {}", report.getId());
            return report;
        } catch (Exception e) {
            log.error("报告创建失败", e);
            throw e;
        }
    }
}
```

## 功能特性

### 报告处理
- PDF 首页自动提取并生成缩略图
- 异步 AI 摘要生成
- 缩略图管理（删除、重新生成）

### 文件管理
- 支持 PDF、Word、Excel、PowerPoint 格式
- 自动上传到阿里云 OSS
- 私有文件访问控制

### 权限管理
- JWT令牌认证
- 基于注解的权限控制
- 角色和权限级别管理

## 开发说明

### 依赖管理
项目使用 Maven 进行依赖管理，主要依赖包括：
- Spring Boot 3.x
- MyBatis
- 阿里云 OSS SDK
- PDFBox (PDF 处理)
- Thumbnailator (图片缩放)
- Lombok (代码简化)
- JWT (认证)

### 异步支持
使用 `@EnableAsync` 注解启用异步支持，摘要生成等耗时操作在后台异步执行。

### 配置验证
启动时会自动验证 AI 配置是否正确加载，并在控制台输出配置信息。

### 接口设计原则
1. **分离关注点**: 后台管理和小程序使用不同的接口路径
2. **数据适配**: 小程序接口返回简化的数据结构
3. **权限控制**: 后台接口需要管理员权限，小程序接口公开访问
4. **性能优化**: 小程序接口限制分页大小，避免大数据量传输
5. **安全优先**: 使用JWT令牌和注解驱动的权限控制

### 测试账号

开发环境可以使用以下测试账号：
- 用户名: `admin`
- 密码: `admin123`
- 角色: `ADMIN`

**注意**: 生产环境必须修改默认密码和JWT密钥！