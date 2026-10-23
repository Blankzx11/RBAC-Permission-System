# 前端与可运行界面原型

当前已交付的前端成果是 [../prototype/](../prototype/) 内的本地模拟原型，保留交付包目录位置以维持相对链接和验证记录。

- [Vue 3 / TypeScript / Tailwind 工程](../prototype/vue/)：源码在 `src/`，已构建离线入口为 `dist/index.html`。
- [独立 HTML 兼容版](../prototype/index.html)：下载后直接打开。
- [页面、交互与 API 映射说明](../docs/03-界面原型设计.adoc)。

Vue/TS 宿主复用原有 JavaScript 交互引擎；没有全量迁移引擎内部类型，也没有连接真实后端。数据只在浏览器内存中，刷新重置。开发/构建步骤见 [原型工程说明](../prototype/vue/README.adoc)。
