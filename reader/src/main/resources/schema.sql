-- 报告表
CREATE TABLE IF NOT EXISTS reports (
    id VARCHAR(50) PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    summary TEXT,
    source VARCHAR(100),
    category VARCHAR(50),
    pages INTEGER,
    file_size BIGINT,
    publish_date DATE,
    update_date DATE,
    thumbnail VARCHAR(500),
    download_count INTEGER DEFAULT 0,
    view_count INTEGER DEFAULT 0,
    is_free BOOLEAN DEFAULT FALSE,
    price INTEGER DEFAULT 0,
    report_file_id VARCHAR(50),
    report_file_url VARCHAR(500),
    report_file_name VARCHAR(255),
    report_file_size VARCHAR(50)
);

-- 报告标签表
CREATE TABLE IF NOT EXISTS report_tags (
    report_id VARCHAR(50) NOT NULL,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (report_id, tag),
    FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
);

-- 文件信息表
CREATE TABLE IF NOT EXISTS file_info (
    id VARCHAR(50) PRIMARY KEY,
    file_name VARCHAR(500) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50),
    folder VARCHAR(100),
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    upload_user_id VARCHAR(50),
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, DELETED
    request_id VARCHAR(100),
    page_nums INTEGER DEFAULT 0
);

-- 后台用户表（简单账号密码，实际生产请使用加盐哈希）
CREATE TABLE IF NOT EXISTS admin_users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    status INTEGER DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 微信小程序用户表
CREATE TABLE IF NOT EXISTS wechat_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    open_id VARCHAR(100) UNIQUE NOT NULL,
    union_id VARCHAR(100),
    session_key VARCHAR(100),
    nick_name VARCHAR(100),
    avatar_url VARCHAR(500),
    gender INTEGER DEFAULT 0, -- 0:未知, 1:男, 2:女
    country VARCHAR(50),
    province VARCHAR(50),
    city VARCHAR(50),
    language VARCHAR(20),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_login_time DATETIME,
    status INTEGER DEFAULT 1 -- 1:正常, 0:禁用
);
