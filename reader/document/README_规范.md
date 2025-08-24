# AI-Reader 代码规范

## 1. 编码规范

### 1.1 命名规范

#### 1.1.1 包命名
- 包名全部小写，使用点分隔符分隔各个部分
- 包名应简洁明了，能够准确反映包的功能
- 例如：`com.yuesf.aireader.controller`

#### 1.1.2 类命名
- 类名使用大驼峰命名法（Pascal Case）
- 类名应该是名词或名词短语
- 例如：`ReportController`, `FileUploadService`

#### 1.1.3 接口命名
- 接口名使用大驼峰命名法
- 接口名应该体现其能力或契约
- 例如：`ReportService`

#### 1.1.4 方法命名
- 方法名使用小驼峰命名法（Camel Case）
- 方法名应该是动词或动词短语
- 例如：`getReportList`, `createReport`

#### 1.1.5 变量命名
- 变量名使用小驼峰命名法
- 变量名应简洁明了，能够准确表达变量的用途
- 例如：`reportId`, `fileName`

#### 1.1.6 常量命名
- 常量名全部大写，单词间用下划线分隔
- 例如：`MAX_FILE_SIZE`, `DEFAULT_PAGE_SIZE`

### 1.2 代码格式

#### 1.2.1 缩进
- 使用4个空格进行缩进，不使用Tab字符

#### 1.2.2 行宽
- 每行代码不超过120个字符

#### 1.2.3 空行
- 类成员之间用空行分隔
- 方法之间用空行分隔
- 逻辑代码块之间可用空行分隔

#### 1.2.4 括号
- 左大括号不换行，与语句在同一行
- 右大括号独占一行

```java
public class ReportService {
    public List<Report> getReports() {
        if (condition) {
            // do something
        }
    }
}
```

### 1.3 注释规范

#### 1.3.1 类注释
- 每个类都需要有类注释
- 类注释使用`/** */`格式
- 包含类的功能描述、作者、创建时间等信息

```java
/**
 * 报告服务类
 * 提供报告相关的业务逻辑处理
 *
 * @author yourname
 * @since 2025-01-01
 */
@Service
public class ReportService {
    // ...
}
```

#### 1.3.2 方法注释
- 每个public方法都需要有方法注释
- 方法注释使用`/** */`格式
- 包含方法功能描述、参数说明、返回值说明、异常说明

```java
/**
 * 获取报告列表
 *
 * @param request 查询条件
 * @return 报告列表
 * @throws BusinessException 业务异常
 */
public ReportListResponse getReports(ReportListRequest request) throws BusinessException {
    // ...
}
```

#### 1.3.3 字段注释
- public字段需要有注释
- 注释使用`/** */`或`//`格式

#### 1.3.4 行内注释
- 行内注释使用`//`格式
- 注释应解释代码的意图，而不是描述代码在做什么

## 2. 项目结构规范

### 2.1 分层架构
项目采用标准的分层架构：
- controller层：负责处理HTTP请求
- service层：负责业务逻辑处理
- mapper层：负责数据访问
- entity层：负责数据实体映射
- dto层：负责数据传输对象

### 2.2 包结构
```
com.yuesf.aireader
├── annotation      # 自定义注解
├── config          # 配置类
├── controller      # 控制器层
├── dto             # 数据传输对象
├── entity          # 实体类
├── exception       # 异常处理
├── interceptor     # 拦截器
├── mapper          # 数据访问层
├── service         # 业务逻辑层
├── util            # 工具类
└── vo              # 视图对象
```

## 3. 编程实践

### 3.1 异常处理
- 使用自定义异常类处理业务异常
- 不捕获不处理的异常
- 异常信息应清晰明确，便于问题定位

### 3.2 日志记录
- 使用SLF4J进行日志记录
- 日志级别应合理使用（ERROR, WARN, INFO, DEBUG）
- 日志信息应包含足够的上下文信息

```java
private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

logger.info("获取报告列表，查询条件: {}", request);
```

### 3.3 参数校验
- 使用Spring Boot的验证框架进行参数校验
- 在controller层进行基础参数校验
- 在service层进行业务逻辑校验

### 3.4 事务管理
- 合理使用`@Transactional`注解管理事务
- 避免在事务中执行长时间操作
- 注意事务的传播行为和隔离级别

## 4. 数据库规范

### 4.1 表命名
- 表名使用下划线分隔的小写字母
- 表名应能准确反映表的用途
- 例如：`reports`, `file_info`

### 4.2 字段命名
- 字段名使用下划线分隔的小写字母
- 字段名应简洁明了，能够准确表达字段的含义
- 例如：`report_id`, `file_name`

### 4.3 SQL编写
- SQL语句应格式化，关键字大写
- 复杂SQL应添加注释说明
- 避免使用SELECT *，应明确指定字段

```sql
<!-- 查询报告列表 -->
<select id="selectReports" parameterType="map" resultType="Report">
    SELECT 
        id,
        title,
        summary
    FROM reports
    WHERE status = 'ACTIVE'
    <if test="keyword != null and keyword != ''">
        AND (title LIKE CONCAT('%', #{keyword}, '%') 
             OR summary LIKE CONCAT('%', #{keyword}, '%'))
    </if>
    ORDER BY publish_date DESC
    LIMIT #{pageSize} OFFSET #{offset}
</select>
```

## 5. 接口规范

### 5.1 RESTful设计
- 使用HTTP动词表示操作类型
- 使用名词表示资源
- URI应简洁明了，能够准确表达资源

### 5.2 返回格式
- 所有接口返回统一的[ApiResponse](file:///d:/projects/ai-reader/reader/src/main/java/com/yuesf/aireader/dto/ApiResponse.java#L11-L65)包装格式
- 正确使用HTTP状态码
- 错误信息应清晰明确

### 5.3 版本控制
- 接口应包含版本信息
- 版本号使用`v1`, `v2`格式
- 例如：`/v1/reports`

## 6. Git提交规范

### 6.1 提交信息格式
```
<type>(<scope>): <subject>

<body>

<footer>
```

### 6.2 提交类型
- feat：新功能
- fix：修复bug
- docs：文档更新
- style：代码格式调整
- refactor：代码重构
- test：测试相关
- chore：构建过程或辅助工具的变动

### 6.3 提交示例
```
feat(report): 添加报告搜索功能

实现报告的关键词搜索功能，支持按标题和摘要搜索

Closes #123
```

## 7. 测试规范

### 7.1 单元测试
- 每个service类应有对应的单元测试
- 使用JUnit 5作为测试框架
- 测试覆盖率应达到80%以上

### 7.2 集成测试
- 对重要的业务流程进行集成测试
- 使用@SpringBootTest注解进行集成测试

### 7.3 测试命名
- 测试方法使用`should_xxx_when_xxx`格式命名
- 测试类与被测试类一一对应，名称加Test后缀

```java
class ReportServiceTest {
    
    @Test
    void should_return_reports_when_query_with_keyword() {
        // given
        
        // when
        
        // then
    }
}
```