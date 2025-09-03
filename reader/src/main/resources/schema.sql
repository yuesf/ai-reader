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

-- ========== 埋点数据表 ==========

-- 用户会话表
CREATE TABLE IF NOT EXISTS user_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id VARCHAR(64) UNIQUE NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    start_time BIGINT NOT NULL,
    end_time BIGINT,
    page_count INTEGER DEFAULT 0,
    event_count INTEGER DEFAULT 0,
    duration INTEGER DEFAULT 0, -- 会话时长(秒)
    device_info TEXT, -- JSON格式存储设备信息
    network_type VARCHAR(16),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 埋点事件表
CREATE TABLE IF NOT EXISTS tracking_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(32) NOT NULL, -- button_click, page_view, etc.
    page_path VARCHAR(128) NOT NULL, -- /pages/index/index
    element_id VARCHAR(64), -- search_btn, download_btn
    element_text VARCHAR(128), -- "搜索", "下载"
    properties TEXT, -- JSON格式存储自定义属性
    timestamp BIGINT NOT NULL,
    device_info TEXT, -- JSON格式存储设备信息
    network_type VARCHAR(16),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    -- 外键关联
    FOREIGN KEY (session_id) REFERENCES user_sessions(session_id)
);

-- 创建索引优化查询性能
CREATE INDEX IF NOT EXISTS idx_tracking_events_user_id ON tracking_events(user_id);
CREATE INDEX IF NOT EXISTS idx_tracking_events_timestamp ON tracking_events(timestamp);
CREATE INDEX IF NOT EXISTS idx_tracking_events_page_path ON tracking_events(page_path);
CREATE INDEX IF NOT EXISTS idx_tracking_events_event_type ON tracking_events(event_type);
CREATE INDEX IF NOT EXISTS idx_tracking_events_session_id ON tracking_events(session_id);

CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_session_id ON user_sessions(session_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_start_time ON user_sessions(start_time);

-- 创建复合索引支持常用查询
CREATE INDEX IF NOT EXISTS idx_tracking_events_user_time ON tracking_events(user_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_tracking_events_page_time ON tracking_events(page_path, timestamp);
