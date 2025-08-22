# 阿里云OSS配置说明

## 1. 配置阿里云OSS

### 1.1 获取OSS配置信息
1. 登录阿里云控制台
2. 进入OSS服务
3. 创建Bucket或使用现有Bucket
4. 获取以下信息：
   - Endpoint（地域节点）
   - AccessKey ID
   - AccessKey Secret
   - Bucket名称

### 1.2 修改配置文件
编辑 `src/main/resources/application.yml` 文件，更新OSS配置：

```yaml
app:
  oss:
    endpoint: https://oss-cn-hangzhou.aliyuncs.com  # 替换为你的地域节点
    access-key-id: your-access-key-id               # 替换为你的AccessKey ID
    access-key-secret: your-access-key-secret       # 替换为你的AccessKey Secret
    bucket-name: your-bucket-name                   # 替换为你的Bucket名称
    upload:
      max-file-size: 100MB                          # 最大文件大小
      allowed-extensions: jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,ppt,pptx  # 允许的文件类型
      base-url: https://your-bucket-name.oss-cn-hangzhou.aliyuncs.com      # 替换为你的Bucket访问域名
```

## 2. 私有存储桶配置

### 2.1 在阿里云OSS控制台设置私有存储桶
1. 登录阿里云控制台
2. 进入OSS服务
3. 创建或选择一个存储桶
4. 在存储桶的"权限管理"中，将"读写权限"设置为"私有"
5. 确保ACL授权策略允许您的AccessKey进行读写操作

### 2.2 配置应用以使用私有存储桶
配置文件保持不变，但需要确保AccessKey具有足够的权限：

```yaml
app:
  oss:
    endpoint: https://oss-cn-hangzhou.aliyuncs.com  # 替换为你的地域节点
    access-key-id: your-access-key-id               # 替换为你的AccessKey ID
    access-key-secret: your-access-key-secret       # 替换为你的AccessKey Secret
    bucket-name: your-private-bucket-name           # 替换为你的私有存储桶名称
    upload:
      max-file-size: 100MB                          # 最大文件大小
      allowed-extensions: jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,ppt,pptx  # 允许的文件类型
      base-url: https://your-private-bucket-name.oss-cn-hangzhou.aliyuncs.com  # 替换为你的存储桶访问域名
```

## 3. 技术实现说明

### 3.1 文件上传时设置ACL权限
在上传文件时，系统会自动将文件ACL设置为私有，通过在ObjectMetadata中设置ACL：

```java
// 设置对象元数据
ObjectMetadata metadata = new ObjectMetadata();
metadata.setContentLength(file.getSize());
// 设置ACL为私有
metadata.setObjectAcl(com.aliyun.oss.model.CannedAccessControlList.Private);

// 上传到OSS
PutObjectRequest putObjectRequest = new PutObjectRequest(
        ossProperties.getBucketName(),
        fileName,
        file.getInputStream(),
        metadata
);
```

### 3.2 存储请求ID
上传文件后，系统会从返回结果中提取请求ID并存储到数据库：

```java
PutObjectResult result = ossClient.putObject(putObjectRequest);
// 存储请求ID
fileInfo.setRequestId(result.getResponse().getRequestId());
```

### 3.3 生成临时访问URL
对于私有存储桶中的文件，需要生成临时访问URL才能访问：

```java
@GetMapping("/reports/file/{id}")
public ApiResponse<String> getReportFileUrl(@PathVariable String id) {
    // ... 获取报告信息 ...
    
    // 生成临时访问URL，有效期1小时
    String presignedUrl = fileUploadService.generatePresignedUrl(objectKey, 3600);
    return ApiResponse.success(presignedUrl);
}
```

## 4. 文件上传接口

### 4.1 上传报告文件
```
POST /v1/upload/report
Content-Type: multipart/form-data

参数：
- file: 要上传的文件
```

### 4.2 上传图片文件
```
POST /v1/upload/image
Content-Type: multipart/form-data

参数：
- file: 要上传的图片文件
```

### 4.3 通用文件上传
```
POST /v1/upload/file
Content-Type: multipart/form-data

参数：
- file: 要上传的文件
- folder: 存储文件夹（可选，默认为"files"）
```

### 4.4 删除文件
```
DELETE /v1/upload/file

参数：
- url: 要删除的文件URL
```

## 5. 使用流程

### 5.1 上传文件
文件上传流程保持不变，系统会自动处理ACL权限设置：

```bash
POST /v1/upload/report/info
Content-Type: multipart/form-data

file: [报告文件]
uploadUserId: user123
```

### 5.2 获取文件临时访问链接
要访问私有存储桶中的文件，需要先获取临时访问链接：

```bash
GET /v1/reports/file/{reportId}
```

响应示例：
```json
{
  "code": 200,
  "message": "success",
  "data": "https://your-private-bucket-name.oss-cn-hangzhou.aliyuncs.com/reports/20241201/abc123.pdf?Expires=1770000000&OSSAccessKeyId=your-access-key-id&Signature=signature"
}
```

### 5.3 使用临时链接访问文件
使用返回的临时链接可以直接下载文件，链接在指定时间内有效（默认1小时）。

## 6. 创建报告时使用文件上传

### 6.1 上传文件流程
1. 先调用文件上传接口上传文件到OSS
2. 获得文件URL后，在创建报告时传入文件信息

### 6.2 创建报告请求示例
```json
{
  "title": "2025 AI行业报告",
  "summary": "AI行业发展趋势分析",
  "source": "艾瑞咨询",
  "category": "行业报告",
  "pages": 50,
  "publishDate": "2025-01-15",
  "thumbnail": "https://your-bucket.oss-cn-hangzhou.aliyuncs.com/images/20250115/abc123.jpg",
  "tags": ["AI", "行业报告"],
  "isFree": true,
  "reportFileUrl": "https://your-bucket.oss-cn-hangzhou.aliyuncs.com/reports/20250115/def456.pdf",
  "reportFileName": "2025_AI_Industry_Report.pdf",
  "reportFileSize": "2.5MB"
}
```

## 7. 数据库结构更新

### 7.1 file_info表结构更新
添加了request_id字段用于存储OSS请求ID：

```sql
ALTER TABLE file_info ADD COLUMN request_id VARCHAR(100);
```

完整的表结构：
```sql
CREATE TABLE IF NOT EXISTS file_info (
    id VARCHAR(50) PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_type VARCHAR(50),
    folder VARCHAR(100),
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    upload_user_id VARCHAR(50),
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, DELETED
    request_id VARCHAR(100)
);
```

## 8. 安全注意事项

### 8.1 AccessKey安全
- 不要将AccessKey硬编码在代码中
- 建议使用环境变量或配置中心
- 定期轮换AccessKey

### 8.2 文件上传安全
- 限制文件大小和类型

### 8.3 访问控制
- 所有文件默认设置为私有，无法直接通过URL访问
- 必须通过应用程序生成临时访问链接
- 临时链接有过期时间，过期后无法访问

### 8.4 AccessKey安全（补充）
- 确保AccessKey具有适当权限（仅授予必要的OSS操作权限）
- 定期轮换AccessKey
- 不要在客户端代码中暴露AccessKey

### 8.5 临时链接安全
- 根据实际需求设置合适的过期时间
- 不要在公开场合分享临时链接
- 对于敏感文件，使用较短的过期时间

## 9. 故障排除

### 9.1 常见错误
- `AccessDenied`: 检查AccessKey权限和存储桶ACL设置
- `RequestTimeTooSkewed`: 检查服务器时间是否正确
- `SignatureDoesNotMatch`: 检查AccessKey是否正确
- `You have no right to access this object because of bucket acl`: 确保在上传时正确设置了ACL权限

### 9.2 日志查看
查看应用日志中的OSS相关错误信息：
```yaml
logging:
  level:
    com.yuesf.aireader: DEBUG
    com.aliyun.oss: DEBUG
```

## 10. 高级配置

### 10.1 自定义过期时间
可以通过修改[ReportController.java](file:///d%3A/projects/ai-reader/reader/src/main/java/com/yuesf/aireader/controller/ReportController.java#L16-L143)中的`generatePresignedUrl`调用参数来调整过期时间：

```java
// 设置过期时间为30分钟 (1800秒)
String presignedUrl = fileUploadService.generatePresignedUrl(objectKey, 1800);
```

### 10.2 权限管理
可以根据业务需求实现更细粒度的权限控制：
- 基于用户角色的访问控制
- 基于文件类型的访问控制
- 基于时间窗口的访问控制