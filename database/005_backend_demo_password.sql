-- 第一次检查演示账号口令：DemoOnly12345
-- 仅用于本地/演示数据库；生产环境必须重新生成随机口令并更换哈希。
-- 需在 001_rbac0.sql、003_demo_seed.sql 之后执行。
SET NAMES utf8mb4;
SET time_zone = '+00:00';

UPDATE iam_user
   SET password_hash = 'pbkdf2$120000$O34Pel0sShucjX5vWks8LQ==$UMqWYzYkdeldgefmxw0H0KbJsRztCSI5EqHicqxLcGE='
 WHERE username IN ('platform_admin','system_admin','auditor','chenyu','lichen');

COMMIT;
