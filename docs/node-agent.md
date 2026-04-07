# Loom Node Agent

`loom-node` 是 Phase 1 的只读节点代理，职责固定为“注册 + 心跳 + 快照 + 探测”。

## 当前能力

- 注册自身到 Loom Server
- 周期性发送 Bearer Token 鉴权的心跳
- 上报真实系统 CPU、进程 CPU、物理内存、磁盘使用情况
- 执行结构化 HTTP / TCP 服务探测
- 将 `nodeId`、最近心跳、最近错误持久化到本地状态文件，避免重启重复注册

## 结构化 probe

配置键为 `loom.node.service-probes[]`，字段：

- `name`
- `kind(http|tcp)`
- `target`
- `timeoutMs`
- `expectedStatus`

生产 compose 通过 `SPRING_APPLICATION_JSON` 注入默认 probe：

- `loom-server -> http://loom-server:8080/api/health`
- `loom-web -> http://loom-web/`

## 服务端状态机

服务端不会把节点当成简单的“有心跳就在线”。最终状态由两部分共同决定：

- 最近心跳时间
- 最近 snapshot / probe 结果

因此节点会被归类为：

- `online`
- `offline`
- `degraded`

## Phase 1 边界

- 只读，不执行 shell
- 不做部署自动化
- 不提供远程命令分发

## 测试覆盖

- probe 解析与 HTTP / TCP 探测
- 注册去重与持久化 `nodeId`
- Bearer Token 请求
- 服务端降级 / 离线状态计算
