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

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_reports_category ON reports(category);
CREATE INDEX IF NOT EXISTS idx_reports_source ON reports(source);
CREATE INDEX IF NOT EXISTS idx_reports_publish_date ON reports(publish_date);
CREATE INDEX IF NOT EXISTS idx_reports_keyword ON reports(title, summary);
CREATE INDEX IF NOT EXISTS idx_report_tags_tag ON report_tags(tag);

-- 后台用户表（简单账号密码，实际生产请使用加盐哈希）
CREATE TABLE IF NOT EXISTS admin_users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    status INTEGER DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);