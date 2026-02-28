# Loom 项目

## 项目简介
Loom 是一个前后端分离的项目模板，使用 Vue 3.4.21 + Vite 5.2.0 作为前端技术栈，Spring Boot 3.2.0 + Java 17 作为后端技术栈。项目支持单体部署和前后端分离部署。

## 技术栈

### 前端技术栈
- **框架**: Vue 3.4.21
- **构建工具**: Vite 5.2.0
- **路由**: Vue Router 4.3.0
- **状态管理**: 预留 store 目录（可根据需要集成 Pinia 或 Vuex）
- **HTTP 客户端**: 预留 api 目录（可根据需要集成 Axios）

### 后端技术栈
- **框架**: Spring Boot 3.2.0
- **语言**: Java 17
- **构建工具**: Maven 3.11.0+
- **Web框架**: Spring Web
- **监控**: Spring Boot Actuator (健康检查、指标监控)
- **打包方式**: JAR

## 目录结构
```
loom/
├── frontend/      （Vue 3 + Vite 项目）
│   ├── src/       （源代码目录）
│   │   ├── api/   （API 调用模块）
│   │   ├── assets/（静态资源）
│   │   ├── components/（组件）
│   │   ├── router/（路由配置）
│   │   ├── store/ （状态管理）
│   │   ├── utils/ （工具函数）
│   │   ├── views/ （页面组件）
│   │   ├── App.vue（根组件）
│   │   └── main.js（入口文件）
│   ├── public/    （公共资源）
│   ├── package.json（项目配置）
│   └── vite.config.js（Vite 配置）
├── backend/       （Spring Boot + Java 17）
│   ├── src/        （源代码）
│   │   ├── main/java/com/loom/ （Java 源码）
│   │   │   ├── config/   （配置类）
│   │   │   ├── controller/ （控制器）
│   │   │   ├── model/    （数据模型）
│   │   │   ├── repository/ （数据访问）
│   │   │   ├── service/   （业务逻辑）
│   │   │   └── utils/    （工具类）
│   │   └── main/resources/ （资源文件）
│   ├── target/     （构建产物）
│   ├── pom.xml     （Maven 配置）
│   └── .mvn/       （Maven 包装器）
├── docker/        （Dockerfile）
└── README.md
```

## 启动方式

### 前端启动
1. 进入 frontend 目录：`cd frontend`
2. 安装依赖：`npm install`
3. 启动开发服务器：`npm run dev`
4. 访问：http://localhost:5173

#### 前端代理配置
前端开发服务器配置了 API 代理，将 `/api` 路径代理到 `http://localhost:8080`，方便本地开发时调用后端 API。配置文件位于 `frontend/vite.config.js`。

### 后端启动

#### 开发模式启动（推荐）
1. 进入 backend 目录：`cd backend`
2. 运行开发模式：`mvn spring-boot:run`
3. 访问 API：http://localhost:8080/api/hello

#### 构建后启动
1. 进入 backend 目录：`cd backend`
2. 构建项目：`mvn clean package`
3. 启动应用：`java -jar target/loom-backend.jar`
4. 访问 API：http://localhost:8080/api/hello

## 构建方式

### 前端构建
1. 进入 frontend 目录：`cd frontend`
2. 安装依赖：`npm install`
3. 执行构建：`npm run build`
4. 构建产物将生成在 `frontend/dist` 目录，包含优化后的静态资源文件。

#### 构建配置
前端构建使用 Vite 进行，配置文件位于 `frontend/vite.config.js`，可根据需要调整构建选项。

### 后端构建
```bash
cd backend
mvn clean package
```
构建产物将生成在 `backend/target/loom-backend.jar`。

#### 构建配置
后端构建使用 Maven 进行，配置文件位于 `backend/pom.xml`，包含以下主要插件：
- Spring Boot Maven Plugin：用于打包 Spring Boot 应用
- Maven Compiler Plugin：用于编译 Java 代码
- Maven Surefire Plugin：用于运行测试

## Docker 运行方式
Dockerfile 采用多阶段构建，会自动构建前端和后端，并将前端构建产物复制到后端的静态资源目录中。

1. 进入 docker 目录：`cd docker`
2. 构建 Docker 镜像：`docker build -t loom-backend .`
3. 运行容器：`docker run -p 8080:8080 loom-backend`

## 后端 API 接口

### 健康检查
- **路径**: `/actuator/health`
- **方法**: GET
- **描述**: 检查应用健康状态

### 示例接口
- **路径**: `/api/hello`
- **方法**: GET
- **描述**: 返回问候信息
- **响应示例**:
  ```json
  {
    "message": "Hello from Loom backend!"
  }
  ```

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
