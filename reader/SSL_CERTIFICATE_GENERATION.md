# 本地开发SSL证书生成指南

## 概述

为了在本地开发环境中启用HTTPS，我们需要生成一个自签名SSL证书。本文档将指导您如何生成和配置SSL证书。

## 生成自签名SSL证书

### 方法一：使用keytool命令（推荐）

在终端或命令提示符中执行以下命令：

```bash
keytool -genkeypair -alias yuesf.cn -keyalg RSA -keysize 2048 -storetype JKS -keystore yuesf.cn.jks -validity 3650 -storepass changeit -keypass changeit -dname "CN=yuesf.cn, OU=Local Development, O=AI Reader, L=City, ST=State, C=CN" -ext "SAN=dns:localhost,ip:127.0.0.1"
```

### 方法二：分步生成（适用于需要更多自定义选项的情况）

1. 生成私钥和证书签名请求：
```bash
keytool -genkeypair -alias yuesf.cn -keyalg RSA -keysize 2048 -storetype JKS -keystore yuesf.cn.jks -validity 3650
```

2. 按提示输入以下信息：
   - 密码：changeit
   - 名字和姓氏：yuesf.cn
   - 组织单位：Local Development
   - 组织：AI Reader
   - 城市：City
   - 省份：State
   - 国家代码：CN

### 重要参数说明

- `-alias yuesf.cn`: 证书别名
- `-keyalg RSA`: 使用RSA算法
- `-keysize 2048`: 密钥长度
- `-storetype JKS`: 密钥库类型
- `-keystore yuesf.cn.jks`: 生成的密钥库文件名
- `-validity 3650`: 证书有效期（10年）
- `-storepass changeit`: 密钥库密码
- `-keypass changeit`: 私钥密码
- `-dname`: 证书的专有名称（DN）
- `-ext "SAN=dns:localhost,ip:127.0.0.1"`: 主题备用名称扩展

## 证书文件放置

生成证书后，请将 [yuesf.cn.jks](file:///d:/projects/ai-reader/reader/src/main/resources/yuesf.cn.jks) 文件放置在 `reader/src/main/resources` 目录下。

## 配置说明

当前项目已配置为使用以下SSL设置：

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

如果使用不同的密码或别名，请相应地更新 [application.yml](file:///d:/projects/ai-reader/reader/src/main/resources/application.yml) 文件。

## 浏览器访问

由于是自签名证书，浏览器会显示安全警告。在开发环境中，可以选择忽略警告继续访问。

对于Chrome浏览器，可以访问 `chrome://flags/#allow-insecure-localhost` 并启用 "Allow invalid certificates for resources loaded from localhost" 选项。

## 验证证书

可以使用以下命令验证证书：

```bash
keytool -list -keystore yuesf.cn.jks -storepass changeit
```

## 注意事项

1. 此证书仅用于本地开发测试，不能用于生产环境
2. 证书密码默认为 `changeit`，生产环境中应使用更安全的密码
3. 如果更改了证书信息，请同步更新 [application.yml](file:///d:/projects/ai-reader/reader/src/main/resources/application.yml) 中的配置
4. 请勿将生产证书提交到版本控制系统中