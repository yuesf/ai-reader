# 阿里云OSS私有存储桶配置与使用说明

## 1. 私有存储桶配置

### 1.1 在阿里云OSS控制台设置私有存储桶
1. 登录阿里云控制台
2. 进入OSS服务
3. 创建或选择一个存储桶
4. 在存储桶的"权限管理"中，将"读写权限"设置为"私有"
5. 确保ACL授权策略允许您的AccessKey进行读写操作

### 1.2 配置应用以使用私有存储桶
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

## 2. 技术实现说明

### 2.1 文件上传时设置ACL权限
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

### 2.2 存储请求ID
上传文件后，系统会从返回结果中提取请求ID并存储到数据库：

```java
PutObjectResult result = ossClient.putObject(putObjectRequest);
// 存储请求ID
fileInfo.setRequestId(result.getResponse().getRequestId());
```

### 2.3 生成临时访问URL
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

## 3. 使用流程

### 3.1 上传文件
文件上传流程保持不变，系统会自动处理ACL权限设置：

```bash
POST /v1/upload/report/info
Content-Type: multipart/form-data

file: [报告文件]
uploadUserId: user123
```

### 3.2 获取文件临时访问链接
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

### 3.3 使用临时链接访问文件
使用返回的临时链接可以直接下载文件，链接在指定时间内有效（默认1小时）。

## 4. 数据库结构更新

### 4.1 file_info表结构更新
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

## 5. 安全注意事项

### 5.1 访问控制
- 所有文件默认设置为私有，无法直接通过URL访问
- 必须通过应用程序生成临时访问链接
- 临时链接有过期时间，过期后无法访问

### 5.2 AccessKey安全
- 确保AccessKey具有适当权限（仅授予必要的OSS操作权限）
- 定期轮换AccessKey
- 不要在客户端代码中暴露AccessKey

### 5.3 临时链接安全
- 根据实际需求设置合适的过期时间
- 不要在公开场合分享临时链接
- 对于敏感文件，使用较短的过期时间

## 6. 故障排除

### 6.1 常见错误
- `AccessDenied`: 检查AccessKey权限和存储桶ACL设置
- `RequestTimeTooSkewed`: 检查服务器时间是否正确
- `SignatureDoesNotMatch`: 检查AccessKey是否正确
- `You have no right to access this object because of bucket acl`: 确保在上传时正确设置了ACL权限

### 6.2 日志查看
查看应用日志中的OSS相关错误信息：
```yaml
logging:
  level:
    com.yuesf.aireader: DEBUG
    com.aliyun.oss: DEBUG
```

## 7. 高级配置

### 7.1 自定义过期时间
可以通过修改[ReportController.java](file:///d%3A/projects/ai-reader/reader/src/main/java/com/yuesf/aireader/controller/ReportController.java#L16-L143)中的`generatePresignedUrl`调用参数来调整过期时间：

```java
// 设置过期时间为30分钟 (1800秒)
String presignedUrl = fileUploadService.generatePresignedUrl(objectKey, 1800);
```

### 7.2 权限管理
可以根据业务需求实现更细粒度的权限控制：
- 基于用户角色的访问控制
- 基于文件类型的访问控制
- 基于时间窗口的访问控制