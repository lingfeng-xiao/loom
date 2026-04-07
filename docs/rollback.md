# Loom 运维回滚文档

## 适用场景

- `jd` 上 Loom 独立 compose 栈切换后 smoke test 失败
- `loom-web` 首页或 `/api/health` 不通
- MySQL、节点、资产写入在切换后异常

## 回滚原则

- 先停新栈，再决定是否恢复旧 `sprite`
- 旧 `sprite` 的 volume / network 在验收窗口内不删除
- 先保留现场，再做清理

## 最小回滚步骤

1. 记录现状

```bash
docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Ports}}'
ss -ltnp
```

2. 停掉 Loom

```bash
cd ~/loom
docker compose --env-file .env down --remove-orphans
```

3. 如果需要恢复旧 `sprite`

- 回到旧 `sprite` compose 目录
- 执行 `docker compose up -d`
- 不要在验收窗口内手动删除旧 volume

4. 重新验证入口

```bash
curl -fsS http://127.0.0.1 >/dev/null
curl -fsS http://127.0.0.1/api/health >/dev/null
```

## 切换成功后的清理

- 只有在 Loom 独立栈通过一轮完整验收后，才考虑清理旧 `sprite` volume / network
- 清理前先导出一次 `docker ps`、`docker volume ls`、`docker network ls`
