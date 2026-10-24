# RBAC-Permission-System

多系统隔离的 **RBAC0 权限管理课程项目**。仓库包含 V1.2 需求与设计基线，以及 2026-10-24 新增的 **F 审计模块实现**：成功变更审计、独立安全事件、平台/系统四个查询接口、真实 API 日志页面和五份详细设计。

F 已可独立演示；A—E 的正式业务与认证集成、MySQL 实库验收及服务器部署仍待完成。F 的测试结果不代表整套 RBAC0 已完成。

## 分类目录

| 目录/文件 | 内容 |
| --- | --- |
| [backend/](backend/) | F 模块 Java 17 / Spring Boot 后端、JDBC、事务及 34 项测试 |
| [frontend/](frontend/) | F 模块 Vue 3 / TypeScript / Tailwind 页面，调用真实后端；含源码及构建产物 |
| [docs/](docs/) | 需求、原型/API/数据库设计、六人分工；[F 主设计](docs/detail-design/F-审计记录与查询详细设计.adoc)、[F 函数说明](docs/detail-design/F-函数说明.adoc) |
| [uml/](uml/) | 原有设计图及 [F 的三张详细设计图](uml/detail-design/) |
| [api/](api/) | 原有 38 接口设计；[F 四接口契约](api/F-audit-openapi.json)可导入 Apifox |
| [database/](database/) | 11 张基础表、字典、种子和[F 审计表集成说明](database/F-审计模块集成说明.adoc) |
| [prototype/](prototype/) | 原有浏览器内存模拟原型，保留离线演示用途 |
| [scripts/](scripts/) | 设计生成、契约校验、浏览器联调与打包脚本 |
| [validation/](validation/) | 原有基线检查及 [F 实际验证报告](validation/F-module-report.md)、覆盖率和截图 |
| [deliverables/](deliverables/) | 原 V1.2 设计包和新增 F 代码包，附 SHA-256 |

## 运行 F 模块

先阅读 [F 运行与交接说明](F-审计模块使用说明.md)。需要 Java 17+、Maven 3.6.3+ 和 Node.js 24。分别在两个终端运行：

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

打开 `http://127.0.0.1:5173`，默认演示密码 `DemoOnly12345`，点击“查询日志”。demo 使用真实后端、JDBC 与 H2 内存数据库；平台管理员、系统 A/B 审计员和普通成员的账号说明见 [backend/README.md](backend/README.md)。重启后演示数据重新初始化。

也可下载 [F 完整代码包](deliverables/RBAC0-F审计模块代码包.zip)，其中 `build/rbac0-audit-1.0.0.jar` 已编译，可直接使用 Java 17+ 启动。前端 `dist` 需要 HTTP 服务和 `/api` 代理，不能双击当作离线模拟原型使用。

## 设计方法与交接

以结构化功能分解组织流程；通过不可变值对象、端口和组合划分对象职责；日期规范化、白名单脱敏、ID 集合与前后值差异使用无 I/O 纯函数。F 复用原有日志表，不引入角色继承或约束模型。

A/B/C/D 在业务事务中调用 `AuditWriter`，审计失败随业务一起回滚；A/E 通过 `SecurityEventRecorder` 独立记录安全事件，并提供经认证校验的 `AuditIdentity`。非 demo 默认拒绝访问，宿主 Bearer 认证需要 A/E 集成。F 没有对客户端开放日志写入、修改或删除接口。

六人分工仍然只分详细功能设计。F 的 5 份文件已提供、需本人复核；A—E 的 25 份仍待完成，详见[六人分工](docs/07-RBAC0六人分工.adoc)。

## 验证材料

F：后端 **34/34**、前端纯函数 **5/5**、浏览器真实接口联调 **25/25** 通过，Vue/TS 与 Vite 构建通过，四接口 OpenAPI 标准校验通过；JaCoCo 行覆盖率 **92.8%**，分支覆盖率 **75.57%**。

详细证据：[F 报告](validation/F-module-report.md)、[测试及产物哈希](validation/F-module-report.json)、[浏览器请求记录](validation/F-browser-report.json)、[JaCoCo HTML](validation/F-audit-jacoco/index.html)。GitHub 文件页显示 HTML 源码，覆盖率阅读版需下载后打开。

原设计与模拟原型仍保留 43 项一致性、56 项兼容原型、16 项 Vue 离线交互等记录，见 [基线验证记录](validation/report.md)。H2 测试不代替 MySQL 实库验收，尚未执行全组集成、服务器部署或压力测试。

课程要求与剩余检查项见 [第一次检查复核](docs/09-第一次检查要求与前四步复核.adoc)。本仓库按用户要求上传到 GitHub；课程另列的 Gitee/课程网站提交未因本次上传自动完成。
