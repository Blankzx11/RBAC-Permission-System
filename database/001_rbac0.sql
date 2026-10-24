-- 多系统 RBAC0 新库结构；MySQL >=8.0.16；不含 DROP，不对已有库自动覆盖。
-- 应用连接须 SET time_zone = '+00:00'；BIGINT ID 由应用生成，JSON 输出为字符串。
SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE iam_platform_guard (
  id TINYINT NOT NULL COMMENT '固定单行1，平台写入互斥锁',
  PRIMARY KEY (id),
  CONSTRAINT ck_guard_id CHECK (id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO iam_platform_guard(id) VALUES (1);

CREATE TABLE iam_user (
  id BIGINT NOT NULL COMMENT '全局用户ID',
  username VARCHAR(32) COLLATE utf8mb4_bin NOT NULL COMMENT '全局唯一小写用户名，不可改',
  password_hash VARCHAR(255) NOT NULL COMMENT '自适应密码哈希，禁止API及日志输出',
  display_name VARCHAR(64) NOT NULL COMMENT '全局显示名',
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED或DISABLED',
  platform_admin BOOLEAN NOT NULL DEFAULT FALSE COMMENT '仅平台接口可维护',
  auth_version BIGINT NOT NULL DEFAULT 1 COMMENT '全局安全版本，改密启停递增',
  failed_attempts INT NOT NULL DEFAULT 0 COMMENT '连续失败次数',
  locked_until DATETIME(3) NULL COMMENT '临时登录锁定到期时间UTC',
  version BIGINT NOT NULL DEFAULT 1 COMMENT '根资源并发版本',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间UTC',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间UTC',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_username (username),
  KEY idx_user_status (status,id),
  CONSTRAINT ck_user_admin CHECK (platform_admin IN (0,1)),
  CONSTRAINT ck_user_status CHECK (status IN ('ENABLED','DISABLED')),
  CONSTRAINT ck_user_versions CHECK (auth_version > 0 AND version > 0 AND failed_attempts >= 0),
  CONSTRAINT ck_user_id CHECK (id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE iam_system (
  id BIGINT NOT NULL COMMENT '子系统ID',
  code VARCHAR(40) COLLATE utf8mb4_bin NOT NULL COMMENT '全局唯一系统编码，不可改',
  name VARCHAR(64) NOT NULL COMMENT '系统名称',
  description VARCHAR(500) NOT NULL DEFAULT '' COMMENT '用途描述',
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '系统启用状态',
  policy_version BIGINT NOT NULL DEFAULT 1 COMMENT '本系统策略版本，权限事实变化递增',
  version BIGINT NOT NULL DEFAULT 1 COMMENT '系统属性并发版本',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间UTC',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间UTC',
  PRIMARY KEY (id),
  UNIQUE KEY uk_system_code (code),
  CONSTRAINT ck_system_status CHECK (status IN ('ENABLED','DISABLED')),
  CONSTRAINT ck_system_versions CHECK (policy_version > 0 AND version > 0),
  CONSTRAINT ck_system_id CHECK (id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE iam_member (
  sys_id BIGINT NOT NULL COMMENT '所属子系统',
  user_id BIGINT NOT NULL COMMENT '全局账号',
  display_name VARCHAR(64) NOT NULL COMMENT '本系统内显示名',
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '本地成员启用状态',
  version BIGINT NOT NULL DEFAULT 1 COMMENT '成员属性和角色集合共享版本',
  created_by BIGINT NULL COMMENT '操作者快照ID，初始化可空',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '加入时间UTC',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间UTC',
  PRIMARY KEY (sys_id,user_id),
  KEY idx_member_user (user_id,sys_id),
  KEY idx_member_status (sys_id,status,user_id),
  CONSTRAINT fk_member_system FOREIGN KEY (sys_id) REFERENCES iam_system(id),
  CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES iam_user(id),
  CONSTRAINT ck_member_status CHECK (status IN ('ENABLED','DISABLED')),
  CONSTRAINT ck_member_version CHECK (version > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE iam_role (
  id BIGINT NOT NULL COMMENT '角色ID',
  sys_id BIGINT NOT NULL COMMENT '所属子系统',
  code VARCHAR(40) COLLATE utf8mb4_bin NOT NULL COMMENT '系统内唯一角色编码，不可改',
  name VARCHAR(64) NOT NULL COMMENT '角色名称',
  description VARCHAR(500) NOT NULL DEFAULT '' COMMENT '角色说明',
  builtin BOOLEAN NOT NULL DEFAULT FALSE COMMENT '内置受保护角色标记',
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '启用状态',
  version BIGINT NOT NULL DEFAULT 1 COMMENT '角色属性和权限等子资源共享版本',
  created_by BIGINT NULL COMMENT '操作者快照ID',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间UTC',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间UTC',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_scope_id (sys_id,id),
  UNIQUE KEY uk_role_code (sys_id,code),
  KEY idx_role_status (sys_id,status,id),
  CONSTRAINT fk_role_system FOREIGN KEY (sys_id) REFERENCES iam_system(id),
  CONSTRAINT ck_role_builtin CHECK (builtin IN (0,1)),
  CONSTRAINT ck_role_status CHECK (status IN ('ENABLED','DISABLED')),
  CONSTRAINT ck_role_version CHECK (version > 0),
  CONSTRAINT ck_role_id CHECK (id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE iam_permission (
  id BIGINT NOT NULL COMMENT '权限ID',
  sys_id BIGINT NOT NULL COMMENT '所属子系统',
  code VARCHAR(80) COLLATE utf8mb4_bin NOT NULL COMMENT '系统内唯一逻辑权限码，不可改',
  name VARCHAR(64) NOT NULL COMMENT '权限名称',
  type VARCHAR(16) NOT NULL COMMENT 'MENU页面或ACTION操作',
  parent_id BIGINT NULL COMMENT '同系统菜单父节点，不意味着隐式授权',
  route_path VARCHAR(200) NULL COMMENT 'MENU本地路由；ACTION必须为空',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '同级排序',
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '启用状态',
  version BIGINT NOT NULL DEFAULT 1 COMMENT '并发版本',
  created_by BIGINT NULL COMMENT '操作者快照ID',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间UTC',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间UTC',
  PRIMARY KEY (id),
  UNIQUE KEY uk_permission_scope_id (sys_id,id),
  UNIQUE KEY uk_permission_code (sys_id,code),
  KEY idx_permission_parent (sys_id,parent_id,sort_order,id),
  CONSTRAINT fk_permission_system FOREIGN KEY (sys_id) REFERENCES iam_system(id),
  CONSTRAINT fk_permission_parent FOREIGN KEY (sys_id,parent_id) REFERENCES iam_permission(sys_id,id),
  CONSTRAINT ck_permission_type CHECK (type IN ('MENU','ACTION')),
  CONSTRAINT ck_permission_route CHECK ((type='MENU' AND route_path IS NOT NULL AND route_path IN ('/overview','/members','/roles','/permissions','/audit','/profile')) OR (type='ACTION' AND route_path IS NULL)),
  CONSTRAINT ck_permission_status CHECK (status IN ('ENABLED','DISABLED')),
  CONSTRAINT ck_permission_values CHECK (version > 0 AND sort_order BETWEEN 0 AND 9999 AND (parent_id IS NULL OR parent_id <> id)),
  CONSTRAINT ck_permission_id CHECK (id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE iam_user_role (
  sys_id BIGINT NOT NULL COMMENT '系统隔离键',
  user_id BIGINT NOT NULL COMMENT '本系统成员用户ID',
  role_id BIGINT NOT NULL COMMENT '本系统角色ID',
  created_by BIGINT NULL COMMENT '指派操作者',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '指派时间UTC',
  PRIMARY KEY (sys_id,user_id,role_id),
  KEY idx_ur_role (sys_id,role_id,user_id),
  CONSTRAINT fk_ur_member FOREIGN KEY (sys_id,user_id) REFERENCES iam_member(sys_id,user_id),
  CONSTRAINT fk_ur_role FOREIGN KEY (sys_id,role_id) REFERENCES iam_role(sys_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE iam_role_permission (
  sys_id BIGINT NOT NULL COMMENT '系统隔离键',
  role_id BIGINT NOT NULL COMMENT '本系统角色ID',
  permission_id BIGINT NOT NULL COMMENT '本系统权限ID',
  created_by BIGINT NULL COMMENT '授权操作者',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '授权时间UTC',
  PRIMARY KEY (sys_id,role_id,permission_id),
  KEY idx_rp_permission (sys_id,permission_id,role_id),
  CONSTRAINT fk_rp_role FOREIGN KEY (sys_id,role_id) REFERENCES iam_role(sys_id,id),
  CONSTRAINT fk_rp_permission FOREIGN KEY (sys_id,permission_id) REFERENCES iam_permission(sys_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE iam_session (
  session_id CHAR(36) COLLATE utf8mb4_bin NOT NULL COMMENT '随机会话UUID，与JWT sid一致',
  user_id BIGINT NOT NULL COMMENT '全局用户ID',
  sys_id BIGINT NULL COMMENT '绑定系统；NULL仅平台上下文',
  auth_version BIGINT NOT NULL COMMENT '登录时账号安全版本',
  expires_at DATETIME(3) NOT NULL COMMENT '到期时间UTC',
  revoked_at DATETIME(3) NULL COMMENT '撤销时间UTC',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '签发时间UTC',
  PRIMARY KEY (session_id),
  KEY idx_session_user (user_id,expires_at),
  KEY idx_session_expiry (expires_at),
  CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES iam_user(id),
  CONSTRAINT fk_session_system FOREIGN KEY (sys_id) REFERENCES iam_system(id),
  CONSTRAINT ck_session_version CHECK (auth_version > 0 AND expires_at > created_at AND (revoked_at IS NULL OR revoked_at >= created_at))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE iam_audit_log (
  id BIGINT NOT NULL COMMENT '审计ID',
  sys_id BIGINT NULL COMMENT '目标系统；NULL为平台事件',
  actor_id BIGINT NULL COMMENT '操作者历史快照ID，无级联引用',
  actor_name VARCHAR(64) NOT NULL COMMENT '操作者名称快照',
  action VARCHAR(80) NOT NULL COMMENT '操作类型，例如 MEMBER_ROLES_REPLACED',
  target_type VARCHAR(40) NOT NULL COMMENT '资源类型',
  target_id VARCHAR(80) NOT NULL COMMENT '资源标识历史快照',
  before_json JSON NULL COMMENT '变更前白名单快照，禁止凭据',
  after_json JSON NULL COMMENT '变更后白名单快照，禁止凭据',
  request_id VARCHAR(80) NOT NULL COMMENT '请求追踪ID，非唯一',
  ip VARCHAR(45) NULL COMMENT '经可信代理解析的来源IP',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发生时间UTC',
  PRIMARY KEY (id),
  KEY idx_audit_system_time (sys_id,created_at,id),
  KEY idx_audit_actor (sys_id,actor_id,created_at,id),
  KEY idx_audit_time (created_at,id),
  KEY idx_audit_request (request_id),
  CONSTRAINT ck_audit_log_id CHECK (id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE iam_login_log (
  id BIGINT NOT NULL COMMENT '安全事件ID',
  user_id BIGINT NULL COMMENT '已知用户快照ID',
  username VARCHAR(64) NOT NULL COMMENT '脱敏登录用户名快照',
  sys_id BIGINT NULL COMMENT '尝试进入的系统',
  outcome VARCHAR(16) NOT NULL COMMENT 'SUCCESS或DENIED',
  reason VARCHAR(64) NOT NULL COMMENT '有限枚举原因，不含堆栈凭据',
  ip VARCHAR(45) NULL COMMENT '来源IP',
  request_id VARCHAR(80) NOT NULL COMMENT '请求追踪ID',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发生时间UTC',
  PRIMARY KEY (id),
  KEY idx_login_user_time (user_id,created_at,id),
  KEY idx_login_sys_time (sys_id,created_at,id),
  KEY idx_login_time (created_at,id),
  CONSTRAINT ck_login_outcome CHECK (outcome IN ('SUCCESS','DENIED')),
  CONSTRAINT ck_login_log_id CHECK (id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
