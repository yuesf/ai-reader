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
    request_id VARCHAR(100)
);
