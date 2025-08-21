-- 报告标签表
CREATE TABLE IF NOT EXISTS report_tags (
    report_id VARCHAR(50) NOT NULL,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (report_id, tag),
    FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_report_tags_tag ON report_tags(tag);


