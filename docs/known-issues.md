# Loom 已知问题与工程化待修复清单
更新时间：2026-04-08

## 维护规则

- 所有问题都要带 `ID`、`标题`、`优先级`、`状态`、`现状`、`影响`、`建议动作`。
- `Open` 表示仍待修复，`Resolved` 表示本轮已修复并进入回归观察。
- 这里只维护真实工程问题，不记录泛泛而谈的优化愿望。

## Open

| ID | 标题 | 优先级 | 状态 | 现状 / 不正常点 | 影响 | 建议动作 |
| --- | --- | --- | --- | --- | --- | --- |
| `LOOM-ENG-002` | `jd` 独立容器栈切换尚未完成远端验收 | P1 | Open | 仓库内的独立 compose 栈、`sprite` 退役脚本和 smoke test 已经就位，但还没有在 `jd` 上完成一轮真实切换验收。 | 远端环境是否已彻底脱离旧 `sprite` 体系，仍需最终上线验证。 | 按部署文档执行 `bootstrap -> retire-sprite -> deploy -> smoke`，验收通过后再关闭。 |
| `LOOM-ENG-004` | `jd` 的正式端口切换仍待验收 | P1 | Open | 按计划，Loom 生产只应暴露宿主机 `80`，后端不应再对外暴露 `8080`；但 `jd` 上是否已彻底腾空旧占用，还没有最终验收记录。 | 端口规划不清会影响独立 compose 栈上线、切换和观测。 | 在 `jd` 上完成一次真实切换，并留档 `docker ps` / `ss -ltnp` 结果。 |
| `LOOM-ENG-006` | 远端容器构建链仍不稳定 | P2 | Open | 之前在 `jd` 上构建时遇到镜像拉取、代理污染、基础镜像依赖问题。 | CI/CD 难以直接对齐远端环境。 | 固化远端 Docker 环境、清理代理、补镜像缓存策略。 |
| `LOOM-UI-003` | 前端专业感仍需主观验收 | P2 | Open | 新版已重构为项目级工作台，但“是否足够高级、专业”仍需要产品主观验收。 | 可能还需要第二轮视觉、排版和交互收口。 | 根据验收反馈继续做第二轮 UI 收口。 |

## Resolved

| ID | 标题 | 优先级 | 状态 | 本轮处理 | 验收影响 |
| --- | --- | --- | --- | --- | --- |
| `LOOM-UI-001` | 桌面端页面不是全屏工作台 | P1 | Resolved | 前端已改为全屏桌面工作台，不再使用居中卡片式外壳。 | 电脑端打开后应直接看到完整工作台布局。 |
| `LOOM-UI-002` | 普通用户文案不是中文优先 | P1 | Resolved | 前端源码、mock 数据、共享 contracts 和 bootstrap 默认文案已统一改为中文优先。 | 除专业术语外，不应再大面积出现英文按钮和提示。 |
| `LOOM-UI-004` | 项目级会话管理缺失 | P1 | Resolved | 已补齐项目切换、项目级会话列表、多轮切换、重命名、归档、搜索、Chat / Plan 分组。 | 验收时应能直接看到项目级会话工作流。 |
| `LOOM-UI-005` | 设置页没有真实内容 | P1 | Resolved | 已补齐真实设置中心，包括工作区、项目默认项、模型、Vault、Nodes、Commands / Skills、诊断。 | 设置页不再是空说明页。 |
| `LOOM-DATA-001` | 默认种子数据过少，无法直接展示项目级体验 | P1 | Resolved | 已补齐多项目、多会话、运行中 Plan、完成 Plan、记忆和资产默认数据。 | 首次打开即可直接看到项目级工作台效果。 |
| `LOOM-ENG-001` | 服务端仍为内存存储 | P0 | Resolved | 服务端已接入 MySQL、Flyway 与 JDBC repository，并补了 [ServerPersistenceIntegrationTest.java](C:\Users\16343\Desktop\loom\apps\loom-server\src\test\java\com\loom\server\ServerPersistenceIntegrationTest.java) 覆盖项目、会话、记忆、计划、资产、节点与设置的持久化链路。 | 重启后不再默认丢失核心数据，Phase 1 的持久化底座已经落地。 |
| `LOOM-ENG-003` | Web 对外入口仍依赖 Python 代理 | P1 | Resolved | 仓库已退役进程式 runtime / Python 网关路径，正式入口固定为 [docker-compose.yml](C:\Users\16343\Desktop\loom\docker-compose.yml) 中的 `loom-web` Nginx 同源代理。 | 仓库内不再保留 Python 代理作为正式生产方案，部署路径与计划一致。 |
| `LOOM-ENG-005` | Node 指标仍是最小实现 | P1 | Resolved | 节点代理现在会在 [NodeSnapshotService.java](C:\Users\16343\Desktop\loom\apps\loom-node\src\main\java\com\loom\node\service\NodeSnapshotService.java) 采集真实 CPU、进程 CPU、物理内存和磁盘使用率，并在 [NodeProbeService.java](C:\Users\16343\Desktop\loom\apps\loom-node\src\main\java\com\loom\node\service\NodeProbeService.java) 执行结构化服务探测。 | `Nodes` 视图已有真实运行指标和服务探针，不再只是最小演示数据。 |
| `LOOM-ENV-001` | 本机默认 Java 版本过低 | P2 | Resolved | Windows 侧的 [mvnw.cmd](C:\Users\16343\Desktop\loom\apps\loom-server\mvnw.cmd) 与 [mvnw.cmd](C:\Users\16343\Desktop\loom\apps\loom-node\mvnw.cmd) 现已在 `JAVA_HOME` 过低时自动回退到本机可用的 JDK 21，发布脚本也不再硬编码单一路径。 | 在当前工作站上直接执行 Maven wrapper 不会再因为默认 Java 8 卡死。 |
| `LOOM-APP-003` | 节点重复注册 | P1 | Resolved | 在 [NodeLifecycleService.java](C:\Users\16343\Desktop\loom\apps\loom-node\src\main\java\com\loom\node\service\NodeLifecycleService.java) 收紧注册并发，避免启动时重复注册。 | 节点列表更稳定，不再因启动竞争出现重复节点。 |
| `LOOM-APP-004` | 服务端最初不能输出可执行 jar | P1 | Resolved | 已补 Spring Boot repackage，并确认在显式 JDK 21 环境下可打包。 | 运行时发布脚本可稳定拉起服务端。 |
| `LOOM-APP-005` | `/api/messages` 路由重复 | P1 | Resolved | 已去掉重复映射，消息接口已能正常返回。 | Chat 视图拉消息时不应再触发映射冲突。 |

## 后续修复顺序建议

1. 先完成 `LOOM-ENG-002`、`LOOM-ENG-004` 的远端切换和端口验收。
2. 再解决 `LOOM-ENG-006`，把远端容器构建链稳定下来。
3. 最后根据产品验收继续收口 `LOOM-UI-003`。
