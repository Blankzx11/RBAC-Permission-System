# F 模块实际验证

记录时间：2026-10-24T04:27:38.663969+00:00

| 检查 | 结果 |
| --- | --- |
| Java/Maven 测试 | 34/34 通过 |
| Vue/TS 纯函数测试 | 5/5 通过 |
| 浏览器真实接口联调 | 25/25 通过；0脚本错误 |
| vue-tsc + Vite | 构建通过 |
| JaCoCo 行覆盖率 | 92.8% |
| JaCoCo 分支覆盖率 | 75.57% |
| OpenAPI 子集 | 4个操作，标准校验通过 |

覆盖范围：F 的成功审计、安全事件、四个只读查询接口、实际 API 页面。数据库测试为 H2/JDBC；没有执行 MySQL 实库、A—E 集成和服务器部署。

## 检查入口

1. 打开 `F-audit-jacoco/index.html`，查看实际覆盖率及逐行标记。
2. 查看 `F-module-report.json` 的测试名称、源文件和构建产物哈希。
3. 查看 `F-browser-report.json` 的真实请求状态和检查记录，以及三张 `F-audit-*.png` 截图。
4. 后端执行 `mvn clean verify`，前端执行 `npm test`、`npm run build` 可重新验证。
