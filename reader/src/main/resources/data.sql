-- 初始化一个后台管理员账号（用户名：admin 密码：admin123）
INSERT OR IGNORE INTO admin_users (id, username, password, display_name, status) VALUES
(1, 'admin', 'admin123', '管理员', 1);