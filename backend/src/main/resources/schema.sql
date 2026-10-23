DROP TABLE IF EXISTS iam_audit_log;
DROP TABLE IF EXISTS iam_login_log;
DROP TABLE IF EXISTS iam_session;
DROP TABLE IF EXISTS iam_role_permission;
DROP TABLE IF EXISTS iam_user_role;
DROP TABLE IF EXISTS iam_permission;
DROP TABLE IF EXISTS iam_role;
DROP TABLE IF EXISTS iam_member;
DROP TABLE IF EXISTS iam_system;
DROP TABLE IF EXISTS iam_user;
DROP TABLE IF EXISTS iam_platform_guard;

CREATE TABLE iam_platform_guard (
  id TINYINT PRIMARY KEY,
  CONSTRAINT ck_guard_id CHECK (id = 1)
);

CREATE TABLE iam_user (
  id BIGINT PRIMARY KEY,
  username VARCHAR(32) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
  platform_admin BOOLEAN NOT NULL DEFAULT FALSE,
  auth_version BIGINT NOT NULL DEFAULT 1,
  failed_attempts INT NOT NULL DEFAULT 0,
  locked_until TIMESTAMP NULL,
  version BIGINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_user_status CHECK (status IN ('ENABLED','DISABLED')),
  CONSTRAINT ck_user_admin CHECK (platform_admin IN (FALSE, TRUE))
);

CREATE TABLE iam_system (
  id BIGINT PRIMARY KEY,
  code VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  description VARCHAR(500) NOT NULL DEFAULT '',
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
  policy_version BIGINT NOT NULL DEFAULT 1,
  version BIGINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_system_status CHECK (status IN ('ENABLED','DISABLED'))
);

CREATE TABLE iam_member (
  sys_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
  version BIGINT NOT NULL DEFAULT 1,
  created_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (sys_id, user_id),
  CONSTRAINT fk_member_system FOREIGN KEY (sys_id) REFERENCES iam_system(id),
  CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES iam_user(id),
  CONSTRAINT ck_member_status CHECK (status IN ('ENABLED','DISABLED'))
);

CREATE TABLE iam_role (
  id BIGINT PRIMARY KEY,
  sys_id BIGINT NOT NULL,
  code VARCHAR(40) NOT NULL,
  name VARCHAR(64) NOT NULL,
  description VARCHAR(500) NOT NULL DEFAULT '',
  builtin BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
  version BIGINT NOT NULL DEFAULT 1,
  created_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (sys_id, code),
  UNIQUE (sys_id, id),
  CONSTRAINT fk_role_system FOREIGN KEY (sys_id) REFERENCES iam_system(id),
  CONSTRAINT ck_role_status CHECK (status IN ('ENABLED','DISABLED'))
);

CREATE TABLE iam_permission (
  id BIGINT PRIMARY KEY,
  sys_id BIGINT NOT NULL,
  code VARCHAR(80) NOT NULL,
  name VARCHAR(64) NOT NULL,
  type VARCHAR(16) NOT NULL,
  parent_id BIGINT NULL,
  route_path VARCHAR(64) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
  version BIGINT NOT NULL DEFAULT 1,
  created_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (sys_id, code),
  UNIQUE (sys_id, id),
  CONSTRAINT fk_permission_system FOREIGN KEY (sys_id) REFERENCES iam_system(id),
  CONSTRAINT ck_permission_type CHECK (type IN ('MENU','ACTION')),
  CONSTRAINT ck_permission_status CHECK (status IN ('ENABLED','DISABLED'))
);

CREATE TABLE iam_user_role (
  sys_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (sys_id, user_id, role_id),
  CONSTRAINT fk_ur_member FOREIGN KEY (sys_id, user_id) REFERENCES iam_member(sys_id, user_id),
  CONSTRAINT fk_ur_role FOREIGN KEY (sys_id, role_id) REFERENCES iam_role(sys_id, id)
);

CREATE TABLE iam_role_permission (
  sys_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  created_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (sys_id, role_id, permission_id),
  CONSTRAINT fk_rp_role FOREIGN KEY (sys_id, role_id) REFERENCES iam_role(sys_id, id),
  CONSTRAINT fk_rp_permission FOREIGN KEY (sys_id, permission_id) REFERENCES iam_permission(sys_id, id)
);

CREATE TABLE iam_session (
  session_id CHAR(36) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  sys_id BIGINT NULL,
  auth_version BIGINT NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  revoked_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES iam_user(id),
  CONSTRAINT fk_session_system FOREIGN KEY (sys_id) REFERENCES iam_system(id)
);

CREATE TABLE iam_audit_log (
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
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE iam_login_log (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NULL,
  username VARCHAR(64) NOT NULL,
  sys_id BIGINT NULL,
  outcome VARCHAR(16) NOT NULL,
  reason VARCHAR(64) NOT NULL,
  ip VARCHAR(45) NULL,
  request_id VARCHAR(80) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
