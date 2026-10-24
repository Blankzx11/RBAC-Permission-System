# RBAC0 完整交付包

- 文件：[RBAC0-需求与设计交付包.zip](RBAC0-需求与设计交付包.zip)
- 材料版本：V1.2，2026-10-21；仓库导入日期：2026-10-23。
- 大小：1987811 字节。
- SHA-256：`18c93585a68000abae4f2678019d769fc2be4fa0aef9d5a60043f41f961c4f7d`。

此 ZIP 与之前交付给用户的包完全一致，含85份原始文件。包内文件另按原目录展开于仓库，根目录及 docs/frontend/backend 的 Markdown 导航是本次仓库整理新增内容。后续仓库提交可能先于重新打包的 ZIP 更新，版本以提交和具体文件说明为准。

解压后打开 `docs/index.html` 查看全部设计，打开 `prototype/vue/dist/index.html` 运行离线原型。它是设计与原型交付包，尚无生产后端或服务器部署。

## F 审计模块代码包（2026-10-24）

- [RBAC0-F审计模块代码包.zip](RBAC0-F审计模块代码包.zip)：后端、前端、五份设计、四接口契约、实际测试/覆盖率、截图和已编译 JAR。
- 大小：29047826 字节；文件数：202。
- SHA-256：`e9cb31c8bd43e45927b6ce5c83aae93856340dbbd21760330e659177ce7cdc5e`。
- 运行说明见 [F-审计模块使用说明.md](../F-审计模块使用说明.md)。F 使用 H2 独立演示；A—E 的正式认证、MySQL 实库和服务器部署仍需集成。

原 V1.2 ZIP 保持原样；新增实现请以 F 代码包和当前源码为准。源码在仓库按 backend/frontend/docs/uml/api/database/validation 分类，未包含 node_modules、Maven 缓存和临时工作目录。
