-- 仅用于F模块独立演示/测试；生产环境复用database/001_rbac0.sql。
-- H2用CLOB承载JSON文本，不能据此声称MySQL JSON/外键已验证。
CREATE TABLE IF NOT EXISTS iam_audit_log (
  id BIGINT PRIMARY KEY,
  sys_id BIGINT NULL,
  actor_id BIGINT NULL,
  actor_name VARCHAR(64) NOT NULL,
  action VARCHAR(80) NOT NULL,
  target_type VARCHAR(40) NOT NULL,
  target_id VARCHAR(80) NOT NULL,
  before_json CLOB NULL,
  after_json CLOB NULL,
  request_id VARCHAR(80) NOT NULL,
  ip VARCHAR(45) NULL,
  created_at TIMESTAMP(3) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_audit_system_time ON iam_audit_log(sys_id,created_at,id);
CREATE INDEX IF NOT EXISTS idx_audit_actor ON iam_audit_log(sys_id,actor_id,created_at,id);
CREATE TABLE IF NOT EXISTS iam_login_log (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NULL,
  username VARCHAR(64) NOT NULL,
  sys_id BIGINT NULL,
  outcome VARCHAR(16) NOT NULL CHECK(outcome IN ('SUCCESS','DENIED')),
  reason VARCHAR(64) NOT NULL,
  ip VARCHAR(45) NULL,
  request_id VARCHAR(80) NOT NULL,
  created_at TIMESTAMP(3) NOT NULL
);
