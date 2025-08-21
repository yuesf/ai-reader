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


