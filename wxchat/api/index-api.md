# 报告查询接口API文档

## 接口概述
提供报告查询和列表获取的RESTful API接口

## 基础信息
- 基础URL: `https://wx.example.com/v1`
- 请求方式: GET/POST
- 数据格式: JSON
- 字符编码: UTF-8

## 接口列表

### 1. 获取/搜索报告列表

#### 接口地址
`POST /reports`

#### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | number | 否 | 页码，默认1 |
| pageSize | number | 否 | 每页数量，默认10，最大50 |
| keyword | string | 否 | 搜索关键词 |
| category | string | 否 | 报告分类 |
| source | string | 否 | 报告来源 |
| startDate | string | 否 | 开始日期，格式：YYYY-MM-DD |
| endDate | string | 否 | 结束日期，格式：YYYY-MM-DD |
| filters | object | 否 | 高级过滤条件 |
| filters.category | array | 否 | 分类过滤（支持多选） |
| filters.source | array | 否 | 来源过滤（支持多选） |
| filters.dateRange | object | 否 | 日期范围过滤 |
| filters.dateRange.start | string | 否 | 开始日期 |
| filters.dateRange.end | string | 否 | 结束日期 |
| sortBy | string | 否 | 排序字段（publishDate, updateDate, downloadCount, viewCount等） |
| sortOrder | string | 否 | 排序方向(asc/desc)，默认desc |

#### 请求示例

**基础查询**
```json
{
  "page": 1,
  "pageSize": 10,
  "keyword": "光伏",
  "category": "综合"
}
```

**高级搜索**
```json
{
  "page": 1,
  "pageSize": 20,
  "keyword": "光伏储能",
  "filters": {
    "category": ["综合", "新能源"],
    "source": ["盛世华研", "其他机构"],
    "dateRange": {
      "start": "2024-01-01",
      "end": "2024-12-31"
    }
  },
  "sortBy": "publishDate",
  "sortOrder": "desc"
}
```

**仅获取列表（无搜索条件）**
```json
{
  "page": 1,
  "pageSize": 15
}
```

#### 响应参数
| 参数名 | 类型 | 说明 |
|--------|------|------|
| code | number | 响应状态码，200表示成功 |
| message | string | 响应消息 |
| data | object | 响应数据 |
| data.total | number | 总记录数 |
| data.page | number | 当前页码 |
| data.pageSize | number | 每页数量 |
| data.list | array | 报告列表 |

#### 报告对象结构
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | string | 报告唯一标识 |
| title | string | 报告标题 |
| summary | string | 报告摘要 |
| source | string | 报告来源 |
| category | string | 报告分类 |
| pages | number | 报告页数 |
| fileSize | number | 文件大小(KB) |
| publishDate | string | 发布日期 |
| updateDate | string | 更新日期 |
| thumbnail | string | 缩略图URL |
| tags | array | 标签列表 |
| downloadCount | number | 下载次数 |
| viewCount | number | 浏览次数 |
| isFree | boolean | 是否免费 |
| price | number | 价格(分) |

#### 成功响应示例
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 156,
    "page": 1,
    "pageSize": 10,
    "list": [
      {
        "id": "report_001",
        "title": "2024-2025年光伏与储能逆变器市场现状调研及前景趋势预测报告",
        "summary": "本报告深入分析了光伏与储能逆变器市场的发展现状...",
        "source": "盛世华研",
        "category": "综合",
        "pages": 116,
        "fileSize": 2048,
        "publishDate": "2024-07-13",
        "updateDate": "2024-07-13",
        "thumbnail": "https://example.com/thumbnails/report_001.jpg",
        "tags": ["光伏", "储能", "逆变器", "市场调研"],
        "downloadCount": 1250,
        "viewCount": 5600,
        "isFree": false,
        "price": 9900
      }
    ]
  }
}
```

#### 错误响应示例
```json
{
  "code": 400,
  "message": "参数错误",
  "data": null
}
```

### 2. 获取报告详情

#### 接口地址
`GET /reports/{id}`

#### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | string | 是 | 报告ID |

#### 响应格式
返回单个报告对象的详细信息

## 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 注意事项

1. 所有日期格式统一使用ISO 8601标准：YYYY-MM-DD
2. 价格单位为分，避免浮点数精度问题
3. 文件大小单位为KB
4. 分页参数page从1开始计数
5. 搜索接口支持模糊匹配和精确匹配
6. 支持GET和POST两种请求方式，POST方式支持更复杂的查询条件
7. 建议实现接口缓存机制，提升性能
8. 当不提供keyword时，返回所有报告列表；提供keyword时，进行关键词搜索
