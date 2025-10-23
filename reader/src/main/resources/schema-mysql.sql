-- MySQL 5.7 schema (InnoDB + utf8mb4)
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

-- 报告表
CREATE TABLE IF NOT EXISTS reports (
    id VARCHAR(50) PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    summary TEXT,
    source VARCHAR(100),
    category VARCHAR(50),
    pages INT,
    file_size BIGINT,
    publish_date DATE,
    update_date DATE,
    thumbnail VARCHAR(500),
    download_count INT DEFAULT 0,
    view_count INT DEFAULT 0,
    is_free TINYINT(1) DEFAULT 0,
    price INT DEFAULT 0,
    report_file_id VARCHAR(50),
    report_file_url VARCHAR(500),
    report_file_name VARCHAR(255),
    report_file_size VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报告表';

-- 报告标签表
CREATE TABLE IF NOT EXISTS report_tags (
    report_id VARCHAR(50) NOT NULL,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (report_id, tag),
    CONSTRAINT fk_report_tags_report
      FOREIGN KEY (report_id) REFERENCES reports(id)
      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报告标签表';

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
    status VARCHAR(20) DEFAULT 'ACTIVE',
    request_id VARCHAR(100),
    page_nums INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件信息表';

-- 后台用户表
CREATE TABLE IF NOT EXISTS admin_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    status TINYINT(1) DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台用户表';

-- 微信小程序用户表
CREATE TABLE IF NOT EXISTS wechat_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    open_id VARCHAR(100) UNIQUE NOT NULL,
    union_id VARCHAR(100),
    session_key VARCHAR(100),
    nick_name VARCHAR(100),
    avatar_url VARCHAR(500),
    gender TINYINT(1) DEFAULT 0,
    country VARCHAR(50),
    province VARCHAR(50),
    city VARCHAR(50),
    language VARCHAR(20),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_time DATETIME,
    status TINYINT(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微信小程序用户表';

-- ========== 埋点数据表 ==========

-- 用户会话表
CREATE TABLE IF NOT EXISTS user_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) UNIQUE NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    start_time BIGINT NOT NULL,
    end_time BIGINT,
    page_count INT DEFAULT 0,
    event_count INT DEFAULT 0,
    duration INT DEFAULT 0,
    device_info TEXT,
    network_type VARCHAR(16),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户会话表';

-- 埋点事件表
CREATE TABLE IF NOT EXISTS tracking_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    page_path VARCHAR(128) NOT NULL,
    element_id VARCHAR(64),
    element_text VARCHAR(128),
    properties TEXT,
    timestamp BIGINT NOT NULL,
    device_info TEXT,
    network_type VARCHAR(16),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='埋点事件表';

-- 索引（MySQL 5.7 不支持 IF NOT EXISTS；默认空库初始化不重复执行）
CREATE INDEX idx_tracking_events_user_id ON tracking_events(user_id);
CREATE INDEX idx_tracking_events_timestamp ON tracking_events(timestamp);
CREATE INDEX idx_tracking_events_page_path ON tracking_events(page_path);
CREATE INDEX idx_tracking_events_event_type ON tracking_events(event_type);
CREATE INDEX idx_tracking_events_session_id ON tracking_events(session_id);

CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_session_id ON user_sessions(session_id);
CREATE INDEX idx_user_sessions_start_time ON user_sessions(start_time);

-- 复合索引
CREATE INDEX idx_tracking_events_user_time ON tracking_events(user_id, timestamp);
CREATE INDEX idx_tracking_events_page_time ON tracking_events(page_path, timestamp);

SET FOREIGN_KEY_CHECKS=1;