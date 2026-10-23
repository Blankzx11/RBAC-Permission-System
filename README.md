# RBAC-Permission-System

基于 RBAC0 的多系统权限管理系统课程项目。仓库当前在 V1.2 需求、界面原型、API 契约和数据库设计基线上，新增了**可运行 Java 后端**与 E 模块（有效权限、菜单、统一鉴权）闭环。

## 当前可用内容

| 目录 | 内容 |
| --- | --- |
| `backend/` | Spring Boot + JDBC 可运行后端、联调控制台、自动化测试、JaCoCo |
| `docs/` | 需求、原型、API、数据库、详细设计、六人分工、现场演示说明 |
| `prototype/` | 独立 HTML 与 Vue 3/TypeScript 离线界面原型 |
| `api/` | 38 个端点的 OpenAPI 3.0.3 契约与 Apifox 导入说明 |
| `database/` | MySQL 11 表 DDL、演示数据、口令脚本、只读核对 SQL |
| `uml/` | 用例、领域、组件、状态及 E 模块设计图 |
| `validation/` | 既有设计、原型、构建与契约验证记录 |

## 直接运行后端

要求 JDK 21+：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

打开 <http://localhost:8080/>。默认 H2 内存库自动建表并载入演示数据，重启恢复初始状态。

演示账号密码均为 `DemoOnly12345`，例如：

- `platform_admin` / 系统 A：平台管理员；
- `chenyu` / 系统 A：成员查看员；
- `auditor` / 系统 A：审计查看员。

完整操作步骤见 [docs/10-第一次检查后端联调与现场演示.adoc](docs/10-第一次检查后端联调与现场演示.adoc) 与 [backend/README.md](backend/README.md)。

## 已实现的 RBAC0 闭环

- 登录、退出、系统上下文切换；
- 有效权限：有效成员 → 启用角色 → 启用权限，并集去重；
- 动态菜单：MENU 授权、ACTION 父容器与祖先补全、非导航容器；
- 统一鉴权：Bearer 会话 + 账号/系统/成员安全门 + `@RequirePermission`；
- 系统隔离：路径 `sysId` 与会话一致，SQL 强制携带 `sys_id`；
- 角色、权限、成员读取与角色授权/成员角色分配；
- ETag 乐观锁、策略版本递增、审计写入；
- 5 个自动化测试和 JaCoCo 报告。

运行验证：

```powershell
cd backend
.\mvnw.cmd verify
```

JaCoCo 报告位于 `backend/target/site/jacoco/index.html`，可执行包位于 `backend/target/rbac-backend-1.0.0.jar`。

## 数据库与 MySQL

默认 H2 仅用于低门槛演示。真实 MySQL 8.0.16+ 可依次执行：

```text
database/001_rbac0.sql
database/003_demo_seed.sql
database/005_backend_demo_password.sql
database/006_backend_verification.sql
```

再使用 `backend/README.md` 中的 `mysql` profile 配置启动。当前尚未在带管理员权限的 MySQL 服务上保存实库执行截图，不应把 H2 测试等同于 MySQL 验收。

## 完成边界

后端实现覆盖第一次检查所需的核心 RBAC0 闭环，但不是 38 个 OpenAPI 端点全部实现；缓存仍是单实例内存实现；公网服务器部署、压力测试、完整审计查询及 Gitee/课程网站提交仍需小组完成。设计与原型验证不能替代生产安全和真实部署证明。
