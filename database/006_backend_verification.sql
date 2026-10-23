-- 后端第一次检查只读核对；执行 001、003、005 后运行。
SET NAMES utf8mb4;
SET time_zone = '+00:00';

SELECT 'table_count' AS check_name, COUNT(*) AS actual, 11 AS expected
  FROM information_schema.tables
 WHERE table_schema = DATABASE()
   AND table_name LIKE 'iam_%';

SELECT 'user_count' AS check_name, COUNT(*) AS actual, 5 AS expected FROM iam_user;
SELECT 'system_count' AS check_name, COUNT(*) AS actual, 2 AS expected FROM iam_system;
SELECT 'role_count' AS check_name, COUNT(*) AS actual, 8 AS expected FROM iam_role;
SELECT 'permission_count' AS check_name, COUNT(*) AS actual, 36 AS expected FROM iam_permission;
SELECT 'user_role_count' AS check_name, COUNT(*) AS actual, 6 AS expected FROM iam_user_role;
SELECT 'role_permission_count' AS check_name, COUNT(*) AS actual, 54 AS expected FROM iam_role_permission;

-- 陈雨在系统 A：应只有 menu:members、sys:member:read。
SELECT p.sys_id, p.code
  FROM iam_member m
  JOIN iam_user u ON u.id = m.user_id AND u.status = 'ENABLED'
  JOIN iam_user_role ur ON ur.sys_id = m.sys_id AND ur.user_id = m.user_id
  JOIN iam_role r ON r.sys_id = ur.sys_id AND r.id = ur.role_id AND r.status = 'ENABLED'
  JOIN iam_role_permission rp ON rp.sys_id = r.sys_id AND rp.role_id = r.id
  JOIN iam_permission p ON p.sys_id = rp.sys_id AND p.id = rp.permission_id AND p.status = 'ENABLED'
 WHERE m.sys_id = 1 AND m.user_id = 4 AND m.status = 'ENABLED'
 ORDER BY p.code;

-- 同一账号在系统 B：编码相同，但必须来自系统 B 的角色与权限行。
SELECT p.sys_id, p.code
  FROM iam_member m
  JOIN iam_user u ON u.id = m.user_id AND u.status = 'ENABLED'
  JOIN iam_user_role ur ON ur.sys_id = m.sys_id AND ur.user_id = m.user_id
  JOIN iam_role r ON r.sys_id = ur.sys_id AND r.id = ur.role_id AND r.status = 'ENABLED'
  JOIN iam_role_permission rp ON rp.sys_id = r.sys_id AND rp.role_id = r.id
  JOIN iam_permission p ON p.sys_id = rp.sys_id AND p.id = rp.permission_id AND p.status = 'ENABLED'
 WHERE m.sys_id = 2 AND m.user_id = 4 AND m.status = 'ENABLED'
 ORDER BY p.code;
