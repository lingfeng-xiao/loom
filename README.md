# Loom

Loom 是一个项目优先的个人 AI OS 单仓库，当前仓库已经落下 Phase 1 MVP 的服务端、前端、节点代理、部署脚本和运行文档。

当前仓库按“独立容器栈”维护：

- 生产入口固定为 `loom-web` 的 Nginx，同源代理 `/api/* -> loom-server:8080`
- 数据持久化固定为 MySQL 8 + Flyway
- `loom-node` 只保留只读节点注册、心跳、快照和 HTTP/TCP 探测
- `jd` 的切换路径以独立 `docker compose` 为准，不再保留进程式 runtime 发布作为正式方案

## 目录结构

```text
apps/
  loom-server/   Spring Boot 服务端
  loom-web/      React 工作台
  loom-node/     Java 21 节点代理
packages/
  contracts/     共享 TypeScript 契约
deploy/
  scripts/       构建、发布、冒烟测试脚本
docs/            部署、节点、问题台账、前端基线文档
```

## 常用命令

```bash
./apps/loom-server/mvnw -q test
./apps/loom-server/mvnw -q -DskipTests package
cd apps/loom-web && npm run build
./apps/loom-node/mvnw -q test
docker compose config
docker compose -f docker-compose.yml -f docker-compose.dev.yml config
```

PowerShell 下把 `./apps/.../mvnw` 换成 `.\apps\...\mvnw.cmd` 即可。
Windows 侧的 Maven wrapper 现在会优先复用 `JAVA_HOME`，若版本过低会自动回退到本机可用的 JDK 21。

## 文档入口

- [文档索引](C:\Users\16343\Desktop\loom\docs\README.md)
- [部署文档](C:\Users\16343\Desktop\loom\docs\deployment.md)
- [Git 工作流规范](C:\Users\16343\Desktop\loom\docs\git-workflow.md)
- [节点代理说明](C:\Users\16343\Desktop\loom\docs\node-agent.md)
- [运维回滚文档](C:\Users\16343\Desktop\loom\docs\rollback.md)
- [已知问题与工程化待修复清单](C:\Users\16343\Desktop\loom\docs\known-issues.md)
- [前端工作台与中文化基线](C:\Users\16343\Desktop\loom\docs\frontend-baseline.md)

## 当前说明

- 服务端已经接入 MySQL、Flyway 和 JDBC repository；本地集成测试使用 H2 的 MySQL 兼容模式回归持久化链路。
- 生产编排入口固定为 [docker-compose.yml](C:\Users\16343\Desktop\loom\docker-compose.yml)，本地调试暴露端口放在 [docker-compose.dev.yml](C:\Users\16343\Desktop\loom\docker-compose.dev.yml)。
- `loom-node` 通过结构化 `service-probes` 上报真实 CPU、内存、磁盘和服务探测结果，服务端会根据心跳时间和 probe 状态计算在线 / 离线 / 降级。
- 前端现在按桌面全屏工作台布局设计，普通用户文案默认使用中文，专业术语按基线文档保留。
## Real LLM

- Loom server now generates assistant replies through an OpenAI-compatible `POST /chat/completions` call.
- Configure `LOOM_AI_PROVIDER_LABEL`, `LOOM_AI_BASE_URL`, `LOOM_AI_MODEL`, `LOOM_AI_TEMPERATURE`, and `LOOM_AI_API_KEY` in `.env`.
- The API key is server-side only and is not stored in workspace settings or exposed to the frontend.

## GitHub Actions Deploy

- `.github/workflows/release.yml` now builds and pushes `loom-server`, `loom-web`, and `loom-node` images to GHCR.
- When deploy secrets are configured, the same workflow uploads `deploy/docker-compose.release.yml` plus remote scripts to the server and performs an SSH-based rollout.
- Required GitHub secrets: `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_PRIVATE_KEY`.
- Optional GitHub secrets: `DEPLOY_PORT`, `DEPLOY_ROOT`, `DEPLOY_SSH_KNOWN_HOSTS`, `GHCR_USERNAME`, `GHCR_TOKEN`, `DEPLOY_WEB_URL`.
- The server should have a `${DEPLOY_ROOT}/.env` file based on `.env.example`.
