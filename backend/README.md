# RBAC0 后端（第一次检查版）

本目录是可直接运行的 Spring Boot RBAC0 后端，重点实现 E 模块：

- 有效权限求解：有效成员 → 启用角色 → 启用权限，并集去重；
- 动态菜单：MENU 授权、ACTION 父容器补全、祖先补全、`navigable=false`；
- 统一鉴权：`@RequirePermission` 静态元数据 + 会话/账号/系统/成员安全门；
- 系统隔离：路径 `sysId` 必须等于会话 `sysId`，查询 SQL 始终带 `sys_id`；
- 策略版本缓存：键为 `sysId:userId:policyVersion`，撤权后自然换键；
- 角色/权限/成员授权事务、ETag 乐观锁和审计写入；
- 自动化测试与 JaCoCo 报告。

## 快速运行

要求 JDK 21 或更高版本。

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

打开：<http://localhost:8080/>

默认使用内存 H2，启动时自动建表并载入演示数据。停止后数据重置，适合第一次检查演示。

演示账号密码均为：`DemoOnly12345`

| 账号 | 系统 A | 系统 B |
| --- | --- | --- |
| `platform_admin` | 平台管理员，可进入任一系统 | 同左 |
| `system_admin` | 系统管理员 | 成员查看员 |
| `auditor` | 审计查看员 | 系统管理员 |
| `chenyu` | 成员查看员 | 成员查看员 |
| `lichen` | 账号停用，不能登录 | 同左 |

## 推荐现场演示

1. 用 `platform_admin` 登录系统 A，展示 18 个权限码和 4 个菜单。
2. 在“成员角色指派演示”中选择陈雨，把“成员查看员”换成“审计查看员”并保存。
3. 退出，用 `chenyu` 登录系统 A，展示只剩 `menu:audit` 与 `sys:audit:read`。
4. 切换到系统 B，展示同一用户在另一系统仍是成员查看员，证明没有跨系统串权。
5. 用陈雨访问角色接口，展示 403；不带令牌访问，展示 401。
6. 展示 `backend/target/site/jacoco/index.html` 的覆盖率报告。

## 自动化验证

```powershell
cd backend
.\mvnw.cmd verify
```

结果：

- 测试：5 个测试全部通过；
- JaCoCo：`backend/target/site/jacoco/index.html`；
- 可执行包：`backend/target/rbac-backend-1.0.0.jar`。

也可以先启动服务，再运行：

```powershell
cd backend
.\scripts\demo-smoke.ps1
```

## 主要接口

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| POST | `/api/v1/auth/sessions` | 公开 |
| POST | `/api/v1/auth/contexts` | 已登录 |
| GET | `/api/v1/me` | 已登录 |
| GET | `/api/v1/me/permissions` | 已登录 |
| GET | `/api/v1/me/menus` | 已登录 |
| GET | `/api/v1/systems/{sysId}/members` | `sys:member:read` |
| GET/PUT | `/api/v1/systems/{sysId}/members/{userId}/roles` | `sys:member:assign` |
| GET/POST | `/api/v1/systems/{sysId}/roles` | `sys:role:read/create` |
| GET/PUT | `/api/v1/systems/{sysId}/roles/{roleId}/permissions` | `sys:role:grant` |
| GET/POST | `/api/v1/systems/{sysId}/permissions` | `sys:permission:read/create` |
| GET | `/api/v1/systems/{sysId}/audit-logs` | `sys:audit:read` |

## 使用 MySQL 验证

默认 H2 用于“随时可运行”。如课程检查要求真实 MySQL，在具有 MySQL 8.0.16+ 的电脑上：

```sql
CREATE DATABASE rbac_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'rbac_app'@'localhost' IDENTIFIED BY 'change_me';
GRANT ALL PRIVILEGES ON rbac_demo.* TO 'rbac_app'@'localhost';
FLUSH PRIVILEGES;
```

依次执行仓库根目录：

```text
database/001_rbac0.sql
database/003_demo_seed.sql
database/005_backend_demo_password.sql
database/006_backend_verification.sql
```

然后启动：

```powershell
$env:SPRING_PROFILES_ACTIVE='mysql'
$env:DB_URL='jdbc:mysql://127.0.0.1:3306/rbac_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false'
$env:DB_USERNAME='rbac_app'
$env:DB_PASSWORD='change_me'
.\mvnw.cmd spring-boot:run
```

MySQL 模式下不会自动建表，必须先成功执行上述脚本并保存真实输出。

## 边界说明

- 这是第一次检查的可运行后端与 E 模块闭环，不等同于 38 个 OpenAPI 端点全部完成。
- 当前缓存是单实例进程内缓存，生产多实例应替换为 Redis。
- 未完成公网服务器部署、限流、完整审计查询和生产级运维配置。
- 演示口令哈希仅用于本地种子数据，不能用于生产。
