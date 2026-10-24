# 接口契约

- [openapi.json](openapi.json)：完整 RBAC0 设计契约，38 个操作；不能据此认定所有接口已实现。
- [F-audit-openapi.json](F-audit-openapi.json)：F 模块实际实现的四个日志查询接口，含平台/系统列表与详情，可单独导入 Apifox。
- [Apifox 导入与核对](README-Apifox导入与核对.adoc)。

F 独立 demo 使用服务端 Basic 认证；生产 Bearer 认证需 A/E 宿主接入。成功审计及安全事件通过内部端口写入，不提供公开写入或删除 API。
