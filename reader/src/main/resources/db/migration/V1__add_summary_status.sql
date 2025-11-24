-- 为现有的reports表添加summary_status字段
-- 执行时间: 2025-01-23

-- 添加summary_status字段（如果不存在）
ALTER TABLE reports ADD COLUMN IF NOT EXISTS summary_status VARCHAR(20) DEFAULT 'NONE';

-- 为已有数据设置默认状态
-- 如果已有摘要，设置为COMPLETED；否则设置为NONE
UPDATE reports 
SET summary_status = CASE 
    WHEN summary IS NOT NULL AND summary != '' THEN 'COMPLETED'
    ELSE 'NONE'
END
WHERE summary_status IS NULL OR summary_status = 'NONE';

-- 创建索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_reports_summary_status ON reports(summary_status);
