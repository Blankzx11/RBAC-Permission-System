# 数据字典

由建表 SQL 自动提取。约束和索引请同时查看原始 SQL。

## iam_platform_guard

|字段|类型与约束|说明|
|---|---|---|
|id|`TINYINT NOT NULL`|固定单行1，平台写入互斥锁|

## iam_user

|字段|类型与约束|说明|
|---|---|---|
|id|`BIGINT NOT NULL`|全局用户ID|
|username|`VARCHAR(32) COLLATE utf8mb4_bin NOT NULL`|全局唯一小写用户名，不可改|
|password_hash|`VARCHAR(255) NOT NULL`|自适应密码哈希，禁止API及日志输出|
|display_name|`VARCHAR(64) NOT NULL`|全局显示名|
|status|`VARCHAR(16) NOT NULL DEFAULT 'ENABLED'`|ENABLED或DISABLED|
|platform_admin|`BOOLEAN NOT NULL DEFAULT FALSE`|仅平台接口可维护|
|auth_version|`BIGINT NOT NULL DEFAULT 1`|全局安全版本，改密启停递增|
|failed_attempts|`INT NOT NULL DEFAULT 0`|连续失败次数|
|locked_until|`DATETIME(3) NULL`|临时登录锁定到期时间UTC|
|version|`BIGINT NOT NULL DEFAULT 1`|根资源并发版本|
|created_at|`DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)`|创建时间UTC|
|updated_at|`DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)`|更新时间UTC|

## iam_system

|字段|类型与约束|说明|
|---|---|---|
|id|`BIGINT NOT NULL`|子系统ID|
|code|`VARCHAR(40) COLLATE utf8mb4_bin NOT NULL`|全局唯一系统编码，不可改|
|name|`VARCHAR(64) NOT NULL`|系统名称|
|description|`VARCHAR(500) NOT NULL DEFAULT ''`|用途描述|
|status|`VARCHAR(16) NOT NULL DEFAULT 'ENABLED'`|系统启用状态|
|policy_version|`BIGINT NOT NULL DEFAULT 1`|本系统策略版本，权限事实变化递增|
|version|`BIGINT NOT NULL DEFAULT 1`|系统属性并发版本|
|created_at|`DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)`|创建时间UTC|
|updated_at|`DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)`|更新时间UTC|

## iam_member

|字段|类型与约束|说明|
|---|---|---|
|sys_id|`BIGINT NOT NULL`|所属子系统|
|user_id|`BIGINT NOT NULL`|全局账号|
|display_name|`VARCHAR(64) NOT NULL`|本系统内显示名|
|status|`VARCHAR(16) NOT NULL DEFAULT 'ENABLED'`|本地成员启用状态|
|version|`BIGINT NOT NULL DEFAULT 1`|成员属性和角色集合共享版本|
|created_by|`BIGINT NULL`|操作者快照ID，初始化可空|
|created_at|`DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)`|加入时间UTC|
|updated_at|`DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)`|更新时间UTC|

## iam_role

|字段|类型与约束|说明|
|---|---|---|
|id|`BIGINT NOT NULL`|角色ID|
|sys_id|`BIGINT NOT NULL`|所属子系统|
|code|`VARCHAR(40) COLLATE utf8mb4_bin NOT NULL`|系统内唯一角色编码，不可改|
|name|`VARCHAR(64) NOT NULL`|角色名称|
|description|`VARCHAR(500) NOT NULL DEFAULT ''`|角色说明|
|builtin|`BOOLEAN NOT NULL DEFAULT FALSE`|内置受保护角色标记|
|status|`VARCHAR(16) NOT NULL DEFAULT 'ENABLED'`|启用状态|
|version|`BIGINT NOT NULL DEFAULT 1`|角色属性和权限等子资源共享版本|
|created_by|`BIGINT NULL`|操作者快照ID|
|created_at|`DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)`|创建时间UTC|
|updated_at|`DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)`|更新时间UTC|

## iam_permission

|字段|类型与约束|说明|
|---|---|---|
|id|`BIGINT NOT NULL`|权限ID|
|sys_id|`BIGINT NOT NULL`|所属子系统|
|code|`VARCHAR(80) COLLATE utf8mb4_bin NOT NULL`|系统内唯一逻辑权限码，不可改|
|name|`VARCHAR(64) NOT NULL`|权限名称|
|type|`VARCHAR(16) NOT NULL`|MENU页面或ACTION操作|
|parent_id|`BIGINT NULL`|同系统菜单父节点，不意味着隐式授权|
|route_path|`VARCHAR(200) NULL`|MENU本地路由；ACTION必须为空|
|sort_order|`INT NOT NULL DEFAULT 0`|同级排序|
|status|`VARCHAR(16) NOT NULL DEFAULT 'ENABLED'`|启用状态|
|version|`BIGINT NOT NULL DEFAULT 1`|并发版本|
|created_by|`BIGINT NULL`|操作者快照ID|
|created_at|`DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)`|创建时间UTC|
|updated_at|`DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)`|更新时间UTC|

## iam_user_role

|字段|类型与约束|说明|
|---|---|---|
|sys_id|`BIGINT NOT NULL`|系统隔离键|
|user_id|`BIGINT NOT NULL`|本系统成员用户ID|
|role_id|`BIGINT NOT NULL`|本系统角色ID|
|created_by|`BIGINT NULL`|指派操作者|
|created_at|`DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)`|指派时间UTC|

## iam_role_permission

|字段|类型与约束|说明|
|---|---|---|
|sys_id|`BIGINT NOT NULL`|系统隔离键|
|role_id|`BIGINT NOT NULL`|本系统角色ID|
|permission_id|`BIGINT NOT NULL`|本系统权限ID|
|created_by|`BIGINT NULL`|授权操作者|
|created_at|`DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)`|授权时间UTC|

## iam_session

|字段|类型与约束|说明|
|---|---|---|
|session_id|`CHAR(36) COLLATE utf8mb4_bin NOT NULL`|随机会话UUID，与JWT sid一致|
|user_id|`BIGINT NOT NULL`|全局用户ID|
|sys_id|`BIGINT NULL`|绑定系统；NULL仅平台上下文|
|auth_version|`BIGINT NOT NULL`|登录时账号安全版本|
|expires_at|`DATETIME(3) NOT NULL`|到期时间UTC|
|revoked_at|`DATETIME(3) NULL`|撤销时间UTC|
|created_at|`DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)`|签发时间UTC|

## iam_audit_log

|字段|类型与约束|说明|
|---|---|---|
|id|`BIGINT NOT NULL`|审计ID|
|sys_id|`BIGINT NULL`|目标系统；NULL为平台事件|
|actor_id|`BIGINT NULL`|操作者历史快照ID，无级联引用|
|actor_name|`VARCHAR(64) NOT NULL`|操作者名称快照|
|action|`VARCHAR(80) NOT NULL`|操作类型，例如 MEMBER_ROLES_REPLACED|
|target_type|`VARCHAR(40) NOT NULL`|资源类型|
|target_id|`VARCHAR(80) NOT NULL`|资源标识历史快照|
|before_json|`JSON NULL`|变更前白名单快照，禁止凭据|
|after_json|`JSON NULL`|变更后白名单快照，禁止凭据|
|request_id|`VARCHAR(80) NOT NULL`|请求追踪ID，非唯一|
|ip|`VARCHAR(45) NULL`|经可信代理解析的来源IP|
|created_at|`DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)`|发生时间UTC|

## iam_login_log

|字段|类型与约束|说明|
|---|---|---|
|id|`BIGINT NOT NULL`|安全事件ID|
|user_id|`BIGINT NULL`|已知用户快照ID|
|username|`VARCHAR(64) NOT NULL`|脱敏登录用户名快照|
|sys_id|`BIGINT NULL`|尝试进入的系统|
|outcome|`VARCHAR(16) NOT NULL`|SUCCESS或DENIED|
|reason|`VARCHAR(64) NOT NULL`|有限枚举原因，不含堆栈凭据|
|ip|`VARCHAR(45) NULL`|来源IP|
|request_id|`VARCHAR(80) NOT NULL`|请求追踪ID|
|created_at|`DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)`|发生时间UTC|
