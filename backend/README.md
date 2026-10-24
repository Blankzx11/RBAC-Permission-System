# F：审计与安全事件后端

Java 17 / Spring Boot 3.5.16 / Spring JDBC。只实现 F 的审计模块，不包含 A—E 的完整业务后端。

## 运行与验证

```sh
mvn clean verify
java -jar target/rbac0-audit-1.0.0.jar --spring.profiles.active=demo
```

后端默认端口 `8080`。`demo` 使用独立 H2 内存库并初始化四条审计记录，重启恢复；它运行真实 HTTP、JDBC 和事务，不使用浏览器内存模拟。H2 测试不等于 MySQL 实库验证。

`mvn verify` 执行单元、接口和事务集成测试并生成 `target/site/jacoco/index.html`。首次构建需要 Maven 下载依赖。

## 已实现的接口

| 方法 | 路径 | 条件 |
| --- | --- | --- |
| GET | `/api/v1/platform/audit-logs` | 平台管理员的平台上下文；可按 `sysId` 筛选 |
| GET | `/api/v1/platform/audit-logs/{logId}` | 同上 |
| GET | `/api/v1/systems/{sysId}/audit-logs` | 路径与当前上下文相同，且有 `sys:audit:read` 或为该系统上下文的平台管理员 |
| GET | `/api/v1/systems/{sysId}/audit-logs/{logId}` | 同上；异系统日志返回 404 |

列表参数为 `from/to`、`actorId`、`action`、`page/pageSize`；时间采用 UTC 半开区间，默认近 7 个上海自然日，最多 90 日；`pageSize` 为 1—100。固定按 `created_at DESC, id DESC` 排序。ID 在 JSON 中全部为字符串。

## 独立演示账号

以下账号仅存在于 `demo` profile，初始密码都是 `DemoOnly12345`，可通过 `AUDIT_DEMO_PASSWORD` 改写。

| 账号 | 服务端固定身份上下文 |
| --- | --- |
| `platform_admin` | 平台管理员，平台上下文 |
| `platform_a` | 平台管理员，系统 1 上下文 |
| `auditor_a` | 系统 1 审计员 |
| `auditor_b` | 系统 2 审计员 |
| `ordinary_a` | 系统 1 普通成员，无审计读取权限 |

演示使用 HTTP Basic，凭据由后端验证。切换查询系统不会改变账号的固定上下文，越界请求仍会被拒绝。非 `demo` 模式默认拒绝访问，直到集成 A/E 的可信认证链；任意 Bearer 字符串、`X-User`、`X-SysId` 或 `X-Permissions` 都不能赋权。

## 代码分类与设计方法

- `domain/`：不可变身份、范围、事件与查询值对象，负责自身不变量。
- `port/`：成功审计、安全事件、仓储、ID 生成和 A/E 身份协作接口。
- `application/`：结构化用例流程、范围检查、事务编排。
- `functional/`：不访问数据库/网络/系统时钟的日期、白名单脱敏、集合规范化和差异计算。
- `infrastructure/`：参数化 JDBC 与可替换 ID 生成器。
- `web/`：四个查询入口、参数绑定、响应和错误映射。
- `demo/`、`config/`：独立运行装配；演示认证不进入生产默认配置。
- `src/test/`：事务原子性、独立安全事件、隔离、鉴权、日期与脱敏测试。

结构化主流程是本次检查的实现基线；接口、值对象与职责封装体现面向对象设计；纯函数、不可变结果与组合式映射体现函数式设计。这里是一套一致的 RBAC0 实现，没有为了凑范式复制三套业务代码。

## 与 A—E 对接

认证模块在完成签名、会话、账号状态、系统成员和实时权限校验后，将实现 `AuditIdentity` 的 principal 放入 Spring Security。F 只读取这些服务端事实。宿主应提供自己的 `SecurityFilterChain`；必要时在组件扫描中排除 `DefaultAuditSecurityConfiguration`，不启用 `demo`。

D 的事务用例注入 `AuditWriter`，先修改业务关联和版本，再在**同一事务**调用：

```java
auditWriter.appendInCurrentTransaction(new AuditChange(
    actor, "MEMBER_ROLES_REPLACED", AuditChange.TargetType.MEMBER,
    userId.toString(), beforeSnapshot, afterSnapshot
));
```

该端口采用 `MANDATORY`：调用方没有事务会直接失败；审计插入或脱敏校验失败会回滚业务，不能捕获后声称成功。不要异步化此调用，也不要给成功审计单独开启新事务。

A/E 在登录失败或访问拒绝处注入 `SecurityEventRecorder` 并调用 `recordSecurityEvent`。事件在独立 Bean 的 `REQUIRES_NEW` 事务中提交，因此外层业务回滚不会抹去事件；存储失败返回 `false` 并发出固定错误日志，原请求仍保持拒绝。

成功快照按目标类型选择白名单，凭据字段不写入；角色/权限 ID 集合排序去重。查询会再次对白名单投影，未知或非法遗留快照以 `redacted=true` 隐藏。历史记录无业务外键，不依赖目标账号或角色继续存在。

## MySQL 集成

复用 `../database/001_rbac0.sql` 已有的 `iam_audit_log`、`iam_login_log`，不新增生产业务表。设置 `DB_URL`、`DB_USER`、`DB_PASSWORD`，勿启用 `demo`。连接时区为 UTC，`AUDIT_NODE_ID` 在不同实例之间必须唯一（0—1023），或由全组统一的 `AuditIdGenerator` 替换默认实现。

审计表只授予应用账号 `SELECT/INSERT`，不授予 `UPDATE/DELETE`；已有宽泛库级权限也需要在实际部署时按表拆分。见 `../database/F-审计模块集成说明.adoc`。
