# F 审计模块代码与交接

你负责的 F 已按原分工实现为独立可运行的审计模块，包含后端、真实接口调用页面、测试和五份详细设计成果。

| 分类 | 位置 |
| --- | --- |
| 后端代码与运行说明 | [backend/README.md](backend/README.md)、[backend/src/main/java/com/rbac/audit/](backend/src/main/java/com/rbac/audit/) |
| 四个接口入口 | [AuditController.java](backend/src/main/java/com/rbac/audit/web/AuditController.java) |
| 给其他成员的写端口 | [AuditWriter.java](backend/src/main/java/com/rbac/audit/port/AuditWriter.java)、[SecurityEventRecorder.java](backend/src/main/java/com/rbac/audit/port/SecurityEventRecorder.java) |
| 纯函数与白名单脱敏 | [AuditFunctions.java](backend/src/main/java/com/rbac/audit/functional/AuditFunctions.java) |
| 前端页面 | [frontend/README.md](frontend/README.md)、[frontend/src/features/audit/](frontend/src/features/audit/) |
| 数据库对接 | [database/F-审计模块集成说明.adoc](database/F-审计模块集成说明.adoc)；复用原有两张日志表 |
| 主设计 | [F-审计记录与查询详细设计.adoc](docs/detail-design/F-审计记录与查询详细设计.adoc) |
| 函数说明 | [F-函数说明.adoc](docs/detail-design/F-函数说明.adoc) |
| 三张设计图 | [功能结构图](uml/detail-design/F-审计功能结构图.puml)、[事务时序图](uml/detail-design/F-同事务审计与回滚时序图.puml)、[范围查询流程图](uml/detail-design/F-审计范围查询流程图.puml) |
| F接口契约 | [F-audit-openapi.json](api/F-audit-openapi.json)，可导入 Apifox；仅4个已实现的查询接口 |
| 实际验证与截图 | [F-module-report.md](validation/F-module-report.md)、[JaCoCo](validation/F-audit-jacoco/index.html)、[页面截图](validation/F-audit-page.png) |

## 快速运行

需要 Java 17+、Maven 3.6.3+、Node.js 24（或兼容版本）。在两个终端分别执行：

```sh
cd backend
mvn clean verify
java -jar target/rbac0-audit-1.0.0.jar --spring.profiles.active=demo
```

```sh
cd frontend
npm ci
npm run dev
```

浏览器打开 `http://127.0.0.1:5173`，默认演示密码 `DemoOnly12345`，点击“查询日志”。系统 A/B 审计员、平台管理员和无审计权限成员的区别见后端说明。`demo` 使用真实后端、JDBC 和 H2 事务；MySQL 配置和宿主 A/E 认证需要后续集成。

本机已编译 JAR 在 `backend/target/rbac0-audit-1.0.0.jar`。独立压缩包 `RBAC0-F审计模块代码包.zip` 内放在 `build/rbac0-audit-1.0.0.jar`，可直接 `java -jar build/rbac0-audit-1.0.0.jar --spring.profiles.active=demo`。前端 `dist` 需通过 HTTP 服务和 `/api` 代理访问，不能当作原来的离线模拟原型直接双击。

## 怎么检查我负责的功能

1. 默认选择“系统A审计员”，点击查询，应只看到系统 1 的两条记录；点击“前后值”，检查权限集合和版本差异。
2. 切换“系统B审计员”，应只看到系统 2 的一条记录；手动把当前系统ID改成 1 再查询，后端应返回 403。
3. 切换“平台管理员”，应看到全部四条记录；筛选系统ID填 2 应剩一条；清除筛选应恢复四条。
4. 切换“普通成员”，查询应返回 403；输入错误密码应返回 401；日期只填一端应提示错误。
5. 在 `backend` 执行 `mvn clean verify` 后打开 `target/site/jacoco/index.html`，看测试及逐行覆盖率。成功审计回滚和失败事件独立提交的证据在 `AuditTransactionTest.java`。

演示数据在每次启动后写入内存库，重启会重新初始化。页面展示的是成功变更审计；登录失败/拒绝访问记录写入独立 `iam_login_log`，原契约没有为它另设查询接口。

## 代码如何对应老师的设计方法

结构化：控制器 → 查询/写入用例 → 数据访问，以明确步骤组织每个功能；对象化：身份、事件、范围对象封装不变量，端口分离职责，使用组合和依赖注入；函数式：日期规范化、白名单投影、ID集合规范化、差异计算为无 I/O 纯函数，输入不变、输出独立。

这是第一次 RBAC0 检查的结构化实现基线，同时使用 OO 和函数式思想改善模块内部设计。没有加入角色继承或约束模型，也没有把三套重复业务代码当作三个范式的完整后端。

## 与其他成员对接时需要说清楚

1. A/E 提供已经过会话、状态和实时权限校验的 `AuditIdentity`，F 不信任客户端身份头或任意 token。
2. A/B/C/D 的成功变更在自己的事务中调用 `AuditWriter`；日志失败必须让业务一起回滚。
3. A/E 的失败登录、拒绝访问调用 `SecurityEventRecorder`，它独立提交，记录失败不能让原请求变为成功。
4. F 只查询/追加日志，不替别人修改角色、成员或授权，不提供审计写入、修改或删除的公共 HTTP 接口。

测试与覆盖率以本次实际生成的 `validation/F-module-report.json`、`backend/target/surefire-reports/` 和 `backend/target/site/jacoco/index.html` 为准。它们只代表 F 模块，不代表整个 RBAC0 系统已实现、部署或完成 MySQL 验收。
