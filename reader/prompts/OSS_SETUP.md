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

## 2. 文件上传接口

### 2.1 上传报告文件
```
POST /v1/upload/report
Content-Type: multipart/form-data

参数：
- file: 要上传的文件
```

### 2.2 上传图片文件
```
POST /v1/upload/image
Content-Type: multipart/form-data

参数：
- file: 要上传的图片文件
```

### 2.3 通用文件上传
```
POST /v1/upload/file
Content-Type: multipart/form-data

参数：
- file: 要上传的文件
- folder: 存储文件夹（可选，默认为"files"）
```

### 2.4 删除文件
```
DELETE /v1/upload/file

参数：
- url: 要删除的文件URL
```

## 3. 创建报告时使用文件上传

### 3.1 上传文件流程
1. 先调用文件上传接口上传文件到OSS
2. 获得文件URL后，在创建报告时传入文件信息

### 3.2 创建报告请求示例
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

## 4. 安全注意事项

### 4.1 AccessKey安全
- 不要将AccessKey硬编码在代码中
- 建议使用环境变量或配置中心
- 定期轮换AccessKey

### 4.2 文件上传安全
- 限制文件大小和类型
- 验证文件内容
- 设置合适的Bucket权限

### 4.3 网络安全
- 使用HTTPS传输
- 配置CORS策略
- 设置防盗链

## 5. 故障排除

### 5.1 常见错误
- `ECONNREFUSED`: 检查网络连接和Endpoint配置
- `AccessDenied`: 检查AccessKey权限和Bucket权限
- `NoSuchBucket`: 检查Bucket名称是否正确

### 5.2 日志查看
查看应用日志中的OSS相关错误信息：
```yaml
logging:
  level:
    com.yuesf.aireader: DEBUG
    com.aliyun.oss: DEBUG
```

## 6. 性能优化

### 6.1 文件上传优化
- 使用分片上传处理大文件
- 启用断点续传
- 配置合适的超时时间

### 6.2 存储优化
- 选择合适的存储类型
- 配置生命周期管理
- 启用CDN加速
