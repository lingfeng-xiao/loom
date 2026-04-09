# loom Deployment

## Local Acceptance Deployment

Use this flow when you want to validate the current loom shell locally without relying on an external MySQL instance.

Backend:

1. Run from [apps/server](C:/Users/16343/Desktop/loom/apps/server) with the `local` profile.
2. The `local` profile uses an embedded H2 database stored under `.loom-local`.
3. Default API URL: `http://127.0.0.1:8080`

Frontend:

1. Run from [apps/web](C:/Users/16343/Desktop/loom/apps/web) with Vite dev server.
2. Default acceptance URL: `http://127.0.0.1:4173`
3. `/api` requests proxy to the local backend during development.

Recommended commands:

```powershell
cd C:\Users\16343\Desktop\loom\apps\server
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

```powershell
cd C:\Users\16343\Desktop\loom\apps\web
npm run dev -- --host 127.0.0.1 --port 4173
```

## Production Baseline

The production deployment still targets the template-style host layout until the loom infra docs are fully renamed:

- install root: `/opt/template`
- compose file: `/opt/template/compose/docker-compose.production.yml`
- environment file: `/opt/template/env/.env.production`
- state directory: `/opt/template/state`
- systemd unit: `template.service`

## Development-time Production Access

开发期间允许使用 `ssh jd` 连接生产机，但必须遵守以下规则：

- 只有 PM / Orchestrator 可以执行生产机命令
- 并发开发智能体不得直接登录生产机
- 默认先本地验证，再进入生产机调试窗口
- 生产机上优先执行只读检查，再决定是否进入写操作
- 任何写操作都必须具备可执行回退路径

## Runtime Topology

- `template-edge`: public nginx entrypoint on port `80`
- `template-web`: internal static web container
- `template-server`: internal Spring Boot API
- `template-node`: internal probe and heartbeat agent
- `template-mysql`: internal MySQL database

## Release Flow

1. GitHub Actions builds the server, web, and node images.
2. The release workflow pushes images to GHCR.
3. A deploy bundle is uploaded to the target host.
4. `remote-release.sh` renders a candidate env file and runs preflight checks.
5. The candidate deployment is started, smoke-tested, and then promoted to the current env snapshot.
6. The systemd unit is reloaded or started after deployment succeeds.

开发期若需要在生产机做候选验证，仍按同一条 release / smoke / rollback 链路执行，不允许跳过现有脚本直接改运行态。

## Files That Must Stay Aligned

- `deploy/compose/docker-compose.production.yml`
- `deploy/compose/edge/nginx.conf`
- `deploy/systemd/template.service`
- `deploy/scripts/remote-*.sh`
- `.env.example`

## Notes

- Local acceptance now prefers the `local` profile so reviewers can start the backend without provisioning MySQL first.
- If image names, service names, ports, or startup modes change, update this file together with the release and rollback docs.
