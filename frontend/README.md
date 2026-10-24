# F：审计日志页面

Vue 3 / TypeScript / Tailwind。页面调用 F 后端的四个实际查询接口，支持日期、操作者、操作类型、平台系统筛选、分页、前后值与字段差异，以及加载、空、失败、无权限状态。

## 启动

先按 `../backend/README.md` 启动后端 `demo`，再在本目录运行：

```sh
npm ci
npm run dev
```

打开终端提示的 `http://127.0.0.1:5173`。默认系统 A 审计员，密码 `DemoOnly12345`；点击“查询日志”访问后端。切换身份后需重新查询。选择“平台管理员”会切到平台查询；选择系统 B 身份会切到系统 2。

```sh
npm test
npm run build
npm run preview
```

开发服务器和 preview 将 `/api` 代理到 `http://127.0.0.1:8080`。构建后的 `dist/` 需要 HTTP 服务；由于页面调用真实后端，不使用双击 HTML 的运行方式。生产部署可用同源反向代理，或通过 `VITE_API_BASE` 配置 API 地址并由宿主处理跨域。

## 集成与代码位置

- `src/features/audit/AuditLogPage.vue`：P10 日志列表、筛选、分页和详情入口。
- `AuditDiffPanel.vue`：字段差异与只读快照，使用文本插值，不使用 `v-html`。
- `auditApi.ts`：四个接口、统一错误、认证头与中断请求。
- `useAuditLogs.ts`：列表/详情状态；身份或范围变化后取消旧请求，旧响应不回填。
- `auditFunctions.ts`：日期转换、BIGINT 校验、稳定差异计算等纯函数。
- `src/App.vue`：单独演示的身份连接面板；集成时可移除，由 A/E 提供真实会话。

集成页面只需传入 `authorization` 回调和随会话变化的 `identityKey`，例如 `<AuditLogPage :authorization="() => 'Bearer ' + accessToken" :identity-key="sessionRevision" />`。界面输入的系统 ID 不产生授权，后端仍按服务端上下文判断。

账号口令与访问令牌仅放页面内存，不写 localStorage、URL 或控制台。Basic 模式只对应后端显式启用的 `demo`；Bearer 模式需要宿主 A/E 的认证实现，独立 F 模块默认不会信任任意 token。
