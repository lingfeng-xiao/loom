# 验证 Dockerfile 启动项目

## 步骤 1：构建 Docker 镜像
- 进入项目根目录
- 执行 `docker build -t loom-app -f docker/Dockerfile .` 命令构建镜像

## 步骤 2：运行 Docker 容器
- 执行 `docker run -p 8080:8080 --name loom-container loom-app` 命令启动容器

## 步骤 3：验证服务是否正常运行
- 检查容器是否成功启动
- 使用 `curl http://localhost:8080` 验证服务是否响应
- 查看容器日志，确认服务是否正常运行

## 步骤 4：清理资源
- 停止并删除容器
- 删除构建的镜像（如果需要）

## 预期结果
- Docker 镜像构建成功
- 容器正常启动
- 服务在 8080 端口响应
- 前端页面可以访问