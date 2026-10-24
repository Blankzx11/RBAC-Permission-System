-- 在全新测试库执行001、003后执行本文件。全部为只读核对，不修改授权数据。
-- 期望：表数11；账号5；系统2；成员8；角色8；权限36；UA6；PA54；审计1。
SELECT COUNT(*) AS actual_tables, 11 AS expected_tables
FROM information_schema.tables
WHERE table_schema = DATABASE() AND LEFT(table_name,4) = 'iam_';

SELECT 'users' AS item, COUNT(*) AS actual, 5 AS expected FROM iam_user
UNION ALL SELECT 'systems', COUNT(*), 2 FROM iam_system
UNION ALL SELECT 'members', COUNT(*), 8 FROM iam_member
UNION ALL SELECT 'roles', COUNT(*), 8 FROM iam_role
UNION ALL SELECT 'permissions', COUNT(*), 36 FROM iam_permission
UNION ALL SELECT 'user_roles', COUNT(*), 6 FROM iam_user_role
UNION ALL SELECT 'role_permissions', COUNT(*), 54 FROM iam_role_permission
UNION ALL SELECT 'audit_logs', COUNT(*), 1 FROM iam_audit_log
UNION ALL SELECT 'guard', COUNT(*), 1 FROM iam_platform_guard
UNION ALL SELECT 'sessions', COUNT(*), 0 FROM iam_session
UNION ALL SELECT 'login_logs', COUNT(*), 0 FROM iam_login_log;

-- 以下查询都应返回0行。
SELECT ur.* FROM iam_user_role ur
LEFT JOIN iam_member m ON m.sys_id=ur.sys_id AND m.user_id=ur.user_id
LEFT JOIN iam_role r ON r.sys_id=ur.sys_id AND r.id=ur.role_id
WHERE m.user_id IS NULL OR r.id IS NULL;

SELECT rp.* FROM iam_role_permission rp
LEFT JOIN iam_role r ON r.sys_id=rp.sys_id AND r.id=rp.role_id
LEFT JOIN iam_permission p ON p.sys_id=rp.sys_id AND p.id=rp.permission_id
WHERE r.id IS NULL OR p.id IS NULL;

SELECT child.* FROM iam_permission child
LEFT JOIN iam_permission parent ON parent.sys_id=child.sys_id AND parent.id=child.parent_id
WHERE child.parent_id IS NOT NULL AND (parent.id IS NULL OR parent.type<>'MENU');

-- 陈雨在系统1应有menu:members与sys:member:read两项有效直接权限。
SELECT DISTINCT p.code FROM iam_user u
JOIN iam_member m ON m.user_id=u.id AND m.sys_id=1 AND m.status='ENABLED'
JOIN iam_system s ON s.id=m.sys_id AND s.status='ENABLED'
JOIN iam_user_role ur ON ur.sys_id=m.sys_id AND ur.user_id=m.user_id
JOIN iam_role r ON r.sys_id=ur.sys_id AND r.id=ur.role_id AND r.status='ENABLED'
JOIN iam_role_permission rp ON rp.sys_id=r.sys_id AND rp.role_id=r.id
JOIN iam_permission p ON p.sys_id=rp.sys_id AND p.id=rp.permission_id AND p.status='ENABLED'
WHERE u.id=4 AND u.status='ENABLED' ORDER BY p.code;
