# Loom 项目

## 项目简介
Loom 是一个前后端分离的项目模板，使用 Vue 3 + Vite 作为前端技术栈，Spring Boot + Java 17 作为后端技术栈。项目支持单体部署和未来的前后端分离部署。

## 目录结构
```
loom/
├── frontend/      （Vue 3 + Vite 项目）
├── backend/       （Spring Boot + Java 17）
├── docker/        （Dockerfile）
└── README.md
```

## 启动方式

### 前端启动
1. 进入 frontend 目录
2. 安装依赖：`npm install`
3. 启动开发服务器：`npm run dev`
4. 访问：http://localhost:5173

### 后端启动
1. 进入 backend 目录
2. 构建项目：`mvn clean package`
3. 启动应用：`java -jar target/app.jar`
4. 访问 API：http://localhost:8080/api/hello

## 构建方式

### 前端构建
```bash
cd frontend
npm install
npm run build
```
构建产物将生成在 `frontend/dist` 目录。

### 后端构建
```bash
cd backend
mvn clean package
```
构建产物将生成在 `backend/target/app.jar`。

## Docker 运行方式
1. 首先构建后端项目：`mvn clean package`
2. 进入 docker 目录
3. 构建 Docker 镜像：`docker build -t loom-backend .`
4. 运行容器：`docker run -p 8080:8080 loom-backend`

## 未来扩展说明

### 前后端分离部署
- 前端可以部署到静态资源服务器（如 Nginx、CDN）
- 后端可以部署到独立的应用服务器
- API 路径保持 `/api` 相对形式，便于前端调用

### CI/CD 扩展
- 可加入 GitHub Actions 自动构建
- 可推送镜像到 Docker 仓库
- 可配置自动化测试和部署流程

### 功能扩展
- 前端可添加更多页面和组件
- 后端可添加数据库支持和业务逻辑
- 可集成认证和授权功能
- 可添加日志和监控
