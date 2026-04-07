# Loom Deployment

Loom Phase 1 以独立 `docker compose` 为唯一正式部署路径。

不再保留进程式 runtime 发布，也不再保留 Python `serve-web.py` 网关作为正式入口。外部访问统一走 `loom-web` 容器内的 Nginx，同源代理 `/api/* -> loom-server:8080`。

## Prerequisites

- `jd` 上的旧 `sprite` 容器允许先退役，volume / network 保留一轮验收窗口
- Docker 28.2.2 or newer
- The repository checked out under `~/loom`
- A `.env` file based on [.env.example](../.env.example)
- 如果本地需要执行 `mvnw` 或打包 Java 服务，本机仍需要可用的 JDK 21

## Local Compose

```bash
cp .env.example .env
docker compose up -d --build
```

如果要本地调试宿主机端口，再叠加 dev override：

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build
```

标准栈会拉起：

- `loom-server`
- `loom-web`
- `loom-node`
- `loom-mysql`

## `jd` 切换顺序

1. 预备目录与环境文件

```bash
bash deploy/scripts/bootstrap-jd.sh
```

2. 留档并退役旧 `sprite`

```bash
bash deploy/scripts/retire-sprite-jd.sh
```

这个脚本会：

- 导出当前容器、端口、volume、network 状态
- 对 `sprite` compose 执行 `down --remove-orphans`
- 不删除旧 volume / network，保留回滚窗口

3. 部署 Loom 独立栈

```bash
bash deploy/scripts/deploy-jd.sh
```

4. 运行冒烟验证

```bash
bash deploy/scripts/smoke-test-jd.sh
```

## 端口策略

- 生产只对外暴露宿主机 `80`
- `loom-server`、`loom-node`、`loom-mysql` 不映射宿主机端口
- 本地调试时才通过 `docker-compose.dev.yml` 暴露 `3306 / 8080 / 3000 / 8090`

## Java 21

Windows 上用 PowerShell 运行 `mvnw.cmd` 时，wrapper 会优先使用当前 `JAVA_HOME`，若版本过低则自动回退到本机可用的 JDK 21，例如 IntelliJ JBR。

## Notes

- The node agent is read-only in Phase 1.
- The server Vault is the write target for assets.
- The local Vault can be synced separately and is not reconciled by the app.
- 回滚策略见 [rollback.md](./rollback.md)。
## GitHub Actions Release Deploy

The repository now contains a release-only compose bundle at `deploy/docker-compose.release.yml`.

`release.yml` performs these steps on every push to `main` when relevant files change:

1. build and push `loom-server`, `loom-web`, and `loom-node` images to GHCR
2. upload the release compose file and remote scripts to the server over SSH
3. pull the exact `${GITHUB_SHA}` image tags on the server
4. run a basic smoke test against `/`, `/api/health`, and `/api/nodes`

Required GitHub secrets:

- `DEPLOY_HOST`
- `DEPLOY_USER`
- `DEPLOY_SSH_PRIVATE_KEY`

Optional GitHub secrets:

- `DEPLOY_PORT`
- `DEPLOY_ROOT`
- `DEPLOY_SSH_KNOWN_HOSTS`
- `GHCR_USERNAME`
- `GHCR_TOKEN`
- `DEPLOY_WEB_URL`

Server-side prerequisites:

- Docker and Docker Compose installed
- a writable deploy directory, defaulting to `~/loom-deploy`
- a `${DEPLOY_ROOT}/.env` file derived from the repository `.env.example`

If your GHCR package is private, set `GHCR_USERNAME` and `GHCR_TOKEN` so the remote host can `docker login ghcr.io` before pulling images.

## Real LLM Configuration

Loom now calls an OpenAI-compatible LLM endpoint whenever a user sends a chat message.

Set these values in the server `.env` file:

- `LOOM_AI_PROVIDER_LABEL`
- `LOOM_AI_BASE_URL`
- `LOOM_AI_MODEL`
- `LOOM_AI_TEMPERATURE`
- `LOOM_AI_API_KEY`

Examples:

- OpenAI: `LOOM_AI_BASE_URL=https://api.openai.com/v1`
- DeepSeek: `LOOM_AI_BASE_URL=https://api.deepseek.com/v1`
- Self-hosted OpenAI-compatible gateway: point `LOOM_AI_BASE_URL` at that service
