# RBAC-Permission-System

基于 RBAC 模型的前后端分离权限管理系统。本次导入 **RBAC0 需求与设计交付包 V1.2**（材料修订于 2026-10-21，仓库整理于 2026-10-23）。

当前交付包括需求分析、可运行的本地界面原型、38 个接口的 OpenAPI 设计、11 张表的数据库设计、详细设计底稿和六人详细功能设计分工。**生产 Java 后端、真实 API 联调、MySQL 实库验证、服务器部署及后端 JaCoCo 尚未完成。**

## 分类目录

| 目录/文件 | 内容 |
| --- | --- |
| [docs/](docs/) | 课程依据、需求、原型说明、API/数据库说明、详细设计、六人分工及前四步复核 |
| [prototype/](prototype/) | 独立 HTML 原型、Vue 3/TypeScript/Tailwind 工程及离线构建产物 |
| [api/](api/) | OpenAPI 3.0.3 契约、38 个接口清单、Apifox 导入说明 |
| [database/](database/) | MySQL 建表、演示种子、只读核对 SQL、数据字典 |
| [uml/](uml/) | 用例、实体、组件、状态与授权事务的可编辑 PlantUML 源 |
| [scripts/](scripts/) | 契约/文档生成、原型验证、标准校验和打包脚本 |
| [validation/](validation/) | 设计一致性、浏览器交互、OpenAPI 和 Vue 构建的已有验证记录 |
| [deliverables/](deliverables/) | 原始完整交付 ZIP 及校验值 |
| [frontend/](frontend/) | 前端原型入口与工程边界说明 |
| [backend/](backend/) | 后端待实现状态和设计入口 |

`prototype/` 保留交付包原有位置，确保文档链接、构建脚本和已有 SHA-256 验证记录一致。`.work/`、历史备份、`node_modules/`、临时失败截图和缓存不纳入版本控制。

## 从哪里开始

1. [第一次检查要求与前四步复核](docs/09-第一次检查要求与前四步复核.adoc)：按需求分析 → 原型 → API → 数据库检查实际产物与剩余事项。
2. [六人详细功能设计分工](docs/07-RBAC0六人分工.adoc)：只分详细功能设计；每人主设计、函数说明及 3 张设计图，共 5 份文件。30 份个人成果待成员实际细化，不是已完成的个人贡献记录。
3. [需求分析](docs/01-需求分析.adoc)、[API 设计](docs/04-API设计.adoc)、[数据库设计](docs/05-数据库设计.adoc)、[详细设计底稿](docs/06-详细设计与模块分解.adoc)。
4. [完整交付包说明](README.adoc) 与 [需求追踪矩阵](docs/traceability.csv)。

## 运行原型

下载并解压 [RBAC0 完整交付包](deliverables/RBAC0-需求与设计交付包.zip)，或克隆本仓库后用浏览器打开：

- `prototype/vue/dist/index.html`：已打包的 Vue 工程版，包含本地 JS/CSS，无 CDN 依赖。
- `prototype/index.html`：独立 HTML 兼容版。
- `docs/index.html`：完整设计离线阅读版；`docs/07-RBAC0六人分工.html` 为独立分工阅读版。

原型初始演示口令为 `DemoOnly12345`，全部数据只在页面内存中，刷新恢复；SQL 种子的密码为禁止登录占位值。Vue/TypeScript 宿主复用共享 JavaScript 交互引擎，工程边界与复建方式见 [prototype/vue/README.adoc](prototype/vue/README.adoc)。GitHub 文件页显示 HTML 源文件，需下载到本地打开；本次没有配置网站托管。

## 验证与实施状态

已有记录包括 43 项设计一致性检查、38 个操作的 OpenAPI 标准校验、56 项兼容原型交互、16 项 Vue 离线交互，以及 Vue/TS 检查与构建；详见 [validation/report.md](validation/report.md)。这些是设计和模拟原型证据，不是后端集成、并发、部署或覆盖率验收结果。验证脚本当前使用本机运行时路径，换电脑需按交付说明配置。

本仓库按用户要求托管到 GitHub；课程原文列出的 Gitee/课程网站提交要求见课程依据文档，未因本次 GitHub 上传而自动完成。
