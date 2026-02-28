# 创建 loom 项目结构

## 步骤 1：创建项目基础结构
- 创建 loom 目录
- 在 loom 目录下创建 frontend、backend、docker 子目录
- 创建 README.md 文件

## 步骤 2：生成前端模板（Vue 3 + Vite）
- 在 frontend 目录初始化 Vue 3 + Vite 项目
- 配置 SPA 路由
- 设置 API 请求使用相对路径 (/api)
- 创建基础页面示例
- 配置 package.json 包含基本脚本（install、dev、build）
- 创建 .gitignore 文件，排除 dist 目录

## 步骤 3：生成后端模板（Spring Boot + Java 17）
- 在 backend 目录初始化 Spring Boot 项目（Java 17）
- 添加 Spring Boot Web 依赖
- 创建 Maven 构建配置
- 实现 REST API 示例：GET /api/hello
- 创建目录结构：src/main/java/com/loom/controller、service、repository
- 配置 application.yml：server.port=8080，spring.application.name=loom

## 步骤 4：生成 Docker 模板
- 在 docker 目录创建 Dockerfile
- 使用 Java 17 镜像
- 配置复制 backend 构建产物：target/app.jar
- 设置容器启动命令：java -jar app.jar
- 暴露端口：8080

## 步骤 5：生成 README.md
- 编写项目简介
- 提供启动方式
- 说明构建方式
- 描述 Docker 运行方式
- 说明未来扩展方向

## 步骤 6：验证项目结构
- 检查所有目录和文件是否正确创建
- 确保前端和后端物理分离
- 验证配置文件是否符合要求

该计划将确保项目满足当前可单体部署、未来可前后端分离、CI/CD 可扩展的要求。