# 🚀 Loom 项目上线发布文档

## 适用架构

- 前后端合一（Spring Boot 内嵌静态资源）
- Docker 部署
- 镜像仓库：GHCR
- 自动化：GitHub Actions
- 服务器：单机 Docker 运行

---

## 一、版本管理规范

采用语义化版本：

```
MAJOR.MINOR.PATCH
```

示例：

```
1.0.0
1.0.1
1.1.0
2.0.0
```

规则：

| 类型   | 说明         |
| ------ | ------------ |
| MAJOR  | 破坏性更新   |
| MINOR  | 新功能       |
| PATCH  | Bug 修复     |

---

## 二、版本发布流程

### 1️⃣ 开发完成后

合并代码到 main 分支。

### 2️⃣ 打 Git Tag

```bash
git tag v1.0.0
git push origin v1.0.0
```

注意：

- Git Tag 必须以 v 开头
- 镜像版本会自动去掉 v

### 3️⃣ CI 自动执行

GitHub Actions 会自动：

1. 解析版本号 → 1.0.0
2. 构建 Docker 镜像
3. 推送到 GHCR：
   - ghcr.io/lingfeng-xiao/your-image:1.0.0
   - ghcr.io/lingfeng-xiao/your-image:latest
4. 通过 SSH 登录服务器部署该版本

---

## 三、镜像管理规则

### 镜像命名

```
ghcr.io/lingfeng-xiao/your-image:<version>
```

示例：

```
ghcr.io/lingfeng-xiao/your-image:1.0.0
ghcr.io/lingfeng-xiao/your-image:1.0.1
ghcr.io/lingfeng-xiao/your-image:latest
```

说明：

- latest 永远指向最新正式版本
- 生产环境只使用明确版本号
- 不允许生产环境直接使用 latest

---

## 四、CI 变量设计

### 固定变量

```yaml
env:
  IMAGE_NAME: ghcr.io/lingfeng-xiao/your-image
```

### 自动变量（来自 Git Tag）

```bash
VERSION=${GITHUB_REF#refs/tags/v}
```

### GitHub Secrets

在仓库 Settings → Secrets 中配置：

- HOST
- USERNAME
- SSH_KEY

---

## 五、服务器部署规则

服务器始终运行明确版本：

```bash
docker pull ghcr.io/lingfeng-xiao/your-image:1.0.0

docker stop loom || true
docker rm loom || true

docker run -d \
  --name loom \
  -p 8080:8080 \
  --restart always \
  ghcr.io/lingfeng-xiao/your-image:1.0.0
```

---

## 六、回滚流程

如新版本异常：

```bash
docker stop loom
docker rm loom

docker run -d \
  --name loom \
  -p 8080:8080 \
  ghcr.io/lingfeng-xiao/your-image:1.0.0
```

回滚时间 < 10 秒。

---

## 七、查看当前运行版本

```bash
docker inspect loom | grep Image
```

---

## 八、开发与生产区分

| 阶段 | 前端           | 后端              | Docker     |
| ---- | -------------- | ----------------- | ---------- |
| 开发 | npm run dev    | spring-boot:run   | ❌ 不需要  |
| 构建 | npm build      | mvn package       | 可选       |
| 生产 | 静态资源内嵌   | jar 运行          | ✅ 必须    |

---

## 九、完整发布流程图

```
开发
 ↓
合并 main
 ↓
git tag v1.0.0
 ↓
push tag
 ↓
CI 构建镜像
 ↓
推送 GHCR
 ↓
SSH 自动部署
 ↓
服务器运行 1.0.0
```

---

## 十、重要原则

1. 生产环境禁止只用 latest
2. 每次发布必须打 Git Tag
3. 版本号 = 镜像版本
4. 所有版本必须可回滚
5. 容器不是版本管理对象，镜像才是
