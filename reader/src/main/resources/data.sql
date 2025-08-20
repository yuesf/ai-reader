-- 插入报告数据
INSERT OR IGNORE INTO reports (id, title, summary, source, category, pages, file_size, publish_date, update_date, thumbnail, download_count, view_count, is_free, price) VALUES
('report_001', '2024-2025年光伏与储能逆变器市场现状调研及前景趋势预测报告', '本报告深入分析了光伏与储能逆变器市场的发展现状，包括市场规模、竞争格局、技术发展趋势等关键信息。', '盛世华研', '综合', 116, 2048, '2024-07-13', '2024-07-13', 'https://example.com/thumbnails/report_001.jpg', 1250, 5600, 0, 9900),
('report_002', '中国新能源汽车产业链深度分析报告', '全面分析中国新能源汽车产业链的发展现状、技术路线、市场前景等关键要素。', '艾瑞咨询', '汽车', 89, 1536, '2024-06-28', '2024-06-28', 'https://example.com/thumbnails/report_002.jpg', 890, 3200, 1, 0),
('report_003', '人工智能在医疗健康领域的应用前景研究', '深入探讨AI技术在医疗诊断、药物研发、健康管理等领域的应用现状和未来发展趋势。', '麦肯锡', '医疗健康', 156, 3072, '2024-07-05', '2024-07-05', 'https://example.com/thumbnails/report_003.jpg', 2100, 7800, 0, 12900),
('report_004', '全球芯片产业发展趋势与投资机会分析', '分析全球芯片产业的发展现状、技术突破、市场格局以及投资机会。', '高盛', '科技', 134, 2560, '2024-06-20', '2024-06-20', 'https://example.com/thumbnails/report_004.jpg', 1560, 4500, 0, 8900),
('report_005', '中国房地产市场2024年下半年展望', '基于当前市场数据和政策环境，分析中国房地产市场下半年的发展趋势和投资机会。', '仲量联行', '房地产', 78, 1280, '2024-07-01', '2024-07-01', 'https://example.com/thumbnails/report_005.jpg', 980, 2800, 1, 0);

-- 插入标签数据
INSERT OR IGNORE INTO report_tags (report_id, tag) VALUES
('report_001', '光伏'),
('report_001', '储能'),
('report_001', '逆变器'),
('report_001', '市场调研'),
('report_002', '新能源汽车'),
('report_002', '产业链'),
('report_002', '技术分析'),
('report_003', '人工智能'),
('report_003', '医疗健康'),
('report_003', 'AI应用'),
('report_004', '芯片'),
('report_004', '半导体'),
('report_004', '投资分析'),
('report_005', '房地产'),
('report_005', '市场展望'),
('report_005', '投资分析');

-- 初始化一个后台管理员账号（用户名：admin 密码：admin123）
INSERT OR IGNORE INTO admin_users (id, username, password, display_name, status) VALUES
(1, 'admin', 'admin123', '管理员', 1);